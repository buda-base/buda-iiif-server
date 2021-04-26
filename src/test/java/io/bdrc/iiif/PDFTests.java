package io.bdrc.iiif;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PageLabelNumberingStyle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;



public class PDFTests {

//    static void addBWTiffImageStream_PDFBox(PDDocument doc, InputStream bmg, String fileName, int w, int h) throws IOException  {
//        // this fails miserably... no PDF library write group4 tiffs without reencoding apparently!
//        PDImageXObject pdImage;
//        pdImage = new PDImageXObject(doc, bmg, 
//                COSName.CCITTFAX_DECODE, w, h, 1,
//                PDDeviceGray.INSTANCE);
//        PDPage page = new PDPage(
//                new PDRectangle(w, h));
//        doc.addPage(page);
//        PDPageContentStream contents = new PDPageContentStream(doc, page);
//        contents.drawImage(pdImage, 0, 0);
//        contents.close();
//    }
//    
//    static void addJpgStream_PDFBox(PDDocument doc, InputStream bmg, String fileName, int w, int h) throws IOException  {
//        // that works! no need to reencode each JPG as was done on tbrc.org
//        PDImageXObject pdImage;
//        BufferedInputStream bis = new BufferedInputStream(bmg);
//        PDColorSpace cs = ReadImageProcess.getPDColorSpace(bis, fileName);
//        pdImage = new PDImageXObject(doc, bis, 
//                COSName.DCT_DECODE, w, h, 8, cs);
//        PDPage page = new PDPage(
//                new PDRectangle(w, h));
//        doc.addPage(page);
//        PDPageContentStream contents = new PDPageContentStream(doc, page);
//        contents.drawImage(pdImage, 0, 0);
//        contents.close();
//    }
//
//    static void addImageReencode_PDFBox(PDDocument doc, InputStream bmg, String fileName, int w, int h) throws IOException  {
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        IOUtils.copy(bmg, baos);
//        baos.flush();
//        PDImageXObject pdImage = PDImageXObject.createFromByteArray(doc, baos.toByteArray(), "");
//        baos.close();
//        PDPage page = new PDPage(
//                new PDRectangle(w, h));
//        doc.addPage(page);
//        PDPageContentStream contents = new PDPageContentStream(doc, page);
//        contents.drawImage(pdImage, 0, 0);
//        contents.close();
//    }
//    
//    static void PDFGenSimple_PDFBOX() throws IOException {
//        // create a PDF with twice the same image
//        PDDocument doc = new PDDocument();
//        InputStream bmg;
//        //bmg = new FileInputStream("src/test/resources/61450006.tif");
//        //addImageReencode_PDFBox(doc, bmg, "src/test/resources/61450006.tif", 11712, 2848);
//        //bmg = new FileInputStream("src/test/resources/61450006.tif");
//        //addBWTiffImageStream_PDFBox(doc, bmg, "src/test/resources/61450006.tif", 11712, 2848);
//        bmg = new FileInputStream("testspdftbrcorg/original.jpg");
//        addJpgStream_PDFBox(doc, bmg, "src/test/resources/ORIGINAL_S3.jpg", 2500, 500);
//        OutputStream baos = new FileOutputStream("testpdfgen.pdf");
//        COSWriter cw = new COSWriter(baos);
//        cw.write(doc);
//        cw.close();
//        doc.close();
//    }
    
    static void PDFGenSimple_itext() throws IOException {
        // create a PDF with twice the same image
        FileOutputStream file = new FileOutputStream("testpdfgen-simpleitext.pdf");
        

        PdfWriter pdfWriter = new PdfWriter(file);
        PdfDocument doc = new PdfDocument(pdfWriter);
        Document d = new Document(doc);

        InputStream is = new FileInputStream("src/test/resources/61450006.tif");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(is, baos);
        baos.flush();
        ImageData imageData = ImageDataFactory.create(baos.toByteArray());
        baos.close();
        Image pdfImg = new Image(imageData);
        doc.addNewPage(new PageSize(11712, 2848));
        PdfPage page = doc.getPage(1);
        page.setPageLabel(PageLabelNumberingStyle.LOWERCASE_LETTERS, "i. 1");
        pdfImg.setFixedPosition(1, 0, 0);
        pdfImg.setWidth(11712);
        pdfImg.setHeight(2848);
        d.add(pdfImg);
        
        d.flush();
        
        imageData = ImageDataFactory.create("testspdftbrcorg/original.jpg");
        pdfImg = new Image(imageData);
        doc.addNewPage(new PageSize(2500, 500));
        pdfImg.setFixedPosition(2, 0, 0);
        pdfImg.setWidth(2500);
        pdfImg.setHeight(500);
        d.add(pdfImg);
        
        d.close();
    }
    
    public static void main(String[] args) throws IOException {
        long deb = System.currentTimeMillis();
        //PDFGenSimple_PDFBOX();
        long mid = System.currentTimeMillis();
        System.out.println(mid-deb);
        deb = mid;
        
        //PDFGenSimple_PDFBOX();
        mid = System.currentTimeMillis();
        System.out.println(mid-deb);
        deb = mid;
        
        PDFGenSimple_itext();
        
        mid = System.currentTimeMillis();
        System.out.println(mid-deb);
        deb = mid;
        
        PDFGenSimple_itext();
        
        mid = System.currentTimeMillis();
        System.out.println(mid-deb);
        deb = mid;
        
        System.out.println(System.currentTimeMillis()-mid);
    }
    
}
