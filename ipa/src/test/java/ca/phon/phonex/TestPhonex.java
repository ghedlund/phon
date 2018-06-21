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
package ca.phon.phonex;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import ca.phon.ipa.IPATranscript;
import junit.framework.Assert;

@RunWith(JUnit4.class)
public class TestPhonex extends PhonexTest {

	@Test(expected=NoSuchPluginException.class)
	public void testMissingPlugin() throws Exception {
		final String phonex = "\\v:noplugin()";
		PhonexPattern.compile(phonex);
	}

	@Test(expected=PhonexPatternException.class)
	public void textInvalidFeature() throws Exception {
		PhonexPattern.compile("{invalid}");
	}

	@Test
	public void testBacktracking() throws Exception {
		final String phonex = "(\\c*\\v+\\c*)*";
		final PhonexPattern pattern = PhonexPattern.compile(phonex);
		final String ipa = "bbaab aba";

		final PhonexMatcher matcher = pattern.matcher(IPATranscript.parseIPATranscript(ipa));
		int numMatches = 0;
		while(matcher.find()) {
			++numMatches;
		}

		Assert.assertEquals(2, numMatches);
	}

	@Test
	public void testCompoundPhoneMatcher() throws ParseException {
		final String text = "st\u0361\u0283aad\u035c\u0292";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(text);
		
		// look for all compound phones
		final String phonex = "._.";
		final PhonexPattern pattern = PhonexPattern.compile(phonex);
		final PhonexMatcher matcher = pattern.matcher(ipa);
		
		int numFound = 0;
		while(matcher.find()) numFound++;
		Assert.assertEquals(2, numFound);
		
		// look for specific compound phones
	}
	
	@Test
	public void testRegexMatcher() throws ParseException {
		final String text =  "st\u0361\u0283aad\u035c\u0292";
		final String phonex = "'t.+'";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(text);
		
		final PhonexPattern pattern = PhonexPattern.compile(phonex);
		final PhonexMatcher matcher = pattern.matcher(ipa);
		
		Assert.assertEquals(true, matcher.find());
	}
	
	@Test
	public void testFeatureSetMatcher() throws ParseException {
		
	}

	@Test
	public void testUnicodeValueMatcher() throws ParseException {
		
	}

	@Test
	public void testAlignmentMarker() throws ParseException {
		final String text = "ba \u2194 ab";
		final String phonex = "(\\w+)\\b\u2194\\b(\\w+)";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(text);
		
		final PhonexPattern pattern = PhonexPattern.compile(phonex);
		final PhonexMatcher matcher = pattern.matcher(ipa);
		
		Assert.assertEquals(true, matcher.matches());
		Assert.assertEquals(ipa.subsection(0, 2).toList(), matcher.group(1));
		Assert.assertEquals(ipa.subsection(5, 7).toList(), matcher.group(2));
	}
	
	@Test
	public void testEmptyQuery() throws ParseException {
		final IPATranscript ipa = new IPATranscript();
		boolean matches = ipa.matches("^$");
		
		Assert.assertEquals(true, matches);
	}
	
	@Test
	public void testLookAheadBehind() throws ParseException {
		final String text = "ˈkʀət͡jə";
		final String phonex = "(?<\\w)\\c(?>\\w)";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(text);
		final PhonexPattern pattern = PhonexPattern.compile(phonex);
		final PhonexMatcher matcher = pattern.matcher(ipa);
		
		Assert.assertTrue(matcher.find());
		Assert.assertEquals(ipa.elementAt(2), matcher.group().get(0));
	}
	
	@Test
	public void testLookBehindWithBoundary() throws ParseException {
		final String text = "ˈk:oʀ:oi:di:dt͡j:oi:n";
		// Beginning of input boundary marker must be inside look-behind group
		final String phonex = "(?<\\S\\c:L*)\\c:O(?>\\w:sctype(\"-O\"))";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(text);
		
		final PhonexPattern pattern = PhonexPattern.compile(phonex);
		
		final PhonexMatcher matcher = pattern.matcher(ipa);
		
		Assert.assertTrue(matcher.find());
		Assert.assertEquals(ipa.elementAt(5), matcher.group().get(0));
	}
	
