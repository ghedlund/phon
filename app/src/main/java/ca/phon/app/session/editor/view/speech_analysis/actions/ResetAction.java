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
package ca.phon.app.session.editor.view.speech_analysis.actions;

import java.awt.event.*;

import javax.swing.*;

import ca.phon.app.session.editor.*;
import ca.phon.app.session.editor.view.speech_analysis.*;
import ca.phon.util.icons.*;

public class ResetAction extends SpeechAnalysisEditorViewAction {

	private static final long serialVersionUID = 2541481642552447379L;
	
	private final static String CMD_NAME = "Reset";
	
	private final static String SHORT_DESC = "Fit segement to view";
	
	private final static ImageIcon ICON =
			IconManager.getInstance().getIcon("actions/reload", IconSize.SMALL);

	public ResetAction(SessionEditor editor, SpeechAnalysisEditorView view) {
		super(editor, view);
		
		putValue(NAME, CMD_NAME);
		putValue(SHORT_DESCRIPTION, SHORT_DESC);
		putValue(SMALL_ICON, ICON);
	}

	@Override
	public void hookableActionPerformed(ActionEvent e) {
		getView().update();
		for(SpeechAnalysisTier tier:getView().getPluginTiers()) {
			tier.onRefresh(); 
		}
	}

}
