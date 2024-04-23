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
package ca.phon.app.opgraph.editor.actions.file;

import ca.phon.app.log.LogUtil;
import ca.phon.app.menu.file.OpenFileHistory;
import ca.phon.app.opgraph.editor.OpgraphEditor;
import ca.phon.app.opgraph.editor.actions.OpgraphEditorAction;
import ca.phon.ui.toast.ToastFactory;
import ca.phon.util.RecentFiles;
import ca.phon.util.icons.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;

public class SaveAction extends OpgraphEditorAction {
	
	public final static String TXT = "Save";
	
	public final static String DESC = "Save graph";
	
	public final static KeyStroke KS = KeyStroke.getKeyStroke(KeyEvent.VK_S,
			Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
	
	public final static ImageIcon ICON = 
			IconManager.getInstance().getIcon("actions/document-save", IconSize.SMALL);

	public SaveAction(OpgraphEditor editor) {
		super(editor);
	
		putValue(NAME, TXT);
		putValue(SHORT_DESCRIPTION, DESC);
		putValue(ACCELERATOR_KEY, KS);
		putValue(SMALL_ICON, ICON);
	}

	@Override
	public void hookableActionPerformed(ActionEvent arg0) {
		// call save on the editor
		try {
			getEditor().saveData();
			
			if(getEditor().getCurrentFile() != null) {
				final RecentFiles recentFiles = new RecentFiles(OpgraphEditor.RECENT_DOCS_PROP);
				recentFiles.addToHistory(getEditor().getCurrentFile());
				
				OpenFileHistory openFileHistory = new OpenFileHistory();
				openFileHistory.addToHistory(getEditor().getCurrentFile());
			}
		} catch (IOException e) {
			LogUtil.warning(e);
			ToastFactory.makeToast(e.getLocalizedMessage()).start(getEditor());
			Toolkit.getDefaultToolkit().beep();
		}
	}

}
