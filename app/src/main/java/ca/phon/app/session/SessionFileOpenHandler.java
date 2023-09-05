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
package ca.phon.app.session;

import ca.phon.app.actions.XMLOpenHandler;
import ca.phon.app.log.LogUtil;
import ca.phon.app.modules.EntryPointArgs;
import ca.phon.app.project.*;
import ca.phon.app.session.editor.*;
import ca.phon.plugin.*;
import ca.phon.project.Project;
import ca.phon.project.exceptions.ProjectConfigurationException;
import ca.phon.session.Session;
import ca.phon.session.io.*;
import ca.phon.ui.CommonModuleFrame;
import org.apache.commons.io.FilenameUtils;

import javax.xml.stream.events.StartElement;
import java.io.*;
import java.util.*;

/**
 * Open session files in Phon format. If no project is detected a temorary
 * project is created for the session editor.
 * 
 */
public class SessionFileOpenHandler implements XMLOpenHandler, IPluginExtensionPoint<XMLOpenHandler> {

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
		return Set.of("xml");
	}

	@Override
	public boolean canRead(StartElement startEle) {
		if((startEle.getName().getNamespaceURI().equals("http://phon.ling.mun.ca/ns/phonbank")
				|| startEle.getName().getNamespaceURI().equals("https://phon.ca/ns/session"))
				&& startEle.getName().getLocalPart().equals("session")) {
			return true;
		} else {
			return false;
		}
	}

	private SessionEditor findEditorForFile(File file) {
		for(CommonModuleFrame cmf:CommonModuleFrame.getOpenWindows()) {
			if(cmf instanceof SessionEditorWindow sessionEditorWindow) {
				SessionEditor editor = sessionEditorWindow.getSessionEditor();
				
				Project project = editor.getProject();
				Session session = editor.getSession();
				String sessionPath = project.getSessionPath(session);
				File sessionFile = new File(sessionPath);
				
				if(sessionFile.equals(file)) {
					return editor;
				}
			}
		}
		
		return null;
	}
	
	@Override
	public void openXMLFile(File file, Map<String, Object> args) throws IOException {
		SessionEditor existingEditor = findEditorForFile(file);
		Session session = existingEditor != null ? existingEditor.getSession() : openSession(file);
		Project project = existingEditor != null ? existingEditor.getProject() : findProjectForFile(file);
		if(existingEditor != null) {
			if (session.getName() == null || session.getName().trim().length() == 0) {
				session.setName(FilenameUtils.removeExtension(file.getName()));
			}
			if(project == null) {
				project = createTempProjectForFile(file);
			}
		}

		final EntryPointArgs epArgs = new EntryPointArgs(args);
		epArgs.put(EntryPointArgs.PROJECT_OBJECT, project);
		epArgs.put(EntryPointArgs.SESSION_OBJECT, session);
		PluginEntryPointRunner.executePluginInBackground(SessionEditorEP.EP_NAME, epArgs);
	}
	
	protected Session openSession(File file) throws IOException {
		SessionInputFactory factory = new SessionInputFactory();
		SessionReader reader = factory.createReaderForFile(file);
		Session session = reader.readSession(new FileInputStream(file));
		session.setCorpus(file.getParentFile().getName());
		return session;
	}
	
	protected Project createTempProjectForFile(File file) {
		File tmpFolder = new File(System.getProperty("java.io.tmpdir"));
		File projectFolder = new File(tmpFolder, UUID.randomUUID().toString());
		projectFolder.mkdirs();
		
		try {
			DesktopProject project = new DesktopProject(projectFolder);
			project.setName("Temp");
			project.addCorpus(file.getParentFile().getName(), "");
			project.setCorpusPath(file.getParentFile().getName(), file.getParentFile().getAbsolutePath());
			project.setCorpusMediaFolder(file.getParentFile().getName(), file.getParentFile().getAbsolutePath());
			
			return project;
		} catch (ProjectConfigurationException | IOException e) {
			LogUtil.warning(e);
		}
		return null;
	}
	
	protected Project findProjectForFile(File file) {
		File corpusFolder = file.getParentFile();
		File projectFolder = corpusFolder.getParentFile();
		
		// see if project is already open
		for(CommonModuleFrame cmf:CommonModuleFrame.getOpenWindows()) {
			Project windowProj = cmf.getExtension(Project.class);
			if(windowProj != null) {
				File windowProjFolder = new File(windowProj.getLocation());
				if(windowProjFolder.equals(projectFolder)) {
					return windowProj;
				}
			}
		}
		
		try {
			return (new DesktopProjectFactory()).openProject(projectFolder);
		} catch (IOException | ProjectConfigurationException e) {
			return null;
		}
	}

}
