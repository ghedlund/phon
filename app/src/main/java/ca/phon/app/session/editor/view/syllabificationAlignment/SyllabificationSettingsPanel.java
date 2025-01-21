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

import ca.phon.session.*;
import ca.phon.syllabifier.*;
import ca.phon.ui.SyllabifierSelector;
import ca.phon.util.Language;

import javax.swing.*;
import java.awt.*;

public class SyllabificationSettingsPanel extends JPanel {

	public final static String IPA_TARGET_SYLLABIFIER_PROP = "_ipa_target_syllabifier_";

	public final static String IPA_ACTUAL_SYLLABIFIER_PROP = "_ipa_actual_syllabifier_";

	private SyllabifierSelector ipaTargetSelector;
	private SyllabifierSelector ipaActualSelector;

	private final SyllabifierInfo syllabifierInfo;

	public SyllabificationSettingsPanel(SyllabifierInfo info) {
		super();
		this.syllabifierInfo = info;
		
		init();
	}

	private void init() {
		SyllabifierInfo info = getSyllabifierInfo();

		Language ipaTargetLang = info.getSyllabifierLanguageForTier(SystemTierType.IPATarget.getName());
		if(ipaTargetLang == null)
			ipaTargetLang = SyllabifierLibrary.getInstance().defaultSyllabifierLanguage();
		Language ipaActualLang = info.getSyllabifierLanguageForTier(SystemTierType.IPAActual.getName());
		if(ipaActualLang == null)
			ipaActualLang = SyllabifierLibrary.getInstance().defaultSyllabifierLanguage();

		ipaTargetSelector = new SyllabifierSelector();
		ipaTargetSelector.setSelectedLanguage(ipaTargetLang);
		ipaTargetSelector.addListSelectionListener( (e) -> {
			Language currentSyllabifier = info.getSyllabifierLanguageForTier(SystemTierType.IPATarget.getName());
			info.setSyllabifierLanguageForTier(SystemTierType.IPATarget.getName(), ipaTargetSelector.getSelectedSyllabifier().getLanguage());
			firePropertyChange(IPA_ACTUAL_SYLLABIFIER_PROP, currentSyllabifier, ipaTargetSelector.getSelectedSyllabifier().getLanguage() );
		});

		ipaActualSelector = new SyllabifierSelector();
		ipaActualSelector.setSelectedLanguage(ipaActualLang);
		ipaActualSelector.addListSelectionListener( (e) -> {
			Language currentSyllabifier = info.getSyllabifierLanguageForTier(SystemTierType.IPAActual.getName());
			info.setSyllabifierLanguageForTier(SystemTierType.IPAActual.getName(), ipaActualSelector.getSelectedSyllabifier().getLanguage());
			firePropertyChange(IPA_ACTUAL_SYLLABIFIER_PROP, currentSyllabifier, ipaActualSelector.getSelectedSyllabifier().getLanguage() );
		});

		SwingUtilities.invokeLater( () -> {
			ipaTargetSelector.ensureIndexIsVisible(ipaTargetSelector.getSelectedIndex());
			ipaActualSelector.ensureIndexIsVisible(ipaActualSelector.getSelectedIndex());
		});

		JScrollPane ipaTargetScroller = new JScrollPane(ipaTargetSelector);
		ipaTargetScroller.setBorder(BorderFactory.createTitledBorder("IPA Target Syllabifier"));

		JScrollPane ipaActualScroller = new JScrollPane(ipaActualSelector);
		ipaActualScroller.setBorder(BorderFactory.createTitledBorder("IPA Actual Syllabifier"));

		setLayout(new GridLayout(2, 1));
		add(ipaTargetScroller);
		add(ipaActualScroller);
	}

	public SyllabifierInfo getSyllabifierInfo() {
		return this.syllabifierInfo;
	}

	public Language getSelectedTargetSyllabifier() {
		final Syllabifier syllabifier = ipaTargetSelector.getSelectedSyllabifier();
		if(syllabifier != null) {
			return syllabifier.getLanguage();
		}
		return null;
	}
	
	public Language getSelectedActualSyllabifier() {
		final Syllabifier syllabifier = ipaActualSelector.getSelectedSyllabifier();
		if(syllabifier != null) {
			return syllabifier.getLanguage();
		}
		return null;
	}

}
