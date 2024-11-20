/*
 * Copyright (C) 2005-2020 Gregory Hedlund & Yvan Rose
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 *    http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.phon.app.session.editor.actions;

import ca.phon.app.session.editor.SessionEditor;
import ca.phon.app.session.editor.undo.MediaLocationEdit;
import ca.phon.app.session.editor.view.mediaPlayer.MediaPlayerEditorView;
import ca.phon.ui.nativedialogs.*;
import ca.phon.util.icons.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class AssignMediaAction extends SessionEditorAction {

	private static final long serialVersionUID = 5340838112757528304L;
	
	private final static String TXT = "Assign media to session...";
	
	private final static String DESC = "Select media file for session using filesystem browser";
	
	private final static ImageIcon ICON =
			IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "media_link", IconSize.SMALL, UIManager.getColor("Button.foreground"));

	public AssignMediaAction(SessionEditor editor) {
		super(editor);
		
		putValue(NAME, TXT);
		putValue(SHORT_DESCRIPTION, DESC);
		putValue(SMALL_ICON, ICON);
	}

	@Override
	public void hookableActionPerformed(ActionEvent e) {
		final MediaPlayerEditorView mediaPlayerView = 
				(MediaPlayerEditorView)getEditor().getViewModel()
				.getView(MediaPlayerEditorView.VIEW_NAME);
		if(mediaPlayerView != null) {
			if(mediaPlayerView.getPlayer() != null && mediaPlayerView.getPlayer().isPlaying())
				mediaPlayerView.getPlayer().pause();
		}
		
		final OpenDialogProperties props = new OpenDialogProperties();
		props.setRunAsync(false);
		props.setAllowMultipleSelection(false);
		props.setCanChooseDirectories(false);
		props.setCanChooseFiles(true);
		props.setFileFilter(FileFilter.mediaFilter);
		props.setTitle("Select media for session");
		
		final List<String> selectedFiles = 
				NativeDialogs.showOpenDialog(props);
		if(selectedFiles != null && selectedFiles.size() > 0) {
			final String path = selectedFiles.get(0);
			
			final MediaLocationEdit edit = new MediaLocationEdit(getEditor(), path);
			getEditor().getUndoSupport().postEdit(edit);
		}
	}

}
