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
import ca.phon.app.session.editor.undo.MoveRecordEdit;
import ca.phon.session.Record;
import ca.phon.session.*;

import java.awt.event.ActionEvent;

public class MoveRecordBackwardAction extends SessionEditorAction {

	private static final long serialVersionUID = 7866859643630125691L;

	private final String TXT = "Move record backward";
	
	private final String DESC = "Move record backward in session";

	public MoveRecordBackwardAction(SessionEditor editor) {
		super(editor);
		
		putValue(NAME, TXT);
		putValue(SHORT_DESCRIPTION, DESC);
	}

	@Override
	public void hookableActionPerformed(ActionEvent ae) {
		final SessionEditor editor = getEditor();
		final Session session = editor.getSession();
		final Record record = editor.currentRecord();
		
		final int position = editor.getCurrentRecordIndex() - 1;
		
		if(position >= 0 && position < session.getRecordCount()) {
			final MoveRecordEdit edit = new MoveRecordEdit(getEditor(), record, position);
			getEditor().getUndoSupport().postEdit(edit);
		}
	}
	
}
