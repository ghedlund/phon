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
package ca.phon.app.autosave;

import ca.phon.app.log.LogUtil;
import ca.phon.app.session.editor.SessionEditorWindow;
import ca.phon.project.Project;
import ca.phon.session.Session;
import ca.phon.ui.CommonModuleFrame;

import javax.swing.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.Calendar;

/**
 * Handles timer and performs actions for autosave of sessions.
 * 
 * Autosave timer interval is read from a user defined property.
 * Default is 10 mins.
 */
public class AutosaveManager {
	
	/**
	 * Autosave property
	 */
	public static final String AUTOSAVE_INTERVAL_PROP = AutosaveManager.class.getName() + ".interval";
	
	public static final String AUTOSAVE_PREFIX = "__autosave_";
	
	/**
	 * Default interval (seconds)
	 */
	public static final int defaultInterval = (5*60);
	
	/**
	 * Interval (miliseconds)
	 */
	private long interval = 0;
	
	/**
	 * Timer
	 */
	private Timer timer;
	
	/**
	 * singleton instance
	 */
	private static AutosaveManager _instance;
	
	/*
	 * Get the shared instance
	 */
	public static AutosaveManager getInstance() {
		if(_instance == null) {
			_instance = new AutosaveManager();
		//	_instance.setInterval(defaultInterval);
		}
		return _instance;
	}
	
	/**
	 * (Hidden) constructor
	 */
	private AutosaveManager() {
	}
	
	/**
	 * Set the timer interval in seconds.
	 */
	public void setInterval(int sec) {
		setInterval((long)(sec*1000));
	}
	
	/**
	 * Set the timer interval in miliseconds.
	 * Starts the timer if not started.
	 * 
	 * @param ms the interval for performing
	 * the autosave action.  Setting value to 0
	 * zero will stop the timer.
	 */
	public void setInterval(long ms) {
		interval = ms;
		
		if(interval == 0) {
			if(timer != null) {
				LogUtil.info("Stopping autosave manager.");
				timer.stop();
				timer = null;
			}
			return;
		}
		
		if(timer == null) {
			LogUtil.info("Starting autosave manager.");
			timer = new Timer((int)interval, new AutosaveAction());
			timer.start();
		} else {
			timer.setDelay((int)interval);
		}
		LogUtil.info("Autosaving every " + (ms/1000/60) + " minutes");
	}
	
	
	/**
	 * Autosave action
	 */
	private class AutosaveAction implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			
			LogUtil.info("Starting autosave at " + 
					Calendar.getInstance().getTime().toString() + "...");
			
			Runtime runtime = Runtime.getRuntime();
		
			// print out avail memory (may be useful)
			int freeMemory = (int)(runtime.freeMemory() / 1024);
			int totalMemory = (int)(runtime.totalMemory() / 1024);
			int usedMemory = (totalMemory - freeMemory);
			
			LogUtil.info("Java heap: " + usedMemory + "Kb / "
					+ totalMemory + "Kb, " + (usedMemory * 100 / totalMemory)
					+ "%");
			
			// find all open sessions
			for(CommonModuleFrame cmf:CommonModuleFrame.getOpenWindows()) {
				if(cmf instanceof SessionEditorWindow) {
					final SessionEditorWindow editor = (SessionEditorWindow)cmf;
					
					if(editor.hasUnsavedChanges()) {
						final Project project = editor.getProject();
						final Session session = editor.getSession();
						final Autosaves autosaves = project.getExtension(Autosaves.class);
						
						LogUtil.info("Autosaving session '" + 
								session.getCorpus() + "." + session.getName() + "'...");
						
						try {
							autosaves.createAutosave(session);
						} catch (IOException e1) {
							LogUtil.severe(
									e1.getLocalizedMessage(), e1);
						}
					}
				}
			}
			
			LogUtil.info(
					"Autosave action finished at " + Calendar.getInstance().getTime().toString());
		}
		
	}
}
