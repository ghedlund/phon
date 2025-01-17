package ca.phon.app.session.editor.view.transcript;

import ca.phon.app.log.LogUtil;
import ca.phon.session.Session;
import ca.phon.ui.fonts.FontPreferences;
import ca.phon.util.PrefHelper;
import org.apache.commons.logging.Log;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * The {@link ViewFactory} used by the {@link TranscriptEditor}
 */
public class TranscriptViewFactory implements ViewFactory {

    public final static int LABEL_COLUMN_WIDTH = 150;

    public final static int LABEL_COLUMN_PADDING = 0;

    public final static int PAGE_WIDTH = (int)(8.5 * 96);

    public final static int MAX_LABEL_COLUMN_WIDTH = PAGE_WIDTH / 2;

    public final static float LINE_SPACING = 0.2f;

    public int pageWidth = PAGE_WIDTH;

    private int labelColumnWidth = -1;

    /**
     * The spacing between the lines of the document
     */

    private float lineSpacing = 0.2f;

    /**
     * Calculate preferred tier label width for given document
     *
     * @param g
     * @param transcriptDocument
     * @return width of tier label column
     */
    public static int calculatePreferredLabelColumnWidth(Graphics g, TranscriptDocument transcriptDocument) {
        var root = transcriptDocument.getDefaultRootElement();
        return calculatePreferredLabelColumnWidth(g, 0, root);
    }

    private static int calculatePreferredLabelColumnWidth(Graphics g, int currentMax, Element element) {
        if(element.isLeaf()) {
            var attrs = element.getAttributes();
            var isLabel = TranscriptStyleConstants.isLabel(attrs);
            if (isLabel) {
                var tier = TranscriptStyleConstants.getTier(attrs);
                if(tier != null) {
                    var lblText = "    " + tier.getName() + ": ";
                    var tierLabelWidth = g.getFontMetrics(FontPreferences.getTierFont()).stringWidth(lblText);
                    return Math.max(currentMax, tierLabelWidth);
                }
            }
            return currentMax;
        } else {
            int retVal = currentMax;
            for (int i = 0; i < element.getElementCount(); i++) {
                var elem = element.getElement(i);
                retVal = Math.max(retVal, calculatePreferredLabelColumnWidth(g, retVal, elem));
            }
            return retVal;
        }
    }

    private static int calculatePreferredLabelColumnWidth(Graphics g, Session session) {
        int currentMax = 0;

        for (var tier : session.getTierView()) {
            if(!tier.isVisible()) continue;
            var lblText = "    " + tier.getTierName() + ": ";
            final float fontSizeDelta = PrefHelper.getFloat(TranscriptView.FONT_SIZE_DELTA_PROP, 0.0f);
            var tierLabelWidth = g.getFontMetrics(FontPreferences.getTierFont().deriveFont(
                    FontPreferences.getTierFont().getSize() + fontSizeDelta
            )).stringWidth(lblText);
            currentMax = Math.max(currentMax, tierLabelWidth);
        }

        return currentMax + LABEL_COLUMN_PADDING;
    }

    private final Session session;

    public TranscriptViewFactory(Session session) {
        this.session = session;

    }

    public float getLineSpacing() {
        return lineSpacing;
    }

    public int getLabelColumnWidth(Graphics g, TranscriptDocument transcriptDocument) {
        if(labelColumnWidth < 0) {
            labelColumnWidth = Math.min(calculatePreferredLabelColumnWidth(g, this.session), MAX_LABEL_COLUMN_WIDTH);
        }
        return labelColumnWidth;
    }

    public int getCurrentLabelColumnWidth() {
        if(labelColumnWidth < 0) {
            // create a buffered image, get the graphics and calculate preferred label column width
            var bi = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            var g = bi.createGraphics();
            labelColumnWidth = calculatePreferredLabelColumnWidth(g, this.session);
        }
        return labelColumnWidth;
    }

    public int getPageWidth() {
        return pageWidth;
    }

