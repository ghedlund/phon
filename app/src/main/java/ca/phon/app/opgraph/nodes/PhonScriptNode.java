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
package ca.phon.app.opgraph.nodes;

import java.awt.Component;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.mozilla.javascript.Scriptable;

import ca.gedge.opgraph.InputField;
import ca.gedge.opgraph.OpContext;
import ca.gedge.opgraph.OpNode;
import ca.gedge.opgraph.OpNodeInfo;
import ca.gedge.opgraph.OutputField;
import ca.gedge.opgraph.app.GraphDocument;
import ca.gedge.opgraph.app.extensions.NodeSettings;
import ca.gedge.opgraph.exceptions.ProcessingException;
import ca.gedge.opgraph.nodes.general.script.InputFields;
import ca.gedge.opgraph.nodes.general.script.OutputFields;
import ca.phon.app.opgraph.wizard.NodeWizard;
import ca.phon.app.query.ScriptPanel;
import ca.phon.plugin.PluginManager;
import ca.phon.script.BasicScript;
import ca.phon.script.PhonScript;
import ca.phon.script.PhonScriptContext;
import ca.phon.script.PhonScriptException;
import ca.phon.script.params.ScriptParam;
import ca.phon.script.params.ScriptParameters;

@OpNodeInfo(
		name="Script",
		category="General",
		description="Generic script node with optional parameter setup.",
		showInLibrary=true
)
public class PhonScriptNode extends OpNode implements NodeSettings {
	
	private final static Logger LOGGER = Logger.getLogger(PhonScriptNode.class.getName());
	
	private PhonScript script;
	
	private ScriptPanel scriptPanel;
	
	private InputField paramsInputField = new InputField("parameters", "Map of query parameters, these will override query settings.",
			true, true, Map.class);
	
	private OutputField paramsOutputField = new OutputField("parameters",
			"Parameters used for query, including those entered using the settings dialog", true, Map.class);
	
	public PhonScriptNode() {
		this("");
	}
	
	public PhonScriptNode(String text) {
		this(new BasicScript(text));
	}
	
	public PhonScriptNode(PhonScript script) {
		super();
		
		this.script = script;
		addQueryLibrary();
		
		putField(paramsInputField);
		putField(paramsOutputField);
		
		putExtension(NodeSettings.class, this);
	}
	
	private void reloadFields() {
		final PhonScript phonScript = getScript();
		final PhonScriptContext scriptContext = phonScript.getContext();
		
		final List<InputField> fixedInputs =
				getInputFields().stream().filter( f -> f.isFixed() && f != ENABLED_FIELD ).collect( Collectors.toList() );
		final List<OutputField> fixedOutputs =
				getOutputFields().stream().filter( OutputField::isFixed ).collect( Collectors.toList() );
		
		removeAllInputFields();
		removeAllOutputFields();
		
		for(InputField field:fixedInputs) {
			putField(field);
		}
		for(OutputField field:fixedOutputs) {
			putField(field);
		}
		
		try {
			final Scriptable scope = scriptContext.getEvaluatedScope();
			scriptContext.installParams(scope);
			
			final InputFields inputFields = new InputFields(this);
			final OutputFields outputFields = new OutputFields(this);
			scriptContext.callFunction(scope, "init", inputFields, outputFields);
		} catch (PhonScriptException e) {
			LOGGER.fine(e.getLocalizedMessage());
		}
	}

