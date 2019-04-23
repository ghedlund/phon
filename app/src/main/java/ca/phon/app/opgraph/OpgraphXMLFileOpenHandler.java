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
package ca.phon.app.opgraph;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import javax.xml.stream.events.StartElement;

import ca.phon.app.actions.XMLOpenHandler;
import ca.phon.app.log.LogUtil;
import ca.phon.app.modules.EntryPointArgs;
import ca.phon.app.opgraph.analysis.AnalysisComposerEP;
import ca.phon.app.opgraph.analysis.AnalysisWizardExtension;
import ca.phon.app.opgraph.editor.OpgraphEditorEP;
import ca.phon.app.opgraph.editor.OpgraphEditorModel;
import ca.phon.app.opgraph.editor.OpgraphEditorModelFactory;
import ca.phon.app.opgraph.editor.SimpleEditorExtension;
import ca.phon.app.opgraph.report.ReportComposerEP;
import ca.phon.app.opgraph.report.ReportWizardExtension;
import ca.phon.app.opgraph.wizard.WizardExtension;
import ca.phon.opgraph.OpGraph;
import ca.phon.opgraph.app.OpgraphIO;
import ca.phon.plugin.IPluginExtensionFactory;
import ca.phon.plugin.IPluginExtensionPoint;
import ca.phon.plugin.PluginEntryPointRunner;
import ca.phon.project.Project;
import ca.phon.ui.CommonModuleFrame;

public class OpgraphXMLFileOpenHandler implements XMLOpenHandler, IPluginExtensionPoint<XMLOpenHandler> {

	@Override
	public Class<?> getExtensionType() {
		return XMLOpenHandler.class;
	}

	@Override
	public IPluginExtensionFactory<XMLOpenHandler> getFactory() {
		return (args) -> this;
	}

	@Override
	public Set<String> supportedExtensions() {
		return Set.of("xml", "opgraph");
	}

	@Override
	public boolean canRead(StartElement startEle) {
		if(startEle.getName().getNamespaceURI().equals("https://www.phon.ca/ns/opgraph")
				&& startEle.getName().getLocalPart().equals("opgraph")) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void openXMLFile(File file) throws IOException {
		try {
			final OpGraph graph = OpgraphIO.read(file);
			
			// check for simple editor extension
			SimpleEditorExtension simpleEditorExt = graph.getExtension(SimpleEditorExtension.class);
			
			if(simpleEditorExt != null) {
				openSimpleEditor(file, graph);
			} else {
				openEditor(file, graph);
			}
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	private void openSimpleEditor(File file, OpGraph graph) {
		EntryPointArgs args = new EntryPointArgs();
		Project project = CommonModuleFrame.getCurrentFrame().getExtension(Project.class);
		args.put(EntryPointArgs.PROJECT_OBJECT, project);
		
		final WizardExtension wizardType = graph.getExtension(WizardExtension.class);
		if(wizardType.getClass() == AnalysisWizardExtension.class) {
			args.put(AnalysisComposerEP.ANALYSIS_FILE, file);
			args.put(AnalysisComposerEP.ANALYSIS_GRAPH, graph);
			
			PluginEntryPointRunner.executePluginInBackground(AnalysisComposerEP.EP_NAME, args);
		} else if(wizardType.getClass() == ReportWizardExtension.class) {
			args.put(ReportComposerEP.REPORT_FILE, file);
			args.put(ReportComposerEP.REPORT_GRAPH, graph);
			
			PluginEntryPointRunner.executePluginInBackground(ReportComposerEP.EP_NAME, args);
		}
	}
	
	private void openEditor(File file, OpGraph graph) {
		final EntryPointArgs args = new EntryPointArgs();
		
		Project project = CommonModuleFrame.getCurrentFrame().getExtension(Project.class);
		args.put(EntryPointArgs.PROJECT_OBJECT, project);
		
		OpgraphEditorModelFactory factory = new OpgraphEditorModelFactory();
		try {
			OpgraphEditorModel model = factory.fromGraph(graph);
			
			args.put(OpgraphEditorEP.OPGRAPH_MODEL_KEY, model);
			args.put(OpgraphEditorEP.OPGRAPH_FILE_KEY, file);
			
			PluginEntryPointRunner.executePluginInBackground(OpgraphEditorEP.EP_NAME, args);
		} catch (ClassNotFoundException e) {
			LogUtil.severe(e);
		}
	}
	
}

