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
package ca.phon.app.session.editor.undo;

import ca.phon.app.session.editor.*;
import ca.phon.session.Tier;

/**
 * A change to the value of a group in a tier.
 * 
 */
public class TierEdit<T> extends SessionEditorUndoableEdit {
	
	private static final long serialVersionUID = -3236844601334798650L;

	/**
	 * tier
	 */
	private final Tier<T> tier;
	
	/**
	 * Old value
	 */
	private T oldValue;
	
	/**
	 * New value
	 */
	private final T newValue;
	
	/**
	 * Tells this edit to fire a 'hard' change on undo.
	 * A 'hard' change calls TIER_CHANGED_EVENT after TIER_CHANGE_EVENT
	 */
	private boolean fireHardChangeOnUndo = false;
	
	/**
	 * Constructor 
	 * 
	 * @param editor
	 * @param tier
	 * @param groupIndex
	 * @param newValue
	 */
	public TierEdit(SessionEditor editor, Tier<T> tier, T newValue) {
		super(editor);
		this.tier = tier;
		this.newValue = newValue;
	}
	
	@Override
	public String getUndoPresentationName() {
		return "Undo edit tier " + tier.getName();
	}
	
	@Override
	public String getRedoPresentationName() {
		return "Redo edit tier " + tier.getName();
	}

	public T getOldValue() {
		return oldValue;
	}

	public void setOldValue(T oldValue) {
		this.oldValue = oldValue;
	}

	public Tier<T> getTier() {
		return tier;
	}

	public T getNewValue() {
		return newValue;
	}
	
	public boolean isFireHardChangeOnUndo() {
		return fireHardChangeOnUndo;
	}

	public void setFireHardChangeOnUndo(boolean fireHardChangeOnUndo) {
		this.fireHardChangeOnUndo = fireHardChangeOnUndo;
	}

	@Override
	public void undo() {
		super.undo();

		final T oldVal = getOldValue();
		tier.setValue(oldVal);
		
		if(getEditor() != null) {
			final EditorEventType.TierChangeData tcd = new EditorEventType.TierChangeData(tier, newValue, oldVal);
			final EditorEvent<EditorEventType.TierChangeData> tierChangeEvt =
					new EditorEvent<>(EditorEventType.TierChange, getEditor(), tcd);
			getEditor().getEventManager().queueEvent(tierChangeEvt);
			if(isFireHardChangeOnUndo()) {
				final EditorEvent<EditorEventType.TierChangeData> tierChangedEvt =
						new EditorEvent<>(EditorEventType.TierChange, getEditor(), tcd);
				getEditor().getEventManager().queueEvent(tierChangedEvt);
			}
		}
	}
	
	@Override
	public void doIt() {
		Tier<T> tier = getTier();
		T newValue = getNewValue();
		tier.setValue(newValue);

		if(getEditor() != null) {
			final EditorEventType.TierChangeData tcd = new EditorEventType.TierChangeData(tier, getOldValue(), newValue);
			final EditorEvent<EditorEventType.TierChangeData> tierChangeEvt =
					new EditorEvent<>(EditorEventType.TierChange, getEditor(), tcd);
			getEditor().getEventManager().queueEvent(tierChangeEvt);
			if(isFireHardChangeOnUndo()) {
				final EditorEvent<EditorEventType.TierChangeData> tierChangedEvt =
						new EditorEvent<>(EditorEventType.TierChange, getEditor(), tcd);
				getEditor().getEventManager().queueEvent(tierChangedEvt);
			}
		}
	}

}
