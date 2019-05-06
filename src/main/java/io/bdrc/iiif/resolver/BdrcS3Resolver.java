package io.bdrc.iiif.resolver;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.digitalcollections.core.backend.impl.file.repository.resource.resolver.S3Resolver;
import de.digitalcollections.core.model.api.resource.exceptions.ResourceIOException;
import de.digitalcollections.iiif.myhymir.Application;

public class BdrcS3Resolver implements S3Resolver {

    private static final Logger log = LoggerFactory.getLogger(BdrcS3Resolver.class);
    private static final Pattern oldImageGroupPattern = Pattern.compile("^I\\d{4}$");

    @Override
    public String getS3Identifier(String identifier) throws ResourceIOException {

        try {
            String id = "";
            String[] parts = identifier.split("::");
            if (parts.length == 2 && parts[0].equals("static")) {
                id = parts[1].trim();
            } else {
                id = "Works/";
                IdentifierInfo info = IdentifierInfo.getIndentifierInfo(identifier);
                String work = info.getWork().substring(info.getWork().lastIndexOf('/') + 1);
                // String imgGroup = parts[0].substring(parts[0].lastIndexOf('_') + 1);
                String imgGroup = info.getImageGroup();
                MessageDigest md = MessageDigest.getInstance("MD5");
                md.reset();
                md.update(work.getBytes(Charset.forName("UTF8")));
                final String hash = new String(Hex.encodeHex(md.digest())).substring(0, 2);
                if (oldImageGroupPattern.matcher(imgGroup).matches()) {
                    imgGroup = imgGroup.substring(1);
                }
                id = id + hash + "/" + work + "/images/" + work + "-" + imgGroup + "/" + parts[1];
            }
            return id;
        } catch (Exception ex) {
            String msg = "BdrcS3Resolver was unable to produce identifier for key >> " + identifier;
            log.debug(msg, ex);
            throw new ResourceIOException(msg, ex);
        }
    }

    /*
     * public String getIdentifier(String identifier, IdentifierInfo info) throws
     * NoSuchAlgorithmException { String id = "Works/"; String[] parts =
     * identifier.split("::"); String work =
     * info.getWork().substring(info.getWork().lastIndexOf('/') + 1); String
     * imgGroup = parts[0].substring(parts[0].lastIndexOf('_') + 1); MessageDigest
     * md = MessageDigest.getInstance("MD5"); md.reset();
     * md.update(work.getBytes(Charset.forName("UTF8"))); final String hash = new
     * String(Hex.encodeHex(md.digest())).substring(0, 2); if
     * (oldImageGroupPattern.matcher(imgGroup).matches()) { imgGroup =
     * imgGroup.substring(1); } id = id + hash + "/" + work + "/images/" + work +
     * "-" + imgGroup + "/" + parts[1]; return id; }
     */

    public static void main(String[] args) throws IOException {
        Application.initForTests();
        BdrcS3Resolver dr = new BdrcS3Resolver();
        System.out.println(dr.getS3Identifier("bdr:V1NLM7_I1NLM7_001::I1NLM7_0010003.jpg"));
    }

}
