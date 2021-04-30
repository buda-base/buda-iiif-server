package io.bdrc.iiif.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Properties;
import java.util.Timer;

import javax.imageio.ImageIO;

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
@ComponentScan(basePackages = {"io.bdrc.iiif"})

// REMINDER : DO NOT REMOVE THE FOLLOWING COMMENTS
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
    public static Logger log = LoggerFactory.getLogger(Application.class);
    public static Properties props;
    private static boolean logPerf = true;
    public static final String DISK_SOURCE = "disk";
    public static final String S3_SOURCE = "s3";

    static class GlobalThreadExceptionHandler implements UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread thread, Throwable throwable) {
            log.error(String.format("Caught unhandled exception in thread %s", thread), throwable);
            Runtime.getRuntime().halt(137);
        }
    }
    
    public static void main(String[] args) throws Exception {
        log.info("load {}", configPath + "iiifserv.properties");
        InputStream input = new FileInputStream(new File(configPath + "iiifserv.properties"));
        props = new Properties();
        props.load(input);
        input.close();
        SpringApplication app = new SpringApplication(Application.class);
        app.setDefaultProperties(props);
        try {
            InputStream is = new FileInputStream("/etc/buda/share/shared-private.properties");
            props.load(is);
            is.close();
        } catch (Exception ex) {
            // do nothing, continue props initialization
            log.error("can't load load /etc/buda/share/shared-private.properties");
        }
        AuthProps.init(props);
        if (props.getProperty("logPerf") != null) {
            logPerf = Boolean.parseBoolean(props.getProperty("logPerf"));
        }
        log.info("{}", props);
        // every minute ?
        EHServerCache.init();
        new Timer(true).schedule(new MetricsTask(), 0, 60000);
        app.run(args);
        if ("true".equals(props.getProperty("authEnabled")) && !isInChina()) {
            RdfAuthModel.init();
        }
        logPerf("Application main", "Test PERF Log ");
        Thread.setDefaultUncaughtExceptionHandler(new GlobalThreadExceptionHandler());
        ImageIO.setUseCache(false);
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
        input.close();
        try {
            InputStream is = new FileInputStream("/etc/buda/share/shared-private.properties");
            props.load(is);
            is.close();
        } catch (Exception ex) {
            // do nothing, continue props initialization
        }
    }

    public static String getProperty(String key) {
        return props.getProperty(key);
    }

    public static boolean isInChina() {
        String val = props.getProperty("serverLocation");
        if (val != null) {
            if (val.equals("china")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPdfSync() {
        String val = props.getProperty("pdfsync");
        if (val == null) {
            return false;
        }
        try {
            return Boolean.parseBoolean(val);
        } catch (Exception ex) {
            return false;
        }

    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Application.class);
    }

}