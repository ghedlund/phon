package ca.phon.app.session.editor.view.transcript.extensions;

import ca.phon.app.session.editor.EditorEventManager;
import ca.phon.app.session.editor.SessionEditor;
import ca.phon.app.session.editor.view.syllabificationAlignment.ScTypeEdit;
import ca.phon.app.session.editor.view.transcript.BreakableFlowLayout;
import ca.phon.app.session.editor.view.transcript.BreakableView;
import ca.phon.app.session.editor.view.transcript.ComponentFactory;
import ca.phon.ipa.IPATranscript;
import ca.phon.ipa.IPATranscriptBuilder;
import ca.phon.session.Session;
import ca.phon.session.Tier;
import ca.phon.syllable.SyllableConstituentType;
import ca.phon.ui.ipa.SyllabificationDisplay;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.undo.UndoableEditSupport;
import java.util.Arrays;

public class SyllabificationComponentFactory implements ComponentFactory {

    final Session session;

    final EditorEventManager eventManager;

    final UndoableEditSupport undoSupport;

    private JPanel previousComponent;

    public SyllabificationComponentFactory(Session session, EditorEventManager eventManager, UndoableEditSupport undoSupport) {
        this.session = session;
        this.eventManager = eventManager;
        this.undoSupport = undoSupport;
    }

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

        int currentIndex = 0;

        // clone transcript
        final IPATranscript clonedTranscript = (new IPATranscriptBuilder()).append(tier.getValue().toString(true)).toIPATranscript();
        for(IPATranscript word:clonedTranscript.words()) {
            final SyllabificationDisplay display = new SyllabificationDisplay();
            display.setTranscript(word);
            retVal.add(display);

            display.addPropertyChangeListener("focusNext", (e) -> {
                final int idx = Arrays.asList(retVal.getComponents()).indexOf(display);
                if(idx < retVal.getComponentCount()-1) {
                    final SyllabificationDisplay nextDisplay = (SyllabificationDisplay)retVal.getComponent(idx+1);
                    if(nextDisplay != null) {
                        nextDisplay.requestFocus();
                        nextDisplay.setFocusedPhone(0);
                    }
                }
            });
            display.addPropertyChangeListener("focusPrev", (e) -> {
                final int idx = Arrays.asList(retVal.getComponents()).indexOf(display);
                if(idx > 0) {
                    final SyllabificationDisplay prevDisplay = (SyllabificationDisplay)retVal.getComponent(idx-1);
                    if(prevDisplay != null) {
                        prevDisplay.requestFocus();
                        prevDisplay.setFocusedPhone(prevDisplay.getDisplayedPhones().length()-1);
                    }
                }
            });

            if(this.session != null && this.eventManager != null && this.undoSupport != null) {
                final int phoneIndex = currentIndex;
                display.addPropertyChangeListener(SyllabificationDisplay.SYLLABIFICATION_PROP_ID, (e) -> {
                    final SyllabificationDisplay.SyllabificationChangeData data = (SyllabificationDisplay.SyllabificationChangeData) e.getNewValue();
                    final ScTypeEdit edit = new ScTypeEdit(this.session, this.eventManager, tier.getValue(), phoneIndex + data.getPosition(), data.getScType());
                    edit.setSource(display);
                    this.undoSupport.postEdit(edit);
                });
            }
            currentIndex += word.length() + 1;
        }

        previousComponent = retVal;

        return retVal;
    }

    @Override
    public void requestFocusStart() {
        if(previousComponent != null && previousComponent.getComponentCount() > 0) {
            final SyllabificationDisplay display = (SyllabificationDisplay) previousComponent.getComponent(0);
            if(display != null) {
                display.requestFocus();
                display.setFocusedPhone(0);
            }
        }
    }

    @Override
    public void requestFocusEnd() {
        if(previousComponent != null && previousComponent.getComponentCount() > 0) {
            final SyllabificationDisplay display = (SyllabificationDisplay) previousComponent.getComponent(previousComponent.getComponentCount()-1);
            if(display != null) {
                display.requestFocus();
                display.setFocusedPhone(display.getDisplayedPhones().length()-1);
            }
        }
    }

    @Override
    public void requestFocusAtOffset(int offset) {
        if(previousComponent != null) {
            int currentOffset = 0;
            for(int i = 0; i < previousComponent.getComponentCount(); i++) {
                final SyllabificationDisplay display = (SyllabificationDisplay) previousComponent.getComponent(i);
                if(currentOffset + display.getDisplayedPhones().length() >= offset) {
                    display.requestFocus();
                    display.setFocusedPhone(offset - currentOffset);
                    return;
                }
                currentOffset += display.getDisplayedPhones().length();
            }
            if(offset >= currentOffset) {
                requestFocusEnd();
            } else if(offset <= 0) {
                requestFocusStart();
            }
        }
    }

    @Override
    public JComponent getComponent() {
        return previousComponent;
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
