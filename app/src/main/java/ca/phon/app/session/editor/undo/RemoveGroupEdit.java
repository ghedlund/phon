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
import ca.phon.ipa.IPATranscript;
import ca.phon.ipa.alignment.PhoneMap;
import ca.phon.orthography.Orthography;
import ca.phon.session.Record;
import ca.phon.session.*;

import javax.swing.undo.CannotUndoException;
import java.util.*;

public class RemoveGroupEdit extends SessionEditorUndoableEdit {

	/**
	 * tier
	 */
	private final Record record;
	
	/** 
	 * group index
	 */
	private final int groupIndex;

	private final Map<String, Object> oldGroupData = new HashMap<>();
	
	public RemoveGroupEdit(SessionEditor editor, Record record, int groupIndex) {
		super(editor);
		this.record = record;
		this.groupIndex = groupIndex;
	}
	
	@Override
	public void undo() throws CannotUndoException {
		super.undo();
		
		for(String key:oldGroupData.keySet()) {
			
			final Tier<?> tier = record.getTier(key);
			if(tier != null) {
				while(tier.numberOfGroups() < groupIndex) tier.addGroup();
			}
			
			if(SystemTierType.tierFromString(key) == SystemTierType.Orthography)
				record.getOrthographyTier().addGroup(groupIndex, (Orthography)oldGroupData.get(key));
			else if(SystemTierType.tierFromString(key) == SystemTierType.IPATarget)
				record.getIPATargetTier().addGroup(groupIndex, (IPATranscript)oldGroupData.get(key));
			else if(SystemTierType.tierFromString(key) == SystemTierType.IPAActual) 
				record.getIPAActualTier().addGroup(groupIndex, (IPATranscript)oldGroupData.get(key));
			else if(SystemTierType.tierFromString(key) == SystemTierType.SyllableAlignment)
				record.getPhoneAlignmentTier().addGroup(groupIndex, (PhoneMap)oldGroupData.get(key));
			else {
				record.getTier(key, String.class).addGroup(groupIndex, (String)oldGroupData.get(key));
			}
		}

		final EditorEvent<Void> ee = new EditorEvent<>(EditorEventType.GroupListChange, getSource(), null);
		getEditor().getEventManager().queueEvent(ee);
	}

	@Override
	public void doIt() {
		oldGroupData.clear();
		oldGroupData.put(SystemTierType.Orthography.getName(), 
				(groupIndex < record.getOrthographyTier().numberOfGroups() ? record.getOrthographyTier().getGroup(groupIndex) : new Orthography()));
		oldGroupData.put(SystemTierType.IPATarget.getName(), 
				(groupIndex < record.getIPATargetTier().numberOfGroups() ? record.getIPATargetTier().getGroup(groupIndex) : new IPATranscript()));
		oldGroupData.put(SystemTierType.IPAActual.getName(), 
				(groupIndex < record.getIPAActualTier().numberOfGroups() ? record.getIPAActualTier().getGroup(groupIndex) : new IPATranscript()));
		oldGroupData.put(SystemTierType.SyllableAlignment.getName(), 
				(groupIndex < record.getPhoneAlignmentTier().numberOfGroups() ? record.getPhoneAlignmentTier().getGroup(groupIndex) : new PhoneMap()));
		
		for(String tierName:record.getUserDefinedTierNames()) {
			final Tier<String> extraTier = record.getTier(tierName, String.class);
			if(extraTier.isGrouped())
				oldGroupData.put(tierName, 
						(groupIndex < extraTier.numberOfGroups() ? extraTier.getGroup(groupIndex) : ""));
		}
		
		record.removeGroup(groupIndex);

		final EditorEvent<Void> ee = new EditorEvent<>(EditorEventType.GroupListChange, getSource(), null);
		getEditor().getEventManager().queueEvent(ee);
	}

}
