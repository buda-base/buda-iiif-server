package io.bdrc.iiif.metrics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.digitalcollections.iiif.myhymir.Application;
import io.bdrc.iiif.exceptions.IIIFException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;

public class ImageMetrics {

    private static final Logger log = LoggerFactory.getLogger(ImageMetrics.class);
    public static ArrayList<String> counters;

    static {
        counters = new ArrayList<>();
        counters.add("image.calls");
        counters.add("image.calls.pdf");
        counters.add("image.calls.zip");
    }

    public static void imageGetCount(String origin) throws IIIFException {
        Counter cnt = Metrics.counter("image.calls", "context", origin);
        cnt.increment();
        log.info("Incremented image counter {}; it's value is now {}", cnt.getId(), cnt.count());
    }

    public static void imagePdfGetCount(String origin) throws IIIFException {
        Counter cnt = Metrics.counter("image.calls.pdf", "context", origin);
        cnt.increment();
        log.info("Incremented pdf image counter {}; it's value is now {}", cnt.getId(), cnt.count());
    }

    public static void imageZipGetCount(String origin) throws IIIFException {
        Counter cnt = Metrics.counter("image.calls.zip", "context", origin);
        cnt.increment();
        log.info("Incremented zip image counter {}; it's value is now {}", cnt.getId(), cnt.count());
    }

    public static void init() throws ClientProtocolException, IOException {

        ObjectMapper om = new ObjectMapper();
        String root = Application.getProperty("promQueryRangeURL");
        for (String c : counters) {
            String cstr = c.replace(".", "_") + "_total";
            String json = PromQLProcessor.getCounterValues(root, cstr);
            JsonNode jn = om.readTree(json);
            Iterator<JsonNode> it = jn.at("/data/result").iterator();
            while (it.hasNext()) {
                JsonNode jd = it.next();
                Iterator<JsonNode> ij = jd.elements();
                JsonNode metric = ij.next();
                JsonNode values = ij.next();
                String val = values.get(values.size() - 1).get(1).asText();
                Metric m = Metric.getMetric(c, metric.toString(), val);
                Counter cnt = Metrics.counter(c, "context", m.getContext());
                cnt.increment(Double.parseDouble(m.getCount()));
                log.info("{} METRICS INCREMENTED to >> {} for context: {}", c, m.getCount(), m.getContext());
            }
        }
    }

    public static void main(String[] args) throws ClientProtocolException, IOException {
        Application.initForTests();
        ImageMetrics.init();
    }
}
