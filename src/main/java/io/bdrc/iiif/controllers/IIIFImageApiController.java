package io.bdrc.iiif.controllers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.time.Instant;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.imaging.ImageReadException;
import org.apache.http.client.ClientProtocolException;
import org.apache.jena.atlas.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.digitalcollections.iiif.model.PropertyValue;
import de.digitalcollections.model.api.identifiable.resource.exceptions.ResourceNotFoundException;
import io.bdrc.auth.Access;
import io.bdrc.auth.AuthProps;
import io.bdrc.auth.TokenValidation;
import io.bdrc.auth.rdf.RdfAuthModel;
import io.bdrc.iiif.auth.AuthServiceInfo;
import io.bdrc.iiif.auth.ResourceAccessValidation;
import io.bdrc.iiif.core.Application;
import io.bdrc.iiif.core.EHServerCache;
import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.iiif.exceptions.InvalidParametersException;
import io.bdrc.iiif.exceptions.UnsupportedFormatException;
import io.bdrc.iiif.image.service.ImageProviderService;
import io.bdrc.iiif.image.service.ImageService;
import io.bdrc.iiif.image.service.ReadImageProcess;
import io.bdrc.iiif.image.service.WriteImageProcess;
import io.bdrc.iiif.metrics.ImageMetrics;
import io.bdrc.iiif.metrics.JVMMetrics;
import io.bdrc.iiif.model.DecodedImage;
import io.bdrc.iiif.model.ImageApiProfile;
import io.bdrc.iiif.model.ImageApiProfile.Format;
import io.bdrc.iiif.model.ImageApiProfile.Quality;
import io.bdrc.iiif.model.ImageApiSelector;
import io.bdrc.iiif.model.ImageReader_ICC;
import io.bdrc.iiif.model.RegionRequest;
import io.bdrc.iiif.model.SizeRequest;
import io.bdrc.iiif.model.TileInfo;
import io.bdrc.iiif.resolver.AccessType;
import io.bdrc.iiif.resolver.IdentifierInfo;
import io.bdrc.iiif.resolver.ImageInfo;

@RestController
@Component
@RequestMapping("/")
public class IIIFImageApiController {

    @Autowired
    private AuthServiceInfo serviceInfo;

    private static final Logger log = LoggerFactory.getLogger(IIIFImageApiController.class);

