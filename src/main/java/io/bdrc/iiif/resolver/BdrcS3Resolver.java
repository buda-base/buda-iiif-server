package io.bdrc.iiif.resolver;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Hex;
import org.apache.http.client.ClientProtocolException;

import de.digitalcollections.core.backend.impl.file.repository.resource.resolver.S3Resolver;
import de.digitalcollections.iiif.myhymir.Application;
import de.digitalcollections.model.api.identifiable.resource.exceptions.ResourceNotFoundException;
import io.bdrc.iiif.exceptions.IIIFException;

public class BdrcS3Resolver implements S3Resolver {

    private static final Pattern oldImageGroupPattern = Pattern.compile("^I\\d{4}$");

    @Override
    public String getS3Identifier(String identifier) throws ResourceNotFoundException, ClientProtocolException, IOException, IIIFException {
        String id = "";
        String[] parts = identifier.split("::");
        if (parts.length == 2 && parts[0].equals("static")) {
            return parts[1].trim();
        }
        id = "Works/";
        IdentifierInfo info = new IdentifierInfo(identifier);
        String w_id = info.igi.imageInstanceId.substring(info.igi.imageInstanceId.lastIndexOf('/') + 1);
        // String imgGroup = parts[0].substring(parts[0].lastIndexOf('_') + 1);
        String i_id = info.igi.imageGroup;
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IIIFException(e);
        }
        md.reset();
        md.update(w_id.getBytes(Charset.forName("UTF8")));
        final String hash = new String(Hex.encodeHex(md.digest())).substring(0, 2);
        if (oldImageGroupPattern.matcher(i_id).matches()) {
            i_id = i_id.substring(1);
        }
        id = id + hash + "/" + w_id + "/images/" + w_id + "-" + i_id + "/" + parts[1];
        return id;
    }

    public static void main(String[] args) throws IOException, ResourceNotFoundException, IIIFException {
        Application.initForTests();
        BdrcS3Resolver dr = new BdrcS3Resolver();
        System.out.println(dr.getS3Identifier("bdr:V1NLM7_I1NLM7_001::I1NLM7_0010003.jpg"));
    }

}
