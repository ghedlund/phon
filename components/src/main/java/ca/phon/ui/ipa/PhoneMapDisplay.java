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
import ca.phon.ipa.alignment.PhoneMap;
import ca.phon.util.Tuple;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import java.util.*;

/**
 * Display a list of phonemaps for editing.
 * Each phonemap adds to the number of possible
 * phone positions (for alignment.)
 *
 */
public class PhoneMapDisplay extends JComponent {

	/** Alignment change property */
	public static final String ALIGNMENT_CHANGE_PROP = "_aligned_changed_";

	/** Property for drawing colours */
	public static final String PAINT_PHONE_BACKGROUND_PROP = "_paint_phone_background_";

	private boolean showDiacritics = false;

	/** List of phone maps *
	 *
	 */
	private List<PhoneMap> alignments = new ArrayList<>();

	/**
	 * The currently focused position
	 * 
	 */
	private int currentFocusPosition = 0;

	/**
	 * Phone current focused.
	 */
	private Phone focusedPhone = null;

	private boolean paintPhoneBackground = false;

	
	/** The ui class ID */
	private static final String uiClassId = "PhoneMapDisplayUI";

	public PhoneMapDisplay() {
		super();
		updateUI();
	}

	@Override
	public String getUIClassID() {
		return uiClassId;
	}

	@Override
	protected void setUI(ComponentUI newUI) {
		super.setUI(newUI);
	}

	@Override
	public void updateUI() {
		setUI(new DefaultPhoneMapDisplayUI(this));
	}

	public PhoneMapDisplayUI getUI() {
		return (PhoneMapDisplayUI)ui;
	}

	public boolean isShowDiacritics() {
		return this.showDiacritics;
	}

	public void setShowDiacritics(boolean showDiacritics) {
		boolean oldVal = this.showDiacritics;
		this.showDiacritics = showDiacritics;
		firePropertyChange("showDiacritics", oldVal, showDiacritics);
	}

	/**
	 * Get the number of positions in the display.  This includes
	 * indel positions.
	 */
	public int getNumberOfAlignmentPositions() {
		int retVal = 0;

		for(PhoneMap pm: alignments) {
			if(pm == null) continue;
			retVal += pm.getAlignmentLength();
		}

		return retVal;
	}

	public int getNumberOfGroups() {
		return alignments.size();
	}

	public PhoneMap getPhoneMapForWord(int wordIndex) {
		PhoneMap retVal = null;

		if(wordIndex >=0 && wordIndex < alignments.size()) {
			retVal = alignments.get(wordIndex);

		}

		return retVal;
	}

	public void setPhoneMapForWord(int wordIndex, PhoneMap pm) {
		if(wordIndex < alignments.size()) {
			alignments.remove(wordIndex);
		}
		alignments.add(wordIndex, pm);

		super.invalidate();
	}

	public void clear() {
		alignments.clear();
		repaint();
	}

	public int getFocusedPosition() {
		return this.currentFocusPosition;
	}

	public void setFocusedPosition(int nextFocus) {
		if(nextFocus >= 0 && nextFocus <
				this.getNumberOfAlignmentPositions()) {
			this.currentFocusPosition = nextFocus;
			repaint();
		}
	}

	public Tuple<Integer, Integer> positionToWordIndex(int pos) {
		Tuple<Integer, Integer> retVal = new Tuple<Integer, Integer>(0, 0);

		int gIdx = 0;
		int cPos = pos;
		for(PhoneMap pm: alignments) {
			if(pm.getAlignmentLength() <= cPos) {
				cPos -= pm.getAlignmentLength();
				gIdx++;
			} else {
				break;
			}
		}

		retVal.setObj1(gIdx);
		retVal.setObj2(cPos);

		return retVal;
	}

	/**
	 * Get aligned phones at the given position.
	 * Target alignment is obj1, actual is obj2.
	 *
	 * Indels are indicated by null.
	 *
	 * @param wordIndex
	 * @return the aligned phones at the given position
	 */
	public Tuple<IPAElement, IPAElement> getAlignedPhones(int wordIndex) {
		Tuple<IPAElement, IPAElement> retVal = new Tuple<IPAElement, IPAElement>();

		int gIdx = 0;
		int pIdx = 0;
		int currentIdx = 0;

		PhoneMap currentGrp = null;
		while(gIdx < alignments.size() && currentIdx <= wordIndex) {

			if(currentGrp == null) {
				currentGrp = alignments.get(gIdx);
				pIdx = 0;
			}

			if(pIdx >= currentGrp.getAlignmentLength()) {
				gIdx++;
				currentGrp = null;
			} else if(currentIdx == wordIndex) {

				List<IPAElement> ps = 
						currentGrp.getAlignedElements(pIdx);
				retVal.setObj1(ps.get(0));
				retVal.setObj2(ps.get(1));

				currentIdx++;
			} else {
				currentIdx++;
				pIdx++;
			}
		}

		return retVal;
	}

