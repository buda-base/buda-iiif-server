package de.digitalcollections.iiif.hymir.image.frontend;

import java.awt.Dimension;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageReader;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.WebRequest;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.digitalcollections.core.business.api.ResourceService;
import de.digitalcollections.core.model.api.MimeType;
import de.digitalcollections.core.model.api.resource.Resource;
import de.digitalcollections.core.model.api.resource.enums.ResourcePersistenceType;
import de.digitalcollections.iiif.hymir.model.exception.InvalidParametersException;
import de.digitalcollections.iiif.hymir.model.exception.ResourceNotFoundException;
import de.digitalcollections.iiif.hymir.model.exception.UnsupportedFormatException;
import de.digitalcollections.iiif.model.PropertyValue;
import de.digitalcollections.iiif.model.image.ImageApiProfile;
import de.digitalcollections.iiif.model.image.ImageApiSelector;
import de.digitalcollections.iiif.model.image.ImageService;
import de.digitalcollections.iiif.model.image.ResolvingException;
import de.digitalcollections.iiif.model.jackson.IiifObjectMapper;
import de.digitalcollections.iiif.myhymir.Application;
import de.digitalcollections.iiif.myhymir.ResourceAccessValidation;
import de.digitalcollections.iiif.myhymir.ServerCache;
import de.digitalcollections.iiif.myhymir.image.business.BDRCImageServiceImpl;
import io.bdrc.auth.Access;
import io.bdrc.auth.AuthProps;
import io.bdrc.auth.TokenValidation;
import io.bdrc.iiif.auth.AuthServiceInfo;
import io.bdrc.iiif.resolver.IdentifierInfo;
import io.bdrc.pdf.presentation.exceptions.BDRCAPIException;

@Controller
// @RequestMapping("/image/v2/")
@RequestMapping("/")
public class IIIFImageApiController {

    public static final String IIIF_IMG = "IIIF_IMG";

    // public static final String VERSION = "v2";

    @Autowired
    private BDRCImageServiceImpl imageService;

    @Autowired
    private AuthServiceInfo serviceInfo;

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private IiifObjectMapper objectMapper;

    @Value("${cache-control.maxage}")
    private long maxAge;

    private static final Logger log = LoggerFactory.getLogger(IIIFImageApiController.class);

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

    @RequestMapping(value = "/setcookie")
    ResponseEntity<String> getCookie(HttpServletRequest req, HttpServletResponse response) throws JsonProcessingException, UnsupportedEncodingException {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        ResponseEntity<String> resp = null;
        boolean valid = false;
        String token = getToken(req.getHeader("Authorization"));
        if (token == null) {
            Cookie[] cks = req.getCookies();
            if (cks == null) {
                return new ResponseEntity<>("{\"success\":" + false + "}", headers, HttpStatus.BAD_REQUEST);
            }
            for (Cookie ck : cks) {
                if (ck.getName().equals(AuthProps.getProperty("cookieKey"))) {
                    // invalidates cookie if present and token is null
                    ck.setMaxAge(0);
                    response.addCookie(ck);
                    return new ResponseEntity<>("{\"success\":" + true + "}", headers, HttpStatus.OK);
                }
            }
            return new ResponseEntity<>("{\"success\":" + valid + "}", headers, HttpStatus.FORBIDDEN);
        }
        TokenValidation tkVal = new TokenValidation(token);
        valid = tkVal.isValid();
        if (valid) {
            Cookie c = new Cookie(AuthProps.getProperty("cookieKey"), URLEncoder.encode(token, "UTF-8"));
            // c.setSecure(true);
            c.setMaxAge(computeExpires(tkVal));
            c.setHttpOnly(true);
            response.addCookie(c);
            resp = new ResponseEntity<>("{\"success\":" + valid + "}", headers, HttpStatus.OK);
        } else {
            resp = new ResponseEntity<>("{\"success\":" + valid + "}", headers, HttpStatus.FORBIDDEN);
        }
        return resp;
    }

