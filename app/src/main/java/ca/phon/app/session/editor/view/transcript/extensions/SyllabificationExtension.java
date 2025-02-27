package ca.phon.app.session.editor.view.transcript.extensions;

import ca.phon.app.log.LogUtil;
import ca.phon.app.session.editor.EditorEvent;
import ca.phon.app.session.editor.EditorEventManager;
import ca.phon.app.session.editor.EditorEventType;
import ca.phon.app.session.editor.view.syllabificationAlignment.SyllabificationAlignmentEditorView;
import ca.phon.app.session.editor.view.syllabificationAlignment.SyllabifyEdit;
import ca.phon.app.session.editor.view.transcript.*;
import ca.phon.ipa.IPAElement;
import ca.phon.ipa.IPATranscript;
import ca.phon.ipa.IPATranscriptBuilder;
import ca.phon.session.*;
import ca.phon.session.Record;
import ca.phon.session.position.TranscriptElementLocation;
import ca.phon.syllabifier.Syllabifier;
import ca.phon.syllabifier.SyllabifierLibrary;
import ca.phon.syllable.SyllabificationInfo;
import ca.phon.syllable.SyllableConstituentType;
import ca.phon.ui.action.PhonActionEvent;
import ca.phon.ui.action.PhonUIAction;
import ca.phon.ui.ipa.SyllabificationDisplay;
import ca.phon.ui.menu.MenuBuilder;
import ca.phon.util.Language;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.util.*;
import java.util.List;

/**
 * An extension that provides syllabification support to the {@link TranscriptEditor}
 * */
public class SyllabificationExtension implements TranscriptEditorExtension {
    private TranscriptEditor editor;
    private TranscriptDocument doc;

    /* Document property stuff */

    public final static String SYLLABIFICATION_IS_VISIBLE = "isSyllabificationVisible";
    public final static boolean SYLLABIFICATION_IS_VISIBLE_DEFAULT = false;
    public final static String SYLLABIFICATION_IS_COMPONENT = "isSyllabificationComponent";
    public final static boolean SYLLABIFICATION_IS_COMPONENT_DEFAULT = true;

    /* State */

    private boolean syllabificationEditMode = false;

    /**
     * Constructor
     * */
    public SyllabificationExtension() {
        super();
    }

    @Override
    public void install(TranscriptEditor editor) {
        this.editor = editor;
        this.doc = editor.getTranscriptDocument();

        // add syllabification tier at the end of the regular IPA tier content
        doc.addInsertionHook(new DefaultInsertionHook() {
            @Override
            public List<DefaultStyledDocument.ElementSpec> endTier(MutableAttributeSet attrs) {
                TranscriptBatchBuilder builder = new TranscriptBatchBuilder(editor.getTranscriptDocument());
                if (!isSyllabificationVisible() || !doc.getSingleRecordView()) return builder.getBatch();
                buildSyllabificationBatch(builder, attrs);
                return builder.getBatch();
            }
        });

        doc.addDocumentPropertyChangeListener(SYLLABIFICATION_IS_VISIBLE, this::syllabificationVisiblePropertyChangeHandler);
        doc.addDocumentPropertyChangeListener(SYLLABIFICATION_IS_COMPONENT, this::setSyllabificationIsComponentPropertyChangeHandler);

        // XXX remove due to complications with other key bindings
//        InputMap inputMap = editor.getInputMap();
//        ActionMap actionMap = editor.getActionMap();
//
//        KeyStroke esc = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
//        inputMap.put(esc, "pressedEsc");
//        PhonUIAction<Void> escAct = PhonUIAction.runnable(() -> {
//            if (syllabificationEditMode) setSyllabificationEditMode(false);
//        });
//        actionMap.put("pressedEsc", escAct);

//        editor.addCaretListener(e -> {
//            if (!syllabificationEditMode) return;
//            TranscriptElementLocation location = editor.charPosToSessionLocation(e.getDot());
//            String tierName = location.tier();
//            if (!SystemTierType.TargetSyllables.getName().equals(tierName) && !SystemTierType.ActualSyllables.getName().equals(tierName)) {
//                setSyllabificationEditMode(false);
//            }
//        });

        editor.getEventManager().registerActionForEvent(EditorEventType.TierChange, this::onTierDataChanged, EditorEventManager.RunOn.AWTEventDispatchThread);
        editor.getEventManager().registerActionForEvent(SyllabificationAlignmentEditorView.ScEdit, this::onScEdit, EditorEventManager.RunOn.AWTEventDispatchThread);
        editor.getEventManager().registerActionForEvent(TranscriptEditor.transcriptLocationChanged, this::onTranscriptLocationChanged, EditorEventManager.RunOn.AWTEventDispatchThread);

        doc.addNotEditableAttribute(TranscriptStyleConstants.ATTR_KEY_SYLLABIFICATION);
    }