	public boolean isPaintPhoneBackground() {
		return this.paintPhoneBackground;
	}

	public void setPaintPhoneBackground(boolean v) {
		boolean oldVal = this.paintPhoneBackground;
		this.paintPhoneBackground = v;
		repaint();

		super.firePropertyChange(PAINT_PHONE_BACKGROUND_PROP, oldVal, v);
	}

	public void togglePaintPhoneBackground() {
		this.setPaintPhoneBackground(!this.isPaintPhoneBackground());
	}

	/* Movement */
	/**
	 * This method will move the value at
	 * alignment[0][position] one place right.  The
	 * return value is the mutated alignment.
	 */
	public Integer[][] mutateAlignment(
			Integer[][] alignment, int position) {
		// calculate the new alignment the size
		// is always +1 except when moving into an indel
		Integer newTopAlignment[] = null;
		Integer newBottomAlignment[] = null;
		
		int nextIndel = -1;
		for(int i = position+1; i < alignment[0].length; i++) {
			if(alignment[0][i] == -1) {
				nextIndel = i;
				break;
			}
		}

		if(nextIndel >= 0) {
			// in this case we can just swap the values
			newTopAlignment = alignment[0];
			Integer[] temp = Arrays.copyOfRange(newTopAlignment, position, nextIndel);
			newTopAlignment[position] = -1;
			for(int i = 0; i < temp.length; i++) {
				newTopAlignment[position+(i+1)] = temp[i];
			}

			newBottomAlignment = alignment[1];
		} else {
			int newAlignmentSize = alignment[0].length+1;
			newTopAlignment = new Integer[newAlignmentSize];
			newBottomAlignment = new Integer[newAlignmentSize];

			for(int i = 0; i < position; i++) {
				newTopAlignment[i] = alignment[0][i];
			}
			newTopAlignment[position] = -1;
			for(int i = position; i < alignment[0].length; i++) {
				newTopAlignment[i+1] = alignment[0][i];
			}
			
			newBottomAlignment[newAlignmentSize-1] = -1;
			for(int i = 0; i < alignment[1].length; i++) {
				newBottomAlignment[i] = alignment[1][i];
			}
		}

		// remove instances of indels aligned with indels
		ArrayList<Integer> compactPositions = new ArrayList<Integer>();
		for(int i = 0; i < newTopAlignment.length; i++) {
			if(newTopAlignment[i] == -1 && newBottomAlignment[i] == -1)
				compactPositions.add(i);
		}

		Integer tempTop[] = newTopAlignment;
		Integer tempBottom[] = newBottomAlignment;

		newTopAlignment = new Integer[tempTop.length-compactPositions.size()];
		newBottomAlignment = new Integer[tempBottom.length-compactPositions.size()];

		int tempIndex = 0;
		for(int i = 0; i < tempTop.length; i++) {
			// skip aligned indels
			if(!compactPositions.contains(i)) {
				newTopAlignment[tempIndex] = tempTop[i];
				newBottomAlignment[tempIndex] = tempBottom[i];
				tempIndex++;
			}
		}

		Integer[][] newAlignment = new Integer[2][];
		newAlignment[0] = newTopAlignment;
		newAlignment[1] = newBottomAlignment;

		return newAlignment;
	}

	/**
	 * Move specified phone one position right
	 * @param wordIndex
	 * @param alignmentIndex
	 * @param top <code>true</code> if top side of alignment, <code>false</code> if bottom
	 */
	public void movePhoneRight(int wordIndex, int alignmentIndex, boolean top) {
		PhoneMap pm = alignments.get(wordIndex);
		int pos = alignmentIndex;

		IPAElement phoneToMove =
				(!top ? pm.getBottomAlignmentElements().get(pos)
						: pm.getTopAlignmentElements().get(pos));

		// can't move indels
		if (phoneToMove == null) return;

		Integer[][] oldAlignment = new Integer[2][];
		oldAlignment[0] =
				Arrays.copyOf((!top ? pm.getBottomAlignment() : pm.getTopAlignment()), pm.getAlignmentLength());
		oldAlignment[1] =
				Arrays.copyOf((!top ? pm.getTopAlignment() : pm.getBottomAlignment()), pm.getAlignmentLength());

		Integer[][] newAlignment = 
				mutateAlignment(oldAlignment, pos);
		
		if(!top) {
			final Integer[] temp = newAlignment[0];
			newAlignment[0] = newAlignment[1];
			newAlignment[1] = temp;
		}

		pm.setTopAlignment(newAlignment[0]);
		pm.setBottomAlignment(newAlignment[1]);
		
		// check to see if we need to move our focus
		IPAElement phoneAfterMove = 
				(!top ? pm.getBottomAlignmentElements().get(pos)
					: pm.getTopAlignmentElements().get(pos));
		
		if(phoneAfterMove == null || phoneAfterMove != phoneToMove) {
			setFocusedPosition(getFocusedPosition()+1);
		}
		AlignmentChangeData oldData =
				new AlignmentChangeData(wordIndex, oldAlignment);
		AlignmentChangeData newData =
				new AlignmentChangeData(wordIndex, newAlignment);
		
		if(oldAlignment[0].length != newAlignment[0].length) {
			revalidate();
		}
		repaint();

		super.firePropertyChange(ALIGNMENT_CHANGE_PROP, oldData, newData);
	}

