package kr.co.enjoytrip.service;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicBoolean;
import kr.co.enjoytrip.config.AppConfig;
import kr.co.enjoytrip.dao.AttractionDao;
import kr.co.enjoytrip.dao.LookupDao;
import kr.co.enjoytrip.dto.AttractionDto;
import kr.co.enjoytrip.dto.SidoDto;
import kr.co.enjoytrip.dto.ContentTypeDto;
import kr.co.enjoytrip.external.TourApiClient;

public class AttractionService {
    private final AttractionDao dao;
    private final LookupDao lookup;
    private final TourApiClient api;
    private final AtomicBoolean syncing=new AtomicBoolean(false);
    private volatile String syncStatus="아직 동기화하지 않았습니다.";

    public AttractionService(){this(new AttractionDao(),new LookupDao(),new TourApiClient());}
    AttractionService(AttractionDao dao,LookupDao lookup,TourApiClient api){this.dao=dao;this.lookup=lookup;this.api=api;}

    public List<SidoDto> sidos() throws SQLException{return lookup.sidos();}
    public List<ContentTypeDto> contentTypes() throws SQLException{return lookup.contentTypes();}

    public List<AttractionDto> search(Integer area,Integer type,String keyword,int page) throws SQLException {
        if(area!=null&&sidos().stream().noneMatch(s->s.code()==area))throw new ValidationException("등록되지 않은 지역입니다.");
        if(type!=null&&contentTypes().stream().noneMatch(t->t.code()==type))throw new ValidationException("등록되지 않은 유형입니다.");
        return dao.search(area,type,keyword,Math.max(1,page),21);
    }
    public List<AttractionDto> autocomplete(String query) throws SQLException {
        if(query==null||query.isBlank()||query.length()>100)throw new ValidationException("검색어는 1~100자로 입력하세요.");
        return dao.search(null,null,query.trim(),1,10);
    }
    public AttractionDto findStored(long id) throws SQLException{return dao.find(id);}
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
            List<SidoDto> regions=sidos();
            Set<Integer> types=contentTypes().stream().map(ContentTypeDto::code).collect(Collectors.toSet());
            if(regions.isEmpty()||types.isEmpty())throw new IllegalStateException("관광정보 기준정보가 없습니다. DB 초기 데이터를 확인하세요.");
            for(SidoDto region:regions) {
                int areaCode=region.code();
                Set<Integer> guguns=lookup.guguns(areaCode).stream().map(g->g.code()).collect(Collectors.toSet());
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
                    for(AttractionDto a:data.attractions()) {
                        if(a.areaCode()!=areaCode||!types.contains(a.contentType())||a.sigunguCode()!=null&&!guguns.contains(a.sigunguCode()))
                            throw new IllegalStateException("관광정보 기준정보에 없는 지역·유형·구군 코드입니다. content_id="+a.contentId());
                    }
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
