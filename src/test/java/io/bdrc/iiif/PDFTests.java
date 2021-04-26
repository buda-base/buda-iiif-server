package io.bdrc.iiif;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdfwriter.COSWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceGray;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import io.bdrc.iiif.image.service.ReadImageProcess;


public class PDFTests {

    static void addBWTiffImageStream_PDFBox(PDDocument doc, InputStream bmg, String fileName, int w, int h) throws IOException  {
        // this fails miserably... no PDF library write group4 tiffs without reencoding apparently!
        PDImageXObject pdImage;
        pdImage = new PDImageXObject(doc, bmg, 
                COSName.CCITTFAX_DECODE, w, h, 1,
                PDDeviceGray.INSTANCE);
        PDPage page = new PDPage(
                new PDRectangle(w, h));
        doc.addPage(page);
        PDPageContentStream contents = new PDPageContentStream(doc, page);
        contents.drawImage(pdImage, 0, 0);
        contents.close();
    }
    
    static void addJpgStream_PDFBox(PDDocument doc, InputStream bmg, String fileName, int w, int h) throws IOException  {
        // that works! no need to reencode each JPG as was done on tbrc.org
        PDImageXObject pdImage;
        BufferedInputStream bis = new BufferedInputStream(bmg);
        PDColorSpace cs = ReadImageProcess.getPDColorSpace(bis, fileName);
        pdImage = new PDImageXObject(doc, bis, 
                COSName.DCT_DECODE, w, h, 8, cs);
        PDPage page = new PDPage(
                new PDRectangle(w, h));
        doc.addPage(page);
        PDPageContentStream contents = new PDPageContentStream(doc, page);
        contents.drawImage(pdImage, 0, 0);
        contents.close();
    }

    static void addBWTiffImageReencode_PDFBox(PDDocument doc, InputStream bmg, String fileName, int w, int h) throws IOException  {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(bmg, baos);
        baos.flush();
        PDImageXObject pdImage = PDImageXObject.createFromByteArray(doc, baos.toByteArray(), "");
        baos.close();
        PDPage page = new PDPage(
                new PDRectangle(w, h));
        doc.addPage(page);
        PDPageContentStream contents = new PDPageContentStream(doc, page);
        contents.drawImage(pdImage, 0, 0);
        contents.close();
    }
    
    static void PDFGenSimple() throws IOException {
        // create a PDF with twice the same image
        PDDocument doc = new PDDocument();
        InputStream bmg;
        bmg = new FileInputStream("src/test/resources/61450006.tif");
        addBWTiffImageReencode_PDFBox(doc, bmg, "src/test/resources/61450006.tif", 11712, 2848);
        bmg = new FileInputStream("src/test/resources/61450006.tif");
        addBWTiffImageStream_PDFBox(doc, bmg, "src/test/resources/61450006.tif", 11712, 2848);
        bmg = new FileInputStream("src/test/resources/ORIGINAL_S3.jpg");
        addJpgStream_PDFBox(doc, bmg, "src/test/resources/ORIGINAL_S3.jpg", 2500, 500);
        OutputStream baos = new FileOutputStream("testpdfgen.pdf");
        COSWriter cw = new COSWriter(baos);
        cw.write(doc);
        cw.close();
    }
    
    public static void main(String[] args) throws IOException {
        PDFGenSimple();
    }
    
}
