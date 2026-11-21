package tools;

import javax.imageio.ImageIO;
import java.io.File;
import java.awt.image.BufferedImage;

public class InspectAssets {
    public static void main(String[] args) throws Exception {
        BufferedImage ott = ImageIO.read(new File("watchface/src/main/res/drawable/v5_ottoman.png"));
        System.out.println("v5_ottoman: " + ott.getWidth() + "x" + ott.getHeight());
        BufferedImage dial = ImageIO.read(new File("watchface/src/main/res/drawable/v5_dial.png"));
        System.out.println("v5_dial: " + dial.getWidth() + "x" + dial.getHeight());
        BufferedImage sageOtt = ImageIO.read(new File("watchface/src/main/res/drawable/sage_ottoman.png"));
        System.out.println("sage_ottoman: " + sageOtt.getWidth() + "x" + sageOtt.getHeight());
    }
}