    @RequestMapping(value = "/setcookie")
    ResponseEntity<String> getCookie(HttpServletRequest req, HttpServletResponse response)
            throws JsonProcessingException, UnsupportedEncodingException {
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
    public ResponseEntity<byte[]> getImageRepresentation(@PathVariable String identifier, @PathVariable String region,
            @PathVariable String size, @PathVariable String rotation, @PathVariable String quality,
            @PathVariable String format, HttpServletRequest request, HttpServletResponse response,
            WebRequest webRequest)
            throws ClientProtocolException, IOException, IIIFException, InvalidParametersException,
            UnsupportedOperationException, UnsupportedFormatException, ResourceNotFoundException, ImageReadException {
        log.info("main endpoint getImageRepresentation() for id {}", identifier);
        long deb = System.currentTimeMillis();
        long maxAge = Long.parseLong(Application.getProperty("maxage"));
        boolean staticImg = false;
        String path = request.getServletPath();
        if (request.getPathInfo() != null) {
            path = request.getPathInfo();
        }
        String img = "";
        HttpHeaders headers = new HttpHeaders();
        ImageApiSelector selector = getImageApiSelector(identifier, region, size, rotation, quality, format);
        final ImageApiProfile profile = ImageApiProfile.LEVEL_TWO;
        // TODO: the first part seems ignored?
        // ImageService info = new ImageService("https://iiif.bdrc.io/" +
        // identifier,
        // profile);
        headers.setContentType(MediaType.parseMediaType(selector.getFormat().getMimeType().getTypeName()));
        headers.set("Content-Disposition",
                "inline; filename=" + path.replaceFirst("/image/", "").replace('/', '_').replace(',', '_'));
        headers.add("Link", String.format("<%s>;rel=\"profile\"", profile.getIdentifier().toString()));
        if (identifier.split("::").length > 1) {
            img = identifier.split("::")[1];
            staticImg = identifier.split("::")[0].trim().equals("static");
        }
        ResourceAccessValidation accValidation = null;
        IdentifierInfo idi = null;
        if (!staticImg) {
            idi = new IdentifierInfo(identifier);
            accValidation = new ResourceAccessValidation((Access) request.getAttribute("access"), idi, img);
            log.info("Access Validation is {} and is Accessible={}", accValidation,
                    accValidation.isAccessible(request));
            identifier = URLDecoder.decode(identifier, "UTF-8");
            if (!accValidation.isAccessible(request)) {
                HttpHeaders headers1 = new HttpHeaders();
                headers1.setCacheControl(CacheControl.noCache());
                if (serviceInfo.authEnabled() && serviceInfo.hasValidProperties()) {
                    return new ResponseEntity<>("You must be authenticated before accessing this resource".getBytes(),
                            headers1, HttpStatus.UNAUTHORIZED);
                } else {
                    return new ResponseEntity<>("Insufficient rights".getBytes(), headers1, HttpStatus.FORBIDDEN);
                }
            }
            headers.setCacheControl(CacheControl.maxAge(maxAge, TimeUnit.MILLISECONDS).cachePrivate());
        } else {
            headers.setCacheControl(CacheControl.maxAge(maxAge, TimeUnit.MILLISECONDS).cachePublic());
        }
        if (idi != null) {
            if (idi.igi.access.equals(AccessType.OPEN)) {
                headers.setCacheControl(CacheControl.maxAge(maxAge, TimeUnit.MILLISECONDS).cachePublic());
            } else {
                headers.setCacheControl(CacheControl.maxAge(maxAge, TimeUnit.MILLISECONDS).cachePrivate());
            }
        }

        long deb1 = System.currentTimeMillis();

        // Now a shortcut:

        if (!requestDiffersFromOriginal(identifier, selector)) {
            // let's get our hands dirty
            final String s3key;
            final ImageProviderService service;
            if (identifier.startsWith("static::")) {
                s3key = identifier.substring(8);
                service = ImageProviderService.InstanceStatic;
            } else {
                s3key = ImageProviderService.getKey(idi);
                service = ImageProviderService.InstanceArchive;
            }
            byte[] bytes = null;
            try {
                bytes = service.getAsync(s3key).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new IIIFException(404, 5000, e);
            }
            Application.logPerf("got the bytes in {} ms for {}", (System.currentTimeMillis() - deb1), identifier);
            ImageMetrics.imageCount(ImageMetrics.IMG_CALLS_COMMON, (String) request.getAttribute("origin"));
            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);

        }
        deb1 = System.currentTimeMillis();
        ByteArrayOutputStream os = null;
        ImageReader_ICC imgReader = null;
        try {
            // imgReader = ReadImageProcess.readImageInfo(identifier, info,
            // null,false);
            Application.logPerf("end reading from image service after {} ms for {} with reader {}",
                    (System.currentTimeMillis() - deb1), identifier, imgReader);
            // final String canonicalForm = idi.getCanonical();
            // TODO: make it actually canonical...
            // headers.add("Link", String.format("<%s>;rel=\"canonical\"",
            // Application.getProperty("iiifserv_baseurl") + canonicalForm));
            deb1 = System.currentTimeMillis();
            Application.logPerf("processing image output stream for {}", identifier);
            os = new ByteArrayOutputStream();
            Object[] obj = ReadImageProcess.readImage(identifier, selector, profile, false);
            DecodedImage decImg = (DecodedImage) obj[0];
            imgReader = (ImageReader_ICC) obj[1];
            WriteImageProcess.processImage(decImg, identifier, selector, profile, os, imgReader);
            Application.logPerf("ended processing image after {} ms for {}", (System.currentTimeMillis() - deb1),
                    identifier);
            Application.logPerf("Total request time {} ms ", (System.currentTimeMillis() - deb), identifier);
            imgReader.getReader().dispose();
            ImageMetrics.imageCount(ImageMetrics.IMG_CALLS_COMMON, (String) request.getAttribute("origin"));
        } catch (Exception e) {
            log.error("Resource was not found for identifier " + identifier + " Message: " + e.getMessage()
                    + " Trying failover method");
            try {
                os = new ByteArrayOutputStream();
                Object[] obj = ReadImageProcess.readImage(identifier, selector, profile, true);
                DecodedImage decImg = (DecodedImage) obj[0];
                imgReader = (ImageReader_ICC) obj[1];
                WriteImageProcess.processImage(decImg, identifier, selector, profile, os, imgReader);
                Application.logPerf("ended processing image after {} ms for {}", (System.currentTimeMillis() - deb1),
                        identifier);
                Application.logPerf("Total request time {} ms ", (System.currentTimeMillis() - deb), identifier);
                imgReader.getReader().dispose();
                ImageMetrics.imageCount(ImageMetrics.IMG_CALLS_COMMON, (String) request.getAttribute("origin"));
            } catch (Exception ex) {
                log.error("Somethng WENT WRONG ");
                ex.printStackTrace();
                return new ResponseEntity<>(("Resource was not found for identifier " + identifier).getBytes(),
                        HttpStatus.NOT_FOUND);
            }
        }
        return new ResponseEntity<>(os.toByteArray(), headers, HttpStatus.OK);
    }

