package io.bdrc.iiif.metrics;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.digitalcollections.iiif.myhymir.ServerCache;
import io.bdrc.iiif.exceptions.IIIFException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;

public class ImageMetrics {

    private static final Logger log = LoggerFactory.getLogger(ImageMetrics.class);

    public static void imageGetCount(String origin) throws IIIFException {
        Counter cnt = Metrics.counter("image.calls", "context", origin);
        cnt.increment();
        log.info("Incremented image counter {}; it's value is now {}", cnt.getId(), cnt.count());
        ServerCache.addToCache("monitor", "image.calls.context." + origin, new Double(cnt.count()));
        updateOriginList(origin);
    }

    public static void imagePdfGetCount(String origin) throws IIIFException {
        Counter cnt = Metrics.counter("image.calls.pdf", "context", origin);
        cnt.increment();
        log.info("Incremented pdf image counter {}; it's value is now {}", cnt.getId(), cnt.count());
        ServerCache.addToCache("monitor", "image.calls.pdf.context." + origin, new Double(cnt.count()));
        updateOriginList("PDF_" + origin);
    }

    public static void imageZipGetCount(String origin) throws IIIFException {
        Counter cnt = Metrics.counter("image.calls.zip", "context", origin);
        cnt.increment();
        log.info("Incremented pdf image counter {}; it's value is now {}", cnt.getId(), cnt.count());
        ServerCache.addToCache("monitor", "image.calls.zip.context." + origin, new Double(cnt.count()));
        updateOriginList("ZIP_" + origin);
    }

    public static void init() {
        @SuppressWarnings("unchecked")
        ArrayList<String> orig_list = (ArrayList<String>) ServerCache.getObjectFromCache("monitor", "origins");
        if (orig_list != null) {
            for (String s : orig_list) {
                if (s.startsWith("PDF_")) {
                    Counter cnt = Metrics.counter("image.calls.pdf", "context", s);
                    cnt.increment(((Double) ServerCache.getObjectFromCache("monitor", "image.calls.pdf.context." + s)).doubleValue());
                }
                if (s.startsWith("ZIP_")) {
                    Counter cnt = Metrics.counter("image.calls.zip", "context", s);
                    cnt.increment(((Double) ServerCache.getObjectFromCache("monitor", "image.calls.zip.context." + s)).doubleValue());
                }
                if (!s.startsWith("PDF_") && !s.startsWith("ZIP_")) {
                    Counter cnt = Metrics.counter("image.calls", "context", s);
                    cnt.increment(((Double) ServerCache.getObjectFromCache("monitor", "image.calls.context." + s)).doubleValue());
                }
            }
        }
    }

    @SuppressWarnings({ "unchecked", "unused" })
    private static void updateOriginList(String origin) throws IIIFException {
        ArrayList<String> orig_list = (ArrayList<String>) ServerCache.getObjectFromCache("monitor", "origins");
        if (orig_list == null) {
            orig_list = new ArrayList<>();
        }
        if (!orig_list.contains(origin)) {
            orig_list.add(origin);
        }
        ServerCache.addToCache("monitor", "origins", orig_list);
    }

}
