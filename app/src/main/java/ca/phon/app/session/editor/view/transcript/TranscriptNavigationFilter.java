package ca.phon.app.session.editor.view.transcript;

import ca.phon.app.log.LogUtil;
import ca.phon.app.session.editor.EditorEvent;
import ca.phon.session.*;
import ca.phon.session.Record;
import ca.phon.util.PrefHelper;

import javax.swing.*;
import javax.swing.text.*;

/**
 * The default navigation filter for the transcript editor
 */
public class TranscriptNavigationFilter extends NavigationFilter {

    private final TranscriptEditor editor;

    /**
     * Constructor
     *
     * @param editor a reference to the transcript editor
     */
    public TranscriptNavigationFilter(TranscriptEditor editor) {
        this.editor = editor;
    }

    @Override
    public void setDot(FilterBypass fb, int dot, Position.Bias bias) {
        TranscriptDocument doc = editor.getTranscriptDocument();
        if (doc.getLength() == 0) {
            fb.setDot(dot, bias);
        }
        if(doc.isBypassDocumentFilter()) {
            fb.setDot(dot, bias);
            return;
        }

        Element elem = doc.getCharacterElement(dot);
        AttributeSet attrs = elem.getAttributes();
        if (attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_NOT_TRAVERSABLE) != null) return;

//        AttributeSet prevAttrs = doc.getCharacterElement(fb.getCaret().getDot()).getAttributes();
//        AttributeSet nextAttrs = doc.getCharacterElement(dot).getAttributes();

//        String prevElemType = (String) prevAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);
//        String nextElemType = (String) nextAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);
//        Tier<?> nextTier = (Tier<?>) nextAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);

//        if (prevElemType != null) {
//            try {
//                switch (prevElemType) {
//                    case TranscriptStyleConstants.ATTR_KEY_RECORD -> {
//                        Tier<?> prevTier = (Tier<?>) prevAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);
//                        if (prevTier == null || prevTier.getDeclaredType().equals(PhoneAlignment.class)) break;
//                        if (SystemTierType.Segment.getName().equals(prevTier.getName())) break;
//                        if (nextElemType != null && nextElemType.equals(TranscriptStyleConstants.ATTR_KEY_RECORD)) {
//                            if (nextTier != null && nextTier == prevTier) break;
//                        }
//                        int start = doc.getTierContentStart(prevTier);
//                        int end = doc.getTierEnd(prevTier) - 1;
//                        String newValue = doc.getText(start, end - start);
//                        String oldString = prevTier.toString();
//                        if (!oldString.equals(newValue)) {
////                            editor.setInternalEdit(true);
//                            editor.changeTierData((Record) prevAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_RECORD), prevTier, newValue);
//                        }
//                    }
//                    case TranscriptStyleConstants.ATTR_KEY_COMMENT -> {
//                        Comment prevComment = (Comment) prevAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_COMMENT);
//                        if (prevComment == null) break;
//                        if (nextElemType != null && nextElemType.equals(TranscriptStyleConstants.ATTR_KEY_COMMENT)) {
//                            Comment nextComment = (Comment) nextAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_COMMENT);
//                            if (nextComment != null && nextComment == prevComment) break;
//                        }
//                        int start = doc.getCommentContentStart(prevComment);
//                        int end = doc.getCommentEnd(prevComment) - 1;
//                        String newValue = doc.getText(start, end - start);
//                        editor.commentDataChanged(prevComment, newValue);
//                    }
//                    case TranscriptStyleConstants.ATTR_KEY_GEM -> {
//                        Gem prevGem = (Gem) prevAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GEM);
//                        if (prevGem == null) break;
//                        if (nextElemType != null && nextElemType.equals(TranscriptStyleConstants.ATTR_KEY_GEM)) {
//                            Gem nextGem = (Gem) nextAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GEM);
//                            if (nextGem != null && nextGem == prevGem) break;
//                        }
//                        int start = doc.getGemContentStart(prevGem);
//                        int end = doc.getGemEnd(prevGem) - 1;
//                        String newValue = doc.getText(start, end - start);
//                        editor.gemDataChanged(prevGem, newValue);
//                    }
//                    case TranscriptStyleConstants.ATTR_KEY_GENERIC_TIER -> {
//                        Tier<?> prevGenericTier = (Tier<?>) prevAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GENERIC_TIER);
//                        if (prevGenericTier == null) break;
//                        if (nextElemType != null && nextElemType.equals(TranscriptStyleConstants.ATTR_KEY_GENERIC_TIER)) {
//                            Tier<?> nextGenericTier = (Tier<?>) nextAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GENERIC_TIER);
//                            if (nextGenericTier != null && nextGenericTier == prevGenericTier) break;
//                        }
//                        int start = doc.getGenericContentStart(prevGenericTier);
//                        int end = doc.getGenericEnd(prevGenericTier) - 1;
//                        String newValue = doc.getText(start, end - start);
//                        if (!prevGenericTier.hasValue() || !prevGenericTier.getValue().toString().equals(newValue)) {
////                            editor.setInternalEdit(true);
//                            editor.genericDataChanged(prevGenericTier, newValue);
//                        }
//                    }
//                }
//            } catch (BadLocationException e) {
//                LogUtil.severe(e);
//            }
//        }

