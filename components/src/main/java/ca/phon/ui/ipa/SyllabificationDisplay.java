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

package ca.phon.ui.ipa;

import ca.phon.ipa.*;
import ca.phon.syllable.SyllabificationInfo;
import ca.phon.syllable.SyllableConstituentType;
import ca.phon.util.Tuple;

import javax.swing.*;
import java.awt.*;

/**
 */
public class SyllabificationDisplay extends JComponent {

	/** Syllabification prop */
	public static final String SYLLABIFICATION_PROP_ID = "_syllabification_";
	
	public static final String HIATUS_CHANGE_PROP_ID = "_hiatus_change_";
	
	public static final String RESYLLABIFY_PROP_ID = "_resyllabify_";

	private boolean showDiacritics = false;

	/** The ui class ID */
	private static final String uiClassId = "SyllabificationDisplayUI";

	/**
	 * Transcription
	 */
	private IPATranscript transcript = new IPATranscript();
	
	/**
	 * Display transcription
	 */
	private IPATranscript displayedPhones = new IPATranscript();

	/** The focused phone */
	private int focusedPhone = -1;

	/** Phon focus property */
	public final static String PHONE_FOCUS = "_phone_focus_";

	public SyllabificationDisplay() {
		super();
		setOpaque(false);
		updateUI();
	}

	@Override
	public String getUIClassID() {
		return uiClassId;
	}

	public void setUI(SyllabificationDisplayUI newUI) {
		super.setUI(newUI);
	}

	@Override
	public void updateUI() {
		setUI(new DefaultSyllabificationDisplayUI(this));
	}

	public SyllabificationDisplayUI getUI() {
		return (SyllabificationDisplayUI)ui;
	}

	public boolean isShowDiacritics() {
		return this.showDiacritics;
	}

	public void setShowDiacritics(boolean showDiacritics) {
		boolean oldVal = this.showDiacritics;
		this.showDiacritics = showDiacritics;
		firePropertyChange("showDiacritics", oldVal, showDiacritics);
	}

	public int getFocusedPhone() {
		return focusedPhone;
	}

	public void setFocusedPhone(int pIdx) {
		int currentFocus = focusedPhone;
		int direction = pIdx - currentFocus;
		if(pIdx >= 0 && pIdx < getNumberOfDisplayedPhones()) {
			IPAElement p = getPhoneAtIndex(pIdx);
			if(p.getText().equals(" ")) {
				if(direction < 0)
					pIdx--;
				else
					pIdx++;
			}
			focusedPhone = pIdx;
			repaint();

			super.firePropertyChange(PHONE_FOCUS, currentFocus, pIdx);
		}
	}

	public int getNumberOfDisplayedPhones() {
		return getDisplayedPhones().length();
	}
	public IPATranscript getTranscript() {
		return this.transcript;
	}

	public void setTranscript(IPATranscript phones) {
		this.transcript = phones;
		displayedPhones = this.transcript.removePunctuation(true);
		
		repaint();

		super.invalidate();
	}

	public void clear() {
		this.transcript = new IPATranscript();
		this.displayedPhones = new IPATranscript();
		repaint();
	}

	public IPAElement getPhoneAtIndex(int idx) {
		IPAElement retVal = null;
		if(idx >= 0 && idx < getDisplayedPhones().length()) {
			retVal = getDisplayedPhones().elementAt(idx);
		}
		return retVal;
	}

	/**
	 * Set syllabification at given index
	 */
	public void setSyllabificationAtIndex(int pIdx, SyllableConstituentType scType) {
		IPAElement p = getPhoneAtIndex(pIdx);
		if(p != null) {
			final int realPhonexIndex = getTranscript().indexOf(p);
			SyllabificationChangeData oldData =
					new SyllabificationChangeData(realPhonexIndex, p.getScType());
			SyllabificationChangeData newData =
					new SyllabificationChangeData(realPhonexIndex, scType);
			
			p.setScType(scType);

			super.firePropertyChange(SYLLABIFICATION_PROP_ID, oldData, newData);
		}
	}

	public void toggleHiatus(int pIdx) {
		final IPAElement ele = getPhoneAtIndex(pIdx);
		final IPAElement prevEle = getPhoneAtIndex(pIdx-1);
		final SyllabificationInfo info = ele.getExtension(SyllabificationInfo.class);
		final SyllabificationInfo prevInfo = prevEle.getExtension(SyllabificationInfo.class);
		if(info != null) {
			boolean wasHiatus = info.getConstituentType() == SyllableConstituentType.NUCLEUS && !info.isDiphthongMember();
			final int eleIdx = getTranscript().indexOf(ele);
			HiatusChangeData oldData = new HiatusChangeData(eleIdx, wasHiatus);
			HiatusChangeData newData = new HiatusChangeData(eleIdx, !wasHiatus);
			info.setDiphthongMember(wasHiatus);
			prevInfo.setDiphthongMember(wasHiatus);
			super.firePropertyChange(HIATUS_CHANGE_PROP_ID, oldData, newData);
		}
	}

	@Override
	public Dimension getPreferredSize() {
		return getUI().getPreferredSize(this);
	}
	
	public IPATranscript getDisplayedPhones() {
		return displayedPhones;
	}
	
	/**
	 * Syllabification change data.  Sent during syllabification events.
	 */
	public record SyllabificationChangeData(int position, SyllableConstituentType scType) {}

	/**
	 * Hiatus change data.  Sent during hiatus change events.
	 *
	 * @param position
	 * @param hiatus
	 */
	public record HiatusChangeData(int position, boolean hiatus) {}

}
