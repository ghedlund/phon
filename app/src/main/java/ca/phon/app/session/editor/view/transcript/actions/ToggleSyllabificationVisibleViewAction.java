package ca.phon.app.session.editor.view.transcript.actions;

import ca.phon.app.session.editor.SessionEditor;
import ca.phon.app.session.editor.view.transcript.TranscriptView;

import java.awt.event.ActionEvent;

public class ToggleSyllabificationVisibleViewAction extends TranscriptViewAction {
    private static final long serialVersionUID = -6339597839656747666L;

    public ToggleSyllabificationVisibleViewAction(SessionEditor editor, TranscriptView view) {
        super(editor, view);

        final boolean syllabificationVisible = view.isSyllabificationVisible();
        putValue(NAME, (syllabificationVisible ? "Hide" : "Show") + " syllabification");
    }

    @Override
    public void hookableActionPerformed(ActionEvent e) {
        this.getView().toggleSyllabificationVisible();
        this.getView().getTranscriptEditor().recalculateTierLabelWidth();
    }
}
