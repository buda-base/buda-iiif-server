package de.digitalcollections.iiif.myhymir;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(
        basePackages = {
          "de.digitalcollections.iiif.hymir",
          "de.digitalcollections.iiif.myhymir"
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