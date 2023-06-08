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
package ca.phon.app.session.editor.view.common;

import javax.swing.*;
import java.util.List;

/**
 * Interface used to load tier editors.
 */
public interface TierEditor<T> {

	/**
	 * Get the editor component
	 * 
	 * @return component
	 */
	public JComponent getEditorComponent();
	
	/**
	 * Add tier editor listener
	 */
	public void addTierEditorListener(TierEditorListener<T> listener);
	
	/**
	 * remove tier editor listener
	 */
	public void removeTierEditorListener(TierEditorListener<T> listener);
	
	/**
	 * Get tier editor listeners
	 */
	public List<TierEditorListener<T>> getTierEditorListeners();
	
}
