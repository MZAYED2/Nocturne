package tools;

import javax.imageio.ImageIO;
import java.io.File;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import static java.lang.Math.*;

public class GenerateMasterAssets {

    public static void main(String[] args) throws Exception {
        System.out.println("Starting Master Asset Generation...");

        File outputDirWatchface = new File("watchface/build/generated/refinement/res/drawable");
        File outputDirApp = new File("app/src/main/res/drawable");
        File outputDirWatchfaceSrc = new File("watchface/src/main/res/drawable");
        outputDirWatchface.mkdirs();

        // 1. Generate Sage Dials using Noto Serif (soft, elegant, upright, medium-weight)
        generateSageDials(outputDirWatchface);

        // 2. Generate Photorealistic Lunar Phases (920px diameter, 29 phases, bilinear sampling)
        generateLunarPhases(outputDirWatchface);

        // 3. Generate Obsidian Atelier Assets (Platinum / Titanium / Graphite, NO LOGO)
        generateObsidianAssets(outputDirWatchface);

        // Copy generated assets to app drawable so companion app canvas has them
        copyAssets(outputDirWatchface, outputDirApp);

        System.out.println("Master Asset Generation Completed Successfully!");
    }

    // =========================================================================
    // 1. SAGE NOCTURNE DIALS
    // =========================================================================
    static void generateSageDials(File outDir) throws Exception {
        System.out.println("Generating Sage Dials (Noto Serif)...");
        Font notoSerif = Font.createFont(Font.TRUETYPE_FONT, new File("watchface/src/main/res/font/ref_serif.ttf"));
        Font font = notoSerif.deriveFont(Font.PLAIN, 106f);

        for (int theme = 0; theme <= 4; theme++) {
            for (int style = 0; style <= 2; style++) {
                for (boolean ambient : new boolean[]{false, true}) {
                    BufferedImage img = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = img.createGraphics();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                    java.util.List<Rectangle2D> bounds = new java.util.ArrayList<>();

                    // Hours
                    for (int hour = 1; hour <= 12; hour++) {
                        if (style == 2 || (style == 1 && hour % 3 != 0)) continue;
                        String text = String.valueOf(hour);
                        var glyph = font.createGlyphVector(g.getFontRenderContext(), text);
                        var b = glyph.getVisualBounds();
                        double angle = toRadians(hour * 30.0 - 90.0);
                        double radius = (hour >= 10) ? 426.0 : 434.0;
                        double tx = 500 + cos(angle) * radius - b.getCenterX();
                        double ty = 500 + sin(angle) * radius - b.getCenterY();

                        bounds.add(new Rectangle2D.Double(b.getX() + tx - 6, b.getY() + ty - 6, b.getWidth() + 12, b.getHeight() + 12));

                        // Warm ivory / champagne color matching Image 1
                        Color textColor = ambient ? new Color(0xF1, 0xE6, 0xD2, 175) : new Color(0xF1, 0xE6, 0xD2);
                        g.setColor(textColor);
                        g.drawGlyphVector(glyph, (float)tx, (float)ty);
                    }

                    // Minute Marks
                    for (int m = 0; m < 60; m++) {
                        boolean major = (m % 5 == 0);
                        double angle = toRadians(m * 6.0 - 90);
                        double endR = 484.0;
                        double startR = major ? 462.0 : 472.0;
                        double ex = 500 + cos(angle) * endR;
                        double ey = 500 + sin(angle) * endR;

                        boolean collides = false;
                        for (Rectangle2D rect : bounds) {
                            if (rect.intersectsLine(500 + cos(angle) * startR, 500 + sin(angle) * startR, ex, ey)) {
                                collides = true;
                                break;
                            }
                        }
                        if (!collides) {
                            g.setStroke(new BasicStroke(major ? 4.5f : 2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                            g.setColor(major ? (ambient ? new Color(0xF1, 0xE6, 0xD2, 150) : new Color(0xF1, 0xE6, 0xD2))
                                             : (ambient ? new Color(0x86, 0x9B, 0xA5, 100) : new Color(0x86, 0x9B, 0xA5, 185)));
                            g.draw(new Line2D.Double(500 + cos(angle) * startR, 500 + sin(angle) * startR, ex, ey));
                        }
                    }

                    g.dispose();
                    String name = "refined_" + theme + "_dial_" + style + (ambient ? "_aod" : "");
                    ImageIO.write(img, "png", new File(outDir, name + ".png"));
                }
            }
        }
    }

    // =========================================================================
    // 2. LUNAR PHASES (Photorealistic, 920px diameter, Bilinear sampling)
    // =========================================================================
    static void generateLunarPhases(File outDir) throws Exception {
        System.out.println("Generating Photorealistic Lunar Phases (920px & 216px)...");
        File moonSrcFile = new File("tools/moon-source.jpg");
        BufferedImage srcBmp = moonSrcFile.exists() ? ImageIO.read(moonSrcFile) : null;

        for (int phase = 0; phase <= 28; phase++) {
            BufferedImage bigMoon = renderPhotorealisticMoon(920, phase, false, srcBmp);
            BufferedImage smallMoon = renderPhotorealisticMoon(216, phase, true, srcBmp);

            String pStr = String.format("%02d", phase);
            ImageIO.write(bigMoon, "png", new File(outDir, "lunar_moon_" + pStr + ".png"));
            ImageIO.write(smallMoon, "png", new File(outDir, "refined_moon_" + pStr + ".png"));
        }

        // Also generate Lunar AOD Dial (soft numerals, no moon)
        Font notoSerif = Font.createFont(Font.TRUETYPE_FONT, new File("watchface/src/main/res/font/ref_serif.ttf"));
        Font font = notoSerif.deriveFont(Font.PLAIN, 104f);
        BufferedImage aodDial = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_ARGB);
        Graphics2D lg = aodDial.createGraphics();
        lg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        lg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        java.util.List<Rectangle2D> bounds = new java.util.ArrayList<>();
        for (int hour = 1; hour <= 12; hour++) {
            String text = String.valueOf(hour);
            var glyph = font.createGlyphVector(lg.getFontRenderContext(), text);
            var b = glyph.getVisualBounds();
            double angle = toRadians(hour * 30.0 - 90.0);
            double radius = (hour >= 10) ? 426.0 : 434.0;
            double tx = 500 + cos(angle) * radius - b.getCenterX();
            double ty = 500 + sin(angle) * radius - b.getCenterY();

            bounds.add(new Rectangle2D.Double(b.getX() + tx - 6, b.getY() + ty - 6, b.getWidth() + 12, b.getHeight() + 12));
            lg.setColor(new Color(0xDC, 0xE3, 0xEA, 180));
            lg.drawGlyphVector(glyph, (float)tx, (float)ty);
        }

        for (int m = 0; m < 60; m++) {
            boolean major = (m % 5 == 0);
            double angle = toRadians(m * 6.0 - 90);
            double endR = 484.0;
            double startR = major ? 462.0 : 472.0;
            double ex = 500 + cos(angle) * endR;
            double ey = 500 + sin(angle) * endR;

            boolean collides = false;
            for (Rectangle2D rect : bounds) {
                if (rect.intersectsLine(500 + cos(angle) * startR, 500 + sin(angle) * startR, ex, ey)) {
                    collides = true;
                    break;
                }
            }
            if (!collides) {
                lg.setStroke(new BasicStroke(major ? 4.0f : 2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                lg.setColor(major ? new Color(0xDC, 0xE3, 0xEA, 140) : new Color(0x71, 0x80, 0x96, 90));
                lg.draw(new Line2D.Double(500 + cos(angle) * startR, 500 + sin(angle) * startR, ex, ey));
            }
        }
        lg.dispose();
        ImageIO.write(aodDial, "png", new File(outDir, "lunar_dial_aod.png"));
    }

    static BufferedImage renderPhotorealisticMoon(int size, int phase, boolean isSmall, BufferedImage srcBmp) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        double R = size / 2.0 - 1.0;
        double cx = size / 2.0;
        double cy = size / 2.0;
        double t = phase / 28.0;

        // Astronomical lighting direction
        double sx = sin(2.0 * PI * t);
        double sy = 0.0;
        double sz = -cos(2.0 * PI * t);

        double srcW = srcBmp != null ? srcBmp.getWidth() : 1024.0;
        double srcH = srcBmp != null ? srcBmp.getHeight() : 1024.0;
        double srcR = srcW * 0.495;
        double srcCx = srcW * 0.5;
        double srcCy = srcH * 0.5;

        for (int y = 0; y < size; y++) {
            double dy = y + 0.5 - cy;
            for (int x = 0; x < size; x++) {
                double dx = x + 0.5 - cx;
                double dist = hypot(dx, dy);
                if (dist > R + 0.6) continue;

                double nx = dx / R;
                double ny = dy / R;
                double nz = sqrt(max(0.0, 1.0 - nx * nx - ny * ny));

                // Bilinear texture sampling for ultra-smooth texture without block pixelation
                double texVal;
                if (srcBmp != null) {
                    double u = srcCx + nx * srcR;
                    double v = srcCy + ny * srcR;
                    texVal = sampleBilinear(srcBmp, u, v);
                } else {
                    texVal = 0.80;
                }

                // Contrast curve
                double cTex = ((texVal - 0.5) * (isSmall ? 1.25 : 1.15) + 0.5);
                cTex = max(0.0, min(1.0, cTex));

                // Lommel-Seeliger scattering + diffuse illumination
                double cosIncident = nx * sx + ny * sy + nz * sz;
                double transWidth = 0.045; // Smooth sub-pixel transition
                double sunlitFade = max(0.0, min(1.0, (cosIncident / transWidth) + 0.5));

                // Natural lunar scattering (retro-reflective regolith)
                double sunlitLum = max(0.0, min(1.0, 0.25 + 0.75 * max(0.0, cosIncident)));

                // Earthshine on unlit portion
                double earthshineBase = isSmall ? 0.12 : 0.08;
                double earthshine = earthshineBase * (0.75 + 0.25 * (1.0 - cos(2.0 * PI * t))) * (0.6 + 0.4 * nz);

                double illumination = sunlitFade * sunlitLum + (1.0 - sunlitFade) * earthshine;

                // Subtle ash light / limb glow
                double distToEdge = R - dist;
                double rimStrength = isSmall ? 0.28 : 0.18;
                double rim = 0.0;
                if (distToEdge >= 0.0 && distToEdge <= 2.5) {
                    double curve = max(0.0, min(1.0, 1.0 - abs(distToEdge - 1.0) / 1.5));
                    rim = rimStrength * curve * (1.0 - sunlitFade * 0.85);
                }

                double finalLuminance = max(0.0, min(1.0, cTex * illumination + rim));

                // Color tint: crisp moonlight silver/white
                int rOut = (int)round(max(0.0, min(255.0, finalLuminance * 242.0 + 7.0)));
                int gOut = (int)round(max(0.0, min(255.0, finalLuminance * 238.0 + 7.0)));
                int bOut = (int)round(max(0.0, min(255.0, finalLuminance * 230.0 + 8.0)));

                // Anti-aliased outer edge
                double edgeAlpha = (dist > R - 0.75) ? max(0.0, min(1.0, (R + 0.6 - dist) / 1.35)) : 1.0;
                int alphaOut = (int)round(edgeAlpha * 255.0);

                img.setRGB(x, y, (alphaOut << 24) | (rOut << 16) | (gOut << 8) | bOut);
            }
        }
        return img;
    }

    static double sampleBilinear(BufferedImage bmp, double u, double v) {
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        int x0 = (int)floor(u);
        int y0 = (int)floor(v);
        int x1 = min(w - 1, x0 + 1);
        int y1 = min(h - 1, y0 + 1);
        x0 = max(0, min(w - 1, x0));
        y0 = max(0, min(h - 1, y0));

        double fx = u - x0;
        double fy = v - y0;

        double c00 = getLuminance(bmp.getRGB(x0, y0));
        double c10 = getLuminance(bmp.getRGB(x1, y0));
        double c01 = getLuminance(bmp.getRGB(x0, y1));
        double c11 = getLuminance(bmp.getRGB(x1, y1));

        double top = c00 * (1 - fx) + c10 * fx;
        double bottom = c01 * (1 - fx) + c11 * fx;
        return top * (1 - fy) + bottom * fy;
    }

    static double getLuminance(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (r * 0.299 + g * 0.587 + b * 0.114) / 255.0;
    }

    // =========================================================================
    // 3. OBSIDIAN ATELIER (Brushed Platinum, Soft Titanium, Smoked Graphite)
    // =========================================================================
    static void generateObsidianAssets(File outDir) throws Exception {
        System.out.println("Generating Obsidian Atelier Haute Horlogerie Assets (Zero Gold, Pure Platinum/Titanium)...");
        Font notoSerif = Font.createFont(Font.TRUETYPE_FONT, new File("watchface/src/main/res/font/ref_serif.ttf"));

        // 3.1 ACTIVE DIAL (1000x1000)
        BufferedImage dial = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dial.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Circular clip
        g.setClip(new Ellipse2D.Double(0, 0, 1000, 1000));

        // Base Smoked Graphite Gradient
        Point2D center = new Point2D.Float(500, 500);
        float radius = 500f;
        float[] dists = {0.0f, 0.55f, 0.85f, 1.0f};
        Color[] colors = {
            new Color(0x22, 0x25, 0x2A), // Soft inner graphite
            new Color(0x17, 0x19, 0x1D),
            new Color(0x0E, 0x10, 0x12),
            new Color(0x06, 0x07, 0x08)  // Deep perimeter
        };
        RadialGradientPaint p = new RadialGradientPaint(center, radius, dists, colors);
        g.setPaint(p);
        g.fillRect(0, 0, 1000, 1000);

        // Fine Radial Brush Texture (Haute Horlogerie metallic sunburst)
        for (int i = 0; i < 720; i++) {
            double angle = toRadians(i * 0.5);
            double noise = (sin(i * 13.7) * cos(i * 7.3)) * 0.5 + 0.5;
            int alpha = (int)(6 + noise * 10);
            g.setColor(new Color(255, 255, 255, alpha));
            g.setStroke(new BasicStroke(1.0f));
            g.draw(new Line2D.Double(500, 500, 500 + cos(angle) * 500, 500 + sin(angle) * 500));
        }

        // Concentric Micro-Tracks (Fine brushed circular lines)
        for (int r = 100; r < 490; r += 16) {
            g.setColor(new Color(255, 255, 255, 4));
            g.setStroke(new BasicStroke(0.6f));
            g.draw(new Ellipse2D.Double(500 - r, 500 - r, r * 2, r * 2));
        }

        // Outer Metallic Bevel Rim (Platinum / Titanium)
        g.setStroke(new BasicStroke(6f));
        g.setColor(new Color(0x4A, 0x55, 0x68, 160));
        g.draw(new Ellipse2D.Double(3, 3, 994, 994));
        g.setStroke(new BasicStroke(2f));
        g.setColor(new Color(0xE2, 0xE8, 0xF0, 210));
        g.draw(new Ellipse2D.Double(1, 1, 998, 998));

        // Subdial 1: Sunken Small Seconds Counter at 6 o'clock (center: 500, 715; radius: 155)
        int ssCx = 500;
        int ssCy = 715;
        int ssR = 155;

        // Drop shadow for sunken depth
        g.setColor(new Color(0, 0, 0, 140));
        g.fill(new Ellipse2D.Double(ssCx - ssR - 2, ssCy - ssR - 1, (ssR + 2) * 2, (ssR + 2) * 2));

        // Subdial inner graphite surface
        g.setColor(new Color(0x13, 0x15, 0x18));
        g.fill(new Ellipse2D.Double(ssCx - ssR, ssCy - ssR, ssR * 2, ssR * 2));

        // Concentric circular guilloché / snailed graining
        for (int gr = 10; gr < ssR - 4; gr += 3) {
            g.setColor(new Color(255, 255, 255, (gr % 6 == 0) ? 14 : 7));
            g.setStroke(new BasicStroke(0.75f));
            g.draw(new Ellipse2D.Double(ssCx - gr, ssCy - gr, gr * 2, gr * 2));
        }

        // Platinum beveled rim of small seconds subdial
        g.setStroke(new BasicStroke(2.5f));
        g.setColor(new Color(0x94, 0xA3, 0xB8, 180));
        g.draw(new Ellipse2D.Double(ssCx - ssR, ssCy - ssR, ssR * 2, ssR * 2));
        g.setStroke(new BasicStroke(1.2f));
        g.setColor(new Color(0xE2, 0xE8, 0xF0, 230));
        g.draw(new Ellipse2D.Double(ssCx - ssR + 1, ssCy - ssR + 1, (ssR - 1) * 2, (ssR - 1) * 2));

        // Small Seconds Numerals: 60, 15, 30, 45 & Tick Marks
        Font functionalFont = Font.createFont(Font.TRUETYPE_FONT, new File("watchface/src/main/res/font/functional_medium.ttf"));
        Font ssFont = functionalFont.deriveFont(Font.PLAIN, 28f);
        g.setFont(ssFont);
        g.setColor(new Color(0xDC, 0xE3, 0xEA));

        // 60, 15, 30, 45
        drawCenteredText(g, "60", ssCx, ssCy - ssR + 28);
        drawCenteredText(g, "30", ssCx, ssCy + ssR - 16);
        drawCenteredText(g, "15", ssCx + ssR - 26, ssCy + 10);
        drawCenteredText(g, "45", ssCx - ssR + 26, ssCy + 10);

        // Small seconds tick marks (every 5 seconds major, every 1 second minor)
        for (int s = 0; s < 60; s++) {
            if (s % 15 == 0) continue; // skip numbers
            double angle = toRadians(s * 6.0 - 90);
            double outerD = ssR - 6;
            double innerD = (s % 5 == 0) ? ssR - 16 : ssR - 12;
            g.setStroke(new BasicStroke((s % 5 == 0) ? 2.0f : 1.0f));
            g.setColor((s % 5 == 0) ? new Color(0xDC, 0xE3, 0xEA, 200) : new Color(0x94, 0xA3, 0xB8, 120));
            g.draw(new Line2D.Double(ssCx + cos(angle) * innerD, ssCy + sin(angle) * innerD,
                                    ssCx + cos(angle) * outerD, ssCy + sin(angle) * outerD));
        }

        // Subdial 2: Power Reserve Gauge at 9 o'clock (center: 215, 475; radius: 115)
        int prCx = 215;
        int prCy = 475;
        int prR = 115;

        // Recessed shadow & inner surface
        g.setColor(new Color(0, 0, 0, 120));
        g.fill(new Ellipse2D.Double(prCx - prR - 2, prCy - prR - 1, (prR + 2) * 2, (prR + 2) * 2));
        g.setColor(new Color(0x13, 0x15, 0x18));
        g.fill(new Ellipse2D.Double(prCx - prR, prCy - prR, prR * 2, prR * 2));

        // Guilloché concentric texture
        for (int gr = 10; gr < prR - 4; gr += 3) {
            g.setColor(new Color(255, 255, 255, (gr % 6 == 0) ? 12 : 6));
            g.setStroke(new BasicStroke(0.7f));
            g.draw(new Ellipse2D.Double(prCx - gr, prCy - gr, gr * 2, gr * 2));
        }

        // Platinum beveled rim
        g.setStroke(new BasicStroke(2.2f));
        g.setColor(new Color(0x94, 0xA3, 0xB8, 180));
        g.draw(new Ellipse2D.Double(prCx - prR, prCy - prR, prR * 2, prR * 2));
        g.setStroke(new BasicStroke(1.0f));
        g.setColor(new Color(0xE2, 0xE8, 0xF0, 220));
        g.draw(new Ellipse2D.Double(prCx - prR + 1, prCy - prR + 1, (prR - 1) * 2, (prR - 1) * 2));

        // Stepped Power Reserve Arc (between 140 and 280 degrees)
        for (int b = 0; b < 10; b++) {
            double angle = toRadians(145 + b * 13.5);
            double arcInner = prR - 36 - (b * 1.5);
            double arcOuter = prR - 14;
            g.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // Stepped color gradient in soft titanium/platinum
            g.setColor(new Color(0xDC, 0xE3, 0xEA, 180 + b * 7));
            g.draw(new Line2D.Double(prCx + cos(angle) * arcInner, prCy + sin(angle) * arcInner,
                                    prCx + cos(angle) * arcOuter, prCy + sin(angle) * arcOuter));
        }

        // Battery Icon inside Power Reserve
        drawBatteryIcon(g, prCx - 20, prCy + 10, 40, 22, new Color(0xDC, 0xE3, 0xEA, 220));
        g.setFont(functionalFont.deriveFont(Font.BOLD, 26f));
        g.setColor(new Color(0xDC, 0xE3, 0xEA));
        drawCenteredText(g, "72%", prCx, prCy + 54);

        // Feature 3: Recessed Angled Date Window at 4:30 position
        // Center around x=775, y=595, rotated ~32 degrees
        AffineTransform oldTx = g.getTransform();
        g.translate(775, 595);
        g.rotate(toRadians(32));

        // Recessed outer dark bevel
        g.setColor(new Color(0, 0, 0, 200));
        g.fill(new RoundRectangle2D.Double(-42, -32, 84, 64, 8, 8));

        // Platinum beveled frame
        g.setColor(new Color(0x94, 0xA3, 0xB8));
        g.setStroke(new BasicStroke(3.5f));
        g.draw(new RoundRectangle2D.Double(-40, -30, 80, 60, 6, 6));

        g.setColor(new Color(0xF1, 0xF5, 0xF9));
        g.setStroke(new BasicStroke(1.2f));
        g.draw(new RoundRectangle2D.Double(-38, -28, 76, 56, 5, 5));

        // Date window inner aperture (dark charcoal)
        g.setColor(new Color(0x0F, 0x11, 0x14));
        g.fill(new RoundRectangle2D.Double(-36, -26, 72, 52, 4, 4));

        // Date Disc Numeral ("12") in crisp soft white
        g.setFont(notoSerif.deriveFont(Font.PLAIN, 40f));
        g.setColor(new Color(0xF8, 0xFA, 0xFC));
        drawCenteredText(g, "12", 0, 14);

        g.setTransform(oldTx);

        // Applied 3D Faceted Smoked Titanium Hour Batons
        // Double baton at 12 o'clock, single batons at 1, 2, 3, 5, 7, 8, 10, 11
        for (int h = 1; h <= 12; h++) {
            if (h == 6) continue; // Small seconds counter is at 6
            if (h == 9) continue; // Power reserve is at 9
            if (h == 4) continue; // Date window is at 4:30

            double angle = toRadians(h * 30.0 - 90.0);
            if (h == 12) {
                // Double baton at 12
                drawFacetedBaton(g, 500 - 12, 500, angle, 400, 470, 14);
                drawFacetedBaton(g, 500 + 12, 500, angle, 400, 470, 14);
            } else {
                drawFacetedBaton(g, 500, 500, angle, 405, 470, 16);
            }
        }

        // Perimeter Minute Ticks (Fine crisp batons)
        for (int m = 0; m < 60; m++) {
            if (m % 5 == 0) continue; // major hours have applied batons
            double angle = toRadians(m * 6.0 - 90);
            double startR = 472;
            double endR = 484;
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(0x94, 0xA3, 0xB8, 160));
            g.draw(new Line2D.Double(500 + cos(angle) * startR, 500 + sin(angle) * startR,
                                    500 + cos(angle) * endR, 500 + sin(angle) * endR));
        }

        // REMINDER: ABSOLUTELY NO LOGO, NO BRANDING, NO TEXT ABOVE CENTER!
        // The upper dial remains pristine brushed obsidian graphite.

        g.dispose();
        ImageIO.write(dial, "png", new File(outDir, "obsidian_dial.png"));

        // 3.2 OBSIDIAN AOD DIAL (AMOLED Black, Applied Markers, Date Window, Small Seconds Contour)
        BufferedImage aod = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_ARGB);
        Graphics2D ag = aod.createGraphics();
        ag.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        ag.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // AMOLED Pure Black Background
        ag.setColor(Color.BLACK);
        ag.fillOval(0, 0, 1000, 1000);

        // Subdial rim outlines
        ag.setStroke(new BasicStroke(1.5f));
        ag.setColor(new Color(0x64, 0x74, 0x8B, 120));
        ag.draw(new Ellipse2D.Double(ssCx - ssR, ssCy - ssR, ssR * 2, ssR * 2));
        ag.draw(new Ellipse2D.Double(prCx - prR, prCy - prR, prR * 2, prR * 2));

        // Applied batons in AOD
        for (int h = 1; h <= 12; h++) {
            if (h == 6 || h == 9 || h == 4) continue;
            double angle = toRadians(h * 30.0 - 90.0);
            if (h == 12) {
                drawFacetedBatonAod(ag, 500 - 12, 500, angle, 400, 470, 14);
                drawFacetedBatonAod(ag, 500 + 12, 500, angle, 400, 470, 14);
            } else {
                drawFacetedBatonAod(ag, 500, 500, angle, 405, 470, 16);
            }
        }

        // Date Window Frame in AOD
        oldTx = ag.getTransform();
        ag.translate(775, 595);
        ag.rotate(toRadians(32));
        ag.setColor(new Color(0x64, 0x74, 0x8B, 180));
        ag.setStroke(new BasicStroke(2.0f));
        ag.draw(new RoundRectangle2D.Double(-40, -30, 80, 60, 6, 6));
        ag.setFont(notoSerif.deriveFont(Font.PLAIN, 40f));
        ag.setColor(new Color(0x94, 0xA3, 0xB8, 200));
        drawCenteredText(ag, "12", 0, 14);
        ag.setTransform(oldTx);

        // Minute dots in AOD
        for (int m = 0; m < 60; m++) {
            if (m % 5 == 0) continue;
            double angle = toRadians(m * 6.0 - 90);
            ag.setColor(new Color(0x47, 0x55, 0x69, 110));
            ag.fill(new Ellipse2D.Double(500 + cos(angle) * 478 - 1.5, 500 + sin(angle) * 478 - 1.5, 3, 3));
        }

        ag.dispose();
        ImageIO.write(aod, "png", new File(outDir, "obsidian_dial_aod.png"));

        // 3.3 3D FACETED DAUPHINE HANDS (Brushed Platinum & Polished Titanium)
        generateFacetedDauphineHands(outDir);
    }

