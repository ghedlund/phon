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
package ca.phon.app.session.editor.view.record_data;

import ca.phon.app.session.editor.*;
import ca.phon.plugin.*;

/**
 * Provides plug-in extension point for {@link RecordDataEditorView}
 *
 */
@PhonPlugin(name=RecordDataEditorView.VIEW_NAME)
@EditorViewInfo(name=RecordDataEditorView.VIEW_NAME, category=EditorViewCategory.RECORD, icon=RecordDataEditorView.VIEW_ICON)
public class RecordDataViewExtension implements IPluginExtensionPoint<EditorView> {

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
			return new RecordDataEditorView(editor);
		}
		
	};
	
}
