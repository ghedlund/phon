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
import ca.phon.app.session.editor.undo.AddRecordEdit;
import ca.phon.session.Record;
import ca.phon.session.*;
import ca.phon.util.icons.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class DuplicateRecordAction extends SessionEditorAction {

	private static final long serialVersionUID = 8927870935233124123L;
	
	private final static String CMD_NAME = "Duplicate record";
	
	private final static String SHORT_DESC = "Duplicate record after current";
	
	private final static String ICON = "misc/record-duplicate";
	
	private final static KeyStroke KS = KeyStroke.getKeyStroke(
			KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());

	public DuplicateRecordAction(SessionEditor editor) {
		super(editor);
		
		putValue(NAME, CMD_NAME);
		putValue(SHORT_DESCRIPTION, SHORT_DESC);
		putValue(ACCELERATOR_KEY, KS);
		putValue(SMALL_ICON, IconManager.getInstance().getIcon(ICON, IconSize.SMALL));
	}

	@Override
	public void hookableActionPerformed(ActionEvent arg0) {
		final SessionFactory factory = SessionFactory.newFactory();
		final Record r = getEditor().currentRecord();
		if(r != null) {
			final Record dup = factory.cloneRecord(r);
			dup.setSpeaker(r.getSpeaker());
			final AddRecordEdit edit = new AddRecordEdit(getEditor(), dup, getEditor().getSession().getRecordElementIndex(r) + 1);
			getEditor().getUndoSupport().postEdit(edit);

			int recordIndex = getEditor().getSession().getRecordPosition(edit.getRecord());
			getEditor().setCurrentRecordIndex(recordIndex);
		}
	}

}
