package io.bdrc.iiif.image.service;

import static io.bdrc.iiif.resolver.AppConstants.CACHEPREFIX_IIL;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.auth.AuthProps;
import io.bdrc.iiif.core.Application;
import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.iiif.resolver.ImageInfo;
import io.bdrc.libraries.GlobalHelpers;

public class ImageInfoListService extends ConcurrentResourceService<List<ImageInfo>> {

    final static ObjectMapper mapper = new ObjectMapper();
    // TODO: get from config
    final static String bucketName = "archive.tbrc.org";
    private static AmazonS3 s3Client = null;
    static MessageDigest md;
    static private final ObjectMapper om;
    private static final Logger logger = LoggerFactory.getLogger(ImageInfoListService.class);
    public static final ImageInfoListService Instance = new ImageInfoListService();

    ImageInfoListService() {
        super("imageListInfo", CACHEPREFIX_IIL);
    }

    static {
        om = new ObjectMapper();
    }

    private static AmazonS3 getClient() {
        if (s3Client == null) {
            AmazonS3ClientBuilder clientBuilder = AmazonS3ClientBuilder.standard().withRegion(AuthProps.getProperty("awsRegion"));
            s3Client = clientBuilder.build();
        }
        return s3Client;
    }

    public static String getKey(final String workLocalId, String imageGroupId) {
        String md5firsttwo = "";
        md5firsttwo = GlobalHelpers.getTwoLettersBucket(workLocalId);
        imageGroupId = getS3ImageGroupId(imageGroupId);
        return "Works/" + md5firsttwo + "/" + workLocalId + "/images/" + workLocalId + "-" + imageGroupId + "/dimensions.json";
    }

    public static final Pattern oldImageGroupPattern = Pattern.compile("^I\\d{4}$");

    // for image groups like I\d\d\d\d, the s3 key doesn't contain the I (ex: I0886
    // -> 0886)
    public static String getS3ImageGroupId(final String dataImageGroupId) {
        if (oldImageGroupPattern.matcher(dataImageGroupId).matches())
            return dataImageGroupId.substring(1);
        return dataImageGroupId;
    }

    public final CompletableFuture<List<ImageInfo>> getAsync(final String workLocalId, final String imageGroupId) throws IIIFException {
        final String s3key = getKey(workLocalId, imageGroupId);
        return getAsync(s3key);
    }

    @Override
    final public List<ImageInfo> getFromApi(final String s3key) throws IIIFException {
        String source = Application.getProperty("imageSourceType");
        switch (source) {
        case Application.S3_SOURCE:
            final AmazonS3 s3Client = getClient();
            logger.info("fetching s3 key {}", s3key);
            final S3Object object;
            try {
                object = s3Client.getObject(new GetObjectRequest(bucketName, s3key));
            } catch (AmazonS3Exception e) {
                if (e.getErrorCode().equals("NoSuchKey")) {
                    logger.error("NoSuchKey: {}", s3key);
                    throw new IIIFException(404, 5000, "sorry, BDRC did not complete the data migration for this Work");
                } else {
                    logger.error("S3 error on: {}", s3key);
                    throw new IIIFException(500, 5000, e);
                }
            }
            final InputStream objectData = object.getObjectContent();
            try {
                final GZIPInputStream gis = new GZIPInputStream(objectData);
                final List<ImageInfo> imageList = om.readValue(gis, new TypeReference<List<ImageInfo>>() {
                });
                gis.close();
                objectData.close();
                imageList.removeIf(imageInfo -> imageInfo.filename.endsWith("json"));
                return imageList;
            } catch (IOException e) {
                throw new IIIFException(500, 5000, e);
            }
        case Application.DISK_SOURCE:
            try {
                String rootDir = Application.getProperty("imageSourceDiskRootDir");
                FileInputStream in = new FileInputStream(new File(rootDir + s3key));
                final GZIPInputStream gis = new GZIPInputStream(in);
                final List<ImageInfo> imageList = om.readValue(gis, new TypeReference<List<ImageInfo>>() {
                });
                imageList.removeIf(imageInfo -> imageInfo.filename.endsWith("json"));
                gis.close();
                in.close();
                return imageList;
            } catch (Exception e) {
                throw new IIIFException(500, 5000, e);
            }
        default:
            return new ArrayList<>();
        }
    }

}
