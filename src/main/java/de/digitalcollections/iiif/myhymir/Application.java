package de.digitalcollections.iiif.myhymir;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

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

import de.digitalcollections.iiif.myhymir.backend.impl.repository.S3ResourceRepositoryImpl;
import io.bdrc.auth.AuthProps;
import io.bdrc.auth.rdf.RdfAuthModel;

@SpringBootApplication
@Configuration
@EnableAutoConfiguration
@Primary
@ComponentScan(basePackages = { "de.digitalcollections.core.business.impl.service", "io.bdrc.archives", "io.bdrc.iiif",
        "de.digitalcollections.iiif.hymir", "de.digitalcollections.iiif.myhymir",
        "de.digitalcollections.core.backend.impl.file.repository.resource.util" })

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
        S3ResourceRepositoryImpl.initWithProps(props);
        ServerCache.init();
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
        // ImageMetrics.init();
    }

    public static void initForTests() throws IOException {
        InputStream input = Application.class.getClassLoader().getResourceAsStream("test.properties");
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