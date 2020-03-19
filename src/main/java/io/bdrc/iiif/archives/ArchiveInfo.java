package io.bdrc.iiif.archives;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;

import org.apache.http.client.ClientProtocolException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;

import io.bdrc.iiif.core.Application;
import io.bdrc.iiif.core.EHServerCache;
import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.iiif.resolver.IdentifierInfo;

public class ArchiveInfo {

    public static Property PREF_LABEL = ResourceFactory.createProperty("http://www.w3.org/2004/02/skos/core#prefLabel");
    public static Property BIBLIO_NOTE = ResourceFactory.createProperty("http://purl.bdrc.io/ontology/core/workBiblioNote");
    public static Property CATALOG_INFO = ResourceFactory.createProperty("http://purl.bdrc.io/ontology/core/workCatalogInfo");
    public static Property NUM_VOLUMES = ResourceFactory.createProperty("http://purl.bdrc.io/ontology/core/workNumberOfVolumes");
    public static Property CREATOR = ResourceFactory.createProperty("http://purl.bdrc.io/ontology/core/creator");
    public static Property AGENT = ResourceFactory.createProperty("http://purl.bdrc.io/ontology/core/agent");
    public static Property PUBLISHER_LOC = ResourceFactory.createProperty("http://purl.bdrc.io/ontology/core/workPublisherLocation");
    public static Property PUBLISHER_NAME = ResourceFactory.createProperty("http://purl.bdrc.io/ontology/core/workPublisherName");

    IdentifierInfo inf;
    Model m;
    static HashMap<String, Integer> langOrder;

    private ArchiveInfo(IdentifierInfo inf) {
        super();
        this.inf = inf;

        langOrder = new HashMap<>();
        langOrder.put("bo-x-ewts", 0);
        langOrder.put("bo", 1);
        langOrder.put("en", 2);
        this.m = ModelFactory.createDefaultModel();
        // this is a trick we should not need (i.e the resource is prefixed by
        // purl.bdrc.io)
        String name = inf.igi.instanceId;
        name = name.substring(name.lastIndexOf("/") + 1);
        m.read(Application.getProperty("dataserver") + "resource/" + name + ".ttl", "TURTLE");
    }

    public static ArchiveInfo getInstance(IdentifierInfo inf) throws IIIFException {
        ArchiveInfo info = (ArchiveInfo) EHServerCache.ARCHIVE_INFO.get(inf.volumeId);
        if (info == null) {
            info = new ArchiveInfo(inf);
            EHServerCache.ARCHIVE_INFO.put(inf.volumeId, info);
        }
        return info;
    }

    private String getBiblioNote() {
        String workId = inf.igi.instanceId;
        String id = workId.substring(workId.lastIndexOf("/") + 1);
        String ret = "";
        NodeIterator ni = m.listObjectsOfProperty(ResourceFactory.createResource("http://purl.bdrc.io/resource/" + id), PREF_LABEL);
        if (ni.hasNext()) {
            RDFNode nd = ni.next();
            ret = nd.asLiteral().getString();
        }
        return ret;
    }

    private String getNumVolumes() {
        String workId = inf.igi.instanceId;
        String id = workId.substring(workId.lastIndexOf("/") + 1);
        String ret = "";
        NodeIterator ni = m.listObjectsOfProperty(ResourceFactory.createResource("http://purl.bdrc.io/resource/" + id), NUM_VOLUMES);
        if (ni.hasNext()) {
            RDFNode nd = ni.next();
            ret = nd.asLiteral().getString();
        }
        return ret;
    }

    private String getCatalogInfo() {
        String workId = inf.igi.instanceId;
        String id = workId.substring(workId.lastIndexOf("/") + 1);
        String ret = "";
        NodeIterator ni = m.listObjectsOfProperty(ResourceFactory.createResource("http://purl.bdrc.io/resource/" + id), CATALOG_INFO);
        if (ni.hasNext()) {
            RDFNode nd = ni.next();
            ret = nd.asLiteral().getString();
        }
        return ret;
    }

    private String getPublisherName() {
        String workId = inf.igi.instanceId;
        String id = workId.substring(workId.lastIndexOf("/") + 1);
        String ret = "";
        NodeIterator ni = m.listObjectsOfProperty(ResourceFactory.createResource("http://purl.bdrc.io/resource/" + id), PUBLISHER_NAME);
        if (ni.hasNext()) {
            RDFNode nd = ni.next();
            ret = nd.asLiteral().getString();
        }
        return ret;
    }

    private String getPublisherLocation() {
        String workId = inf.igi.instanceId;
        String id = workId.substring(workId.lastIndexOf("/") + 1);
        String ret = "";
        NodeIterator ni = m.listObjectsOfProperty(ResourceFactory.createResource("http://purl.bdrc.io/resource/" + id), PUBLISHER_LOC);
        if (ni.hasNext()) {
            RDFNode nd = ni.next();
            ret = nd.asLiteral().getString();
        }
        return ret;
    }

    private String getAuthor() {
        String workId = inf.igi.instanceId;
        String id = workId.substring(workId.lastIndexOf("/") + 1);
        String ret = "";
        Resource agent = null;
        NodeIterator ni = m.listObjectsOfProperty(ResourceFactory.createResource("http://purl.bdrc.io/resource/" + id), CREATOR);
        if (ni.hasNext()) {
            RDFNode nd = ni.next();
            Resource creator = nd.asResource();
            NodeIterator n = m.listObjectsOfProperty(creator, AGENT);
            if (n.hasNext()) {
                RDFNode nd1 = n.next();
                agent = nd1.asResource();
            }
        }
        if (agent != null) {
            Model md = ModelFactory.createDefaultModel();
            md.read(agent.getURI() + ".ttl", "TURTLE");
            NodeIterator n2 = md.listObjectsOfProperty(agent, PREF_LABEL);
            if (n2.hasNext()) {
                RDFNode nd = n2.next();
                ret = nd.asLiteral().getString();
            }
        }
        return ret;
    }

    private String getPrefLabel() throws ClientProtocolException, IOException {
        String workId = inf.igi.instanceId;
        String id = workId.substring(workId.lastIndexOf("/") + 1);
        String ret = "";
        String tmp = "";
        NodeIterator ni = m.listObjectsOfProperty(ResourceFactory.createResource("http://purl.bdrc.io/resource/" + id), PREF_LABEL);
        int min = 999;
        while (ni.hasNext()) {
            RDFNode nd = ni.next();
            tmp = nd.asLiteral().getString();
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
        // TODO
        // docInf.setCustomMetadataValue("license", inf.getLicense());
        // docInf.setCustomMetadataValue("URL", inf.getAsset());
        docInf.setCustomMetadataValue("Bibliographical note", getBiblioNote());
        docInf.setCustomMetadataValue("Catalog info", getCatalogInfo());
        docInf.setCustomMetadataValue("Number of volumes", getNumVolumes());
        docInf.setCustomMetadataValue("Volume number", Integer.toString(inf.igi.volumeNumber));
        docInf.setCustomMetadataValue("Publisher name", getPublisherName());
        docInf.setCustomMetadataValue("Publisher location", getPublisherLocation());
        docInf.setTitle(getPrefLabel());
        docInf.setAuthor(getAuthor());
        return docInf;
    }

}
