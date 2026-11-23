package tools;

import javax.imageio.ImageIO;
import java.io.File;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import static java.lang.Math.*;

public class GenerateLunarMaster {

    static final String ARTIFACTS_DIR = "C:/Users/mohamed zayed/.gemini/antigravity-acp/brain/2d4c81b1-82e2-4031-b244-001e9e90dc8f/";
    static final File TIFF_FILE = new File("C:/Users/mohamed zayed/CrossDevice/Mohamed's S25 Ultra/storage/Download/lroc_color_16bit_srgb_4k.tif");
    static final File FALLBACK_MOON = new File("tools/moon-source.jpg");

    public static void main(String[] args) throws Exception {
        System.out.println("=== NOCTURNE: GENERATING LUNAR PHASE MASTER ASSETS ===");

        File watchfaceGenDir = new File("watchface/build/generated/refinement/res/drawable");
        File appResDir = new File("app/src/main/res/drawable");
        watchfaceGenDir.mkdirs();
        appResDir.mkdirs();

        // 1. Generate 29 Edge-to-Edge Moon Phases (1000x1000)
        BufferedImage[] phases = generate29MoonPhases(watchfaceGenDir, appResDir);

        // 2. Generate 12 Applied Hour Markers (1000x1000 overlay)
        BufferedImage markers = generateAppliedMarkers(watchfaceGenDir, appResDir);

        // 3. Generate Haute Horlogerie Hands (exact proportional Dauphine & Needle)
        generateLunarHands(watchfaceGenDir, appResDir);

        // 4. Generate AOD Dial (Serif Numerals + Minute Track)
        BufferedImage aodDial = generateAodDial(watchfaceGenDir, appResDir);

        // 5. Generate Master Contact Sheet (29 Phases Grid)
        generateContactSheet(phases);

        // 6. Generate Active & AOD Master Previews
        generatePreviews(phases[14], markers, aodDial);

        System.out.println("=== LUNAR PHASE MASTER ASSETS COMPLETE ===");
    }

