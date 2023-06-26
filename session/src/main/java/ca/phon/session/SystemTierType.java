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
package ca.phon.session;

import ca.phon.ipa.IPATranscript;
import ca.phon.orthography.Orthography;
import ca.phon.session.usertier.UserTierData;

/**
 * Tier descriptions for default tiers.
 */
public enum SystemTierType {
	Orthography("Orthography", Orthography.class),
	IPATarget("IPA Target", IPATranscript.class),
	TargetSyllables("Target Syllables", IPATranscript.class),
	IPAActual("IPA Actual", IPATranscript.class),
	ActualSyllables("Actual Syllables", IPATranscript.class),
	PhoneAlignment("Alignment", PhoneAlignment.class),
	Segment("Segment", MediaSegment.class),
	Notes("Notes", UserTierData.class);
	
	private String tierName;
	
	private Class<?> type;

	private TierAlignmentRules tierAlignmentRules;
	
	private SystemTierType(String tierName, Class<?> type) {
		this.tierName = tierName;
		this.type = type;
	}
	
	public static boolean isSystemTier(String tierName) {
		return tierFromString(tierName) != null;
	}
	
	public static SystemTierType tierFromString(String tierName) {
		for(SystemTierType t:values()) {
			if(t.getName().equals(tierName))
				return t;
		}
		
		return null;
	}

	public String getName() {
		return this.tierName;
	}

	public Class<?> getDeclaredType() {
		return this.type;
	}
	
}
