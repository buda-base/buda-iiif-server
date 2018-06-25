package io.bdrc.pdf;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PdfCacheCleaner extends Thread{
    
    public static final String PDF_DIR="pdf/"; 
    
    @Override
    public void run() {
        try {
            while (true) {
                cleanup(); 
                //Checks daily
                Thread.sleep(86400000);
            }

        } catch (InterruptedException ie) {
            ie.printStackTrace();
        } 
        catch (IOException io) {
            io.printStackTrace();
        } 
    }
    
    private void cleanup() throws IOException {
        System.out.println("Pdf cleanup ........");
        DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(PDF_DIR));
        for (Path path : stream) {            
            String tmp=path.toString();
            //Filtering arq files
            if(tmp.endsWith(".pdf")) {
                File pdf=new File(path.toAbsolutePath().toString());
                //pdf is deleted if older than 7 days,
                if(System.currentTimeMillis()-pdf.lastModified()> 604800000 ) {
                    System.out.println("Pdf deleted >>> "+path.toAbsolutePath().toString());
                    //Sleep for a day
                    pdf.delete();
                }
            }
        }
        
    }

}
