package de.digitalcollections.core.backend.impl.file.repository.resource.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import de.digitalcollections.core.backend.impl.file.repository.resource.resolver.S3Resolver;
import de.digitalcollections.core.model.api.MimeType;
import de.digitalcollections.core.model.api.resource.enums.ResourcePersistenceType;
import de.digitalcollections.core.model.api.resource.exceptions.ResourceIOException;
import de.digitalcollections.iiif.myhymir.backend.impl.repository.S3ResourceRepositoryImpl;

@Component
public class S3ResourcePersistenceTypeHandler implements ResourcePersistenceTypeHandler{

    private static final Logger LOGGER = LoggerFactory.getLogger(S3ResourcePersistenceTypeHandler.class);
    
    static Properties local=new Properties();
    final String S3_RESOLVER="S3_RESOLVER";
    
    static {
        try {
            InputStream in = S3ResourceRepositoryImpl.class.getClassLoader().getResourceAsStream("s3repo.properties");          
            local.load(in);          
            in.close();         
            
          } catch (IOException ex) {
              String msg="Coudn't load S3 properties... ";
              LOGGER.error(msg, ex);
              throw new IllegalStateException(ex);
        }      
    }
    
    public S3ResourcePersistenceTypeHandler() {
        
    }
 
    
    @Override
    public ResourcePersistenceType getResourcePersistenceType() {
        // to Adjust I added S3 to de.digitalcollections.core.model.api.resource.enums.ResourcePersistenceType        
        return ResourcePersistenceType.S3;
    }

    @Override
    public List<URI> getUris(String resolvingKey, MimeType mimeType) throws ResourceIOException {
        List<URI> list=null;
        URI[] uri=new URI[1];
        try {
            S3Resolver resolver=(S3Resolver) Class.forName(local.getProperty(S3_RESOLVER)).newInstance();
            uri[0]=new URI(resolver.getS3Identifier(resolvingKey));
            list=Arrays.asList(uri);
        }
        catch(Exception ex) {
            String msg="";
            throw new ResourceIOException(msg +" "+ex.getMessage());
        }
        return list;
    }
    
    public String getIdentifier(String resolvingKey) throws ResourceIOException{
        try {
            S3Resolver resolver=(S3Resolver) Class.forName(local.getProperty(S3_RESOLVER)).newInstance();
            return resolver.getS3Identifier(resolvingKey);
        }
        catch(Exception ex) {
            String msg="";
            throw new ResourceIOException(msg +" "+ex.getMessage());
        }
    }

}
