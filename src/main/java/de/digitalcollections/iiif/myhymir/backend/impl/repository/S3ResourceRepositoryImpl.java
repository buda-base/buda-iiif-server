package de.digitalcollections.iiif.myhymir.backend.impl.repository;

import de.digitalcollections.core.backend.api.resource.ResourceRepository;
import de.digitalcollections.core.backend.impl.file.repository.resource.util.S3ResourcePersistenceTypeHandler;
import de.digitalcollections.core.model.api.MimeType;
import de.digitalcollections.core.model.api.resource.Resource;
import de.digitalcollections.core.model.api.resource.enums.ResourcePersistenceType;
import de.digitalcollections.core.model.api.resource.exceptions.ResourceIOException;
import de.digitalcollections.core.model.impl.resource.S3Resource;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.util.Properties;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;


/**
 * A ResourceRepository implementation to use with Amazon S3 services
 * 
 * @author @marcagate - Buddhist Digital Resource Center
 *
 */
@Primary
@Repository
public class S3ResourceRepositoryImpl implements ResourceRepository<Resource> {
    
    private static final Logger log = LoggerFactory.getLogger(S3ResourceRepositoryImpl.class);    
    
    
    static Properties props = new Properties();
    static Properties local=new Properties();
    
    static final String S3_ENDPOINT;
    static final String S3_BUCKET;
    static final String S3_REGION;
    static final String S3_ACCESS_KEY_ID;
    static final String S3_SECRET_KEY;
    static final String S3_MAX_CONNECTIONS;
    
    static {
        loadS3Props();
        S3_ENDPOINT=local.getProperty("S3_ENDPOINT");
        S3_BUCKET=local.getProperty("S3_BUCKET");
        S3_REGION=local.getProperty("S3_REGION");
        S3_ACCESS_KEY_ID=local.getProperty("S3_ACCESS_KEY_ID");
        S3_SECRET_KEY=local.getProperty("S3_SECRET_KEY");
        S3_MAX_CONNECTIONS=local.getProperty("S3_MAX_CONNECTIONS");
    }

    @Override
    public S3Resource create(String key, ResourcePersistenceType resourcePersistenceType, MimeType mimeType) throws ResourceIOException {
        S3Resource resource = new S3Resource();
        if (mimeType != null) {
          if (mimeType.getExtensions() != null && !mimeType.getExtensions().isEmpty()) {
            resource.setFilenameExtension(mimeType.getExtensions().get(0));
          }
          resource.setMimeType(mimeType);
        }
        if (ResourcePersistenceType.REFERENCED.equals(resourcePersistenceType)) {
          resource.setReadonly(true);
        }
        if (ResourcePersistenceType.MANAGED.equals(resourcePersistenceType)) {
          resource.setUuid(UUID.fromString(key));
        }
        S3ResourcePersistenceTypeHandler spt=new S3ResourcePersistenceTypeHandler();
        resource.setIdentifier(spt.getIdentifier(key));        
        return resource;        
    }

    @Override
    public void delete(Resource r) throws ResourceIOException {
      throw new UnsupportedOperationException("Not supported yet."); 
      //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public S3Resource find(String key, ResourcePersistenceType resourcePersistenceType, MimeType mimeType) throws ResourceIOException {
        S3Resource resource = create(key, resourcePersistenceType, mimeType);
        return resource;
    }
    
    @Override
    public byte[] getBytes(Resource r) throws ResourceIOException {
      throw new UnsupportedOperationException("Not supported yet."); 
      //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public InputStream getInputStream(URI uri) throws ResourceIOException {
      throw new UnsupportedOperationException("Not supported yet."); 
      //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public InputStream getInputStream(Resource r) throws ResourceIOException {
        return getInputStream((S3Resource) r);
    }
    
    public InputStream getInputStream(S3Resource r) throws ResourceIOException{
        log.info("Getting input stream for resource >> "+r.toString());
        AmazonS3 s3=S3ResourceRepositoryImpl.getClientInstance();
        S3Object obj=null;
        try {
            GetObjectRequest request = new GetObjectRequest(
                    props.getProperty(S3_BUCKET),
                    r.getIdentifier());
            obj=s3.getObject(request);
            log.info("Obj from s3 >> "+obj);
        } 
        catch (AmazonS3Exception e) {            
            throw new ResourceIOException(e.getMessage());            
        }
        InputStream stream=obj.getObjectContent();
        log.info("Obj from s3 >> "+stream);
        return stream;
    }

    @Override
    public Reader getReader(Resource r) throws ResourceIOException {
      throw new UnsupportedOperationException("Not supported yet."); 
      //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(Resource rsrc, String string) throws ResourceIOException {
      throw new UnsupportedOperationException("Not supported yet."); 
      //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(Resource rsrc, InputStream in) throws ResourceIOException {
      throw new UnsupportedOperationException("Not supported yet."); 
      //To change body of generated methods, choose Tools | Templates.
    }
  
    /**
     * Loads properties for S3
     * local.properties contains both "real" s3 properties files path (external to the project)
     * and properties keys.
     * s3.properties contains the actual s3 properties under keys defined in local props
     */
    private static void loadS3Props() {        
        try {
          InputStream in = S3ResourceRepositoryImpl.class.getClassLoader().getResourceAsStream("s3repo.properties");          
          local.load(in);          
          in.close();          
          FileReader fr= new FileReader(new File(local.getProperty("s3propsPath")));          
          props.load(fr);
          fr.close();
        } catch (IOException ex) {
            String msg="Coudn't load S3 properties... ";
            log.error(msg, ex);
            throw new IllegalStateException(ex);
        }      
    }
    
    /**
     * @return a client to interact with S3 bucket
     */
    private static synchronized AmazonS3 getClientInstance() {
        
        BasicAWSCredentials bac=new BasicAWSCredentials(props.getProperty(S3_ACCESS_KEY_ID),props.getProperty(S3_SECRET_KEY));
        ClientConfiguration ccfg = new ClientConfiguration();
        ccfg.setMaxConnections(Integer.parseInt(props.getProperty(S3_MAX_CONNECTIONS)));
        
        return AmazonS3ClientBuilder.standard()
        .withEndpointConfiguration(new EndpointConfiguration(props.getProperty(S3_ENDPOINT),props.getProperty(S3_REGION)))
        .withCredentials(new AWSStaticCredentialsProvider(bac))
        .withClientConfiguration(ccfg)        
        .build();
    }

}