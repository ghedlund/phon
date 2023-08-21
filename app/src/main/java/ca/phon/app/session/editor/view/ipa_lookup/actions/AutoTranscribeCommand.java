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
package ca.phon.app.session.editor.view.ipa_lookup.actions;

import ca.phon.app.session.editor.*;
import ca.phon.app.session.editor.view.ipa_lookup.*;
import ca.phon.ipadictionary.*;
import ca.phon.session.Session;
import ca.phon.worker.*;

import javax.swing.undo.UndoableEdit;
import java.awt.event.ActionEvent;
import java.util.Optional;

/**
 * Action for auto-transcribing a {@link Session} using the
 * current {@link IPADictionary}.
 */
public class AutoTranscribeCommand extends IPALookupViewAction {
	
	private final static String CMD_NAME = "Auto-transcribe Session";
	
	// TODO icon?
	
	// TODO keystroke?

	public AutoTranscribeCommand(IPALookupView view) {
		super(view);
		
		putValue(NAME, CMD_NAME);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		final SessionEditor sessionEditor = getLookupView().getEditor();
		final AutoTranscriptionDialog autoTranscribeDialog = 
				new AutoTranscriptionDialog(sessionEditor.getProject(), sessionEditor.getSession(), getLookupView().getSelectedDictionaryLanguage());
		autoTranscribeDialog.setModal(true);
		
		autoTranscribeDialog.pack();
		autoTranscribeDialog.setLocationRelativeTo(sessionEditor);
		autoTranscribeDialog.setVisible(true);
		
		if(!autoTranscribeDialog.wasCanceled()) {
			// perform auto transcription
			final PhonTask task = new PhonTask() {
				
				@Override
				public void performTask() {
					setStatus(TaskStatus.RUNNING);
					setProperty(PhonTask.PROGRESS_PROP, -1f);

					Optional<IPADictionary> dict =
							IPADictionaryLibrary.getInstance().dictionariesForLanguage(autoTranscribeDialog.getForm().getDictionaryLanguage()).stream().findAny();

					final AutoTranscriber transcriber = new AutoTranscriber(sessionEditor);
					transcriber.setDictionary(dict.orElse(getLookupView().getLookupContext().getDictionary()));
					transcriber.setOverwrite(autoTranscribeDialog.getForm().isOverwrite());
					transcriber.setSetIPAActual(autoTranscribeDialog.getForm().isSetIPAActual());
					transcriber.setSetIPATarget(autoTranscribeDialog.getForm().isSetIPATarget());
					transcriber.setRecordFilter(autoTranscribeDialog.getForm().getRecordFilter());
					transcriber.setSyllabifier(autoTranscribeDialog.getForm().getSyllabifier());
					transcriber.setTranscriber(getLookupView().getEditor().getDataModel().getTranscriber());
					
					final UndoableEdit edit = transcriber.transcribeSession(sessionEditor.getSession());
					sessionEditor.getUndoSupport().postEdit(edit);

					final EditorEvent<EditorEventType.RecordChangedData> ee =
							new EditorEvent<>(EditorEventType.RecordRefresh, sessionEditor,
									new EditorEventType.RecordChangedData(sessionEditor.currentRecord(),
											sessionEditor.getSession().getRecordElementIndex(sessionEditor.getCurrentRecordIndex()),
											sessionEditor.getCurrentRecordIndex()));
					sessionEditor.getEventManager().queueEvent(ee);
					
					setStatus(TaskStatus.FINISHED);
				}
			};
			getLookupView().getEditor().getStatusBar().watchTask(task); 
			
			PhonWorker worker = PhonWorker.createWorker();
			worker.invokeLater(task);
			worker.setFinishWhenQueueEmpty(true);
			worker.start();
		}
	}
	
}
