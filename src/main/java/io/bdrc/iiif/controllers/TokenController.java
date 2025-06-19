package io.bdrc.iiif.controllers;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.client.ClientProtocolException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.bdrc.auth.AccessInfo;
import io.bdrc.auth.AccessInfoAuthImpl;
import io.bdrc.auth.AuthProps;
import io.bdrc.auth.TokenValidation;
import io.bdrc.auth.model.Endpoint;
import io.bdrc.auth.rdf.RdfAuthModel;
import io.bdrc.auth.rdf.RdfConstants;
import io.bdrc.auth.rdf.Subscribers;
import io.bdrc.iiif.auth.AuthFilter;
import io.bdrc.iiif.core.GeoLocation;
import io.bdrc.iiif.exceptions.IIIFException;

@RestController
@Component
@RequestMapping("/debug/")
public class TokenController {

    private final ObjectMapper mapper = new ObjectMapper();
    
    @RequestMapping(value = "/debugOtherToken/{token}", method = {RequestMethod.GET, RequestMethod.HEAD})
    public ResponseEntity<StreamingResponseBody> debugOtherToken(@PathVariable("token") String token, HttpServletRequest request, HttpServletResponse response,
            WebRequest webRequest)
            throws ClientProtocolException, IOException, IIIFException {
        return debugToken(request, response, webRequest, Optional.of(token));
    }
    
    @RequestMapping(value = "/personalAccess/{userqname}", method = {RequestMethod.GET, RequestMethod.HEAD})
    public ResponseEntity<String> debugPersonalAccess(@PathVariable("userqname") String userqname, HttpServletRequest request, HttpServletResponse response,
            WebRequest webRequest)
            throws ClientProtocolException, IOException {
        final String userUri = "http://purl.bdrc.io/resource-nc/auth/"+userqname.substring(4);
        final List<String> res = RdfAuthModel.getPersonalAccess(userUri);
        final HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        if (res == null || res.isEmpty()) {
            return new ResponseEntity<String>("\"\"",
                    headers, HttpStatus.OK);
        }
        return new ResponseEntity<String>(mapper.writeValueAsString(res),
                headers, HttpStatus.OK);
    }
    
    @RequestMapping(value = "/debugToken", method = {RequestMethod.GET, RequestMethod.HEAD})
    public ResponseEntity<StreamingResponseBody> debugSelfToken(HttpServletRequest request, HttpServletResponse response,
            WebRequest webRequest)
            throws ClientProtocolException, IOException, IIIFException {
        return debugToken(request, response, webRequest, Optional.empty());
    }
        
    public ResponseEntity<StreamingResponseBody> debugToken(HttpServletRequest request, HttpServletResponse response,
                WebRequest webRequest, Optional<String> token)
                throws ClientProtocolException, IOException, IIIFException {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        final ObjectNode rootNode = mapper.createObjectNode();
        
        AccessInfo acc = null;
        
        if (!token.isPresent()) {
            // debugging the token of the request
            String mytoken = AuthFilter.getToken(((HttpServletRequest) request).getHeader("Authorization"));
            if (mytoken == null) {
                Cookie[] cookies = ((HttpServletRequest) request).getCookies();
                if (cookies != null) {
                    for (Cookie cook : cookies) {
                        if (cook.getName().equals(AuthProps.getProperty("cookieKey"))) {
                            mytoken = cook.getValue();
                            break;
                        }
                    }
                }
            }
            rootNode.put("token", mytoken);
            acc = (AccessInfo) request.getAttribute("access");
            if (acc == null || !acc.isLogged())
                return new ResponseEntity<StreamingResponseBody>(IIIFImageApiController.streamingResponseFrom("\"You must be authenticated to access this service\""),
                        headers, HttpStatus.UNAUTHORIZED); 
        } else {
            // debugging a token passed as argument
            rootNode.put("token", token.get());
            TokenValidation validation = new TokenValidation(token.get());
            rootNode.put("tokenvalid", validation.isValid());
            acc = new AccessInfoAuthImpl(validation.getUser(), new Endpoint());
            if (acc == null || !acc.isLogged())
                return new ResponseEntity<StreamingResponseBody>(IIIFImageApiController.streamingResponseFrom("\"a valid access cannot be built from this token\""),
                        headers, HttpStatus.UNAUTHORIZED);
        }
        rootNode.put("access", acc.toString());
        if (acc instanceof AccessInfoAuthImpl) {
            List<String> personalAccessL = RdfAuthModel.getPersonalAccess(RdfConstants.AUTH_RESOURCE_BASE + ((AccessInfoAuthImpl) acc).getUser().getUserId());
            if (personalAccessL == null) {
                rootNode.putNull("personalAccess");
            } else {
                ArrayNode an = mapper.valueToTree(personalAccessL);
                rootNode.putArray("personalAccess").addAll(an);
            }
        }
        final String ipAddress = request.getHeader(GeoLocation.HEADER_NAME);
        rootNode.put("ip", ipAddress);
        rootNode.put("subscriber", Subscribers.getCachedSubscriber(ipAddress));
        String test = GeoLocation.getCountryCode(ipAddress);
        rootNode.put("inChina", test == null || "CN".equalsIgnoreCase(test));
        rootNode.put("isAdmin", acc.isAdmin());
        rootNode.put("isEditor", acc.isAdmin());
        rootNode.put("isContributor", acc.isContributor());
        return new ResponseEntity<StreamingResponseBody>(IIIFImageApiController.streamingResponseFrom(mapper.writeValueAsString(rootNode)),
                headers, HttpStatus.OK);
        
    }
    
}
