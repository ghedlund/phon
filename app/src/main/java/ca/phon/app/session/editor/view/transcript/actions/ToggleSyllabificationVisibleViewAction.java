package ca.phon.app.session.editor.view.transcript.actions;

import ca.phon.app.session.editor.SessionEditor;
import ca.phon.app.session.editor.view.transcript.TranscriptView;
import ca.phon.ipa.IPATranscript;
import ca.phon.session.SystemTierType;
import ca.phon.session.TierDescription;
import ca.phon.session.TierViewItem;
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
            for (TierViewItem tvi : this.getView().getEditor().getSession().getTierView()) {
                if (tvi.isVisible()) {
                    if (tvi.getTierName().equals(SystemTierType.IPATarget.getName())) {
                        additionalTiers.add(SystemTierType.TargetSyllables.getName());
                    } else if (tvi.getTierName().equals(SystemTierType.IPAActual.getName())) {
                        additionalTiers.add(SystemTierType.ActualSyllables.getName());
                    } else {
                        TierDescription td = this.getView().getEditor().getSession().getTier(tvi.getTierName());
                        if (td != null && td.getDeclaredType() == IPATranscript.class) {
                            additionalTiers.add(tvi.getTierName() + " Syllables");
                        }
                    }
                }
            }
        }
        this.getView().getTranscriptEditor().recalculateTierLabelWidth(additionalTiers);
    }
}
