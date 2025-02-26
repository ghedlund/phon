package ca.phon.app.session.editor.view.transcript.extensions;

import ca.phon.app.session.editor.EditorEventManager;
import ca.phon.app.session.editor.view.transcript.BreakableFlowLayout;
import ca.phon.app.session.editor.view.transcript.ComponentFactory;
import ca.phon.app.session.editor.view.transcript.TranscriptEditor;
import ca.phon.ipa.IPAElement;
import ca.phon.ipa.IPATranscript;
import ca.phon.ipa.IPATranscriptBuilder;
import ca.phon.ipa.Phone;
import ca.phon.ipa.alignment.PhoneMap;
import ca.phon.session.PhoneAlignment;
import ca.phon.session.Session;
import ca.phon.session.Tier;
import ca.phon.ui.ipa.PhoneMapDisplay;
import ca.phon.ui.ipa.SyllabificationDisplay;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.undo.UndoableEditSupport;
import java.util.Iterator;

public class AlignmentComponentFactory implements ComponentFactory {

    final TranscriptEditor editor;

    final Session session;

    final EditorEventManager eventManager;

    final UndoableEditSupport undoSupport;

    private JPanel previousComponent;

    public AlignmentComponentFactory(TranscriptEditor editor) {
        this.editor = editor;
        this.session = editor.getSession();
        this.eventManager = editor.getEventManager();
        this.undoSupport = editor.getUndoSupport();
    }

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

        previousComponent = retVal;

        return retVal;
    }

    @Override
    public JComponent getComponent() {
        return previousComponent;
    }

    @Override
    public void requestFocusStart() {
        if(previousComponent != null && previousComponent.getComponentCount() > 0) {
            if(previousComponent.getComponent(0) instanceof PhoneMapDisplay display) {
                display.requestFocus();
                display.setFocusedPosition(0);
            }
        }
    }

    @Override
    public void requestFocusEnd() {
        if(previousComponent != null && previousComponent.getComponentCount() > 0) {
            if(previousComponent.getComponent(previousComponent.getComponentCount()-1) instanceof PhoneMapDisplay display) {
                display.requestFocus();
                display.setFocusedPosition(display.getNumberOfAlignmentPositions()-1);
            }
        }
    }

    private IPATranscript buildTranscriptFromPanel(JPanel panel) {
        final IPATranscriptBuilder builder = new IPATranscriptBuilder();
        for(int i = 0; i < panel.getComponentCount(); i++) {
            if(panel.getComponent(i) instanceof PhoneMapDisplay display) {
                if(i > 0)
                    builder.appendWordBoundary();
                builder.append(display.getPhoneMapForWord(0).getTargetRep());
            }
        }
        return builder.toIPATranscript();
    }

    @Override
    public void requestFocusAtOffset(int offset) {
        if(previousComponent != null) {
            final IPATranscript transcript = buildTranscriptFromPanel(previousComponent);
            if(offset >= transcript.length()) {
                requestFocusEnd();
                return;
            } else if(offset == 0) {
                requestFocusStart();
                return;
            }

            IPAElement selectedElement = null;
            int ipaOffset = transcript.ipaIndexOf(offset);
            while(!((selectedElement = transcript.elementAt(ipaOffset)) instanceof Phone)) {
                ipaOffset++;
            }
            for(int i = 0; i < previousComponent.getComponentCount(); i++) {
                if(previousComponent.getComponent(i) instanceof PhoneMapDisplay display) {
                    if(display.getPhoneMapForWord(0).getTargetRep().indexOf(selectedElement) >= 0) {
                        display.requestFocus();
                        display.setFocusedPosition(display.getPhoneMapForWord(0).getTopAlignmentElements().indexOf(selectedElement));
                        break;
                    }
                }
            }
        }
    }
}
