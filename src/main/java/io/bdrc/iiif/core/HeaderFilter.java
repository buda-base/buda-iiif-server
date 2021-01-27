package io.bdrc.iiif.core;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class HeaderFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(HeaderFilter.class);

    @Override
    public void destroy() {
        // TODO Auto-generated method stub
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        try {
            HttpServletRequest request = (HttpServletRequest) req;
            String orig = request.getHeader("Origin");
            log.info("Origin in HeaderFilter: {}" + orig);
            if (orig == null) {
                orig = "*";
            }
            String referer = request.getHeader("Referer");
            log.info("referer in HeaderFilter: {}" + referer);
            String ref_orig = "";
            if (referer == null) {
                ref_orig = request.getHeader("Origin");
                log.info("referer is null");
                if (ref_orig == null || ref_orig.equals("")) {
                    ref_orig = request.getHeader("Host");
                }
            } else {
                String queryString = referer.substring(referer.indexOf("?") + 1);
                String[] parts = queryString.split("&");
                log.info("queryString parts {}", Arrays.asList(parts));
                if (parts.length == 2) {
                    for (String p : parts) {
                        String[] pair = p.split("=");
                        if (pair[0].contentEquals("origin")) {
                            ref_orig = pair[1];
                        }
                    }
                    log.info("origin in parts {}", ref_orig);
                } else {
                    URL ref = new URL(referer);
                    ref_orig = ref.getHost();
                }
            }
            if (ref_orig == null || ref_orig.equals("")) {
                ref_orig = "unknown";
            }
            log.info("final ref_orig {}", ref_orig);
            request.setAttribute("origin", ref_orig);
            HttpServletResponse response = (HttpServletResponse) res;
            response.setHeader("Access-Control-Allow-Origin", orig);
            response.setHeader("Access-Control-Allow-Headers",
                    "Origin, Authorization, Keep-Alive, User-Agent, If-Modified-Since, If-None-Match, Cache-Control");
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, HEAD, OPTIONS");
            response.setHeader("Access-Control-Expose-Headers",
                    "Cache-Control,ETag, Last-Modified, Content-Type, Cache-Control, Vary, Access-Control-Max-Age");
            chain.doFilter(req, res);
        } catch (IOException | ServletException e) {
            log.error("Header filter failed ! Message: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {
        // TODO Auto-generated method stub
    }

}
