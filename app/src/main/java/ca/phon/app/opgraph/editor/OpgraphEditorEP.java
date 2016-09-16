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

import java.util.Map;

import javax.swing.SwingUtilities;

import ca.phon.plugin.IPluginEntryPoint;

public class OpgraphEditorEP implements IPluginEntryPoint {

	public final static String OPGRAPH_MODEL_KEY = "model";
	
	public final static String EP_NAME = "OpgraphEditor";
	
	@Override
	public String getName() {
		return EP_NAME;
	}

	@Override
	public void pluginStart(Map<String, Object> args) {
		final OpgraphEditorModel model =
				(args.containsKey(OPGRAPH_MODEL_KEY) ? (OpgraphEditorModel)args.get(OPGRAPH_MODEL_KEY) : 
					new DefaultOpgraphEditorModel());
		final Runnable onEDT = () -> {
			final OpgraphEditor editor = new OpgraphEditor(model);
			editor.pack();
			editor.setSize(1064, 768);
			editor.setLocationByPlatform(true);
			editor.setVisible(true);
		};
		if(SwingUtilities.isEventDispatchThread())
			onEDT.run();
		else
			SwingUtilities.invokeLater(onEDT);
	}

}
