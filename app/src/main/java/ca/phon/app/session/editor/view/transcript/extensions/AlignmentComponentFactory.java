package ca.phon.app.session.editor.view.transcript.extensions;

import ca.phon.app.session.editor.EditorEventManager;
import ca.phon.app.session.editor.undo.TierEdit;
import ca.phon.app.session.editor.view.transcript.BreakableFlowLayout;
import ca.phon.app.session.editor.view.transcript.ComponentFactory;
import ca.phon.app.session.editor.view.transcript.TranscriptEditor;
import ca.phon.app.session.editor.view.transcript.TranscriptStyleConstants;
import ca.phon.ipa.IPAElement;
import ca.phon.ipa.IPATranscript;
import ca.phon.ipa.IPATranscriptBuilder;
import ca.phon.ipa.Phone;
import ca.phon.ipa.alignment.PhoneMap;
import ca.phon.session.*;
import ca.phon.session.Record;
import ca.phon.ui.action.PhonActionEvent;
import ca.phon.ui.action.PhonUIAction;
import ca.phon.ui.ipa.PhoneMapDisplay;
import ca.phon.ui.ipa.SyllabificationDisplay;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.undo.UndoableEditSupport;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class AlignmentComponentFactory implements ComponentFactory {

    public static final String Y_AXIS_ALIGNMENT = "yAxisAlignment";
    public static final float Y_AXIS_ALIGNMENT_VALUE = .5f;

    final TranscriptEditor editor;

    final Session session;

    final EditorEventManager eventManager;

    final UndoableEditSupport undoSupport;

    final Transcriber transcriber;

    private JPanel previousComponent;

    public AlignmentComponentFactory(TranscriptEditor editor) {
        this.editor = editor;
        this.session = editor.getSession();
        this.eventManager = editor.getEventManager();
        this.undoSupport = editor.getUndoSupport();
        this.transcriber = editor.getDataModel().getTranscriber();
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
        retVal.putClientProperty(Y_AXIS_ALIGNMENT, Y_AXIS_ALIGNMENT_VALUE);

        final Record record = TranscriptStyleConstants.getRecord(attrs);
        if(record == null) return retVal;

        final PhoneAlignment phoneAlignment = tier.isBlind() ?
                transcriber == Transcriber.VALIDATOR ? tier.getValue() : tier.getBlindTranscription(transcriber.getUsername())
                : tier.getValue();
        final List<PhoneMap> clonedMaps = new ArrayList<>();
        for(PhoneMap pm:phoneAlignment) {
            final PhoneMap clonedPm = PhoneMap.fromString(pm.getTargetRep(), pm.getActualRep(), pm.toString());
            clonedMaps.add(clonedPm);
        }
        for (PhoneMap phoneMap:clonedMaps) {
            final PhoneMapDisplay display = new PhoneMapDisplay();
            display.setPhoneMapForWord(0, phoneMap);
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
                    if(retVal.getComponent(idx+1) instanceof PhoneMapDisplay nextDisplay) {
                        nextDisplay.requestFocus();
                        nextDisplay.setFocusedPosition(0);
                    }
                }
            });
            display.addPropertyChangeListener("focusPrev", (e) -> {
                final int idx = Arrays.asList(retVal.getComponents()).indexOf(display);
                if(idx > 0) {
                    if(retVal.getComponent(idx-1) instanceof PhoneMapDisplay prevDisplay) {
                        prevDisplay.requestFocus();
                        prevDisplay.setFocusedPosition(prevDisplay.getNumberOfAlignmentPositions()-1);
                    }
                }
            });

            if(this.session != null && this.eventManager != null && this.undoSupport != null) {
                final int wIdx = clonedMaps.indexOf(phoneMap);
                display.addPropertyChangeListener(PhoneMapDisplay.ALIGNMENT_CHANGE_PROP, (e) -> {
                    final PhoneMapDisplay.AlignmentChangeData newVal = (PhoneMapDisplay.AlignmentChangeData)e.getNewValue();
                    final List<IPATranscript> targetWords = record.getIPATarget().words();
                    final List<IPATranscript> actualWords = record.getIPAActual().words();
                    final IPATranscript ipaTarget = wIdx < targetWords.size() ? targetWords.get(wIdx) : new IPATranscript();
                    final IPATranscript ipaActual = wIdx < actualWords.size() ? actualWords.get(wIdx) : new IPATranscript();
                    final PhoneMap pm = new PhoneMap(ipaTarget, ipaActual);
                    pm.setTopAlignment(newVal.alignment()[0]);
                    pm.setBottomAlignment(newVal.alignment()[1]);

                    final PhoneAlignment origAlignment = record.getPhoneAlignmentTier().isBlind()
                            ? transcriber == Transcriber.VALIDATOR ? record.getPhoneAlignment() : record.getPhoneAlignmentTier().getBlindTranscription(transcriber.getUsername())
                            : record.getPhoneAlignment();
                    final List<PhoneMap> modifiedAlignments = new ArrayList<>();
                    for(int i = 0; i < origAlignment.getAlignments().size(); i++) {
                        if(i == wIdx)
                            modifiedAlignments.add(pm);
                        else
                            modifiedAlignments.add(origAlignment.getAlignments().get(i));
                    }

                    final TierEdit<PhoneAlignment> edit = new TierEdit<>(session, eventManager, transcriber, record, record.getPhoneAlignmentTier(),
                            new PhoneAlignment(modifiedAlignments));
                    edit.setSource(display);
                    edit.setValueAdjusting(false);
                    undoSupport.postEdit(edit);
                });
            }
        }

        previousComponent = retVal;

        return retVal;
    }

    private void focusNextTier(PhonActionEvent<JPanel> pae) {
        final IPATranscript transcript = buildTranscriptFromPanel(pae.getData());

        // get focused element
        int offset = -1;
        for(int i = 0; i < pae.getData().getComponentCount(); i++) {
            if(pae.getData().getComponent(i) instanceof PhoneMapDisplay display) {
                if(display.hasFocus()) {
                    final var alignedElements =
                            display.getPhoneMapForWord(0).getAlignedElements(display.getFocusedPosition());
                    IPAElement focusedElement = alignedElements.get(0) == null ? alignedElements.get(1) : alignedElements.get(0);
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
            if(pae.getData().getComponent(i) instanceof PhoneMapDisplay display) {
                if(display.hasFocus()) {
                    final var alignedElements =
                            display.getPhoneMapForWord(0).getAlignedElements(display.getFocusedPosition());
                    IPAElement focusedElement = alignedElements.get(0) == null ? alignedElements.get(1) : alignedElements.get(0);
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
