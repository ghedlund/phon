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
package ca.phon.app.project.actions;

import ca.phon.app.log.LogUtil;
import ca.phon.app.project.ProjectWindow;
import ca.phon.plugin.*;
import ca.phon.session.SessionPath;
import ca.phon.util.icons.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashMap;

public class OpenSessionAction extends ProjectWindowAction {

	private String corpus = null;
	private String sessionName = null;
	
	private boolean blindMode = false;
	
	public OpenSessionAction(ProjectWindow projectWindow) {
		this(projectWindow, null, null);
	}
	
	public OpenSessionAction(ProjectWindow projectWindow, boolean blindMode) {
		this(projectWindow, null, null, blindMode);
	}
	
	public OpenSessionAction(ProjectWindow projectWindow, String corpus, String sessionName) {
		this(projectWindow, corpus, sessionName, false);
	}
	
	public OpenSessionAction(ProjectWindow projectWindow, String corpus, String sessionName, boolean blindMode) {
		super(projectWindow);
		
		this.corpus = corpus;
		this.sessionName = sessionName;
		this.blindMode = blindMode;

		ImageIcon icn = IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "file_open", IconSize.MEDIUM, UIManager.getColor("Button.foreground"));
		putValue(NAME, "Open session");
		putValue(SHORT_DESCRIPTION, "Open session in a new editor window");
		putValue(SMALL_ICON, icn);
	}

	@Override
	public void hookableActionPerformed(ActionEvent ae) {
		final String corpus = 
				(this.corpus == null ? getWindow().getSelectedCorpus() : this.corpus);
		final String sessionName =
				(this.sessionName == null ? getWindow().getSelectedSessionName() : this.sessionName);
		
		HashMap<String, Object> initInfo = new HashMap<String, Object>();
		initInfo.put("project", getWindow().getProject());
		initInfo.put("sessionName", new SessionPath(corpus, sessionName).toString());
		initInfo.put("blindmode", blindMode);
		
		try {
			PluginEntryPointRunner.executePlugin("SessionEditor", initInfo);
		} catch (PluginException e) {
			LogUtil.warning(e);
			showMessage("Open Session", e.getLocalizedMessage());
			Toolkit.getDefaultToolkit().beep();
		}
	}

}
