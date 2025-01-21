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
package ca.phon.app.session.editor.view.syllabificationAlignment.actions;

import ca.phon.app.session.editor.SessionEditor;
import ca.phon.app.session.editor.view.syllabificationAlignment.*;
import ca.phon.session.*;
import ca.phon.ui.CommonModuleFrame;
import ca.phon.ui.decorations.DialogHeader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class SyllabificationSettingsCommand extends SyllabificationAlignmentCommand {
	
	private final static String TEXT = "Syllabifier Settings";

	private final static String DESC = "Edit syllabifier settings...";
	
	public SyllabificationSettingsCommand(SessionEditor editor,
			SyllabificationAlignmentEditorView view) {
		super(editor, view);
		
		putValue(NAME, TEXT);
		putValue(SHORT_DESCRIPTION, DESC);
	}

	@Override
	public void hookableActionPerformed(ActionEvent e) {
		final JDialog settingsDialog = new JDialog(CommonModuleFrame.getCurrentFrame());
		settingsDialog.setModal(true);
		
		settingsDialog.setLayout(new BorderLayout());
		final DialogHeader header = new DialogHeader("Syllabifier settings", "Select syllabifier for IPA tiers.");
		settingsDialog.add(header, BorderLayout.NORTH);
		
		final SyllabifierInfo info = getEditor().getSession().getExtension(SyllabifierInfo.class);
		final SyllabificationSettingsPanel settingsPanel = new SyllabificationSettingsPanel(info);
		settingsDialog.add(settingsPanel, BorderLayout.CENTER);
		
		final AtomicBoolean wasCanceled = new AtomicBoolean(true);
		final JPanel btmPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		final JButton okBtn = new JButton("Ok");
		okBtn.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				wasCanceled.getAndSet(false);
				settingsDialog.setVisible(false);
			}
			
		});
		btmPanel.add(okBtn);
		settingsDialog.getRootPane().setDefaultButton(okBtn);
		
		settingsDialog.add(btmPanel, BorderLayout.SOUTH);
		settingsDialog.pack();
		settingsDialog.setLocationRelativeTo(getView());
		settingsDialog.setVisible(true);
		
		// wait
		
		if(!wasCanceled.get()) {
			info.setSyllabifierLanguageForTier(SystemTierType.IPATarget.getName(), settingsPanel.getSelectedTargetSyllabifier());
			info.setSyllabifierLanguageForTier(SystemTierType.IPAActual.getName(), settingsPanel.getSelectedActualSyllabifier());
			
			info.saveInfo(getEditor().getSession());
		}
	}

}
