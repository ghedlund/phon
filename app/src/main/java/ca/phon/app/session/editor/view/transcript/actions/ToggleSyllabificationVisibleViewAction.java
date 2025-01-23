package ca.phon.app.session.editor.view.transcript.actions;

import ca.phon.app.session.editor.SessionEditor;
import ca.phon.app.session.editor.view.transcript.TranscriptView;
import ca.phon.session.SystemTierType;
import com.kitfox.svg.A;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

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
        List<String> additionalTiers = new ArrayList<>();
        if(this.getView().isSyllabificationVisible()) {
            additionalTiers.add(SystemTierType.IPATarget.getName() + " Syllables");
            additionalTiers.add(SystemTierType.IPAActual.getName() + " Syllables");
        }
        this.getView().getTranscriptEditor().recalculateTierLabelWidth(additionalTiers);
    }
}
