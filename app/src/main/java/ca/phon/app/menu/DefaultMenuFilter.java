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
package ca.phon.app.menu;

import ca.phon.app.actions.OpenFileEP;
import ca.phon.app.menu.analysis.AnalysisMenuListener;
import ca.phon.app.menu.edit.EditMenuListener;
import ca.phon.app.menu.file.*;
import ca.phon.app.menu.help.*;
import ca.phon.app.menu.macro.MacroMenuListener;
import ca.phon.app.menu.query.QueryMenuListener;
import ca.phon.app.menu.tools.*;
import ca.phon.app.menu.window.OpenWindowsMenuListener;
import ca.phon.app.prefs.PhonProperties;
import ca.phon.app.welcome.WelcomeWindow;
import ca.phon.plugin.IPluginMenuFilter;
import ca.phon.project.Project;
import ca.phon.ui.CommonModuleFrame;
import ca.phon.ui.menu.MenuBuilder;
import ca.phon.util.PrefHelper;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;

/**
 * Create the default menu for all Phon windows.
 *
 */
public class DefaultMenuFilter implements IPluginMenuFilter {

	@Override
	public void filterWindowMenu(Window owner, JMenuBar menu) {
		addFileMenu(owner, menu);
		addEditMenu(owner, menu);
		addQueryMenu(owner, menu);
		addAnalysisMenu(owner, menu);
		addToolsMenu(owner, menu);
		addWindowMenu(owner, menu);
		addHelpMenu(owner, menu);
	}

	/**
	 * Add 'File' menu
	 */
	protected void addFileMenu(Window owner, JMenuBar menu) {
		final MenuBuilder builder = new MenuBuilder(menu);
		final JMenu fileMenu = builder.addMenu(".@^", "File");
		
		fileMenu.add(new OpenFileEP());
		JMenu recentFilesMenu = new JMenu("Recent files");
		recentFilesMenu.addMenuListener(new RecentFilesMenuListener());
		fileMenu.add(recentFilesMenu);
		
		fileMenu.addSeparator();
		
		fileMenu.add(new NewProjectCommand());
		
		fileMenu.add(new OpenProjectCommand());
		final JMenu recentProjectsMenu = new JMenu("Recent Projects");
		recentProjectsMenu.addMenuListener(new RecentProjectsMenuListener());
		fileMenu.add(recentProjectsMenu);
		
		fileMenu.addSeparator();
		
		
		fileMenu.add(new ShowApplicationDataFolderCommand());

		fileMenu.addSeparator();

		final JMenuItem exitItem = new JMenuItem(new ExitCommand());
		fileMenu.add(exitItem);

		menu.add(fileMenu);
	}

	/**
	 * Add 'Edit' menu
	 *
	 */
	protected void addEditMenu(Window owner, JMenuBar menu) {
		final MenuBuilder builder = new MenuBuilder(menu);
		JMenu editMenu = builder.addMenu(".@File", "Edit");
		
		final MenuListener editListener = new EditMenuListener(owner);
		editMenu.addMenuListener(editListener);

		final MenuEvent me = new MenuEvent(editMenu);
		editListener.menuSelected(me);

		menu.add(editMenu);
	}

	/**
	 * Add 'Query' menu
	 */
	protected void addQueryMenu(Window owner, JMenuBar menu) {
		if(!(owner instanceof CommonModuleFrame)) return;
		final CommonModuleFrame frame = (CommonModuleFrame)owner;
		if(!(frame instanceof WelcomeWindow)) {
			final MenuBuilder builder = new MenuBuilder(menu);
			final JMenu queryMenu  = builder.addMenu(".@Workspace", "Query");
			
			final QueryMenuListener queryMenuListener = new QueryMenuListener();
			queryMenu.addMenuListener(queryMenuListener);

			final MenuEvent me = new MenuEvent(queryMenu);
			queryMenuListener.menuSelected(me);

			menu.add(queryMenu);
		}
	}

