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

import ca.phon.app.session.editor.EditorEventManager;

import javax.swing.undo.*;
import java.lang.ref.WeakReference;

/**
 * Undo support for the {@link EditorEventManager}
 *
 */
public class SessionEditUndoSupport extends UndoableEditSupport {

	private String presentationName = "";
	
	public SessionEditUndoSupport() {
	}

	@Override
	public synchronized void postEdit(UndoableEdit e) {
		if(e instanceof SessionUndoableEdit edit) {
			edit.doIt();
		}
		super.postEdit(e);
	}

	@Override
	protected CompoundEdit createCompoundEdit() {
		return new SessionCompoundEdit(presentationName);
	}

	public void beginUpdate(String presentationName) {
		this.presentationName = presentationName;
		super.beginUpdate();
		this.presentationName = "";
	}

	public void endUpdate() {
		super.endUpdate();
	}

	private static class SessionCompoundEdit extends CompoundEdit {

		private String presentationName = "";

		public SessionCompoundEdit(String presentationName) {
			super();
			this.presentationName = presentationName;
		}

		@Override
		public String getPresentationName() {
			return presentationName;
		}

		@Override
		public String getUndoPresentationName() {
			return "Undo " + presentationName;
		}

		@Override
		public String getRedoPresentationName() {
			return "Redo " + presentationName;
		}

		@Override
		public void undo() throws CannotUndoException {
			super.undo();
		}

		@Override
		public void redo() throws CannotRedoException {
			super.redo();
		}
	}

}
