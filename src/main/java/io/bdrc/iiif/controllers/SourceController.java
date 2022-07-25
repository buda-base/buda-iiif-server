package io.bdrc.iiif.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import io.bdrc.auth.AccessInfo;
import io.bdrc.auth.AccessInfo.AccessLevel;
import io.bdrc.iiif.auth.AuthServiceInfo;
import io.bdrc.iiif.auth.ResourceAccessValidation;
import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.iiif.exceptions.UnsupportedFormatException;
import io.bdrc.iiif.image.service.ImageProviderService;
import io.bdrc.iiif.image.service.SimpleBVMService;
import io.bdrc.iiif.model.SimpleBVM;
import io.bdrc.iiif.resolver.AppConstants;
import io.bdrc.iiif.resolver.IdentifierInfo;

@Controller
public class SourceController {
    
    private static final Logger log = LoggerFactory.getLogger(SourceController.class);
    
    @Autowired
    private AuthServiceInfo serviceInfo;
    
    @RequestMapping(value = "sourcefile/{identifier}", method = {RequestMethod.GET, RequestMethod.HEAD})
    public ResponseEntity<StreamingResponseBody> getSourceFile(@PathVariable String identifier, HttpServletRequest req,
            HttpServletResponse res, WebRequest webRequest) throws ClientProtocolException, IOException, IIIFException,
            UnsupportedOperationException, UnsupportedFormatException, InterruptedException, ExecutionException {
        log.info("sourcefile/{identifier} endpoint getInfo() for id {}", identifier);
        String img = "";
        if (identifier.split("::").length > 1) {
            img = identifier.split("::")[1];
        }
        log.info("Entering endpoint getInfo for {}", identifier);
        IdentifierInfo idi = new IdentifierInfo(identifier);
        final AccessInfo acc = (AccessInfo) req.getAttribute("access");
        final ResourceAccessValidation accValidation = new ResourceAccessValidation(acc, idi);
        final AccessLevel al = accValidation.getAccessLevel(req);
        if (serviceInfo.authEnabled() && (al.equals(AccessLevel.NOACCESS) || al.equals(AccessLevel.MIXED) || al.equals(AccessLevel.FAIR_USE))) {
            final HttpStatus st = acc.isLogged() ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED;
            return new ResponseEntity<>(null, st);
        }
        final SimpleBVMService bvmService = SimpleBVMService.Instance;
        final SimpleBVM bvm = bvmService.getAsync(idi.volumeId.substring(4)).get();
        if (bvm == null) {
            log.error("couldn't find bvm for {}", idi.volumeId);
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
        final String sourcePath = bvm.getSourcePathForFn(img);
        if (sourcePath == null) {
            log.error("couldn't find source path for {} in bvm for {}", img, idi.volumeId);
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
        final String w_lname = idi.igi.imageInstanceUri.substring(AppConstants.BDR_len);
        final String s3key = ImageProviderService.getSourcesPrefix(w_lname)+sourcePath;
        final String mimeType = URLConnection.guessContentTypeFromName(s3key);
        final ImageProviderService service = ImageProviderService.InstanceArchive;
        final InputStream is = service.getNoCache(s3key);
        final HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", mimeType);
        final String userfilename = sourcePath.substring(sourcePath.lastIndexOf('/')+1);
        headers.setContentDispositionFormData("attachment", userfilename);
        return ResponseEntity.ok().headers(headers).body(IIIFImageApiController.streamingResponseFrom(is));
    }
}