    private void setSyllabificationIsComponentPropertyChangeHandler(PropertyChangeEvent evt) {
        if(!isSyllabificationVisible()) return;
        if(editor.isSingleRecordView()) {
            doc.reload();
        } else {
            final TranscriptElementLocation currentLocation = editor.getCurrentSessionLocation();
            if(!currentLocation.valid() || currentLocation.transcriptElementIndex() < 0) return;
            final Transcript.Element element = editor.getSession().getTranscript().getElementAt(currentLocation.transcriptElementIndex());
            if(!element.isRecord()) return;
            if((boolean)evt.getNewValue()) {
                // insert syllabification tiers
                addSyllabificationTiersForRecord(currentLocation.transcriptElementIndex());
            } else {
                removeSyllabificationTiersForRecord(currentLocation.transcriptElementIndex());
            }
        }
    }

    private void syllabificationVisiblePropertyChangeHandler(PropertyChangeEvent evt) {
        int transcriptElementIndex = -1;
        if(editor.isSingleRecordView()) {
            transcriptElementIndex = editor.getSession().getTranscript().getRecordElementIndex(
                    editor.getTranscriptDocument().getSingleRecordIndex()
            );
        } else {
            final TranscriptElementLocation currentLocation = editor.getCurrentSessionLocation();
            if (currentLocation == null || !currentLocation.valid() || currentLocation.transcriptElementIndex() < 0)
                return;
            final Transcript.Element element = editor.getSession().getTranscript().getElementAt(currentLocation.transcriptElementIndex());
            if (!element.isRecord()) return;
            transcriptElementIndex = currentLocation.transcriptElementIndex();
        }
        if(transcriptElementIndex >= 0) {
            if((boolean)evt.getNewValue()) {
                // insert syllabification tiers
                addSyllabificationTiersForRecord(transcriptElementIndex);
            } else {
                removeSyllabificationTiersForRecord(transcriptElementIndex);
            }
        }
    }

    private void buildSyllabificationBatch(TranscriptBatchBuilder builder, MutableAttributeSet attrs) {
        // Begin syllabification edit mode
        PhonUIAction<Void> syllabificationEditModeAct = PhonUIAction.runnable(() -> {
            String tierName = editor.getCurrentSessionLocation().tier();
            if (!tierName.equals(SystemTierType.TargetSyllables.getName()) && !tierName.equals(SystemTierType.ActualSyllables.getName())) return;
            setSyllabificationEditMode(!syllabificationEditMode);
        });

        Tier<?> tier = (Tier<?>) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);
        if (tier != null && tier.getDeclaredType().equals(IPATranscript.class)) {
            Tier<IPATranscript> ipaTier = (Tier<IPATranscript>) tier;

            // Create a dummy tier for the syllabification
            IPATranscript ipa = ipaTier.getValue();
            Tier<IPATranscript> syllableTier = doc.getSessionFactory().createTier(getSyllabifierTierNameForIPATier(tier.getName()), IPATranscript.class);
            syllableTier.setValue(ipa);

            // Set up the tier attributes for the dummy tier
            final TierViewItem tierViewItem = doc.getSession().getTierView().stream().filter(item -> item.getTierName().equals(tier.getName())).findFirst().orElse(null);
            MutableAttributeSet tierAttrs = new SimpleAttributeSet(doc.getTranscriptStyleContext().getTierAttributes(tier, tierViewItem));
            Record record = TranscriptStyleConstants.getRecord(attrs);
            TranscriptStyleConstants.setElementType(tierAttrs, TranscriptStyleConstants.ELEMENT_TYPE_RECORD);
            TranscriptStyleConstants.setRecord(tierAttrs, record);
            TranscriptStyleConstants.setParentTier(tierAttrs, tier);
            TranscriptStyleConstants.setTier(tierAttrs, syllableTier);
            TranscriptStyleConstants.setClickHandler(tierAttrs, SyllabificationExtension.this::syllabificationTierLabelClickHandler);
            builder.appendTierLabel(doc.getSession(), record, syllableTier, syllableTier.getName(), null, doc.isChatTierNamesShown(), tierAttrs);

            if(isSyllabificationComponent()) {
                tierAttrs.addAttributes(getSyllabificationDisplayAttributes());
                builder.appendBatchString(syllableTier.getValue().toString(), tierAttrs);
            } else {
                builder.appendAll(getFormattedSyllabification(ipa, tierAttrs));
            }

            final SimpleAttributeSet finalAttrs = new SimpleAttributeSet(builder.getTrailingAttributes());
            TranscriptStyleConstants.setTier(finalAttrs, syllableTier);
            TranscriptStyleConstants.setNotEditable(finalAttrs, true);
            TranscriptStyleConstants.setNotTraversable(finalAttrs, true);
            TranscriptStyleConstants.setComponentFactory(finalAttrs, new ComponentFactory() {
                @Override
                public JComponent createComponent(AttributeSet attrs) {
                    final JPanel retVal = new JPanel();
                    retVal.setPreferredSize(new Dimension(0, 0));
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
            });

            builder.appendEOL(finalAttrs);
        }
    }

