package ca.phon.util.icons;

import jiconfont.DefaultIconCode;
import jiconfont.IconCode;
import jiconfont.IconFont;
import jiconfont.swing.IconFontSwing;

import javax.swing.*;
import java.awt.*;
import java.awt.font.LineMetrics;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.AttributedCharacterIterator;
import java.util.HashMap;
import java.util.Map;

public class GoogleMaterialIconFont implements IconFont {

    public static Map<GoogleMaterialFonts, GoogleMaterialIconFont> staticFonts = new HashMap<>();

    public static void registerFonts() {
        for(GoogleMaterialFonts font: GoogleMaterialFonts.values()) {
            final GoogleMaterialIconFont iconFont = new GoogleMaterialIconFont(font);
            IconFontSwing.register(iconFont);
            staticFonts.put(font, iconFont);
        }
    }

    public static GoogleMaterialIconFont getIconFont(GoogleMaterialFonts font) {
        return staticFonts.get(font);
    }

    private final GoogleMaterialFonts fontInfo;

    private Font font;

    private final Map<String, Character> codepoints;

    private GoogleMaterialIconFont(GoogleMaterialFonts fontInfo) {
        this.fontInfo = fontInfo;
        this.codepoints = readCodepoints();
    }

    @Override
    public InputStream getFontInputStream() {
        return GoogleMaterialIconFont.class.getClassLoader().getResourceAsStream(fontInfo.getFontFile());
    }

    @Override
    public String getFontFamily() {
        return fontInfo.getFontName();
    }

    /**
     * Read codepoints from file
     *
     * @return map of icon names to codepoints
     */
    public Map<String, Character> readCodepoints() {
        final Map<String, Character> retVal = new HashMap<>();
        try {
            final InputStream is = GoogleMaterialIconFont.class.getClassLoader().getResourceAsStream(fontInfo.getCodepointFile());
            final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line = null;
            while((line = reader.readLine()) != null) {
                final String[] parts = line.split(" ");
                retVal.put(parts[0], (char)Integer.parseInt(parts[1], 16));
            }
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return retVal;
    }

    public IconCode getIconCode(String iconName) {
        return new DefaultIconCode(fontInfo.getFontName(), codepoints.get(iconName.toLowerCase()));
    }

    public static class CustomAttributes extends AttributedCharacterIterator.Attribute {
        public static final AttributedCharacterIterator.Attribute FILL = new CustomAttributes("FILL");

        protected CustomAttributes(String name) {
            super(name);
        }
    }

    public Icon buildIcon(String name, Float size, Color color) {
        if(font == null) {
            try {
                font = Font.createFont(Font.TRUETYPE_FONT, getFontInputStream());
            } catch (IOException | FontFormatException e) {
                throw new RuntimeException(e);
            }
        }
        final Font iconFont = font.deriveFont(size);
        final var codepoint = codepoints.get(name.toLowerCase());
        if(codepoint == null) {
            throw new IllegalArgumentException("No icon with name " + name);
        }
        final BufferedImage img = buildImage(Character.toString(codepoints.get(name.toLowerCase())), iconFont, color);
        final ImageIcon icon = new ImageIcon(img);
        return icon;
    }

    public Font createFont(float size) throws IOException, FontFormatException {
        final Map<AttributedCharacterIterator.Attribute, Object> map = new HashMap<>();
        map.put(CustomAttributes.FILL, 100.0f);
        final Font font = Font.createFont(Font.TRUETYPE_FONT, getFontInputStream()).deriveFont(size);
        return font;
    }

    private static BufferedImage buildImage(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(font);
        Dimension dim = label.getPreferredSize();
        BufferedImage bufImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = bufImage.createGraphics();
        int width = dim.width;
        final LineMetrics lineMetrics = font.getLineMetrics(text, g2d.getFontRenderContext());
        int height = (int)lineMetrics.getAscent() - (int)lineMetrics.getDescent();
        label.setSize(width, height);
        bufImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        g2d.dispose();
        g2d = bufImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        label.print(g2d);
        g2d.dispose();
        return bufImage;
    }

}
