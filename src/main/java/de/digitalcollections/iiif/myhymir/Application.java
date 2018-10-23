package de.digitalcollections.iiif.myhymir;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import io.bdrc.auth.AuthProps;
import io.bdrc.auth.rdf.RdfAuthModel;


@SpringBootApplication
@Configuration
@EnableAutoConfiguration
@ComponentScan(
        basePackages = {
          "io.bdrc.archives",
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
        SpringApplication.run(Application.class, args);
        AuthProps.init(configPath+"iiifserv.properties");
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>> "+AuthProps.getProperty("issuer")+ " config >>"+configPath);
        RdfAuthModel.init();
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Application.class);
    }

}