package io.bdrc.iiif.resolver;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.digitalcollections.core.backend.impl.file.repository.resource.resolver.S3Resolver;
import de.digitalcollections.core.model.api.resource.exceptions.ResourceIOException;

public class BdrcS3Resolver implements S3Resolver {

    private static final Logger log = LoggerFactory.getLogger(BdrcS3Resolver.class);
    
    @Override
    public String getS3Identifier(String identifier) throws ResourceIOException {
        // Resolving : "bdr:V29329_I1KG15042::I1KG150420003.jpg"
        // to s3 path: "Works/21/W29329/images/W29329-I1KG15042/I1KG150420003.jpg"
        Pattern oldImageGroupPattern = Pattern.compile("^I\\d{4}$");
        try {
            String id="Works/";
            String[] parts=identifier.split("::");
            IdentifierInfo info=new IdentifierInfo(parts[0]);
            String access=info.getAccess().substring(info.getAccess().lastIndexOf('/')+1);
            String format=parts[1].substring(parts[1].lastIndexOf('.'));
            if(access.equalsIgnoreCase("AccessOpen")) {
                log.info("S3 Resolver IdentifierInfo >>>>>>>> "+info+ " Access >>"+access+ " Format >> "+format);            
                String work=info.getWork().substring(info.getWork().lastIndexOf('/')+1);
                String imgGroup=parts[0].substring(parts[0].lastIndexOf('_')+1);
                MessageDigest md = MessageDigest.getInstance("MD5");
                md.reset();
                md.update(work.getBytes(Charset.forName("UTF8")));
                final String hash = new String(Hex.encodeHex(md.digest())).substring(0,2);
                if (oldImageGroupPattern.matcher(imgGroup).matches()) {
                    imgGroup= imgGroup.substring(1);
                }
                id=id+hash+"/"+work+"/images/"+work+"-"+imgGroup+"/"+parts[1]; 
            }else {
                id="static/error"+format;
            }
            return id;
        }
        catch(Exception ex) {
            String msg="BdrcS3Resolver was unable to produce identifier for key >> "+identifier;
            throw new ResourceIOException(msg, ex);
        }
    }
    
    public static void main(String[] args) throws ResourceIOException {
        BdrcS3Resolver s3=new BdrcS3Resolver();
        log.info("Path s3 >>"+s3.getS3Identifier("bdr:V29329_I1KG15042::I1KG150420003.jpg"));        
    }

}
