package io.bdrc.iiif;


import java.io.InputStream;
import java.util.Properties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;

import de.digitalcollections.iiif.myhymir.IIIFRdfAuthFilter;
import io.bdrc.auth.rdf.RdfAuthModel;


@SpringBootApplication
@Configuration
@EnableAutoConfiguration
@Primary
@ComponentScan(
        basePackages = {
          "io.bdrc.archives",
          "io.bdrc.iiif",
          "de.digitalcollections.iiif.hymir",
          "de.digitalcollections.iiif.myhymir.backend",
          //"de.digitalcollections.iiif.myhymir.image",
          "de.digitalcollections.core.backend.impl.file.repository.resource.util"
        }
,
        excludeFilters = @ComponentScan.Filter(
               type = FilterType.ASSIGNABLE_TYPE, value = {IIIFRdfAuthFilter.class}))
public class TestApplication extends SpringBootServletInitializer{

    static final String configPath= System.getProperty("iiifserv.configpath");

    public static void main(String[] args) throws Exception {
        InputStream input=TestApplication.class.getClassLoader().getResourceAsStream("iiifserv.properties");
        Properties props=new Properties();
        props.load(input);
        /*try {
            InputStream is = new FileInputStream(configPath+"iiifserv-private.properties");
            props.load(is);

        }catch(Exception ex) {
            //do nothing, continue props initialization
        }
        AuthProps.init(props);*/
        SpringApplication.run(TestApplication.class, args);
        RdfAuthModel.init();
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(TestApplication.class);
    }

}