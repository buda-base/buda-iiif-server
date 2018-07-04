package de.digitalcollections.iiif.myhymir;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HeaderFilter implements Filter {

    @Value("${cache-control.maxage}")
    private String maxAge;
    @Value("${access-control.Allow-Origin}")
    private String allowOrigin;
    @Value("${access-control.Allow-Headers}")
    private String allowHeaders;
    @Value("${access-control.Allow-Credentials}")
    private String allowCredentials;
    @Value("${access-control.Allow-Methods}")
    private String allowMethods;
    @Value("${access-control.Expose-Headers}")
    private String exposeHeaders;
    
    @Override
    public void destroy() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        response.addHeader("Cache-Control", " public, max-age="+maxAge);  
        response.addHeader("Access-Control-Allow-Origin", allowOrigin);
        response.addHeader("Access-Control-Allow-Headers",allowHeaders);
        response.addHeader("Access-Control-Allow-Credentials", allowCredentials);
        response.addHeader("Access-Control-Allow-Methods",allowMethods);
        response.addHeader("Access-Control-Expose-Headers",exposeHeaders);
        chain.doFilter(req, res);
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {
        // TODO Auto-generated method stub
    }

}
