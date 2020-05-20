package io.bdrc.iiif.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Timer;

import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import io.bdrc.auth.AuthProps;
import io.bdrc.auth.rdf.RdfAuthModel;
import io.bdrc.iiif.metrics.ImageMetrics;
import io.bdrc.iiif.metrics.MetricsTask;

@SpringBootApplication
@Configuration
@EnableAutoConfiguration
@EnableWebMvc
@Primary
@ComponentScan(basePackages = { "io.bdrc.iiif" })

//REMINDER : DO NOT REMOVE THE FOLLOWING COMMENTS
/*
 * , excludeFilters = { @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
 * value = ReferencedResourcePersistenceTypeHandler.class),
 * 
 * @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value =
 * ManagedResourcePersistenceTypeHandler.class), @ComponentScan.Filter(type =
 * FilterType.ASSIGNABLE_TYPE, value =
 * ResolvedResourcePersistenceTypeHandler.class) } type =
 * FilterType.ASSIGNABLE_TYPE, value = {ResourceRepositoryImpl.class}))
 */

public class Application extends SpringBootServletInitializer {

    static final String configPath = System.getProperty("iiifserv.configpath");
    public static Logger perfLog = LoggerFactory.getLogger("performance");
    private static Properties props;
    private static boolean logPerf = true;

    public static void main(String[] args) throws Exception {
        InputStream input = new FileInputStream(new File(configPath + "iiifserv.properties"));
        props = new Properties();
        props.load(input);
        try {
            InputStream is = new FileInputStream("/etc/buda/share/shared-private.properties");
            props.load(is);

        } catch (Exception ex) {
            // do nothing, continue props initialization
        }
        AuthProps.init(props);
        if ("true".equals(props.getProperty("authEnabled"))) {
            RdfAuthModel.init();
        }
        if (props.getProperty("logPerf") != null) {
            logPerf = Boolean.parseBoolean(props.getProperty("logPerf"));
        }
        // every minute ?
        EHServerCache.init();
        new Timer(true).schedule(new MetricsTask(), 0, 60000);
        SpringApplication.run(Application.class, args);
        logPerf("Application main", "Test PERF Log ");
    }

    public static void logPerf(String msg) {
        if (logPerf) {
            perfLog.debug(msg);
        }
    }

    public static void logPerf(String msg, Object o) {
        if (logPerf) {
            perfLog.debug(msg, o);
        }
    }

    public static void logPerf(String msg, Object o, Object i) {
        if (logPerf) {
            perfLog.debug(msg, o, i);
        }
    }

    public static void logPerf(String msg, Object o, Object i, Object j) {
        if (logPerf) {
            perfLog.debug(msg, o, i, j);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void postStartup() throws ClientProtocolException, IOException {
        if ("true".equals(props.getProperty("metricsEnabled"))) {
            ImageMetrics.init();
        }
    }

    public static void initForTests() throws IOException {
        InputStream input = new FileInputStream(new File("/etc/buda/iiifserv/iiifserv.properties"));
        props = new Properties();
        props.load(input);
        try {
            InputStream is = new FileInputStream("/etc/buda/share/shared-private.properties");
            props.load(is);

        } catch (Exception ex) {
            // do nothing, continue props initialization
        }
    }

    public static String getProperty(String key) {
        return props.getProperty(key);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Application.class);
    }

}