    @RequestMapping(value = "{identifier}/{region}/{size}/{rotation}/{quality}.{format}")
    public ResponseEntity<byte[]> getImageRepresentation(@PathVariable String identifier, @PathVariable String region, @PathVariable String size, @PathVariable String rotation, @PathVariable String quality, @PathVariable String format,
            HttpServletRequest request, HttpServletResponse response, WebRequest webRequest) throws UnsupportedFormatException, UnsupportedOperationException, IOException, InvalidParametersException, ResourceNotFoundException, BDRCAPIException {
        long deb = System.currentTimeMillis();
        String img = "";
        if (identifier.split("::").length > 1) {
            img = identifier.split("::")[1];
        }
        ResourceAccessValidation accValidation = new ResourceAccessValidation((Access) request.getAttribute("access"), IdentifierInfo.getIndentifierInfo(identifier), img);
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
        String path = request.getServletPath();
        if (request.getPathInfo() != null) {
            path = request.getPathInfo();
        }
        if (accValidation.isOpenAccess()) {
            headers.setCacheControl(CacheControl.maxAge(maxAge, TimeUnit.MILLISECONDS).cachePublic());
        } else {
            headers.setCacheControl(CacheControl.maxAge(maxAge, TimeUnit.MILLISECONDS).cachePrivate());
        }
        webRequest.checkNotModified(imageService.getImageModificationDate(identifier).toEpochMilli());
        headers.setDate("Last-Modified", imageService.getImageModificationDate(identifier).toEpochMilli());
        ImageApiSelector selector = getImageApiSelector(identifier, region, size, rotation, quality, format);
        long deb1 = System.currentTimeMillis();
        final ImageApiProfile profile = ImageApiProfile.LEVEL_TWO;
        ImageService info = new ImageService("http://foo.org/" + identifier, profile);
        // TODO: what's this foo.org thing?
        headers.setContentType(MediaType.parseMediaType(selector.getFormat().getMimeType().getTypeName()));
        headers.set("Content-Disposition", "inline; filename=" + path.replaceFirst("/image/", "").replace('/', '_').replace(',', '_'));
        headers.add("Link", String.format("<%s>;rel=\"profile\"", profile.getIdentifier().toString()));
        // Now a shortcut:
        if (!BDRCImageServiceImpl.requestDiffersFromOriginal(identifier, selector)) {
            // let's get our hands dirty
            final Resource res = resourceService.get(identifier, ResourcePersistenceType.RESOLVED, MimeType.MIME_IMAGE);
            byte[] osbytes = (byte[]) ServerCache.getObjectFromCache(IIIF_IMG, identifier);
            if (osbytes == null) {
                final InputStream input = resourceService.getInputStream(res);
                Application.perf.debug("got the S3 inputstream in {} ms for {}", (System.currentTimeMillis() - deb1), identifier);
                osbytes = StreamUtils.copyToByteArray(input);
            } else {
                Application.perf.debug("Not different from original, got byes from cache for {}", identifier);
            }
            Application.perf.debug("got the bytes in {} ms for {}", (System.currentTimeMillis() - deb1), identifier);
            return new ResponseEntity<>(osbytes, headers, HttpStatus.OK);
        }
        deb1 = System.currentTimeMillis();
        final ImageReader imgReader = imageService.readImageInfo(identifier, info, null);
        Application.perf.debug("end reading from image service after {} ms for {} with reader {}", (System.currentTimeMillis() - deb1), identifier, imgReader);
        final String canonicalForm;
        try {
            canonicalForm = selector.getCanonicalForm(new Dimension(info.getWidth(), info.getHeight()), profile, selector.getQuality());
        } catch (ResolvingException e) {
            throw new InvalidParametersException(e);
        }
        headers.add("Link", String.format("<%s>;rel=\"canonical\"", getUrlBase(request) + path.substring(0, path.indexOf(identifier)) + canonicalForm));
        deb1 = System.currentTimeMillis();
        Application.perf.debug("processing image output stream for {}", identifier);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        imageService.processImage(identifier, selector, profile, os, imgReader, request.getRequestURI());
        Application.perf.debug("ended processing image after {} ms for {}", (System.currentTimeMillis() - deb1), identifier);
        Application.perf.debug("Total request time {} ms ", (System.currentTimeMillis() - deb), identifier);
        imgReader.dispose();
        return new ResponseEntity<>(os.toByteArray(), headers, HttpStatus.OK);
    }

    public static boolean pngOutput(final String filename) {
        final String ext = filename.substring(filename.length() - 4).toLowerCase();
        return (ext.equals(".tif") || ext.equals("tiff"));
    }

    public static final PropertyValue pngHint = new PropertyValue("png", "jpg");

