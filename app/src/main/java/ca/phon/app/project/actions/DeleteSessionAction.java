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
import ca.phon.project.Project;
import ca.phon.ui.CommonModuleFrame;
import ca.phon.ui.nativedialogs.*;
import ca.phon.util.OSInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.List;
import java.util.*;

public class DeleteSessionAction extends ProjectWindowAction {
	
	public DeleteSessionAction(ProjectWindow projectWindow) {
		super(projectWindow);
		
		putValue(Action.NAME, "Delete session");
		putValue(Action.SHORT_DESCRIPTION, "Delete selected session");
	}

	@Override
	public void hookableActionPerformed(ActionEvent ae) {
		final List<String> sessionNames = getWindow().getSelectedSessionNames();
		if(sessionNames.size() == 0) return;
		
		final MessageDialogProperties props = new MessageDialogProperties();
		props.setParentWindow(CommonModuleFrame.getCurrentFrame());
		props.setRunAsync(false);
		if(sessionNames.size() > 1) {
			props.setHeader("Delete selected sessions?");
			props.setMessage("Move selected sessions to " + 
					(OSInfo.isWindows() ? "Recycle Bin" : "Trash") + "?");
		} else {
			props.setHeader("Delete session: " + sessionNames.get(0));
			props.setMessage("Move session to " + 
					(OSInfo.isWindows() ? "Recycle Bin" : "Trash") + "?");
		}
		
		props.setOptions(MessageDialogProperties.okCancelOptions);
		int retVal = NativeDialogs.showMessageDialog(props);
		
		final Project project = getWindow().getProject();
		final String corpus = getWindow().getSelectedCorpus();
		if(retVal == 0) {
			for(String sessionName:sessionNames) {
				try {
					UUID writeLock = project.getSessionWriteLock(corpus, sessionName);
					project.removeSession(corpus, sessionName, writeLock);
					project.releaseSessionWriteLock(corpus, sessionName, writeLock);
				} catch (IOException e) {
					LogUtil.warning(e);
					Toolkit.getDefaultToolkit().beep();
					showMessage("Delete Session", e.getLocalizedMessage());
				}
			}
			getWindow().getSessionList().clearSelection();
		}
	}

}
