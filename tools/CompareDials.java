package tools;

import javax.imageio.ImageIO;
import java.io.File;
import java.awt.image.BufferedImage;
import java.awt.Color;

public class CompareDials {
    public static void main(String[] args) throws Exception {
        BufferedImage v5 = ImageIO.read(new File("watchface/src/main/res/drawable/v5_dial.png"));
        System.out.println("v5_dial: " + v5.getWidth() + "x" + v5.getHeight());
        BufferedImage ref = ImageIO.read(new File("watchface/build/generated/refinement/res/drawable/refined_0_dial_0.png"));
        System.out.println("refined_0_dial_0: " + ref.getWidth() + "x" + ref.getHeight());
    }
}