    public static boolean pngOutput(final String filename) {
        final String ext = filename.substring(filename.length() - 4).toLowerCase();
        return (ext.equals(".tif") || ext.equals("tiff"));
    }

    public static final PropertyValue pngHint = new PropertyValue("png", "jpg");

    @RequestMapping(value = "{identifier}/info.json", method = {RequestMethod.GET, RequestMethod.HEAD})
    public ResponseEntity<String> getInfo(@PathVariable String identifier, HttpServletRequest req,
            HttpServletResponse res, WebRequest webRequest) throws ClientProtocolException, IOException, IIIFException,
            UnsupportedOperationException, UnsupportedFormatException, InterruptedException, ExecutionException {
        log.info("{identifier}/info.json endpoint getInfo() for id {}", identifier);
        long deb = System.currentTimeMillis();
        long maxAge = Long.parseLong(Application.getProperty("maxage"));
        ObjectMapper objectMapper = new ObjectMapper();
        String img = "";
        boolean staticImg = false;
        if (identifier.split("::").length > 1) {
            img = identifier.split("::")[1];
            staticImg = identifier.split("::")[0].trim().equals("static");
        }
        log.info("Entering endpoint getInfo for {}", identifier);
        boolean unAuthorized = false;
        IdentifierInfo idi = new IdentifierInfo(identifier);
        ImageService info = new ImageService(Application.getProperty("iiifserv_baseurl") + identifier);
        ImageInfo imgInf = idi.getImageInfo(idi.imageName);
        if (imgInf == null) {
            log.error("couldn't find {} in image list");
            return new ResponseEntity<>("Resource was not found (image not listed) for identifier " + identifier,
                    HttpStatus.NOT_FOUND);
        }
        updateInfo(imgInf, info);
        if (!staticImg) {
            ResourceAccessValidation accValidation = null;
            accValidation = new ResourceAccessValidation((Access) req.getAttribute("access"), idi, img);
            unAuthorized = !accValidation.isAccessible(req);
        }
        if (unAuthorized && serviceInfo.authEnabled() && serviceInfo.hasValidProperties()) {
            info.addService(serviceInfo);
        }
        if (pngOutput(identifier)) {
            info.setPreferredFormats(pngHint);
        }
        HttpHeaders headers = new HttpHeaders();
        try {
            headers.setDate("Last-Modified", getImageModificationDate(identifier).toEpochMilli());
        } catch (IIIFException e) {
            log.error("Resource was not found for identifier " + identifier + " Message: " + e.getMessage());
            return new ResponseEntity<>("Resource was not found for identifier " + identifier,
                    HttpStatus.NOT_FOUND);
        }
        if ("application/ld+json".equals(req.getHeader("Accept"))) {
            headers.set("Content-Type", req.getHeader("Accept"));
        } else {
            headers.set("Content-Type", "application/json");
            headers.add("Link", "<http://iiif.io/api/image/2/context.json>; "
                    + "rel=\"http://www.w3.org/ns/json-ld#context\"; " + "type=\"application/ld+json\"");
        }
        headers.add("Link",
                String.format("<%s>;rel=\"profile\"", info.getProfiles().get(0).getIdentifier().toString()));
        // We set the header ourselves, since using @CrossOrigin doesn't
        // expose "*", but
        // always sets the requesting domain
        // headers.add("Access-Control-Allow-Origin", "*");
        Application.logPerf("getInfo ready to return after {} ms for {}", (System.currentTimeMillis() - deb),
                identifier);
        if (unAuthorized) {
            if (serviceInfo.hasValidProperties() && serviceInfo.authEnabled()) {
                return new ResponseEntity<>(objectMapper.writeValueAsString(info), headers,
                        HttpStatus.UNAUTHORIZED);
            } else {
                headers.setCacheControl(CacheControl.noCache());
                return new ResponseEntity<>(objectMapper.writeValueAsString(info), headers, HttpStatus.FORBIDDEN);
            }
        } else {
            if (idi.igi.access.equals(AccessType.OPEN)) {
                headers.setCacheControl(CacheControl.maxAge(maxAge, TimeUnit.MILLISECONDS).cachePublic());
            } else {
                headers.setCacheControl(CacheControl.maxAge(maxAge, TimeUnit.MILLISECONDS).cachePrivate());
            }
            return new ResponseEntity<>(objectMapper.writeValueAsString(info), headers, HttpStatus.OK);
        }
    }

