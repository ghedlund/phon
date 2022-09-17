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
package ca.phon.app.opgraph.macro;

import ca.phon.app.opgraph.editor.OpgraphEditorModel;
import ca.phon.opgraph.OpGraph;
import ca.phon.plugin.*;

@PhonPlugin(author="Greg J. Hedlund <ghedlund@mun.ca>", minPhonVersion="2.1.0", name="Default Opgraph Editor Model")
public class MacroOpgraphEditorModelExtPt implements IPluginExtensionPoint<OpgraphEditorModel> {

	@Override
	public Class<?> getExtensionType() {
		return OpgraphEditorModel.class;
	}

	@Override
	public IPluginExtensionFactory<OpgraphEditorModel> getFactory() {
		final IPluginExtensionFactory<OpgraphEditorModel> factory = (Object ... args) -> {
			if(args.length > 0 && args[0] instanceof OpGraph) {
				return new MacroOpgraphEditorModel((OpGraph)args[0]);
			} else {
				return new MacroOpgraphEditorModel();
			}
		};
		return factory;	
	}

}
