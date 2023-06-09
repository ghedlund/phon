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
package ca.phon.session.position;

import ca.phon.util.Range;
import ca.phon.util.Tuple;

public class RecordRange extends Tuple<String, Range> {

	public RecordRange(String tier, Range r) {
		super(tier, r);
	}

	public String getTier() {
		return super.getObj1();
	}

	public void setTier(String tier) {
		super.setObj1(tier);
	}

	public Range getRange() {
		return super.getObj2();
	}

	public void setRange(Range r) {
		super.setObj2(r);
	}
	
	public RecordLocation start() {
		return new RecordLocation(getTier(), getRange().getStart());
	}
	
	public RecordLocation end() {
		return new RecordLocation(getTier(), getRange().getEnd());
	}
	
}