	@Override
	public void operate(OpContext context) throws ProcessingException {
		final PhonScript phonScript = getScript();
		final PhonScriptContext ctx = phonScript.getContext();
		
		ScriptParameters scriptParams = new ScriptParameters();
		try {
			scriptParams = ctx.getScriptParameters(ctx.getEvaluatedScope());
		} catch (PhonScriptException e) {
			throw new ProcessingException(null, e);
		}
		
		final Map<?, ?> inputParams = (Map<?,?>)context.get(paramsInputField);
		final Map<String, Object> allParams = new LinkedHashMap<>();
		for(ScriptParam sp:scriptParams) {
			for(String paramId:sp.getParamIds()) {
				if(inputParams != null && inputParams.containsKey(paramId)) {
					sp.setValue(paramId, inputParams.get(paramId));
				}
				
				if(paramId.endsWith("ignoreDiacritics")
						&& context.containsKey(NodeWizard.IGNORE_DIACRITICS_GLOBAL_OPTION)) {
					sp.setValue(paramId, context.get(NodeWizard.IGNORE_DIACRITICS_GLOBAL_OPTION));
				}
				
				if(paramId.endsWith("caseSensitive")
						&& context.containsKey(NodeWizard.CASE_SENSITIVE_GLOBAL_OPTION)) {
					sp.setValue(paramId, context.get(NodeWizard.CASE_SENSITIVE_GLOBAL_OPTION));
				}
				
				allParams.put(paramId, sp.getValue(paramId));
			}
		}
		
		// ensure query form validates (if available)
		if(scriptPanel != null && !scriptPanel.checkParams()) {
			throw new ProcessingException(null, "Invalid settings");
		}
		
		try {
			final Scriptable scope = ctx.getEvaluatedScope();
			ctx.installParams(scope);
			
			ctx.callFunction(scope, "run", context);
		} catch (PhonScriptException e) {
			LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
		
		context.put(paramsOutputField, allParams);
	}
	
	public PhonScript getScript() {
		return this.script;
	}

	@Override
	public Component getComponent(GraphDocument document) {
		if(this.scriptPanel == null) {
			this.scriptPanel = new ScriptPanel(getScript());
		}
		return this.scriptPanel;
	}

	@Override
	public Properties getSettings() {
		final Properties retVal = new Properties();
		
		retVal.setProperty("__script", getScript().getScript());
		
		try {
			final ScriptParameters scriptParams = getScript().getContext().getScriptParameters(
					getScript().getContext().getEvaluatedScope());
			for(ScriptParam param:scriptParams) {
				if(param.hasChanged()) {
					for(String paramId:param.getParamIds()) {
						retVal.setProperty(paramId, param.getValue(paramId).toString());
					}
				}
			}
		} catch (PhonScriptException e) {
			LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
		
		return retVal;
	}
	
	/**
	 * Make query library functions available to scripts.
	 * 
	 */
	private void addQueryLibrary() {
		script.addPackageImport("Packages.ca.phon.session");
		script.addPackageImport("Packages.ca.phon.project");
		script.addPackageImport("Packages.ca.phon.ipa");
		script.addPackageImport("Packages.ca.phon.query");
		script.addPackageImport("Packages.ca.phon.query.report");
		script.addPackageImport("Packages.ca.phon.query.report.datasource");
		
		final ClassLoader cl = PluginManager.getInstance();
		Enumeration<URL> libUrls;
		try {
			libUrls = cl.getResources("ca/phon/query/script/");
			while(libUrls.hasMoreElements()) {
				final URL url = libUrls.nextElement();
				try {
					final URI uri = url.toURI();
					script.addRequirePath(uri);
				} catch (URISyntaxException e) {
					LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
				}
			}
		} catch (IOException e1) {
			LOGGER.log(Level.SEVERE, e1.getLocalizedMessage(), e1);
		}
	}

	@Override
	public void loadSettings(Properties properties) {
		if(properties.containsKey("__script")) {
			this.script = new BasicScript(properties.getProperty("__script"));
			addQueryLibrary();
			if(scriptPanel != null)
				scriptPanel.setScript(this.script);
			reloadFields();
			
			try {
				final ScriptParameters scriptParams = getScript().getContext().getScriptParameters(
						getScript().getContext().getEvaluatedScope());
				for(ScriptParam param:scriptParams) {
					for(String paramId:param.getParamIds()) {
						if(properties.containsKey(paramId)) {
							param.setValue(paramId, properties.get(paramId));
						}
					}
				}
			} catch (PhonScriptException e) {
				LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);	
			}
		}
	}

}
