package io.bdrc.iiif.metrics;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import de.digitalcollections.iiif.myhymir.Application;

public class PromQLProcessor {

    public static String getFilteredCounterValues(String promURL, String countName, String filterkey, String filterValue) throws ClientProtocolException, IOException {
        String count = countName + "%7B" + filterkey + "=%22" + filterValue + "%22%7D";
        System.out.println("COUNT >>" + count);
        return getCounterValues(promURL, count);
    }

    public static String getCounterValues(String promURL, String countName) throws ClientProtocolException, IOException {
        HttpClient client = HttpClientBuilder.create().build();
        long end_in_seconds = (long) (System.currentTimeMillis() / 1000);
        // // two days period
        long start_in_seconds = end_in_seconds - 172800;
        String query = countName + "&_=" + Long.toString((long) (System.currentTimeMillis() / 1000)) + "&start=" + start_in_seconds + "&end=" + end_in_seconds + "&step=2000";
        System.out.println("URL FULL >>> " + promURL + query);
        HttpGet get = new HttpGet(promURL + query);
        HttpResponse resp = client.execute(get);
        String json = EntityUtils.toString(resp.getEntity(), "UTF-8");
        return json;
    }

    public static void main(String[] args) throws ClientProtocolException, IOException {
        Application.initForTests();
        String root = Application.getProperty("promQueryRangeURL");
        System.out.println(PromQLProcessor.getCounterValues(root, "image_calls_total"));
        System.out.println(PromQLProcessor.getFilteredCounterValues(root, "image_calls_total", "context", "website.com"));
    }

}
