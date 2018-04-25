/*
 * Phon - An open source tool for research in phonology.
 * Copyright (C) 2005 - 2017, Gregory Hedlund <ghedlund@mun.ca> and Yvan Rose <yrose@mun.ca>
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
package ca.phon.app.opgraph.nodes.table;

import java.net.URI;

import ca.phon.opgraph.OpNode;
import ca.phon.opgraph.library.NodeData;
import ca.phon.opgraph.library.instantiators.Instantiator;
import ca.phon.script.PhonScript;

public class TableScriptNodeData extends NodeData {

	private PhonScript phonScript;
	
	public TableScriptNodeData(PhonScript phonScript, URI uri, String name, String description, String category,
			Instantiator<? extends OpNode> instantiator) {
		super(uri, name, description, category, instantiator);
		this.phonScript = phonScript;
	}
	
	public PhonScript getPhonScript() {
		return this.phonScript;
	}

	public void setPhonScript(PhonScript phonScript) {
		this.phonScript = phonScript;
	}
	
}
