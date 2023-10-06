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

import ca.phon.ipa.parser.*;
import ca.phon.syllable.SyllableConstituentType;
import ca.phon.util.PrefHelper;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Test methods for the ipa parser.
 *
 */
@RunWith(JUnit4.class)
public class TestIPAParser {
	
	/**
	 * Test each individual consonant and vowel.
	 * 
	 */
	@Test
	public void testIndividualPhones() throws ParseException {
		final List<Character> testChars = new ArrayList<Character>();
		final IPATokens tokens = IPATokens.getSharedInstance();
		testChars.addAll(tokens.getCharactersForType(IPATokenType.CONSONANT));
		testChars.addAll(tokens.getCharactersForType(IPATokenType.VOWEL));
		
		for(Character c:testChars) {
			final String testString = "" + c;
			final IPATranscript transcript = IPATranscript.parseIPATranscript(testString);
			if(transcript.length() < 1) {
				System.err.println(testString + " " + Integer.toHexString((int)c.charValue()));
			}
			Assert.assertEquals(1, transcript.length());
			
			final IPAElement ipaEle = transcript.elementAt(0);
			Assert.assertEquals(Phone.class, ipaEle.getClass());
			
			final Phone p = (Phone)ipaEle;
			Assert.assertEquals(testString, p.getText());
		}
	}
	
	@Test
	public void testPrefixDiacritics() throws ParseException {
		final IPATokens tokens = IPATokens.getSharedInstance();
		final List<Character> testChars = new ArrayList<Character>();
		testChars.addAll(tokens.getCharactersForType(IPATokenType.CONSONANT));
		testChars.addAll(tokens.getCharactersForType(IPATokenType.VOWEL));
		final Set<Character> prefixChars = tokens.getCharactersForType(IPATokenType.PREFIX_DIACRITIC);
		
		for(Character c:testChars) {
			for(Character prefixChar:prefixChars) {
				final String testString = prefixChar + "" + c;
				final IPATranscript transcript = IPATranscript.parseIPATranscript(testString);
				if(transcript.length() < 1) {
					System.err.println(testString + " " + Integer.toHexString((int)c.charValue()));
				}
				Assert.assertEquals(1, transcript.length());
				
				final IPAElement ipaEle = transcript.elementAt(0);
				Assert.assertEquals(Phone.class, ipaEle.getClass());
				
				final Phone p = (Phone)ipaEle;
				Assert.assertEquals(testString, p.getText());
				
				Assert.assertEquals(p.getPrefixDiacritics()[0].getCharacter(), prefixChar);
				Assert.assertEquals(p.getBasePhone(), c);
			}
		}
	}
	
	@Test
	public void testSuffixDiacritics() throws ParseException {
		final IPATokens tokens = IPATokens.getSharedInstance();
		final List<Character> testChars = new ArrayList<Character>();
		testChars.addAll(tokens.getCharactersForType(IPATokenType.CONSONANT));
		testChars.addAll(tokens.getCharactersForType(IPATokenType.VOWEL));
		final Set<Character> suffixChars = tokens.getCharactersForType(IPATokenType.SUFFIX_DIACRITIC);
		
		for(Character c:testChars) {
			for(Character suffixChar:suffixChars) {
				final String testString = c + "" + suffixChar;
				final IPATranscript transcript = IPATranscript.parseIPATranscript(testString);
				if(transcript.length() < 1) {
					System.err.println(testString + " " + Integer.toHexString((int)c.charValue()));
				} else if(transcript.length() > 1) {
					System.err.println(testString + " parsed as two phones.");
				}
				Assert.assertEquals(1, transcript.length());
				
				final IPAElement ipaEle = transcript.elementAt(0);
				Assert.assertEquals(Phone.class, ipaEle.getClass());
				
				final Phone p = (Phone)ipaEle;
				Assert.assertEquals(testString, p.getText());
				
				Assert.assertEquals(suffixChar, p.getSuffixDiacritics()[0].getCharacter());
				Assert.assertEquals(p.getBasePhone(), c);
			}
		}
	}
	
