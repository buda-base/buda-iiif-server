package io.bdrc.iiif.auth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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

import java.util.Base64;

import io.bdrc.auth.AccessInfo;
import io.bdrc.auth.AccessInfoAuthImpl;
import io.bdrc.auth.AuthProps;
import io.bdrc.auth.TokenValidation;
import io.bdrc.auth.UserProfile;
import io.bdrc.auth.model.Endpoint;
import io.bdrc.iiif.core.Application;

@Component
@Order(1)
public class AuthFilter implements Filter {

    public final static Logger log = LoggerFactory.getLogger(AuthFilter.class.getName());
    public static String cookieKey;
    public static Map<String, AccessInfo> apiKeys = new HashMap<>();

    @Override
    public void destroy() {
        //
    }
    
    public static void init(Properties props) {
        cookieKey = props.getProperty("cookieKey");
        final String naKeysS = props.getProperty("auth.apikeys.na");
        if (naKeysS != null && !naKeysS.isEmpty()) {
            for (final String naKey : naKeysS.split(",")) {
                apiKeys.put(naKey, APIKeyNormalAccessInfo.INSTANCE);
            }
        }
        final String faKeysS = props.getProperty("auth.apikeys.fa");
        if (faKeysS != null && !faKeysS.isEmpty()) {
            for (final String faKey : faKeysS.split(",")) {
                apiKeys.put(faKey, APIKeyFullAccessInfo.INSTANCE);
            }
        }
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        String method = ((HttpServletRequest) req).getMethod();
        try {
            UserProfile prof = null;
            if (!Application.isInChina() && "true".equals(AuthProps.getProperty("authEnabled")) && !method.equalsIgnoreCase("OPTIONS")) {
                log.info("IIIF SERVER IS USING AUTH !");
                String token = getToken(((HttpServletRequest) req).getHeader("Authorization"));
                log.info("TOKEN >> {}", token);
                if (token == null) {
                    log.info("TOKEN is null, looking for cookies >> ");
                    final Cookie[] cookies = ((HttpServletRequest) req).getCookies();
                    if (cookies != null) {
                        for (Cookie cook : cookies) {
                            if (cook.getName().equals(cookieKey)) {
                                token = cook.getValue();
                                log.info("TOKEN was found in cookies >> {}", token);
                                break;
                            }
                        } ;
                    }
                }
                log.info("TOKEN is null {}", (token == null));
                if (token != null) {
                    // User is logged in
                    // Getting his profile
                    final TokenValidation validation = new TokenValidation(token);
                    if (!validation.isValid()) {
                        log.error("invalid token: {}", token);
                    } else {
                        prof = validation.getUser();
                        log.info("validation is {}", validation);
                        log.info("profile is {}", prof);
                    }
                }
            }
            if (prof != null) {
                req.setAttribute("access", new AccessInfoAuthImpl(prof, new Endpoint()));
            } else {
                final String apiKey = getApiKey(((HttpServletRequest) req).getHeader("Authorization"));
                if (apiKey != null && apiKeys.containsKey(apiKey)) {
                    final AccessInfo ai = apiKeys.get(apiKey);
                    log.info("found api key, access is {}", ai.getClass());
                    req.setAttribute("access", ai);
                } else {
                    log.info("no token, no api key or invalid api key, default access");
                    req.setAttribute("access", new AccessInfoAuthImpl());
                }
            }
            log.debug("REQUEST SET WITH ACCESS {}", req.getAttribute("access"));
            chain.doFilter(req, res);
        } catch (IOException | ServletException e) {
            log.error("IIIF RdfAuth filter failed ! Message: " + e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {
        //
    }

    public static String getToken(final String header) {
        if (header == null || !header.startsWith("Bearer "))
            return null;
        return header.substring(7);
    }
    
    public static String getApiKey(final String header) {
        if (header == null || !header.startsWith("XBdrcKey "))
            return null;
        final String b64 = header.substring(9);
        final byte[] keyba = Base64.getDecoder().decode(b64);
        final String keyS = new String(keyba, StandardCharsets.UTF_8);
        return keyS;
    }

}
