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

import ca.phon.app.session.editor.SessionEditor;
import ca.phon.app.session.editor.undo.TierEdit;
import ca.phon.app.session.editor.view.syllabificationAlignment.*;
import ca.phon.ipa.IPATranscript;
import ca.phon.ipa.alignment.PhoneMap;
import ca.phon.plugin.*;
import ca.phon.session.Record;
import ca.phon.session.*;
import ca.phon.syllabifier.*;
import ca.phon.ui.ipa.*;

import java.beans.*;

/**
 * Editor for IPATranscript tiers 
 */
@TierEditorInfo(type=IPATranscript.class)
public class IPATierEditorExtension implements IPluginExtensionPoint<TierEditor<IPATranscript>> {

	@Override
	public Class<?> getExtensionType() {
		return TierEditor.class;
	}

	@Override
	public IPluginExtensionFactory<TierEditor<IPATranscript>> getFactory() {
		return factory;
	}
	
	private final IPluginExtensionFactory<TierEditor<IPATranscript>> factory = args -> {
		final SessionEditor editor = SessionEditor.class.cast(args[TierEditorFactory.EDITOR]);
		final Record record = Record.class.cast(args[TierEditorFactory.RECORD]);
		final Tier<?> tier = Tier.class.cast(args[TierEditorFactory.TIER]);

		if(tier.getDeclaredType() != IPATranscript.class) {
			throw new IllegalArgumentException("Tier type must be " + IPATranscript.class.getName());
		}

		@SuppressWarnings("unchecked")
		final Tier<IPATranscript> ipaTier = (Tier<IPATranscript>)tier;

		final Tier<PhoneAlignment> alignmentTier = record.getPhoneAlignmentTier();
		final PhoneAlignment alignment = alignmentTier.getValue();

		Syllabifier syllabifier = null;
		final SyllabifierInfo info = editor.getSession().getExtension(SyllabifierInfo.class);
		if(info != null && info.getSyllabifierLanguageForTier(tier.getName()) != null) {
			syllabifier = SyllabifierLibrary.getInstance().getSyllabifierForLanguage(
					info.getSyllabifierLanguageForTier(tier.getName()));
		}

		IPAGroupField retVal = new IPAGroupField(ipaTier, editor.getDataModel().getTranscriber(), syllabifier);

		final IPAFieldTooltip tooltip = new IPAFieldTooltip();
		tooltip.setAlignmentTier(alignmentTier);
		tooltip.install(retVal);
		tooltip.addPropertyChangeListener(SyllabificationDisplay.SYLLABIFICATION_PROP_ID, new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				final SyllabificationDisplay.SyllabificationChangeData newVal = (SyllabificationDisplay.SyllabificationChangeData)evt.getNewValue();
				final SyllabificationDisplay display = (SyllabificationDisplay)evt.getSource();
				final ScTypeEdit edit = new ScTypeEdit(editor, display.getTranscript(), newVal.position(), newVal.scType());
				editor.getUndoSupport().postEdit(edit);
			}

		});

		tooltip.addPropertyChangeListener(SyllabificationDisplay.HIATUS_CHANGE_PROP_ID, new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				final SyllabificationDisplay display = (SyllabificationDisplay)evt.getSource();
				final ToggleDiphthongEdit edit = new ToggleDiphthongEdit(editor, display.getTranscript(), (Integer)evt.getNewValue());
				editor.getUndoSupport().postEdit(edit);
			}

		});

		tooltip.addPropertyChangeListener(PhoneMapDisplay.ALIGNMENT_CHANGE_PROP, (evt) -> {
			final PhoneMapDisplay.AlignmentChangeData newVal = (PhoneMapDisplay.AlignmentChangeData)evt.getNewValue();
			final PhoneAlignment phoneAlignment = PhoneAlignment.fromTiers(record.getIPATargetTier(), record.getIPAActualTier());
			final int wordIndex = newVal.wordIndex();
			final PhoneMap pm = wordIndex <= phoneAlignment.getAlignments().size() ? phoneAlignment.getAlignments().get(wordIndex) : null;
			if(pm == null) return; // should not happen
			pm.setTopAlignment(newVal.alignment()[0]);
			pm.setBottomAlignment(newVal.alignment()[1]);

			final TierEdit<PhoneAlignment> edit = new TierEdit<>(editor, alignmentTier, phoneAlignment);
			edit.setFireHardChangeOnUndo(true);
			editor.getUndoSupport().postEdit(edit);
		});

		return retVal;
	};
	
}
