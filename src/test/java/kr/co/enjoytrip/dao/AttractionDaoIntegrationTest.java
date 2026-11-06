package kr.co.enjoytrip.dao;

import static org.junit.jupiter.api.Assertions.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import kr.co.enjoytrip.config.Database;
import kr.co.enjoytrip.dto.AttractionDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named="ENJOYTRIP_DB_TEST",matches="true")
class AttractionDaoIntegrationTest {
    @Test void roundTripsNullableCodesPreservesIdsAndRollsBackFailedBatch() throws Exception {
        try(Connection c=Database.open()){
            assertEquals("enjoytrip_test",c.getCatalog(),"Integration tests must use dedicated enjoytrip_test database");
        }
        long content=8_000_000_000L+Math.abs(UUID.randomUUID().getLeastSignificantBits()%100_000_000L);
        String title="DB integration "+content;
        var dao=new AttractionDao();
        try {
            dao.upsert(row(content,title,null,"overview"));
            var original=dao.search(1,12,title,1,10).getFirst();
            assertNull(original.sigunguCode());
            dao.upsert(row(content,title,23,""));
            var updated=dao.find(original.id());
            assertEquals(original.id(),updated.id());assertEquals(23,updated.sigunguCode());assertEquals("overview",updated.description());
            assertThrows(SQLException.class,()->dao.upsertAll(List.of(row(content+1,title,null,""),row(content+2,title,999,""))));
            assertEquals(1,dao.search(1,12,title,1,10).size(),"Failed batch must leave neither row");
            var lookup=new LookupDao();
            assertEquals(17,lookup.sidos().size());assertEquals(8,lookup.contentTypes().size());
            assertTrue(lookup.guguns(1).stream().anyMatch(g->g.code()==23&&g.sidoCode()==1));
            assertTrue(lookup.guguns(8).stream().noneMatch(g->g.code()==23));
        } finally {
            try(Connection c=Database.open();var s=c.prepareStatement("DELETE FROM attractions WHERE content_id BETWEEN ? AND ?")){
                s.setLong(1,content);s.setLong(2,content+2);s.executeUpdate();
            }
        }
    }
    private AttractionDto row(long content,String title,Integer gugun,String description){
        return new AttractionDto(0,content,title,12,1,gugun,"Test address",description,37.5,126.9,"https://example.test/photo.jpg","20261106");
    }
}
