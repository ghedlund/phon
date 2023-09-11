package ca.phon.app.session.editor.view.transcriptEditor;

import ca.phon.ipa.IPATranscript;
import ca.phon.session.Tier;
import ca.phon.ui.ipa.SyllabificationDisplay;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.StyleConstants;

public class SyllabificationComponentFactory implements ComponentFactory {
    @Override
    public JComponent createComponent(AttributeSet attrs) {
        Tier<IPATranscript> tier = (Tier<IPATranscript>) attrs.getAttribute("tier");

        SyllabificationDisplay display = new SyllabificationDisplay();
        display.setTranscript(tier.getValue());

        return display;
    }
}
