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
package ca.phon.app.session.editor.view.syllabificationAlignment.actions;

import ca.phon.app.session.editor.SessionEditor;
import ca.phon.app.session.editor.view.syllabificationAlignment.*;
import ca.phon.ipa.IPATranscript;
import ca.phon.session.Record;
import ca.phon.session.*;
import ca.phon.syllabifier.*;
import ca.phon.util.Language;

import javax.swing.undo.CompoundEdit;
import java.awt.event.ActionEvent;

public class ResetSyllabificationCommand extends SyllabificationAlignmentCommand {
	
	private final String ipaTier;

	public ResetSyllabificationCommand(SessionEditor editor,
			SyllabificationAlignmentEditorView view, String tier) {
		super(editor, view);
		this.ipaTier = tier;
		
		putValue(NAME, "Syllabify " + ipaTier);
		putValue(SHORT_DESCRIPTION, "Syllabify " + ipaTier);
	}

	@Override
	public void hookableActionPerformed(ActionEvent e) {
		final SyllabifierInfo info = getEditor().getSession().getExtension(SyllabifierInfo.class);
		final Record r = getEditor().currentRecord();
		
		final Tier<IPATranscript> tier = 
				r.getTier(ipaTier, IPATranscript.class);
		if(tier == null) return;
		
		final SyllabifierLibrary library = SyllabifierLibrary.getInstance();
		Language syllabifierLanguage = info.getSyllabifierLanguageForTier(tier.getName());
		if(syllabifierLanguage == null)
			syllabifierLanguage = library.defaultSyllabifierLanguage();
		final Syllabifier syllabifier = library.getSyllabifierForLanguage(syllabifierLanguage);
		
		final CompoundEdit edit = new CompoundEdit();
		final SyllabifyEdit ed = new SyllabifyEdit(getEditor(), tier, syllabifier);
		ed.doIt();
		edit.addEdit(ed);
		edit.end();
		
		getEditor().getUndoSupport().postEdit(edit);
	}

}
