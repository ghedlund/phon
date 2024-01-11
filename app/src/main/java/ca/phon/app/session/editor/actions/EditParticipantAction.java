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
package ca.phon.app.session.editor.actions;

import ca.phon.app.session.editor.*;
import ca.phon.app.session.editor.undo.ParticipantUndoableEdit;
import ca.phon.session.Record;
import ca.phon.session.*;
import ca.phon.ui.CommonModuleFrame;
import ca.phon.ui.participant.ParticipantEditor;
import ca.phon.util.icons.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class EditParticipantAction extends SessionEditorAction {

	private final Participant participant;
	
	private final ImageIcon ICON = 
			IconManager.getInstance().getFontIcon("person_edit", IconSize.SMALL, UIManager.getColor("Button.foreground"));
	
	public EditParticipantAction(SessionEditor editor, Participant participant) {
		super(editor);
		this.participant = participant;
		
		putValue(SMALL_ICON, ICON);
		putValue(NAME, "Edit " + participant.toString() + "...");
	}

	@Override
	public void hookableActionPerformed(ActionEvent e) {
		final SessionFactory factory = SessionFactory.newFactory();
		final Participant part = factory.createParticipant();
		Participants.copyParticipantInfo(participant, part);
		
		ParticipantEditor.editParticipant(CommonModuleFrame.getCurrentFrame(), part,
				getEditor().getDataModel().getSession().getDate(),
				getEditor().getDataModel().getSession().getParticipants().otherParticipants(participant),
				(wasCanceled) -> {
					if(!wasCanceled) {
						if (!participant.getId().equals(part.getId())) {
							// XXX we need to ensure that every record is loaded
							// so that participant information changes when id is modified
							for (Record r : getEditor().getSession().getRecords()) {
								r.getSpeaker();
							}
						}
						final ParticipantUndoableEdit edit = new ParticipantUndoableEdit(getEditor(), participant, part);
						getEditor().getUndoSupport().postEdit(edit);

						final EditorEvent<EditorEventType.RecordChangedData> ee =
								new EditorEvent<>(EditorEventType.RecordRefresh, getEditor(),
										new EditorEventType.RecordChangedData(getEditor().currentRecord(),
												getEditor().getSession().getRecordElementIndex(getEditor().getCurrentRecordIndex()),
												getEditor().getCurrentRecordIndex()));
						getEditor().getEventManager().queueEvent(ee);
					}
				});
	}

}
