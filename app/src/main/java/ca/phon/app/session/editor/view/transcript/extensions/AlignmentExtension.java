package ca.phon.app.session.editor.view.transcript.extensions;

import ca.phon.app.log.LogUtil;
import ca.phon.app.session.editor.EditorEvent;
import ca.phon.app.session.editor.EditorEventManager;
import ca.phon.app.session.editor.EditorEventType;
import ca.phon.app.session.editor.view.transcript.*;
import ca.phon.session.*;
import ca.phon.session.Record;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
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
                Tier<?> tier = (Tier<?>) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);
                TierViewItem alignmentParent = getAlignmentParent();

                if (tier != null && isAlignmentVisible() && alignmentParent != null && tier.getName().equals(alignmentParent.getTierName())) {
                    Record record = TranscriptStyleConstants.getRecord(attrs);
                    final TranscriptBatchBuilder batchBuilder = new TranscriptBatchBuilder(doc);
//                    batchBuilder.appendEOL();
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
                    return batchBuilder.getBatch();
                }

                return new ArrayList<>();
            }
        });

        EditorEventManager eventManager = editor.getEventManager();
        eventManager.registerActionForEvent(EditorEventType.TierChange, this::onTierDataChanged, EditorEventManager.RunOn.AWTEventDispatchThread);

        doc.addDocumentPropertyChangeListener(ALIGNMENT_IS_VISIBLE, evt -> {
            if (isAlignmentVisible()) {
                doc.putDocumentProperty(ALIGNMENT_PARENT, calculateAlignmentParent());
            }
            else {
                doc.putDocumentProperty(ALIGNMENT_PARENT, ALIGNMENT_PARENT_DEFAULT);
            }
            doc.reload();
        });
        doc.addDocumentPropertyChangeListener(ALIGNMENT_IS_COMPONENT, evt -> {
            if (isAlignmentVisible()) {
                doc.reload();
            }
        });

        editor.getEventManager().registerActionForEvent(EditorEventType.TierViewChanged, (event) -> {
            doc.putDocumentProperty(ALIGNMENT_PARENT, calculateAlignmentParent());
        }, EditorEventManager.RunOn.AWTEventDispatchThread);
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