    static void drawFacetedBaton(Graphics2D g, double cx, double cy, double angle, double r1, double r2, double width) {
        AffineTransform old = g.getTransform();
        g.translate(cx, cy);
        g.rotate(angle);

        double length = r2 - r1;
        double halfW = width / 2.0;

        // Drop shadow for applied 3D relief
        g.setColor(new Color(0, 0, 0, 140));
        g.fill(new RoundRectangle2D.Double(r1 - 1, -halfW + 2, length + 2, width, 4, 4));

        // Faceted Left side (lit platinum facet)
        Path2D.Double left = new Path2D.Double();
        left.moveTo(r1, 0);
        left.lineTo(r1 + 6, -halfW);
        left.lineTo(r2 - 4, -halfW);
        left.lineTo(r2, 0);
        left.closePath();
        g.setColor(new Color(0xF1, 0xF5, 0xF9));
        g.fill(left);

        // Faceted Right side (shadowed titanium facet)
        Path2D.Double right = new Path2D.Double();
        right.moveTo(r1, 0);
        right.lineTo(r1 + 6, halfW);
        right.lineTo(r2 - 4, halfW);
        right.lineTo(r2, 0);
        right.closePath();
        g.setColor(new Color(0x94, 0xA3, 0xB8));
        g.fill(right);

        // Center spine highlight
        g.setColor(new Color(0xFF, 0xFF, 0xFF, 200));
        g.setStroke(new BasicStroke(1.0f));
        g.draw(new Line2D.Double(r1, 0, r2, 0));

        g.setTransform(old);
    }