	@Test
	public void testReversedDiacritics() throws ParseException {
		final IPATokens tokens = IPATokens.getSharedInstance();
		final List<Character> testChars = new ArrayList<Character>();
		final char[] roleReversers = new char[]{'\u0335'};
		testChars.addAll(tokens.getCharactersForType(IPATokenType.CONSONANT));
		testChars.addAll(tokens.getCharactersForType(IPATokenType.VOWEL));
		final Set<Character> prefixChars = tokens.getCharactersForType(IPATokenType.PREFIX_DIACRITIC);
		final Set<Character> suffixChars = tokens.getCharactersForType(IPATokenType.SUFFIX_DIACRITIC);
		
		for(char roleReverse:roleReversers) {
			for(Character c:testChars) {
				for(Character prefixChar:prefixChars) {
					final String testString = 
							(roleReverse == roleReversers[0] ? c + "" + prefixChar + "" + roleReverse
									: c + "" + roleReverse + "" + prefixChar);
					final IPATranscript transcript = IPATranscript.parseIPATranscript(testString);
					if(transcript.length() < 1) {
						System.err.println(testString + " " + Integer.toHexString((int)c.charValue()));
					}
					Assert.assertEquals(1, transcript.length());
					
					final IPAElement ipaEle = transcript.elementAt(0);
					Assert.assertEquals(Phone.class, ipaEle.getClass());
					
					final Phone p = (Phone)ipaEle;
					Assert.assertEquals(testString, p.getText());
					
					Assert.assertEquals(p.getSuffixDiacritics()[0].getCharacter(), prefixChar);
					Assert.assertEquals(p.getBasePhone(), c);
				}
			}
			
			for(Character c:testChars) {
				for(Character suffixChar:suffixChars) {
					final String testString = suffixChar + "" + roleReverse + "" + c;
					final IPATranscript transcript = IPATranscript.parseIPATranscript(testString);
					if(transcript.length() < 1) {
						System.err.println(testString + " " + Integer.toHexString((int)c.charValue()));
					}
					Assert.assertEquals(1, transcript.length());
					
					final IPAElement ipaEle = transcript.elementAt(0);
					Assert.assertEquals(Phone.class, ipaEle.getClass());
					
					final Phone p = (Phone)ipaEle;
					Assert.assertEquals(testString, p.getText());
					
					Assert.assertEquals(p.getPrefixDiacritics()[0].getCharacter(), suffixChar);
					Assert.assertEquals(p.getBasePhone(), c);
				}
			}
		}
	}
	
	@Test
	public void testCompoundPhoneChaining() throws ParseException {
		final String txt = "iʰ͡b͡c";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(txt);
		
		Assert.assertEquals(txt, ipa.toString());
		Assert.assertEquals(1, ipa.length());
		Assert.assertEquals(CompoundPhone.class, ipa.elementAt(0).getClass());
	}

	@Test
	public void testCompoundPhonePrefixDiacritics() throws ParseException {
		final String txt = "ᵐt͡s";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(txt);

		Assert.assertEquals(txt, ipa.toString());
		Assert.assertEquals(1, ipa.length());
		Assert.assertEquals(CompoundPhone.class, ipa.elementAt(0).getClass());
		final CompoundPhone cp = (CompoundPhone) ipa.elementAt(0);
		Assert.assertEquals("t", cp.getFirstPhone().getBasePhone().toString());
		Assert.assertEquals("ᵐ", cp.getFirstPhone().getPrefix());
		Assert.assertEquals("s", cp.getSecondPhone().getBasePhone().toString());
	}

	@Test
	public void testCompoundPhoneSuffixDiacritics() throws ParseException {
		final String txt = "t͡sʰ";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(txt);

		Assert.assertEquals(txt, ipa.toString());
		Assert.assertEquals(1, ipa.length());
		Assert.assertEquals(CompoundPhone.class, ipa.elementAt(0).getClass());
		final CompoundPhone cp = (CompoundPhone) ipa.elementAt(0);
		Assert.assertEquals("t", cp.getFirstPhone().getBasePhone().toString());
		Assert.assertEquals("s", cp.getSecondPhone().getBasePhone().toString());
		Assert.assertEquals("ʰ", cp.getSecondPhone().getSuffix());
	}

