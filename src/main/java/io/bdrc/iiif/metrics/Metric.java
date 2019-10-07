package io.bdrc.iiif.metrics;

import java.io.IOException;
import java.util.TreeMap;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Metric {

    public String __name__;
    public String context;
    public String instance;
    public String job;
    public String count;
    public static TreeMap<Object, Object> values;

    /*
     * public Metric() { // TODO Auto-generated constructor stub }
     */

    public String get__name__() {
        return __name__;
    }

    public void set__name__(String __name__) {
        this.__name__ = __name__;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return "Metric [__name__=" + __name__ + ", context=" + context + ", instance=" + instance + ", job=" + job + ", count=" + count + "]";
    }

    public static Metric getMetric(String name, String json, String val) throws JsonParseException, JsonMappingException, IOException {
        Metric m = new ObjectMapper().readValue(json, Metric.class);
        m.setCount(val);
        m.set__name__(name);
        if (m.context == null || m.context.equals("")) {
            m.setContext("unknown");
        }
        return m;
    }

    public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException {
        String test = "{\"__name__\":\"image_calls_total\",\"context\":\"get\",\"instance\":\"localhost:9001\",\"job\":\"iiifserv-actuator\"},\"values\":[[1570113940,\"3\"]]}";
        ObjectMapper om = new ObjectMapper();
        Metric m = om.readValue(test, Metric.class);
        System.out.println("Metric >>" + m);
    }

}
