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
package ca.phon.app.session.editor.view.syllabificationAlignment;

import ca.phon.app.session.editor.*;
import ca.phon.app.session.editor.undo.SessionUndoableEdit;
import ca.phon.ipa.*;
import ca.phon.session.Session;
import ca.phon.syllable.SyllabificationInfo;

public class ToggleDiphthongEdit extends SessionUndoableEdit {

	private final IPATranscript transcript;
	
	private final int index;



	public ToggleDiphthongEdit(SessionEditor editor, IPATranscript transcript, int index) {
		this(editor.getSession(), editor.getEventManager(), transcript, index);
	}

	public ToggleDiphthongEdit(Session session, EditorEventManager editorEventManager, IPATranscript transcript, int index) {
		super(session, editorEventManager);
		this.transcript = transcript;
		this.index = index;
	}
	
	@Override
	public void undo() {
		super.redo();
	}

	@Override
	public void doIt() {
		if(index >= 0 && index < transcript.length()) {
			final IPAElement ele = transcript.elementAt(index);
			final SyllabificationInfo info = ele.getExtension(SyllabificationInfo.class);
			info.setDiphthongMember(!info.isDiphthongMember());

			final EditorEvent<SyllabificationAlignmentEditorView.ScEditData> ee =
					new EditorEvent<>(SyllabificationAlignmentEditorView.ScEdit, getSource(),
							new SyllabificationAlignmentEditorView.ScEditData(transcript, index, info.getConstituentType(), info.getConstituentType()));
			getEditorEventManager().queueEvent(ee);
		}
	}

}
