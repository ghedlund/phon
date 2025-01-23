package ca.phon.app.session.editor.view.transcript.actions;

import ca.phon.app.session.editor.SessionEditor;
import ca.phon.app.session.editor.view.transcript.TranscriptView;

import java.awt.event.ActionEvent;

public class ToggleAlignmentVisibleViewAction extends TranscriptViewAction {
    private static final long serialVersionUID = -6337537859256747666L;

    public ToggleAlignmentVisibleViewAction(SessionEditor editor, TranscriptView view) {
        super(editor, view);

        final boolean alignmentVisible = view.isAlignmentVisible();
        putValue(NAME, (alignmentVisible ? "Hide" : "Show") + " alignment");
    }

    @Override
    public void hookableActionPerformed(ActionEvent e) {
        this.getView().toggleAlignmentVisible();
        this.getView().getTranscriptEditor().recalculateTierLabelWidth();
    }
}