	/**
	 * Add 'Assessment' menu
	 */
	protected void addAnalysisMenu(Window owner, JMenuBar menu) {
		if(!(owner instanceof CommonModuleFrame)) return;
		final CommonModuleFrame frame = (CommonModuleFrame)owner;
		final Project project = frame.getExtension(Project.class);
		if(project != null) {
			final MenuBuilder builder = new MenuBuilder(menu);
			final JMenu assessmentMenu = builder.addMenu(".@Query", "Analysis");
			
			final AnalysisMenuListener assessmentMenuListener = new AnalysisMenuListener();
			assessmentMenu.addMenuListener(assessmentMenuListener);
	
			final MenuEvent me = new MenuEvent(assessmentMenu);
			assessmentMenuListener.menuSelected(me);
	
			menu.add(assessmentMenu);
		}
	}

	/**
	 * Add 'Tools' menu
	 */
	protected void addToolsMenu(Window owner, JMenuBar menu) {
		final MenuBuilder builder = new MenuBuilder(menu);
		JMenu toolsMenu = builder.addMenu(".@Analysis", "Tools");
	
		// ipa chart
		final JMenuItem ipaItem = new JMenuItem(new IpaMapCommand());
		toolsMenu.add(ipaItem);
	
		toolsMenu.addSeparator();
	
		// language codes
		final JMenuItem langItem = new JMenuItem(new LanguageCodesCommand());
		toolsMenu.add(langItem);
	
		menu.add(toolsMenu);
	}

	/**
	 * Add 'Window' menu
	 */
	protected void addWindowMenu(Window owner, JMenuBar menu) {
		final MenuBuilder builder = new MenuBuilder(menu);
		JMenu windowMenu = builder.addMenu(".@Tools", "Window");
		
		final MenuListener openWindowMenuListener = new OpenWindowsMenuListener(owner);
		windowMenu.addMenuListener(openWindowMenuListener);

		final MenuEvent me = new MenuEvent(windowMenu);
		openWindowMenuListener.menuSelected(me);

		// keep on tops
		menu.add(windowMenu);
	}

	/**
	 * Add 'Help' menu
	 */
	protected void addHelpMenu(Window owner, JMenuBar menu) {
		final MenuBuilder builder = new MenuBuilder(menu);
	
		JMenu oldHelpMenu = builder.getMenu("Help");
		if(oldHelpMenu != null)
			menu.remove(oldHelpMenu);
		final JMenu helpMenu = builder.addMenu(".@$", "Help");
		
		for(int i = 0; i < oldHelpMenu.getMenuComponentCount(); i++) {
			helpMenu.add(oldHelpMenu.getMenuComponent(i));
		}
		
		final JMenuItem logItem = new JMenuItem(new LogCommand());
		helpMenu.add(logItem);
	
		// about
		final JMenuItem aboutItem = new JMenuItem(new HelpCommand());
		helpMenu.add(aboutItem);
	}

	/**
	 * Add macro menu
	 */
	protected void addMacroMenu(Window owner, JMenuBar menu) {
		if(!(owner instanceof CommonModuleFrame)) return;
		final CommonModuleFrame frame = (CommonModuleFrame)owner;
		final Project project = frame.getExtension(Project.class);
		if(project != null) {
			final JMenu macroMenu = new JMenu("Macro");
			final MacroMenuListener macroMenuListener = new MacroMenuListener();
			macroMenu.addMenuListener(macroMenuListener);
	
			final MenuEvent me = new MenuEvent(macroMenu);
			macroMenuListener.menuSelected(me);
	
			menu.add(macroMenu);
		}
	}

	/**
	 * Add 'Plugin' menu
	 */
	protected void addPluginMenu(Window owner, JMenuBar menu) {
		JMenu pluginMenu = new JMenu("Plugins");
		menu.add(pluginMenu);
	}

}