    static void drawFacetedBatonAod(Graphics2D g, double cx, double cy, double angle, double r1, double r2, double width) {
        AffineTransform old = g.getTransform();
        g.translate(cx, cy);
        g.rotate(angle);

        double length = r2 - r1;
        double halfW = width / 2.0;

        g.setColor(new Color(0xE2, 0xE8, 0xF0, 220));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new RoundRectangle2D.Double(r1, -halfW, length, width, 3, 3));

        g.setTransform(old);
    }

    static void drawBatteryIcon(Graphics2D g, int x, int y, int w, int h, Color c) {
        g.setColor(c);
        g.setStroke(new BasicStroke(2.2f));
        g.draw(new RoundRectangle2D.Double(x, y, w - 4, h, 4, 4));
        g.fill(new RoundRectangle2D.Double(x + w - 3, y + h * 0.28, 3, h * 0.44, 2, 2));
        g.fill(new RoundRectangle2D.Double(x + 4, y + 4, (w - 12) * 0.72, h - 8, 2, 2));
    }

    static void drawCenteredText(Graphics2D g, String text, int x, int y) {
        FontMetrics fm = g.getFontMetrics();
        int tx = x - fm.stringWidth(text) / 2;
        g.drawString(text, tx, y);
    }

    // 3.3 3D FACETED DAUPHINE HANDS
    static void generateFacetedDauphineHands(File outDir) throws Exception {
        // Hour Hand: 72x520 (pivot at y=464, length 400)
        BufferedImage hour = createDauphineHand(72, 520, 464, false);
        BufferedImage hourAod = createDauphineHandAod(72, 520, 464);
        ImageIO.write(hour, "png", new File(outDir, "obsidian_hour.png"));
        ImageIO.write(hourAod, "png", new File(outDir, "obsidian_hour_aod.png"));

        // Minute Hand: 64x720 (pivot at y=663, length 560)
        BufferedImage min = createDauphineHand(64, 720, 663, true);
        BufferedImage minAod = createDauphineHandAod(64, 720, 663);
        ImageIO.write(min, "png", new File(outDir, "obsidian_minute.png"));
        ImageIO.write(minAod, "png", new File(outDir, "obsidian_minute_aod.png"));

        // Small Seconds Hand: 24x280 (needle with counterweight)
        BufferedImage ss = createSmallSecHand(24, 280, 210);
        ImageIO.write(ss, "png", new File(outDir, "obsidian_small_sec.png"));

        // Power Reserve Needle: 24x200
        BufferedImage pr = createPowerHand(24, 200, 160);
        ImageIO.write(pr, "png", new File(outDir, "obsidian_power_hand.png"));

        // Multi-layered Center Hub: 80x80
        BufferedImage hub = createCenterHub(80);
        ImageIO.write(hub, "png", new File(outDir, "obsidian_hub.png"));
    }

    static BufferedImage createDauphineHand(int w, int h, int pivotY, boolean isMinute) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double cx = w / 2.0;
        double tipY = 16.0;
        double tailY = h - 16.0;
        double midY = tipY + (pivotY - tipY) * 0.70;
        double maxHalfW = (w / 2.0) - 4.0;

        // Left Facet (Light Brushed Platinum)
        Path2D.Double left = new Path2D.Double();
        left.moveTo(cx, tipY);
        left.lineTo(cx - maxHalfW, midY);
        left.lineTo(cx - maxHalfW * 0.45, pivotY);
        left.lineTo(cx, tailY);
        left.closePath();

        GradientPaint pLeft = new GradientPaint(
            (float)cx, (float)tipY, new Color(0xF8, 0xFA, 0xFC),
            (float)(cx - maxHalfW), (float)midY, new Color(0xCB, 0xD5, 0xE1)
        );
        g.setPaint(pLeft);
        g.fill(left);

        // Right Facet (Shadowed Polished Titanium)
        Path2D.Double right = new Path2D.Double();
        right.moveTo(cx, tipY);
        right.lineTo(cx + maxHalfW, midY);
        right.lineTo(cx + maxHalfW * 0.45, pivotY);
        right.lineTo(cx, tailY);
        right.closePath();

        GradientPaint pRight = new GradientPaint(
            (float)cx, (float)tipY, new Color(0x94, 0xA3, 0xB8),
            (float)(cx + maxHalfW), (float)midY, new Color(0x47, 0x55, 0x69)
        );
        g.setPaint(pRight);
        g.fill(right);

        // Crisp central ridge line
        g.setColor(new Color(0xFF, 0xFF, 0xFF, 220));
        g.setStroke(new BasicStroke(1.2f));
        g.draw(new Line2D.Double(cx, tipY + 4, cx, tailY - 4));

        g.dispose();
        return img;
    }

    static BufferedImage createDauphineHandAod(int w, int h, int pivotY) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double cx = w / 2.0;
        double tipY = 16.0;
        double tailY = h - 16.0;
        double midY = tipY + (pivotY - tipY) * 0.70;
        double maxHalfW = (w / 2.0) - 4.0;

        Path2D.Double contour = new Path2D.Double();
        contour.moveTo(cx, tipY);
        contour.lineTo(cx - maxHalfW, midY);
        contour.lineTo(cx - maxHalfW * 0.45, pivotY);
        contour.lineTo(cx, tailY);
        contour.lineTo(cx + maxHalfW * 0.45, pivotY);
        contour.lineTo(cx + maxHalfW, midY);
        contour.closePath();

        g.setColor(new Color(0xE2, 0xE8, 0xF0, 230));
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(contour);

        g.dispose();
        return img;
    }

    static BufferedImage createSmallSecHand(int w, int h, int pivotY) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double cx = w / 2.0;
        g.setColor(new Color(0xE2, 0xE8, 0xF0));
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(cx, pivotY, cx, 12));

        // Tail counterweight
        g.setColor(new Color(0x94, 0xA3, 0xB8));
        g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(cx, pivotY, cx, h - 12));

        // Round pivot disc
        g.setColor(new Color(0xF1, 0xF5, 0xF9));
        g.fill(new Ellipse2D.Double(cx - 5, pivotY - 5, 10, 10));
        g.setColor(new Color(0x33, 0x41, 0x55));
        g.fill(new Ellipse2D.Double(cx - 2, pivotY - 2, 4, 4));

        g.dispose();
        return img;
    }

    static BufferedImage createPowerHand(int w, int h, int pivotY) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double cx = w / 2.0;
        g.setColor(new Color(0xE2, 0xE8, 0xF0));
        g.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(cx, pivotY, cx, 16));

        // Tail
        g.setColor(new Color(0x64, 0x74, 0x8B));
        g.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(cx, pivotY, cx, h - 14));

        g.setColor(new Color(0xF1, 0xF5, 0xF9));
        g.fill(new Ellipse2D.Double(cx - 6, pivotY - 6, 12, 12));
        g.setColor(new Color(0x1E, 0x29, 0x3B));
        g.fill(new Ellipse2D.Double(cx - 2.5, pivotY - 2.5, 5, 5));

        g.dispose();
        return img;
    }

    static BufferedImage createCenterHub(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double c = size / 2.0;
        // Outer beveled ring
        g.setColor(new Color(0x33, 0x41, 0x55));
        g.fill(new Ellipse2D.Double(2, 2, size - 4, size - 4));
        // Platinum tier
        g.setColor(new Color(0xE2, 0xE8, 0xF0));
        g.fill(new Ellipse2D.Double(8, 8, size - 16, size - 16));
        // Titanium inner tier
        g.setColor(new Color(0x64, 0x74, 0x8B));
        g.fill(new Ellipse2D.Double(16, 16, size - 32, size - 32));
        // Center pin
        g.setColor(new Color(0x0F, 0x17, 0x2A));
        g.fill(new Ellipse2D.Double(c - 6, c - 6, 12, 12));

        g.dispose();
        return img;
    }

    static void copyAssets(File srcDir, File destDir) {
        if (!destDir.exists()) destDir.mkdirs();
        File[] files = srcDir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && f.getName().endsWith(".png")) {
                    File target = new File(destDir, f.getName());
                    try {
                        java.nio.file.Files.copy(f.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