	@Test
	public void testCompoundPhoneInnerSuffixDiacritics() throws ParseException {
		final String txt = "tʰ͡s";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(txt);

		Assert.assertEquals(txt, ipa.toString());
		Assert.assertEquals(1, ipa.length());
		Assert.assertEquals(CompoundPhone.class, ipa.elementAt(0).getClass());
		final CompoundPhone cp = (CompoundPhone) ipa.elementAt(0);
		Assert.assertEquals("t", cp.getFirstPhone().getBasePhone().toString());
		Assert.assertEquals("ʰ", cp.getFirstPhone().getSuffix());
		Assert.assertEquals("s", cp.getSecondPhone().getBasePhone().toString());
	}

	@Test
	public void testCompoundPhoneInnerPrefixDiacritics()  throws ParseException {
		final String txt = "t͡ᵐs";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(txt);

		Assert.assertEquals(txt, ipa.toString());
		Assert.assertEquals(1, ipa.length());
		Assert.assertEquals(CompoundPhone.class, ipa.elementAt(0).getClass());
		final CompoundPhone cp = (CompoundPhone) ipa.elementAt(0);
		Assert.assertEquals("t", cp.getFirstPhone().getBasePhone().toString());
		Assert.assertEquals("ᵐ", cp.getSecondPhone().getPrefix());
		Assert.assertEquals("s", cp.getSecondPhone().getBasePhone().toString());
	}

	@Test
	public void testCompoundMarker() throws Exception {
		final String testString = "yes+sir";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(testString);
		
		Assert.assertEquals(7, ipa.length());
		Assert.assertEquals(CompoundWordMarker.class, ipa.elementAt(3).getClass());
	}

	@Test
	public void testComplexCompound() throws Exception {
		final String testString = "ⁿ̃e̯ːˑ³⁷͡ʰ̵ɪᶾ⁵¹";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(testString);

		Assert.assertEquals(1, ipa.length());
		Assert.assertEquals(testString, ipa.toString());
	}
	
	@Test
	public void testLinker() throws Exception {
		final String testString = "yes\u2040sir";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(testString);
		
		Assert.assertEquals(7, ipa.length());
		Assert.assertEquals(Linker.class, ipa.elementAt(3).getClass());
	}
	
	@Test
	public void testContraction() throws Exception {
		final String testString = "yes\u203fsir";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(testString);
		
		Assert.assertEquals(7, ipa.length());
		Assert.assertEquals(Contraction.class, ipa.elementAt(3).getClass());
	}
	
	@Test
	public void testEmbeddedSyllabification() throws Exception {
		final String testString = "s:Eh:Le:Ol:Nl:Co:R";
		final IPATranscript transcript = IPATranscript.parseIPATranscript(testString);
		
		Assert.assertEquals(6, transcript.length());
		Assert.assertEquals(SyllableConstituentType.OEHS, transcript.elementAt(0).getScType());
		Assert.assertEquals(SyllableConstituentType.LEFTAPPENDIX, transcript.elementAt(1).getScType());
		Assert.assertEquals(SyllableConstituentType.ONSET, transcript.elementAt(2).getScType());
		Assert.assertEquals(SyllableConstituentType.NUCLEUS, transcript.elementAt(3).getScType());
		Assert.assertEquals(SyllableConstituentType.CODA, transcript.elementAt(4).getScType());
		Assert.assertEquals(SyllableConstituentType.RIGHTAPPENDIX, transcript.elementAt(5).getScType());
	}

	@Test
	public void testPhonexMatcherReference() throws Exception {
		for(int i = 0; i < 10; i++) {
			final String ipaTxt = "t\\" + i + "st";
			final IPATranscript ipa = IPATranscript.parseIPATranscript(ipaTxt);
			Assert.assertEquals(PhonexMatcherReference.class, ipa.elementAt(1).getClass());
		}
		
		final String ipaTxt = "t\\{o}st";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(ipaTxt);
		Assert.assertEquals(PhonexMatcherReference.class, ipa.elementAt(1).getClass());
	}

	@Test
	public void testIntraWordPause() throws Exception {
		final String txt = "te^st";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(txt);
		
		Assert.assertEquals(5, ipa.length());
		Assert.assertEquals(IntraWordPause.class, ipa.elementAt(2).getClass());
	}
	
