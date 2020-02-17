package io.bdrc.iiif.resolver;

import static io.bdrc.iiif.resolver.AppConstants.CACHEPREFIX_VI;
import static io.bdrc.iiif.resolver.AppConstants.LDS_VOLUME_QUERY;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.riot.ResultSetMgr;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.iiif.exceptions.IIIFException;

public class ImageGroupInfoService extends ConcurrentResourceService<ImageGroupInfo> {

	private static final Logger logger = LoggerFactory.getLogger(ImageGroupInfoService.class);

	public static final ImageGroupInfoService Instance = new ImageGroupInfoService();

	ImageGroupInfoService() {
		super("info", CACHEPREFIX_VI);
	}

	@Override
	final public ImageGroupInfo getFromApi(final String volumeId) throws IIIFException {
		logger.info("fetch volume info on LDS for {}", volumeId);
		final HttpClient httpClient = HttpClientBuilder.create().build(); // Use this instead
		final ImageGroupInfo resVolumeInfo;
		try {
			final HttpPost request = new HttpPost(LDS_VOLUME_QUERY);
			// we suppose that the volumeId is well formed, which is checked by the
			// Identifier constructor
			final StringEntity params = new StringEntity("{\"R_RES\":\"" + volumeId + "\"}", ContentType.APPLICATION_JSON);
			// request.addHeader(HttpHeaders.ACCEPT, "application/json");
			request.setEntity(params);
			final HttpResponse response = httpClient.execute(request);
			int code = response.getStatusLine().getStatusCode();
			if (code != 200) {
				throw new IIIFException(500, 5000, "LDS lookup returned an error for volume " + volumeId, response.toString(), "");
			}
			final InputStream body = response.getEntity().getContent();
			final ResultSet res = ResultSetMgr.read(body, ResultSetLang.SPARQLResultSetJSON);
			if (!res.hasNext()) {
				throw new IIIFException(404, 5000, "cannot find image group " + volumeId + " in the database");
			}
			final QuerySolution sol = res.next();
			resVolumeInfo = new ImageGroupInfo(sol);
			if (res.hasNext()) {
				throw new IIIFException(500, 5000, "more than one volume found in the database for " + volumeId + ", this shouldn't happen");
			}
		} catch (IOException ex) {
			throw new IIIFException(500, 5000, ex);
		}
		logger.info("found volume info: {}", resVolumeInfo);
		return resVolumeInfo;
	}
}
