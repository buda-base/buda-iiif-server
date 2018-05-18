package de.digitalcollections.iiif.myhymir;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@SpringBootApplication
@Configuration
@EnableAutoConfiguration 
@ComponentScan(
        basePackages = {
          "de.digitalcollections.iiif.hymir",
          "de.digitalcollections.iiif.myhymir",
          "de.digitalcollections.core.backend.impl.file.repository.resource.util"
        })
//,
//        excludeFilters = @ComponentScan.Filter(
//                type = FilterType.ASSIGNABLE_TYPE, value = {ResourceRepositoryImpl.class}))
public class Application extends SpringBootServletInitializer{

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Application.class, args);
    }
    
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Application.class);
    }
    
    

}