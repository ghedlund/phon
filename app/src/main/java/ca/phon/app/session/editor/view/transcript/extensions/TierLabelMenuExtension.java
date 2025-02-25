package ca.phon.app.session.editor.view.transcript.extensions;

import ca.phon.app.session.editor.undo.ChangeCommentTypeEdit;
import ca.phon.app.session.editor.undo.ChangeGemTypeEdit;
import ca.phon.app.session.editor.undo.ChangeSpeakerEdit;
import ca.phon.app.session.editor.undo.DeleteTranscriptElementEdit;
import ca.phon.app.session.editor.view.tierManagement.TierMenuBuilder;
import ca.phon.app.session.editor.view.transcript.*;
import ca.phon.plugin.PluginManager;
import ca.phon.session.*;
import ca.phon.session.Record;
import ca.phon.ui.action.PhonUIAction;
import ca.phon.ui.menu.MenuBuilder;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.MutableAttributeSet;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Adds label mouse handlers to default transcript elements: header tiers, record tiers, comments, gems
 */
public class TierLabelMenuExtension implements TranscriptEditorExtension {

    private WeakReference<TranscriptEditor> editorRef;

    @Override
    public void install(TranscriptEditor editor) {
        editorRef = new WeakReference<>(editor);
        editor.getTranscriptDocument().addInsertionHook(new TierLabelInsertionHook());
    }

    private TranscriptEditor getEditor() {
        return editorRef.get();
    }

    private Session getSession() {
        return getEditor().getSession();
    }

