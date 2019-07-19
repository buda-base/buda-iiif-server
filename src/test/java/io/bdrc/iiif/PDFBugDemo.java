package io.bdrc.iiif;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfVersion;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.WriterProperties;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;

public class PDFBugDemo {

    static void testIText() throws IOException {
        // generate a one page PDF with itext from src/test/resources/default.tif
        // output pdf in /tmp/testitext.pdf
        PdfWriter writer = new PdfWriter("testPdfItext.pdf", new WriterProperties().setPdfVersion(PdfVersion.PDF_2_0));
        PdfDocument pdfDocument = new PdfDocument(writer);
        Document document = new Document(pdfDocument);
        FileInputStream in = new FileInputStream("src/test/resources/test.tif");
        byte[] img = IOUtils.toByteArray(in);
        Image image = new Image(ImageDataFactory.create(img));
        pdfDocument.addNewPage(new PageSize(image.getImageWidth(), image.getImageHeight()));
        document.add(image);
        document.close();
        writer.close();
    }

    static void testApachePDFBox() throws IOException {
        PDDocument doc = new PDDocument();
        File img = new File("src/test/resources/test.tif");
        BufferedImage bImg = ImageIO.read(img);
        PDPage page = new PDPage(new PDRectangle(bImg.getWidth(), bImg.getHeight()));
        doc.addPage(page);
        PDImageXObject pdImage = LosslessFactory.createFromImage(doc, bImg);
        PDPageContentStream contents = new PDPageContentStream(doc, page);
        contents.drawImage(pdImage, 0, 0);
        contents.close();
        doc.save(new File("testPdfBox.pdf"));
        doc.close();
    }

    public static void main(String[] args) throws IOException {
        PDFBugDemo.testIText();
        PDFBugDemo.testApachePDFBox();
    }

}
