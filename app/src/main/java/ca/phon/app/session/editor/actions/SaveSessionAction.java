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

import ca.phon.app.log.LogUtil;
import ca.phon.app.session.editor.SessionEditor;
import ca.phon.session.Session;
import ca.phon.util.icons.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;

/**
 * <p>Save the {@link Session} currently open in the {@link SessionEditor}.</p>
 */
public class SaveSessionAction extends SessionEditorAction {
	
	private final static String CMD_NAME = "Save";
	
	private final static String SHORT_DESC = "Save session";
	
	private final static String ICON = "actions/filesave";

	private final static KeyStroke KS = KeyStroke.getKeyStroke(KeyEvent.VK_S,
			Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());

	public SaveSessionAction(SessionEditor editor) {
		super(editor);
		
		putValue(NAME, CMD_NAME);
		putValue(SHORT_DESCRIPTION, SHORT_DESC);
		putValue(SMALL_ICON, IconManager.getInstance().getIcon(ICON, IconSize.SMALL));
		putValue(ACCELERATOR_KEY, KS);
	}

	@Override
	public void hookableActionPerformed(ActionEvent ae) {
		try {
			getEditor().saveData();
		} catch (IOException e) {
			LogUtil.warning(e);
		}
	}

}