	@Test
	public void testGrouping1() throws ParseException {
		final String text = "zuˈkini";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(text);
		
		final String phonex = "\\S(\\c)(\\v)";
		IPATranscript[][] answers = {
				{ ipa.subsection(0, 1), ipa.subsection(1, 2) }, 
				{ ipa.subsection(3, 4), ipa.subsection(4, 5) },
				{ ipa.subsection(5, 6), ipa.subsection(6, 7) },
				
		};
		testGroups(ipa, phonex, answers);
	}
	
	@Test
	public void testNamedGrouping1() throws ParseException {
		final String text = "zuˈkini";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(text);
		
		final String phonex = "\\S(C=\\c)(V=\\v)";
		
		final Map<String, IPATranscript> hm1 = new HashMap<>();
		hm1.put("C", ipa.subsection(0, 1));
		hm1.put("V", ipa.subsection(1, 2));
		
		final Map<String, IPATranscript> hm2 = new HashMap<>();
		hm2.put("C", ipa.subsection(3, 4));
		hm2.put("V", ipa.subsection(4, 5));
		
		final Map<String, IPATranscript> hm3 = new HashMap<>();
		hm3.put("C", ipa.subsection(5, 6));
		hm3.put("V", ipa.subsection(6, 7));
		
		final List<Map<String, IPATranscript>> answers = List.of(hm1, hm2, hm3);
		testNamedGroups(ipa, phonex, answers);
	}
	
	@Test
	public void testGrouping2() throws ParseException {
		final String text = "zuˈkini";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(text);
		
		final String phonex = "(\\S(\\c)(\\v))";
		IPATranscript[][] answers = {
				{ ipa.subsection(0, 2), ipa.subsection(0, 1), ipa.subsection(1, 2) }, 
				{ ipa.subsection(2, 5), ipa.subsection(3, 4), ipa.subsection(4, 5) },
				{ ipa.subsection(5, 7), ipa.subsection(5, 6), ipa.subsection(6, 7) },
				
		};
		testGroups(ipa, phonex, answers);
	}
	
	@Test
	public void testNamedGrouping2() throws ParseException {
		final String text = "zuˈkini";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(text);
		
		final String phonex = "(\\S(C=\\c)(V=\\v))";
		
		final Map<String, IPATranscript> hm1 = new HashMap<>();
		hm1.put("C", ipa.subsection(0, 1));
		hm1.put("V", ipa.subsection(1, 2));
		
		final Map<String, IPATranscript> hm2 = new HashMap<>();
		hm2.put("C", ipa.subsection(3, 4));
		hm2.put("V", ipa.subsection(4, 5));
		
		final Map<String, IPATranscript> hm3 = new HashMap<>();
		hm3.put("C", ipa.subsection(5, 6));
		hm3.put("V", ipa.subsection(6, 7));
		
		final List<Map<String, IPATranscript>> answers = List.of(hm1, hm2, hm3);
		testNamedGroups(ipa, phonex, answers);
	}
	
	@Test
	public void testGrouping3() throws ParseException {
		final String text = "zuˈkini";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(text);
		
		final String phonex = "(\\S((\\c)(\\v)))";
		IPATranscript[][] answers = {
				{ ipa.subsection(0, 2), ipa.subsection(0, 2), ipa.subsection(0, 1), ipa.subsection(1, 2) }, 
				{ ipa.subsection(2, 5), ipa.subsection(3, 5), ipa.subsection(3, 4), ipa.subsection(4, 5) },
				{ ipa.subsection(5, 7), ipa.subsection(5, 7), ipa.subsection(5, 6), ipa.subsection(6, 7) },
				
		};
		testGroups(ipa, phonex, answers);
	}
	
	@Test
	public void testNamedGrouping3() throws ParseException {
		final String text = "zuˈkini";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(text);
		
		final String phonex = "(syll=\\S((C=\\c)(V=\\v)))";
		
		final Map<String, IPATranscript> hm1 = new HashMap<>();
		hm1.put("C", ipa.subsection(0, 1));
		hm1.put("V", ipa.subsection(1, 2));
		hm1.put("syll", ipa.subsection(0, 2));
		
		final Map<String, IPATranscript> hm2 = new HashMap<>();
		hm2.put("C", ipa.subsection(3, 4));
		hm2.put("V", ipa.subsection(4, 5));
		hm2.put("syll", ipa.subsection(2, 5));
		
		final Map<String, IPATranscript> hm3 = new HashMap<>();
		hm3.put("C", ipa.subsection(5, 6));
		hm3.put("V", ipa.subsection(6, 7));
		hm3.put("syll", ipa.subsection(5, 7));
		
		final List<Map<String, IPATranscript>> answers = List.of(hm1, hm2, hm3);
		testNamedGroups(ipa, phonex, answers);
	}
	
