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
package ca.phon.app.query;

import ca.phon.app.log.LogUtil;
import ca.phon.app.modules.EntryPointArgs;
import ca.phon.app.session.editor.*;
import ca.phon.plugin.*;
import ca.phon.project.Project;
import ca.phon.query.db.*;
import ca.phon.ui.CommonModuleFrame;
import org.apache.commons.logging.Log;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Display a result set optionally along with the
 * associated session.
 * 
 */
@PhonPlugin(name="default")
public class ResultSetEP implements IPluginEntryPoint {
	
	public final static String EP_NAME = "ResultSetViewer";

	@Override
	public String getName() {
		return EP_NAME;
	}

	@Override
	public void pluginStart(Map<String, Object> initInfo) {
		final AtomicReference<Project> projectRef = new AtomicReference<>();
		final AtomicReference<Project> tempProjectRef = new AtomicReference<>();
		final AtomicReference<Query> queryRef = new AtomicReference<>();
		final AtomicReference<ResultSet> resultSetRef = new AtomicReference<>();
		final AtomicReference<Boolean> openSessionRef = new AtomicReference<>(Boolean.TRUE);
		
		// check args
		if(initInfo.get("project") == null) {
			throw new IllegalArgumentException("project must be given");
		} else {
			projectRef.set((Project)initInfo.get("project"));
		}
		
		if(initInfo.get("tempProject") != null) {
			tempProjectRef.set((Project)initInfo.get("tempProject"));
		}
		
		// query is optional
		queryRef.set((Query)initInfo.get("query"));
		
		if(initInfo.get("resultset") == null) {
			throw new IllegalArgumentException("resultset must be given");
		} else {
			resultSetRef.set((ResultSet)initInfo.get("resultset"));
		}
		
		if(initInfo.get("opensession") != null) {
			openSessionRef.set(Boolean.parseBoolean(initInfo.get("opensession").toString()));
		}
		
		// look for an existing windows
		final AtomicReference<ResultSetEditor> windowRef = new AtomicReference<>();
		final AtomicReference<SessionEditor> sessionEditorRef = new AtomicReference<>();
		for(CommonModuleFrame cmf:CommonModuleFrame.getOpenWindows()) {
			if(windowRef.get() == null && cmf instanceof ResultSetEditor) {
				final ResultSetEditor rsViewer = (ResultSetEditor)cmf;
				final boolean sameProject = (rsViewer.getProject() == projectRef.get());
				final boolean sameQuery = 
						sameProject && (queryRef.get().getUUID().equals(rsViewer.getQuery().getUUID()));
				final boolean sameResultSet = 
						sameQuery && (resultSetRef.get().getSessionPath().equals(rsViewer.getResultSet().getSessionPath()));
				if(sameResultSet) {
					windowRef.set(rsViewer);
				}
			} else if(sessionEditorRef.get() == null && cmf instanceof SessionEditorWindow sessionEditorWindow) {
				SessionEditor editor = sessionEditorWindow.getSessionEditor();
				if(editor.getProject() == projectRef.get() && editor.getSession().getSessionPath().equals(resultSetRef.get().getSessionPath())) {
					sessionEditorRef.set(editor);
				}
			}
		}
		
		final Runnable onEDT = () -> {
			ResultSetEditor window = windowRef.get();
			if(window != null) {
				window.toFront();
				window.requestFocus();
			} else {
				window = new ResultSetEditor(projectRef.get(), queryRef.get(), resultSetRef.get());
				if(tempProjectRef.get() != null)
					window.setTempProject(tempProjectRef.get());
				
				window.setPreferredSize(new Dimension(500, 600));
				window.pack();
	
				// setup location next to editor if attached
				if(window.getEditor() != null) {
					window.positionRelativeTo(SwingConstants.RIGHT, SwingConstants.LEADING, CommonModuleFrame.getCurrentFrame());
				} else {
					window.setLocationByPlatform(true);
				}
				KeyboardFocusManager.getCurrentKeyboardFocusManager().clearFocusOwner();
				KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
				window.setVisible(true);
				window.getTable().grabFocus();
			}
		};
		
		if(openSessionRef.get()) {
			Runnable openSessionRunnable = () -> { openSession(projectRef.get(), resultSetRef.get()); };
			if(SwingUtilities.isEventDispatchThread())
				openSessionRunnable.run();
			else {
				try {
					SwingUtilities.invokeAndWait(openSessionRunnable);
				} catch(InterruptedException | InvocationTargetException e) {
					LogUtil.severe(e);
				}
			}
			
			if(sessionEditorRef.get() == null) {
				CommonModuleFrame.addNewWindowListener(this, (cmf) -> {
					if(cmf instanceof SessionEditorWindow sessionEditorWindow) {
						SessionEditor editor = sessionEditorWindow.getSessionEditor();
						if(editor.getSession().getSessionPath().equals(resultSetRef.get().getSessionPath())) {
							onEDT.run();
						}
					}
				});
			} else {
				if(SwingUtilities.isEventDispatchThread())
					onEDT.run();
				else
					SwingUtilities.invokeLater(onEDT);
			}
		} else {
			if(SwingUtilities.isEventDispatchThread())
				onEDT.run();
			else
				SwingUtilities.invokeLater(onEDT);
		}
	}
	
	private void openSession(Project project, ResultSet rs) {
		final EntryPointArgs epArgs = new EntryPointArgs();
		epArgs.put(EntryPointArgs.PROJECT_OBJECT, project);
		epArgs.put(EntryPointArgs.CORPUS_NAME, rs.getCorpus());
		epArgs.put(EntryPointArgs.SESSION_NAME, rs.getSession());
		epArgs.put("grabFocus", Boolean.FALSE);
		
		try {
			PluginEntryPointRunner.executePlugin(SessionEditorEP.EP_NAME, epArgs);
		} catch (PluginException e) {
			LogUtil.severe( e.getLocalizedMessage(), e);
		}
	}

}
