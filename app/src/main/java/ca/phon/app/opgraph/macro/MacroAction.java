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
package ca.phon.app.opgraph.macro;

import ca.phon.app.hooks.HookableAction;
import ca.phon.app.log.LogUtil;
import ca.phon.opgraph.OpGraph;
import ca.phon.opgraph.app.OpgraphIO;
import ca.phon.project.Project;
import ca.phon.worker.PhonWorker;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutionException;

public class MacroAction extends HookableAction {

	private Project project;
	
	private URL analysisURL;
	
	private boolean showWizard = true;
	
	public MacroAction(Project project, URL analysisURL) {
		super();
		
		this.project = project;
		this.analysisURL = analysisURL;
		
		@SuppressWarnings("deprecation")
		String name = URLDecoder.decode(analysisURL.getPath());
		if(name.endsWith(".xml")) name = name.substring(0, name.length()-4);
		if(name.endsWith(".opgraph")) name = name.substring(0, name.length()-8);
		final File asFile = new File(name);
		putValue(NAME, asFile.getName());
		putValue(SHORT_DESCRIPTION, analysisURL.getPath());
	}
	
	public boolean isShowWizard() {
		return showWizard;
	}

	public void setShowWizard(boolean showWizard) {
		this.showWizard = showWizard;
	}

	@Override
	public void hookableActionPerformed(ActionEvent ae) {
		final MacroWorker worker = new MacroWorker();
		worker.run();
	}
	
	private OpGraph loadMacro() throws IOException {
		return OpgraphIO.read(analysisURL.openStream());
	}

	private class MacroWorker extends SwingWorker<OpGraph, Object> {

		@Override
		protected OpGraph doInBackground() throws Exception {
			final OpGraph graph = loadMacro();
			return graph;
		}

		@Override
		protected void done() {
			try {
				final MacroRunner analysisRunner =
						new MacroRunner(get(), project, showWizard);
				PhonWorker.getInstance().invokeLater(analysisRunner);
			} catch (ExecutionException | InterruptedException e) {
				LogUtil.warning(e);
			}
		}
		
		
	}
	
}
