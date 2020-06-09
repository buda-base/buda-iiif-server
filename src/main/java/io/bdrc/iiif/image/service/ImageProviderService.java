package io.bdrc.iiif.image.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

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
import io.bdrc.iiif.core.Application;
import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.iiif.resolver.IdentifierInfo;
import io.bdrc.libraries.GlobalHelpers;

public class ImageProviderService extends ConcurrentResourceService<byte[]> {

    private static final ClientConfiguration config = new ClientConfiguration().withConnectionTimeout(300000).withMaxConnections(50)
            .withMaxErrorRetry(100).withSocketTimeout(300000);
    // TODO: get from config
    final static String bucketNameArchive = "archive.tbrc.org";
    final static String bucketNameStatic = "static-images.bdrc.io";
    private static final Logger logger = LoggerFactory.getLogger(ImageProviderService.class);
    public static final ImageProviderService InstanceStatic = new ImageProviderService(bucketNameStatic, "static:");
    public static final ImageProviderService InstanceArchive = new ImageProviderService(bucketNameArchive, "archive:");
    public String bucketName;
    private static AmazonS3ClientBuilder clientBuilder = AmazonS3ClientBuilder.standard().withRegion(AuthProps.getProperty("awsRegion"))
            .withClientConfiguration(config);

    ImageProviderService(final String bucketName, final String cachePrefix) {
        super("iiif_img", cachePrefix);
        this.bucketName = bucketName;
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

    @Override
    final public byte[] getFromApi(final String s3key) throws IIIFException {
        String source = Application.getProperty("imageSourceType");
        switch (source) {
        case Application.S3_SOURCE:
            final AmazonS3 s3Client = getClient();
            logger.info("fetching s3 key {}", s3key);
            final S3Object object;
            try {
                object = s3Client.getObject(new GetObjectRequest(bucketName, s3key));
                InputStream objectData = object.getObjectContent();
                final byte[] rawContent = IOUtils.toByteArray(objectData);
                objectData.close();
                return rawContent;
            } catch (AmazonS3Exception e) {
                if (e.getErrorCode().equals("NoSuchKey")) {
                    logger.error("NoSuchKey: {}", s3key);
                    throw new IIIFException(404, 5000, "image not available in our archive");
                } else {
                    throw new IIIFException(500, 5000, e);
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        case Application.DISK_SOURCE:
            try {
                String rootDir = Application.getProperty("imageSourceDiskRootDir");
                return getImageAsBytes(rootDir + s3key);
            } catch (Exception e) {
                throw new IIIFException(500, 5000, e);
            }
        default:
            return new byte[0];
        }

    }

    public static byte[] getImageAsBytes(String filePath) throws Exception {
        FileInputStream in = new FileInputStream(new File(filePath));
        byte[] data = IOUtils.toByteArray(in);
        in.close();
        return data;
    }

}
