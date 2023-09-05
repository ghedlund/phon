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

import ca.phon.app.session.SessionToHTMLWizard;
import ca.phon.app.session.editor.SessionEditor;
import ca.phon.project.Project;
import ca.phon.ui.CommonModuleFrame;

import java.awt.event.ActionEvent;

public class ExportAsHTMLAction extends SessionEditorAction {

	public ExportAsHTMLAction(SessionEditor editor) {
		super(editor);
		
		putValue(SessionEditorAction.NAME, "Export session (HTML and spreadsheet)...");
		putValue(SessionEditorAction.SHORT_DESCRIPTION, "Export session as HTML or in a spreadsheet");
	}

	@Override
	public void hookableActionPerformed(ActionEvent ae) {
		final SessionToHTMLWizard wizard = new SessionToHTMLWizard("Export as HTML", getEditor().getProject(), getEditor().getSession());
		wizard.putExtension(Project.class, getEditor().getProject());
		wizard.setParentFrame(CommonModuleFrame.getCurrentFrame());
		wizard.pack();
		wizard.setSize(1024, 768);
		wizard.centerWindow();
		wizard.setVisible(true);
	}

}
