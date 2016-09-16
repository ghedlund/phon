/*
 * Phon - An open source tool for research in phonology.
 * Copyright (C) 2005 - 2016, Gregory Hedlund <ghedlund@mun.ca> and Yvan Rose <yrose@mun.ca>
 * Dept of Linguistics, Memorial University <https://phon.ca>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ca.phon.app.project.actions;

import java.awt.event.ActionEvent;
import java.util.HashMap;

import ca.phon.app.project.ProjectWindow;
import ca.phon.plugin.PluginEntryPointRunner;
import ca.phon.ui.toast.ToastFactory;

public class OpenCorpusTemplateAction extends ProjectWindowAction {

	private static final long serialVersionUID = 6335879665708654561L;
	
	private String corpus;
	
	public OpenCorpusTemplateAction(ProjectWindow projectWindow) {
		this(projectWindow, null);
	}

	public OpenCorpusTemplateAction(ProjectWindow projectWindow, String corpus) {
		super(projectWindow);
		this.corpus = corpus;
		putValue(NAME, "Open Corpus Template...");
		putValue(SHORT_DESCRIPTION, "Open template for sesssion in the selected corpus");
	}

	@Override
	public void hookableActionPerformed(ActionEvent ae) {
		final String corpus = 
				(this.corpus == null ? getWindow().getSelectedCorpus() : this.corpus);
		if(corpus == null) {
			ToastFactory.makeToast("Please select a corpus").start(getWindow().getCorpusList());
			return;
		}
		
		HashMap<String, Object> initInfo = new HashMap<String, Object>();
		initInfo.put("project", getWindow().getProject());
		initInfo.put("corpusName", corpus);

		PluginEntryPointRunner.executePluginInBackground("CorpusTemplate", initInfo);
	}

}
