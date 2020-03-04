package io.bdrc.archives;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
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

import org.apache.pdfbox.pdfwriter.COSWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.AmazonS3;

import ch.qos.logback.classic.Logger;
import de.digitalcollections.iiif.myhymir.Application;
import de.digitalcollections.iiif.myhymir.EHServerCache;
import de.digitalcollections.iiif.myhymir.ServerCache;
import de.digitalcollections.iiif.myhymir.backend.impl.repository.S3ResourceRepositoryImpl;
import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.iiif.resolver.IdentifierInfo;
import io.bdrc.iiif.resolver.ImageInfo;

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
            ExecutorService service = Executors.newFixedThreadPool(25);
            AmazonS3 s3 = S3ResourceRepositoryImpl.getClientInstance();
            Application.logPerf("S3 client obtained in building pdf {} after {} ", inf.volumeId, System.currentTimeMillis() - deb);
            TreeMap<Integer, Future<?>> t_map = new TreeMap<>();
            HashMap<String, ImageInfo> imgDim = new HashMap<>();
            for (ImageInfo i : inf.getImageInfoList()) {
                imgDim.put(i.filename, i);
            }
            int i = 1;
            while (idList.hasNext()) {
                final String id = inf.getVolumeId() + "::" + idList.next();
                ArchiveImageProducer tmp = null;
                tmp = new ArchiveImageProducer(s3, id, PDF_TYPE, origin);
                log.info("added Future for image {}", id);
                Future<?> fut = service.submit((Callable) tmp);
                t_map.put(i, fut);
                i += 1;
            }
            log.info("Setting output {} to false", output);
            ServerCache.PDF_JOBS.put(output, false);
            PDDocument doc = new PDDocument();
            doc.setDocumentInformation(ArchiveInfo.getInstance(inf).getDocInformation());
            Application.logPerf("building pdf writer and document opened {} after {}", inf.volumeId, System.currentTimeMillis() - deb);
            for (int k = 1; k <= t_map.keySet().size(); k++) {
                Future<?> tmp = t_map.get(k);
                // BufferedImage bImg = (BufferedImage) tmp.get();
                Object[] obj = (Object[]) tmp.get();
                byte[] bmg = (byte[]) obj[0];
                String imgKey = (String) obj[1];
                log.debug("building pdf writer is imagage null {} ", (bmg == null));
                if (bmg == null) {
                    // Trying to insert image indicating that original image is missing
                    try {
                        bmg = toByteArray(ArchiveImageProducer.getBufferedMissingImage("Page " + k + " couldn't be found"));
                    } catch (Exception e) {
                        // We don't interrupt the pdf generation process
                        log.error("Could not get Buffered Missing image from producer for page {} of volume {}", k, inf.volumeId);
                    }
                }
                PDPage page = new PDPage(new PDRectangle(imgDim.get(imgKey).getWidth(), imgDim.get(imgKey).getHeight()));
                doc.addPage(page);
                PDImageXObject pdImage = PDImageXObject.createFromByteArray(doc, bmg, "");
                PDPageContentStream contents = new PDPageContentStream(doc, page);
                contents.drawImage(pdImage, 0, 0);
                log.debug("page was drawn for img {} ", bmg);
                contents.close();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            COSWriter cw = new COSWriter(baos);
            cw.write(doc);
            cw.close();
            log.debug("Closing doc after writing {} ", doc);
            doc.close();
            Application.logPerf("pdf document finished and closed for {} after {}", inf.volumeId, System.currentTimeMillis() - deb);
            EHServerCache.IIIF.put(output.substring(4), baos.toByteArray());
            ServerCache.PDF_JOBS.put(output, true);
        } catch (ExecutionException | InterruptedException e) {
            log.error("Error while building pdf for identifier info " + inf.toString(), "");
            throw new IIIFException(500, IIIFException.GENERIC_APP_ERROR_CODE, e);
        }

    }

    public static byte[] toByteArray(BufferedImage img) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        baos.flush();
        byte[] imageInByte = baos.toByteArray();
        baos.close();
        return imageInByte;
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
                Object[] obj = (Object[]) tmp.get();
                img = (byte[]) obj[0];
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
