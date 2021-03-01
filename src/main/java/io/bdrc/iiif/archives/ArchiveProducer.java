package io.bdrc.iiif.archives;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.auth.Access;
import io.bdrc.iiif.core.EHServerCache;
import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.iiif.resolver.IdentifierInfo;
import io.bdrc.libraries.Identifier;

public class ArchiveProducer implements Callable<Void> {

    public static final String IIIF_IMG = "iiif_img";
    public final static Logger log = LoggerFactory.getLogger(ArchiveProducer.class);
    
    public static final int PDF = 0;
    public static final int ZIP = 1;

    String cacheKey;
    String origin;
    Access acc;
    IdentifierInfo inf;
    Identifier idf;
    int type;

    public ArchiveProducer(Access acc, IdentifierInfo inf, Identifier idf, String cacheKey, String origin, int type) throws IIIFException {
        this.cacheKey = cacheKey;
        this.acc = acc;
        this.inf = inf;
        this.idf = idf;
        this.origin = origin;
        this.type = type;
    }

    @Override
    public Void call() throws IIIFException {
        if (this.type == PDF) {
            if (EHServerCache.IIIF_PDF.containsKey(this.cacheKey) || ArchiveBuilder.pdfjobs.containsKey(this.cacheKey)) {
                return null;
            }
            ArchiveBuilder.buildSyncPdf(this.acc, this.inf, this.idf, this.cacheKey, this.origin);
            return null;
        } else {
            if (EHServerCache.IIIF_ZIP.containsKey(this.cacheKey) || ArchiveBuilder.zipjobs.containsKey(this.cacheKey)) {
                return null;
            }
            ArchiveBuilder.buildSyncZip(this.acc, this.inf, this.idf, this.cacheKey, this.origin);
            return null;
        }
    }

}