    private void tierLabelClickHandler(MouseEvent me, AttributeSet attributeSet) {
        Point2D point = new Point2D.Double(me.getX(), me.getY());
        Tier<?> tier = (Tier<?>) attributeSet.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);
        Record record = (Record) attributeSet.getAttribute(TranscriptStyleConstants.ATTR_KEY_RECORD);
        onClickTierLabel(point, tier, record);
    }

    /**
     * Runs when the user clicks on a tier label
     *
     * @param point  the point where the user clicked
     * @param tier   the tier that the label belongs to
     * @param record the record containing the tier
     */
    private void onClickTierLabel(Point2D point, Tier<?> tier, Record record) {
        // Build a new popup menu
        JPopupMenu menu = new JPopupMenu();
        MenuBuilder builder = new MenuBuilder(menu);

        if(SystemTierType.Orthography.getName().equals(tier.getName())) {
            final JMenu speakerMenu = builder.addMenu(".", "Change speaker");
            appendChangeSpeakerMenu(record, new MenuBuilder(speakerMenu));
            builder.addSeparator(".", "speaker");
        }

        final TierDescription td = getSession().getTier(tier.getName());
        final TierViewItem tvi = getSession().getTierView().stream().filter(tv -> tv.getTierName().equals(tier.getName())).findFirst().orElse(null);
        if(tvi != null)
            TierMenuBuilder.setupTierMenu(getEditor().getDataModel(), getEditor().getEventManager(), getEditor().getUndoSupport(), td, tvi, builder);

        var extPts = PluginManager.getInstance().getExtensionPoints(TierLabelMenuHandler.class);

        for (var extPt : extPts) {
            var menuHandler = extPt.getFactory().createObject();
            menuHandler.addMenuItems(builder, getSession(), getEditor().getEventManager(), getEditor().getUndoSupport(), tier, record);
        }

        // Show it where the user clicked
        menu.show(getEditor(), (int) point.getX(), (int) point.getY());
    }

    private void commentLabelClickHandler(MouseEvent me, AttributeSet attributeSet) {
        Point2D point = new Point2D.Double(me.getX(), me.getY());
        Comment comment = TranscriptStyleConstants.getComment(attributeSet);
        onClickCommentLabel(point, comment);
    }

    /**
     * Runs when the user click on a comment label
     *
     * @param point   the point where the user clicks
     * @param comment the comment that the label belongs to
     */
    private void onClickCommentLabel(Point2D point, Comment comment) {
        // Build a new popup menu
        JPopupMenu menu = new JPopupMenu();

        JMenu changeTypeMenu = new JMenu("Change type");
        ButtonGroup changeTypeButtonGroup = new ButtonGroup();
        for (CommentType type : CommentType.values()) {
            JRadioButtonMenuItem changeTypeItem = new JRadioButtonMenuItem();
            changeTypeButtonGroup.add(changeTypeItem);
            if (comment.getType().equals(type)) {
                changeTypeButtonGroup.setSelected(changeTypeItem.getModel(), true);
            }
            PhonUIAction<Void> changeTypeAct = PhonUIAction.runnable(() -> {
                ChangeCommentTypeEdit edit = new ChangeCommentTypeEdit(getSession(), getEditor().getEventManager(), comment, type);
                getEditor().getUndoSupport().postEdit(edit);
            });
            changeTypeAct.putValue(PhonUIAction.NAME, type.getLabel());
            changeTypeItem.setAction(changeTypeAct);
            changeTypeMenu.add(changeTypeItem);
        }

        menu.add(changeTypeMenu);


        JMenuItem deleteThis = new JMenuItem();
        PhonUIAction<Void> deleteThisAct = PhonUIAction.runnable(() -> deleteTranscriptElement(new Transcript.Element(comment)));
        deleteThisAct.putValue(PhonUIAction.NAME, "Delete this comment");
        deleteThis.setAction(deleteThisAct);
        menu.add(deleteThis);

        // Show it where the user clicked
        menu.show(getEditor(), (int) point.getX(), (int) point.getY());
    }

    private void gemLabelClickHandler(MouseEvent me, AttributeSet attributeSet) {
        Point2D point = new Point2D.Double(me.getX(), me.getY());
        Gem gem = TranscriptStyleConstants.getGEM(attributeSet);
        onClickGemLabel(point, gem);
    }

    /**
     * Runs when the user click on a gem label
     *
     * @param point the point where the user clicks
     * @param gem   the gem that the label belongs to
     */
    private void onClickGemLabel(Point2D point, Gem gem) {
        // Build a new popup menu
        JPopupMenu menu = new JPopupMenu();


        JMenu changeTypeMenu = new JMenu("Change type");

        ButtonGroup changeTypeButtonGroup = new ButtonGroup();
        for (GemType type : GemType.values()) {
            JRadioButtonMenuItem changeTypeItem = new JRadioButtonMenuItem();
            changeTypeButtonGroup.add(changeTypeItem);
            if (gem.getType().equals(type)) {
                changeTypeButtonGroup.setSelected(changeTypeItem.getModel(), true);
            }
            PhonUIAction<Void> changeTypeAct = PhonUIAction.runnable(() -> {
                ChangeGemTypeEdit edit = new ChangeGemTypeEdit(getSession(), getEditor().getEventManager(), gem, type);
                getEditor().getUndoSupport().postEdit(edit);
            });
            changeTypeAct.putValue(PhonUIAction.NAME, type.name());
            changeTypeItem.setAction(changeTypeAct);
            changeTypeMenu.add(changeTypeItem);
        }

        menu.add(changeTypeMenu);


        JMenuItem deleteThis = new JMenuItem();
        PhonUIAction<Void> deleteThisAct = PhonUIAction.runnable(() -> deleteTranscriptElement(new Transcript.Element(gem)));
        deleteThisAct.putValue(PhonUIAction.NAME, "Delete this gem");
        deleteThis.setAction(deleteThisAct);
        menu.add(deleteThis);


        // Show it where the user clicked
        menu.show(getEditor(), (int) point.getX(), (int) point.getY());
    }

    /**
     * Append change speaker items to given menu builder
     *
     * @param menuBuilder
     */
    private void appendChangeSpeakerMenu(Record record, MenuBuilder menuBuilder) {

        ButtonGroup buttonGroup = new ButtonGroup();

        var participants = getSession().getParticipants();
        for (Participant participant : participants) {
            JRadioButtonMenuItem participantItem = new JRadioButtonMenuItem();
            PhonUIAction<Void> participantAction = PhonUIAction.runnable(() -> {
                ChangeSpeakerEdit edit = new ChangeSpeakerEdit(getSession(), getEditor().getEventManager(), record, participant);
                getEditor().getUndoSupport().postEdit(edit);
            });
            participantAction.putValue(PhonUIAction.NAME, participant.toString());
            participantItem.setAction(participantAction);
            buttonGroup.add(participantItem);
            menuBuilder.addItem(".", participantItem);
        }

        menuBuilder.addSeparator(".", "");

        JRadioButtonMenuItem unknownParticipantItem = new JRadioButtonMenuItem();
        PhonUIAction<Void> unknownParticipantAction = PhonUIAction.runnable(() -> {
            ChangeSpeakerEdit edit = new ChangeSpeakerEdit(getSession(), getEditor().getEventManager(), record, Participant.UNKNOWN);
            getEditor().getUndoSupport().postEdit(edit);
        });
        unknownParticipantAction.putValue(PhonUIAction.NAME, Participant.UNKNOWN.toString());
        unknownParticipantItem.setAction(unknownParticipantAction);
        buttonGroup.add(unknownParticipantItem);
        menuBuilder.addItem(".", unknownParticipantItem);

    }
    /**
     * Deletes a given transcript element from the transcript
     *
     * @param elem the element to be deleted
     */
    private void deleteTranscriptElement(Transcript.Element elem) {
        DeleteTranscriptElementEdit edit = new DeleteTranscriptElementEdit(getSession(), getEditor().getEventManager(), elem, getSession().getTranscript().getElementIndex(elem));
        getEditor().getUndoSupport().postEdit(edit);
    }


    private class TierLabelInsertionHook extends DefaultInsertionHook {

        @Override
        public List<DefaultStyledDocument.ElementSpec> batchInsertString(StringBuilder buffer, MutableAttributeSet attrs) {
            List<DefaultStyledDocument.ElementSpec> retVal = new ArrayList<>();

            if(TranscriptStyleConstants.isLabel(attrs) && TranscriptStyleConstants.isUnderlineOnHover(attrs)) {
                final String elementType = TranscriptStyleConstants.getElementType(attrs);
                switch (elementType) {
                    case TranscriptStyleConstants.ELEMENT_TYPE_RECORD ->
                        TranscriptStyleConstants.setClickHandler(attrs, TierLabelMenuExtension.this::tierLabelClickHandler);
                    case TranscriptStyleConstants.ELEMENT_TYPE_COMMENT ->
                        TranscriptStyleConstants.setClickHandler(attrs, TierLabelMenuExtension.this::commentLabelClickHandler);
                    case TranscriptStyleConstants.ELEMENT_TYPE_GEM ->
                        TranscriptStyleConstants.setClickHandler(attrs, TierLabelMenuExtension.this::gemLabelClickHandler);
//                    case TranscriptStyleConstants.ELEMENT_TYPE_BLIND_TRANSCRIPTION -> {
//                        Record record = TranscriptStyleConstants.getRecord(attrs);
//                        Tier<?> tier = TranscriptStyleConstants.getTier(attrs);
//                        String transcriber = TranscriptStyleConstants.getTranscriber(attrs);
//                        onClickBlindTranscriptionLabel(e.getPoint(), record, tier, transcriber);
//                    }
                }
            }

            return retVal;
        }
    }

}
