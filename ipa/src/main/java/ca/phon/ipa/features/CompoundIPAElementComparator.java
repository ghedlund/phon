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
package ca.phon.ipa.features;

import ca.phon.ipa.IPAElement;
import ca.phon.util.CompoundComparator;

import java.text.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * A compound comparator for features, with a fallback to a string comparator
 *
 */
public class CompoundIPAElementComparator extends CompoundComparator<IPAElement> {
	
	public CompoundIPAElementComparator() {
		super();
	}

	public CompoundIPAElementComparator(
			Collection<Comparator<IPAElement>> comparators) {
		super(comparators);
	}

	@SafeVarargs
	public CompoundIPAElementComparator(Comparator<IPAElement>... comparators) {
		super(comparators);
	}

	@Override
	public int compare(IPAElement o1, IPAElement o2) {
		int retVal = super.compare(o1, o2);
		if(retVal == 0) {
			try {
				final Collator collator = new IPACollator();
				retVal = collator.compare(o1.toString(), o2.toString());
				retVal = (retVal > 0 ? 1 : (retVal < 0 ? -1 : 0));
			} catch (ParseException e) {
				Logger.getLogger(getClass().getName()).warning(e.getLocalizedMessage());
			}
		}
		
		return retVal;
	}
	
}
