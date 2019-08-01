package io.bdrc.archives;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;

import de.digitalcollections.iiif.myhymir.ServerCache;
import io.bdrc.iiif.exceptions.IIIFException;

public class PdfItemInfo {

    public String itemId;
    public List<String> itemVolumes;
    public HashMap<String, String> volNumbers;
    public String itemAccess;

    public static String ITEM_URL_ROOT = "http://purl.bdrc.io/query/graph/IIIFPres_itemGraph?R_RES=";
    public static String BDO = "http://purl.bdrc.io/ontology/core/";
    public static String BDR = "http://purl.bdrc.io/resource/";
    public static String BDA = "http://purl.bdrc.io/admindata/";
    public static String ADM = "http://purl.bdrc.io/ontology/admin/";
    public static String TMP = "http://purl.bdrc.io/ontology/tmp/";
    private Model itemModel;

    public static PdfItemInfo getPdfItemInfo(String itemId) throws IIIFException {

        PdfItemInfo meta = (PdfItemInfo) ServerCache.getObjectFromCache("info", itemId + "_PdfItemInfo");
        if (meta == null) {
            meta = new PdfItemInfo(itemId);
            ServerCache.addToCache("info", itemId + "_PdfItemInfo", meta);
        }
        return meta;
    }

    private PdfItemInfo(String itemId) {
        super();
        this.itemModel = ModelFactory.createDefaultModel();
        if (itemId != null) {
            this.itemId = itemId;
            itemModel.read(ITEM_URL_ROOT + itemId + "&format=ttl", "TURTLE");
            itemModel.write(System.out, "TURTLE");
        }
    }

    public String getItemAccess() {
        if (itemAccess == null) {
            NodeIterator ni = itemModel.listObjectsOfProperty(ResourceFactory.createProperty(ADM + "access"));
            itemAccess = ni.next().asNode().getURI();
            itemAccess = itemAccess.substring(itemAccess.lastIndexOf('/') + 1);
        }
        return itemAccess;
    }

    public List<String> getItemVolumes() {
        List<RDFNode> nodes;
        if (itemVolumes == null) {
            itemVolumes = new ArrayList<>();
            nodes = itemModel.listObjectsOfProperty(ResourceFactory.createProperty(BDO + "itemHasVolume")).toList();
            for (RDFNode nd : nodes) {
                itemVolumes.add(nd.asResource().getURI());
            }
        }
        return itemVolumes;
    }

    public HashMap<String, String> getItemVolumeNumbers() {
        if (volNumbers == null) {
            volNumbers = new HashMap<>();
            List<String> itemVols = getItemVolumes();
            for (String vol : itemVols) {
                String shortName = vol.substring(vol.lastIndexOf('/') + 1);
                String num = itemModel.listObjectsOfProperty(ResourceFactory.createResource(vol), ResourceFactory.createProperty(BDO + "volumeNumber")).next().asLiteral().getString();
                volNumbers.put(shortName, num);
            }
        }
        return volNumbers;
    }

    public String getItemVolumeNumber(String volumeId) {
        return getItemVolumeNumbers().get(volumeId);
    }

    @Override
    public String toString() {
        return "PdfItemInfo [itemId=" + itemId + ", itemVolumes=" + itemVolumes + ", volNumbers=" + volNumbers + ", itemAccess=" + itemAccess + ", itemModel=" + itemModel + "]";
    }

    public static void main(String[] args) throws MalformedURLException, IIIFException {
        PdfItemInfo pdf = PdfItemInfo.getPdfItemInfo("bdr:I23703");
        System.out.println(pdf.getItemVolumes());
        System.out.println(pdf.getItemVolumeNumbers());
        System.out.println(pdf.getItemVolumeNumber("V23703_I1529"));
        System.out.println(pdf.getItemAccess());
    }

}
