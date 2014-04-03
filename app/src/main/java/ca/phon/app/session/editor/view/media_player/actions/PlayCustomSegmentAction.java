package ca.phon.app.session.editor.view.media_player.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import ca.phon.app.session.editor.view.media_player.MediaPlayerEditorView;
import ca.phon.ui.action.PhonActionEvent;

public class PlayCustomSegmentAction extends MediaPlayerAction {

	private static final long serialVersionUID = 8216764220991547294L;
	
	private final static String CMD_NAME = "Play custom segment...";
	
	private final static String SHORT_DESC = "";
	
	private final static String ICON = "";
	
	private final static KeyStroke KS = 
			KeyStroke.getKeyStroke(KeyEvent.VK_R,
					Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.ALT_MASK);

	public PlayCustomSegmentAction(MediaPlayerEditorView view) {
		super(view);
		
		putValue(NAME, CMD_NAME);
		putValue(SHORT_DESCRIPTION, SHORT_DESC);
		putValue(ACCELERATOR_KEY, KS);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		final MediaPlayerEditorView view = getMediaPlayerView();
		view.onPlayCustomSegment(new PhonActionEvent(e));
	}

}
