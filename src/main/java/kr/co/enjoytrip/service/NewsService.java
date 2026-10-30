package kr.co.enjoytrip.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import kr.co.enjoytrip.dao.NewsDao;
import kr.co.enjoytrip.dto.NewsDto;
import kr.co.enjoytrip.external.XmlHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class NewsService {
    private static final List<String> FEEDS=List.of(
            "https://www.newsis.com/RSS/culture.xml",
            "https://www.newsis.com/RSS/country.xml",
            "https://www.newsis.com/RSS/met.xml");
    private static final List<String> TOPICS=List.of(
            "여행","관광","축제","휴가","나들이","단풍","명소","캠핑","트레킹","해수욕장","국립공원","둘레길","벚꽃");
    private final NewsDao dao=new NewsDao();
    private final XmlHttpClient http=new XmlHttpClient();
    private final AtomicBoolean syncing=new AtomicBoolean();
    private volatile String status="아직 수집하지 않았습니다.";
    public List<NewsDto> list(int page)throws Exception{return dao.list(page);}
    public String status(){return status;}
    public void sync(){
        if(!syncing.compareAndSet(false,true))return;
        int saved=0,matching=0,feedsRead=0;
        Set<String> seen=new HashSet<>();
        try{
            for(String feed:FEEDS){
                Document doc;
                try{doc=http.get(feed);feedsRead++;}catch(Exception e){continue;}
                NodeList items=doc.getElementsByTagName("item");
                for(int i=0;i<items.getLength();i++){
                    Element item=(Element)items.item(i);String title=XmlHttpClient.field(item,"title"),url=XmlHttpClient.field(item,"link");
                    if(title.isBlank()||title.length()>500||url.length()>1000||!url.startsWith("https://www.newsis.com/")||
                            TOPICS.stream().noneMatch(title::contains)||!seen.add(url))continue;
                    matching++;
                    String date=XmlHttpClient.field(item,"pubDate");LocalDateTime published=null;
                    try{published=ZonedDateTime.parse(date,DateTimeFormatter.RFC_1123_DATE_TIME).withZoneSameInstant(ZoneId.of("Asia/Seoul")).toLocalDateTime();}catch(Exception ignored){}
                    if(dao.insert(title,"뉴시스",url,published))saved++;
                }
            }
            status=feedsRead==0?"수집 실패: RSS 3곳에 연결할 수 없습니다.":
                    "완료: 새 기사 "+saved+"건 / 관련 기사 "+matching+"건 확인 (RSS "+feedsRead+"/"+FEEDS.size()+"곳)";
        }catch(Exception e){status="수집 실패: "+e.getClass().getSimpleName();}
        finally{syncing.set(false);}
    }
}
