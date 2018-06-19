import java.text.ParseException;

import javax.swing.text.MaskFormatter;

import org.junit.Test;

public class HymirTest {
    
    @Test
    public void ImageList() {
        String imageList="I1KG120990001.jpg:651";
        String[] part=imageList.split(":");
        String pages[]=part[0].split("\\.");
        String firstPage=pages[0].substring(pages[0].length()-4);
        String root=pages[0].substring(0,pages[0].length()-4);
        for(int x=Integer.parseInt(firstPage);x<Integer.parseInt(part[1])+1;x++) {
            System.out.println(root+String.format("%04d", x)+"."+pages[1]);
        }
    }

}
