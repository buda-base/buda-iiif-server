package io.bdrc.iiif;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.iiif.resolver.ImageInfo;

public class ImageInfoCheck {

    public ImageInfoCheck() {
        // TODO Auto-generated constructor stub
    }

    @Test
    public void getImageListTest() throws ClientProtocolException, IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet("https://iiifpres.bdrc.io/il/v:bdr:V22703_I5449");
        HttpResponse resp = client.execute(get);
        System.out.println("STATUS >>> " + resp.getStatusLine());
        ObjectMapper om = new ObjectMapper();
        final InputStream objectData = resp.getEntity().getContent();
        try {
            final List<ImageInfo> imageList = om.readValue(objectData, new TypeReference<List<ImageInfo>>() {
            });
            objectData.close();
            imageList.removeIf(imageInfo -> imageInfo.filename.endsWith("json"));
            System.out.println("List >>> " + imageList.size());
            for (ImageInfo inf : imageList) {
                System.out.println("ImageInfo >>> " + inf);
            }
        } catch (IOException e) {
            throw e;
        }
    }

}
