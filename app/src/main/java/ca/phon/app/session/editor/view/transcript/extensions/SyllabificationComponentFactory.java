package ca.phon.app.session.editor.view.transcript.extensions;

import ca.phon.app.session.editor.EditorEventManager;
import ca.phon.app.session.editor.SessionEditor;
import ca.phon.app.session.editor.view.syllabificationAlignment.ScTypeEdit;
import ca.phon.app.session.editor.view.syllabificationAlignment.ToggleDiphthongEdit;
import ca.phon.app.session.editor.view.transcript.BreakableFlowLayout;
import ca.phon.app.session.editor.view.transcript.BreakableView;
import ca.phon.app.session.editor.view.transcript.ComponentFactory;
import ca.phon.app.session.editor.view.transcript.TranscriptEditor;
import ca.phon.ipa.IPAElement;
import ca.phon.ipa.IPATranscript;
import ca.phon.ipa.IPATranscriptBuilder;
import ca.phon.ipa.Phone;
import ca.phon.session.Session;
import ca.phon.session.Tier;
import ca.phon.syllable.SyllableConstituentType;
import ca.phon.ui.action.PhonActionEvent;
import ca.phon.ui.action.PhonUIAction;
import ca.phon.ui.ipa.SyllabificationDisplay;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.undo.UndoableEditSupport;
import java.awt.event.KeyEvent;
import java.util.Arrays;

public class SyllabificationComponentFactory implements ComponentFactory {

    public static String Y_AXIS_ALIGNMENT = "yAxisAlignment";
    public static float Y_AXIS_ALIGNMENT_VALUE = 0.75f;

    final TranscriptEditor editor;

    final Session session;

    final EditorEventManager eventManager;

    final UndoableEditSupport undoSupport;

    private JPanel previousComponent;

    public SyllabificationComponentFactory(TranscriptEditor editor) {
        this.editor = editor;
        this.session = editor.getSession();
        this.eventManager = editor.getEventManager();
        this.undoSupport = editor.getUndoSupport();
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
        retVal.putClientProperty(Y_AXIS_ALIGNMENT, Y_AXIS_ALIGNMENT_VALUE);
        retVal.setBackground(UIManager.getColor("text"));

        int currentIndex = 0;

        // clone transcript
        final IPATranscript clonedTranscript = (new IPATranscriptBuilder()).append(tier.getValue().toString(true)).toIPATranscript();
        for(IPATranscript word:clonedTranscript.words()) {
            final SyllabificationDisplay display = new SyllabificationDisplay();
            display.setTranscript(word);
            retVal.add(display);

            // setup tab, shift+tab, up/down key actions
            final InputMap inputMap = display.getInputMap(JComponent.WHEN_FOCUSED);
            final ActionMap actionMap = display.getActionMap();

            final KeyStroke tabKey = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0);
            inputMap.put(tabKey, "focusNextTier");
            actionMap.put("focusNextTier", PhonUIAction.eventConsumer(this::focusNextTier, retVal));

            final KeyStroke shiftTabKey = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, KeyEvent.SHIFT_DOWN_MASK);
            inputMap.put(shiftTabKey, "focusPrevTier");
            actionMap.put("focusPrevTier", PhonUIAction.eventConsumer(this::focusPrevTier, retVal));

