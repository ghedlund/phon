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
package ca.phon.app.opgraph.editor;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Consumer;

import ca.gedge.opgraph.OpGraph;
import ca.phon.app.opgraph.nodes.query.QueryNode;
import ca.phon.app.opgraph.nodes.query.QueryNodeData;
import ca.phon.app.opgraph.nodes.query.QueryNodeInstantiator;
import ca.phon.query.script.QueryName;
import ca.phon.query.script.QueryScript;
import ca.phon.query.script.QueryScriptLibrary;

@OpgraphEditorModelInfo(name="General", description="Empty graph with default context")
public class DefaultOpgraphEditorModel extends OpgraphEditorModel {

	public DefaultOpgraphEditorModel() {
		this(new OpGraph());
	}

	public DefaultOpgraphEditorModel(OpGraph opgraph) {
		super(opgraph);
		
		addQueryNodes();
	}
	
	@Override
	public String getTitle() {
		return "Node Editor";
	}

	private void addQueryNodes() {
		Consumer<QueryScript> addToLibrary = (QueryScript script) -> {
			final QueryName qn = script.getExtension(QueryName.class);
			final String name = (qn != null ? qn.getName() : "<unknown>");
			try {
				final URI queryNodeClassURI = new URI("class", QueryNode.class.getName(), qn.getName());
				final QueryNodeInstantiator instantiator = new QueryNodeInstantiator();
				
				final String description = 
						"Add " + qn.getName() + " query to graph.";
				
				final QueryNodeData nodeData = new QueryNodeData(script, queryNodeClassURI,
						name, description, "Query", instantiator);
				getNodeLibrary().getLibrary().put(nodeData);
			} catch (URISyntaxException e) {
				
			}
		};
		
		final QueryScriptLibrary library = new QueryScriptLibrary();
		library.stockScriptFiles().forEach(addToLibrary);
		library.userScriptFiles().forEach(addToLibrary);
	}
	
}
