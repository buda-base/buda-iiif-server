package io.bdrc.iiif.image.service;

import static io.bdrc.iiif.resolver.AppConstants.CACHEPREFIX_VI;
import static io.bdrc.iiif.resolver.AppConstants.LDS_VOLUME_QUERY;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.riot.ResultSetMgr;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.iiif.resolver.ImageGroupInfo;

public class ImageGroupInfoService extends ConcurrentResourceService<ImageGroupInfo> {

    private static final Logger logger = LoggerFactory.getLogger(ImageGroupInfoService.class);

    public static final ImageGroupInfoService Instance = new ImageGroupInfoService();

    ImageGroupInfoService() {
        super("imageGroupInfo", CACHEPREFIX_VI);
    }

    @Override
    final public ImageGroupInfo getFromApi(final String volumeId) throws IIIFException {
        logger.info("fetch volume info on LDS for {}", volumeId);
        final CloseableHttpClient httpClient = HttpClients.createDefault(); // Use this instead
        final ImageGroupInfo resVolumeInfo;
        CloseableHttpResponse response = null;
        InputStream body = null;
        try {
            URIBuilder builder = new URIBuilder(LDS_VOLUME_QUERY);
            builder.setParameter("R_RES", volumeId);
            builder.setParameter("format", "json");
            final HttpGet request = new HttpGet(builder.build());
            logger.info("LDS request for {}: {}", volumeId, request);
            // we suppose that the volumeId is well formed, which is checked by the
            // Identifier constructor
            response = httpClient.execute(request);
            int code = response.getStatusLine().getStatusCode();
            if (code != 200) {
                response.close();
                httpClient.close();
                throw new IIIFException(500, 5000, "LDS lookup returned an error for volume " + volumeId, response.toString(), "");
            }
            body = response.getEntity().getContent();
            final ResultSet res = ResultSetMgr.read(body, ResultSetLang.SPARQLResultSetJSON);
            if (!res.hasNext()) {
                body.close();
                response.close();
                httpClient.close();
                throw new IIIFException(404, 5000, "cannot find image group " + volumeId + " in the database");
            }
            final QuerySolution sol = res.next();
            resVolumeInfo = new ImageGroupInfo(sol, volumeId);
            if (res.hasNext()) {
                body.close();
                response.close();
                httpClient.close();
                throw new IIIFException(500, 5000, "more than one volume found in the database for " + volumeId + ", this shouldn't happen");
            }
        } catch (IOException | URISyntaxException ex) {
            try {
                if (body != null)
                    body.close();
                if (response != null)
                    response.close();
                if (httpClient != null)
                    httpClient.close();
            } catch (IOException e) {
                logger.error("error closing body: ", e);
            }
            throw new IIIFException(500, 5000, ex);
        }
        logger.info("found volume info: {}", resVolumeInfo);
        try {
            body.close();
            response.close();
            httpClient.close();
        } catch (IOException e) {
            logger.error("error closing body: ", e);
        }
        return resVolumeInfo;
    }
}
