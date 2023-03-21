package io.bdrc.iiif.image.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.iiif.core.Application;
import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.iiif.model.ContentLocation;
import io.bdrc.iiif.resolver.AppConstants;
import io.bdrc.libraries.Models;



public class ContentLocationService extends ConcurrentResourceService<List<ContentLocation>> {
    
    private static final Logger logger = LoggerFactory.getLogger(ContentLocationService.class);
    public static final ContentLocationService Instance = new ContentLocationService();
    
    static final Property pageStart = ResourceFactory.createProperty(AppConstants.BDO+"contentLocationPage");
    static final Property pageEnd = ResourceFactory.createProperty(AppConstants.BDO+"contentLocationEndPage");
    static final Property volumeStart = ResourceFactory.createProperty(AppConstants.BDO+"contentLocationVolume");
    static final Property volumeEnd = ResourceFactory.createProperty(AppConstants.BDO+"contentLocationEndVolume");
    static final Property clInstance = ResourceFactory.createProperty(AppConstants.BDO+"contentLocationInstance");
    
    final Integer getVal(final Resource s, final Property p) {
        Statement st = s.getProperty(p);
        if (st == null)
            return null;
        return st.getInt();
    }
    
    @Override
    public final List<ContentLocation> getFromApi(final String resQname) throws IIIFException {
        final String uri = Models.BDR + resQname.substring(4);
        final String sparqlStr = "construct { ?cls ?clp ?clo } where { <"+uri+"> <"+AppConstants.BDO+"contentLocation> ?cls . ?cls ?clp ?clo . }";
        final Query q = QueryFactory.create(sparqlStr);
        final QueryExecution qe = QueryExecution.service(Application.getProperty("fusekiUrl")).query(q).build();
        final Model clm = qe.execConstruct();
        if (clm.isEmpty()) {
            logger.info("no content location found for {}", resQname);
            qe.close();
            return null;
        }
        final List<ContentLocation> cll = new ArrayList<>();
        final ResIterator cli = clm.listSubjects();
        while (cli.hasNext()) {
            final Resource cl = cli.next();
            final Resource clInst = cl.getPropertyResourceValue(clInstance);
            final ContentLocation clo = new ContentLocation(clInst, getVal(cl, volumeStart), getVal(cl, volumeEnd), getVal(cl, pageStart), getVal(cl, pageEnd));
            cll.add(clo);
        }
        cll.sort(null);
        qe.close();
        return cll;
    }
    
    ContentLocationService() {
        super("contentlocation", AppConstants.CACHEPREFIX_CL);
    }

}
