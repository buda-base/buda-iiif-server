package io.bdrc.iiif.archives;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.iiif.core.Application;
import io.bdrc.iiif.core.EHServerCache;
import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.libraries.Models;

public class PdfItemInfo {

    private static final Logger log = LoggerFactory.getLogger(PdfItemInfo.class);

    public String iinstanceQname;
    public List<String> volumes;
    public HashMap<String, Integer> volNumbers;
    public String access;
    public String status;

    public static String ITEM_URL_ROOT = Application.getProperty("dataserver") + "query/graph/IIIFPres_imageInstanceGraph?R_RES=";
    private Model model;

    public static PdfItemInfo getPdfItemInfo(String iinstanceQname) throws IIIFException {
        PdfItemInfo meta = (PdfItemInfo) EHServerCache.PDF_ITEM_INFO.get(iinstanceQname + "_PdfItemInfo");
        if (meta == null) {
            meta = new PdfItemInfo(iinstanceQname);
            EHServerCache.PDF_ITEM_INFO.put(iinstanceQname + "_PdfItemInfo", meta);
        }
        return meta;
    }

    private PdfItemInfo(String iinstanceQname) {
        super();
        this.model = ModelFactory.createDefaultModel();
        if (iinstanceQname != null) {
            this.iinstanceQname = iinstanceQname;
            log.info("PDF Info Url {}", ITEM_URL_ROOT + iinstanceQname + "&format=ttl");
            model.read(ITEM_URL_ROOT + iinstanceQname + "&format=ttl", "TURTLE");
        }
    }

    public static final Property admAccess = ResourceFactory.createProperty(Models.ADM+"access");
    public String getAccessLName() {
        if (access == null) {
            NodeIterator ni = model.listObjectsOfProperty(admAccess);
            access = ni.next().asResource().getLocalName();
        }
        return access;
    }
    
    public static final Property admStatus = ResourceFactory.createProperty(Models.ADM+"status");
    public String getStatusLName() {
        if (status == null) {
            NodeIterator ni = model.listObjectsOfProperty(admStatus);
            status = ni.next().asResource().getLocalName();
        }
        return status;
    }

    static final Property volumerNumberP = ResourceFactory.createProperty(Models.BDO + "volumeNumber"); 
    public List<String> getVolumes() {
        if (this.volumes == null) {
            if (this.volNumbers == null)
                this.volNumbers = new HashMap<>();
            this.volumes = new ArrayList<>();
            final StmtIterator stmti = model.listStatements(null, volumerNumberP, (RDFNode) null);
            while (stmti.hasNext()) {
                final Statement st = stmti.next();
                final String uri = st.getSubject().getURI();
                this.volumes.add(uri);
                final int volnum = st.getInt();
                this.volNumbers.put(uri, volnum);
            }
            this.volumes.sort(
                    (final String volUri1, final String volUri2) -> this.volNumbers.get(volUri1).compareTo(this.volNumbers.get(volUri2)));
        }
        return this.volumes;
    }

    public HashMap<String, Integer> getVolumeNumbers() {
        if (this.volNumbers == null) {
            getVolumes();
        }
        return this.volNumbers;
    }

    public Integer getVolumeNumber(String volumeId) {
        return getVolumeNumbers().get(volumeId);
    }

    @Override
    public String toString() {
        return "PdfItemInfo [itemId=" + iinstanceQname + ", itemVolumes=" + volumes + ", volNumbers=" + volNumbers + ", itemAccess=" + access
                + ", itemModel=" + model + "]";
    }

    public static void main(String[] args) throws MalformedURLException, IIIFException {
        PdfItemInfo pdf = PdfItemInfo.getPdfItemInfo("bdr:I23703");
        System.out.println(pdf.getVolumes());
        System.out.println(pdf.getVolumeNumbers());
        System.out.println(pdf.getVolumeNumber("V23703_I1529"));
        System.out.println(pdf.getAccessLName());
    }

}
