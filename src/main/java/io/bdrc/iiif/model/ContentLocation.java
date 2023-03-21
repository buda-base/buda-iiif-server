package io.bdrc.iiif.model;

import java.util.Comparator;

import org.apache.jena.rdf.model.Resource;

import io.bdrc.iiif.resolver.AppConstants;

public class ContentLocation implements Comparator<ContentLocation> {
    public final int vol_start;
    public final int vol_end;
    public final Integer page_start;
    public final Integer page_end;
    public final String iiQname;
    
    public ContentLocation(final Resource inst, final Integer vol_start, final Integer vol_end, final Integer page_start, final Integer page_end) {
        this.page_start = page_start;
        this.page_end = page_end;
        if (vol_start == null || vol_start < 1) {
            this.vol_start = 1;
        } else {
            this.vol_start = vol_start;
        }
        if (vol_end == null || vol_end < this.vol_start) {
            this.vol_end = this.vol_start;
        } else {
            this.vol_end = vol_end;
        }
        if (inst.getURI().startsWith(AppConstants.BDR)) {
            this.iiQname = "bdr:"+inst.getURI().substring(AppConstants.BDR_len);
        } else {
            this.iiQname = null;
        }
    }

    @Override
    public final int compare(final ContentLocation o1, final ContentLocation o2) {
        if (!o1.iiQname.equals(o2))
            return o1.iiQname.compareTo(o2.iiQname);
        if (o1.vol_start != o2.vol_start)
            return Integer.compare(o1.vol_start, o2.vol_start);
        if (o1.vol_end != o2.vol_end)
            return Integer.compare(o1.vol_end, o2.vol_end);
        if (o1.page_start != o2.page_start)
            return Integer.compare(o1.page_start, o2.page_start);
        return Integer.compare(o1.page_end, o2.page_end);
    }
}
