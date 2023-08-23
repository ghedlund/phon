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
package ca.phon.app.session.editor.view.tier_management.actions;

import ca.phon.app.session.editor.SessionEditor;
import ca.phon.app.session.editor.undo.TierViewItemEdit;
import ca.phon.app.session.editor.view.tier_management.TierOrderingEditorView;
import ca.phon.session.*;

import java.awt.event.ActionEvent;

public class ToggleTierVisibleAction extends TierManagementAction {

	private static final long serialVersionUID = -4743364905920496226L;
	
	private final static String SHOW_TIER = "Show tier";

	private final static String HIDE_TIER = "Hide tier";
	
	private final static String SHORT_DESC = "";
	
	private final TierViewItem item;

	public ToggleTierVisibleAction(SessionEditor editor,
			TierOrderingEditorView view, TierViewItem tierViewItem) {
		super(editor, view);
		this.item = tierViewItem;

		if(tierViewItem.isVisible())
			putValue(NAME, HIDE_TIER);
		else
			putValue(NAME, SHOW_TIER);
		putValue(SHORT_DESCRIPTION, SHORT_DESC);
	}

	@Override
	public void hookableActionPerformed(ActionEvent e) {
		final SessionFactory factory = SessionFactory.newFactory();
		final TierViewItem newItem = factory.createTierViewItem(item.getTierName(), !item.isVisible(), item.getTierFont(), item.isTierLocked());
		
		final TierViewItemEdit edit = new TierViewItemEdit(getEditor(), item, newItem);
		getEditor().getUndoSupport().postEdit(edit);
	}

}