        if (doc.getLength() == dot) return;

        if (!editor.isCaretMoveFromUpDown()) editor.setUpDownOffset(-1);
        editor.setCaretMoveFromUpDown(false);

        int prevCaretPos = editor.getCaretPosition();

        if(PrefHelper.isDebugMode()) {
            LogUtil.info("Setting dot to " + dot);
        }
        fb.setDot(dot, bias);

        TranscriptEditor.TranscriptLocationChangeData transcriptLocationChangeData = new TranscriptEditor.TranscriptLocationChangeData(
                editor.charPosToSessionLocation(prevCaretPos),
                editor.charPosToSessionLocation(dot)
        );
        if(!transcriptLocationChangeData.newLoc().equals(transcriptLocationChangeData.oldLoc())) {
            final EditorEvent<TranscriptEditor.TranscriptLocationChangeData> e = new EditorEvent<>(
                    TranscriptEditor.transcriptLocationChanged,
                    editor,
                    transcriptLocationChangeData
            );
            editor.getEventManager().queueEvent(e);
        }
    }

    @Override
    public void moveDot(FilterBypass fb, int dot, Position.Bias bias) {
        TranscriptDocument doc = editor.getTranscriptDocument();

        if (editor.getTranscriptDocument().getLength() == dot) return;

        AttributeSet attrs = doc.getCharacterElement(editor.getCaretPosition()).getAttributes();
        String elementType = (String) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);

        if (elementType != null) {
            int start = -1;
            int end = -1;

            switch (elementType) {
                case TranscriptStyleConstants.ATTR_KEY_RECORD, TranscriptStyleConstants.ATTR_KEY_BLIND_TRANSCRIPTION -> {
                    Record record = TranscriptStyleConstants.getRecord(attrs);
                    if(record == null) return;
                    int recordIndex = editor.getSession().getRecordPosition(record);
                    if(recordIndex < 0) return;
                    Tier<?> tier = (Tier<?>) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);
                    if (tier != null) {
                        final TranscriptDocument.StartEnd tierStartEnd = doc.getTierContentStartEnd(recordIndex, tier.getName());
                        start = tierStartEnd.start();
                        end = tierStartEnd.end();
                    }
                }
                case TranscriptStyleConstants.ATTR_KEY_COMMENT -> {
                    Comment comment = (Comment) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_COMMENT);
                    if (comment != null) {
                        start = doc.getCommentContentStart(comment);
                        end = doc.getCommentEnd(comment);
                    }
                }
                case TranscriptStyleConstants.ATTR_KEY_GEM -> {
                    Gem gem = (Gem) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GEM);
                    if (gem != null) {
                        start = doc.getGemContentStart(gem);
                        end = doc.getGemEnd(gem);
                    }
                }
                case TranscriptStyleConstants.ATTR_KEY_GENERIC_TIER -> {
                    Tier<?> generic = (Tier<?>) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GENERIC_TIER);
                    if (generic != null) {
                        start = doc.getGenericContentStart(generic);
                        end = doc.getGenericEnd(generic);
                    }
                }
            }

            if (start != -1 && end != -1) {
                dot = Math.min(Math.max(dot, start), end);
                if(PrefHelper.isDebugMode()) {
                    LogUtil.info("Moving dot to " + dot);
                }
            }
        }

        fb.moveDot(dot, bias);
    }
}
