package io.bdrc.archives;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.AmazonS3;
import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfWriter;

import ch.qos.logback.classic.Logger;
import de.digitalcollections.iiif.myhymir.Application;
import de.digitalcollections.iiif.myhymir.EHServerCache;
import de.digitalcollections.iiif.myhymir.ServerCache;
import de.digitalcollections.iiif.myhymir.backend.impl.repository.S3ResourceRepositoryImpl;
import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.iiif.resolver.IdentifierInfo;

public class ArchiveBuilder {

    public static final String IIIF = "IIIF";
    public static final String IIIF_ZIP = "IIIF_ZIP";
    public final static String PDF_TYPE = "pdf";
    public final static String ZIP_TYPE = "zip";

    public final static Logger log = (Logger) LoggerFactory.getLogger(ArchiveBuilder.class.getName());

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void buildPdf(Iterator<String> idList, IdentifierInfo inf, String output, String origin) throws Exception {
        long deb = System.currentTimeMillis();
        try {
            Application.logPerf("Starting building pdf {}", inf.volumeId);
            ExecutorService service = Executors.newFixedThreadPool(50);
            AmazonS3 s3 = S3ResourceRepositoryImpl.getClientInstance();
            Application.logPerf("S3 client obtained in building pdf {} after {} ", inf.volumeId, System.currentTimeMillis() - deb);
            TreeMap<Integer, Future<?>> t_map = new TreeMap<>();
            int i = 1;
            while (idList.hasNext()) {
                final String id = inf.getVolumeId() + "::" + idList.next();
                ArchiveImageProducer tmp = null;
                tmp = new ArchiveImageProducer(s3, id, PDF_TYPE, origin);
                Future<?> fut = service.submit((Callable) tmp);
                t_map.put(i, fut);
                i += 1;
            }
            log.info("Setting output {} to false", output);
            ServerCache.PDF_JOBS.put(output, false);
            Document document = new Document();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            PdfWriter writer = null;
            try {
                writer = PdfWriter.getInstance(document, stream);
            } catch (DocumentException e) {
                throw e;
            }
            writer.open();
            document.open();
            Application.perfLog.debug("building pdf writer and document opened {} after {}", inf.volumeId, System.currentTimeMillis() - deb);
            for (int k = 1; k <= t_map.keySet().size(); k++) {
                Future<?> tmp = t_map.get(k);
                Image img = null;
                try {
                    img = (Image) tmp.get();
                    if (img == null) {
                        // Trying to insert image indicating that original image is missing
                        try {
                            img = ArchiveImageProducer.getMissingImage("Page " + k + " couldn't be found");
                            document.setPageSize(new Rectangle(img.getWidth(), img.getHeight()));
                            document.newPage();
                            document.add(img);
                        } catch (BadElementException | IOException e) {
                            // We don't interrupt the pdf generation process
                            e.printStackTrace();
                        }
                    }
                    log.info("added image index {}", k);
                    document.setPageSize(new Rectangle(img.getWidth(), img.getHeight()));
                    document.newPage();
                    document.add(img);
                } catch (DocumentException | ExecutionException | InterruptedException e) {
                    throw e;
                }
            }
            document.close();
            Application.logPerf("pdf document finished and closed for {} after {}", inf.volumeId, System.currentTimeMillis() - deb);
            EHServerCache.IIIF.put(output.substring(4), stream.toByteArray());
            ServerCache.PDF_JOBS.put(output, true);
        } catch (ExecutionException | InterruptedException e) {
            log.error("Error while building pdf for identifier info " + inf.toString(), "");
            throw new IIIFException(500, IIIFException.GENERIC_APP_ERROR_CODE, e);
        }

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void buildZip(Iterator<String> idList, IdentifierInfo inf, String output, String origin) throws IIIFException {
        try {
            long deb = System.currentTimeMillis();
            Application.logPerf("Starting building zip {}", inf.volumeId);
            ExecutorService service = Executors.newFixedThreadPool(50);
            AmazonS3 s3 = S3ResourceRepositoryImpl.getClientInstance();
            Application.logPerf("S3 client obtained in building pdf {} after {} ", inf.volumeId, System.currentTimeMillis() - deb);
            TreeMap<Integer, Future<?>> t_map = new TreeMap<>();
            TreeMap<Integer, String> images = new TreeMap<>();
            int i = 1;
            while (idList.hasNext()) {
                String img = idList.next();
                final String id = inf.getVolumeId() + "::" + img;
                ArchiveImageProducer tmp = null;
                tmp = new ArchiveImageProducer(s3, id, ZIP_TYPE, origin);
                Future<?> fut = service.submit((Callable) tmp);
                t_map.put(i, fut);
                images.put(i, img);
                i += 1;
            }
            ServerCache.ZIP_JOBS.put(output, false);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zipOut = new ZipOutputStream(baos);
            Application.logPerf("building zip stream opened {} after {}", inf.volumeId, System.currentTimeMillis() - deb);

            for (int k = 1; k <= t_map.keySet().size(); k++) {
                Future<?> tmp = t_map.get(k);
                byte[] img = null;
                img = (byte[]) tmp.get();
                if (img == null) {
                    // Trying to insert image indicating that original image is missing
                    try {
                        BufferedImage bImg = ArchiveImageProducer.getBufferedMissingImage("Page " + k + " couldn't be found");
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        ImageIO.write(bImg, "png", out);
                        img = out.toByteArray();
                    } catch (IOException e) {
                        // We don't interrupt the pdf generation process
                        log.error("Could not get Buffered Missing image from producer for page {} of volume {}", k, inf.volumeId);
                    }
                }
                ZipEntry zipEntry = new ZipEntry(images.get(k));
                zipOut.putNextEntry(zipEntry);
                zipOut.write(img);
                zipOut.closeEntry();
            }
            zipOut.close();
            Application.logPerf("zip document finished and closed for {} after {}", inf.volumeId, System.currentTimeMillis() - deb);
            EHServerCache.IIIF_ZIP.put(output.substring(3), baos.toByteArray());
            ServerCache.ZIP_JOBS.put(output, true);
        } catch (IOException | ExecutionException | InterruptedException e) {
            log.error("Error while building zip archives ", e.getMessage());
            throw new IIIFException(500, IIIFException.GENERIC_APP_ERROR_CODE, e);
        }
    }

    public static boolean isPdfDone(String id) {
        log.debug("IS PDF DONE job " + id);
        if (ServerCache.PDF_JOBS.get(id) == null) {
            log.debug("IS PDF DONE null in cache for " + id);
            return false;
        }
        log.debug("IS PDF DONE returns from cache value for " + id + ">>" + ServerCache.PDF_JOBS.get(id));
        return (boolean) ServerCache.PDF_JOBS.get(id);
    }

    public static boolean isZipDone(String id) {
        log.debug("IS ZIP DONE job " + id);
        if (ServerCache.ZIP_JOBS.get(id) == null) {
            log.debug("IS ZIP DONE null in cache for " + id);
            return false;
        }
        log.debug("IS ZIP DONE returns from cache value for " + id + ">>" + ServerCache.ZIP_JOBS.get(id));
        return (boolean) ServerCache.ZIP_JOBS.get(id);
    }
}