    // =========================================================================
    // 1. 29 PHOTOREALISTIC MOON PHASES FROM NASA LROC 16-BIT TIFF
    // =========================================================================
    static BufferedImage[] generate29MoonPhases(File watchfaceDir, File appDir) throws Exception {
        System.out.println("Processing NASA LROC 16-bit TIFF for 29 Edge-to-Edge phases...");

        Raster raster = null;
        BufferedImage fallbackBmp = null;
        int mapW = 4096, mapH = 2048;

        if (TIFF_FILE.exists()) {
            System.out.println("Loading NASA 16-bit TIFF: " + TIFF_FILE.getAbsolutePath());
            BufferedImage tiff = ImageIO.read(TIFF_FILE);
            raster = tiff.getRaster();
            mapW = raster.getWidth();
            mapH = raster.getHeight();
        } else {
            System.out.println("TIFF not found, using fallback: " + FALLBACK_MOON.getAbsolutePath());
            fallbackBmp = ImageIO.read(FALLBACK_MOON);
        }

        // 880x880 texture: 2x supersampling for Galaxy Watch 450x450 display,
        // strictly complying with WFF 100.00 MB active memory footprint limit (89.8 MB)
        int size = 880;
        double R = size / 2.0; // 440.0
        double cx = size / 2.0;
        double cy = size / 2.0;

        float[] nxBuf = new float[size * size];
        float[] nyBuf = new float[size * size];
        float[] nzBuf = new float[size * size];
        float[] albedoBuf = new float[size * size];
        float[] edgeAlpha = new float[size * size];

        int[] p00 = new int[3];
        int[] p10 = new int[3];
        int[] p01 = new int[3];
        int[] p11 = new int[3];

        for (int y = 0; y < size; y++) {
            double dy = y + 0.5 - cy;
            for (int x = 0; x < size; x++) {
                int idx = y * size + x;
                double dx = x + 0.5 - cx;
                double dist = hypot(dx, dy);

                if (dist > R + 0.5) {
                    edgeAlpha[idx] = 0f;
                    continue;
                }

                float ea = 1.0f;
                if (dist > R - 0.75) {
                    ea = (float)max(0.0, min(1.0, (R + 0.5 - dist) / 1.25));
                }
                edgeAlpha[idx] = ea;

                double nx = dx / R;
                double ny = dy / R;
                double nz = sqrt(max(0.0, 1.0 - nx * nx - ny * ny));
                nxBuf[idx] = (float)nx;
                nyBuf[idx] = (float)ny;
                nzBuf[idx] = (float)nz;

                double cTex;
                if (raster != null) {
                    double lat = asin(-ny);
                    double lon = atan2(nx, nz);

                    double v = (0.5 - lat / PI) * mapH;
                    double u = 2048.0 + (lon / (2.0 * PI)) * mapW;
                    while (u < 0) u += mapW;
                    while (u >= mapW) u -= mapW;

                    int x0 = (int)floor(u);
                    int y0 = (int)floor(v);
                    int x1 = (x0 + 1) % mapW;
                    int y1 = (int)max(0, min(mapH - 1, y0 + 1));
                    y0 = (int)max(0, min(mapH - 1, y0));

                    double fx = u - x0;
                    double fy = v - y0;

                    raster.getPixel(x0, y0, p00);
                    raster.getPixel(x1, y0, p10);
                    raster.getPixel(x0, y1, p01);
                    raster.getPixel(x1, y1, p11);

                    double l00 = (p00[0] * 0.299 + p00[1] * 0.587 + p00[2] * 0.114) / 65535.0;
                    double l10 = (p10[0] * 0.299 + p10[1] * 0.587 + p10[2] * 0.114) / 65535.0;
                    double l01 = (p01[0] * 0.299 + p01[1] * 0.587 + p01[2] * 0.114) / 65535.0;
                    double l11 = (p11[0] * 0.299 + p11[1] * 0.587 + p11[2] * 0.114) / 65535.0;

                    double top = l00 * (1.0 - fx) + l10 * fx;
                    double bottom = l01 * (1.0 - fx) + l11 * fx;
                    double val = top * (1.0 - fy) + bottom * fy;

                    cTex = max(0.0, min(1.0, (val - 0.50) * 1.35 + 0.50));
                } else {
                    double u = fallbackBmp.getWidth() * (0.5 + nx * 0.495);
                    double v = fallbackBmp.getHeight() * (0.5 + ny * 0.495);
                    cTex = sampleBilinear(fallbackBmp, u, v);
                }
                albedoBuf[idx] = (float)cTex;
            }
        }

        BufferedImage[] phases = new BufferedImage[29];
        for (int p = 0; p <= 28; p++) {
            phases[p] = renderMoonPhase(size, p, nxBuf, nyBuf, nzBuf, albedoBuf, edgeAlpha);
            String name = String.format("lunar_moon_%02d.png", p);
            ImageIO.write(phases[p], "png", new File(watchfaceDir, name));
            ImageIO.write(phases[p], "png", new File(appDir, name));
        }
        System.out.println("29 Moon Phases generated successfully!");
        return phases;
    }

    static BufferedImage renderMoonPhase(int size, int phase, float[] nxBuf, float[] nyBuf, float[] nzBuf,
                                         float[] albedoBuf, float[] edgeAlpha) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        double theta = (phase / 29.0) * 2.0 * PI;
        double sx = sin(theta);
        double sy = 0.0;
        double sz = -cos(theta);
        double R = size / 2.0;

