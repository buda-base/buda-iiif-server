package io.bdrc.iiif;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.WebRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.digitalcollections.iiif.hymir.model.exception.InvalidParametersException;
import de.digitalcollections.iiif.hymir.model.exception.UnsupportedFormatException;
import io.bdrc.auth.Access;
import io.bdrc.auth.TokenValidation;
import io.bdrc.iiif.auth.AuthServiceInfo;
import io.bdrc.iiif.core.ResourceAccessValidation;
import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.iiif.image.BDRCImageServiceImpl;
import io.bdrc.iiif.model.ImageService;
import io.bdrc.iiif.resolver.AccessType;
import io.bdrc.iiif.resolver.IdentifierInfo;
import io.bdrc.iiif.resolver.ImageGroupInfo;

@Controller
@RequestMapping("/test/v2/")
public class IIIFImageTestApiController {

    public static final String VERSION = "v2";

    @Autowired
    private AuthServiceInfo serviceInfo;

    @Value("${cache-control.maxage}")
    private long maxAge;

    /**
     * Get the base URL for all Image API URLs from the request.
     *
     * This will handle cases such as reverse-proxying and SSL-termination on the
     * frontend server
     */
    private String getUrlBase(HttpServletRequest request) {
        String scheme = request.getHeader("X-Forwarded-Proto");
        if (scheme == null) {
            scheme = request.getScheme();
        }

        String host = request.getHeader("X-Forwarded-Host");
        if (host == null) {
            host = request.getHeader("Host");
        }
        if (host == null) {
            host = request.getRemoteHost();
        }
        String base = String.format("%s://%s", scheme, host);
        if (!request.getContextPath().isEmpty()) {
            base += request.getContextPath();
        }
        return base;
    }

    @RequestMapping(value = "{identifier}/{region}/{size}/{rotation}/{quality}.{format}")
    public ResponseEntity<byte[]> getImageRepresentation(@PathVariable String identifier, @PathVariable String region, @PathVariable String size,
            @PathVariable String rotation, @PathVariable String quality, @PathVariable String format, HttpServletRequest request,
            HttpServletResponse response, WebRequest webRequest)
            throws UnsupportedFormatException, UnsupportedOperationException, IOException, InvalidParametersException, IIIFException {
        String pth = "/test/v2/" + identifier + "/" + region + "/" + size + "/" + rotation + "/" + quality + "." + format;
        ImageGroupInfo igi = new ImageGroupInfo();
        igi.access = AccessType.OPEN;
        igi.restrictedInChina = false;
        igi.statusUri = "http://purl.bdrc.io/admindata/StatusReleased";
        IdentifierInfo idf = new IdentifierInfo(identifier, igi);
        System.out.println("PATH >>>>> " + pth);
        ResourceAccessValidation accValidation = new ResourceAccessValidation((Access) request.getAttribute("access"), idf);
        identifier = URLDecoder.decode(identifier, "UTF-8");
        if (!accValidation.isAccessible(request)) {
            HttpHeaders headers1 = new HttpHeaders();
            headers1.setCacheControl(CacheControl.noCache());
            if (serviceInfo.authEnabled() && serviceInfo.hasValidProperties()) {
                return new ResponseEntity<>("You must be authenticated before accessing this resource".getBytes(), headers1, HttpStatus.UNAUTHORIZED);
            } else {
                return new ResponseEntity<>("Insufficient rights".getBytes(), headers1, HttpStatus.FORBIDDEN);
            }
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setDate("Last-Modified", 111111111);

        return new ResponseEntity<>("ok".getBytes(), headers, HttpStatus.OK);

    }

    @RequestMapping(value = "{identifier}/info.json", method = { RequestMethod.GET, RequestMethod.HEAD })
    public ResponseEntity<String> getInfo(@PathVariable String identifier, HttpServletRequest req, HttpServletResponse res, WebRequest webRequest)
            throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        String pth = "/test/v2/" + identifier + "/info.json";
        System.out.println("PATH >>>>> " + pth);
        ImageGroupInfo igi = new ImageGroupInfo();
        igi.access = AccessType.OPEN;
        igi.restrictedInChina = false;
        igi.statusUri = "http://purl.bdrc.io/admindata/StatusReleased";
        IdentifierInfo idf = new IdentifierInfo(identifier, igi);
        ResourceAccessValidation accValidation = new ResourceAccessValidation((Access) req.getAttribute("access"), idf);
        boolean unAuthorized = !accValidation.isAccessible(req);
        long modified = BDRCImageServiceImpl.getImageModificationDate(identifier).toEpochMilli();
        webRequest.checkNotModified(modified);
        String path;
        if (req.getPathInfo() != null) {
            path = req.getPathInfo();
        } else {
            path = req.getServletPath();
        }
        String baseUrl = getUrlBase(req);

        ImageService info = new ImageService(baseUrl + path.replace("/info.json", ""));
        if (unAuthorized && serviceInfo.authEnabled() && serviceInfo.hasValidProperties()) {
            info.addService(serviceInfo);
        }
        BDRCImageServiceImpl.readImageInfo(identifier, info, null);
        HttpHeaders headers = new HttpHeaders();
        headers.setDate("Last-Modified", modified);
        String contentType = req.getHeader("Accept");
        if (contentType != null && contentType.equals("application/ld+json")) {
            headers.set("Content-Type", contentType);
        } else {
            headers.set("Content-Type", "application/json");
            headers.add("Link", "<http://iiif.io/api/image/2/context.json>; " + "rel=\"http://www.w3.org/ns/json-ld#context\"; "
                    + "type=\"application/ld+json\"");
        }
        headers.add("Link", String.format("<%s>;rel=\"profile\"", info.getProfiles().get(0).getIdentifier().toString()));
        // We set the header ourselves, since using @CrossOrigin doesn't expose "*", but
        // always sets the requesting domain
        // headers.add("Access-Control-Allow-Origin", "*");
        if (unAuthorized) {
            if (serviceInfo.hasValidProperties() && serviceInfo.authEnabled()) {
                headers.setCacheControl(CacheControl.maxAge(maxAge, TimeUnit.MILLISECONDS).cachePublic());
                return new ResponseEntity<>(objectMapper.writeValueAsString(info), headers, HttpStatus.UNAUTHORIZED);
            } else {
                headers.setCacheControl(CacheControl.noCache());
                return new ResponseEntity<>(objectMapper.writeValueAsString(info), headers, HttpStatus.FORBIDDEN);
            }
        } else {
            HttpHeaders headers1 = new HttpHeaders();
            headers1.setCacheControl(CacheControl.maxAge(maxAge, TimeUnit.MILLISECONDS).cachePublic());
            return new ResponseEntity<>(objectMapper.writeValueAsString(info), headers1, HttpStatus.OK);
        }
    }

    @RequestMapping(value = "{identifier}", method = { RequestMethod.GET, RequestMethod.HEAD })
    public String getInfoRedirect(@PathVariable String identifier, HttpServletResponse response) {
        // response.setHeader("Access-Control-Allow-Origin", "*");
        return "redirect:/image/" + VERSION + "/" + identifier + "/info.json";
    }

    public int computeExpires(TokenValidation tkVal) {
        long expires = tkVal.getVerifiedJwt().getExpiresAt().toInstant().getEpochSecond();
        long current = Calendar.getInstance().getTime().toInstant().getEpochSecond();
        return (int) (expires - current);
    }

    String getToken(String header) {
        try {
            if (header != null) {
                return header.split(" ")[1];
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        return null;
    }
}
