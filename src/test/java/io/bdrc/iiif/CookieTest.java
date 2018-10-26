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
import com.auth0.json.auth.TokenHolder;
import com.auth0.net.AuthRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.digitalcollections.iiif.myhymir.Application;
import io.bdrc.auth.AuthProps;
import io.bdrc.auth.rdf.RdfAuthModel;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class,webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CookieTest {

    static AuthAPI auth;
    static String token;

    @Autowired
    Environment environment;

    @BeforeClass
    public static void init() throws IOException {
        InputStream is=new FileInputStream("/etc/buda/iiifserv/iiifserv.properties");
        Properties props=new Properties();
        props.load(is);
        AuthProps.init(props);
        auth = new AuthAPI("bdrc-io.auth0.com", AuthProps.getProperty("lds-pdiClientID"), AuthProps.getProperty("lds-pdiClientSecret"));
        HttpClient client=HttpClientBuilder.create().build();
        HttpPost post=new HttpPost("https://bdrc-io.auth0.com/oauth/token");
        HashMap<String,String> json = new HashMap<>();
        json.put("grant_type","client_credentials");
        json.put("client_id",AuthProps.getProperty("lds-pdiClientID"));
        json.put("client_secret",AuthProps.getProperty("lds-pdiClientSecret"));
        json.put("audience","https://bdrc-io.auth0.com/api/v2/");
        ObjectMapper mapper=new ObjectMapper();
        String post_data=mapper.writer().writeValueAsString(json);
        StringEntity se = new StringEntity(post_data);
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
        post.setEntity(se);
        HttpResponse response = client.execute(post);
        //System.out.println("Post_DATA >> "+post_data);
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        response.getEntity().writeTo(baos);
        String json_resp=baos.toString();
        baos.close();
        JsonNode node=mapper.readTree(json_resp);
        token=node.findValue("access_token").asText();
        RdfAuthModel.init();
    }

    @Test
    public void ValidTokenTest() throws ClientProtocolException, IOException, IllegalArgumentException, CertificateException, InvalidKeySpecException, NoSuchAlgorithmException {
        ObjectMapper mapper=new ObjectMapper();
        AuthRequest req=auth.login("tchame@rimay.net", AuthProps.getProperty("tchame@rimay.net"));
        req.setScope("openid offline_access user_metadata app_metadata");
        TokenHolder holder=req.execute();
        String tok=holder.getIdToken();
        HttpClient client=HttpClientBuilder.create().build();
        HttpGet get=new HttpGet("http://localhost:"+environment.getProperty("local.server.port")+"/image/v2/setcookie");
        get.addHeader("Authorization", "Bearer "+tok);
        System.out.println("GET Header >>" +get.getFirstHeader("Authorization").getValue());
        HttpResponse resp=client.execute(get);
        assert(resp.getStatusLine().getStatusCode()==200);
        Header[] h=resp.getHeaders("Set-Cookie");
        assert(h!=null);
        for(Header hd:h) {
            System.out.println("Set cookie Headers >>" +hd);
        }
        HttpEntity ent=resp.getEntity();
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        ent.writeTo(baos);
        String json_resp=baos.toString();
        JsonNode node=mapper.readTree(json_resp);
        baos.close();
        System.out.println("Success Value >>" +node.findValue("success").asBoolean());
        assert(node.findValue("success").asBoolean());
    }
}
