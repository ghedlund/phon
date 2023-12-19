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
package ca.phon.app.session.editor.view.ipaDictionary.actions;

import ca.phon.app.session.editor.view.ipaDictionary.IPADictionaryView;
import ca.phon.ui.CommonModuleFrame;
import ca.phon.ui.nativedialogs.*;

import java.awt.event.ActionEvent;

public class ExportIPACommand extends IPALookupViewAction {

	private final static String CMD_NAME = "Export IPA";
	
	private final static String SHORT_DESC = "Export current IPA dictionary to CSV";
	
	// TODO icon
	
	// TODO keystroke
	
	public ExportIPACommand(IPADictionaryView view) {
		super(view);
		
		putValue(NAME, CMD_NAME);
		putValue(SHORT_DESCRIPTION, SHORT_DESC);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		final SaveDialogProperties props = new SaveDialogProperties();
		props.setParentWindow(CommonModuleFrame.getCurrentFrame());
		props.setRunAsync(false);
		props.setCanCreateDirectories(true);
		props.setFileFilter(FileFilter.csvFilter);
		props.setTitle("Save IPA dictionary");
		final String saveFile = NativeDialogs.showSaveDialog(props);
		if(saveFile != null) {
			getLookupView().getLookupContext().exportData(saveFile);
		}
	}

}
