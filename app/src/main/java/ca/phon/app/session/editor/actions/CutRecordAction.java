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
import ca.phon.app.session.editor.undo.DeleteRecordEdit;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class CutRecordAction extends CopyRecordAction {

	private static final String CMD_NAME = "Cut record";
	
	private static final String SHORT_DESC = "Copy record to clipboard and remove record";
	
	private static final String ICON = "";
	
	private static final KeyStroke KS = 
			KeyStroke.getKeyStroke(KeyEvent.VK_X,
					Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | KeyEvent.SHIFT_MASK);

	public CutRecordAction(SessionEditor editor) {
		super(editor);
		
		putValue(NAME, CMD_NAME);
		putValue(SHORT_DESCRIPTION, SHORT_DESC);
		putValue(ACCELERATOR_KEY, KS);
	}

	@Override
	public void hookableActionPerformed(ActionEvent e) {
		super.hookableActionPerformed(e);
		
		final DeleteRecordEdit edit = new DeleteRecordEdit(getEditor(), getEditor().getCurrentRecordIndex());
		getEditor().getUndoSupport().postEdit(edit);
	}

}
