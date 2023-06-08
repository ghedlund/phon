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

import ca.phon.session.Tier;

/**
 * Listener interface for tier editors.
 *
 */
public interface TierEditorListener<T> {
	
	/**
	 * Called when the value of a tier has changed and focus has left the tier editor.
	 * 
	 * @param tier
	 * @param newValue
	 * @param oldValue
	 * @param valueIsAdjusting if value is currently in the middle of multiple adjustments
	 */
	public void tierValueChanged(Tier<T> tier, T newValue, T oldValue, boolean valueIsAdjusting);

}
