package io.bdrc.iiif;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Properties;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringRunner;

import com.auth0.client.auth.AuthAPI;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.auth.TokenHolder;
import com.auth0.net.AuthRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.digitalcollections.iiif.myhymir.Application;
import de.digitalcollections.iiif.myhymir.backend.impl.repository.S3ResourceRepositoryImpl;
import io.bdrc.auth.AuthProps;
import io.bdrc.auth.rdf.RdfAuthModel;

@RunWith(SpringRunner.class)
//@SpringBootTest(classes = Application.class,webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@SpringBootTest(classes = Application.class,webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class AuthTest {

    static AuthAPI auth;
    static String token;
    static String publicToken;
    static String adminToken;

    @Autowired
    Environment environment;

    @BeforeClass
    public static void init() throws IOException {
        InputStream is=new FileInputStream("/etc/buda/iiifserv/iiifservTest.properties");
        Properties props=new Properties();
        props.load(is);
        AuthProps.init(props);
        S3ResourceRepositoryImpl.initWithProps(props);
        auth = new AuthAPI(AuthProps.getProperty("authAPI"), AuthProps.getProperty("lds-pdiClientID"), AuthProps.getProperty("lds-pdiClientSecret"));
        HttpClient client=HttpClientBuilder.create().build();
        HttpPost post=new HttpPost(AuthProps.getProperty("issuer")+"oauth/token");
        HashMap<String,String> json = new HashMap<>();
        json.put("grant_type","client_credentials");
        json.put("client_id",AuthProps.getProperty("lds-pdiClientID"));
        json.put("client_secret",AuthProps.getProperty("lds-pdiClientSecret"));
        //json.put("audience","urn:auth0-authz-api");
        json.put("audience","https://dev-bdrc.auth0.com/api/v2/");
        ObjectMapper mapper=new ObjectMapper();
        String post_data=mapper.writer().writeValueAsString(json);
        StringEntity se = new StringEntity(post_data);
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
        post.setEntity(se);
        HttpResponse response = client.execute(post);
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        response.getEntity().writeTo(baos);
        String json_resp=baos.toString();
        baos.close();
        JsonNode node=mapper.readTree(json_resp);
        token=node.findValue("access_token").asText();
        RdfAuthModel.initForTest(false);
        setTokens();
    }

    private static void setTokens() throws Auth0Exception {
        AuthRequest req=auth.login("admin@bdrc-test.com", AuthProps.getProperty("admin@bdrc-test.com"));
        req.setScope("openid offline_access");
        TokenHolder holder=req.execute();
        adminToken=holder.getIdToken();
        req=auth.login("public@bdrc-test.com", AuthProps.getProperty("public@bdrc-test.com"));
        req.setScope("openid offline_access");
        holder=req.execute();
        publicToken=holder.getIdToken();
    }

    @Test
    public void ValidTokenForCookieRequestTest() throws ClientProtocolException, IOException, IllegalArgumentException, CertificateException, InvalidKeySpecException, NoSuchAlgorithmException {
        ObjectMapper mapper=new ObjectMapper();
        HttpClient client=HttpClientBuilder.create().build();
        HttpGet get=new HttpGet("http://localhost:"+environment.getProperty("local.server.port")+"/image/v2/setcookie");
        get.addHeader("Authorization", "Bearer "+token);
        HttpResponse resp=client.execute(get);
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
    }

    @Test
    public void publicResource() throws ClientProtocolException, IOException, IllegalArgumentException, CertificateException, InvalidKeySpecException, NoSuchAlgorithmException {
        HttpClient client=HttpClientBuilder.create().build();
        HttpGet get=new HttpGet("http://localhost:"+environment.getProperty("local.server.port")+"/image/v2/bdr:V29329_I1KG15042::I1KG150420325.jpg/full/full/0/default.jpg");
        get.addHeader("Authorization", "Bearer "+publicToken);
        HttpResponse resp=client.execute(get);
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
}