    @Override
    public View create(Element elem) {
        String kind = elem.getName();
        var attrs = elem.getAttributes();

        var componentFactory = TranscriptStyleConstants.getComponentFactory(attrs);
        if (componentFactory != null) {
            kind = "componentFactory";
        }

        var isLabel = TranscriptStyleConstants.isLabel(attrs);
        var clickable = TranscriptStyleConstants.isUnderlineOnHover(attrs);
        if(isLabel && clickable) {
            kind = "label";
        }

        if(TranscriptStyleConstants.isNewParagraph(attrs)) {
            kind = "paragraphMarker";
        }

        if (kind != null) {
            switch (kind) {
                case "paragraphMarker" -> {
                    return new ParagraphMarkerView(elem);
                }
                case "label" -> {
                    return new TierLabelView(elem);
                }
                case AbstractDocument.ContentElementName -> {
                    return new CustomWrapView(elem);
                }
                case AbstractDocument.ParagraphElementName -> {
                    return new TierParagraphView(elem);
                }
                case AbstractDocument.SectionElementName -> {
                    return new SessionBoxView(elem, View.Y_AXIS);
                }
                case StyleConstants.ComponentElementName -> {
                    return new ComponentView(elem);
                }
                case StyleConstants.IconElementName -> {
                    return new IconView(elem);
                }
                case "componentFactory" -> {
                    return new ComponentFactoryView(elem);
                }
            }
        }

        // default to text display
        return new LabelView(elem);
    }

    /**
     * A {@link ComponentView} that dynamically loads components from a component factory referenced in the
     * {@code TranscriptStyleConstants.ATTR_KEY_COMPONENT_FACTORY} attribute
     */
    private static class ComponentFactoryView extends ComponentView {
        public ComponentFactoryView(Element elem) {
            super(elem);
        }

        @Override
        protected Component createComponent() {
            AttributeSet attrs = getAttributes();
            var componentFactory = TranscriptStyleConstants.getComponentFactory(attrs);
            if (componentFactory != null) {
                return componentFactory.createComponent(attrs);
            }

            return null;
        }
    }

    /**
     * A {@link BoxView} that is used to display the transcript and is used to set the width of the
     * transcript to the width of a letter page
     */
    private class SessionBoxView extends BoxView {

        public SessionBoxView(Element elem, int axis) {
            super(elem, axis);
        }

        @Override
        protected void layoutMinorAxis(int targetSpan, int axis, int[] offsets, int[] spans) {
            super.layoutMinorAxis(getPageWidth(), axis, offsets, spans);
        }

    }

    /**
     * A {@link LabelView} that is used to display tier labels
     */
    private class TierLabelView extends LabelView {

        public TierLabelView(Element elem) {
            super(elem);
        }

        @Override
        public float getPreferredSpan(int axis) {
            float span = super.getPreferredSpan(axis);
            if(axis == View.X_AXIS) {
                span = getLabelColumnWidth(getGraphics(), (TranscriptDocument) getElement().getDocument()) - labelEndWidth();
            }
            return span;
        }

        @Override
        public float getMinimumSpan(int axis) {
            return getPreferredSpan(axis);
        }

        @Override
        public float getMaximumSpan(int axis) {
            return getPreferredSpan(axis);
        }

        @Override
        public int getBreakWeight(int axis, float pos, float len) {
            return View.BadBreakWeight;
        }

        private float labelTextWidth() {
            int p0 = getStartOffset();
            int p1 = getEndOffset();
            Segment text = getText(p0, p1);
            String str = text.toString();
            Font font = getFont();
            Rectangle2D textBounds = getGraphics().getFontMetrics(font).getStringBounds(text.toString(), getGraphics());
            return (int)textBounds.getWidth();
        }

        private int labelEndWidth() {
            int p0 = getStartOffset();
            int p1 = getEndOffset();
            String text = ": ";
            Font font = getFont();
            Rectangle2D textBounds = getGraphics().getFontMetrics(font).getStringBounds(text.toString(), getGraphics());
            return (int)textBounds.getWidth();
        }


        @Override
        public TabExpander getTabExpander() {
            final TabExpander defaultExpander = super.getTabExpander();
            return new TabExpander() {
                @Override
                public float nextTabStop(float x, int tabOffset) {
                    return getLabelColumnWidth(getGraphics(), (TranscriptDocument) getElement().getDocument()) - labelTextWidth() - labelEndWidth();
                }
            };
        }

