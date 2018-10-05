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
package ca.phon.phonex;

import java.util.ArrayList;
import java.util.List;

import ca.phon.ipa.CompoundPhone;
import ca.phon.ipa.IPAElement;
import ca.phon.ipa.Pause;
import ca.phon.ipa.Phone;
import ca.phon.ipa.StressMarker;
import ca.phon.ipa.SyllableBoundary;
import ca.phon.ipa.WordBoundary;
import ca.phon.visitor.VisitorAdapter;
import ca.phon.visitor.annotation.Visits;

/**
 * Filter a iterable list of phones based on phonex
 * flags.
 */
public class PhonexFlagVisitor extends VisitorAdapter<IPAElement> {
	
	/**
	 * Return value
	 */
	private final List<IPAElement> filteredList = 
			new ArrayList<IPAElement>();
	
	/**
	 * phonex flags
	 */
	

	@Override
	public void fallbackVisit(IPAElement obj) {
		
	}
	
	@Visits
	public void basicPhone(Phone phone) {
		
	}
	
	@Visits
	public void compoundPhone(CompoundPhone phone) {
		
	}
	
	@Visits
	public void pause(Pause phone) {
		
	}
	
	@Visits
	public void stress(StressMarker phone) {
		
	}

	@Visits
	public void syllableBoundary(SyllableBoundary phone) {
		
	}
	
	@Visits
	public void wordBoundary(WordBoundary phone) {
		
	}
}
