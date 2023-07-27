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
package ca.phon.ipa;

import ca.phon.visitor.VisitorAdapter;
import ca.phon.visitor.annotation.Visits;

import java.util.*;

/**
 * Visitor for filtering a list of phones into a list of 
 * audible phones.
 * 
 */
public class AudiblePhoneVisitor extends VisitorAdapter<IPAElement> {

	private final List<IPAElement> phones = new ArrayList<IPAElement>();
	
	@Override
	public void fallbackVisit(IPAElement obj) {	
	}

	@Visits
	public void visitPhone(Phone p) {
		phones.add(p);
	}

	@Visits
	public void visitPause(Pause pause) {
		phones.add(pause);
	}
	
	@Visits
	public void visitCompoundPhone(CompoundPhone cp) {
		phones.add(cp);
	}
	
	public List<IPAElement> getPhones() {
		return Collections.unmodifiableList(this.phones);
	}
	
	public void reset() {
		this.phones.clear();
	}
	
}