    private void syllabificationTierLabelClickHandler(MouseEvent e, AttributeSet attrs) {
        // show menu for syllabification tier
        JPopupMenu popup = new JPopupMenu();
        final MenuBuilder builder = new MenuBuilder(popup);

        final Record record = TranscriptStyleConstants.getRecord(attrs);
        final Tier<IPATranscript> syllabifierTier = (Tier<IPATranscript>) TranscriptStyleConstants.getTier(attrs);
        final String ipaTierName = getIPATierForSyllabifierTier(syllabifierTier.getName());
        final Tier<IPATranscript> ipaTier = record.getTier(ipaTierName, IPATranscript.class);

        // reset syllabification for ipa tier
        final PhonUIAction<ResetSyllabificationData> resetSyllabificationAct = PhonUIAction.eventConsumer(this::resetSyllabification,
                new ResetSyllabificationData(TranscriptStyleConstants.getRecord(attrs), ipaTier, syllabifierTier));
        resetSyllabificationAct.putValue(PhonUIAction.NAME, "Reset syllabification");
        builder.addItem(".", resetSyllabificationAct);

        popup.show(e.getComponent(), e.getX(), e.getY());
    }

    private Syllabifier syllabifierForTier(Tier<IPATranscript> tier) {
        final SyllabifierLibrary library = SyllabifierLibrary.getInstance();
        final SyllabifierInfo info = editor.getSession().getExtension(SyllabifierInfo.class);
        Language syllabifierLanguage = info.getSyllabifierLanguageForTier(tier.getName());
        if(syllabifierLanguage == null)
            syllabifierLanguage = library.defaultSyllabifierLanguage();
        return library.getSyllabifierForLanguage(syllabifierLanguage);
    }

    private record ResetSyllabificationData(Record record, Tier<IPATranscript> ipaTier, Tier<IPATranscript> syllabifierTier) {}
    private void resetSyllabification(PhonActionEvent<ResetSyllabificationData> resetData) {
        final Tier<IPATranscript> ipaTier = resetData.getData().ipaTier();
        final Tier<IPATranscript> syllabifierTier = resetData.getData().syllabifierTier();
        final Syllabifier syllabifier = syllabifierForTier(ipaTier);

        final SyllabifyEdit edit = new SyllabifyEdit(editor.getSession(), editor.getEventManager(), ipaTier, syllabifier);

        // find component to use as source for edit
        final int recordIndex = editor.getSession().getRecordPosition(resetData.getData().record());
        final String tierName = getSyllabifierTierNameForIPATier(resetData.getData().ipaTier().getName());
        final TranscriptDocument.StartEnd syllabificationTierStartEnd =
                editor.getTranscriptDocument().getTierContentStartEnd(recordIndex, syllabifierTier.getName());
        if(syllabificationTierStartEnd.valid()) {
            final AttributeSet attrs = editor.getTranscriptDocument().getCharacterElement(syllabificationTierStartEnd.start()).getAttributes();
            final ComponentFactory componentFactory = TranscriptStyleConstants.getComponentFactory(attrs);
            if(componentFactory instanceof SyllabificationComponentFactory) {
                final JPanel parent = (JPanel)componentFactory.getComponent();
                if(parent.getComponentCount() > 0 && parent.getComponent(0) instanceof SyllabificationDisplay display) {
                    edit.setSource(display);
                }
            }
        }

        editor.getUndoSupport().postEdit(edit);
    }

    private String getSyllabifierTierNameForIPATier(String tierName) {
        if (tierName.equals(SystemTierType.IPATarget.getName())) {
            return SystemTierType.TargetSyllables.getName();
        } else if (tierName.equals(SystemTierType.IPAActual.getName())) {
            return SystemTierType.ActualSyllables.getName();
        } else {
            return tierName + " Syllables";
        }
    }

