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
package ca.phon.app.session.editor.view.mediaPlayer.actions;

import ca.phon.app.session.editor.SessionEditor;
import ca.phon.app.session.editor.view.mediaPlayer.MediaPlayerEditorView;
import ca.phon.session.Participant;
import ca.phon.ui.action.PhonActionEvent;

import java.awt.event.ActionEvent;

public class GoToEndOfSegmentedAction extends MediaPlayerAction {

	private static final long serialVersionUID = 5084237133652527770L;

	private final static String CMD_NAME = "Go to end of segmented media";
	
	private final static String SHORT_DESC = "Go to end of segmented media";
	
	private final static String CMD_NAME_PART = "Go to end of last segment for ";
	
	private Participant participant;
	
	public GoToEndOfSegmentedAction(SessionEditor editor,
			MediaPlayerEditorView view) {
		super(editor, view);
		
		putValue(NAME, CMD_NAME);
		putValue(SHORT_DESCRIPTION, SHORT_DESC);
	}
	
	public GoToEndOfSegmentedAction(SessionEditor editor,
			MediaPlayerEditorView view, Participant part) {
		super(editor, view);
		
		this.participant = part;
		
		if(this.participant != null) {
			putValue(NAME, CMD_NAME_PART +
					(participant.getName() == null ? participant.getId() : participant.getName()));
		} else {
			putValue(NAME, CMD_NAME);
			putValue(SHORT_DESCRIPTION, SHORT_DESC);
		}
	}

	@Override
	public void hookableActionPerformed(ActionEvent e) {
		getMediaPlayerView().onMenuGoto(new PhonActionEvent(e, participant));
	}

}
