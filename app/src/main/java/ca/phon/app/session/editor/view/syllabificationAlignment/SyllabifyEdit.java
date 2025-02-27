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
package ca.phon.app.session.editor.view.syllabificationAlignment;

import ca.phon.app.log.LogUtil;
import ca.phon.app.session.editor.*;
import ca.phon.app.session.editor.undo.SessionUndoableEdit;
import ca.phon.ipa.IPATranscript;
import ca.phon.session.Session;
import ca.phon.session.Tier;
import ca.phon.session.Transcriber;
import ca.phon.syllabifier.Syllabifier;
import ca.phon.syllable.*;

import java.text.ParseException;

/**
 * Modifies the syllabifier used for a specific tier.
 */
public class SyllabifyEdit extends SessionUndoableEdit {

	private final Tier<IPATranscript> tier;
	
	private final Syllabifier syllabifier;
	
	private String oldVal = null;

	private Transcriber transcriber = Transcriber.VALIDATOR;
	
	public SyllabifyEdit(SessionEditor editor, Tier<IPATranscript> ipaTier, Syllabifier syllabifier) {
		this(editor.getSession(), editor.getEventManager(), ipaTier, syllabifier, Transcriber.VALIDATOR);
	}

	public SyllabifyEdit(SessionEditor editor, Tier<IPATranscript> ipaTier, Syllabifier syllabifier, Transcriber transcriber) {
		this(editor.getSession(), editor.getEventManager(), ipaTier, syllabifier, transcriber);
	}

	public SyllabifyEdit(Session session, EditorEventManager eventManager, Tier<IPATranscript> ipaTier, Syllabifier syllabifier, Transcriber transcriber) {
		super(session, eventManager);
		this.tier = ipaTier;
		this.syllabifier = syllabifier;
		this.transcriber = transcriber;
	}

	@Override
	public void undo() {
		if(oldVal == null) return;
		try {
			final IPATranscript oldTranscript = IPATranscript.parseIPATranscript(oldVal);
			final IPATranscript grp = tier.getValue();
			
			if(oldTranscript.length() != grp.length()) return;
			for(int j = 0; j < oldTranscript.length(); j++) {
				final SyllabificationInfo oldInfo = oldTranscript.elementAt(j).getExtension(SyllabificationInfo.class);
				grp.elementAt(j).putExtension(SyllabificationInfo.class, oldInfo);
			}

			final EditorEvent<SyllabificationAlignmentEditorView.ScEditData> ee =
					new EditorEvent<>(SyllabificationAlignmentEditorView.ScEdit, getSource(),
							new SyllabificationAlignmentEditorView.ScEditData(grp, -1, SyllableConstituentType.UNKNOWN, SyllableConstituentType.UNKNOWN));
			getEditorEventManager().queueEvent(ee);
		} catch (ParseException e) {
			LogUtil.severe( e.getLocalizedMessage(), e);
		}
	}

	@Override
	public void doIt() {
		final IPATranscript ipa = transcriber == Transcriber.VALIDATOR ? tier.getValue() : tier.getBlindTranscription(transcriber.getUsername());
		oldVal = ipa.toString(true);
		
		final StripSyllabifcationVisitor visitor = new StripSyllabifcationVisitor();
		ipa.accept(visitor);
		
		syllabifier.syllabify(ipa.toList());

		final EditorEvent<SyllabificationAlignmentEditorView.ScEditData> ee =
				new EditorEvent<>(SyllabificationAlignmentEditorView.ScEdit, getSource(),
						new SyllabificationAlignmentEditorView.ScEditData(ipa, -1, SyllableConstituentType.UNKNOWN, SyllableConstituentType.UNKNOWN));
		getEditorEventManager().queueEvent(ee);
	}
	
}
