package io.bdrc.archives;

import java.io.IOException;
import java.io.Serializable;
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

import de.digitalcollections.iiif.myhymir.EHServerCache;
import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.iiif.resolver.IdentifierInfo;

public class ArchiveInfo implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 8907077264323301578L;

    public static Property PREF_LABEL = ResourceFactory.createProperty("http://www.w3.org/2004/02/skos/core#prefLabel");
    public static Property BIBLIO_NOTE = ResourceFactory.createProperty("http://purl.bdrc.io/ontology/core/workBiblioNote");
    public static Property CATALOG_INFO = ResourceFactory.createProperty("http://purl.bdrc.io/ontology/core/workCatalogInfo");
    public static Property NUM_VOLUMES = ResourceFactory.createProperty("http://purl.bdrc.io/ontology/core/workNumberOfVolumes");
    public static Property CREATOR = ResourceFactory.createProperty("http://purl.bdrc.io/ontology/core/creator");
    public static Property AGENT = ResourceFactory.createProperty("http://purl.bdrc.io/ontology/core/agent");
    public static Property PUBLISHER_LOC = ResourceFactory.createProperty("http://purl.bdrc.io/ontology/core/workPublisherLocation");
    public static Property PUBLISHER_NAME = ResourceFactory.createProperty("http://purl.bdrc.io/ontology/core/workPublisherName");

    IdentifierInfo inf;
    String biblioNote;
    String numVolumes;
    String catalogInfo;
    String publisherName;
    String publisherLocation;
    String author;
    String prefLabel;
    static HashMap<String, Integer> langOrder;

    private ArchiveInfo(IdentifierInfo inf) throws ClientProtocolException, IOException {
        super();
        this.inf = inf;

        langOrder = new HashMap<>();
        langOrder.put("bo-x-ewts", 0);
        langOrder.put("bo", 1);
        langOrder.put("en", 2);
        Model m = ModelFactory.createDefaultModel();
        m.read(inf.getWork() + ".ttl", "TURTLE");
        setBiblioNote(m);
        setNumVolumes(m);
        setCatalogInfo(m);
        setPublisherName(m);
        setPublisherLocation(m);
        setAuthor(m);
        setPrefLabel(m);
    }

    public String getBiblioNote() {
        return biblioNote;
    }

    public void setBiblioNote(String biblioNote) {
        this.biblioNote = biblioNote;
    }

    public String getNumVolumes() {
        return numVolumes;
    }

    public String getCatalogInfo() {
        return catalogInfo;
    }

    public String getPublisherName() {
        return publisherName;
    }

    public String getPublisherLocation() {
        return publisherLocation;
    }

    public String getAuthor() {
        return author;
    }

    public String getPrefLabel() {
        return prefLabel;
    }

    public static ArchiveInfo getInstance(IdentifierInfo inf) throws IIIFException, ClientProtocolException, IOException {
        ArchiveInfo info = EHServerCache.ARCHIVE_INFO.get(inf.getVolumeId());
        if (info == null) {
            info = new ArchiveInfo(inf);
            EHServerCache.ARCHIVE_INFO.put(inf.getVolumeId(), info);
        }
        return info;
    }

    private void setBiblioNote(Model m) {
        String workId = inf.getWork();
        String id = workId.substring(workId.lastIndexOf("/") + 1);
        String ret = "";
        NodeIterator ni = m.listObjectsOfProperty(ResourceFactory.createResource("http://purl.bdrc.io/resource/" + id), PREF_LABEL);
        if (ni.hasNext()) {
            RDFNode nd = ni.next();
            ret = nd.asLiteral().getString();
        }
        this.biblioNote = ret;
    }

    private void setNumVolumes(Model m) {
        String workId = inf.getWork();
        String id = workId.substring(workId.lastIndexOf("/") + 1);
        String ret = "";
        NodeIterator ni = m.listObjectsOfProperty(ResourceFactory.createResource("http://purl.bdrc.io/resource/" + id), NUM_VOLUMES);
        if (ni.hasNext()) {
            RDFNode nd = ni.next();
            ret = nd.asLiteral().getString();
        }
        this.numVolumes = ret;
    }

    private void setCatalogInfo(Model m) {
        String workId = inf.getWork();
        String id = workId.substring(workId.lastIndexOf("/") + 1);
        String ret = "";
        NodeIterator ni = m.listObjectsOfProperty(ResourceFactory.createResource("http://purl.bdrc.io/resource/" + id), CATALOG_INFO);
        if (ni.hasNext()) {
            RDFNode nd = ni.next();
            ret = nd.asLiteral().getString();
        }
        this.catalogInfo = ret;
    }

    private void setPublisherName(Model m) {
        String workId = inf.getWork();
        String id = workId.substring(workId.lastIndexOf("/") + 1);
        String ret = "";
        NodeIterator ni = m.listObjectsOfProperty(ResourceFactory.createResource("http://purl.bdrc.io/resource/" + id), PUBLISHER_NAME);
        if (ni.hasNext()) {
            RDFNode nd = ni.next();
            ret = nd.asLiteral().getString();
        }
        this.publisherName = ret;
    }

    private void setPublisherLocation(Model m) {
        String workId = inf.getWork();
        String id = workId.substring(workId.lastIndexOf("/") + 1);
        String ret = "";
        NodeIterator ni = m.listObjectsOfProperty(ResourceFactory.createResource("http://purl.bdrc.io/resource/" + id), PUBLISHER_LOC);
        if (ni.hasNext()) {
            RDFNode nd = ni.next();
            ret = nd.asLiteral().getString();
        }
        this.publisherLocation = ret;
        ;
    }

    private void setAuthor(Model m) {
        String workId = inf.getWork();
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
        this.author = ret;
        ;
    }

    private void setPrefLabel(Model m) throws ClientProtocolException, IOException {
        String workId = inf.getWork();
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
            this.prefLabel = ret;
        } else {
            this.prefLabel = tmp;
        }
    }

    public PDDocumentInformation getDocInformation() throws ClientProtocolException, IOException {
        PDDocumentInformation docInf = new PDDocumentInformation();
        docInf.setCreator("Buddhist Digital Resource Center");
        docInf.setCreationDate(Calendar.getInstance());
        docInf.setCustomMetadataValue("license", inf.getLicense());
        docInf.setCustomMetadataValue("URL", inf.getAsset());
        docInf.setCustomMetadataValue("Bibliographical note", getBiblioNote());
        docInf.setCustomMetadataValue("Catalog info", getCatalogInfo());
        docInf.setCustomMetadataValue("Number of volumes", getNumVolumes());
        docInf.setCustomMetadataValue("Volume number", Integer.toString(inf.getVolumeNumber()));
        docInf.setCustomMetadataValue("Publisher name", getPublisherName());
        docInf.setCustomMetadataValue("Publisher location", getPublisherLocation());
        docInf.setTitle(getPrefLabel());
        docInf.setAuthor(getAuthor());
        return docInf;
    }

}
