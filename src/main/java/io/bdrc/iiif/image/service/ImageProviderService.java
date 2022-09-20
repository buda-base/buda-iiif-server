package io.bdrc.iiif.image.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

import io.bdrc.auth.AuthProps;
import io.bdrc.iiif.core.EHServerCache;
import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.iiif.resolver.IdentifierInfo;
import io.bdrc.libraries.GlobalHelpers;

public class ImageProviderService extends ConcurrentCacheAccessService {

    private static final ClientConfiguration config = new ClientConfiguration().withConnectionTimeout(5000).withMaxConnections(50)
            .withMaxErrorRetry(5).withSocketTimeout(5000);
    // TODO: get from config
    final static String bucketNameArchive = "archive.tbrc.org";
    final static String bucketNameStatic = "static-images.bdrc.io";
    private static final Logger logger = LoggerFactory.getLogger(ImageProviderService.class);
    public static final ImageProviderService InstanceStatic = new ImageProviderService(bucketNameStatic, "static:");
    public static final ImageProviderService InstanceArchive = new ImageProviderService(bucketNameArchive, "archive:");
    public String bucketName;
    private static AmazonS3ClientBuilder clientBuilder = null;
    private String cachePrefix;
    boolean isS3 = false;
    
    static {
        if ("s3".equals(AuthProps.getProperty("imageSourceType")))
            clientBuilder = AmazonS3ClientBuilder.standard().withRegion(AuthProps.getProperty("awsRegion")).withClientConfiguration(config);
    }
            

    ImageProviderService(final String bucketName, final String cachePrefix) {
        super();
        this.cachePrefix = cachePrefix;
        this.bucketName = bucketName;
        if ("s3".equals(AuthProps.getProperty("imageSourceType")))
            isS3 = true;
    }

    public static AmazonS3 getClient() {
        return clientBuilder.build();
    }

    public static String getKey(final IdentifierInfo idf) {
        return getKeyPrefix(idf) + idf.imageId;
    }

    public static String getKeyPrefix(final IdentifierInfo idf) {
        String w_id = idf.igi.imageInstanceUri;
        if (w_id.lastIndexOf('/') != -1) {
            w_id = w_id.substring(w_id.lastIndexOf('/') + 1);
        }
        String md5firsttwo = "";
        md5firsttwo = GlobalHelpers.getTwoLettersBucket(w_id);
        String imageGroupId = ImageInfoListService.getS3ImageGroupId(idf.igi.imageGroup);
        return "Works/" + md5firsttwo + "/" + w_id + "/images/" + w_id + "-" + imageGroupId + "/";
    }
    
    public static String getSourcesPrefix(final String w_lname) {
        String md5firsttwo = "";
        md5firsttwo = GlobalHelpers.getTwoLettersBucket(w_lname);
        return "Works/" + md5firsttwo + "/" + w_lname + "/sources/";
    }
    
    public boolean isInCache(final String s3key) {
        if (!isS3)
            return true;
        final String cacheKey = this.cachePrefix + s3key;
        return EHServerCache.IIIF_IMG.hasKey(cacheKey);
    }
    
    public InputStream getFromCache(String s3Key) throws IIIFException {
        if (isS3) {
            return EHServerCache.IIIF_IMG.getIs(this.cachePrefix + s3Key);
        } else {
            String rootDir = AuthProps.getProperty("imageSourceDiskRootDir");
            try {
                return new FileInputStream(new File(rootDir + s3Key));
            } catch (FileNotFoundException e) {
                logger.error("could not find file for {}", s3Key);
                throw new IIIFException(e);
            }
        }
    }

    @Override
    final public void putInCache(final String s3key) throws IIIFException {
        if (!isS3)
            return;
        final AmazonS3 s3Client = getClient();
        logger.info("fetching s3 key {}", s3key);
        S3Object object = null;
        final String cacheKey = this.cachePrefix + s3key;
        try {
            object = s3Client.getObject(new GetObjectRequest(bucketName, s3key));
            InputStream objectData = object.getObjectContent();
            OutputStream os = EHServerCache.IIIF_IMG.getOs(cacheKey);
            IOUtils.copy(objectData, os);
            objectData.close();
            object.close();
            os.close();
            EHServerCache.IIIF_IMG.outputDone(cacheKey);
        } catch (AmazonS3Exception e) {
            if (e.getErrorCode().equals("NoSuchKey")) {
                logger.error("NoSuchKey: {}", s3key);
                EHServerCache.IIIF_IMG.remove(s3key, true);
                throw new IIIFException(404, 5000, "image not available in our archive");
            } else {
                EHServerCache.IIIF_IMG.remove(s3key, true);
                throw new IIIFException(500, 5000, e);
            }
        } catch (IOException e) {
            if (object != null) {
                try {
                    object.close();
                } catch (IOException e1) {
                    logger.error("error closing s3 object", e1);
                }
            }
            EHServerCache.IIIF_IMG.remove(s3key, true);
            throw new IIIFException(500, 5000, e);
        }
    }
    
    final public InputStream getNoCache(final String s3key) throws IIIFException {
        if (!isS3) {
            String rootDir = AuthProps.getProperty("imageSourceDiskRootDir");
            try {
                return new FileInputStream(new File(rootDir + s3key));
            } catch (FileNotFoundException e) {
                throw new IIIFException("can't find file "+rootDir+s3key);
            }
        }
        final AmazonS3 s3Client = getClient();
        logger.info("fetching s3 key {}", s3key);
        S3Object object = null;
        try {
            object = s3Client.getObject(new GetObjectRequest(bucketName, s3key));
            InputStream res = object.getObjectContent();
            //object.close();
            return res;
        } catch (AmazonS3Exception e) {
            if (e instanceof AmazonS3Exception && ((AmazonS3Exception) e).getErrorCode().equals("NoSuchKey")) {
                logger.error("NoSuchKey: {}", s3key);
                if (object != null) {
                    try {
                        object.close();
                    } catch (IOException e1) {
                        logger.error("error closing s3 object", e1);
                    }
                }
                EHServerCache.IIIF_IMG.remove(s3key, true);
                throw new IIIFException(404, 5000, "image not available in our archive");
            } else {
                if (object != null) {
                    try {
                        object.close();
                    } catch (IOException e1) {
                        logger.error("error closing s3 object", e1);
                    }
                }
                EHServerCache.IIIF_IMG.remove(s3key, true);
                throw new IIIFException(500, 5000, e);
            }
        }
    }

}
