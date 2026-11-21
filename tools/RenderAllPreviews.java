package tools;

import javax.imageio.ImageIO;
import java.io.File;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import static java.lang.Math.*;

public class RenderAllPreviews {

    static final String ARTIFACTS_DIR = "C:/Users/mohamed zayed/.gemini/antigravity-acp/brain/2d4c81b1-82e2-4031-b244-001e9e90dc8f/";
    static final File DRAWABLES = new File("watchface/src/main/res/drawable");
    static final File GEN_DRAWABLES = new File("watchface/build/generated/refinement/res/drawable");

    static BufferedImage load(String name) throws Exception {
        File f = new File(GEN_DRAWABLES, name + ".png");
        if (!f.exists()) f = new File(DRAWABLES, name + ".png");
        if (!f.exists()) f = new File("app/src/main/res/drawable", name + ".png");
        if (!f.exists()) throw new RuntimeException("Drawable not found: " + name);
        return ImageIO.read(f);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Rendering 6 Real Master Previews...");

        renderSageActive();
        renderSageAod();
        renderLunarActive();
        renderLunarAod();
        renderObsidianActive();
        renderObsidianAod();

        System.out.println("All 6 Previews Rendered Successfully!");
    }

    // -------------------------------------------------------------
    // 1. SAGE NOCTURNE ACTIVE (100% Matching Image 1 Authentic Screenshot)
    // -------------------------------------------------------------
    static void renderSageActive() throws Exception {
        BufferedImage img = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        setupHints(g);

        // Circular dial mask
        g.setClip(new Ellipse2D.Double(0, 0, 1000, 1000));
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 1000, 1000);

        // 1. Rich Ottoman Foliage on Left Half (with rosette medallion & battery cutout)
        BufferedImage orn = load("visible_0_orn_1");
        g.drawImage(orn, 0, 0, 1000, 1000, null);

        // 2. Refined Noto Serif Dial Numerals (Soft, upright, elegant, non-italic, medium weight)
        BufferedImage dial = load("refined_0_dial_0");
        g.drawImage(dial, 0, 0, 1000, 1000, null);

        // 3. Subdial Rings
        BufferedImage ring = load("uc_0_ring");
        g.drawImage(ring, 171, 391, 228, 228, null);
        g.drawImage(ring, 601, 391, 228, 228, null);

        // 4. Complications Content matching Image 1:
        // Left Subdial: Battery Icon (top) + 45% (bottom). NO "BATTERY" text!
        BufferedImage bat = load("refined_battery");
        g.drawImage(bat, 285 - 29, 425, 58, 40, null);

        // Right Subdial: Calendar Icon (top) + Nov + 21
        BufferedImage cal = load("sage_calendar");
        g.drawImage(cal, 715 - 25, 422, 50, 46, null);

        // Bottom: Green Running Shoe Icon (top) + 0 (bottom). NO "STEPS" text, NO arcs!
        BufferedImage steps = load("sage_steps");
        g.drawImage(steps, 500 - 24, 622, 48, 36, null);

        Font functional = Font.createFont(Font.TRUETYPE_FONT, new File("watchface/src/main/res/font/functional_medium.ttf"));
        g.setColor(new Color(0xFF, 0xF0, 0xD8));

        // Battery percentage "45%"
        g.setFont(functional.deriveFont(Font.BOLD, 52f));
        drawCentered(g, "45%", 285, 532);

        // Calendar "Nov" and "21"
        g.setFont(functional.deriveFont(Font.BOLD, 40f));
        drawCentered(g, "Nov", 715, 502);
        g.setFont(functional.deriveFont(Font.BOLD, 50f));
        drawCentered(g, "21", 715, 555);

        // Step count "0"
        g.setFont(functional.deriveFont(Font.BOLD, 56f));
        drawCentered(g, "0", 500, 725);

        // 5. Arabic Dhikr Calligraphy: "الْحَمْدُ لِلَّهِ" at (300, 335, 400x80)
        BufferedImage dhikr = load("uc_dhikr_0_precision");
        g.drawImage(dhikr, 300, 335, 400, 80, null);

        // 6. Realistic Moon Phase above Dhikr (at 392, 122, 216x216)
        BufferedImage moon = load("refined_moon_00");
        g.drawImage(moon, 392, 122, 216, 216, null);

