package io.bdrc.archives;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;

import org.apache.http.client.ClientProtocolException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;

import de.digitalcollections.iiif.hymir.model.exception.ResourceNotFoundException;
import de.digitalcollections.iiif.myhymir.ServerCache;
import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.models.Identifier;
import io.bdrc.iiif.resolver.IdentifierInfo;

public class ArchiveInfo {

    public static Property PREF_LABEL = ResourceFactory.createProperty("http://www.w3.org/2004/02/skos/core#prefLabel");

    IdentifierInfo inf;
    static HashMap<String, Integer> langOrder;

    private ArchiveInfo(IdentifierInfo inf) {
        super();
        this.inf = inf;
        langOrder = new HashMap<>();
        langOrder.put("bo-x-ewts", 0);
        langOrder.put("bo", 1);
        langOrder.put("en", 2);
    }

    public static ArchiveInfo getInstance(IdentifierInfo inf) throws BDRCAPIException {
        ArchiveInfo info = (ArchiveInfo) ServerCache.getObjectFromCache("default", inf.getIdentifier());
        if (info == null) {
            info = new ArchiveInfo(inf);
            ServerCache.addToCache("default", inf.getIdentifier(), info);
        }
        return info;
    }

    private String getPrefLabel() throws ClientProtocolException, IOException {
        System.out.println("Inf " + inf);
        String workId = inf.getWork();
        String id = workId.substring(workId.lastIndexOf("/") + 1);
        String ret = "";
        String tmp = "";
        Model m = ModelFactory.createDefaultModel();
        m.read(workId + ".ttl", "TURTLE");
        NodeIterator ni = m.listObjectsOfProperty(ResourceFactory.createResource("http://purl.bdrc.io/resource/" + id), PREF_LABEL);
        int min = 999;
        while (ni.hasNext()) {
            RDFNode nd = ni.next();
            tmp = nd.asLiteral().getString();
            System.out.println(nd.asLiteral().getLanguage());
            Integer i = langOrder.get(nd.asLiteral().getLanguage());
            if (i != null) {
                if (i.intValue() < min) {
                    ret = tmp;
                    min = i.intValue();
                }
            }
        }
        if (!ret.isEmpty()) {
            return ret;
        }
        return tmp;
    }

    public PDDocumentInformation getDocInformation() throws ClientProtocolException, IOException {
        PDDocumentInformation docInf = new PDDocumentInformation();
        docInf.setCreator("Buddhist Digital Resource Center");
        docInf.setCreationDate(Calendar.getInstance());
        docInf.setCustomMetadataValue("license", inf.getLicense());
        docInf.setCustomMetadataValue("URL", inf.getAsset());
        docInf.setTitle(getPrefLabel());
        return docInf;
    }

    public static void main(String[] args) throws ClientProtocolException, IOException, ResourceNotFoundException, BDRCAPIException {
        Identifier idf = new Identifier("v:bdr:V22084_I0900::1-10", Identifier.MANIFEST_ID);
        IdentifierInfo inf = IdentifierInfo.getIndentifierInfo(idf.getVolumeId());
        ArchiveInfo arinf = new ArchiveInfo(inf);
        System.out.println("PrefLabel : " + arinf.getPrefLabel());
    }

}
