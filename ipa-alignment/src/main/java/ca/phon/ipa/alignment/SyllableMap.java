/*
 * Phon - An open source tool for research in phonology.
 * Copyright (C) 2005 - 2016, Gregory Hedlund <ghedlund@mun.ca> and Yvan Rose <yrose@mun.ca>
 * Dept of Linguistics, Memorial University <https://phon.ca>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ca.phon.ipa.alignment;

import ca.phon.alignment.AlignmentMap;
import ca.phon.ipa.IPATranscript;

/**
 *
 */
public class SyllableMap extends AlignmentMap<IPATranscript> {
	/** The target phonetic rep */
	private IPATranscript targetRep;
	/** The actual phonetic rep */
	private IPATranscript actualRep;
	
	/**
	 * Constructor
	 */
	public SyllableMap(IPATranscript targetRep, IPATranscript actualRep) {
		super();
		
		setTargetRep(targetRep);
		setActualRep(actualRep);
	}

	public IPATranscript getActualRep() {
		return actualRep;
	}

	public void setActualRep(IPATranscript actualRep) {
		this.actualRep = actualRep;
		
		this.bottomElements = actualRep.syllables().toArray(new IPATranscript[0]);
//			Syllabifier.getSyllabification(
//					actualRep.getPhones()).toArray(new Syllable[0]);
	}

	public IPATranscript getTargetRep() {
		return targetRep;
	}

	public void setTargetRep(IPATranscript targetRep) {
		this.targetRep = targetRep;
		
		this.topElements = targetRep.syllables().toArray(new IPATranscript[0]);
//			Syllabifier.getSyllabification(
//					targetRep.getPhones()).toArray(new Syllable[0]);
	}
	
}
