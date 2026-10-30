package kr.co.enjoytrip.service;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import kr.co.enjoytrip.config.AppConfig;
import kr.co.enjoytrip.dao.AttractionDao;
import kr.co.enjoytrip.dto.AttractionDto;
import kr.co.enjoytrip.external.TourApiClient;

public class AttractionService {
    private static final int[] AREA_CODES={1,2,3,4,5,6,7,8,31,32,33,34,35,36,37,38,39};
    private final AttractionDao dao=new AttractionDao();
    private final TourApiClient api=new TourApiClient();
    private final AtomicBoolean syncing=new AtomicBoolean(false);
    private volatile String syncStatus="아직 동기화하지 않았습니다.";

    public List<AttractionDto> search(Integer area,Integer type,String keyword,int page) throws SQLException {
        return dao.search(area,type,keyword,Math.max(1,page),21);
    }
    public AttractionDto find(long id) throws SQLException {
        AttractionDto attraction=dao.find(id);
        if(attraction!=null&&(attraction.description()==null||attraction.description().isBlank())&&!AppConfig.get("ENJOYTRIP_TOUR_API_KEY").isEmpty()){
            try{String overview=api.fetchOverview(attraction.contentId());if(!overview.isBlank()){dao.updateDescription(id,overview);return dao.find(id);}}catch(Exception ignored){}
        }
        return attraction;
    }
    public List<String> images(AttractionDto attraction) throws SQLException {
        List<String> images=dao.images(attraction.id());
        if(!images.isEmpty() || AppConfig.get("ENJOYTRIP_TOUR_API_KEY").isEmpty()) return images;
        try {
            int order=0;
            for(String url:api.fetchImages(attraction.contentId())) dao.addImage(attraction.id(),url,order++);
            return dao.images(attraction.id());
        } catch(Exception e) {return images;}
    }
    public String syncStatus(){return syncStatus;}
    public void syncAll() {
        if(!syncing.compareAndSet(false,true)) return;
        int saved=0;
        try {
            for(int areaCode:AREA_CODES) {
                int pageSize=1000;
                for(int page=1;page<=1000;page++) {
                    TourApiClient.Page data=api.fetchPage(areaCode,page,pageSize);
                    if(page==1 && data.total()>0 && data.rawItems()==0) {
                        pageSize=100;
                        data=api.fetchPage(areaCode,page,pageSize);
                    }
                    if(data.total()>0 && data.rawItems()==0)
                        throw new IllegalStateException("관광정보 지역 "+areaCode+"의 전체 건수는 "+data.total()+"건이지만 목록 항목이 비어 있습니다.");
                    if(page==1 && data.rawItems()>0 && data.attractions().isEmpty())
                        throw new IllegalStateException("관광정보 지역 "+areaCode+"의 항목 "+data.rawItems()+"건에 필수 필드가 없습니다.");
                    dao.upsertAll(data.attractions());
                    saved+=data.attractions().size();
                    syncStatus="동기화 중: "+saved+"건 저장 (지역 "+areaCode+")";
                    if(data.rawItems()==0 || page*pageSize>=data.total()) break;
                    Thread.sleep(300);
                }
            }
            if(saved==0)throw new IllegalStateException("관광정보 API가 모든 지역에서 항목 0건을 반환했습니다. 인증키와 활용신청 상태를 확인하세요.");
            syncStatus="완료: "+saved+"건 저장";
        } catch(Exception e) {
            syncStatus="중단: "+saved+"건 저장. "+failure(e);
        } finally {syncing.set(false);}
    }

    private String failure(Exception error) {
        if(error instanceof HttpTimeoutException)return "관광정보 API 응답 시간 초과";
        if(error instanceof UnknownHostException)return "관광정보 API 주소를 찾을 수 없음";
        if(error instanceof ConnectException)return "관광정보 API 연결 실패";
        if(error instanceof IOException && error.getMessage()!=null &&
                error.getMessage().matches("External service unavailable: HTTP [0-9]{3}"))
            return error.getMessage().replace("External service unavailable: ","관광정보 API ");
        if(error instanceof IllegalStateException && error.getMessage()!=null &&
                error.getMessage().matches("Tour API error: [A-Za-z0-9_]+"))
            return error.getMessage();
        if(error instanceof IllegalStateException && error.getMessage()!=null &&
                error.getMessage().startsWith("관광정보 "))
            return error.getMessage();
        return error.getClass().getSimpleName();
    }
}
