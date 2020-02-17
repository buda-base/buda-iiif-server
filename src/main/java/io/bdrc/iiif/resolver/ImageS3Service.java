package io.bdrc.iiif.resolver;

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
import io.bdrc.iiif.exceptions.IIIFException;

public class ImageS3Service extends ConcurrentResourceService<byte[]> {

    private static final ClientConfiguration config = new ClientConfiguration().withConnectionTimeout(300000).withMaxConnections(50).withMaxErrorRetry(100).withSocketTimeout(300000);
	// TODO: get from config
	final static String bucketNameArchive = "archive.tbrc.org";
	final static String bucketNameStatic = "static-images.bdrc.io";
	private static final Logger logger = LoggerFactory.getLogger(ImageS3Service.class);
	public static final ImageS3Service InstanceStatic = new ImageS3Service(bucketNameStatic, "static:");
	public static final ImageS3Service InstanceArchive = new ImageS3Service(bucketNameArchive, "archive:");
	public String bucketName;
	private static AmazonS3ClientBuilder clientBuilder = AmazonS3ClientBuilder.standard().withRegion(AuthProps.getProperty("awsRegion")).withClientConfiguration(config);

	ImageS3Service(final String bucketName, final String cachePrefix) {
		super("IIIF_IMG", cachePrefix);
		this.bucketName = bucketName;
	}

	private static AmazonS3 getClient() {
		return clientBuilder.build();
	}
	
    public static String getKey(final IdentifierInfo idf) {
        String w_id = idf.igi.imageInstanceId;
        w_id = w_id.substring(w_id.lastIndexOf('/')+1);
        final String md5firsttwo = ImageInfoListService.getFirstMd5Nums(w_id);
        String imageGroupId = ImageInfoListService.getS3ImageGroupId(idf.igi.imageGroup);
        return "Works/" + md5firsttwo + "/" + w_id + "/images/" + w_id + "-" + imageGroupId + "/" + idf.imageId;
    }

	@Override
	final public byte[] getFromApi(final String s3key) throws IIIFException {
		final AmazonS3 s3Client = getClient();
		logger.info("fetching s3 key {}", s3key);
		final S3Object object;
		try {
			object = s3Client.getObject(new GetObjectRequest(bucketName, s3key));
		} catch (AmazonS3Exception e) {
			if (e.getErrorCode().equals("NoSuchKey")) {
			    logger.error("NoSuchKey: {}", s3key);
				throw new IIIFException(404, 5000, "image not available in our archive");
			} else {
				throw new IIIFException(500, 5000, e);
			}
		}
		final InputStream objectData = object.getObjectContent();
		try {
			final byte[] rawContent = IOUtils.toByteArray(objectData);
			objectData.close();
			return rawContent;
		} catch (IOException e) {
			throw new IIIFException(500, 5000, e);
		}
	}

}
