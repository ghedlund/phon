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
package ca.phon.query.script;

import ca.phon.query.db.*;
import ca.phon.query.db.xml.XMLQuery;
import ca.phon.script.*;
import ca.phon.script.params.*;

import java.io.*;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Defers loading of the query script until data is needed.
 * 
 */
public class LazyQueryScript extends BasicScript {

	private boolean loaded = false;
	
	private final URL scriptURL;
	
	public LazyQueryScript(String script) {
		super(script);
		scriptURL = null;
	}
	
	public LazyQueryScript(URL url) {
		super("");
		this.scriptURL = url;
		
		putExtension(QueryName.class, new QueryName(url.getFile()));
	}
	
	@Override
	public String getScript() {
		if(!loaded) {
			// load script
			readScript();
			loaded = true;
		}
		return super.getScript();
	}
	
	private void readScript() {
		if(scriptURL == null) return;
		
		try {
			final String name = scriptURL.getPath();
			if(name != null && name.trim().length() > 0) {
				if(name.endsWith(".js")) {
					readRawScript();
				} else if(name.endsWith(".xml")) {
					readXmlScript();
				} else {
					throw new IOException("Unknown query script type " + name);
				}
			}
		} catch (IOException e) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, e.getLocalizedMessage(), e);
		}
	}
	
	private void readRawScript() throws IOException {
		final InputStream in = getScriptURL().openStream();
		final BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		final StringBuffer buffer = getBuffer();
		String line = null;
		while((line = reader.readLine()) != null) {
			buffer.append(line);
			buffer.append("\n");
		}
		in.close();
	}
	
	private void readXmlScript() throws IOException {
		final InputStream in = getScriptURL().openStream();
		final QueryManager qm = QueryManager.getInstance();
		final Query q = qm.loadQuery(in);
		
		if(q instanceof XMLQuery) {
			var scriptURL = ((XMLQuery)q).getXMLObject().getScript().getUrl();
			if(scriptURL != null) {
				QueryName qn = getExtension(QueryName.class);
				qn.setScriptLibrary(ScriptLibrary.valueOf(scriptURL.getRel().name().toUpperCase()));
				qn.setName(scriptURL.getRef());
			}
		}
		
		getBuffer().append(q.getScript().getSource());
		loaded = true;
		
		// setup saved parameters
		ScriptParameters params = new ScriptParameters();
		try {
			params = getContext().getScriptParameters(getContext().getEvaluatedScope());
		} catch (PhonScriptException e) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, e.getLocalizedMessage(), e);
		}
		for(ScriptParam sp:params) {
			for(String id:sp.getParamIds()) {
				Object v = q.getScript().getParameters().get(id);
				if(v != null) {
					sp.setValue(id, v);
				}
			}
		}
	}
	
	public URL getScriptURL() {
		return scriptURL;
	}
	
}
