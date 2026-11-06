package kr.co.enjoytrip.external;

import static org.junit.jupiter.api.Assertions.*;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

class TourApiClientTest {
    private TourApiClient.Page page(String gugun,String lat,String lng) throws Exception {
        String xml="<response><header><resultCode>0000</resultCode></header><body><totalCount>1</totalCount><items><item>"+
                "<contentid>123</contentid><contenttypeid>12</contenttypeid><areacode>1</areacode><title>Place</title>"+
                "<sigungucode>"+gugun+"</sigungucode><mapy>"+lat+"</mapy><mapx>"+lng+"</mapx></item></items></body></response>";
        var doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        return new TourApiClient().parsePage(doc);
    }
    @Test void preservesGugunAndCoordinateOrder() throws Exception {
        var a=page("23","37.5","126.9").attractions().getFirst();
        assertEquals(23,a.sigunguCode());assertEquals(37.5,a.latitude());assertEquals(126.9,a.longitude());
    }
    @Test void treatsEmptyOrZeroGugunAndInvalidCoordinatePairAsMissing() throws Exception {
        assertNull(page("","0","126.9").attractions().getFirst().latitude());
        assertNull(page("0","NaN","126.9").attractions().getFirst().longitude());
        assertNull(page("0","37.5","126.9").attractions().getFirst().sigunguCode());
        assertThrows(IllegalStateException.class,()->page("-1","37.5","126.9"));
        assertThrows(NumberFormatException.class,()->page("unknown","37.5","126.9"));
    }
}
