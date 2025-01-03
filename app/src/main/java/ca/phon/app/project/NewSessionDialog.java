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
package ca.phon.app.project;

import ca.phon.project.Project;
import ca.phon.ui.CommonModuleFrame;
import ca.phon.ui.decorations.DialogHeader;
import ca.phon.ui.layout.ButtonBarBuilder;
import ca.phon.ui.text.SessionNameField;
import ca.phon.ui.toast.ToastFactory;
import ca.phon.util.PhonConstants;

import javax.swing.*;
import java.awt.*;

public class NewSessionDialog extends JDialog {

	private JRadioButton fromMediaButton = new JRadioButton("From existing media file");
	private JRadioButton emptySession = new JRadioButton("Empty session");

	private SessionNameField sessionNameField = new SessionNameField();

	private JButton btnCreateSession = new JButton();
	private JButton btnCancel = new JButton();

	private boolean canceled = false;
	
	private Project proj;
	private String subFolder;

	public NewSessionDialog(Project project) {
		this(project, ".");
	}

	/**
	 * Default constructor
	 */
	public NewSessionDialog(Project project, String subFolder) {
		super();
		this.proj = project;
		this.subFolder = subFolder;
		setTitle(project.getName() + " : New Session");
		setModal(true);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setLocationRelativeTo(CommonModuleFrame.getCurrentFrame());
		
		init();
	}
	
	private void init() {
		setLayout(new BorderLayout());
		
		final DialogHeader header = new DialogHeader(getTitle(), "Create a new Session");
		add(header, BorderLayout.NORTH);


		
		btnCreateSession.setActionCommand("Create");
		btnCreateSession.setName("btnCreateSession");
		btnCreateSession.setText("Ok");
		btnCreateSession.setDefaultCapable(true);
		btnCreateSession.addActionListener( (e) -> {
			if(validateForm()) {
				canceled = false;
				dispose();
			}
		});
		getRootPane().setDefaultButton(btnCreateSession);
		
		btnCancel.setActionCommand("Cancel");
		btnCancel.setName("btnCancel");
		btnCancel.setText("Cancel");
		btnCancel.addActionListener( (e) -> {
			canceled = true;
			dispose();
		});
		
		JComponent buttonBar = ButtonBarBuilder.buildOkCancelBar(btnCreateSession, btnCancel);
		add(buttonBar, BorderLayout.SOUTH);
	}
	
	public boolean validateForm() {
		boolean valid = true;
		// Ensure a non-empty corpus name (description is optional)
		if (getSessionName() == null || getSessionName().length() == 0) {
			valid = false;
		}
		final String sessionName = getSessionName();
		
		if(sessionName.indexOf('.') >= 0) {
			valid = false;
		}
		for(char invalidChar:PhonConstants.illegalFilenameChars) {
			if(sessionName.indexOf(invalidChar) >= 0) {
				valid = false;
				break;
			}
		}

		if(!valid) {
		}
		return valid;
	}

	public String getSessionName() {
		return sessionNameField.getText().trim();
	}

	public String getCorpusName() {
		return subFolder;
	}
	
	public boolean wasCanceled() {
		return canceled;
	}
	
}