    private String getIPATierForSyllabifierTier(String tierName) {
        if (SystemTierType.TargetSyllables.getName().equals(tierName)) {
            return SystemTierType.IPATarget.getName();
        } else if (SystemTierType.ActualSyllables.getName().equals(tierName)) {
            return SystemTierType.IPAActual.getName();
        } else {
            return tierName.replace(" Syllables", "");
        }
    }

    private void addSyllabificationTiersForRecord(int transcriptElementIndex) {
        for (TierViewItem tvi : editor.getSession().getTierView()) {
            final TierDescription td = editor.getSession().getTier(tvi.getTierName());
            if (td != null && td.getDeclaredType() == IPATranscript.class) {
                final int paraEleIdx = editor.getTranscriptDocument().findParagraphElementIndexForTier(transcriptElementIndex, tvi.getTierName());
                if (paraEleIdx >= 0) {
                    final Element paraEle = editor.getTranscriptDocument().getDefaultRootElement().getElement(paraEleIdx);
                    final MutableAttributeSet attrs = new SimpleAttributeSet(paraEle.getElement(paraEle.getElementCount() - 1).getAttributes());
                    final TranscriptBatchBuilder builder = new TranscriptBatchBuilder(editor.getTranscriptDocument());
                    buildSyllabificationBatch(builder, attrs);
                    try {
                        editor.getTranscriptDocument().processBatchUpdates(paraEle.getEndOffset(), builder.getBatch());
                    } catch (BadLocationException e) {
                        LogUtil.warning(e);
                    }
                }
            }
        }
    }

    private void removeSyllabificationTiersForRecord(int elementIndex) {
        for (TierViewItem tvi : editor.getSession().getTierView()) {
            final TierDescription td = editor.getSession().getTier(tvi.getTierName());
            if (td != null && td.getDeclaredType() == IPATranscript.class) {
                final String syllabificationTierName = getSyllabifierTierNameForIPATier(td.getName());
                final int paraEleIdx =
                        editor.getTranscriptDocument().findParagraphElementIndexForTier(elementIndex, syllabificationTierName);
                if (paraEleIdx >= 0) {
                    final Element paraEle = editor.getTranscriptDocument().getDefaultRootElement().getElement(paraEleIdx);
                    try {
                        editor.getTranscriptDocument().setBypassDocumentFilter(true);
                        editor.getTranscriptDocument().remove(paraEle.getStartOffset(), paraEle.getEndOffset() - paraEle.getStartOffset());
                    } catch (BadLocationException e) {
                        LogUtil.warning(e);
                    } finally {
                        editor.getTranscriptDocument().setBypassDocumentFilter(false);
                    }
                }
            }
        }
    }

    public void onTranscriptLocationChanged(EditorEvent<TranscriptEditor.TranscriptLocationChangeData> event) {
        final TranscriptElementLocation oldLocation = event.data().oldLoc();
        final TranscriptElementLocation newLocation = event.data().newLoc();
        if(syllabificationEditMode) {
            String tierName = newLocation.tier();
            if (!SystemTierType.TargetSyllables.getName().equals(tierName) && !SystemTierType.ActualSyllables.getName().equals(tierName)) {
                setSyllabificationEditMode(false);
            }
        }

        // handle caret movements into syllabifier tiers
        if (newLocation.tier().equals(SystemTierType.TargetSyllables.getName()) || newLocation.tier().equals(SystemTierType.ActualSyllables.getName())
            || newLocation.tier().endsWith(" Syllables")) {
            final TranscriptDocument.StartEnd tierStartEnd = doc.getTierContentStartEnd(
                    editor.getSession().getTranscript().getRecordIndex(newLocation.transcriptElementIndex()), newLocation.tier());
            if (tierStartEnd.valid()) {
                final AttributeSet attrs = doc.getCharacterElement(tierStartEnd.start()).getAttributes();
                final ComponentFactory componentFactory = TranscriptStyleConstants.getComponentFactory(attrs);
                if (componentFactory instanceof SyllabificationComponentFactory) {
                    componentFactory.requestFocusAtOffset(newLocation.charPosition());
                }
            }
            return;
        }

        // single record view is already handled
        if(!isSyllabificationVisible() || editor.isSingleRecordView()) return;
        if(oldLocation.transcriptElementIndex() == newLocation.transcriptElementIndex()) return;

        // remove syllabification tiers from previous record (if any)
        editor.getTranscriptEditorCaret().freeze();
        if(oldLocation.transcriptElementIndex() >= 0) {
            final Transcript.Element prevElement = editor.getSession().getTranscript().getElementAt(oldLocation.transcriptElementIndex());
            if (prevElement.isRecord()) {
                removeSyllabificationTiersForRecord(oldLocation.transcriptElementIndex());
            }
        }

        if(newLocation.transcriptElementIndex() >= 0) {
            final Transcript.Element newElement = editor.getSession().getTranscript().getElementAt(newLocation.transcriptElementIndex());
            if (newElement.isRecord()) {
                addSyllabificationTiersForRecord(newLocation.transcriptElementIndex());
            }
        }

        // force update dot to new location without issuing a new location changed event by keeping caret frozen
        final int newCaretLoc = editor.sessionLocationToCharPos(newLocation);
        if(newCaretLoc >= 0) {
            editor.getTranscriptEditorCaret().setDot(newCaretLoc, true);
        }
        editor.getTranscriptEditorCaret().unfreeze();
    }

