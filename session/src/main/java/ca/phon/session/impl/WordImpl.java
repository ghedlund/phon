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
package ca.phon.session.impl;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import ca.phon.ipa.IPATranscript;
import ca.phon.ipa.alignment.PhoneMap;
import ca.phon.orthography.*;
import ca.phon.session.*;

public class WordImpl implements Word {

	// record
	private final AtomicReference<Record> recordRef;

	// group
	private final int groupIndex;

	// word index
	private final int wordIndex;

	public WordImpl(Record record, int groupIndex, int wordIndex) {
		super();
		this.recordRef = new AtomicReference<Record>(record);
		this.groupIndex = groupIndex;
		this.wordIndex = wordIndex;
	}

	@Override
	public Group getGroup() {
		final Record record = recordRef.get();
		if(record != null) {
			return record.getGroup(groupIndex);
		} else {
			return null;
		}
	}

	@Override
	public int getWordIndex() {
		return this.wordIndex;
	}

	@Override
	public OrthoElement getOrthography() {
		final Orthography ortho =
				(getGroup().getOrthography() == null ? new Orthography() : getGroup().getOrthography());
		final OrthoWordExtractor extractor = new OrthoWordExtractor();
		ortho.accept(extractor);

		final List<OrthoElement> wordList = extractor.getWordList();

		if(wordIndex >= 0 && wordIndex < wordList.size()) {
			return wordList.get(wordIndex);
		} else {
			return null;
		}
	}

	@Override
	public int getOrthographyWordLocation() {
		int retVal = -1;

		final OrthoElement ele = getOrthography();
		if(ele != null) {
			final int idx = getGroup().getOrthography().indexOf(ele);
			if(idx >= 0) {
				retVal = 0;
				for(int i = 0; i < idx; i++) {
					retVal += (i > 0 ? 1 : 0) + getGroup().getOrthography().elementAt(i).toString().length();
				}
				if(idx > 0) retVal++;
			}
		}

		return retVal;
	}

	@Override
	public IPATranscript getIPATarget() {
		final IPATranscript ipaTarget =
				(getGroup().getIPATarget() == null ? new IPATranscript() : getGroup().getIPATarget());
		final List<IPATranscript> wordList = ipaTarget.words();

		if(wordIndex >= 0 && wordIndex < wordList.size()) {
			return wordList.get(wordIndex);
		} else {
			return null;
		}
	}

	@Override
	public int getIPATargetWordLocation() {
		int retVal = -1;

		final IPATranscript target = getGroup().getIPATarget();
		final IPATranscript ipa = getIPATarget();
		if(ipa != null) {
			final int eleIdx = target.indexOf(ipa);
			retVal = target.stringIndexOfElement(eleIdx);
		}

		return retVal;
	}

	@Override
	public IPATranscript getIPAActual() {
		final IPATranscript ipaActual =
				(getGroup().getIPAActual() == null ? new IPATranscript() : getGroup().getIPAActual());
		final List<IPATranscript> wordList = ipaActual.words();

		if(wordIndex >= 0 && wordIndex < wordList.size()) {
			return wordList.get(wordIndex);
		} else {
			return null;
		}
	}

	@Override
	public int getIPAActualWordLocation() {
		int retVal = -1;

		final IPATranscript actual = getGroup().getIPAActual();
		final IPATranscript ipa = getIPAActual();
		if(ipa != null) {
			final int eleIdx = actual.indexOf(ipa);
			retVal = actual.stringIndexOfElement(eleIdx);
		}

		return retVal;
	}

	@Override
	public PhoneMap getPhoneAlignment() {
		final IPATranscript ipaT = (getIPATarget() == null ? new IPATranscript() : getIPATarget());
		final IPATranscript ipaA = (getIPAActual() == null ? new IPATranscript() : getIPAActual());

		final PhoneMap grpAlignment = getGroup().getPhoneAlignment();
		if(grpAlignment == null) new PhoneMap();

		return grpAlignment.getSubAlignment(ipaT, ipaA);
	}

	@Override
	public TierString getNotes() {
		final TierString notes = getGroup().getNotes();

		if(wordIndex >= 0 && wordIndex < notes.numberOfWords()) {
			return notes.getWord(wordIndex);
		} else {
			return null;
		}
	}

	@Override
	public int getNotesWordLocation() {
		int retVal = -1;

		final TierString notes = getGroup().getNotes();
		final String[] wordList = notes.split("\\p{Space}");

		if(wordIndex >=0 && wordIndex < wordList.length) {
			int currentIdx = 0;
			for(int i = 0; i < wordIndex; i++) {
				currentIdx += (i > 0 ? 1 : 0) + wordList[i].length();
			}
			retVal = currentIdx;
		}

		return retVal;
	}

	@Override
	public Object getTier(String name) {
		Object retVal = null;

		// check for system tier
		final SystemTierType systemTier = SystemTierType.tierFromString(name);
		if(systemTier != null) {
			switch(systemTier) {
			case Orthography:
				retVal = getOrthography();
				break;

			case IPATarget:
				retVal = getIPATarget();
				break;

			case IPAActual:
				retVal = getIPAActual();
				break;

			case Notes:
				retVal = getNotes();
				break;

			default:
				break;
			}
		}

		if(retVal == null) {
			final TierString tierValue = getGroup().getTier(name, TierString.class);

			if(tierValue != null && wordIndex >= 0 && wordIndex < tierValue.numberOfWords())
				retVal = tierValue.getWord(wordIndex);
		}

		return retVal;
	}

	@Override
	public int getTierWordLocation(String tierName) {
		int retVal = -1;

		final TierString tierString = getGroup().getTier(tierName, TierString.class);

		if(wordIndex >= 0 && wordIndex < tierString.numberOfWords()) {
			retVal = tierString.getWordOffset(wordIndex);
		}

		return retVal;
	}
}
