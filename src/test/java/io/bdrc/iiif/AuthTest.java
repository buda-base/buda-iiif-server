package io.bdrc.iiif;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Properties;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.digitalcollections.iiif.myhymir.backend.impl.repository.S3ResourceRepositoryImpl;
import io.bdrc.auth.AuthProps;
import io.bdrc.auth.rdf.RdfAuthModel;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class,webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class AuthTest {


    static String publicToken="eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJodHRwczovL2Rldi1iZHJjLmF1dGgwLmNvbS9hcGkvdjIvIiwic3ViIjoiYXV0aDB8NWJlOTkyZDlkN2VjZTg3ZjE1OWM4YmVkIiwiYXpwIjoiRzBBam1DS3NwTm5nSnNUdFJuSGFBVUNENDRaeHdvTUoiLCJpc3MiOiJodHRwczovL2Rldi1iZHJjLmF1dGgwLmNvbS8iLCJleHAiOjE3MzU3MzgyNjR9.zqOALhi8Gz1io-B1pWIgHVvkSa0U6BuGmB18FnF3CIg\n";
    static String adminToken="eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJodHRwczovL2Rldi1iZHJjLmF1dGgwLmNvbS9hcGkvdjIvIiwic3ViIjoiYXV0aDB8NWJlOTkyMGJlYzMxMjMyMGY1NjI5NGRjIiwiYXpwIjoiRzBBam1DS3NwTm5nSnNUdFJuSGFBVUNENDRaeHdvTUoiLCJpc3MiOiJodHRwczovL2Rldi1iZHJjLmF1dGgwLmNvbS8iLCJleHAiOjE3MzU3Mzc1OTB9.m1V64-90tjNRMD18RQTF8SBlMFOcqgSuPwtALZBLd8U";

    @Autowired
    Environment environment;

    @BeforeClass
    public static void init() throws IOException {
        InputStream input=TestApplication.class.getClassLoader().getResourceAsStream("test.properties");
        Properties props=new Properties();
        props.load(input);
        AuthProps.init(props);
        S3ResourceRepositoryImpl.initWithProps(props);
        RdfAuthModel.initForStaticTests();
    }

    //Cannot be tested as selfcontained for now because token validation occurs at the endpoint level
    //in the ImageController (i.e after request filtering)
    /*@Test
    public void ValidTokenForCookieRequestTest() throws ClientProtocolException, IOException, IllegalArgumentException, CertificateException, InvalidKeySpecException, NoSuchAlgorithmException {
        ObjectMapper mapper=new ObjectMapper();
        HttpClient client=HttpClientBuilder.create().build();
        HttpGet get=new HttpGet("http://localhost:"+environment.getProperty("local.server.port")+"/image/v2/setcookie");
        get.addHeader("Authorization", "Bearer "+publicToken);
        HttpResponse resp=client.execute(get);
        System.out.println("STATUS >>> "+resp.getStatusLine());
        assert(resp.getStatusLine().getStatusCode()==200);
        Header[] h=resp.getHeaders("Set-Cookie");
        assert(h!=null);
        HttpEntity ent=resp.getEntity();
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        ent.writeTo(baos);
        String json_resp=baos.toString();
        JsonNode node=mapper.readTree(json_resp);
        baos.close();
        assert(node.findValue("success").asBoolean());
    }*/

    @Test
    public void publicResource() throws ClientProtocolException, IOException, IllegalArgumentException, CertificateException, InvalidKeySpecException, NoSuchAlgorithmException {
        HttpClient client=HttpClientBuilder.create().build();
        HttpGet get=new HttpGet("http://localhost:"+environment.getProperty("local.server.port")+"/image/v2/bdr:V29329_I1KG15042::I1KG150420322.jpg/full/full/0/default.jpg");
        //get.addHeader("Authorization", "Bearer "+publicToken);
        HttpResponse resp=client.execute(get);
        System.out.println("STATUS >>> "+resp.getStatusLine());
        assert(resp.getStatusLine().getStatusCode()==200);
    }


    @Test
    public void ChinaRestrictedResource() throws ClientProtocolException, IOException, IllegalArgumentException, CertificateException, InvalidKeySpecException, NoSuchAlgorithmException {
        //with public Token and authorized picture
        HttpClient client=HttpClientBuilder.create().build();
        HttpGet get=new HttpGet("http://localhost:"+environment.getProperty("local.server.port")+"/image/v2/bdr:V1PD96945_I1PD96947::I1PD969470657.tif/full/full/0/default.jpg");
        get.addHeader("Authorization", "Bearer "+publicToken);
        HttpResponse resp=client.execute(get);
        assert(resp.getStatusLine().getStatusCode()==200);
        //with public Token and restricted picture
        client=HttpClientBuilder.create().build();
        get=new HttpGet("http://localhost:"+environment.getProperty("local.server.port")+"/image/v2/bdr:V28810_I4644::46440001.tif/full/full/0/default.jpg");
        get.addHeader("Authorization", "Bearer "+publicToken);
        resp=client.execute(get);
        assert(resp.getStatusLine().getStatusCode()==401);
        //with admin Token and restricted picture
        client=HttpClientBuilder.create().build();
        get=new HttpGet("http://localhost:"+environment.getProperty("local.server.port")+"/image/v2/bdr:V28810_I4644::46440001.tif/full/full/0/default.jpg");
        get.addHeader("Authorization", "Bearer "+adminToken);
        resp=client.execute(get);
        assert(resp.getStatusLine().getStatusCode()==200);
    }

    @Test
    public void fairUseResource() throws ClientProtocolException, IOException, IllegalArgumentException, CertificateException, InvalidKeySpecException, NoSuchAlgorithmException {
        //with public Token and authorized picture
        HttpClient client=HttpClientBuilder.create().build();
        HttpGet get=new HttpGet("http://localhost:"+environment.getProperty("local.server.port")+"/image/v2/bdr:V1PD96945_I1PD96947::I1PD969470001.tif/full/full/0/default.jpg");
        get.addHeader("Authorization", "Bearer "+publicToken);
        HttpResponse resp=client.execute(get);
        assert(resp.getStatusLine().getStatusCode()==200);
        //with public Token and restricted picture
        client=HttpClientBuilder.create().build();
        get=new HttpGet("http://localhost:"+environment.getProperty("local.server.port")+"/image/v2/bdr:V1PD96945_I1PD96947::I1PD969470131.tif/full/full/0/default.jpg");
        get.addHeader("Authorization", "Bearer "+publicToken);
        resp=client.execute(get);
        assert(resp.getStatusLine().getStatusCode()==401);
        //with admin Token and restricted picture
        client=HttpClientBuilder.create().build();
        get=new HttpGet("http://localhost:"+environment.getProperty("local.server.port")+"/image/v2/bdr:V1PD96945_I1PD96947::I1PD969470131.tif/full/full/0/default.jpg");
        get.addHeader("Authorization", "Bearer "+adminToken);
        resp=client.execute(get);
        assert(resp.getStatusLine().getStatusCode()==200);
    }

    @Test
    public void serviceInfoTest() throws ClientProtocolException, IOException, IllegalArgumentException, CertificateException, InvalidKeySpecException, NoSuchAlgorithmException {
        //with public Token and restricted picture
        ObjectMapper mapper=new ObjectMapper();
        HttpClient client=HttpClientBuilder.create().build();
        HttpGet get=new HttpGet("http://localhost:"+environment.getProperty("local.server.port")+"/image/v2/bdr:V28810_I4644::46440001.tif/info.json");
        get.addHeader("Authorization", "Bearer "+publicToken);
        HttpResponse resp=client.execute(get);
        assert(resp.getStatusLine().getStatusCode()==401);
        HttpEntity ent=resp.getEntity();
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        ent.writeTo(baos);
        String json_resp=baos.toString();
        JsonNode node=mapper.readTree(json_resp);
        baos.close();
        assert(node.at("/service/profile").asText().equals("http://iiif.io/api/auth/1/login"));
    }
}
