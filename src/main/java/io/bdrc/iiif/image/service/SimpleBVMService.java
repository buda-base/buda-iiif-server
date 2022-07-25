package io.bdrc.iiif.image.service;

import java.io.File;
import java.io.IOException;
import java.time.Instant;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.auth.AuthProps;
import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.iiif.model.SimpleBVM;
import io.bdrc.iiif.resolver.AppConstants;
import io.bdrc.libraries.GitHelpers;
import io.bdrc.libraries.GlobalHelpers;

public class SimpleBVMService extends ConcurrentResourceService<SimpleBVM> {

    private static final Logger logger = LoggerFactory.getLogger(SimpleBVMService.class);
    public static final SimpleBVMService Instance = new SimpleBVMService();
    public final static ObjectMapper om = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static final int pullEveryS = 6000; // pull every x seconds
    public static Instant lastPull = null;

    public static synchronized void pullIfNecessary() throws IIIFException {
        final String repoBase = AuthProps.getProperty("bvmgitpath");
        final Repository repo = GitHelpers.ensureGitRepo(repoBase);
        // pull if not pull has been made for x s
        final Instant now = Instant.now();
        if (lastPull == null || lastPull.isBefore(now.minusSeconds(pullEveryS))) {
            try {
                GitHelpers.pull(repo);
            } catch (GitAPIException e) {
                throw new IIIFException(500, 5000, e);
            }
        }
        lastPull = now;
    }

    SimpleBVMService() {
        super("bvm", AppConstants.CACHEPREFIX_BVM);
    }

    @Override
    public final SimpleBVM getFromApi(final String imageGroupLocalName) throws IIIFException {
        final String firstTwo = GlobalHelpers.getTwoLettersBucket(imageGroupLocalName);
        String filename = AuthProps.getProperty("bvmgitpath") + firstTwo + "/" + imageGroupLocalName + ".json";
        File f = new File(filename);
        if (!f.exists()) {
            logger.debug("bvm file doesn't exist: {}", filename);
            throw new IIIFException(404, 5000, "no BVM file for " + imageGroupLocalName);
        }
        logger.debug("Git filename is {}", filename);
        try {
            return om.readValue(f, SimpleBVM.class);
        } catch (IOException e) {
            logger.error("Error reading bvm file {}", filename, e);
            throw new IIIFException(500, 5000, e);
        }
    }

}