    public void onScEdit(EditorEvent<SyllabificationAlignmentEditorView.ScEditData> event) {
        if(event.source() instanceof SyllabificationDisplay display) {
            final IPATranscript clonedTranscript = (new IPATranscriptBuilder()).append(event.data().ipa().toString(true)).toIPATranscript();
            final Container parent = display.getParent();
            final List<IPATranscript> words = clonedTranscript.words();
            for(int i = 0 ; i < words.size() && i < parent.getComponentCount(); i++) {
                if(parent.getComponent(i) instanceof SyllabificationDisplay wordDisplay) {
                    wordDisplay.setTranscript(words.get(i));
                }
            }

            // reset syllabification event
            if(event.getData().get().eleIdx() < 0) {
                display.setFocusedPhone(0);
                display.requestFocus();
            }
        }
    }

    /**
     * Update on tier change
     *
     */
    public void onTierDataChanged(EditorEvent<EditorEventType.TierChangeData> event) {
        final Tier<?> tier = event.data().tier();
        if(tier.getDeclaredType().equals(IPATranscript.class) && !event.data().valueAdjusting()) {
            if(isSyllabificationVisible()) {
                final TranscriptDocument.StartEnd range = doc.getTierContentStartEnd(editor.getSession().getRecordPosition(event.data().record()), getSyllabifierTierNameForIPATier(tier.getName()));
                if(!range.valid()) return;
                editor.getTranscriptEditorCaret().freeze();
                try {
                    editor.getTranscriptDocument().setBypassDocumentFilter(true);
                    editor.getTranscriptDocument().remove(range.start(), range.length());
                    final SimpleAttributeSet tierAttrs = editor.getTranscriptDocument().getTranscriptStyleContext().getTierAttributes(tier);
                    tierAttrs.addAttributes(editor.getTranscriptDocument().getTranscriptStyleContext().getRecordAttributes(event.data().record()));
                    TranscriptBatchBuilder builder = new TranscriptBatchBuilder(editor.getTranscriptDocument());
                    if(isSyllabificationComponent()) {
                        tierAttrs.addAttributes(getSyllabificationDisplayAttributes());
                        builder.appendBatchString(((IPATranscript)tier.getValue()).toString(true), tierAttrs);
                    } else {
                        builder.appendAll(getFormattedSyllabification((IPATranscript)tier.getValue(), tierAttrs));
                    }
                    editor.getTranscriptDocument().processBatchUpdates(range.start(), builder.getBatch());
                } catch (BadLocationException e) {
                    LogUtil.warning(e);
                }
                editor.getTranscriptEditorCaret().unfreeze();
            }
        }
    }

    // region Getters and Setters

    /**
     * Gets an attribute set containing a reference to the syllabification component factory.
     * Adding the contents of this attribute set to the attributes of a syllabification tier will
     * cause it to appear as the {@link ca.phon.ui.ipa.SyllabificationDisplay} component instead of text
     *
     * @return an attribute set containing a reference to the syllabification component factory
     */
    public SimpleAttributeSet getSyllabificationDisplayAttributes() {
        final SimpleAttributeSet retVal = new SimpleAttributeSet();
        retVal.addAttribute(TranscriptStyleConstants.ATTR_KEY_COMPONENT_FACTORY, new SyllabificationComponentFactory(editor));
        return retVal;
    }

    public boolean isSyllabificationVisible() {
        return (boolean) doc.getDocumentPropertyOrDefault(
            SYLLABIFICATION_IS_VISIBLE,
            SYLLABIFICATION_IS_VISIBLE_DEFAULT
        );
    }

    public boolean isSyllabificationComponent() {
        return (boolean) doc.getDocumentPropertyOrDefault(
            SYLLABIFICATION_IS_COMPONENT,
            SYLLABIFICATION_IS_COMPONENT_DEFAULT
        );
    }

