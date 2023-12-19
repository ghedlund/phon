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
package ca.phon.app.session.editor.view.ipaDictionary;

import ca.phon.app.ipalookup.OrthoWordIPAOptions;
import ca.phon.ipa.*;
import ca.phon.orthography.*;
import ca.phon.visitor.VisitorAdapter;
import ca.phon.visitor.annotation.Visits;

public class WordLookupVisitor extends VisitorAdapter<OrthographyElement> {

	/**
	 * 
	 */
	private final RecordLookupPanel recordLookupPanel;

	/**
	 * @param recordLookupPanel
	 */
	WordLookupVisitor(RecordLookupPanel recordLookupPanel) {
		this.recordLookupPanel = recordLookupPanel;
	}

	@Override
	public void fallbackVisit(OrthographyElement obj) {
	}
	
	@Visits
	public void visitWord(Word word) {
		OrthoWordIPAOptions ext = word.getExtension(OrthoWordIPAOptions.class);
		if(ext == null) ext = new OrthoWordIPAOptions();
		
		final String txt = (ext.getOptions().size() > 0 ? ext.getOptions().get(ext.getSelectedOption()) : "*");
		addWordToTier(txt);
	}
	
	@Visits
	public void visitCompoundWord(CompoundWord wordnet) {
		OrthoWordIPAOptions opt1 = wordnet.getWord1().getExtension(OrthoWordIPAOptions.class);
		if(opt1 == null) opt1 = new OrthoWordIPAOptions();
		OrthoWordIPAOptions opt2 = wordnet.getWord2().getExtension(OrthoWordIPAOptions.class);
		if(opt2 == null) opt2 = new OrthoWordIPAOptions();
		
		final String t1 = (opt1.getOptions().size() > 0 ? opt1.getOptions().get(opt1.getSelectedOption()) : "*");
		final String t2 = (opt2.getOptions().size() > 0 ? opt2.getOptions().get(opt2.getSelectedOption()) : "*");
		addWordToTier(t1 + wordnet.getMarker().toString() + t2);
	}
	
	private void addWordToTier(String txt) {
		IPATranscript ipa = recordLookupPanel.lookupTier.hasValue() ? recordLookupPanel.lookupTier.getValue() : new IPATranscript();
		final IPATranscriptBuilder builder = new IPATranscriptBuilder();
		builder.append(ipa);
		if(builder.size() > 0)
			builder.appendWordBoundary();
		builder.append(txt, true);
		recordLookupPanel.lookupTier.setValue(builder.toIPATranscript());
	}

}