	@Test
	public void testGrouping4() throws ParseException {
		final String text = "ˈkʀət͡jə";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(text);
		
		final String phonex = "((\\c)(\\c))(\\v)";
		IPATranscript[][] answers = {
				{ ipa.subsection(1, 3), ipa.subsection(1, 2), ipa.subsection(2, 3), ipa.subsection(3, 4) }
		};
		testGroups(ipa, phonex, answers);
	}
	
	@Test
	public void testNamedGrouping4() throws ParseException {
		final String text = "ˈkʀət͡jə";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(text);
		
		final String phonex = "((C1=\\c)(C2=\\c))(V=\\v)";
		
		final HashMap<String, IPATranscript> hm1 = new HashMap<>();
		hm1.put("C1", ipa.subsection(1, 2));
		hm1.put("C2", ipa.subsection(2, 3));
		hm1.put("V", ipa.subsection(3, 4));
		
		final List<Map<String, IPATranscript>> answers = List.of(hm1);
		testNamedGroups(ipa, phonex, answers);
	}
	
	@Test
	public void testOrExprGroups1() throws ParseException {
		final String text = "ˈkʀət͡jə";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(text);
		
		final String phonex = "((C1=\\c)(C2=\\c)|(C1=\\c))\\v";
		IPATranscript[][] answers = {
				{ ipa.subsection(1, 3), ipa.subsection(1, 2), ipa.subsection(2, 3) }, 
				{ ipa.subsection(4, 5), ipa.subsection(4, 5), ipa.subsection(0, 0) }
				
		};
		testGroups(ipa, phonex, answers);
	}
	
	@Test
	public void testOrExprGroups2() throws ParseException {
		final String text = "ˈk:On:Oɛ:Di:Dp:Oə:Nɹ:C";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(text);
		
		final String phonex = "((C1=\\c:sctype(\"O|LA\"))(C2=\\c:sctype(\"O|LA\"))(C3=\\c:O)|(C1=\\c:sctype(\"O|LA\"))(C2=\\c:O)|(C1=\\c:O)).:N";
		IPATranscript[][] answers = {
				{ ipa.subsection(1, 3), ipa.subsection(1, 2), ipa.subsection(2, 3), ipa.subsection(0, 0) },
				{ ipa.subsection(5, 6), ipa.subsection(5, 6), ipa.subsection(0, 0), ipa.subsection(0, 0) }
				
		};
		testGroups(ipa, phonex, answers);
	}
	
	@Test
	public void testOrExprGroups3() throws ParseException {
		final String text = "k:Oə:Nn:Cˈs:Lt:Oɹ:Oe:Dɪ:Dn:Ct:Cs:R";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(text);
		
		final String phonex = "((C1=\\c:L:O)(C2=\\c:L:O)(C3=\\c:L:O)|(C1=\\c:L:O)(C2=\\c:L:O)|(C1=\\c:L:O))";
		IPATranscript[][] answers = {
				{ ipa.subsection(0, 1), ipa.subsection(0, 1), ipa.subsection(0, 0), ipa.subsection(0, 0) },
				{ ipa.subsection(4, 7), ipa.subsection(4, 5), ipa.subsection(5, 6), ipa.subsection(6, 7) }
		};
		
		testGroups(ipa, phonex, answers);
	}
	
	@Test
	public void testOrExprNamedGroups() throws ParseException {
		final String text = "ˈkʀət͡jə";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(text);
		
		final String phonex = "((C1=\\c)(C2=\\c)|(C1=\\c))\\v";
		
		final Map<String, IPATranscript> hm1 = new HashMap<>();
		hm1.put("C1", ipa.subsection(1, 2));
		hm1.put("C2", ipa.subsection(2, 3));
		
		final Map<String, IPATranscript> hm2 = new HashMap<>();
		hm2.put("C1", ipa.subsection(4, 5));
		hm2.put("C2", ipa.subsection(0, 0));
		
		final List<Map<String, IPATranscript>> answers = List.of(hm1, hm2);
		testNamedGroups(ipa, phonex, answers);
	}
	
	@Test(expected=PhonexPatternException.class)
	public void testDuplicateGroupName1() throws ParseException {
		final String phonex = "(C=\\c)(C=\\c)\\v";
		PhonexPattern.compile(phonex);
	}
	