        @Override
        public Font getFont() {
            final Font font = super.getFont();
            final float fontSizeDelta = PrefHelper.getFloat(TranscriptView.FONT_SIZE_DELTA_PROP, 0.0f);
            final float newFontSize = Math.max(Math.min(font.getSize() + fontSizeDelta, 72.0f), 6.0f);
            return font.deriveFont(newFontSize);
        }

        @Override
        public void paint(Graphics g, Shape a) {
//            Graphics2D g2 = (Graphics2D)g;
//            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

//            int p0 = getStartOffset();
//            int p1 = getEndOffset();
//            Segment text = getText(p0, p1);
//            final String tierName = text.toString().trim();
//            final String labelText = tierName;
//            final Font font = getFont();
//            final FontMetrics fm = g.getFontMetrics(font);
//            final int x = a.getBounds().x + a.getBounds().width - fm.stringWidth(labelText);
//            final int y = a.getBounds().y + fm.getAscent();

            g.clearRect(a.getBounds().x, a.getBounds().y, a.getBounds().width, a.getBounds().height);
            super.paint(g, a);
//            g.setColor(getForeground());
//            g.setFont(font);
//            g.drawString(labelText, x, y);
        }

    }

    /**
     * A {@link ParagraphView} that is used to display tier paragraphs including label and content
     */
    private class TierParagraphView extends ParagraphView {

//        private Border border = BorderFactory.createMatteBorder(0, 0, 0, 0, Color.LIGHT_GRAY);

        public TierParagraphView(Element elem) {
            super(elem);
            setPropertiesFromAttributes();
            setLineSpacing(getLineSpacing());
            SimpleAttributeSet attrs = new SimpleAttributeSet();
            StyleConstants.setLeftIndent(attrs, labelColumnWidth);
            setParagraphInsets(attrs);
            setFirstLineIndent(-labelColumnWidth);
        }

        private Border getBorder() {
            final AttributeSet attrs = getAttributes();
            final Border border = TranscriptStyleConstants.getBorder(attrs);
            if(border != null) {
                return border;
            } else {
                return BorderFactory.createEmptyBorder();
            }
        }

        // add border insets to view insets
        @Override
        public short getTopInset() {
            return (short) (super.getTopInset() + getBorder().getBorderInsets(getContainer()).top);
        }

        @Override
        public short getBottomInset() {
            return (short) (super.getBottomInset() + getBorder().getBorderInsets(getContainer()).bottom);
        }

        @Override
        public short getLeftInset() {
            return (short) (super.getLeftInset() + getBorder().getBorderInsets(getContainer()).left);
        }

        @Override
        public short getRightInset() {
            return (short) (super.getRightInset() + getBorder().getBorderInsets(getContainer()).right);
        }

        @Override
        public void paint(Graphics g, Shape a) {
            // return if view count is 0 to avoid an issue at the end of the document when populating using a SwingWorker
            if(getViewCount() == 0) return;
            super.paint(g, a);
            getBorder().paintBorder(getContainer(), g, a.getBounds().x, a.getBounds().y, a.getBounds().width, a.getBounds().height);
        }

    }

    static class ParagraphMarkerView extends LabelView {

        /**
         * Constructs a new view wrapped on an element.
         *
         * @param elem the element
         */
        public ParagraphMarkerView(Element elem) {
            super(elem);

            setSize(0, 0);

        }

        @Override
        public float getMinimumSpan(int axis) {
            if(axis == View.X_AXIS) {
                return 0;
            } else {
                return super.getMinimumSpan(axis);
            }
        }

        @Override
        public float getPreferredSpan(int axis) {
            if(axis == View.X_AXIS) {
                return 0;
            } else {
                return super.getPreferredSpan(axis);
            }
        }

        @Override
        public Font getFont() {
            final Font font = super.getFont();
            final float fontSizeDelta = PrefHelper.getFloat(TranscriptView.FONT_SIZE_DELTA_PROP, 0.0f);
            final float newFontSize = Math.max(Math.min(font.getSize() + fontSizeDelta, 72.0f), 6.0f);
            return font.deriveFont(newFontSize);
        }
    }