        // 7. Authentic Skeleton Hands (Hour: 10:08, Second: ~42s)
        // Hour hand: 10:09 -> 304.5 degrees
        // Minute hand: 09m -> 54 degrees
        // Second hand: 42s -> 252 degrees
        drawHand(g, load("refined_0_hour"), 304.5, 480, 250, 40, 280, 500, 500, 20, 250);
        drawHand(g, load("refined_0_minute"), 54.0, 483, 150, 34, 380, 500, 500, 17, 350);
        drawHand(g, load("uc_0_second"), 252.0, 492, 140, 16, 395, 500, 500, 8, 360);

        // Center Hub
        BufferedImage hub = load("uc_0_hub");
        g.drawImage(hub, 477, 477, 46, 46, null);

        g.dispose();
        ImageIO.write(img, "png", new File(ARTIFACTS_DIR + "preview_sage_active.png"));
    }

    // -------------------------------------------------------------
    // 2. SAGE NOCTURNE AOD
    // -------------------------------------------------------------
    static void renderSageAod() throws Exception {
        BufferedImage img = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        setupHints(g);

        g.setClip(new Ellipse2D.Double(0, 0, 1000, 1000));
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 1000, 1000);

        // AOD Dial (soft numerals and minute markers)
        BufferedImage dialAod = load("refined_0_dial_0_aod");
        g.drawImage(dialAod, 0, 0, 1000, 1000, null);

        // AOD Dhikr (subtle)
        BufferedImage dhikr = load("uc_dhikr_0_precision");
        Composite oldComp = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.65f));
        g.drawImage(dhikr, 300, 335, 400, 80, null);
        g.setComposite(oldComp);

        // AOD Skeleton Hands (Luminous contour)
        drawHand(g, load("refined_0_hour_aod"), 304.5, 480, 250, 40, 280, 500, 500, 20, 250);
        drawHand(g, load("refined_0_minute_aod"), 54.0, 483, 150, 34, 380, 500, 500, 17, 350);

        // Center Hub
        BufferedImage hub = load("uc_0_hub");
        g.drawImage(hub, 477, 477, 46, 46, null);

        g.dispose();
        ImageIO.write(img, "png", new File(ARTIFACTS_DIR + "preview_sage_aod.png"));
    }

    // -------------------------------------------------------------
    // 3. LUNAR PHASE ACTIVE (Photorealistic 92% diameter, Dual-Contrast Hands, NO numbers)
    // -------------------------------------------------------------
    static void renderLunarActive() throws Exception {
        BufferedImage img = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        setupHints(g);

        g.setClip(new Ellipse2D.Double(0, 0, 1000, 1000));
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 1000, 1000);

        // 1. Massive 920px (92%) Photorealistic Moon (Full Moon or Gibbous phase, e.g. phase 14)
        BufferedImage moon = load("lunar_moon_14");
        g.drawImage(moon, 40, 40, 920, 920, null);

        // 2. Dual-Contrast Hands (Luminous center, dark beveled border for high contrast)
        drawHand(g, load("lunar_hour"), 304.5, 480, 250, 40, 280, 500, 500, 20, 250);
        drawHand(g, load("lunar_minute"), 54.0, 483, 150, 34, 380, 500, 500, 17, 350);
        drawHand(g, load("lunar_second"), 252.0, 492, 140, 16, 395, 500, 500, 8, 360);

        // Center Hub
        BufferedImage hub = load("lunar_hub");
        g.drawImage(hub, 483, 483, 34, 34, null);

        g.dispose();
        ImageIO.write(img, "png", new File(ARTIFACTS_DIR + "preview_lunar_active.png"));
    }

    // -------------------------------------------------------------
    // 4. LUNAR PHASE AOD (Pure Black, Moon Hidden, Soft Serif Numerals)
    // -------------------------------------------------------------
    static void renderLunarAod() throws Exception {
        BufferedImage img = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        setupHints(g);

        g.setClip(new Ellipse2D.Double(0, 0, 1000, 1000));
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 1000, 1000);

        // Moon is completely hidden!
        // AOD Dial with soft Noto Serif numerals and minute markers
        BufferedImage dial = load("lunar_dial_aod");
        g.drawImage(dial, 0, 0, 1000, 1000, null);

        // AOD Hands
        drawHand(g, load("lunar_hour_aod"), 304.5, 480, 250, 40, 280, 500, 500, 20, 250);
        drawHand(g, load("lunar_minute_aod"), 54.0, 483, 150, 34, 380, 500, 500, 17, 350);

        // Hub
        BufferedImage hub = load("lunar_hub");
        g.drawImage(hub, 483, 483, 34, 34, null);

        g.dispose();
        ImageIO.write(img, "png", new File(ARTIFACTS_DIR + "preview_lunar_aod.png"));
    }

    // -------------------------------------------------------------
    // 5. OBSIDIAN ATELIER ACTIVE (Brushed Platinum, Soft Titanium, Smoked Graphite, ZERO GOLD, NO BRANDING)
    // -------------------------------------------------------------
    static void renderObsidianActive() throws Exception {
        BufferedImage img = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        setupHints(g);

        g.setClip(new Ellipse2D.Double(0, 0, 1000, 1000));

        // 1. Dial with Sunburst Brush, Applied Markers, Sunken Small Sec, Power Reserve, Date Frame (NO LOGO)
        BufferedImage dial = load("obsidian_dial");
        g.drawImage(dial, 0, 0, 1000, 1000, null);

        // 2. Power Reserve Needle at 9 o'clock (center: 215, 475)
        // Battery at 72% -> (72 - 50) * 1.2 = +26.4 degrees
        AffineTransform old = g.getTransform();
        g.translate(215, 475);
        g.rotate(toRadians(26.4));
        BufferedImage prHand = load("obsidian_power_hand");
        // prHand size: 16x125, pivot at x=8, y=100
        g.drawImage(prHand, -8, -100, 16, 125, null);
        g.setTransform(old);

        // 3. Small Seconds Needle at 6 o'clock (center: 500, 715)
        // Second at 42s -> 42 * 6 = 252 degrees
        g.translate(500, 715);
        g.rotate(toRadians(252.0));
        BufferedImage ssHand = load("obsidian_small_sec");
        // ssHand size: 16x160, pivot at x=8, y=120
        g.drawImage(ssHand, -8, -120, 16, 160, null);
        g.setTransform(old);

        // 4. Center 3D Faceted Dauphine Hands (Hour: 10:09 -> 304.5 deg, Minute: 09m -> 54 deg)
        // NO CENTER SECOND HAND!
        BufferedImage hourHand = load("obsidian_hour");
        drawHand(g, hourHand, 304.5, 476, 214, 48, 320, 500, 500, 24, 286);

        BufferedImage minHand = load("obsidian_minute");
        drawHand(g, minHand, 54.0, 480, 95, 40, 440, 500, 500, 20, 405);

        // 5. Central Multi-Layered Platinum Hub
        BufferedImage hub = load("obsidian_hub");
        g.drawImage(hub, 470, 470, 60, 60, null);

        g.dispose();
        ImageIO.write(img, "png", new File(ARTIFACTS_DIR + "preview_obsidian_active.png"));
    }

    // -------------------------------------------------------------
    // 6. OBSIDIAN ATELIER AOD
    // -------------------------------------------------------------
    static void renderObsidianAod() throws Exception {
        BufferedImage img = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        setupHints(g);

        g.setClip(new Ellipse2D.Double(0, 0, 1000, 1000));
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 1000, 1000);

        // AOD Dial (Pure AMOLED black, applied platinum markers, recessed date frame)
        BufferedImage dialAod = load("obsidian_dial_aod");
        g.drawImage(dialAod, 0, 0, 1000, 1000, null);

        // AOD Contour Dauphine Hands
        BufferedImage hourHand = load("obsidian_hour_aod");
        drawHand(g, hourHand, 304.5, 476, 214, 48, 320, 500, 500, 24, 286);

        BufferedImage minHand = load("obsidian_minute_aod");
        drawHand(g, minHand, 54.0, 480, 95, 40, 440, 500, 500, 20, 405);

        // Central Platinum Hub
        BufferedImage hub = load("obsidian_hub");
        g.drawImage(hub, 470, 470, 60, 60, null);

        g.dispose();
        ImageIO.write(img, "png", new File(ARTIFACTS_DIR + "preview_obsidian_aod.png"));
    }

    // Helper functions
    static void setupHints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    static void drawCentered(Graphics2D g, String text, int x, int y) {
        FontMetrics fm = g.getFontMetrics();
        int tx = x - fm.stringWidth(text) / 2;
        g.drawString(text, tx, y);
    }

    static void drawHand(Graphics2D g, BufferedImage hand, double angleDeg,
                         int drawX, int drawY, int drawW, int drawH,
                         int pivotScreenX, int pivotScreenY, int pivotImgX, int pivotImgY) {
        AffineTransform old = g.getTransform();
        g.translate(pivotScreenX, pivotScreenY);
        g.rotate(toRadians(angleDeg));
        g.drawImage(hand, -pivotImgX, -pivotImgY, drawW, drawH, null);
        g.setTransform(old);
    }
}
