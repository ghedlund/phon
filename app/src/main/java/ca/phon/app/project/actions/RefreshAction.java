/*
 * Copyright (C) 2012-2018 Gregory Hedlund & Yvan Rose
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
package ca.phon.app.project.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import ca.phon.app.project.ProjectWindow;

public class RefreshAction extends ProjectWindowAction {

	private static final long serialVersionUID = 2777437252867377184L;

	public RefreshAction(ProjectWindow projectWindow) {
		super(projectWindow);
		
		putValue(NAME, "Refresh");
		putValue(SHORT_DESCRIPTION, "Refresh project");
		final KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0);
		putValue(ACCELERATOR_KEY, ks);
	}

	@Override
	public void hookableActionPerformed(ActionEvent ae) {
		getWindow().refreshProject();
	}

}
