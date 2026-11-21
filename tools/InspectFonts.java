package tools;

import java.awt.Font;
import java.io.File;

public class InspectFonts {
    public static void main(String[] args) throws Exception {
        Font f1 = Font.createFont(Font.TRUETYPE_FONT, new File("watchface/src/main/res/font/ref_serif.ttf"));
        System.out.println("ref_serif: " + f1.getFontName() + ", family: " + f1.getFamily());
        Font f2 = Font.createFont(Font.TRUETYPE_FONT, new File("tools/CormorantGaramond-SemiBold.ttf"));
        System.out.println("cormorant: " + f2.getFontName() + ", family: " + f2.getFamily());
        Font f3 = Font.createFont(Font.TRUETYPE_FONT, new File("watchface/src/main/res/font/functional_medium.ttf"));
        System.out.println("functional: " + f3.getFontName() + ", family: " + f3.getFamily());
    }
}
