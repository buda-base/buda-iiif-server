package de.digitalcollections.iiif.myhymir;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import io.bdrc.auth.AuthProps;
import io.bdrc.auth.rdf.RdfAuthModel;


@SpringBootApplication
@Configuration
@EnableAutoConfiguration
@Primary
@ComponentScan(
        basePackages = {
          "io.bdrc.archives",
          "io.bdrc.iiif.auth",
          "de.digitalcollections.iiif.hymir",
          "de.digitalcollections.iiif.myhymir",
          "de.digitalcollections.core.backend.impl.file.repository.resource.util"
        })
//,
//        excludeFilters = @ComponentScan.Filter(
//                type = FilterType.ASSIGNABLE_TYPE, value = {ResourceRepositoryImpl.class}))
public class Application extends SpringBootServletInitializer{

    static final String configPath= System.getProperty("iiifserv.configpath");

    public static void main(String[] args) throws Exception {
        InputStream is=new FileInputStream(configPath+"iiifserv.properties");
        Properties props=new Properties();
        props.load(is);
        AuthProps.init(props);
        SpringApplication.run(Application.class, args);
        RdfAuthModel.init();
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Application.class);
    }

}