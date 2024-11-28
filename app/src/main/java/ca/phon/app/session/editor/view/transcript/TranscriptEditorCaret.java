package ca.phon.app.session.editor.view.transcript;

import ca.phon.app.log.LogUtil;
import ca.phon.session.position.TranscriptElementLocation;
import ca.phon.util.PrefHelper;
import org.apache.logging.log4j.Level;

import javax.swing.*;
import javax.swing.plaf.TextUI;
import javax.swing.text.*;
import java.awt.*;

/**
 * Custom caret implementation for {@link TranscriptEditor}.
 */
public class TranscriptEditorCaret extends DefaultCaret {
    private int caretWidth = 1;

    private transient volatile boolean freezeCaret = false;

    public TranscriptEditorCaret() {
        super();
        setBlinkRate(500);
    }

    /* Methods copied from DefaultCaret */
    private boolean _contains(int X, int Y, int W, int H) {
        int w = this.width;
        int h = this.height;
        if ((w | h | W | H) < 0) {
            // At least one of the dimensions is negative...
            return false;
        }
        // Note: if any dimension is zero, tests below must return false...
        int x = this.x;
        int y = this.y;
        if (X < x || Y < y) {
            return false;
        }
        if (W > 0) {
            w += x;
            W += X;
            if (W <= X) {
                // X+W overflowed or W was zero, return false if...
                // either original w or W was zero or
                // x+w did not overflow or
                // the overflowed x+w is smaller than the overflowed X+W
                if (w >= x || W > w) return false;
            } else {
                // X+W did not overflow and W was not zero, return false if...
                // original w was zero or
                // x+w did not overflow and x+w is smaller than X+W
                if (w >= x && W > w) return false;
            }
        }
        else if ((x + w) < X) {
            return false;
        }
        if (H > 0) {
            h += y;
            H += Y;
            if (H <= Y) {
                if (h >= y || H > h) return false;
            } else {
                if (h >= y && H > h) return false;
            }
        }
        else if ((y + h) < Y) {
            return false;
        }
        return true;
    }

    @Override
    public void install(JTextComponent c) {
        if(!(c instanceof TranscriptEditor))
            throw new IllegalArgumentException("Invalid Text component type");
        super.install(c);
    }

    public void paint(Graphics g) {
        if(isVisible()) {
            if(getDot() < 0 || getDot() > getComponent().getDocument().getLength())
                return;
            final TranscriptEditor component = (TranscriptEditor) getComponent();
            try {
                TextUI mapper = component.getUI();
                Rectangle r = mapper.modelToView(component, getDot(), getDotBias());
                if ((r == null) || ((r.width == 0) && (r.height == 0))) {
                    return;
                }
                if (width > 0 && height > 0 &&
                        !this._contains(r.x, r.y, r.width, r.height)) {
                    // We seem to have gotten out of sync and no longer
                    // contain the right location, adjust accordingly.
                    Rectangle clip = g.getClipBounds();

                    if (clip != null && !clip.contains(this)) {
                        // Clip doesn't contain the oldLoc location, force it
                        // to be repainted lest we leave a caret around.
                        repaint();
                    }

                    // This will potentially cause a repaint of something
                    // we're already repainting, but without changing the
                    // semantics of damage we can't really get around this.
                    damage(r);
                }
                Element ele = component.getTranscriptDocument().getCharacterElement(getDot());
                int actualLineHeight = g.getFontMetrics().getHeight();
                if(ele != null) {
                    final AttributeSet attrs = ele.getAttributes();
                    if(StyleConstants.getFontFamily(attrs) != null && StyleConstants.getFontSize(attrs) > 0) {
                        int style = (StyleConstants.isBold(attrs) ? Font.BOLD : 0) |
                                (StyleConstants.isItalic(attrs) ? Font.ITALIC : 0);
                        final float fontSizeDelta = PrefHelper.getFloat(TranscriptView.FONT_SIZE_DELTA_PROP, 0.0f);
                        final Font f = new Font(StyleConstants.getFontFamily(attrs), style, StyleConstants.getFontSize(attrs) + (int)fontSizeDelta);
                        actualLineHeight = g.getFontMetrics(f).getHeight();
                    }
                }
                r.height = actualLineHeight;
                g.setColor(component.getCaretColor());
                int paintWidth = caretWidth;
                r.x -= paintWidth >> 1;
                g.fillRect(r.x, r.y, paintWidth, r.height);
            } catch (BadLocationException e) {
                LogUtil.warning(e.getLocalizedMessage(), e);
            }
        }
    }

    public boolean isFreezeCaret() {
        return this.freezeCaret;
    }

    public void setFreezeCaret(boolean freezeCaret) {
        var oldVal = this.freezeCaret;
        this.freezeCaret = freezeCaret;
        if(oldVal != freezeCaret) {
            fireStateChanged();
            if(PrefHelper.getBoolean("phon.debug", false)) {
                LogUtil.log(Level.TRACE, "Transcript Editor caret freeze: " + freezeCaret);
            }
        }
    }

    public void freeze() {
        setFreezeCaret(true);
    }

    public void unfreeze() {
        setFreezeCaret(false);
    }

    private boolean shouldForceDot = false;
    public void setDot(int dot, boolean force) {
        shouldForceDot = true;
        super.setDot(dot);
        shouldForceDot = false;
    }

    @Override
    public void setDot(int dot, Position.Bias bias) {
        setDot(dot, bias, shouldForceDot);
    }

    public void setDot(int dot, Position.Bias bias, boolean force) {
        if(force || !isFreezeCaret())
            super.setDot(dot, bias);
    }

    @Override
    public void moveDot(int dot, Position.Bias bias) {
        if(!isFreezeCaret())
            super.moveDot(dot, bias);
    }

    /**
     * Get caret transcript location
     */
    public TranscriptElementLocation getTranscriptLocation() {
        final TranscriptEditor editor = (TranscriptEditor)getComponent();
        return editor.charPosToSessionLocation(getDot());
    }

}
