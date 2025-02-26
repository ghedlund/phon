package ca.phon.app.session.editor.view.transcript.extensions;

import ca.phon.app.log.LogUtil;
import ca.phon.app.session.editor.EditorEvent;
import ca.phon.app.session.editor.EditorEventManager;
import ca.phon.app.session.editor.EditorEventType;
import ca.phon.app.session.editor.view.transcript.*;
import ca.phon.ipa.IPATranscript;
import ca.phon.session.*;
import ca.phon.session.Record;
import ca.phon.session.position.TranscriptElementLocation;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * An extension that provides phone alignment support to the {@link TranscriptEditor}
 * */
public class AlignmentExtension implements TranscriptEditorExtension {
    private TranscriptEditor editor;
    private TranscriptDocument doc;

    /* Document property stuff */

    public final static String ALIGNMENT_IS_VISIBLE = "isAlignmentVisible";
    public final static boolean ALIGNMENT_IS_VISIBLE_DEFAULT = false;

    public final static String ALIGNMENT_IS_COMPONENT = "isAlignmentComponent";
    public final static boolean ALIGNMENT_IS_COMPONENT_DEFAULT = true;

    public final static String ALIGNMENT_PARENT = "alignmentParent";
    public final static TierViewItem ALIGNMENT_PARENT_DEFAULT = null;

    @Override
    public void install(TranscriptEditor editor) {
        this.editor = editor;
        doc = editor.getTranscriptDocument();

        // TODO: make this stuff happen in the right order

        doc.addInsertionHook(new DefaultInsertionHook() {
            @Override
            public List<DefaultStyledDocument.ElementSpec> endTier(MutableAttributeSet attrs) {
                TranscriptBatchBuilder builder = new TranscriptBatchBuilder(doc);
                if(!isAlignmentVisible() || !doc.getSingleRecordView()) return builder.getBatch();
                buildAlignmentBatch(builder, attrs);
                return builder.getBatch();
            }
        });


        doc.addDocumentPropertyChangeListener(ALIGNMENT_IS_VISIBLE, this::alignmentVisiblePropertyChangeHandler);
        doc.addDocumentPropertyChangeListener(ALIGNMENT_IS_COMPONENT, this::alignmentComponentPropertyChangeHandler);

        editor.getEventManager().registerActionForEvent(EditorEventType.TierViewChanged, (event) -> {
            doc.putDocumentProperty(ALIGNMENT_PARENT, calculateAlignmentParent());
        }, EditorEventManager.RunOn.AWTEventDispatchThread);
        editor.getEventManager().registerActionForEvent(EditorEventType.TierChange, this::onTierDataChanged, EditorEventManager.RunOn.AWTEventDispatchThread);
        editor.getEventManager().registerActionForEvent(TranscriptEditor.transcriptLocationChanged, this::onTranscriptLocationChanged, EditorEventManager.RunOn.AWTEventDispatchThread);
    }

