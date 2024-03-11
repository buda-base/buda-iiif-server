package io.bdrc.iiif.archives;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfDocumentInfo;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;

import io.bdrc.auth.AccessInfo.AccessLevel;
import io.bdrc.iiif.core.Application;
import io.bdrc.iiif.core.EHServerCache;
import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.iiif.resolver.IdentifierInfo;
import io.bdrc.iiif.resolver.ImageInfo;
import io.bdrc.libraries.Identifier;

public class ArchiveBuilder {

    public static final String IIIF_ZIP = "IIIF_ZIP";
    public final static String PDF_TYPE = "pdf";
    public final static String ZIP_TYPE = "zip";

    public final static Logger log = LoggerFactory.getLogger(ArchiveBuilder.class.getName());

    public static ExecutorService service = Executors.newFixedThreadPool(50);
    
    public static Map<String, Double> pdfjobs = new ConcurrentHashMap<>();
    public static Map<String, Double> zipjobs = new ConcurrentHashMap<>();

    public static void buildPdfInCache(AccessLevel al, IdentifierInfo inf, Identifier idf, String output, String origin)
            throws IIIFException {
        OutputStream os = EHServerCache.IIIF_PDF.getOs(output);
        buildPdf(al, inf, idf, output, origin, os, pdfjobs);
        try {
            os.close();
        } catch (IOException e) {
            log.error("Error while building pdf for identifier info {}", inf.toString());
            throw new IIIFException(500, IIIFException.GENERIC_APP_ERROR_CODE, e);
        }
        EHServerCache.IIIF_PDF.outputDone(output);
    }
    
    public static StreamingResponseBody getPDFStreamingResponseBody(AccessLevel al, IdentifierInfo inf, Identifier idf, String output, String origin) {
        return new StreamingResponseBody() {
            @Override
            public void writeTo(final OutputStream os) throws IOException {
                try {
                    buildPdf(al, inf, idf, output, origin, os, null);
                } catch (IIIFException e) {
                   log.error("problem writing PDF: ", e);
                }
            }
        };
    }
    
    
    public static void writePdf(AccessLevel al, IdentifierInfo inf, Identifier idf, String output, String origin, OutputStream os)
            throws IIIFException {
        buildPdf(al, inf, idf, output, origin, os, null);
    }
    
    public static final List<String> imageLastFour = Arrays.asList(new String[] {".jpg", "jpeg", ".tif", "tiff", ".png"});
    public static boolean isLikelyImage(final String fname) {
        if (fname.length() < 4)
            return false;
        final String lastFour = fname.substring(fname.length()-4).toLowerCase();
        return imageLastFour.contains(lastFour);
    }
    
    public static void buildPdf(AccessLevel al, IdentifierInfo inf, Identifier idf, String output, String origin, OutputStream os, Map<String, Double> jobs)
            throws IIIFException {
        long deb = System.currentTimeMillis();
        int limitsize = Integer.parseInt(Application.getProperty("imgSizeLimit"));
        try {
            if (jobs != null)
                jobs.put(output, 0.);
            log.info("generate PDF for {}", output);
            Application.logPerf("Starting building pdf {}", inf.volumeId);
            List<ImageInfo> imgInfo = getImageInfos(idf, inf, al);
            final int totalImages = imgInfo.size();
            log.info("Setting output {} ", output);
            PdfWriter pdfWriter = new PdfWriter(os);
            PdfDocument doc = new PdfDocument(pdfWriter);
            Document d = new Document(doc, PageSize.DEFAULT, true);
            // TODO: wire the metadata from Fuseki, in the ArchiveInfo class
            PdfDocumentInfo info = doc.getDocumentInfo();
            info.addCreationDate();
            info.setCreator("Buddhist Digital Resource Center (BDRC)");
            Application.logPerf("building pdf writer and document opened {} after {}", inf.volumeId,
                    System.currentTimeMillis() - deb);
            int nbImagesTried = 1;
            int nbImagesAdded = 1;
            for (ImageInfo imgInf : imgInfo) {
                if (isLikelyImage(imgInf.filename) && (imgInf.size == null || imgInf.size <= limitsize)) {
                    log.debug("adding {}", imgInf.filename);
                    // ArchiveImageProducer tmp = null;
                    Object[] obj = ArchiveImageProducer.getImageInputStream(inf, imgInf.filename, origin);
                    InputStream bmg = (InputStream) obj[0];
                    if (bmg == null) {
                        log.error("couldn't find {}", imgInf.filename);
                    } else {
                        // unfortunately it seems there's no way around copying in memory with iText
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        IOUtils.copy(bmg, baos);
                        bmg.close();
                        baos.flush();
                        ImageData imageData = ImageDataFactory.create(baos.toByteArray());
                        baos.close();
                        Image pdfImg = new Image(imageData);
                        doc.addNewPage(new PageSize(imgInf.getWidth(), imgInf.getHeight()));
                        //PdfPage page = doc.getPage(k);
                        //page.setPageLabel(PageLabelNumberingStyle.UPPERCASE_LETTERS, imgInf.filename);
                        //page.setPageLabel(PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS, "i. "+Integer.toString(imgInf.imgNum));
                        pdfImg.setFixedPosition(nbImagesAdded, 0, 0);
                        pdfImg.setWidth(imgInf.getWidth());
                        pdfImg.setHeight(imgInf.getHeight());
                        d.add(pdfImg);
                        d.flush();
                        pdfWriter.flush();
                        os.flush();
                        nbImagesAdded++;
                    }
                }
                if ((nbImagesTried % 5) == 0 && jobs != null) {
                    // every 5 images, update the percentage
                    final double rate = nbImagesTried / ((double) totalImages);
                    jobs.put(output, rate);
                }
                nbImagesTried++;
            }
            log.info("Closing doc after writing {} ", doc);
            d.close();
            doc.close();
            pdfWriter.close();
            Application.logPerf("pdf document finished and closed for {} after {}", inf.volumeId,
                    System.currentTimeMillis() - deb);
        } catch (Exception e) {
            log.error("Error while building pdf for identifier info {}", inf.toString());
            throw new IIIFException(500, IIIFException.GENERIC_APP_ERROR_CODE, e);
        } finally {
            if (jobs != null)
                jobs.remove(output);
        }
    }

    
    public static void buildZipInCache(AccessLevel al, IdentifierInfo inf, Identifier idf, String output, String origin)
            throws IIIFException {
        OutputStream os = EHServerCache.IIIF_ZIP.getOs(output);
        buildZip(al, inf, idf, output, origin, os, zipjobs);
        try {
            os.close();
        } catch (IOException e) {
            log.error("Error while building zip for identifier info {}", inf.toString());
            throw new IIIFException(500, IIIFException.GENERIC_APP_ERROR_CODE, e);
        }
        EHServerCache.IIIF_ZIP.outputDone(output);
    }
    