	public void movePhoneLeft(int wordIndex, int alignmentIndex, boolean top) {
		PhoneMap pm = alignments.get(wordIndex);
		int pos = alignmentIndex;

		IPAElement phoneToMove =
				(!top ? pm.getBottomAlignmentElements().get(pos)
						: pm.getTopAlignmentElements().get(pos));

		// can't move indels
		if (phoneToMove == null) return;

		// same as moveTopPhoneRight, but we will flip the arrays before use
		Integer[] _oldTopAlignment = pm.getTopAlignment();
		ArrayList<Integer> oldTopAlignment = new ArrayList<Integer>();
		for(Integer i:_oldTopAlignment) {
			oldTopAlignment.add(i);
		}
		Collections.reverse(oldTopAlignment);
		Integer[] _oldBottomAlignment = pm.getBottomAlignment();
		ArrayList<Integer> oldBottomAlignment = new ArrayList<Integer>();
		for(Integer i:_oldBottomAlignment) {
			oldBottomAlignment.add(i);
		}
		Collections.reverse(oldBottomAlignment);

		Integer[][] reversedAlignment = new Integer[2][];
		reversedAlignment[0] = (!top ? oldBottomAlignment.toArray(new Integer[0]) :
			oldTopAlignment.toArray(new Integer[0]));
		reversedAlignment[1] = (!top ? oldTopAlignment.toArray(new Integer[0]) :
			oldBottomAlignment.toArray(new Integer[0]));

		int reversePos = (pm.getAlignmentLength()-1) - pos;

		Integer[][] reversedNewAlignment =
			mutateAlignment(reversedAlignment, reversePos);

		ArrayList<Integer> newTopAlignment = new ArrayList<Integer>();
		for(int i = reversedNewAlignment[0].length-1; i >= 0; i--)
			newTopAlignment.add(
					(!top ? reversedNewAlignment[1][i] : reversedNewAlignment[0][i])
					);

		ArrayList<Integer> newBottomAlignment = new ArrayList<Integer>();
		for(int i = reversedNewAlignment[1].length-1; i >= 0; i--)
			newBottomAlignment.add(
					(!top ? reversedNewAlignment[0][i] : reversedNewAlignment[1][i])
					);

		pm.setTopAlignment(newTopAlignment.toArray(new Integer[0]));
		pm.setBottomAlignment(newBottomAlignment.toArray(new Integer[0]));

		Integer[][] oldAlignment = new Integer[2][];
		oldAlignment[0] = _oldTopAlignment;
		oldAlignment[1] = _oldBottomAlignment;

		Integer[][] newAlignment = new Integer[2][];
		newAlignment[0] = newTopAlignment.toArray(new Integer[0]);
		newAlignment[1] = newBottomAlignment.toArray(new Integer[0]);

		// check to see if we need to move our focus
		if(pos >= pm.getAlignmentLength()) {
			setFocusedPosition(getFocusedPosition()-1);
		} else {
			IPAElement phoneAfterMove =
					(!top ? pm.getBottomAlignmentElements().get(pos)
						: pm.getTopAlignmentElements().get(pos));

			if(phoneAfterMove == null || phoneAfterMove != phoneToMove) {
				setFocusedPosition(getFocusedPosition()-1);
			}
		}

		AlignmentChangeData oldData =
				new AlignmentChangeData(wordIndex, oldAlignment);
		AlignmentChangeData newData =
				new AlignmentChangeData(wordIndex, newAlignment);
		
		if(oldAlignment[0].length != newAlignment[0].length) {
			revalidate();
		}
		repaint();

		super.firePropertyChange(ALIGNMENT_CHANGE_PROP, oldData, newData);
	}

	public void fireAlignmentChange(AlignmentChangeData oldValue, AlignmentChangeData newValue) {
		super.firePropertyChange(ALIGNMENT_CHANGE_PROP, oldValue, newValue);
	}

	/** Class for alignment change events */
	public record AlignmentChangeData(Integer wordIndex, Integer[][] alignment) { }

}
