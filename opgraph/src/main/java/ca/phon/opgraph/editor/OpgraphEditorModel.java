package ca.phon.opgraph.editor;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import ca.gedge.opgraph.OpGraph;
import ca.gedge.opgraph.app.GraphEditorModel;
import ca.gedge.opgraph.app.extensions.NodeSettings;
import ca.phon.plugin.IPluginExtensionFactory;
import ca.phon.plugin.IPluginExtensionPoint;
import ca.phon.plugin.PluginManager;

/**
 * Base model for the opgraph editor.
 */
public abstract class OpgraphEditorModel extends GraphEditorModel {
	
	private Map<String, JComponent> viewMap;
	
	public OpgraphEditorModel() {
		this(new OpGraph());
	}

	public OpgraphEditorModel(OpGraph opgraph) {
		super();
		
		getDocument().reset(null, opgraph);
		
		viewMap = new TreeMap<>();
		final JPanel canvasPanel = new JPanel(new BorderLayout());
		canvasPanel.add(getBreadcrumb(), BorderLayout.NORTH);
		canvasPanel.add(new JScrollPane(getCanvas()), BorderLayout.CENTER);
		viewMap.put("Canvas", canvasPanel);
		viewMap.put("Console", getConsolePanel());
		viewMap.put("Debug", getDebugInfoPanel());
		viewMap.put("Defaults", getNodeDefaults());
		viewMap.put("Library", getNodeLibrary());
		viewMap.put("Settings", getNodeSettings());
	}
	
	/**
	 * Return a list of all available view names.  Custom views
	 * should be added to this list by subclasses.
	 * 
	 * @return list of available view names
	 */
	public List<String> getAvailableViewNames() {
		final List<String> retVal = new ArrayList<>();
		retVal.addAll(viewMap.keySet());
		return retVal;
	}
	
	/**
	 * Get specified view component
	 * 
	 * @param viewName
	 * @return
	 */
	public JComponent getView(String viewName) {
		return viewMap.get(viewName);
	}
	
}
