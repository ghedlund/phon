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
package ca.phon.phonex;

import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

import ca.phon.ipa.*;

@RunWith(JUnit4.class)
public class TestSyllableMatcher extends PhonexTest {

	@Test
	public void testSyllableMatcherWithQuantifier() throws Exception {
		final String text = "b:oa:Nn:Cd:Oa:Nk:Oa:N";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(text);
		
		final String phonex = "(\u03c3+)";
		final IPATranscript[][] answers = {
				{ ipa.subsection(0, 7) },
		};
		
		testGroups(ipa, phonex, answers);
	}
	
	@Test
	public void testMultiWord() throws Exception {
		final String text = "b:oa:N d:Oa:Nk:Oa:N";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(text);
		
		final String phonex = "(σ+(?=' 'σ+)*)";
		final IPATranscript[][] answers = {
				{ ipa.subsection(0, 7) },
		};
		
		testGroups(ipa, phonex, answers);
	}
	
	@Test
	public void testSyllableRange() throws Exception {
		final String text = "b:oa:Nn:Cd:Oa:Nk:Oa:N";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(text);
		
		String phonex = "(σ/O..N/+)";
		IPATranscript[][] answers = {
				{ ipa.subsection(0, 2) }, { ipa.subsection(3, 7) },
		};
		testGroups(ipa, phonex, answers);
		
		String phonex2 = "(σ/N/)";
		IPATranscript[][] answers2 = {
				{ ipa.subsection(1, 2) }, { ipa.subsection(4, 5) }, { ipa.subsection(6, 7) },
		};
		testGroups(ipa, phonex2, answers2);
	}
	
}