    private void alignmentVisiblePropertyChangeHandler(PropertyChangeEvent evt) {
        if (isAlignmentVisible()) {
            doc.putDocumentProperty(ALIGNMENT_PARENT, calculateAlignmentParent());
        }
        else {
            doc.putDocumentProperty(ALIGNMENT_PARENT, ALIGNMENT_PARENT_DEFAULT);
        }
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
                addAlignmentTiersForRecord(transcriptElementIndex);
            } else {
                removeAlignmentTiersForRecord(transcriptElementIndex);
            }
        }
    }

    private void alignmentComponentPropertyChangeHandler(PropertyChangeEvent evt) {
        if (isAlignmentVisible()) {
            doc.reload();
        }
    }

    private void buildAlignmentBatch(TranscriptBatchBuilder batchBuilder, AttributeSet attrs) {
        Tier<?> tier = (Tier<?>) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);
        TierViewItem alignmentParent = getAlignmentParent();

        if (tier != null && isAlignmentVisible() && alignmentParent != null && tier.getName().equals(alignmentParent.getTierName())) {
            Record record = TranscriptStyleConstants.getRecord(attrs);
            final SimpleAttributeSet tierAttrs = new SimpleAttributeSet();
            TranscriptStyleConstants.setElementType(tierAttrs, TranscriptStyleConstants.ELEMENT_TYPE_RECORD);
            TranscriptStyleConstants.setRecord(tierAttrs, record);
            TranscriptStyleConstants.setParentTier(tierAttrs, tier);
            TranscriptStyleConstants.setTier(tierAttrs, record.getPhoneAlignmentTier());
            batchBuilder.appendTierLabel(editor.getSession(), record, record.getPhoneAlignmentTier(), record.getPhoneAlignmentTier().getName(), null, doc.isChatTierNamesShown(), tierAttrs);
            batchBuilder.appendAll(getFormattedAlignment(record, record.getPhoneAlignmentTier(), editor.getDataModel().getTranscriber(), tierAttrs));
            final SimpleAttributeSet finalAttrs = new SimpleAttributeSet(batchBuilder.getTrailingAttributes());
            TranscriptStyleConstants.setTier(finalAttrs, record.getPhoneAlignmentTier());
            TranscriptStyleConstants.setNotEditable(finalAttrs, true);
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
            batchBuilder.appendEOL(finalAttrs);
        }
    }

    /**
     * Runs when tier data gets changed.
     * If the alignment tier is the one that gets changed, update it in the doc.
     *
     * @param editorEvent the event that changed the tier data
     * */
    private void onTierDataChanged(EditorEvent<EditorEventType.TierChangeData> editorEvent) {
        if(editorEvent.data().valueAdjusting() || editorEvent.data().record() == null) return;
        Tier<?> tier = editorEvent.data().record().getPhoneAlignmentTier();
        if (!tier.getDeclaredType().equals(PhoneAlignment.class) || !isAlignmentVisible()) return;

        final int recordIndex = editor.getSession().getTranscript().getRecordPosition(editorEvent.data().record());
        final TranscriptDocument.StartEnd alignmentTierContentRange = doc.getTierContentStartEnd(recordIndex, tier.getName());
        if(!alignmentTierContentRange.valid()) return;

        try {
            editor.getTranscriptEditorCaret().freeze();
            doc.setBypassDocumentFilter(true);
            doc.remove(alignmentTierContentRange.start(), alignmentTierContentRange.length());
            doc.processBatchUpdates(alignmentTierContentRange.start(), getFormattedAlignment(editorEvent.data().record(), (Tier<PhoneAlignment>) tier, editor.getDataModel().getTranscriber(), new SimpleAttributeSet()));
        } catch (BadLocationException e) {
            LogUtil.severe(e);
        } finally {
            doc.setBypassDocumentFilter(false);
            editor.getTranscriptEditorCaret().unfreeze();
        }
    }

    /**
     * Gets a list of {@link javax.swing.text.DefaultStyledDocument.ElementSpec} that contains the data for the
     * properly formatted alignment tier content
     *
     * @param record a reference to the record containing the alignment tier
     * @param alignmentTier the alignment tier to format
     * @param attrs the attributes to apply to the tier
     * @return the list of {@link javax.swing.text.DefaultStyledDocument.ElementSpec} data
     */
    public List<DefaultStyledDocument.ElementSpec> getFormattedAlignment(Record record, Tier<PhoneAlignment> alignmentTier, Transcriber transcriber, AttributeSet attrs) {
        final TranscriptBatchBuilder batchBuilder = new TranscriptBatchBuilder(doc);
        // Get the alignment tier
        TranscriptStyleContext transcriptStyleContext = doc.getTranscriptStyleContext();

        // Set up the tier attributes for the dummy tier
        var tierAttrs = new SimpleAttributeSet(attrs);
        tierAttrs.addAttributes(transcriptStyleContext.getTierAttributes(alignmentTier));
        tierAttrs.addAttributes(transcriptStyleContext.getRecordAttributes(record));
        TranscriptStyleConstants.setNotEditable(tierAttrs, true);

        // Get the string version of the alignment
        // Add component factory if needed
        if (isAlignmentComponent()) {
            tierAttrs.addAttributes(transcriptStyleContext.getAlignmentAttributes());
        }
        batchBuilder.appendTierContent(record, alignmentTier, editor.getDataModel().getTranscriber(), tierAttrs);

        return batchBuilder.getBatch();
    }

    private void addAlignmentTiersForRecord(int transcriptElementIndex) {
        final TierViewItem alignmentParent = getAlignmentParent();
        if (alignmentParent == null) return;

        final int paraEleIdx = editor.getTranscriptDocument().findParagraphElementIndexForTier(transcriptElementIndex, alignmentParent.getTierName());
        if (paraEleIdx >= 0) {
            final Element paraEle = editor.getTranscriptDocument().getDefaultRootElement().getElement(paraEleIdx);
            final MutableAttributeSet attrs = new SimpleAttributeSet(paraEle.getElement(paraEle.getElementCount() - 1).getAttributes());
            final TranscriptBatchBuilder builder = new TranscriptBatchBuilder(editor.getTranscriptDocument());
            buildAlignmentBatch(builder, attrs);
            try {
                editor.getTranscriptDocument().processBatchUpdates(paraEle.getEndOffset(), builder.getBatch());
            } catch (BadLocationException e) {
                LogUtil.warning(e);
            }
        }
    }

    private void removeAlignmentTiersForRecord(int transcriptElementIndex) {
        final int paraEleIdx = editor.getTranscriptDocument().findParagraphElementIndexForTier(transcriptElementIndex, SystemTierType.PhoneAlignment.getName());
        if (paraEleIdx >= 0) {
            final Element paraEle = editor.getTranscriptDocument().getDefaultRootElement().getElement(paraEleIdx);
            editor.getTranscriptDocument().setBypassDocumentFilter(true);
            editor.getTranscriptEditorCaret().freeze();
            try {
                editor.getTranscriptDocument().remove(paraEle.getStartOffset(), paraEle.getEndOffset() - paraEle.getStartOffset());
            } catch (BadLocationException e) {
                LogUtil.warning(e);
            } finally {
                editor.getTranscriptDocument().setBypassDocumentFilter(false);
                editor.getTranscriptEditorCaret().unfreeze();
            }
        }
    }

    public void onTranscriptLocationChanged(EditorEvent<TranscriptEditor.TranscriptLocationChangeData> event) {
        final TranscriptElementLocation oldLocation = event.data().oldLoc();
        final TranscriptElementLocation newLocation = event.data().newLoc();

        // single record view is already handled
        if(!isAlignmentVisible() || editor.isSingleRecordView()) return;
        if(oldLocation.transcriptElementIndex() == newLocation.transcriptElementIndex()) return;

        // remove syllabification tiers from previous record (if any)
        editor.getTranscriptEditorCaret().freeze();
        if(oldLocation.transcriptElementIndex() >= 0) {
            final Transcript.Element prevElement = editor.getSession().getTranscript().getElementAt(oldLocation.transcriptElementIndex());
            if (prevElement.isRecord()) {
                removeAlignmentTiersForRecord(oldLocation.transcriptElementIndex());
            }
        }

        if(newLocation.transcriptElementIndex() >= 0) {
            final Transcript.Element newElement = editor.getSession().getTranscript().getElementAt(newLocation.transcriptElementIndex());
            if (newElement.isRecord()) {
                addAlignmentTiersForRecord(newLocation.transcriptElementIndex());
            }
        }

        // force update dot to new location without issuing a new location changed event by keeping caret frozen
        final int newCaretLoc = editor.sessionLocationToCharPos(newLocation);
        if(newCaretLoc >= 0) {
            editor.getTranscriptEditorCaret().setDot(newCaretLoc, true);
        }
        editor.getTranscriptEditorCaret().unfreeze();
    }

    /**
     * Calculates which tier the alignment tier line should be parented to
     *
     * @return the {@link TierViewItem} associated with the calculated parent tier
     * */
    public TierViewItem calculateAlignmentParent() {
        List<TierViewItem> visibleTierView = editor.getSession().getTierView().stream().filter(TierViewItem::isVisible).toList();

        var retVal = visibleTierView.stream().filter(item -> item.getTierName().equals("IPA Actual")).findFirst();
        if (retVal.isPresent()) return retVal.get();

        retVal = visibleTierView.stream().filter(item -> item.getTierName().equals("IPA Target")).findFirst();
        return retVal.orElseGet(() -> visibleTierView.get(visibleTierView.size() - 1));
    }

    // region Getters and Setters

    public boolean isAlignmentVisible() {
        return (boolean) doc.getDocumentPropertyOrDefault(ALIGNMENT_IS_VISIBLE, ALIGNMENT_IS_VISIBLE_DEFAULT);
    }

    public boolean isAlignmentComponent() {
        return (boolean) doc.getDocumentPropertyOrDefault(ALIGNMENT_IS_COMPONENT, ALIGNMENT_IS_COMPONENT_DEFAULT);
    }

    public TierViewItem getAlignmentParent() {
        return (TierViewItem) doc.getDocumentPropertyOrDefault(ALIGNMENT_PARENT, ALIGNMENT_PARENT_DEFAULT);
    }

    // endregion Getters and Setters
}
