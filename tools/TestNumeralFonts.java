package tools;

import javax.imageio.ImageIO;
import java.io.File;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.geom.*;
import static java.lang.Math.*;

public class TestNumeralFonts {
    public static void main(String[] args) throws Exception {
        Font notoSerif = Font.createFont(Font.TRUETYPE_FONT, new File("watchface/src/main/res/font/ref_serif.ttf")).deriveFont(Font.PLAIN, 108f);
        Font georgia = new Font("Georgia", Font.PLAIN, 108);
        Font palatino = new Font("Palatino Linotype", Font.PLAIN, 108);
        Font cormorant = Font.createFont(Font.TRUETYPE_FONT, new File("tools/CormorantGaramond-SemiBold.ttf")).deriveFont(Font.PLAIN, 114f);

        renderDial("dial_noto_serif.png", notoSerif, "Noto Serif");
        renderDial("dial_georgia.png", georgia, "Georgia");
        renderDial("dial_palatino.png", palatino, "Palatino Linotype");
        renderDial("dial_cormorant.png", cormorant, "Cormorant");
        System.out.println("Rendered comparison dials");
    }

    static void renderDial(String filename, Font font, String label) throws Exception {
        BufferedImage img = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 1000, 1000);

        for (int hour = 1; hour <= 12; hour++) {
            String text = String.valueOf(hour);
            var glyph = font.createGlyphVector(g.getFontRenderContext(), text);
            var b = glyph.getVisualBounds();
            double angle = Math.toRadians(hour * 30.0 - 90.0);
            double radius = (hour >= 10) ? 426.0 : 434.0;
            double tx = 500 + cos(angle) * radius - b.getCenterX();
            double ty = 500 + sin(angle) * radius - b.getCenterY();

            g.setColor(new Color(0xF1, 0xE6, 0xD2));
            g.drawGlyphVector(glyph, (float)tx, (float)ty);
        }

        // Minute tick marks
        for (int m = 0; m < 60; m++) {
            boolean major = (m % 5 == 0);
            double angle = Math.toRadians(m * 6.0 - 90);
            double endR = 484.0;
            double startR = major ? 462.0 : 472.0;
            g.setStroke(new BasicStroke(major ? 4.5f : 2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(major ? new Color(0xF1, 0xE6, 0xD2) : new Color(0x86, 0x9B, 0xA5, 190));
            g.draw(new Line2D.Double(500 + cos(angle) * startR, 500 + sin(angle) * startR, 500 + cos(angle) * endR, 500 + sin(angle) * endR));
        }

        g.dispose();
        ImageIO.write(img, "png", new File("C:/Users/mohamed zayed/.gemini/antigravity-acp/brain/2d4c81b1-82e2-4031-b244-001e9e90dc8f/" + filename));
    }
}
