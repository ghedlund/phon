/*
 * Phon - An open source tool for research in phonology.
 * Copyright (C) 2008 The Phon Project, Memorial University <http://phon.ling.mun.ca>
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
package ca.phon.app.menu;

import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEditSupport;

import ca.phon.app.menu.edit.CopyCommand;
import ca.phon.app.menu.edit.CutCommand;
import ca.phon.app.menu.edit.EditMenuListener;
import ca.phon.app.menu.edit.PasteCommand;
import ca.phon.app.menu.edit.PreferencesCommand;
import ca.phon.app.menu.edit.RedoCommand;
import ca.phon.app.menu.edit.UndoCommand;
import ca.phon.app.menu.file.ExitCommand;
import ca.phon.app.menu.file.RecentProjectsMenuListener;
import ca.phon.app.menu.file.WorkspaceCommand;
import ca.phon.app.menu.help.HelpCommand;
import ca.phon.app.menu.help.LogCommand;
import ca.phon.app.menu.query.QueryMenuListener;
import ca.phon.app.menu.tools.IpaMapCommand;
import ca.phon.app.menu.tools.LanguageCodesCommand;
import ca.phon.app.menu.window.OpenWindowsMenuListener;
import ca.phon.app.workspace.WorkspaceDialog;
import ca.phon.extensions.IExtendable;
import ca.phon.plugin.IPluginMenuFilter;
import ca.phon.plugin.PluginAction;
import ca.phon.ui.CommonModuleFrame;

/**
 * Create the default menu for all Phon windows.
 * 
 */
public class DefaultMenuFilter implements IPluginMenuFilter {

	private final static Logger LOGGER = Logger
			.getLogger(DefaultMenuFilter.class.getName());
	
	@Override
	public void filterWindowMenu(Window owner, JMenuBar menu) {
		addFileMenu(owner, menu);
		addEditMenu(owner, menu);
		addQueryMenu(owner, menu);
		addToolsMenu(owner, menu);
		addPluginMenu(owner, menu);
		addWindowMenu(owner, menu);
		addHelpMenu(owner, menu);
	}
	
	/**
	 * Add 'File' menu
	 */
	protected void addFileMenu(Window owner, JMenuBar menu) {
		JMenu fileMenu = new JMenu("File");
		
		// start dialog item
		final JMenuItem workspaceItem = new JMenuItem(new WorkspaceCommand());
		fileMenu.add(workspaceItem);
		
		fileMenu.addSeparator();
		
		// recent menu
		JMenu recentsMenu = new JMenu("Recent projects");
		recentsMenu.addMenuListener(recentsMenuListener);
		fileMenu.add(recentsMenu);
		
		// exit item
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
		JMenu editMenu = new JMenu("Edit");
		
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
		if(!(frame instanceof WorkspaceDialog)) {
			final JMenu queryMenu  = new JMenu("Query");
			queryMenu.addMenuListener(queryMenuListener);
			menu.add(queryMenu);
		}
	}
	
//	private class SearchAction extends AbstractAction {
//		
//		private Project project;
//		private QueryScript script;
//		
//		public SearchAction(Project project, QueryScript script) {
//			super();
//			this.project = project;
//			this.script = script;
//			
//			final QueryName queryName = script.getExtension(QueryName.class);
//			if(queryName != null) {
//				String scriptName = queryName.getName();
//				if(scriptName.indexOf('.') > 0) 
//					scriptName = scriptName.substring(0, scriptName.lastIndexOf('.'));
//				
//				super.putValue(NAME, scriptName + "...");
//			}
//		}
//
//		@Override
//		public void actionPerformed(ActionEvent e) {
//			final QueryName queryName = script.getExtension(QueryName.class);
//			final QueryEditorWindow scriptFrame = new QueryEditorWindow(queryName.getName(), project, script);
//			scriptFrame.setWindowName(queryName.getName());
////			scriptFrame.openFromFile(script.getAbsolutePath(), false);
//			scriptFrame.pack();
//			scriptFrame.setLocationByPlatform(true);
//			scriptFrame.setVisible(true);
//		}
//		
//	}
	
	/**
	 * Add 'Tools' menu
	 */
	protected void addToolsMenu(Window owner, JMenuBar menu) {
		JMenu toolsMenu = new JMenu("Tools");
		
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
		JMenu windowMenu = new JMenu("Window");
		final MenuListener openWindowMenuListener = new OpenWindowsMenuListener(owner);
		windowMenu.addMenuListener(openWindowMenuListener);
		
		final MenuEvent me = new MenuEvent(windowMenu);
		openWindowMenuListener.menuSelected(me);
		
		// keep on tops
		menu.add(windowMenu);
	}
	
	/**
	 * Add 'Plugin' menu
	 */
	protected void addPluginMenu(Window owner, JMenuBar menu) {
		JMenu pluginMenu = new JMenu("Plugins");
		menu.add(pluginMenu);
	}
	
	/**
	 * Add 'Help' menu
	 */
	protected void addHelpMenu(Window owner, JMenuBar menu) {
		JMenu helpMenu = new JMenu("Help");
		
		// log
		final JMenuItem logItem = new JMenuItem(new LogCommand());
		helpMenu.add(logItem);
		
		helpMenu.addSeparator();
		
		// about
		final JMenuItem aboutItem = new JMenuItem(new HelpCommand());
		helpMenu.add(aboutItem);
		
		menu.add(helpMenu);
	}
	
	/**
	 * Recents menu generator
	 */
	private final MenuListener recentsMenuListener = new RecentProjectsMenuListener();
	
	/**
	 * Query menu generator
	 */
	private final MenuListener queryMenuListener = new QueryMenuListener();
	
}