    public static StreamingResponseBody getZipStreamingResponseBody(AccessLevel al, IdentifierInfo inf, Identifier idf, String output, String origin) {
        return new StreamingResponseBody() {
            @Override
            public void writeTo(final OutputStream os) throws IOException {
                try {
                    buildZip(al, inf, idf, output, origin, os, null);
                } catch (IIIFException e) {
                   log.error("problem writing Zip: ", e);
                }
            }
        };
    }
    
    public static void writeZip(AccessLevel al, IdentifierInfo inf, Identifier idf, String output, String origin, OutputStream os)
            throws IIIFException {
        buildZip(al, inf, idf, output, origin, os, null);
    }
    
    public static void buildZip(AccessLevel al, IdentifierInfo inf, Identifier idf, String output, String origin, OutputStream os, Map<String, Double> jobs)
            throws IIIFException {
        try {
            if (jobs != null)
                jobs.put(output, 0.);
            long deb = System.currentTimeMillis();
            Application.logPerf("Starting building zip {}", inf.volumeId);
            Application.logPerf("S3 client obtained in building pdf {} after {} ", inf.volumeId,
                    System.currentTimeMillis() - deb);
            List<ImageInfo> imgInfo = getImageInfos(idf, inf, al);
            final int totalImages = imgInfo.size();
            ZipOutputStream zipOut = new ZipOutputStream(os);
            Application.logPerf("building zip stream opened {} after {}", inf.volumeId,
                    System.currentTimeMillis() - deb);
            int k = 1;
            for (ImageInfo imgInf : imgInfo) {
                Object[] obj = ArchiveImageProducer.getImageInputStream(inf, imgInf.filename, origin);
                InputStream bmg = (InputStream) obj[0];
                if (bmg == null) {
                    log.error("Could not get Buffered Missing image from producer for page {} of volume {}", k,
                            inf.volumeId);
                }
                ZipEntry zipEntry = new ZipEntry(imgInf.filename);
                zipOut.putNextEntry(zipEntry);
                IOUtils.copy(bmg, zipOut);
                bmg.close();
                zipOut.closeEntry();
                zipOut.flush();
                os.flush();
                if ((k % 5) == 0 && jobs != null) {
                    // every 5 images, update the percentage
                    final double rate = k / ((double) totalImages);
                    jobs.put(output, rate);
                }
                k++;
            }
            zipOut.close();
            Application.logPerf("zip document finished and closed for {} after {}", inf.volumeId,
                    System.currentTimeMillis() - deb);
        } catch (IOException e) {
            log.error("Error while building zip archives ", e.getMessage());
            throw new IIIFException(500, IIIFException.GENERIC_APP_ERROR_CODE, e);
        } finally {
            if (jobs != null)
                jobs.remove(output);
        }
    }

    private static List<ImageInfo> getImageInfos(Identifier idf, IdentifierInfo inf, AccessLevel al) throws IIIFException {
        Integer startPage = null;
        if (idf.getBPageNum() != null) {
            startPage = idf.getBPageNum();
        } else {
            startPage = 1;
        }
        Integer endPage = null;
        if (idf.getEPageNum() != null) {
            endPage = idf.getEPageNum();
        } else {
            endPage = inf.getTotalPages();
        }
        return inf.getImageListInfo(al, startPage.intValue(), endPage.intValue());
    }

    public static InputStream toInputStream(BufferedImage img) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream() {
            @Override
            public synchronized byte[] toByteArray() {
                return this.buf;
            }
        };
        ImageIO.write(img, "jpg", output);
        return new ByteArrayInputStream(output.toByteArray(), 0, output.size());
    }
    
    public static byte[] toByteArray(BufferedImage img) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        baos.flush();
        byte[] imageInByte = baos.toByteArray();
        baos.close();
        return imageInByte;
    }
}
