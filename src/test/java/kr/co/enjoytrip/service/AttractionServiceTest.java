package kr.co.enjoytrip.service;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import kr.co.enjoytrip.dao.AttractionDao;
import kr.co.enjoytrip.dao.LookupDao;
import kr.co.enjoytrip.dto.*;
import kr.co.enjoytrip.external.TourApiClient;
import org.junit.jupiter.api.Test;

class AttractionServiceTest {
    @Test void keepsAutocompleteLimitAndStoredLookupReadOnly() throws Exception {
        var dao=new AttractionDao(){
            @Override public List<AttractionDto> search(Integer area,Integer type,String q,int page,int size){
                assertEquals("Place",q);assertEquals(1,page);assertEquals(10,size);return List.of();
            }
            @Override public AttractionDto find(long id){assertEquals(42,id);return null;}
        };
        var api=new TourApiClient(){@Override public String fetchOverview(long id){fail("Stored lookup must not call the API");return "";}};
        var service=new AttractionService(dao,new LookupDao(),api);
        assertTrue(service.autocomplete(" Place ").isEmpty());assertNull(service.findStored(42));
        assertThrows(ValidationException.class,()->service.autocomplete(" "));
    }
    @Test void rejectsUnknownFilterAndUnknownSyncGugunBeforeWriting() throws Exception {
        var dao=new AttractionDao(){
            @Override public void upsertAll(List<AttractionDto> rows){fail("Unknown code must not be written");}
        };
        var lookup=new LookupDao(){
            @Override public List<SidoDto> sidos(){return List.of(new SidoDto(1,"서울"));}
            @Override public List<ContentTypeDto> contentTypes(){return List.of(new ContentTypeDto(12,"관광지"));}
            @Override public List<GugunDto> guguns(int sido){return List.of(new GugunDto(1,23,"종로구"));}
        };
        var api=new TourApiClient(){
            @Override public Page fetchPage(int area,int page,int size){
                return new Page(1,1,List.of(new AttractionDto(0,123,"Place",12,1,999,"","",37.5,126.9,"","")));
            }
        };
        var service=new AttractionService(dao,lookup,api);
        assertThrows(ValidationException.class,()->service.search(999,null,"",1));
        assertThrows(ValidationException.class,()->service.search(null,999,"",1));
        service.syncAll();assertTrue(service.syncStatus().contains("기준정보에 없는"));
    }
}