    private List<DefaultStyledDocument.ElementSpec> getFormattedSyllabification(IPATranscript ipaTranscript, AttributeSet additionalAttrs) {
        List<DefaultStyledDocument.ElementSpec> retVal = new ArrayList<>();

        SimpleAttributeSet attrs = new SimpleAttributeSet();
        if (additionalAttrs != null) attrs.addAttributes(additionalAttrs);
        attrs.addAttribute(TranscriptStyleConstants.ATTR_KEY_SYLLABIFICATION, true);

        Set<SyllableConstituentType> hiddenConstituent = new HashSet<>();
        hiddenConstituent.add(SyllableConstituentType.SYLLABLESTRESSMARKER);
        hiddenConstituent.add(SyllableConstituentType.UNKNOWN);
        hiddenConstituent.add(SyllableConstituentType.WORDBOUNDARYMARKER);

        for (IPAElement p : ipaTranscript) {
            attrs.addAttribute(TranscriptStyleConstants.ATTR_KEY_PHONE, p);
            attrs.removeAttribute(StyleConstants.Foreground);
            attrs.addAttribute(TranscriptStyleConstants.ATTR_KEY_NOT_TRAVERSABLE_SYLLABIFICATION, true);
            attrs.addAttribute(TranscriptStyleConstants.ATTR_KEY_NOT_EDITABLE, true);
            if (p.getScType().equals(SyllableConstituentType.UNKNOWN)) {
                StyleConstants.setForeground(attrs, UIManager.getColor(TranscriptEditorUIProps.IPA_PAUSE));
            }
            retVal.add(TranscriptBatchBuilder.getBatchString(p.toString(), attrs));
            final SyllabificationInfo sInfo = p.getExtension(SyllabificationInfo.class);
            if (hiddenConstituent.contains(sInfo.getConstituentType())) continue;
            retVal.add(TranscriptBatchBuilder.getBatchString(":", attrs));
            attrs.removeAttribute(TranscriptStyleConstants.ATTR_KEY_NOT_TRAVERSABLE_SYLLABIFICATION);
            attrs.removeAttribute(TranscriptStyleConstants.ATTR_KEY_NOT_EDITABLE);
            if(sInfo.getConstituentType() == SyllableConstituentType.NUCLEUS && sInfo.isDiphthongMember()) {
                StyleConstants.setForeground(attrs, Color.RED);
                retVal.add(TranscriptBatchBuilder.getBatchString("D", attrs));
            }
            else {
                StyleConstants.setForeground(attrs, sInfo.getConstituentType().getColor());
                retVal.add(TranscriptBatchBuilder.getBatchString(String.valueOf(sInfo.getConstituentType().getIdChar()), attrs));
            }
        }
        attrs.removeAttribute(StyleConstants.Foreground);

        return retVal;
    }

    /**
     * Sets whether the editor is in "syllabification edit" mode,
     * and modifies the navigation and document filters accordingly
     *
     * @param enabled whether syllabification edit mode will be enabled
     * */
    private void setSyllabificationEditMode(boolean enabled) {
        syllabificationEditMode = enabled;

        if (enabled) {
            editor.setNavigationFilter(new SyllabificationEditNavigationFilter(editor));
            editor.addNotTraversableAttribute(TranscriptStyleConstants.ATTR_KEY_NOT_TRAVERSABLE_SYLLABIFICATION);
            doc.setDocumentFilter(new SyllabificationEditDocumentFilter(doc));
            doc.removeNotEditableAttribute(TranscriptStyleConstants.ATTR_KEY_SYLLABIFICATION);
        }
        else {
            editor.setNavigationFilter(new TranscriptNavigationFilter(editor));
            editor.removeNotTraversableAttribute(TranscriptStyleConstants.ATTR_KEY_NOT_TRAVERSABLE_SYLLABIFICATION);
            doc.setDocumentFilter(new TranscriptDocument.TranscriptDocumentFilter(doc));
            doc.addNotEditableAttribute(TranscriptStyleConstants.ATTR_KEY_SYLLABIFICATION);
        }

        editor.setCaretPosition(editor.getNextValidIndex(editor.getCaretPosition()-1, false));
    }

    // endregion Getters and Setters

    /**
     * The {@link NavigationFilter} that gets used in syllabification edit mode
     * */
    private static class SyllabificationEditNavigationFilter extends NavigationFilter {
        private final TranscriptEditor editor;

        private SyllabificationEditNavigationFilter(TranscriptEditor editor) {
            this.editor = editor;
        }

