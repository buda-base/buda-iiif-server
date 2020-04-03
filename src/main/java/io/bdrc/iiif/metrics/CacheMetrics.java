package io.bdrc.iiif.metrics;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.iiif.core.Application;
import io.bdrc.iiif.core.EHServerCache;
import io.bdrc.iiif.exceptions.IIIFException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;

public class CacheMetrics {

    private static final Logger log = LoggerFactory.getLogger(CacheMetrics.class);
    public static Set<String> counters;

    static {
        counters = EHServerCache.getAllCaches().keySet();
    }

    public static void cacheGet(String cacheName) throws IIIFException {
        if ("true".equals(Application.getProperty("metricsEnabled"))) {
            Counter cnt = Metrics.counter(cacheName + ".gets");
            cnt.increment();
            log.debug("Incremented cache get counter {}; it's value is now {}", cnt.getId(), cnt.count());
        }
    }

    public static void cachePut(String cacheName) throws IIIFException {
        if ("true".equals(Application.getProperty("metricsEnabled"))) {
            Counter cnt = Metrics.counter(cacheName + ".puts");
            cnt.increment();
            log.debug("Incremented cache put counter {}; it's value is now {}", cnt.getId(), cnt.count());
        }
    }

    public static void init() throws ClientProtocolException, IOException {
        ObjectMapper om = new ObjectMapper();
        String root = Application.getProperty("promQueryRangeURL");
        try {
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
                    try {
                        Counter cnt = Metrics.counter(c, "context", m.getContext());
                        cnt.increment(Double.parseDouble(m.getCount()));
                        log.info("{} METRICS INCREMENTED to >> {} for context: {}", c, m.getCount(), m.getContext());
                    } catch (Exception e) {
                        log.error("{} METRICS IS NULL WITH Prometheus resp  >> {} ", c, json);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Image metrics init failed ", e);
        }
    }

    public static void main(String[] args) throws ClientProtocolException, IOException {
        Application.initForTests();
        ImageMetrics.init();
    }
}
