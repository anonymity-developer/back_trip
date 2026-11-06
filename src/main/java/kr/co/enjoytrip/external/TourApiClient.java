package kr.co.enjoytrip.external;

import java.util.ArrayList;
import java.util.List;
import kr.co.enjoytrip.config.AppConfig;
import kr.co.enjoytrip.dto.AttractionDto;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class TourApiClient {
    private static final String BASE="https://apis.data.go.kr/B551011/KorService2/";
    private final XmlHttpClient http=new XmlHttpClient();

    public Page fetchPage(int areaCode,int page,int pageSize) throws Exception {
        String url=BASE+"areaBasedList2?serviceKey="+key()+"&MobileOS=ETC&MobileApp=EnjoyTrip&numOfRows="+pageSize+"&pageNo="+page+"&areaCode="+areaCode+"&arrange=C";
        Document doc=http.get(url);
        return parsePage(doc);
    }

    Page parsePage(Document doc) {
        ensureSuccess(doc);
        int total=parseInt(XmlHttpClient.field(doc.getDocumentElement(),"totalCount"));
        List<AttractionDto> rows=new ArrayList<>();
        NodeList items=doc.getElementsByTagName("item");
        for(int i=0;i<items.getLength();i++) {
            Element item=(Element)items.item(i);
            long id=parseLong(XmlHttpClient.field(item,"contentid"));
            int type=parseInt(XmlHttpClient.field(item,"contenttypeid"));
            int area=parseInt(XmlHttpClient.field(item,"areacode"));
            String title=XmlHttpClient.field(item,"title");
            if(id<=0||type<=0||area<=0||title.isBlank()) continue;
            String address=(XmlHttpClient.field(item,"addr1")+" "+XmlHttpClient.field(item,"addr2")).trim();
            String gugun=XmlHttpClient.field(item,"sigungucode");
            Integer sigunguCode=gugun.isBlank()||gugun.equals("0")?null:Integer.valueOf(gugun);
            if(sigunguCode!=null && sigunguCode<0)throw new IllegalStateException("관광정보 구군 코드가 올바르지 않습니다.");
            Double lat=decimal(XmlHttpClient.field(item,"mapy")),lng=decimal(XmlHttpClient.field(item,"mapx"));
            if(lat==null||lng==null||lat==0||lng==0||lat < -90||lat > 90||lng < -180||lng > 180){lat=null;lng=null;}
            rows.add(new AttractionDto(0,id,title,type,area,sigunguCode,address,"",lat,lng,
                    XmlHttpClient.field(item,"firstimage"),XmlHttpClient.field(item,"modifiedtime")));
        }
        return new Page(total,items.getLength(),rows);
    }

    public List<String> fetchImages(long contentId) throws Exception {
        String url=BASE+"detailImage2?serviceKey="+key()+"&MobileOS=ETC&MobileApp=EnjoyTrip&contentId="+contentId+"&imageYN=Y&numOfRows=50&pageNo=1";
        Document doc=http.get(url); ensureSuccess(doc);
        List<String> images=new ArrayList<>(); NodeList items=doc.getElementsByTagName("item");
        for(int i=0;i<items.getLength();i++) {
            String value=XmlHttpClient.field((Element)items.item(i),"originimgurl");
            if(!value.isBlank()) images.add(value);
        }
        return images;
    }

    public String fetchOverview(long contentId) throws Exception {
        String url=BASE+"detailCommon2?serviceKey="+key()+"&MobileOS=ETC&MobileApp=EnjoyTrip&contentId="+contentId+"&defaultYN=Y&overviewYN=Y";
        Document doc=http.get(url);ensureSuccess(doc);
        NodeList items=doc.getElementsByTagName("item");
        if(items.getLength()==0)return "";
        String html=XmlHttpClient.field((Element)items.item(0),"overview");
        return html.replaceAll("(?i)<br\\s*/?>",System.lineSeparator()).replaceAll("<[^>]*>","").replace("&nbsp;"," ").replace("&amp;","&").trim();
    }

    private void ensureSuccess(Document doc) {
        String code=XmlHttpClient.field(doc.getDocumentElement(),"resultCode");
        if(!code.isBlank()&&!"0000".equals(code)&&!"00".equals(code))
            throw new IllegalStateException("Tour API error: "+code);
    }
    private String key(){return AppConfig.encodedPortalKey("ENJOYTRIP_TOUR_API_KEY");}
    private int parseInt(String value){try{return Integer.parseInt(value);}catch(NumberFormatException e){return 0;}}
    private long parseLong(String value){try{return Long.parseLong(value);}catch(NumberFormatException e){return 0;}}
    private Double decimal(String value){try{double n=Double.parseDouble(value);return Double.isFinite(n)?n:null;}catch(NumberFormatException e){return null;}}
    public record Page(int total,int rawItems,List<AttractionDto> attractions){}
}
