/*
 * Copyright (C) 2012-2018 Gregory Hedlund & Yvan Rose
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
package ca.phon.syllable.phonex;

import java.util.List;

import ca.phon.ipa.IPAElement;
import ca.phon.ipa.features.FeatureSet;
import ca.phon.phonex.PhoneMatcher;
import ca.phon.syllable.SyllabificationInfo;

/**
 * Matcher for tone information in {@link SyllabificationInfo}
 *
 */
public class ToneMatcher implements PhoneMatcher {
	
	private boolean isNot = false;
	
	private List<String> toneNumbers;
	
	public ToneMatcher(List<String> toneNumbers, boolean isNot) {
		this.toneNumbers = toneNumbers;
		this.isNot = isNot;
	}

	@Override
	public boolean matches(IPAElement p) {
		boolean retVal = false;
		
		final SyllabificationInfo info = p.getExtension(SyllabificationInfo.class);
		if(info != null) {
			boolean contains = toneNumbers.contains(info.getToneNumber());
			retVal = (isNot ? !contains : contains);
		}
		
		return retVal;
	}

	@Override
	public boolean matchesAnything() {
		return false;
	}

}