    @RequestMapping(value = "{identifier}/info.json", method = { RequestMethod.GET, RequestMethod.HEAD })
    public ResponseEntity<String> getInfo(@PathVariable String identifier, HttpServletRequest req, HttpServletResponse res, WebRequest webRequest) throws Exception {
        long deb = System.currentTimeMillis();
        String img = "";
        if (identifier.split("::").length > 1) {
            img = identifier.split("::")[1];
        }
        Application.perf.debug("Entering endpoint getInfo for {}", identifier);
        ResourceAccessValidation accValidation = new ResourceAccessValidation((Access) req.getAttribute("access"), IdentifierInfo.getIndentifierInfo(identifier), img);
        boolean unAuthorized = !accValidation.isAccessible(req);
        webRequest.checkNotModified(imageService.getImageModificationDate(identifier).toEpochMilli());
        String path = req.getServletPath();
        ;
        if (req.getPathInfo() != null) {
            path = req.getPathInfo();
        }
        final BDRCImageService info = new BDRCImageService(getUrlBase(req) + path.replace("/info.json", ""));
        if (unAuthorized && serviceInfo.authEnabled() && serviceInfo.hasValidProperties()) {
            info.addService(serviceInfo);
        }
        if (pngOutput(identifier)) {
            info.setFormatHints(pngHint);
        }
        Application.perf.debug("getInfo read ImageInfo for {}", identifier);
        imageService.readImageInfo(identifier, info, null);
        HttpHeaders headers = new HttpHeaders();
        headers.setDate("Last-Modified", imageService.getImageModificationDate(identifier).toEpochMilli());
        if ("application/ld+json".equals(req.getHeader("Accept"))) {
            headers.set("Content-Type", req.getHeader("Accept"));
        } else {
            headers.set("Content-Type", "application/json");
            headers.add("Link", "<http://iiif.io/api/image/2/context.json>; " + "rel=\"http://www.w3.org/ns/json-ld#context\"; " + "type=\"application/ld+json\"");
        }
        headers.add("Link", String.format("<%s>;rel=\"profile\"", info.getProfiles().get(0).getIdentifier().toString()));
        // We set the header ourselves, since using @CrossOrigin doesn't expose "*", but
        // always sets the requesting domain
        // headers.add("Access-Control-Allow-Origin", "*");
        Application.perf.debug("getInfo ready to return after {} ms for {}", (System.currentTimeMillis() - deb), identifier);
        if (unAuthorized) {
            if (serviceInfo.hasValidProperties() && serviceInfo.authEnabled()) {
                headers.setCacheControl(CacheControl.maxAge(maxAge, TimeUnit.MILLISECONDS).cachePublic());
                return new ResponseEntity<>(objectMapper.writeValueAsString(info), headers, HttpStatus.UNAUTHORIZED);
            } else {
                headers.setCacheControl(CacheControl.noCache());
                return new ResponseEntity<>(objectMapper.writeValueAsString(info), headers, HttpStatus.FORBIDDEN);
            }
        } else {
            headers.setCacheControl(CacheControl.maxAge(maxAge, TimeUnit.MILLISECONDS).cachePublic());
            return new ResponseEntity<>(objectMapper.writeValueAsString(info), headers, HttpStatus.OK);
        }
    }

    @RequestMapping(value = "{identifier}", method = { RequestMethod.GET, RequestMethod.HEAD })
    public String getInfoRedirect(@PathVariable String identifier, HttpServletResponse response) {
        // response.setHeader("Access-Control-Allow-Origin", "*");
        // return "redirect:/image/" + VERSION + "/" + identifier + "/info.json";
        return "redirect:/" + identifier + "/info.json";
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

    public ImageApiSelector getImageApiSelector(String identifier, String region, String size, String rotation, String quality, String format) throws InvalidParametersException {
        ImageApiSelector selector = new ImageApiSelector();
        try {
            selector.setIdentifier(identifier);
            selector.setRegion(region);
            selector.setSize(size);
            selector.setRotation(rotation);
            if (quality.equals("native")) {
                quality = "default";
            }
            selector.setQuality(ImageApiProfile.Quality.valueOf(quality.toUpperCase()));
            selector.setFormat(ImageApiProfile.Format.valueOf(format.toUpperCase()));
        } catch (ResolvingException e) {
            throw new InvalidParametersException(e);
        }
        return selector;
    }
}
