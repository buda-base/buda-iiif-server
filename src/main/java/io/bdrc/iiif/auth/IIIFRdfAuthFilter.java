package io.bdrc.iiif.auth;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import io.bdrc.auth.Access;
import io.bdrc.auth.AuthProps;
import io.bdrc.auth.TokenValidation;
import io.bdrc.auth.UserProfile;
import io.bdrc.auth.model.Endpoint;
import io.bdrc.iiif.core.Application;

@Component
@Order(1)
public class IIIFRdfAuthFilter implements Filter {

    public final static Logger log = LoggerFactory.getLogger(IIIFRdfAuthFilter.class.getName());

    @Override
    public void destroy() {
        //
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        String method = ((HttpServletRequest) req).getMethod();

        try {
            if (!Application.isInChina()) {
                if ("true".equals(AuthProps.getProperty("authEnabled")) && !method.equalsIgnoreCase("OPTIONS")) {
                    log.info("IIIF SERVER IS USING AUTH !");
                    String token = getToken(((HttpServletRequest) req).getHeader("Authorization"));
                    log.info("TOKEN >> {}", token);
                    if (token == null) {
                        log.info("TOKEN is null, looking for cookies >> ");
                        Cookie[] cookies = ((HttpServletRequest) req).getCookies();
                        if (cookies != null) {
                            for (Cookie cook : cookies) {
                                if (cook.getName().equals(AuthProps.getProperty("cookieKey"))) {
                                    token = cook.getValue();
                                    log.info("TOKEN was found in cookies >> {}", token);
                                    break;
                                }
                            } ;
                        }

                    }
                    TokenValidation validation = null;
                    UserProfile prof = null;
                    log.info("TOKEN is null {}", (token == null));
                    if (token != null) {
                        // User is logged in
                        // Getting his profile
                        validation = new TokenValidation(token);
                        log.info("AUTH VALIDATION {}", validation);
                        prof = validation.getUser();
                        log.info("validation is {}", validation);
                        log.info("profile is {}", prof);
                        req.setAttribute("access", new Access(prof, new Endpoint()));
                    } else {
                        req.setAttribute("access", new Access());
                    }
                } else {
                    req.setAttribute("access", new Access());
                }
                System.out.println("ACCESS WAS SET AS >>" + req.getAttribute("access"));
                log.debug("REQUEST SET WITH ACCESS {}", req.getAttribute("access"));
            }
            chain.doFilter(req, res);
        } catch (IOException | ServletException e) {
            log.error("IIIF RdfAuth filter failed ! Message: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {
        //
    }

    String getToken(String header) {
        try {
            if (header != null) {
                return header.split(" ")[1];
            }
        } catch (Exception ex) {
            log.error("Could not get Token from header " + header + " Message: " + ex.getMessage());
            return null;
        }
        return null;
    }

}