	@Test(expected=PhonexPatternException.class)
	public void testDuplicateGroupName2() throws ParseException {
		final String phonex = "((C1=\\c)(C2=\\c)|(C2=\\c))\\v";
		PhonexPattern.compile(phonex);
	}
	
	@Test
	public void testNonCapturingGroup1() throws ParseException {
		final String text = "ˈkʀət͡jə";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(text);
		
		final String phonex = "(\\S\\c+\\v)(?=\\S\\c+\\v)";
		IPATranscript[][] answers = {
				{ ipa.subsection(0, 4) }
		};
		testGroups(ipa, phonex, answers);
	}
	
	@Test
	public void testNonCapturingGroup2() throws ParseException {
		final String text = "ˈkʀət͡jə";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(text);
		
		final String phonex = "(?=(\\S\\c+\\v)(\\S\\c+\\v))";
		IPATranscript[][] answers = {
				{} 
		};
		testGroups(ipa, phonex, answers);
	}
	
	@Test
	public void testNonCapturingGroup3() throws ParseException {
		final String text = "ˈkʀət͡jə";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(text);
		
		final String phonex = "((?=\\S\\c+\\v)(\\c+\\v))";
		IPATranscript[][] answers = {
				{ ipa, ipa.subsection(4, 6) }
		};
		testGroups(ipa, phonex, answers);
	}
	
	@Test
	public void testLookBehind1() throws ParseException {
		final String text = "zuˈkini";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(text);
		
		final String phonex = "(?<\\s\\c\\v)(\\c\\v)";
		IPATranscript[][] answers = {
				{ ipa.subsection(5, 7) }
		};
		testGroups(ipa, phonex, answers);
	}
	
	@Test
	public void testLookBehind2() throws ParseException {
		final String text = "zuˈkini";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(text);
		
		final String phonex = "(?<(\\s?\\c\\v)+)(\\c\\v)";
		IPATranscript[][] answers = {
				{ ipa.subsection(5, 7) }
		};
		testGroups(ipa, phonex, answers);
	}
	
	@Test
	public void testReluctantQuantifier() throws ParseException {
		final String text = "ˈh:Oæ:N^p:Oiː:Nb:Oʌ:Nɹ:Cθ:Cˌd:Oe͜ɪ:N";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(text);
		
		final String phonex = "(?<\\p)(.+?)(?>\\S)";
		IPATranscript[][] answers = {
				{ ipa.subsection(4, 6) }
		};
		testGroups(ipa, phonex, answers);
	}
	
	@Test(expected=PhonexPatternException.class)
	public void testUnbalancedParenthesis1() throws ParseException {
		final String phonex = "((C1=\\c)(C2=\\c)))\\v";
		PhonexPattern.compile(phonex);
	}
	
	@Test(expected=PhonexPatternException.class)
	public void testUnbalancedParenthesis2() throws ParseException {
		final String phonex = "((C1=\\c)(C2=\\c)\\v";
		PhonexPattern.compile(phonex);
	}
	
	@Test(expected=PhonexPatternException.class)
	public void testUnbalancedParenthesis3() throws ParseException {
		final String phonex = "((C1=\\c(C2=\\c))\\v";
		PhonexPattern.compile(phonex);
	}
	
	@Test(expected=PhonexPatternException.class)
	public void testInvalidBackReference1() throws ParseException {
		final String phonex = "(\\c)\\2";
		PhonexPattern.compile(phonex);
	}
	
	@Test(expected=PhonexPatternException.class)
	public void testInvalidBackReference2() throws ParseException {
		final String phonex = "(\\c)\\-2";
		PhonexPattern.compile(phonex);
	}
	
	@Test
	public void testBackReference() throws ParseException {
		final String text = "hello";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(text);
		
		final String phonex = "((\\c)\\2)";
		final IPATranscript[][] answers = new IPATranscript[][] {
			{ ipa.subsection(2, 4), ipa.subsection(2, 3) }
		};
		testGroups(ipa, phonex, answers);
	}

	@Test
	public void testRelativeReference() throws ParseException {
		final String text = "hello";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(text);
		
		final String phonex = "((\\c)\\-1)";
		final IPATranscript[][] answers = new IPATranscript[][] {
			{ ipa.subsection(2, 4), ipa.subsection(2, 3) }
		};
		testGroups(ipa, phonex, answers);
	}
	
}
