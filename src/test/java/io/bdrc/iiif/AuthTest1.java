package io.bdrc.iiif;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Properties;

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

import io.bdrc.auth.AuthProps;
import io.bdrc.auth.rdf.RdfAuthModel;
import io.bdrc.auth.rdf.RdfConstants;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class AuthTest1 {

	static String publicToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJodHRwczovL2Rldi1iZHJjLmF1dGgwLmNvbS9hcGkvdjIvIiwic3ViIjoiYXV0aDB8NWJlOTkyZDlkN2VjZTg3ZjE1OWM4YmVkIiwiYXpwIjoiRzBBam1DS3NwTm5nSnNUdFJuSGFBVUNENDRaeHdvTUoiLCJpc3MiOiJodHRwczovL2Rldi1iZHJjLmF1dGgwLmNvbS8iLCJleHAiOjE3MzU3MzgyNjR9.zqOALhi8Gz1io-B1pWIgHVvkSa0U6BuGmB18FnF3CIg\n";
	static String adminToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJodHRwczovL2Rldi1iZHJjLmF1dGgwLmNvbS9hcGkvdjIvIiwic3ViIjoiYXV0aDB8NWJlOTkyMGJlYzMxMjMyMGY1NjI5NGRjIiwiYXpwIjoiRzBBam1DS3NwTm5nSnNUdFJuSGFBVUNENDRaeHdvTUoiLCJpc3MiOiJodHRwczovL2Rldi1iZHJjLmF1dGgwLmNvbS8iLCJleHAiOjE3MzU3Mzc1OTB9.m1V64-90tjNRMD18RQTF8SBlMFOcqgSuPwtALZBLd8U";
	public static HashMap<String, String> map = new HashMap<>();
	public static final String PUBLIC_RES = "/test/v2/bdr:I1KG15042::I1KG150420322.jpg/full/full/0/default.jpg";
	public static final String RESTRICTED_CHINA = "/test/v2/bdr:I4644::46440001.tif/full/full/0/default.jpg";
	public static final String FAIR_USE = "/test/v2/bdr:I1PD96947::I1PD969470131.tif/full/full/0/default.jpg";

	@Autowired
	Environment environment;

	@BeforeClass
	public static void init() throws IOException {
		InputStream input = TestApplication.class.getClassLoader().getResourceAsStream("test.properties");
		Properties props = new Properties();
		props.load(input);
		AuthProps.init(props);
		RdfAuthModel.initForStaticTests();
		map.put(PUBLIC_RES, RdfConstants.OPEN);
		map.put(RESTRICTED_CHINA, RdfConstants.RESTRICTED_CHINA);
		map.put(FAIR_USE, RdfConstants.FAIR_USE);
	}

	@Test
	public void publicResource() throws ClientProtocolException, IOException, IllegalArgumentException, CertificateException, InvalidKeySpecException, NoSuchAlgorithmException {
		HttpClient client = HttpClientBuilder.create().build();
		HttpGet get = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + PUBLIC_RES);
		// get.addHeader("Authorization", "Bearer " + publicToken);
		HttpResponse resp = client.execute(get);
		System.out.println("STATUS >>> " + resp.getStatusLine());
		assert (resp.getStatusLine().getStatusCode() == 200);
	}

	@Test
	public void ChinaRestrictedResource() throws ClientProtocolException, IOException, IllegalArgumentException, CertificateException, InvalidKeySpecException, NoSuchAlgorithmException {
		// with public Token and authorized picture
		HttpClient client = HttpClientBuilder.create().build();
		HttpGet get = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + PUBLIC_RES);
		get.addHeader("Authorization", "Bearer " + publicToken);
		HttpResponse resp = client.execute(get);
		assert (resp.getStatusLine().getStatusCode() == 200);
		// with public Token and restricted picture
		client = HttpClientBuilder.create().build();
		get = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + RESTRICTED_CHINA);
		get.addHeader("Authorization", "Bearer " + publicToken);
		resp = client.execute(get);
		assert (resp.getStatusLine().getStatusCode() == 401);
		// with admin Token and restricted picture
		client = HttpClientBuilder.create().build();
		get = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + RESTRICTED_CHINA);
		get.addHeader("Authorization", "Bearer " + adminToken);
		resp = client.execute(get);
		assert (resp.getStatusLine().getStatusCode() == 200);
	}

}