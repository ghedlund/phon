package ca.phon.app.session.editor.view.transcript;

import ca.phon.session.Session;

import javax.swing.text.Document;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.ViewFactory;
import java.util.List;

/**
 * The {@link javax.swing.text.EditorKit} used by the {@link TranscriptEditor}
 * */
public class TranscriptEditorKit extends StyledEditorKit {
    public static String CONTENT_TYPE = "phon/transcript";
    private final TranscriptViewFactory viewFactory;

    public TranscriptEditorKit(Session session) {
        viewFactory = new TranscriptViewFactory(session);
    }

    public void invalidateTierLabelWidth(List<String> additionalTiers) {
        viewFactory.setAdditionalTiers(additionalTiers);
        viewFactory.setTierLabelWidth(-1);
    }

    @Override
    public Document createDefaultDocument() {
        return new TranscriptDocument(viewFactory);
    }

    @Override
    public String getContentType() {
        return CONTENT_TYPE;
    }

    @Override
    public ViewFactory getViewFactory() {
        return viewFactory;
    }
}