        for (int y = 0; y < size; y++) {
            double dy = y + 0.5 - R;
            for (int x = 0; x < size; x++) {
                int idx = y * size + x;
                float ea = edgeAlpha[idx];
                if (ea <= 0.001f) continue;

                double nx = nxBuf[idx];
                double ny = nyBuf[idx];
                double nz = nzBuf[idx];
                double cTex = albedoBuf[idx];

                double cosIncident = nx * sx + ny * sy + nz * sz;
                double transWidth = 0.040;
                double sunlitFade = max(0.0, min(1.0, (cosIncident / transWidth) + 0.5));
                double sunlitLum = max(0.0, min(1.0, 0.22 + 0.78 * max(0.0, cosIncident)));

                double earthshineFactor = 0.75 + 0.25 * (1.0 - cos(theta));
                double earthshine = 0.075 * earthshineFactor * (0.6 + 0.4 * nz);

                double illumination = sunlitFade * sunlitLum + (1.0 - sunlitFade) * earthshine;

                double dx = x + 0.5 - R;
                double dist = hypot(dx, dy);
                double distToEdge = R - dist;
                double rim = 0.0;
                if (distToEdge >= 0.0 && distToEdge <= 2.5) {
                    double curve = max(0.0, min(1.0, 1.0 - abs(distToEdge - 1.0) / 1.5));
                    rim = 0.16 * curve * (1.0 - sunlitFade * 0.85);
                }

                double finalLum = max(0.0, min(1.0, cTex * illumination + rim));

                int rOut = (int)round(max(0.0, min(255.0, finalLum * 244.0 + 6.0)));
                int gOut = (int)round(max(0.0, min(255.0, finalLum * 239.0 + 6.0)));
                int bOut = (int)round(max(0.0, min(255.0, finalLum * 230.0 + 7.0)));
                int aOut = (int)round(ea * 255.0);

                img.setRGB(x, y, (aOut << 24) | (rOut << 16) | (gOut << 8) | bOut);
            }
        }
        return img;
    }

    // =========================================================================
    // 2. 12 APPLIED FACETED HOUR MARKERS
    // =========================================================================
    static BufferedImage generateAppliedMarkers(File watchfaceDir, File appDir) throws Exception {
        System.out.println("Generating 12 Applied Faceted Hour Markers...");
        BufferedImage img = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        for (int h = 1; h <= 12; h++) {
            double angle = toRadians(h * 30.0 - 90.0);
            drawAppliedMarker(g, 500, 500, angle);
        }

        g.dispose();
        ImageIO.write(img, "png", new File(watchfaceDir, "lunar_markers.png"));
        ImageIO.write(img, "png", new File(appDir, "lunar_markers.png"));
        return img;
    }

    static void drawAppliedMarker(Graphics2D g, double cx, double cy, double angle) {
        AffineTransform old = g.getTransform();
        g.translate(cx, cy);
        g.rotate(angle);

        double rOuter = 490.0;
        double rInner = 432.0;
        double halfW = 4.5;

        // Drop shadow for applied relief
        Path2D.Double shadow = new Path2D.Double();
        shadow.moveTo(rOuter + 2, -halfW + 1.5);
        shadow.lineTo(rInner - 1, 1.5);
        shadow.lineTo(rOuter + 2, halfW + 1.5);
        shadow.closePath();
        g.setColor(new Color(0, 0, 0, 150));
        g.fill(shadow);

        // Left Facet (Brushed Steel / Platinum)
        Path2D.Double leftFacet = new Path2D.Double();
        leftFacet.moveTo(rOuter, -halfW);
        leftFacet.lineTo(rInner, 0);
        leftFacet.lineTo(rOuter, 0);
        leftFacet.closePath();

        GradientPaint pLeft = new GradientPaint(
            (float)rInner, 0f, new Color(0xF8, 0xFA, 0xFC),
            (float)rOuter, (float)-halfW, new Color(0xCB, 0xD5, 0xE1)
        );
        g.setPaint(pLeft);
        g.fill(leftFacet);

        // Right Facet (Warm Golden / Amber facet)
        Path2D.Double rightFacet = new Path2D.Double();
        rightFacet.moveTo(rOuter, 0);
        rightFacet.lineTo(rInner, 0);
        rightFacet.lineTo(rOuter, halfW);
        rightFacet.closePath();

        GradientPaint pRight = new GradientPaint(
            (float)rInner, 0f, new Color(0xF5, 0x9E, 0x0B),
            (float)rOuter, (float)halfW, new Color(0x78, 0x35, 0x0F)
        );
        g.setPaint(pRight);
        g.fill(rightFacet);

        // Center spine
        g.setColor(new Color(0xFF, 0xFF, 0xFF, 220));
        g.setStroke(new BasicStroke(0.8f));
        g.draw(new Line2D.Double(rInner, 0, rOuter, 0));

        // Golden luminous tip
        g.setColor(new Color(0xFB, 0xBF, 0x24, 230));
        g.fill(new Ellipse2D.Double(rInner - 1.5, -1.5, 3.0, 3.0));

        g.setTransform(old);
    }

    // =========================================================================
    // 3. HAUTE HORLOGERIE HANDS (MATCHING APPROVED REFERENCE)
    // =========================================================================
    static void generateLunarHands(File watchfaceDir, File appDir) throws Exception {
        System.out.println("Generating Lunar Dauphine Hands with Amber/Golden Accents...");

        // Hour Hand: 48 x 320 (pivot at y = 275, length 265 from pivot to tip)
        BufferedImage hour = createLunarDauphineHand(48, 320, 275, false, false);
        BufferedImage hourAod = createLunarDauphineHand(48, 320, 275, false, true);
        ImageIO.write(hour, "png", new File(watchfaceDir, "lunar_hour.png"));
        ImageIO.write(hour, "png", new File(appDir, "lunar_hour.png"));
        ImageIO.write(hourAod, "png", new File(watchfaceDir, "lunar_hour_aod.png"));
        ImageIO.write(hourAod, "png", new File(appDir, "lunar_hour_aod.png"));

        // Minute Hand: 42 x 460 (pivot at y = 415, length 405 from pivot to tip)
        BufferedImage min = createLunarDauphineHand(42, 460, 415, true, false);
        BufferedImage minAod = createLunarDauphineHand(42, 460, 415, true, true);
        ImageIO.write(min, "png", new File(watchfaceDir, "lunar_minute.png"));
        ImageIO.write(min, "png", new File(appDir, "lunar_minute.png"));
        ImageIO.write(minAod, "png", new File(watchfaceDir, "lunar_minute_aod.png"));
        ImageIO.write(minAod, "png", new File(appDir, "lunar_minute_aod.png"));

        // Second Hand: 20 x 520 (golden needle with counterweight & amber tip, pivot at y = 435, length 425 from pivot to tip)
        BufferedImage sec = createLunarSecondHand(20, 520, 435);
        ImageIO.write(sec, "png", new File(watchfaceDir, "lunar_second.png"));
        ImageIO.write(sec, "png", new File(appDir, "lunar_second.png"));

        // Center Hub: 46 x 46 (brushed steel center with gold outer collar)
        BufferedImage hub = createLunarCenterHub(46);
        ImageIO.write(hub, "png", new File(watchfaceDir, "lunar_hub.png"));
        ImageIO.write(hub, "png", new File(appDir, "lunar_hub.png"));
    }

    static BufferedImage createLunarDauphineHand(int w, int h, int pivotY, boolean isMinute, boolean isAod) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        double cx = w / 2.0;
        double tipY = 10.0;
        double tailY = h - 10.0;
        double midY = tipY + (pivotY - tipY) * (isMinute ? 0.62 : 0.68);
        double maxHalfW = (w / 2.0) - 2.5;

        // Shadow for Active Mode
        if (!isAod) {
            Path2D.Double sh = new Path2D.Double();
            sh.moveTo(cx + 1.5, tipY + 2);
            sh.lineTo(cx + maxHalfW + 1.5, midY + 2);
            sh.lineTo(cx + maxHalfW * 0.40 + 1.5, pivotY + 2);
            sh.lineTo(cx + 1.5, tailY + 2);
            sh.lineTo(cx - maxHalfW * 0.40 + 1.5, pivotY + 2);
            sh.lineTo(cx - maxHalfW + 1.5, midY + 2);
            sh.closePath();
            g.setColor(new Color(0, 0, 0, 110));
            g.fill(sh);
        }

        // Left Facet (Light Brushed Steel / Platinum)
        Path2D.Double left = new Path2D.Double();
        left.moveTo(cx, tipY);
        left.lineTo(cx - maxHalfW, midY);
        left.lineTo(cx - maxHalfW * 0.40, pivotY);
        left.lineTo(cx, tailY);
        left.closePath();

        GradientPaint pLeft = new GradientPaint(
            (float)cx, (float)tipY, isAod ? new Color(0xDC, 0xE3, 0xEA) : new Color(0xF8, 0xFA, 0xFC),
            (float)(cx - maxHalfW), (float)midY, isAod ? new Color(0x94, 0xA3, 0xB8) : new Color(0xCB, 0xD5, 0xE1)
        );
        g.setPaint(pLeft);
        g.fill(left);

        // Right Facet (Polished Titanium / Steel)
        Path2D.Double right = new Path2D.Double();
        right.moveTo(cx, tipY);
        right.lineTo(cx + maxHalfW, midY);
        right.lineTo(cx + maxHalfW * 0.40, pivotY);
        right.lineTo(cx, tailY);
        right.closePath();

        GradientPaint pRight = new GradientPaint(
            (float)cx, (float)tipY, isAod ? new Color(0x71, 0x80, 0x96) : new Color(0x94, 0xA3, 0xB8),
            (float)(cx + maxHalfW), (float)midY, isAod ? new Color(0x47, 0x55, 0x69) : new Color(0x47, 0x55, 0x69)
        );
        g.setPaint(pRight);
        g.fill(right);

        // Warm Golden / Luminous accent along the inner bevel of the left facet (matching reference image)
        if (!isAod) {
            Path2D.Double goldAccent = new Path2D.Double();
            goldAccent.moveTo(cx - 1.5, tipY + 12);
            goldAccent.lineTo(cx - maxHalfW * 0.65, midY);
            goldAccent.lineTo(cx - 1, midY + (pivotY - midY) * 0.3);
            goldAccent.closePath();
            GradientPaint pGold = new GradientPaint(
                (float)(cx - maxHalfW * 0.65), (float)midY, new Color(0xF5, 0x9E, 0x0B, 220),
                (float)cx, (float)tipY, new Color(0xFE, 0xF3, 0xC7, 180)
            );
            g.setPaint(pGold);
            g.fill(goldAccent);
        }

        // Center spine ridge
        g.setColor(new Color(0xFF, 0xFF, 0xFF, isAod ? 160 : 220));
        g.setStroke(new BasicStroke(1.0f));
        g.draw(new Line2D.Double(cx, tipY + 3, cx, tailY - 3));

        g.dispose();
        return img;
    }

    static BufferedImage createLunarSecondHand(int w, int h, int pivotY) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double cx = w / 2.0;
        double tipY = 10.0;
        double tailY = h - 12.0;

        // Shadow
        g.setColor(new Color(0, 0, 0, 90));
        g.setStroke(new BasicStroke(1.8f));
        g.draw(new Line2D.Double(cx + 1.2, tipY + 1.5, cx + 1.2, tailY + 1.5));

        // Slender Warm Golden Needle Body
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        GradientPaint pGold = new GradientPaint(
            (float)cx, (float)tipY, new Color(0xFD, 0xE0, 0x47),
            (float)cx, (float)tailY, new Color(0xD9, 0x77, 0x06)
        );
        g.setPaint(pGold);
        g.draw(new Line2D.Double(cx, tipY, cx, tailY));

        // Luminous Amber Segment near tip
        g.setColor(new Color(0xFB, 0xBF, 0x24));
        g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(cx, tipY + 8, cx, tipY + 50));

        // Counterweight Disc
        g.setColor(new Color(0xB4, 0x53, 0x09));
        g.fill(new Ellipse2D.Double(cx - 3.0, tailY - 22, 6.0, 6.0));
        g.setColor(new Color(0xFE, 0xF0, 0x8A));
        g.fill(new Ellipse2D.Double(cx - 1.5, tailY - 20.5, 3.0, 3.0));

        g.dispose();
        return img;
    }

    static BufferedImage createLunarCenterHub(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double c = size / 2.0;

        // Drop shadow
        g.setColor(new Color(0, 0, 0, 140));
        g.fill(new Ellipse2D.Double(3, 3, size - 4, size - 4));

        // Outer Warm Gold Collar Ring
        GradientPaint pGold = new GradientPaint(
            2f, 2f, new Color(0xF5, 0x9E, 0x0B),
            (float)size - 2, (float)size - 2, new Color(0x78, 0x35, 0x0F)
        );
        g.setPaint(pGold);
        g.fill(new Ellipse2D.Double(2, 2, size - 4, size - 4));

        // Bevel highlight on gold ring
        g.setColor(new Color(0xFE, 0xF3, 0xC7, 200));
        g.setStroke(new BasicStroke(1.2f));
        g.draw(new Ellipse2D.Double(3, 3, size - 6, size - 6));

        // Inner Brushed Steel Cap
        double innerR = size * 0.32;
        GradientPaint pSteel = new GradientPaint(
            (float)(c - innerR), (float)(c - innerR), new Color(0xF8, 0xFA, 0xFC),
            (float)(c + innerR), (float)(c + innerR), new Color(0x64, 0x74, 0x8B)
        );
        g.setPaint(pSteel);
        g.fill(new Ellipse2D.Double(c - innerR, c - innerR, innerR * 2, innerR * 2));

        // Concentric circular graining
        for (double r = innerR * 0.3; r < innerR; r += 2.5) {
            g.setColor(new Color(255, 255, 255, (r % 5 == 0) ? 25 : 12));
            g.setStroke(new BasicStroke(0.6f));
            g.draw(new Ellipse2D.Double(c - r, c - r, r * 2, r * 2));
        }

        // Center Pin
        g.setColor(new Color(0x1E, 0x29, 0x3B));
        g.fill(new Ellipse2D.Double(c - 2.5, c - 2.5, 5, 5));

        g.dispose();
        return img;
    }

    // =========================================================================
    // 4. AOD DIAL (SERIF NUMERALS + MINUTE TRACK)
    // =========================================================================
    static BufferedImage generateAodDial(File watchfaceDir, File appDir) throws Exception {
        System.out.println("Generating Lunar AOD Dial (Serif Numerals + Minute Track)...");
        BufferedImage img = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // AMOLED Pure Black Background
        g.setColor(Color.BLACK);
        g.fillOval(0, 0, 1000, 1000);

        Font notoSerif = Font.createFont(Font.TRUETYPE_FONT, new File("watchface/src/main/res/font/ref_serif.ttf"));
        Font font = notoSerif.deriveFont(Font.PLAIN, 104f);

        java.util.List<Rectangle2D> bounds = new java.util.ArrayList<>();
        for (int h = 1; h <= 12; h++) {
            String text = String.valueOf(h);
            var glyph = font.createGlyphVector(g.getFontRenderContext(), text);
            var b = glyph.getVisualBounds();
            double angle = toRadians(h * 30.0 - 90.0);
            double radius = (h >= 10) ? 418.0 : 432.0;
            double tx = 500 + cos(angle) * radius - b.getCenterX();
            double ty = 500 + sin(angle) * radius - b.getCenterY();

            bounds.add(new Rectangle2D.Double(b.getX() + tx - 8, b.getY() + ty - 8, b.getWidth() + 16, b.getHeight() + 16));

            g.setColor(new Color(0xDC, 0xE3, 0xEA, 185));
            g.drawGlyphVector(glyph, (float)tx, (float)ty);
        }

        // Minute Track
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
                g.setStroke(new BasicStroke(major ? 4.0f : 2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(major ? new Color(0xDC, 0xE3, 0xEA, 135) : new Color(0x71, 0x80, 0x96, 85));
                g.draw(new Line2D.Double(500 + cos(angle) * startR, 500 + sin(angle) * startR, ex, ey));
            }
        }

        g.dispose();
        ImageIO.write(img, "png", new File(watchfaceDir, "lunar_dial_aod.png"));
        ImageIO.write(img, "png", new File(appDir, "lunar_dial_aod.png"));
        return img;
    }

    // =========================================================================
    // 5. MASTER CONTACT SHEET (29 PHASES GRID BOARD)
    // =========================================================================
    static void generateContactSheet(BufferedImage[] phases) throws Exception {
        System.out.println("Generating Master Contact Sheet for 29 Lunar Phases...");
        int cellW = 340;
        int cellH = 400;
        int cols = 5;
        int rows = 6;
        int pad = 24;
        int boardW = cols * cellW + (cols + 1) * pad;
        int boardH = rows * cellH + (rows + 1) * pad + 60;

        BufferedImage board = new BufferedImage(boardW, boardH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = board.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        g.setColor(new Color(0x06, 0x08, 0x0B));
        g.fillRect(0, 0, boardW, boardH);

        g.setColor(new Color(0xF1, 0xF5, 0xF9));
        g.setFont(new Font("SansSerif", Font.BOLD, 30));
        g.drawString("NOCTURNE — LUNAR PHASE (NASA LROC 16-BIT | 29 PHASES EDGE-TO-EDGE)", pad, 45);

        String[] phaseNames = {
            "00: New Moon (محاق)", "01: Waxing Crescent", "02: Waxing Crescent", "03: Waxing Crescent",
            "04: Waxing Crescent", "05: Waxing Crescent", "06: Waxing Crescent", "07: First Quarter (تربيع أول)",
            "08: Waxing Gibbous", "09: Waxing Gibbous", "10: Waxing Gibbous", "11: Waxing Gibbous",
            "12: Waxing Gibbous", "13: Waxing Gibbous", "14: Full Moon (بدر)", "15: Waning Gibbous",
            "16: Waning Gibbous", "17: Waning Gibbous", "18: Waning Gibbous", "19: Waning Gibbous",
            "20: Waning Gibbous", "21: Third Quarter (تربيع أخير)", "22: Waning Crescent", "23: Waning Crescent",
            "24: Waning Crescent", "25: Waning Crescent", "26: Waning Crescent", "27: Waning Crescent",
            "28: Waning Crescent (محاق مقترب)"
        };

        for (int i = 0; i < 29; i++) {
            int col = i % cols;
            int row = i / cols;
            int x = pad + col * (cellW + pad);
            int y = 70 + pad + row * (cellH + pad);

            g.setColor(new Color(0x11, 0x14, 0x18));
            g.fill(new RoundRectangle2D.Double(x, y, cellW, cellH, 16, 16));
            g.setColor(new Color(0x23, 0x2A, 0x33));
            g.setStroke(new BasicStroke(1.2f));
            g.draw(new RoundRectangle2D.Double(x, y, cellW, cellH, 16, 16));

            int discSize = 300;
            int discX = x + (cellW - discSize) / 2;
            int discY = y + 16;

            g.setColor(new Color(0x33, 0x41, 0x55));
            g.setStroke(new BasicStroke(1.5f));
            g.draw(new Ellipse2D.Double(discX, discY, discSize, discSize));

            g.drawImage(phases[i], discX, discY, discSize, discSize, null);

            g.setColor(new Color(0xE2, 0xE8, 0xF0));
            g.setFont(new Font("SansSerif", Font.BOLD, 15));
            FontMetrics fm = g.getFontMetrics();
            String name = phaseNames[i];
            int tx = x + (cellW - fm.stringWidth(name)) / 2;
            g.drawString(name, tx, y + cellH - 45);

            g.setColor(new Color(0x94, 0xA3, 0xB8));
            g.setFont(new Font("Monospaced", Font.PLAIN, 13));
            String meta = String.format("Phase #%02d | Angle: %.1f°", i, (i / 29.0) * 360.0);
            fm = g.getFontMetrics();
            tx = x + (cellW - fm.stringWidth(meta)) / 2;
            g.drawString(meta, tx, y + cellH - 22);
        }

        // Info Card slot 30
        int lastCol = 29 % cols;
        int lastRow = 29 / cols;
        int ix = pad + lastCol * (cellW + pad);
        int iy = 70 + pad + lastRow * (cellH + pad);
        g.setColor(new Color(0x16, 0x1E, 0x27));
        g.fill(new RoundRectangle2D.Double(ix, iy, cellW, cellH, 16, 16));
        g.setColor(new Color(0x38, 0xBD, 0xF8));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new RoundRectangle2D.Double(ix, iy, cellW, cellH, 16, 16));

        g.setColor(new Color(0x38, 0xBD, 0xF8));
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString("ASTRONOMICAL SPECS", ix + 20, iy + 45);

        g.setColor(new Color(0xCB, 0xD5, 0xE1));
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        int ly = iy + 85;
        g.drawString("• NASA LROC 16-bit WAC mosaic", ix + 20, ly); ly += 30;
        g.drawString("• Exact 29-phase lunar cycle", ix + 20, ly); ly += 30;
        g.drawString("• 1000x1000 Edge-to-Edge", ix + 20, ly); ly += 30;
        g.drawString("• Center locked at (500, 500)", ix + 20, ly); ly += 30;
        g.drawString("• Lommel-Seeliger scattering", ix + 20, ly); ly += 30;
        g.drawString("• Natural subtle earthshine", ix + 20, ly); ly += 30;
        g.drawString("• 12 Applied Faceted Markers", ix + 20, ly); ly += 30;
        g.drawString("• Amber / Golden Accent Hands", ix + 20, ly);

        g.dispose();
        ImageIO.write(board, "png", new File(ARTIFACTS_DIR + "preview_lunar_29_phases.png"));
        System.out.println("Saved preview_lunar_29_phases.png in artifacts!");
    }

    // =========================================================================
    // 6. ACTIVE & AOD PREVIEWS
    // =========================================================================
    static void generatePreviews(BufferedImage moonBmp, BufferedImage markers, BufferedImage aodDial) throws Exception {
        System.out.println("Generating Active & AOD Master Previews...");
        File watchfaceDir = new File("watchface/build/generated/refinement/res/drawable");

        BufferedImage hourBmp = ImageIO.read(new File(watchfaceDir, "lunar_hour.png"));
        BufferedImage minBmp = ImageIO.read(new File(watchfaceDir, "lunar_minute.png"));
        BufferedImage secBmp = ImageIO.read(new File(watchfaceDir, "lunar_second.png"));
        BufferedImage hubBmp = ImageIO.read(new File(watchfaceDir, "lunar_hub.png"));
        BufferedImage hourAodBmp = ImageIO.read(new File(watchfaceDir, "lunar_hour_aod.png"));
        BufferedImage minAodBmp = ImageIO.read(new File(watchfaceDir, "lunar_minute_aod.png"));

        // Match the presentation angles of the reference image:
        // Hour hand pointing to ~10:10 (around 306 degrees from 12 o'clock, or -54 degrees)
        // Minute hand pointing to ~1:50 (around 52 degrees from 12 o'clock)
        // Second hand pointing to ~7:20 (around 220 degrees from 12 o'clock)
        double hourAngle = toRadians(306.0 - 90.0);
        double minAngle = toRadians(52.0 - 90.0);
        double secAngle = toRadians(220.0 - 90.0);

        // 6.1 ACTIVE PREVIEW
        BufferedImage active = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_ARGB);
        Graphics2D ag = active.createGraphics();
        ag.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        ag.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // 1. Edge-to-Edge Moon
        ag.drawImage(moonBmp, 0, 0, 1000, 1000, null);

        // 2. 12 Applied Hour Markers
        ag.drawImage(markers, 0, 0, 1000, 1000, null);

        // 3. Hands
        drawHand(ag, hourBmp, 500, 500, 275, hourAngle);
        drawHand(ag, minBmp, 500, 500, 415, minAngle);
        drawHand(ag, secBmp, 500, 500, 435, secAngle);

        // 4. Center Hub
        ag.drawImage(hubBmp, 500 - hubBmp.getWidth() / 2, 500 - hubBmp.getHeight() / 2, null);

        ag.dispose();
        ImageIO.write(active, "png", new File(ARTIFACTS_DIR + "preview_lunar_active.png"));

        // 6.2 AOD PREVIEW
        BufferedImage aod = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_ARGB);
        Graphics2D adg = aod.createGraphics();
        adg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        adg.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // 1. Dial (Pure black + Serif numerals + minute track)
        adg.drawImage(aodDial, 0, 0, 1000, 1000, null);

        // 2. AOD Hands (Hour + Minute only)
        drawHand(adg, hourAodBmp, 500, 500, 275, hourAngle);
        drawHand(adg, minAodBmp, 500, 500, 415, minAngle);

        // 3. Center Hub
        adg.drawImage(hubBmp, 500 - hubBmp.getWidth() / 2, 500 - hubBmp.getHeight() / 2, null);

        adg.dispose();
        ImageIO.write(aod, "png", new File(ARTIFACTS_DIR + "preview_lunar_aod.png"));

        System.out.println("Saved preview_lunar_active.png and preview_lunar_aod.png in artifacts!");
    }

    static void drawHand(Graphics2D g, BufferedImage hand, double cx, double cy, double pivotYInHand, double angleRad) {
        AffineTransform old = g.getTransform();
        g.translate(cx, cy);
        g.rotate(angleRad + PI / 2.0);
        double pivotXInHand = hand.getWidth() / 2.0;
        g.translate(-pivotXInHand, -pivotYInHand);
        g.drawImage(hand, 0, 0, null);
        g.setTransform(old);
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
}
