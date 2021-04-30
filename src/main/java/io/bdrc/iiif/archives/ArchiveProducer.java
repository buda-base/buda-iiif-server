package io.bdrc.iiif.archives;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.auth.Access;
import io.bdrc.iiif.core.DiskCache;
import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.iiif.resolver.IdentifierInfo;
import io.bdrc.libraries.Identifier;

public class ArchiveProducer implements Callable<Void> {

    public final static Logger log = LoggerFactory.getLogger(ArchiveProducer.class);
    
    public static final int PDF = 0;
    public static final int ZIP = 1;

    String cacheKey;
    String origin;
    Access acc;
    IdentifierInfo inf;
    Identifier idf;
    int type;
    DiskCache dc;

    public ArchiveProducer(Access acc, IdentifierInfo inf, Identifier idf, String cacheKey, String origin, int type, DiskCache dc) throws IIIFException {
        this.cacheKey = cacheKey;
        this.acc = acc;
        this.inf = inf;
        this.idf = idf;
        this.origin = origin;
        this.type = type;
        this.dc = dc;
    }

    @Override
    public Void call() throws IIIFException {
        if (this.type == PDF) {
            if (dc.hasKey(this.cacheKey) || ArchiveBuilder.pdfjobs.containsKey(this.cacheKey)) {
                return null;
            }
            ArchiveBuilder.buildPdfInCache(this.acc, this.inf, this.idf, this.cacheKey, this.origin);
            return null;
        } else {
            if (dc.hasKey(this.cacheKey) || ArchiveBuilder.zipjobs.containsKey(this.cacheKey)) {
                return null;
            }
            ArchiveBuilder.buildZipInCache(this.acc, this.inf, this.idf, this.cacheKey, this.origin);
            return null;
        }
    }

}