        @Override
        public void setDot(NavigationFilter.FilterBypass fb, int dot, Position.Bias bias) {
            TranscriptDocument doc = editor.getTranscriptDocument();
            if (doc.getLength() == 0) {
                fb.setDot(dot, bias);
            }

            Element elem = doc.getCharacterElement(dot);
            AttributeSet attrs = elem.getAttributes();
            if (TranscriptStyleConstants.isNotTraversable(attrs)) return;

            AttributeSet prevAttrs = doc.getCharacterElement(fb.getCaret().getDot()).getAttributes();
            AttributeSet nextAttrs = doc.getCharacterElement(dot).getAttributes();

            String prevElemType = TranscriptStyleConstants.getElementType(prevAttrs);
            String nextElemType = TranscriptStyleConstants.getElementType(nextAttrs);
            Tier<?> nextTier = TranscriptStyleConstants.getTier(nextAttrs);

            if (prevElemType != null) {
                try {
                    switch (prevElemType) {
                        case TranscriptStyleConstants.ATTR_KEY_RECORD -> {
                            final Record record = TranscriptStyleConstants.getRecord(prevAttrs);
                            if (record == null) break;
                            int recordIndex = doc.getSession().getRecordPosition(record);
                            Tier<?> prevTier = TranscriptStyleConstants.getTier(prevAttrs);
                            if (prevTier == null || prevTier.getDeclaredType().equals(PhoneAlignment.class)) break;
                            if (nextElemType != null && nextElemType.equals(TranscriptStyleConstants.ATTR_KEY_RECORD)) {
                                if (nextTier != null && nextTier == prevTier) break;
                            }
                            final TranscriptDocument.StartEnd se = doc.getTierContentStartEnd(recordIndex, prevTier.getName());
                            int start = se.start();
                            int end = se.end();
                            String newValue = doc.getText(start, end - start);
//                            editor.setInternalEdit(true);
                            editor.changeTierData(record, prevTier, newValue);
                        }
                    }
                }
                catch (BadLocationException e) {
                    LogUtil.severe(e);
                }
            }

            if (doc.getLength() == dot) return;

            int prevCaretPos = editor.getCaretPosition();

            if (nextTier != null && nextAttrs.getAttribute("syllabification") != null) {
                final Record nextRecord = TranscriptStyleConstants.getRecord(nextAttrs);
                final int recordIndex = doc.getSession().getRecordPosition(nextRecord);
                int tierEnd = doc.getTierEnd(recordIndex, nextTier.getName());
                fb.setDot(dot, Position.Bias.Forward);
                if (dot != tierEnd) {
                    fb.moveDot(editor.getCaretPosition() + 1, Position.Bias.Forward);
                }
            }
            else {
                fb.setDot(dot, bias);
            }

            TranscriptEditor.TranscriptLocationChangeData transcriptLocationChangeData = new TranscriptEditor.TranscriptLocationChangeData(
                editor.charPosToSessionLocation(prevCaretPos),
                editor.charPosToSessionLocation(dot)
            );

            SwingUtilities.invokeLater(() -> {
                final EditorEvent<TranscriptEditor.TranscriptLocationChangeData> e = new EditorEvent<>(
                    TranscriptEditor.transcriptLocationChanged,
                    editor,
                        transcriptLocationChangeData
                );
                editor.getEventManager().queueEvent(e);
            });
        }
        @Override
        public void moveDot(NavigationFilter.FilterBypass fb, int dot, Position.Bias bias) {
            System.out.println("moveDot");
        }
    }

    /**
     * The {@link DocumentFilter} that gets used in syllabification edit mode
     * */
    private static class SyllabificationEditDocumentFilter extends DocumentFilter {
        private final TranscriptDocument doc;
        private final Set<Character> syllabificationChars;

