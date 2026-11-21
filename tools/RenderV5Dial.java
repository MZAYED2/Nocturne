package tools;

import javax.imageio.ImageIO;
import java.io.File;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class RenderV5Dial {
    public static void main(String[] args) throws Exception {
        BufferedImage v5 = ImageIO.read(new File("watchface/src/main/res/drawable/v5_dial.png"));
        BufferedImage out = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(v5, 0, 0, 1000, 1000, null);
        g.dispose();
        ImageIO.write(out, "png", new File("C:/Users/mohamed zayed/.gemini/antigravity-acp/brain/2d4c81b1-82e2-4031-b244-001e9e90dc8f/v5_dial_1000.png"));
        System.out.println("Written v5_dial_1000.png");
    }
}