	@Test
	public void testInterWordPause() throws Exception {
		for(PauseLength pl:PauseLength.values()) {
			if(pl == PauseLength.NUMERIC) continue;
			final String txt = "hello " + pl.getText() + " world";
			final IPATranscript ipa = IPATranscript.parseIPATranscript(txt);
			
			Assert.assertEquals(3, ipa.words().size());
			Assert.assertEquals(Pause.class, ipa.words().get(1).elementAt(0).getClass());
			Assert.assertEquals(pl, ((Pause)ipa.words().get(1).elementAt(0)).getType());
		}
	}

	@Test
	public void testNumericPause() throws Exception {
		final String txt = "hello (0.5) world";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(txt);

		Assert.assertEquals(3, ipa.words().size());
		Assert.assertEquals(Pause.class, ipa.words().get(1).elementAt(0).getClass());
		Assert.assertEquals(PauseLength.NUMERIC, ((Pause)ipa.words().get(1).elementAt(0)).getType());
		Assert.assertEquals(0.5f, ((Pause)ipa.words().get(1).elementAt(0)).getLength(), 0.001f);
	}
	
	@Test
	public void testIntonationGroups() throws Exception {
		for(IntonationGroupType igType:IntonationGroupType.values()) {
			final String txt = "o " + igType.getGlyph() + " ənˈʤi ˈɪn";
			final IPATranscript ipa = IPATranscript.parseIPATranscript(txt);
			
			Assert.assertEquals(igType.getGlyph() + "", ipa.elementAt(2).toString());
		}
	}
	
	@Test
	public void testAlignment() throws Exception {
		final char alignmentChar = AlignmentMarker.ALIGNMENT_CHAR;
		final String txt = "b " + alignmentChar + " c";
		
		final IPATranscript ipa = IPATranscript.parseIPATranscript(txt);
		Assert.assertEquals(ipa.length(), 5);
		Assert.assertEquals(ipa.elementAt(2).getText(), alignmentChar + "");
	}
	
	@Test
	public void testLeadingWhitespace() throws Exception {
		final String txt = " helo";
		
		final IPATranscript ipa = IPATranscript.parseIPATranscript(txt);
		Assert.assertEquals(4, ipa.length());
		Assert.assertEquals("h", ipa.elementAt(0).getText());
	}
	
	@Test
	public void testTrailingWhitespace() throws Exception {
		final String txt = "helo ";
		
		final IPATranscript ipa = IPATranscript.parseIPATranscript(txt);
		Assert.assertEquals(4, ipa.length());
		Assert.assertEquals("h", ipa.elementAt(0).getText());
	}

	@Test
	public void testToneNumbers() throws Exception {
		final String txt = "b:Oa²³⁴:Nd:Oa⁰:N";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(txt);

		Assert.assertEquals(4, ipa.length());
		Assert.assertEquals(2, ipa.syllables().size());
		Assert.assertEquals("b", ipa.elementAt(0).toString());
		Assert.assertEquals("a²³⁴", ipa.elementAt(1).toString());
		final String toneNumberString = Arrays.stream(((Phone)ipa.elementAt(1)).getToneNumberDiacritics()).map(Object::toString).collect(Collectors.joining());
		Assert.assertEquals("²³⁴", toneNumberString);
		Assert.assertEquals("d", ipa.elementAt(2).toString());
		Assert.assertEquals("a⁰", ipa.elementAt(3).toString());
	}

	@Test
	public void testPg() throws Exception {
		final String txt = "‹se le›";
		final IPATranscript ipa = IPATranscript.parseIPATranscript(txt);

		Assert.assertEquals(7, ipa.length());
		Assert.assertEquals(PhoneticGroupMarker.class, ipa.elementAt(0).getClass());
	}
	
	@Test(expected=ParseException.class)
	public void testInvalidConsitutentType() throws Exception {
		final String txt = "ʌ:wɪtaʊ";
		
		IPATranscript ipa = IPATranscript.parseIPATranscript(txt);

		Assert.assertNull(ipa);
	}

}
