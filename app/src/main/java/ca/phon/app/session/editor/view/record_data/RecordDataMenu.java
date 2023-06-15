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
package ca.phon.app.session.editor.view.record_data;

import ca.phon.app.session.editor.view.record_data.actions.*;
import ca.phon.ui.action.PhonUIAction;
import ca.phon.util.icons.*;

import javax.swing.*;
import javax.swing.event.*;

/**
 * Menu for the Record Data editor view.
 *
 */
public class RecordDataMenu extends JMenu implements MenuListener {

	private final RecordDataEditorView editor;
	
	public RecordDataMenu(RecordDataEditorView editor) {
		super();
		this.editor = editor;
		addMenuListener(this);
	}

	@Override
	public void menuSelected(MenuEvent e) {
		removeAll();
		
		final PhonUIAction<Void> findAndReplaceAct = PhonUIAction.eventConsumer(editor::onToggleFindAndReplace);
		findAndReplaceAct.putValue(PhonUIAction.NAME, "Find & Replace");
		findAndReplaceAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Toggle Find & Replace UI");
		findAndReplaceAct.putValue(PhonUIAction.SMALL_ICON, IconManager.getInstance().getIcon("actions/edit-find-replace", IconSize.SMALL));
		findAndReplaceAct.putValue(PhonUIAction.SELECTED_KEY, editor.isFindAndReplaceVisible());
		add(new JCheckBoxMenuItem(findAndReplaceAct));
	}

	@Override
	public void menuDeselected(MenuEvent e) {
	}

	@Override
	public void menuCanceled(MenuEvent e) {
	}
	
}