    @RequestMapping(value = "{identifier}", method = {RequestMethod.GET, RequestMethod.HEAD})
    public void getInfoRedirect(@PathVariable String identifier, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        if (!identifier.startsWith("bdr:")) {
            log.info("Ignoring request to {}", identifier);
            response.sendError(HttpStatus.NOT_FOUND.value(), "couldn't find the asked resource");
            return;
        }
        String base = Application.getProperty("iiifserv_baseurl");
        log.info("Identifier endpoint getInfoRedirect {} , {}", identifier, base);
        response.sendRedirect(base + identifier + "/info.json");
    }

    @RequestMapping(value = "cache/clear", method = RequestMethod.POST)
    ResponseEntity<String> clearCache(HttpServletRequest req, HttpServletResponse response) {
        log.info("cache/clear endpoint clearCache()");
        ResponseEntity<String> resp = null;
        if (EHServerCache.clearCache()) {
            resp = new ResponseEntity<>("OK", HttpStatus.OK);
        } else {
            resp = new ResponseEntity<>("ERROR", HttpStatus.OK);
        }
        return resp;
    }

    @RequestMapping(value = "cache/view", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getCacheInfo() {
        log.info("Call to getCacheInfo()");
        ModelAndView model = new ModelAndView();
        JVMMetrics cam = new JVMMetrics();
        model.addObject("model", cam);
        model.setViewName("cache");
        return model;
    }

    @RequestMapping(value = "/callbacks/model/bdrc-auth", method = RequestMethod.POST, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> readAuthModel() {
        log.info("updating Auth data model() >>");
        RdfAuthModel.readAuthModel();
        return ResponseEntity.ok("Updated auth Model was read into IIIF serv");
    }

    public int computeExpires(TokenValidation tkVal) {
        long expires = tkVal.getVerifiedJwt().getExpiresAt().toInstant().getEpochSecond();
        long current = Calendar.getInstance().getTime().toInstant().getEpochSecond();
        return (int) (expires - current);
    }

    private String getToken(String header) {
        try {
            if (header != null) {
                return header.split(" ")[1];
            }
        } catch (Exception ex) {
            log.error("Could not get the token from header " + header + " Message: " + ex.getMessage());
            return null;
        }
        return null;
    }

    public ImageApiSelector getImageApiSelector(String identifier, String region, String size, String rotation,
            String quality, String format) throws InvalidParametersException {
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
        } catch (IIIFException e) {
            log.error("ImageApiSelector could not be obtained; Message:" + e.getMessage());
            throw new InvalidParametersException(e);
        }
        return selector;
    }

    // here we return a boolean telling us if the requested image is different
    // from
    // the original image
    // on S3
    public static boolean requestDiffersFromOriginal(final String identifier, final ImageApiSelector selector) {
        if (formatDiffer(identifier, selector))
            return true;
        if (selector.getQuality() != Quality.DEFAULT) // TODO: this could be
                                                      // improved but we can
                                                      // keep that for later
            return true;
        if (selector.getRotation().getRotation() != 0.)
            return true;
        if (!selector.getRegion().equals(new RegionRequest())) // TODO: same
                                                               // here, could be
                                                               // improved by
                                                               // reading the
                                                               // dimensions of
                                                               // the image
            return true;
        if (!selector.getSize().equals(new SizeRequest()) && !selector.getSize().equals(new SizeRequest(true)))
            return true;
        return false;
    }

    public static boolean formatDiffer(final String identifier, final ImageApiSelector selector) {
        final Format outputF = selector.getFormat();
        final String lastFour = identifier.substring(identifier.length() - 4).toLowerCase();
        if (outputF == Format.JPG && (lastFour.equals(".jpg") || lastFour.equals("jpeg")))
            return false;
        if (outputF == Format.PNG && lastFour.equals(".png"))
            return false;
        if (outputF == Format.TIF && (lastFour.equals(".tif") || lastFour.equals("tiff")))
            return false;
        return true;
    }

    public static Instant getImageModificationDate(String identifier) throws IIIFException {
        try {
            return Instant.ofEpochMilli(-1);
        } catch (Exception e) {
            Log.error("Could not get Image modification date from resource for identifier {}", identifier);
            throw new IIIFException("Could not get Image modification date from resource for identifier " + identifier);
        }
    }

    private ImageService updateInfo(ImageInfo imgInf, ImageService info) {
        info.setWidth(imgInf.getWidth());
        info.setHeight(imgInf.getHeight());
        ImageApiProfile profile = new ImageApiProfile();
        profile.addFeature(ImageApiProfile.Feature.BASE_URI_REDIRECT, ImageApiProfile.Feature.CORS,
                ImageApiProfile.Feature.JSONLD_MEDIA_TYPE, ImageApiProfile.Feature.PROFILE_LINK_HEADER,
                ImageApiProfile.Feature.CANONICAL_LINK_HEADER, ImageApiProfile.Feature.REGION_BY_PCT,
                ImageApiProfile.Feature.REGION_BY_PX, ImageApiProfile.Feature.REGION_SQUARE,
                ImageApiProfile.Feature.ROTATION_BY_90S, ImageApiProfile.Feature.MIRRORING,
                ImageApiProfile.Feature.SIZE_BY_CONFINED_WH, ImageApiProfile.Feature.SIZE_BY_DISTORTED_WH,
                ImageApiProfile.Feature.SIZE_BY_H, ImageApiProfile.Feature.SIZE_BY_PCT,
                ImageApiProfile.Feature.SIZE_BY_W, ImageApiProfile.Feature.SIZE_BY_WH);
        info.addProfile(ImageApiProfile.LEVEL_ONE, profile);
        TileInfo tile = new TileInfo(info.getWidth());
        tile.setHeight(info.getHeight());
        if (imgInf.size == null || imgInf.size < 1000000) {
            tile.addScaleFactor(1);
        } else {
            tile.addScaleFactor(1, 2, 4);
        }
        info.addTile(tile);
        return info;
    }
}