            final KeyStroke upKey = KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0);
            inputMap.put(upKey, "focusPrevTier");

            final KeyStroke downKey = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0);
            inputMap.put(downKey, "focusNextTier");

            display.addPropertyChangeListener("focusNext", (e) -> {
                final int idx = Arrays.asList(retVal.getComponents()).indexOf(display);
                if(idx < retVal.getComponentCount()-1) {
                    if(retVal.getComponent(idx+1) instanceof SyllabificationDisplay nextDisplay) {
                        nextDisplay.requestFocus();
                        nextDisplay.setFocusedPhone(0);
                    }
                }
            });
            display.addPropertyChangeListener("focusPrev", (e) -> {
                final int idx = Arrays.asList(retVal.getComponents()).indexOf(display);
                if(idx > 0) {
                    if(retVal.getComponent(idx-1) instanceof SyllabificationDisplay prevDisplay) {
                        prevDisplay.requestFocus();
                        prevDisplay.setFocusedPhone(prevDisplay.getDisplayedPhones().length()-1);
                    }
                }
            });

            if(this.session != null && this.eventManager != null && this.undoSupport != null) {
                final int phoneIndex = currentIndex;
                display.addPropertyChangeListener(SyllabificationDisplay.SYLLABIFICATION_PROP_ID, (e) -> {
                    final SyllabificationDisplay.SyllabificationChangeData data = (SyllabificationDisplay.SyllabificationChangeData) e.getNewValue();
                    final ScTypeEdit edit = new ScTypeEdit(this.session, this.eventManager, tier.getValue(), phoneIndex + data.position(), data.scType());
                    edit.setSource(display);
                    this.undoSupport.postEdit(edit);
                });

                display.addPropertyChangeListener(SyllabificationDisplay.HIATUS_CHANGE_PROP_ID, (e) -> {
                    final SyllabificationDisplay.HiatusChangeData data = (SyllabificationDisplay.HiatusChangeData) e.getNewValue();
                    final int pIdx = phoneIndex + data.position1();
                    final int pIdx2 = phoneIndex + data.position2();
                    final ToggleDiphthongEdit diphthongEdit1 = new ToggleDiphthongEdit(this.session, this.eventManager, tier.getValue(), pIdx);
                    diphthongEdit1.setSource(display);
                    final ToggleDiphthongEdit diphthongEdit2 = new ToggleDiphthongEdit(this.session, this.eventManager, tier.getValue(), pIdx2);
                    diphthongEdit2.setSource(display);
                    this.undoSupport.beginUpdate();
                    this.undoSupport.postEdit(diphthongEdit1);
                    this.undoSupport.postEdit(diphthongEdit2);
                    this.undoSupport.endUpdate();
                });
            }
            currentIndex += word.length() + 1;
        }

        previousComponent = retVal;

        return retVal;
    }

    private IPATranscript buildTranscriptFromPanel(JPanel panel) {
        final IPATranscriptBuilder builder = new IPATranscriptBuilder();
        for(int i = 0; i < panel.getComponentCount(); i++) {
            if(panel.getComponent(i) instanceof SyllabificationDisplay display) {
                if (i > 0)
                    builder.appendWordBoundary();
                builder.append(display.getTranscript());
            }
        }
        return builder.toIPATranscript();
    }

    private void focusNextTier(PhonActionEvent<JPanel> pae) {
        final IPATranscript transcript = buildTranscriptFromPanel(pae.getData());

        // get focused element
        int offset = -1;
        for(int i = 0; i < pae.getData().getComponentCount(); i++) {
            if(pae.getData().getComponent(i) instanceof SyllabificationDisplay display) {
                if(display.hasFocus()) {
                    IPAElement focusedElement = display.getDisplayedPhones().elementAt(display.getFocusedPhone());
                    offset = transcript.indexOf(focusedElement);
                }
            }
        }

        if(offset >= 0) {
            editor.offsetInNextTierOrElement(transcript.stringIndexOfElement(offset));
        } else {
            editor.sameOffsetInNextTierOrElement();
        }
        editor.requestFocus();
    }

    private void focusPrevTier(PhonActionEvent<JPanel> pae) {
        final IPATranscript transcript = buildTranscriptFromPanel(pae.getData());

        // get focused element
        int offset = -1;
        for(int i = 0; i < pae.getData().getComponentCount(); i++) {
            if(pae.getData().getComponent(i) instanceof SyllabificationDisplay display) {
                if(display.hasFocus()) {
                    IPAElement focusedElement = display.getDisplayedPhones().elementAt(display.getFocusedPhone());
                    offset = transcript.indexOf(focusedElement);
                }
            }
        }

        if(offset >= 0) {
            editor.offsetInPrevTierOrElement(transcript.stringIndexOfElement(offset));
        } else {
            editor.sameOffsetInPrevTierOrElement();
        }
        editor.requestFocus();
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
                if(previousComponent.getComponent(i) instanceof SyllabificationDisplay display) {
                    if(display.getDisplayedPhones().indexOf(selectedElement) >= 0) {
                        display.requestFocus();
                        display.setFocusedPhone(display.getDisplayedPhones().indexOf(selectedElement));
                        break;
                    }
                }
            }
        }
    }

    @Override
    public JComponent getComponent() {
        return previousComponent;
    }

}
