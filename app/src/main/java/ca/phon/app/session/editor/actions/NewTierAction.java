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

import ca.phon.app.session.editor.EditorDataModel;
import ca.phon.app.session.editor.EditorEventManager;
import ca.phon.app.session.editor.SessionEditor;
import ca.phon.app.session.editor.undo.AddTierEdit;
import ca.phon.app.session.editor.undo.SessionEditUndoSupport;
import ca.phon.app.session.editor.view.tierManagement.*;
import ca.phon.session.*;
import ca.phon.ui.toast.*;
import ca.phon.util.icons.*;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class NewTierAction extends SessionEditorAction {

	private final static String CMD_NAME = "New custom tier...";
	
	private final static String SHORT_DESC = "Add new tier to session.";
	
	private final static ImageIcon ICON = IconManager.getInstance().getFontIcon("add", IconSize.SMALL, UIManager.getColor("Button.foreground"));

	private int index = -1;

	private final EditorDataModel editorDataModel;

	public NewTierAction(SessionEditor editor) {
		this(editor, -1);
	}

	public NewTierAction(SessionEditor editor, int index) {
		this(editor.getDataModel(), editor.getEventManager(), editor.getUndoSupport(), index);
	}

	public NewTierAction(EditorDataModel dataModel, EditorEventManager eventManager, SessionEditUndoSupport undoSupport) {
		this(dataModel, eventManager, undoSupport, -1);
	}

	public NewTierAction(EditorDataModel dataModel, EditorEventManager eventManager, SessionEditUndoSupport undoSupport, int index) {
		super(dataModel.getSession(), eventManager, undoSupport);
		this.editorDataModel = dataModel;
		this.index = index;

		putValue(NAME, CMD_NAME);
		putValue(SMALL_ICON, ICON);
		putValue(SHORT_DESCRIPTION, SHORT_DESC);
	}

	@Override
	public void hookableActionPerformed(ActionEvent e) {
		TierEditorDialog newTierDialog = new TierEditorDialog(editorDataModel.getSession(), false);
		TierInfoEditor tierEditor = newTierDialog.getTierEditor();
		newTierDialog.add(tierEditor);
		newTierDialog.setTitle("New Tier");
		newTierDialog.setModal(true);
		newTierDialog.pack();

		final boolean isBlind = editorDataModel.getTranscriber() != Transcriber.VALIDATOR;
		if(isBlind) {
			newTierDialog.getTierEditor().setBlind(true);
			newTierDialog.getTierEditor().getBlindBox().setEnabled(false);
		}
		
		if(newTierDialog.showDialog()) {
			final Session session = editorDataModel.getSession();
			// get tier info
			String tierName = tierEditor.getTierName();
			tierName = StringUtils.strip(tierName);
			if(tierName.length() == 0) {
				return;
			}
			
			boolean tierExists = false;
			if(SystemTierType.isSystemTier(tierName)) {
				tierExists = true;
			} else {
				for(TierDescription td:session.getUserTiers()) {
					if(td.getName().equals(tierName)) {
						tierExists = true;
						break;
					}
				}
			}
			
			if(tierExists){
				final Toast toast = ToastFactory.makeToast("A tier with name " + tierEditor.getTierName() + " already exists.");
				toast.start(tierEditor);
				return;
			}
			
			// create tier
			final TierDescription tierDescription = tierEditor.createTierDescription();

			tierDescription.setBlind(isBlind);
			final TierViewItem tierViewItem = tierEditor.createTierViewItem();
			
			final AddTierEdit edit = new AddTierEdit(editorDataModel.getSession(), getEventManager(), tierDescription, tierViewItem, index);
			getUndoSupport().postEdit(edit);
		}
	}

}
