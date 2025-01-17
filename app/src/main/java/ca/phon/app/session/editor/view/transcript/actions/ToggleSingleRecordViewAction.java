package ca.phon.app.session.editor.view.transcript.actions;

import ca.phon.app.session.editor.SessionEditor;
import ca.phon.app.session.editor.view.transcript.TranscriptView;

import java.awt.event.ActionEvent;

public class ToggleSingleRecordViewAction extends TranscriptViewAction {
    private static final long serialVersionUID = -6339057839610947666L;

    public ToggleSingleRecordViewAction(SessionEditor editor, TranscriptView view) {
        super(editor, view);

        final boolean singleRecordActivated = view.isSingleRecordView();
        putValue(NAME, "Show " + (singleRecordActivated ? "all records" : "single record"));
    }

    @Override
    public void hookableActionPerformed(ActionEvent e) {
        this.getView().toggleSingleRecordActive();
    }
}
