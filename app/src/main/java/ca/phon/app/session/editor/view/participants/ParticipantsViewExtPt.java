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
package ca.phon.app.session.editor.view.participants;

import ca.phon.app.session.ViewPosition;
import ca.phon.app.session.editor.*;
import ca.phon.plugin.*;
import ca.phon.util.icons.IconManager;

@PhonPlugin(name="Participants")
@EditorViewInfo(name="Participants",
		category=EditorViewCategory.SESSION,
		dockPosition = ViewPosition.LEFT_TOP,
		icon=ParticipantsView.VIEW_ICON)
public class ParticipantsViewExtPt implements IPluginExtensionPoint<EditorView> {

	@Override
	public Class<?> getExtensionType() {
		return EditorView.class;
	}

	@Override
	public IPluginExtensionFactory<EditorView> getFactory() {
		return factory;
	}

	private final IPluginExtensionFactory<EditorView> factory = new IPluginExtensionFactory<EditorView>() {
		
		@Override
		public EditorView createObject(Object... args) {
			if(args.length != 1 || !(args[0] instanceof SessionEditor)) {
				throw new IllegalArgumentException("Arguments must include SessionEditor reference.");
			}
			final SessionEditor editor = (SessionEditor)args[0];
			return new ParticipantsView(editor);
		}
		
	};
	
}
