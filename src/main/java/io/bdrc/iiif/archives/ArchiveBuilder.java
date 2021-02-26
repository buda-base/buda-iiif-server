package io.bdrc.iiif.archives;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.auth.Access;
import io.bdrc.iiif.core.Application;
import io.bdrc.iiif.core.EHServerCache;
import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.iiif.resolver.IdentifierInfo;
import io.bdrc.iiif.resolver.ImageInfo;
import io.bdrc.libraries.Identifier;

public class ArchiveBuilder {

    public static final String IIIF = "IIIF";
    public static final String IIIF_ZIP = "IIIF_ZIP";
    public final static String PDF_TYPE = "pdf";
    public final static String ZIP_TYPE = "zip";

    public final static Logger log = LoggerFactory.getLogger(ArchiveBuilder.class.getName());

    public static ExecutorService service = Executors.newFixedThreadPool(50);

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void buildPdf(Access acc, IdentifierInfo inf, Identifier idf, String output, String origin)
            throws Exception {
        long deb = System.currentTimeMillis();
        try {
            Application.logPerf("Starting building pdf {}", inf.volumeId);
            Application.logPerf("S3 client obtained in building pdf {} after {} ", inf.volumeId,
                    System.currentTimeMillis() - deb);
            TreeMap<Integer, Future<?>> t_map = new TreeMap<>();
            HashMap<String, ImageInfo> imgDim = new HashMap<>();
            List<ImageInfo> imgInfo = getImageInfos(idf, inf, acc);
            int i = 1;
            for (ImageInfo imgInf : imgInfo) {
                if (imgInf.size == null || imgInf.size <= Integer.parseInt(Application.getProperty("imgSizeLimit"))) {
                    imgDim.put(imgInf.filename, imgInf);
                    ArchiveImageProducer tmp = null;
                    tmp = new ArchiveImageProducer(inf, imgInf.filename, origin);
                    log.info("added Future for image {} of size {}", imgInf.filename, imgInf.size);
                    Future<?> fut = service.submit((Callable) tmp);
                    t_map.put(i, fut);
                    i += 1;
                }
            }
            log.info("Setting output {} to false", output);
            EHServerCache.PDF_JOBS.put(output, true);
            PDDocument doc = new PDDocument();
            doc.setDocumentInformation(ArchiveInfo.getInstance(inf).getDocInformation());
            Application.logPerf("building pdf writer and document opened {} after {}", inf.volumeId,
                    System.currentTimeMillis() - deb);
            for (int k = 1; k <= t_map.keySet().size(); k++) {
                Future<?> tmp = t_map.get(k);
                Object[] obj = (Object[]) tmp.get();
                byte[] bmg = (byte[]) obj[0];
                String imgKey = (String) obj[1];
                if (bmg == null) {
                    // Trying to insert image indicating that original image is
                    // missing
                    try {
                        bmg = toByteArray(
                                ArchiveImageProducer.getBufferedMissingImage("Page " + k + " couldn't be found"));
                    } catch (Exception e) {
                        // We don't interrupt the pdf generation process
                        log.error("Could not get Buffered Missing image from producer for page {} of volume {}", k,
                                inf.volumeId);
                    }
                }
                PDPage page = new PDPage(
                        new PDRectangle(imgDim.get(imgKey).getWidth(), imgDim.get(imgKey).getHeight()));
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
            Application.logPerf("pdf document finished and closed for {} after {}", inf.volumeId,
                    System.currentTimeMillis() - deb);
            EHServerCache.IIIF.put(output, baos.toByteArray());
            EHServerCache.PDF_JOBS.put(output, false);
        } catch (ExecutionException | InterruptedException e) {
            log.error("Error while building pdf for identifier info " + inf.toString(), "");
            throw new IIIFException(500, IIIFException.GENERIC_APP_ERROR_CODE, e);
        }

    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void buildSyncPdf(Access acc, IdentifierInfo inf, Identifier idf, String output, String origin)
            throws IIIFException {
        long deb = System.currentTimeMillis();
        try {
            log.error("generate PDF for {}", output);
            Application.logPerf("Starting building pdf {}", inf.volumeId);
            List<ImageInfo> imgInfo = getImageInfos(idf, inf, acc);
            HashMap<String, ImageInfo> imgDim = new HashMap<>();
            log.info("Setting output {} ", output);
            EHServerCache.PDF_JOBS.put(output, true);
            PDDocument doc = new PDDocument();
            doc.setDocumentInformation(ArchiveInfo.getInstance(inf).getDocInformation());
            Application.logPerf("building pdf writer and document opened {} after {}", inf.volumeId,
                    System.currentTimeMillis() - deb);
            int k = 1;
            log.error("breakpoint 2");
            for (ImageInfo imgInf : imgInfo) {
                if (imgInf.size == null || imgInf.size <= Integer.parseInt(Application.getProperty("imgSizeLimit"))) {
                    log.error("adding {}", imgInf.filename);
                    imgDim.put(imgInf.filename, imgInf);
                    // ArchiveImageProducer tmp = null;
                    Object[] obj = new ArchiveImageProducer(inf, imgInf.filename, origin).getImageAsBytes();
                    byte[] bmg = (byte[]) obj[0];
                    String imgKey = (String) obj[1];
                    if (bmg == null) {
                        // Trying to insert image indicating that original image
                        // is
                        // missing
                        try {
                            bmg = toByteArray(
                                    ArchiveImageProducer.getBufferedMissingImage("Page {}" + k + " couldn't be found"));
                        } catch (Exception e) {
                            // We don't interrupt the pdf generation process
                            log.error("Could not get Buffered Missing image from producer for page {} of volume {}", k,
                                    inf.volumeId);
                        }
                    }
                    PDPage page = new PDPage(
                            new PDRectangle(imgDim.get(imgKey).getWidth(), imgDim.get(imgKey).getHeight()));
                    doc.addPage(page);
                    PDImageXObject pdImage = PDImageXObject.createFromByteArray(doc, bmg, "");
                    PDPageContentStream contents = new PDPageContentStream(doc, page);
                    contents.drawImage(pdImage, 0, 0);
                    log.error("page was drawn for img {}", imgInf.filename);
                    contents.close();
                }
                k++;
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            COSWriter cw = new COSWriter(baos);
            cw.write(doc);
            cw.close();
            log.error("Closing doc after writing {} ", doc);
            doc.close();
            Application.logPerf("pdf document finished and closed for {} after {}", inf.volumeId,
                    System.currentTimeMillis() - deb);
            EHServerCache.IIIF.put(output, baos.toByteArray());
            EHServerCache.PDF_JOBS.put(output, false);
        } catch (Exception e) {
            log.error("Error while building pdf for identifier info {}", inf.toString());
            throw new IIIFException(500, IIIFException.GENERIC_APP_ERROR_CODE, e);
        }

    }
    
    

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void buildZip(Access acc, IdentifierInfo inf, Identifier idf, String output, String origin)
            throws IIIFException {
        try {
            long deb = System.currentTimeMillis();
            Application.logPerf("Starting building zip {}", inf.volumeId);
            Application.logPerf("S3 client obtained in building pdf {} after {} ", inf.volumeId,
                    System.currentTimeMillis() - deb);
            TreeMap<Integer, Future<?>> t_map = new TreeMap<>();
            TreeMap<Integer, String> images = new TreeMap<>();
            List<ImageInfo> imgInfo = getImageInfos(idf, inf, acc);
            int i = 1;
            for (ImageInfo imf : imgInfo) {
                ArchiveImageProducer tmp = new ArchiveImageProducer(inf, imf.filename, origin);
                Future<?> fut = service.submit((Callable) tmp);
                t_map.put(i, fut);
                images.put(i, imf.filename);
                i += 1;
            }
            EHServerCache.ZIP_JOBS.put(output, true);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zipOut = new ZipOutputStream(baos);
            Application.logPerf("building zip stream opened {} after {}", inf.volumeId,
                    System.currentTimeMillis() - deb);

            for (int k = 1; k <= t_map.keySet().size(); k++) {
                Future<?> tmp = t_map.get(k);
                byte[] img = null;
                Object[] obj = (Object[]) tmp.get();
                img = (byte[]) obj[0];
                if (img == null) {
                    // Trying to insert image indicating that original image is
                    // missing
                    try {
                        BufferedImage bImg = ArchiveImageProducer
                                .getBufferedMissingImage("Page " + k + " couldn't be found");
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        ImageIO.write(bImg, "png", out);
                        img = out.toByteArray();
                    } catch (IOException e) {
                        // We don't interrupt the pdf generation process
                        log.error("Could not get Buffered Missing image from producer for page {} of volume {}", k,
                                inf.volumeId);
                    }
                }
                ZipEntry zipEntry = new ZipEntry(images.get(k));
                zipOut.putNextEntry(zipEntry);
                zipOut.write(img);
                zipOut.closeEntry();
            }
            zipOut.close();
            Application.logPerf("zip document finished and closed for {} after {}", inf.volumeId,
                    System.currentTimeMillis() - deb);
            EHServerCache.IIIF_ZIP.put(output, baos.toByteArray());
            log.info("Put zip file in cache with key {}", output.substring(3));
            EHServerCache.ZIP_JOBS.put(output, false);
            log.info("Put true in zip jobs cache for {}", output);
        } catch (IOException | ExecutionException | InterruptedException e) {
            log.error("Error while building zip archives ", e.getMessage());
            throw new IIIFException(500, IIIFException.GENERIC_APP_ERROR_CODE, e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void buildSyncZip(Access acc, IdentifierInfo inf, Identifier idf, String output, String origin)
            throws IIIFException {
        try {
            long deb = System.currentTimeMillis();
            Application.logPerf("Starting building zip {}", inf.volumeId);
            Application.logPerf("S3 client obtained in building pdf {} after {} ", inf.volumeId,
                    System.currentTimeMillis() - deb);
            // TreeMap<Integer, Future<?>> t_map = new TreeMap<>();
            TreeMap<Integer, String> images = new TreeMap<>();
            List<ImageInfo> imgInfo = getImageInfos(idf, inf, acc);
            int i = 1;
            for (ImageInfo imf : imgInfo) {
                ArchiveImageProducer tmp = new ArchiveImageProducer(inf, imf.filename, origin);

            }
            EHServerCache.ZIP_JOBS.put(output, true);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zipOut = new ZipOutputStream(baos);
            Application.logPerf("building zip stream opened {} after {}", inf.volumeId,
                    System.currentTimeMillis() - deb);
            int k = 1;
            for (ImageInfo imf : imgInfo) {
                if (imf.size == null || (imf.size != null
                        && imf.size <= Integer.parseInt(Application.getProperty("imgSizeLimit")))) {
                    Object[] obj = new ArchiveImageProducer(inf, imf.filename, origin).getImageAsBytes();
                    byte[] bmg = (byte[]) obj[0];
                    if (bmg == null) {
                        // Trying to insert image indicating that original image
                        // is
                        // missing
                        try {
                            bmg = toByteArray(
                                    ArchiveImageProducer.getBufferedMissingImage("Page " + k + " couldn't be found"));
                        } catch (Exception e) {
                            // We don't interrupt the pdf generation process
                            log.error("Could not get Buffered Missing image from producer for page {} of volume {}", k,
                                    inf.volumeId);
                        }
                    }
                    ZipEntry zipEntry = new ZipEntry(imf.filename);
                    zipOut.putNextEntry(zipEntry);
                    zipOut.write(bmg);
                    zipOut.closeEntry();
                }
                k++;
            }
            zipOut.close();
            Application.logPerf("zip document finished and closed for {} after {}", inf.volumeId,
                    System.currentTimeMillis() - deb);
            EHServerCache.IIIF_ZIP.put(output, baos.toByteArray());
            log.info("Put zip file in cache with key {}", output.substring(3));
            EHServerCache.ZIP_JOBS.put(output, false);
            log.info("Put true in zip jobs cache for {}", output);
        } catch (IOException e) {
            log.error("Error while building zip archives ", e.getMessage());
            throw new IIIFException(500, IIIFException.GENERIC_APP_ERROR_CODE, e);
        }
    }

    private static List<ImageInfo> getImageInfos(Identifier idf, IdentifierInfo inf, Access acc) throws IIIFException {
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
        return inf.ensureImageListInfo(acc, startPage.intValue(), endPage.intValue());
    }

    public static byte[] toByteArray(BufferedImage img) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        baos.flush();
        byte[] imageInByte = baos.toByteArray();
        baos.close();
        return imageInByte;
    }

    public static boolean isPdfDone(String id) {
        log.debug("IS PDF DONE job {}", id);
        return EHServerCache.IIIF.containsKey(id);
    }

    public static boolean isZipDone(String id) {
        log.debug("IS ZIP DONE job {}", id);
        return EHServerCache.IIIF_ZIP.containsKey(id);
    }
}
