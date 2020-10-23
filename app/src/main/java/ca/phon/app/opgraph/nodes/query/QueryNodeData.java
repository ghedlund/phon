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
package ca.phon.app.opgraph.nodes.query;

import java.net.*;

import ca.phon.opgraph.*;
import ca.phon.opgraph.library.*;
import ca.phon.opgraph.library.instantiators.*;
import ca.phon.query.script.*;

public class QueryNodeData extends NodeData {
	
	private QueryScript queryScript;

	public QueryNodeData(QueryScript queryScript, URI uri, String name, String description,
			String category, Instantiator<? extends OpNode> instantiator) {
		super(uri, name, description, category, instantiator);
		this.queryScript = queryScript;
	}
	
	public QueryScript getQueryScript() {
		return this.queryScript;
	}
	
	public void setQueryScript(QueryScript queryScript) {
		this.queryScript = queryScript;
	}

}
