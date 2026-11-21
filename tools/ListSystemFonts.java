package tools;

import java.awt.GraphicsEnvironment;

public class ListSystemFonts {
    public static void main(String[] args) {
        for (String f : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            if (f.toLowerCase().contains("serif") || f.toLowerCase().contains("georgia") ||
                f.toLowerCase().contains("garamond") || f.toLowerCase().contains("baskerville") ||
                f.toLowerCase().contains("cambria") || f.toLowerCase().contains("palatino") ||
                f.toLowerCase().contains("bodoni") || f.toLowerCase().contains("didot") ||
                f.toLowerCase().contains("century") || f.toLowerCase().contains("times")) {
                System.out.println(f);
            }
        }
    }
}
