package io.bdrc.iiif.image.service;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.net.LoggingEventPreSerializationTransformer;
import io.bdrc.iiif.core.Application;
import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.iiif.resolver.AppConstants;
import io.bdrc.libraries.Models;

public class ThumbnailService extends ConcurrentResourceService<String> {
    private static final Logger logger = LoggerFactory.getLogger(ThumbnailService.class);
    public static final ThumbnailService Instance = new ThumbnailService();
    
    @Override
    public final String getFromApi(final String resQname) throws IIIFException {
        final String uri = Models.BDR + resQname.substring(4);
        final String sparqlStr = "select ?th where { <"+uri+"> <http://purl.bdrc.io/ontology/tmp/thumbnailIIIFService> ?th }";
        final Query q = QueryFactory.create(sparqlStr);
        final QueryExecution qe = QueryExecution.service(Application.getProperty("fusekiUrl")).query(q).build();
        final ResultSet qs = qe.execSelect();
        if (!qs.hasNext()) {
            logger.info("no thumbnail found for {}", resQname);
            qe.close();
            return null;
        }
        final Resource th = qs.next().getResource("th");
        if (qs.hasNext())
            logger.error("multiple thumbnails for {}", resQname);
        qe.close();
        return th.getURI();
    }
    
    ThumbnailService() {
        super("thumbnail", AppConstants.CACHEPREFIX_THUMBNAIL);
    }
}
