package de.digitalcollections.iiif.myhymir;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class HeaderFilter implements Filter {

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
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        String orig = request.getHeader("Origin");
        if (orig == null) {
            orig = allowOrigin;
        }
        String referer = request.getHeader("Referer");
        // for tests
        // referer =
        // "https://library.bdrc.io/scripts/embed-iframe.html?work=bdr:W1ERI0011001&origin=website.com";
        String ref_orig = "";
        if (referer == null) {
            ref_orig = "unknown";
        } else {
            String queryString = referer.substring(referer.indexOf("?") + 1);
            String[] parts = queryString.split("&");
            for (String p : parts) {
                String[] pair = p.split("=");
                if (pair[0].contentEquals("origin")) {
                    ref_orig = pair[1];
                }
            }
        }
        request.setAttribute("origin", ref_orig);
        HttpServletResponse response = (HttpServletResponse) res;
        response.setHeader("Access-Control-Allow-Origin", orig);
        response.setHeader("Access-Control-Allow-Headers", allowHeaders);
        response.setHeader("Access-Control-Allow-Credentials", allowCredentials);
        response.setHeader("Access-Control-Allow-Methods", allowMethods);
        response.setHeader("Access-Control-Expose-Headers", exposeHeaders);
        chain.doFilter(req, res);
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {
        // TODO Auto-generated method stub
    }

}
