package kr.co.enjoytrip.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kr.co.enjoytrip.config.AppConfig;
import kr.co.enjoytrip.dto.AttractionDto;
import kr.co.enjoytrip.external.XmlHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class InfoService {
    private static final Map<Integer,Integer> ZCODES=Map.ofEntries(
        Map.entry(1,11),Map.entry(2,28),Map.entry(3,30),Map.entry(4,27),Map.entry(5,29),Map.entry(6,26),
        Map.entry(7,31),Map.entry(8,36),Map.entry(31,41),Map.entry(32,42),Map.entry(33,43),Map.entry(34,44),
        Map.entry(35,47),Map.entry(36,48),Map.entry(37,45),Map.entry(38,46),Map.entry(39,50));
    private final XmlHttpClient xml=new XmlHttpClient();
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final Map<String,CacheEntry> cache=new ConcurrentHashMap<>();
    private final Map<Integer,RegionCache> chargerCache=new ConcurrentHashMap<>();

    public Info weather(AttractionDto a)throws Exception{
        ensureLocation(a);String key="weather:"+a.id();Info cached=read(key);if(cached!=null)return cached;
        String apiKey=encode(AppConfig.required("ENJOYTRIP_WEATHER_API_KEY"));
        String params="lat="+a.latitude()+"&lon="+a.longitude()+"&appid="+apiKey+"&units=metric&lang=kr";
        JsonObject current=requestJson("https://api.openweathermap.org/data/2.5/weather?"+params);
        JsonObject forecast=requestJson("https://api.openweathermap.org/data/2.5/forecast?"+params);
        double temp=current.getAsJsonObject("main").get("temp").getAsDouble();
        String condition=current.getAsJsonArray("weather").get(0).getAsJsonObject().get("description").getAsString();
        StringBuilder summary=new StringBuilder(String.format("현재 %.1f°C, %s. 예보: ",temp,condition));
        JsonArray entries=forecast.getAsJsonArray("list");int shown=0;
        for(int i=0;i<entries.size()&&shown<5;i++){
            JsonObject entry=entries.get(i).getAsJsonObject();String time=entry.get("dt_txt").getAsString();
            if(!time.contains("12:00:00"))continue;
            if(shown++>0)summary.append(" · ");
            summary.append(time,0,10).append(' ').append(String.format("%.0f°C",entry.getAsJsonObject("main").get("temp").getAsDouble()));
        }
        return save(key,new Info(summary.toString(),List.of(),Instant.now().toString()),Duration.ofMinutes(10));
    }

    public Info sun(AttractionDto a,LocalDate day)throws Exception{
        ensureLocation(a);String key="sun:"+a.id()+":"+day;Info cached=read(key);if(cached!=null)return cached;
        String url="https://apis.data.go.kr/B090041/openapi/service/RiseSetInfoService/getLCRiseSetInfo?ServiceKey="+
                AppConfig.encodedPortalKey("ENJOYTRIP_DATA_GO_API_KEY")+"&locdate="+day.format(DateTimeFormatter.BASIC_ISO_DATE)+
                "&longitude="+a.longitude()+"&latitude="+a.latitude();
        Document doc=xml.get(url);Element root=doc.getDocumentElement();String rise=XmlHttpClient.field(root,"sunrise"),set=XmlHttpClient.field(root,"sunset");
        if(rise.isBlank()||set.isBlank())throw new IllegalStateException("Sun data unavailable");
        return save(key,new Info(day+" 일출 "+rise+" · 일몰 "+set+" (KST)",List.of(),Instant.now().toString()),Duration.ofHours(12));
    }

    public Info chargers(AttractionDto a)throws Exception{
        ensureLocation(a);Integer zcode=ZCODES.get(a.areaCode());if(zcode==null)throw new IllegalStateException("Unknown region");
        List<Charger> all=regionChargers(zcode);
        List<Charger> nearby=all.stream().filter(c->distance(a.latitude(),a.longitude(),c.latitude(),c.longitude())<=10)
                .sorted(Comparator.comparingDouble(c->distance(a.latitude(),a.longitude(),c.latitude(),c.longitude())))
                .limit(20).toList();
        return new Info(nearby.isEmpty()?"반경 10km 안에 표시할 충전소가 없습니다.":"반경 10km 충전소 "+nearby.size()+"곳 (한국환경공단)",nearby,Instant.now().toString());
    }

    private List<Charger> regionChargers(int zcode)throws Exception{
        RegionCache cached=chargerCache.get(zcode);
        if(cached!=null&&Instant.now().isBefore(cached.expires()))return cached.items();
        synchronized(chargerCache){
            cached=chargerCache.get(zcode);if(cached!=null&&Instant.now().isBefore(cached.expires()))return cached.items();
            List<Charger> items=new ArrayList<>();int total=1;
            for(int page=1;page<=100&&((page-1)*1000<total);page++){
                String url="https://apis.data.go.kr/B552584/EvCharger/getChargerInfo?ServiceKey="+
                    AppConfig.encodedPortalKey("ENJOYTRIP_DATA_GO_API_KEY")+"&pageNo="+page+"&numOfRows=1000&zcode="+zcode;
                Document doc=xml.get(url);Element root=doc.getDocumentElement();
                String result=XmlHttpClient.field(root,"resultCode");if(!result.isBlank()&&!result.equals("00"))throw new IllegalStateException("Charger API: "+result);
                total=Integer.parseInt(XmlHttpClient.field(root,"totalCount"));NodeList nodes=doc.getElementsByTagName("item");
                for(int i=0;i<nodes.getLength();i++){
                    Element node=(Element)nodes.item(i);
                    try{
                        double lat=Double.parseDouble(XmlHttpClient.field(node,"lat")),lng=Double.parseDouble(XmlHttpClient.field(node,"lng"));
                        if("Y".equals(XmlHttpClient.field(node,"delYn")))continue;
                        items.add(new Charger(XmlHttpClient.field(node,"statNm"),XmlHttpClient.field(node,"addr"),lat,lng,
                                status(XmlHttpClient.field(node,"stat")),XmlHttpClient.field(node,"statUpdDt")));
                    }catch(NumberFormatException ignored){}
                }
            }
            List<Charger> done=List.copyOf(items);chargerCache.put(zcode,new RegionCache(done,Instant.now().plus(Duration.ofMinutes(15))));return done;
        }
    }

    private JsonObject requestJson(String url)throws Exception{
        HttpRequest req=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().build();
        HttpResponse<String> response=http.send(req,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if(response.statusCode()!=200||response.body().length()>2_000_000)throw new IllegalStateException("Weather API unavailable");
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }
    private String status(String code){return switch(code){case "2"->"충전 가능";case "3"->"충전 중";case "1"->"통신 이상";case "4"->"운영 중지";case "5"->"점검 중";default->"상태 미확인";};}
    private void ensureLocation(AttractionDto a){if(a.latitude()==null||a.longitude()==null)throw new ValidationException("좌표 정보가 없습니다.");}
    private double distance(double lat1,double lon1,double lat2,double lon2){double a=Math.pow(Math.sin(Math.toRadians(lat2-lat1)/2),2)+Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.pow(Math.sin(Math.toRadians(lon2-lon1)/2),2);return 6371*2*Math.atan2(Math.sqrt(a),Math.sqrt(1-a));}
    private String encode(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8);}
    private Info read(String key){CacheEntry entry=cache.get(key);return entry!=null&&Instant.now().isBefore(entry.expires())?entry.info():null;}
    private Info save(String key,Info info,Duration ttl){cache.put(key,new CacheEntry(info,Instant.now().plus(ttl)));return info;}
    public record Info(String summary,List<Charger> chargers,String fetchedAt){}
    public record Charger(String name,String address,double latitude,double longitude,String status,String updatedAt){}
    private record CacheEntry(Info info,Instant expires){}
    private record RegionCache(List<Charger> items,Instant expires){}
}
