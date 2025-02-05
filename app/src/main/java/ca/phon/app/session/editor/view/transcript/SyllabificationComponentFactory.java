package ca.phon.app.session.editor.view.transcript;

import ca.phon.ipa.IPATranscript;
import ca.phon.session.Tier;
import ca.phon.ui.ipa.SyllabificationDisplay;
import org.jdesktop.swingx.HorizontalLayout;
import org.jdesktop.swingx.VerticalLayout;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import java.awt.*;

public class SyllabificationComponentFactory implements ComponentFactory {
    @Override
    public JComponent createComponent(AttributeSet attrs) {
        Tier<IPATranscript> tier = (Tier<IPATranscript>) attrs.getAttribute("tier");

        int breakWidth = -1;
        if(attrs.getAttribute("TranscriptViewFactory.tierWidth") != null) {
            breakWidth = (int)attrs.getAttribute("TranscriptViewFactory.tierWidth");
        }
        final BreakableFlowLayout layout = new BreakableFlowLayout();
        layout.setBreakWidth(breakWidth);
        final JPanel retVal = new JPanel(layout);
        retVal.setBackground(UIManager.getColor("text"));

        for(IPATranscript word:tier.getValue().words()) {
            final SyllabificationDisplay display = new SyllabificationDisplay();
            display.setTranscript(word);
            retVal.add(display);
        }
//        retVal.setPreferredSize(new Dimension(100, retVal.getPreferredSize().height));

        return retVal;
    }

    private static class BreakablePanel extends JPanel implements BreakableView {

        public BreakablePanel() {
            super(new BreakableFlowLayout());
        }

        @Override
        public void breakView(int axis, float pos) {
            final BreakableFlowLayout layout = (BreakableFlowLayout) getLayout();
            if(axis == SwingConstants.HORIZONTAL) {
                System.out.println("Break width: " + pos);
                layout.setBreakWidth((int)pos);
            }
        }
    }

}