        private SyllabificationEditDocumentFilter(TranscriptDocument doc) {
            this.doc = doc;

            syllabificationChars = new HashSet<>();
            for (SyllableConstituentType type : SyllableConstituentType.values()) {
                syllabificationChars.add(type.getIdChar());
            }
            syllabificationChars.add('D');
            syllabificationChars.add('H');
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet _attrs) throws BadLocationException {

            // For some reason attrs gets the attributes from the previous character, so this fixes that
            SimpleAttributeSet attrs = new SimpleAttributeSet();
            attrs.addAttributes(doc.getCharacterElement(offset).getAttributes());

            // Labels and stuff
            if (doc.containsNotEditableAttribute(attrs)) return;

            // Locked tiers
            Tier<?> tier = TranscriptStyleConstants.getTier(attrs);
            if (tier != null) {
                String tierName = tier.getName();
                var tierViewItem = doc
                    .getSession()
                    .getTierView()
                    .stream()
                    .filter(item -> item.getTierName().equals(tierName))
                    .findFirst();
                if (tierViewItem.isPresent() && tierViewItem.get().isTierLocked()) {
                    return;
                }

                // Syllabification tiers
                if (attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_SYLLABIFICATION) != null) {
                    if (text == null || text.isEmpty()) return;
                    final Record record = TranscriptStyleConstants.getRecord(attrs);
                    final int recordIndex = doc.getSession().getRecordPosition(record);
                    if(recordIndex < 0) return;
                    final String textUpper = text.toUpperCase();
                    char c = textUpper.charAt(0);
                    if (syllabificationChars.contains(c)) {
                        SyllableConstituentType type = Arrays
                                .stream(SyllableConstituentType.values())
                                .filter(item -> item.getIdChar() == textUpper.charAt(0))
                                .findFirst()
                                .orElse(null);
                        if (type == null) {
                            List<IPAElement> syllabificationTranscript = ((Tier<IPATranscript>) tier).getValue().toList();
                            IPAElement phone = (IPAElement) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_PHONE);

                            IPAElement otherNucleus = null;
                            for (int i = 0; i < syllabificationTranscript.size(); i++) {
                                IPAElement p = syllabificationTranscript.get(i);
                                if (!p.equals(phone)) continue;
                                if (!p.getScType().equals(SyllableConstituentType.NUCLEUS)) return;
                                if (i < syllabificationTranscript.size() - 1) {
                                    IPAElement nextP = syllabificationTranscript.get(i + 1);
                                    if (nextP.getScType().equals(SyllableConstituentType.NUCLEUS)) {
                                        otherNucleus = nextP;
                                        break;
                                    }
                                }
                                if (i > 0) {
                                    IPAElement prevP = syllabificationTranscript.get(i - 1);
                                    if (prevP.getScType().equals(SyllableConstituentType.NUCLEUS)) {
                                        otherNucleus = prevP;
                                        break;
                                    }
                                }
                            }

                            if (otherNucleus == null) return;

                            final SyllabificationInfo sInfo = phone.getExtension(SyllabificationInfo.class);
                            final SyllabificationInfo otherSInfo = otherNucleus.getExtension(SyllabificationInfo.class);

                            if (c == 'D' && !sInfo.isDiphthongMember()) {
                                sInfo.setDiphthongMember(true);
                                otherSInfo.setDiphthongMember(true);

                                final TranscriptDocument.StartEnd se = doc.getTierContentStartEnd(recordIndex, tier.getName());
                                int start = se.start();
                                int end = se.end();

                                for (int i = start; i < end; i++) {
                                    var charAttrs = doc.getCharacterElement(i).getAttributes();
                                    IPAElement charPhone = (IPAElement) charAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_PHONE);
                                    if (!doc.containsNotEditableAttribute(charAttrs) && charPhone != null && (charPhone.equals(phone) || charPhone.equals(otherNucleus))) {
                                        SimpleAttributeSet newCharAttrs = new SimpleAttributeSet();
                                        newCharAttrs.addAttributes(charAttrs);
                                        StyleConstants.setForeground(newCharAttrs, Color.RED);
                                        super.replace(fb, i, 1, "D", attrs);
                                    }
                                }
                            }
                            else if (c == 'H' && sInfo.isDiphthongMember()) {
                                sInfo.setDiphthongMember(false);
                                otherSInfo.setDiphthongMember(false);

                                final TranscriptDocument.StartEnd se = doc.getTierContentStartEnd(recordIndex, tier.getName());
                                int start = se.start();
                                int end = se.end();

                                for (int i = start; i < end; i++) {
                                    var charAttrs = doc.getCharacterElement(i).getAttributes();
                                    IPAElement charPhone = (IPAElement) charAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_PHONE);
                                    if (!doc.containsNotEditableAttribute(charAttrs) && charPhone != null && (charPhone.equals(phone) || charPhone.equals(otherNucleus))) {
                                        SimpleAttributeSet newCharAttrs = new SimpleAttributeSet();
                                        newCharAttrs.addAttributes(charAttrs);
                                        StyleConstants.setForeground(newCharAttrs, SyllableConstituentType.NUCLEUS.getColor());
                                        super.replace(fb, i, 1, "N", attrs);
                                    }
                                }
                            }
                            return;
                        }
                        else {
                            StyleConstants.setForeground(attrs, type.getColor());
                            text = textUpper;
                        }
                    }
                    else return;
                }
            }
            super.replace(fb, offset, length, text, attrs);
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {

            if (!doc.isBypassDocumentFilter()) {
                var attrs = doc.getCharacterElement(offset).getAttributes();
                if (doc.containsNotEditableAttribute(attrs)) return;
                if (attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_SYLLABIFICATION) != null) return;
            }

            doc.setBypassDocumentFilter(false);
            super.remove(fb, offset, length);
        }
    }
}
