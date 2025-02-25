package ca.phon.app.session.editor.view.transcript;

import ca.phon.ipa.alignment.PhoneMap;
import ca.phon.session.PhoneAlignment;
import ca.phon.session.Tier;
import ca.phon.ui.ipa.PhoneMapDisplay;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import java.util.Iterator;

public class AlignmentComponentFactory implements ComponentFactory {
    @Override
    public JComponent createComponent(AttributeSet attrs) {
        Tier<PhoneAlignment> tier = (Tier<PhoneAlignment>) attrs.getAttribute("tier");

        int breakWidth = -1;
        if(attrs.getAttribute("TranscriptViewFactory.tierWidth") != null) {
            breakWidth = (int)attrs.getAttribute("TranscriptViewFactory.tierWidth");
        }
        final BreakableFlowLayout layout = new BreakableFlowLayout();
        layout.setBreakWidth(breakWidth);
        final JPanel retVal = new JPanel(layout);
        retVal.setBackground(UIManager.getColor("text"));

        for (Iterator<PhoneMap> i = tier.getValue().iterator(); i.hasNext();) {
            var phoneMap = i.next();
            final PhoneMapDisplay display = new PhoneMapDisplay();
            display.setPhoneMapForWord(0, phoneMap);
            retVal.add(display);
        }

        return retVal;
    }

    @Override
    public JComponent getComponent() {
        return null;
    }

    @Override
    public void requestFocusStart() {

    }

    @Override
    public void requestFocusEnd() {

    }

    @Override
    public void requestFocusAtOffset(int offset) {

    }
}
