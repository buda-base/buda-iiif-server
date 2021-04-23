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
import io.bdrc.iiif.core.DiskCache;
import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.iiif.resolver.IdentifierInfo;
import io.bdrc.libraries.GlobalHelpers;

public class ImageProviderService extends ConcurrentCacheAccessService {

    private static final ClientConfiguration config = new ClientConfiguration().withConnectionTimeout(3000).withMaxConnections(50)
            .withMaxErrorRetry(5).withSocketTimeout(3000);
    // TODO: get from config
    final static String bucketNameArchive = "archive.tbrc.org";
    final static String bucketNameStatic = "static-images.bdrc.io";
    private static final Logger logger = LoggerFactory.getLogger(ImageProviderService.class);
    public static final ImageProviderService InstanceStatic = new ImageProviderService(bucketNameStatic, "static:");
    public static final ImageProviderService InstanceArchive = new ImageProviderService(bucketNameArchive, "archive:");
    public String bucketName;
    private static AmazonS3ClientBuilder clientBuilder = null;
    private static DiskCache cache;
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
        String w_id = idf.igi.imageInstanceId;
        if (w_id.lastIndexOf('/') != -1) {
            w_id = w_id.substring(w_id.lastIndexOf('/') + 1);
        }
        String md5firsttwo = "";
        md5firsttwo = GlobalHelpers.getTwoLettersBucket(w_id);
        String imageGroupId = ImageInfoListService.getS3ImageGroupId(idf.igi.imageGroup);
        return "Works/" + md5firsttwo + "/" + w_id + "/images/" + w_id + "-" + imageGroupId + "/";
    }
    
    boolean isInCache(final String s3key) {
        if (!isS3)
            return true;
        final String cacheKey = this.cachePrefix + s3key;
        // TODO: perhaps check the status to make sure
        // it's not currently being written
        return cache.hasKey(cacheKey);
    }
    
    InputStream getFromCache(String s3Key) throws FileNotFoundException {
        if (isS3) {
            return cache.getIs(this.cachePrefix + s3Key);
        } else {
            String rootDir = AuthProps.getProperty("imageSourceDiskRootDir");
            return new FileInputStream(new File(rootDir + s3Key));
        }
    }

    @Override
    final public void putInCache(final String s3key) throws IIIFException {
        if (!isS3)
            return;
        final AmazonS3 s3Client = getClient();
        logger.info("fetching s3 key {}", s3key);
        final S3Object object;
        final String cacheKey = this.cachePrefix + s3key;
        try {
            object = s3Client.getObject(new GetObjectRequest(bucketName, s3key));
            InputStream objectData = object.getObjectContent();
            OutputStream os = cache.getOs(cacheKey);
            IOUtils.copy(objectData, os);
            objectData.close();
            cache.outputDone(cacheKey);
        } catch (AmazonS3Exception e) {
            if (e.getErrorCode().equals("NoSuchKey")) {
                logger.error("NoSuchKey: {}", s3key);
                cache.remove(s3key, true);
                throw new IIIFException(404, 5000, "image not available in our archive");
            } else {
                cache.remove(s3key, true);
                throw new IIIFException(500, 5000, e);
            }
        } catch (IOException e) {
            cache.remove(s3key, true);
            throw new IIIFException(500, 5000, e);
        }
    }

}