    /**
     * A {@link LabelView} that is used to display content within a tier paragraph.  Content is wrapped
     * on spaces only.
     */
    static class CustomWrapView extends LabelView {
        public CustomWrapView(Element elem) {
            super(elem);
        }

        @Override
        public Font getFont() {
            final Font font = super.getFont();
            final float fontSizeDelta = PrefHelper.getFloat(TranscriptView.FONT_SIZE_DELTA_PROP, 0.0f);
            final float newFontSize = Math.max(Math.min(font.getSize() + fontSizeDelta, 72.0f), 6.0f);
            return font.deriveFont(newFontSize);
        }

        @Override
        public void paint(Graphics g, Shape a) {
            g.clearRect(a.getBounds().x, a.getBounds().y, a.getBounds().width, a.getBounds().height);
            // XXX - this is a hack to get the background color to paint correctly, not working on windows
//            final boolean isLabel = TranscriptStyleConstants.isLabel(getAttributes());
//            g.setColor(Color.WHITE);
//            if(isLabel) {
//                g.setColor(UIManager.getColor(TranscriptEditorUIProps.LABEL_BACKGROUND));
//            } else {
//                final String elementType = TranscriptStyleConstants.getElementType(getAttributes());
//                if (elementType != null) {
//                    switch (elementType) {
//                        case TranscriptStyleConstants.ELEMENT_TYPE_COMMENT -> {
//                            g.setColor(UIManager.getColor(TranscriptEditorUIProps.COMMENT_BACKGROUND));
//                        }
//                        case TranscriptStyleConstants.ELEMENT_TYPE_GEM -> {
//                            g.setColor(UIManager.getColor(TranscriptEditorUIProps.GEM_BACKGROUND));
//                        }
//                        case TranscriptStyleConstants.ELEMENT_TYPE_GENERIC -> {
//                            g.setColor(UIManager.getColor(TranscriptEditorUIProps.GENERIC_BACKGROUND));
//                        }
//                    }
//                }
//            }
//            g.fillRect(a.getBounds().x, a.getBounds().y, a.getBounds().width, a.getBounds().height);
            super.paint(g, a);
        }

        @Override
        public int getBreakWeight(int axis, float pos, float len) {
            if (axis == View.X_AXIS) {
                checkPainter();
                int p0 = getStartOffset();
                int p1 = getGlyphPainter().getBoundedPosition(this, p0, pos, len);
                if (p1 == p0) {
                    // can't fit anything into this
                    return View.BadBreakWeight;
                }
                try {
                    if (getDocument().getText(p0, p1 - p0).contains(" ")) {
                        return View.ExcellentBreakWeight;
                    } else if(getDocument().getText(p0, p1 - p0).contains("/")) {
                        return View.ExcellentBreakWeight;
                    } else if(getDocument().getText(p0, p1 - p0).contains("-")) {
                        return View.ExcellentBreakWeight;
                    }
                } catch (BadLocationException ble) {
                    // assume we can't break
                    return View.BadBreakWeight;
                }
            }
            return View.BadBreakWeight;
        }

        @Override
        public View breakView(int axis, int p0, float pos, float len) {
            if (axis == View.X_AXIS) {
                checkPainter();
                int p1 = getGlyphPainter().getBoundedPosition(this, p0, pos, len);
                try {
                    int breakPos = getDocument().getText(p0, p1 - p0).lastIndexOf(" ");
                    if (breakPos >= 0) {
                        p1 = p0 + breakPos + 1;
                    } else {
                        breakPos = getDocument().getText(p0, p1 - p0).lastIndexOf("/");
                        if (breakPos >= 0) {
                            p1 = p0 + breakPos + 1;
                        } else {
                            breakPos = getDocument().getText(p0, p1 - p0).lastIndexOf("-");
                            if (breakPos >= 0) {
                                p1 = p0 + breakPos + 1;
                            }
                        }
                    }
                } catch (BadLocationException ble) {
                    // assume we can't break
                }
                if (p0 == getStartOffset() && p1 == getEndOffset()) {
                    return this;
                }
                return createFragment(p0, p1);
            }
            return this;
        }

    }
}