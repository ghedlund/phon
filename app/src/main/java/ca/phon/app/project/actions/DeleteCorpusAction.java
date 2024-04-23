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

/**
 * Delete corpus/corpora which are currently selected
 * in the project window.
 * 
 */
public class DeleteCorpusAction extends ProjectWindowAction {
	
	public DeleteCorpusAction(ProjectWindow projectWindow) {
		super(projectWindow);
		
		putValue(Action.NAME, "Delete corpus");
		putValue(Action.SHORT_DESCRIPTION, "Delete selected corpora");
	}

	@Override
	public void hookableActionPerformed(ActionEvent ae) {
		final List<String> corpora = getWindow().getSelectedCorpora();
		if(corpora.size() == 0) return;
		
		final MessageDialogProperties props = new MessageDialogProperties();
		props.setParentWindow(CommonModuleFrame.getCurrentFrame());
		props.setRunAsync(false);
		if(corpora.size() > 1) {
			props.setHeader("Delete selected corpora?");
		} else {
			props.setHeader("Delete corpus: " + corpora.get(0));
		}
		props.setMessage("Move" +
				(corpora.size() > 1 ? " corpora" : " this corpus") + " to " +
				(OSInfo.isWindows() ? "Recycle Bin" : "Trash") + "?");
		props.setOptions(MessageDialogProperties.okCancelOptions);
		int retVal = NativeDialogs.showMessageDialog(props);
		
		final Project project = getWindow().getProject();
		if(retVal == 0) {
			getWindow().getCorpusList().clearSelection();
			for(String corpus:corpora) {
				try {
					project.removeCorpus(corpus);
				} catch (IOException e) {
					LogUtil.warning(e);
					Toolkit.getDefaultToolkit().beep();
					showMessage("Delete Corpus", e.getLocalizedMessage());
				}
			}
		}
	}

}
