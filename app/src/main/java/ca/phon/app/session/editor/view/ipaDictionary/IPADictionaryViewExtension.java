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
package ca.phon.app.session.editor.view.ipaDictionary;

import ca.phon.app.session.ViewPosition;
import ca.phon.app.session.editor.*;
import ca.phon.plugin.*;

@PhonPlugin
@EditorViewInfo(name= IPADictionaryView.VIEW_NAME, icon= IPADictionaryView.VIEW_ICON, category=EditorViewCategory.UTILITIES, dockPosition = ViewPosition.RIGHT_BOTTOM)
public class IPADictionaryViewExtension implements IPluginExtensionPoint<EditorView> {

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
			final SessionEditor editor = (SessionEditor)args[0];
			return new IPADictionaryView(editor);
		}
	};

}
