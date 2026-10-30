package kr.co.enjoytrip.external;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class XmlHttpClient {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public Document get(String url) throws Exception {
        HttpRequest req=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(12)).header("User-Agent","EnjoyTrip/1.0").GET().build();
        HttpResponse<byte[]> res=client.send(req,HttpResponse.BodyHandlers.ofByteArray());
        if(res.statusCode()!=200 || res.body().length>10_000_000) throw new IOException("External service unavailable: HTTP "+res.statusCode());
        DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities",false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,true);
        factory.setXIncludeAware(false); factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(new java.io.ByteArrayInputStream(res.body()));
    }

    public static String field(Element element,String name) {
        NodeList nodes=element.getElementsByTagName(name);
        return nodes.getLength()==0?"":nodes.item(0).getTextContent().trim();
    }
}
