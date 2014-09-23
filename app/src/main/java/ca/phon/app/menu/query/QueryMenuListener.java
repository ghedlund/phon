package ca.phon.app.menu.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import ca.phon.project.Project;
import ca.phon.query.script.QueryName;
import ca.phon.query.script.QueryScript;
import ca.phon.query.script.QueryScriptLibrary;
import ca.phon.ui.CommonModuleFrame;
import ca.phon.util.resources.ResourceLoader;

/**
 * Dynamic building of query menu
 */
public class QueryMenuListener implements MenuListener {
	
	public void menuCanceled(MenuEvent e) {
		
	}

	public void menuDeselected(MenuEvent e) {
	
	}

	public void menuSelected(MenuEvent e) {
		final JMenu queryMenu = (JMenu)e.getSource();
		queryMenu.removeAll();
		
		final CommonModuleFrame currentFrame = CommonModuleFrame.getCurrentFrame();
		if(currentFrame == null) return;
		final Project project = CommonModuleFrame.getCurrentFrame().getExtension(Project.class);
		if(project == null) return;

		final QueryScriptLibrary queryScriptLibrary = new QueryScriptLibrary();
		
		// add stock scripts
		final ResourceLoader<QueryScript> stockScriptLoader = queryScriptLibrary.stockScriptFiles();
		final Iterator<QueryScript> stockScriptIterator = stockScriptLoader.iterator();
		while(stockScriptIterator.hasNext()) {
			final QueryScript qs = stockScriptIterator.next();
			
			final JMenuItem sItem = new JMenuItem(new QueryScriptCommand(project, qs));
			queryMenu.add(sItem);
		}

		// add user library scripts
		final ResourceLoader<QueryScript> userScriptLoader = queryScriptLibrary.userScriptFiles();
		final Iterator<QueryScript> userScriptIterator = userScriptLoader.iterator();
		if(userScriptIterator.hasNext()) {
			queryMenu.addSeparator();
			final JLabel lbl = new JLabel("<html><b>User Library</b></html>");
			queryMenu.add(lbl);
		}
		while(userScriptIterator.hasNext()) {
			final QueryScript qs = userScriptIterator.next();
			
			final JMenuItem sItem = new JMenuItem(new QueryScriptCommand(project, qs));
			queryMenu.add(sItem);
		}
		
		// project scripts
		final ResourceLoader<QueryScript> projectScriptLoader = queryScriptLibrary.projectScriptFiles(project);
		final Iterator<QueryScript> projectScriptIterator = projectScriptLoader.iterator();
		if(projectScriptIterator.hasNext()) {
			queryMenu.addSeparator();
			final JLabel lbl = new JLabel("<html><b>Project Library</b></html>");
			queryMenu.add(lbl);
		}
		while(projectScriptIterator.hasNext()) {
			final QueryScript qs = projectScriptIterator.next();
			
			final JMenuItem sItem = new JMenuItem(new QueryScriptCommand(project, qs));
			queryMenu.add(sItem);
		}
		
		// plug-in script
		final ResourceLoader<QueryScript> pluginScriptLoader = queryScriptLibrary.pluginScriptFiles(project);
		final Iterator<QueryScript> pluginScriptIterator = pluginScriptLoader.iterator();
		// organize into categories
		
		final Map<String, List<QueryScript>> categories = new TreeMap<String, List<QueryScript>>();
		
		while(pluginScriptIterator.hasNext()) {
			final QueryScript queryScript = pluginScriptIterator.next();
			final QueryName qn = queryScript.getExtension(QueryName.class);
			
			List<QueryScript> categoryScripts = categories.get(qn.getCategory());
			if(categoryScripts == null) {
				categoryScripts = new ArrayList<QueryScript>();
				categories.put(qn.getCategory(), categoryScripts);
			}
			categoryScripts.add(queryScript);
		}
		for(String category:categories.keySet()) {
			queryMenu.addSeparator();
			final JLabel lbl = new JLabel("<html><b>" + category + "</b></html>");
			queryMenu.add(lbl);
			for(QueryScript qs:categories.get(category)) {
				final JMenuItem sItem = new JMenuItem(new QueryScriptCommand(project, qs));
				queryMenu.add(sItem);
			}
		}
		
		final JMenuItem scriptItem = new JMenuItem(new QueryScriptEditorCommand(project));
		final JMenuItem historyItem = new JMenuItem(new QueryHistoryCommand(project));
		
		queryMenu.addSeparator();
		queryMenu.add(scriptItem);
		queryMenu.add(historyItem);
	}

}
