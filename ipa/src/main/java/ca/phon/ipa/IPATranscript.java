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

import ca.phon.cvseq.*;
import ca.phon.extensions.*;
import ca.phon.ipa.features.*;
import ca.phon.ipa.parser.*;
import ca.phon.ipa.parser.exceptions.IPAParserException;
import ca.phon.phonex.*;
import ca.phon.stresspattern.*;
import ca.phon.syllable.*;
import ca.phon.util.*;
import ca.phon.visitor.*;
import ca.phon.visitor.annotation.Visits;
import org.antlr.v4.runtime.CharStreams;

import java.text.ParseException;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * <p>A (somewhat) immutable representation of an IPA transcription.  While the number of elements
 * in the transcription cannot be changed, runtime extensions provided by the {@link IExtendable}
 * interface may be swapped.</p>
 *
 * <p>Objects of this type should be created using either the {@link IPATranscript#parseIPATranscript(String)}
 * static method or {@link IPATranscriptBuilder}.</p>
 */
public final class IPATranscript implements Iterable<IPAElement>, Visitable<IPAElement>, IExtendable, Comparable<IPATranscript> {

	private static final long serialVersionUID = 8942864962427274326L;

	private final ExtensionSupport extSupport = new ExtensionSupport(IPATranscript.class, this);

	private final IPAElement[] transcription;

	/**
	 * Convert a string into an {@link IPATranscript}
	 *
	 * @param transcript the text of the IPA transcription
	 *
	 */
	public static IPATranscript parseIPATranscript(String transcript)
		throws ParseException {
		IPATranscript retVal = new IPATranscript();

		if(transcript.trim().length() > 0) {
			try {
				org.antlr.v4.runtime.CharStream charStream = CharStreams.fromString(transcript);
				UnicodeIPALexer lexer = new UnicodeIPALexer(charStream);
				UnicodeIPAParserErrorListener errorListener = new UnicodeIPAParserErrorListener();
				lexer.addErrorListener(errorListener);
				org.antlr.v4.runtime.CommonTokenStream tokenStream = new org.antlr.v4.runtime.CommonTokenStream(lexer);

				UnicodeIPAParser parser = new UnicodeIPAParser(tokenStream);
				UnicodeIPAParserListener listener = new UnicodeIPAParserListener();
				parser.setErrorHandler(new UnicodeIPAParserErrorStrategy(listener));
				parser.addParseListener(listener);
				parser.start();

				if (errorListener.getParseExceptions().size() > 0) {
					throw errorListener.getParseExceptions().get(0);
				}
				if(listener.getParserErrors().size() > 0) {
					throw listener.getParserErrors().get(0);
				}

				retVal = listener.getTranscript();
			} catch (IPAParserException e) {
				final ParseException pe = new ParseException(transcript + ": " + e.getLocalizedMessage(), e.getPositionInLine());
				pe.addSuppressed(e);
				throw pe;
			}
		}

		return retVal;
	}

	/**
	 * Create an empty transcript
	 */
	public IPATranscript() {
		this(new IPAElement[0]);
	}

	/**
	 * Create a new transcript for a list of phones.
	 */
	public IPATranscript(List<IPAElement> phones) {
		this(phones.toArray(new IPAElement[0]));
	}

	/**
	 * Createa  new transcript with the phones from the
	 * given transcript.
	 *
	 * @param ipa
	 */
	public IPATranscript(IPATranscript ipa) {
		this(Arrays.copyOf(ipa.transcription, ipa.length()));
	}

	/**
	 * Create a new transcript from an array of phones.
	 */
	public IPATranscript(IPAElement ... phones) {
		super();
		this.transcription = phones;
		
		extSupport.initExtensions();
	}


	public IPATranscript(Object ...elements) {
		super();
		this.transcription = new IPAElement[elements.length];
		for(int i = 0; i < elements.length; i++) {
			final Object ele = elements[i];
			if(ele instanceof IPAElement) {
				transcription[i] = (IPAElement)ele;
			} else {
				transcription[i] = (new IPAElementFactory()).createPhone();
			}
		}
		extSupport.initExtensions();
	}

	/**
	 * Length of transcription (in elements)
	 *
	 * @return length
	 */
	public int length() {
		return transcription.length;
	}

	public boolean matches(String phonex) {
		return matches(phonex, 0);
	}

	/**
	 * Returns <code>true</code> if this transcript matches
	 * the given phonex.
	 *
	 * @param phonex
	 * @param flags
	 * @return <code>true</code> if the transcript matches
	 *  the given phonex, <code>false</code> otherwise
	 *
	 * @throws PhonexPatternException if the given phonex
	 *  is not valid
	 */
	public boolean matches(String phonex, int flags) {
		boolean retVal = false;

		PhonexPattern pattern = PhonexPattern.compile(phonex, flags);
		PhonexMatcher matcher = pattern.matcher(this);
		retVal = matcher.matches();

		return retVal;
	}

	public boolean contains(String phonex) {
		return contains(phonex, 0);
	}

	/**
	 * Returns <code>true</code> if this transcript contains
	 * the given phonex pattern.
	 *
	 * @param phonex
	 * @param flags
	 * @return <code>true</code> if the transcript contains
	 *  the given phonex pattern, <code>false</code> otherwise
	 *
	 * @throws PhonexPatternException if the given phonex
	 *  is not valid
	 */
	public boolean contains(String phonex, int flags) {
		boolean retVal = false;

		PhonexPattern pattern = PhonexPattern.compile(phonex, flags);
		PhonexMatcher matcher = pattern.matcher(this);
		retVal = matcher.find();

		return retVal;
	}

	/**
	 * Get the element at specified index
	 *
	 * @param index
	 *
	 * @return element
	 *
	 * @throws ArrayIndexOutOfBoundsException
	 */
	public IPAElement elementAt(int index) {
		return transcription[index];
	}

	/**
	 * Return the ipa element index of
	 * the given string index.
	 *
	 * @param charIdx
	 * @return ipaIndex
	 *
	 */
	public int ipaIndexOf(int charIdx) {
		int retVal = -1;

		final IPATranscriptBuilder builder = new IPATranscriptBuilder();
		for(int i = 0; i < length(); i++) {
			builder.append(elementAt(i));
			if(builder.toIPATranscript().toString().length() > charIdx) {
				retVal = i;
				break;
			}
		}

		return retVal;
	}

	/**
	 * Return the index of the given element
	 *
	 * @param ele
	 *
	 * @return index of element or < 0 if not found
	 */
	public int indexOf(IPAElement ele) {
		int retVal = -1;
		int idx = 0;
		for(IPAElement e:this) {
			if(ele == e) {
				retVal = idx;
				break;
			}
			++idx;
		}
		return retVal;
	}

	/**
	 * Returns the index of the first phone of the given phonex pattern
	 * in this transcript.
	 *
	 * @param phonex
	 *
	 * @return the index of the first phone in the given pattern or
	 *  -1 if not found
	 *
	 *  @throws PhonexPatternException if the given phonex
	 *   is not valid
	 */
	public int indexOf(String phonex) {
		int retVal = -1;

		PhonexPattern pattern = PhonexPattern.compile(phonex);
		PhonexMatcher matcher = pattern.matcher(this);

		if(matcher.find()) {
			retVal = matcher.start();
		}

		return retVal;
	}

	/**
	 * Returns the index of the first phone of the given phonex pattern
	 * in this transcript starting at the given phone index.
	 *
	 * @param phonex
	 * @param index of the phone to start searching from
	 *
	 * @return the index of the first phone in the given pattern or
	 *  -1 if not found
	 *
	 *  @throws PhonexPatternException if the given phonex
	 *   is not valid
	 */
	public int indexOf(String phonex, int index) {
		int retVal = -1;

		PhonexPattern pattern = PhonexPattern.compile(phonex);
		PhonexMatcher matcher = pattern.matcher(this);

		if(matcher.find(index)) {
			retVal = matcher.start();
		}

		return retVal;
	}

	/**
	 * Return the index of the first element in the given
	 * transcript.
	 *
	 * @param transcript
	 * @return the index or -1 if not found
	 */
	public int indexOf(IPATranscript transcript) {
		if(transcript == null || transcript.length() == 0) return -1;
		int retVal = -1;
		int idx = 0;
		for(IPAElement ele:this) {
			if(ele == transcript.elementAt(0)) {
				retVal = idx;
				break;
			}
			++idx;
		}

		if(retVal >= 0) {
			// test rest of transcript
			for(IPAElement ele:transcript) {
				if(idx >= length() || !(ele == elementAt(idx++))) {
					return -1;
				}
			}
			return retVal;
		}
		return -1;
	}

	public IPATranscript[] split(String phonex) {
		return split(phonex, 0);
	}

	/**
	 * Split transcript using the given phonex pattern
	 * as a delimiter.
	 *
	 * @param phonex
	 * @param flags
	 */
	public IPATranscript[] split(String phonex, int flags) {
		final PhonexPattern pattern = PhonexPattern.compile(phonex, flags);
		final PhonexMatcher matcher = pattern.matcher(this);

		final List<IPATranscript> splitVals = new ArrayList<IPATranscript>();
		int currentStart = 0;
		while(matcher.find()) {
			final int matchStart = matcher.start();

			if(currentStart == 0 && matchStart == 0)
				continue;

			final IPATranscript splitValue =
					new IPATranscript(Arrays.copyOfRange(transcription, currentStart, matchStart));
			splitVals.add(splitValue);

			currentStart = matcher.end();
		}
		if(currentStart < this.length()) {
			final IPATranscript finalValue =
					new IPATranscript(Arrays.copyOfRange(transcription, currentStart, transcription.length));
			splitVals.add(finalValue);
		}
		return splitVals.toArray(new IPATranscript[0]);
	}

	/**
	 * Return a subsection of this transcription.
	 *
	 * @param start
	 * @param end
	 *
	 * @return a new IPATranscript which is a sub-section of this transcription
	 *
	 * @throws ArrayIndexOutOfBoundsException if either <code>start</code> or <code>end</code>
	 *  are out of bounds
	 */
	public IPATranscript subsection(int start, int end) {
		return new IPATranscript(Arrays.copyOfRange(transcription, start, end));
	}

	/**
	 * Replace the first occurrence of the a phonex expression
	 * with the given replacement.
	 *
	 * @param pattern
	 * @param replacement
	 *
	 * @return a new IPA transcript
	 */
	public IPATranscript replaceFirst(String pattern, String replacement) throws ParseException {
		PhonexPattern phonex = PhonexPattern.compile(pattern);
		IPATranscript replace = IPATranscript.parseIPATranscript(replacement);
		return replaceFirst(phonex, replace);
	}

	/**
	 * Replace the first occurrence of the a phonex expression
	 * with the given replacement.
	 *
	 * @param pattern
	 * @param replacement
	 *
	 * @return a new IPA transcript
	 */
	public IPATranscript replaceFirst(PhonexPattern pattern, IPATranscript replacement) {
		IPATranscriptBuilder builder = new IPATranscriptBuilder();
		PhonexMatcher matcher = pattern.matcher(this);

		if(matcher.find()) {
			matcher.appendReplacement(builder, replacement);
		}
		matcher.appendTail(builder);

		return builder.toIPATranscript();
	}

	/**
	 * Replace all occurrences of the a phonex expression with
	 * the given replacement.
	 *
	 * @param pattern
	 * @param replacement
	 *
	 * @return a new IPA transcript
	 */
	public IPATranscript replaceAll(String pattern, String replacement) throws ParseException {
		PhonexPattern phonex = PhonexPattern.compile(pattern);
		IPATranscript replace = IPATranscript.parseIPATranscript(replacement);
		return replaceAll(phonex, replace);
	}

	/**
	 * Replace all occurrences of the a phonex expression with
	 * the given replacement.
	 *
	 * @param pattern
	 * @param replacement
	 *
	 * @return a new IPA transcript
	 */
	public IPATranscript replaceAll(PhonexPattern pattern, IPATranscript replacement) {
		IPATranscriptBuilder builder = new IPATranscriptBuilder();
		PhonexMatcher matcher = pattern.matcher(this);

		while(matcher.find()) {
			matcher.appendReplacement(builder, replacement);
		}
		matcher.appendTail(builder);

		return builder.toIPATranscript();
	}

	/**
	 * Return a new IPATranscript that include the contents
	 * of this transcript along with the contents of the given
	 * transcript appended at the end.
	 *
	 * @param ipa
	 */
	public IPATranscript append(IPATranscript ipa) {
		final IPATranscriptBuilder builder = new IPATranscriptBuilder();
		return builder.append(this)
				.append(ipa)
				.toIPATranscript();
	}

	/**
	 * Create a new transcript with all punctuation
	 * removed (except word boundaries).
	 *
	 * Same as <code>removePunctuation(false)</code>
	 *
	 * @return the filtered transcript
	 */
	public IPATranscript removePunctuation() {
		return removePunctuation(false);
	}

	/**
	 * Create a new transcript with all punctuation
	 * remove.  Option to ignore word boundaries
	 * can be provided.
	 *
	 * @param ignoreWordBoundaries
	 *
	 * @return the filtered transcript
	 */
	public IPATranscript removePunctuation(boolean ignoreWordBoundaries) {
		final PunctuationFilter filter = new PunctuationFilter(ignoreWordBoundaries);
		accept(filter);
		return filter.getIPATranscript();
	}

	/**
	 * Remove all diacritics from phones and compound phones.
	 *
	 * @return transcript with filtered diacritics
	 */
	public IPATranscript stripDiacritics() {
		final DiacriticFilter filter = new DiacriticFilter();
		accept(filter);
		return filter.getIPATranscript();
	}
	
	/**
	 * Strip only diacritics found in given diacritics
	 * 
	 * @param selectedDiacritics
	 */
	public IPATranscript stripDiacritics(Collection<Diacritic> selectedDiacritics) {
		if(selectedDiacritics.size() == 0) {
			return new IPATranscript(this);
		} else {
			Predicate<Diacritic> pred = (dia) -> {
				return selectedDiacritics.parallelStream()
						.filter( (d) -> dia.getText().contentEquals(d.getText()) )
						.count() == 0;
			};
			return stripDiacritics(pred);
		}
	}
	
	/**
	 * Strip diacritics except those found in the given diacritics
	 * 
	 * @param selectedDiacritics
	 */
	public IPATranscript stripDiacriticsExcept(Collection<Diacritic> selectedDiacritics) {
		if(selectedDiacritics.size() == 0) {
			return stripDiacritics();
		} else {
			Predicate<Diacritic> pred = (dia) -> {
				return selectedDiacritics.parallelStream()
						.filter( (d) -> dia.getText().contains(d.getText()) )
						.count() > 0;
			};
			return stripDiacritics(pred);		
		}
	}
	
	/**
	 * Strip diacritics from transcript using custom filter.
	 * 
	 * @param diacriticFilter
	 * @return
	 */
	public IPATranscript stripDiacritics(Predicate<Diacritic> diacriticFilter) {
		final DiacriticFilter filter = new DiacriticFilter(diacriticFilter);
		accept(filter);
		return filter.getIPATranscript();
	}

	/*
	 * Static utility methods for removing diacritics from strings
	 */
	private static boolean keepCharacter(char ch, boolean onlyOrExcept, Collection<Diacritic> selectedDiacritics) {
		FeatureMatrix fm = FeatureMatrix.getInstance();
		Collection<Character> dias = fm.getCharactersWithFeature("diacritic");

		// don't strip ligatures
		dias.remove(Character.valueOf('\u035c'));
		dias.remove(Character.valueOf('\u0361'));
		dias.remove(Character.valueOf('\u0362'));

		if(dias.contains(ch)) {
			boolean inSet = selectedDiacritics.stream().filter( d -> d.getCharacter() == ch ).findFirst().isPresent();

			if(onlyOrExcept) {
				return !inSet;
			} else {
				return inSet;
			}
		} else {
			return true;
		}
	}

	public static String stripDiacriticsFromText(String text, boolean onlyOrExcept, Collection<Diacritic> selectedDiacritics) {
		StringBuffer buffer = new StringBuffer();
		for(int i = 0; i < text.length(); i++) {
			char ch = text.charAt(i);
			if(keepCharacter(ch, onlyOrExcept, selectedDiacritics)) {
				buffer.append(ch);
			}
		}
		return buffer.toString();
	}

	/**
	 * Create a list of IPAElements which produce a sound.
	 *
	 * @return audible phones
	 */
	public IPATranscript audiblePhones() {
		final AudiblePhoneVisitor visitor = new AudiblePhoneVisitor();
		accept(visitor);
		return new IPATranscript(visitor.getPhones());
	}

	/**
	 * Break the transcript into syllables.
	 *
	 * @return syllables
	 */
	public List<IPATranscript> syllables() {
		final SyllableVisitor visitor = new SyllableVisitor();
		accept(visitor);
		return Collections.unmodifiableList(visitor.getSyllables());
	}

	/**
	 * Return syllables, including intra-word pauses.
	 *
	 * @return syllables and pauses
	 */
	public List<IPATranscript> syllablesAndPauses() {
		final SyllableAndPausesVisitor visitor = new SyllableAndPausesVisitor();
		accept(visitor);
		return Collections.unmodifiableList(visitor.getSyllables());
	}

	/**
	 * Reset syllabification for the transcript.
	 *
	 */
	public void resetSyllabification() {
		final PunctuationFilter filter = new PunctuationFilter();
		accept(filter);
		for(IPAElement ele:filter.getIPATranscript()) {
			ele.setScType(SyllableConstituentType.UNKNOWN);
		}
	}

	/**
	 * Has this transcription been syllabified?
	 *
	 * @return true if any phones have syllabification info set to something other than UNKNOWN
	 */
	public boolean hasSyllableInformation() {
		final PunctuationFilter filter = new PunctuationFilter(true);
		accept(filter);
		for(IPAElement ele:filter.getIPATranscript()) {
			if(ele.getScType() != SyllableConstituentType.UNKNOWN)
				return true;
		}
		return false;
	}

	private List<IPATranscript> wordList = null;
	/**
	 * Break the transcript into words
	 *
	 * @return words
	 */
	public List<IPATranscript> words() {
		if(wordList == null) {
			final WordVisitor visitor = new WordVisitor();
			accept(visitor);
			wordList = visitor.getWords();
		}
		return wordList;
	}

	@Override
	public void accept(Visitor<IPAElement> visitor) {
		// visit each phone in sequence
		for(IPAElement p:this) {
			visitor.visit(p);
		}
	}

	/**
	 * Finds the index of the given ipa element in
	 * the string representation of the transcript.
	 *
	 * @param ele
	 * @return the string index of the specified element
	 *  or < 0 if not found
	 */
	public int stringIndexOfElement(IPAElement ele) {
		final int eleIdx = indexOf(ele);
		if(eleIdx >= 0) {
			return stringIndexOfElement(eleIdx);
		} else {
			return eleIdx;
		}
	}

	/**
	 * Find the index of the given ipa transcript in
	 * the string representation of this transcript.
	 *
	 * @param transcript
	 * @return the string index of the given transcript
	 *  or < 0 if not found
	 */
	public int stringIndexOf(IPATranscript transcript) {
		if(transcript.length() > 0)
			return stringIndexOfElement(transcript.elementAt(0));
		else
			return -1;
	}

	/**
	 * Finds the index of the specified ipa element in
	 * the string representation of the transcript.
	 *
	 * @param index
	 * @return the string index of the specified element
	 *  or < 0 if not found
	 */
	public int stringIndexOfElement(int index) {
		if(index < 0 || index > length())
			throw new ArrayIndexOutOfBoundsException(index);
		final IPATranscript before =
				new IPATranscript(Arrays.copyOfRange(transcription, 0, index));
		return before.toString().length();
	}

	public String getStressPattern() {
		return StressPattern.getStressPattern(this.toList());
	}

	/**
	 * Does this transcript's stress pattern match the given
	 * {@link StressPattern}
	 *
	 * @param pattern
	 *
	 * @return <code>true</code> if pattern matches, <code>false</code> otherwise
	 */
	public boolean matchesStressPattern(String pattern) {
		boolean retVal = false;
		try {
			final StressPattern sp = StressPattern.compile(pattern);
			final String mySp = StressPattern.getStressPattern(this.toList());
			final List<StressMatcherType> stTypes =
					StressMatcherType.toStressMatcherList(mySp);

			retVal = sp.matches(stTypes);
		} catch (ParseException e) {
			Logger.getLogger(getClass().getName()).warning(e.getLocalizedMessage());
		}
		return retVal;
	}

	/**
	 * Does this transcript contain the given {@link StressPattern}
	 *
	 * @param pattern
	 * @return <code>true</code> if this transcript contains the stress
	 *  pattern, <code>false</code> otherwise
	 */
	public boolean containsStressPattern(String pattern) {
		boolean retVal = false;
		try {
			final StressPattern sp = StressPattern.compile(pattern);
			final String mySp = StressPattern.getStressPattern(this.toList());
			final List<StressMatcherType> stTypes =
					StressMatcherType.toStressMatcherList(mySp);

			retVal = sp.findWithin(stTypes);
		} catch (ParseException e) {
			Logger.getLogger(getClass().getName()).warning(e.getLocalizedMessage());
		}
		return retVal;
	}

	/**
	 * Find all occurrences of the given {@link StressPattern}
	 *
	 * @param pattern
	 * @return
	 */
	public List<IPATranscript> findStressPattern(String pattern) {
		List<IPATranscript> retVal = new ArrayList<IPATranscript>();

		try {
			final StressPattern sp = StressPattern.compile(pattern);
			final String mySp = StressPattern.getStressPattern(this.toList());
			final List<StressMatcherType> stTypes =
					StressMatcherType.toStressMatcherList(mySp);

			final List<Range> ranges = sp.findRanges(stTypes);
			for(Range range:ranges) {
				final Range phoneRange =
						StressPattern.convertSPRToPR(this.toList(), mySp, range);
				final IPATranscript subT = subsection(phoneRange.getStart(), phoneRange.getEnd());
				retVal.add(subT);
			}
		} catch (ParseException e) {
			Logger.getLogger(getClass().getName()).warning(e.getLocalizedMessage());
		}

		return retVal;
	}

	public String getCvPattern() {
		final CoverVisitor visitor = new CoverVisitor("G=\\g; C=\\c; V=\\v");
		accept(visitor);
		return visitor.getIPATranscript().toString();
	}

	public boolean matchesCVPattern(String pattern) {
		boolean retVal = false;

		try {
			final CVSeqPattern cvPattern = CVSeqPattern.compile(pattern);
			final String myCVPattern = CVSeqPattern.getCVSeq(this.toList());
			final List<CVSeqType> cvTypes = CVSeqType.toCVSeqMatcherList(myCVPattern);

			retVal = cvPattern.matches(cvTypes);
		} catch (ParseException e) {
			Logger.getLogger(getClass().getName()).warning(e.getLocalizedMessage());
		}

		return retVal;
	}

	public boolean containsCVPattern(String pattern) {
		boolean retVal = false;

		try {
			final CVSeqPattern cvPattern = CVSeqPattern.compile(pattern);
			final String myCVPattern = CVSeqPattern.getCVSeq(this.toList());
			final List<CVSeqType> cvTypes = CVSeqType.toCVSeqMatcherList(myCVPattern);

			retVal = cvPattern.findWithin(cvTypes);
		} catch (ParseException e) {
			Logger.getLogger(getClass().getName()).warning(e.getLocalizedMessage());
		}

		return retVal;
	}

	public List<IPATranscript> findCVPattern(String pattern) {
		final List<IPATranscript> retVal = new ArrayList<IPATranscript>();

		try {
			final CVSeqPattern cvPattern = CVSeqPattern.compile(pattern);
			final String myCVPattern = CVSeqPattern.getCVSeq(this.toList());
			final List<CVSeqType> cvTypes = CVSeqType.toCVSeqMatcherList(myCVPattern);

			final List<Range> ranges = cvPattern.findRanges(cvTypes);
			for(Range range:ranges) {
				final Range phoneRange =
						CVSeqPattern.convertCVRangeToPhoneRange(this.toList(), range);
				final IPATranscript subT = subsection(phoneRange.getStart(), phoneRange.getEnd());
				retVal.add(subT);
			}
		} catch (ParseException e) {
			Logger.getLogger(getClass().getName()).warning(e.getLocalizedMessage());
		}

		return retVal;
	}

	/**
	 * Get default CGV pattern cover
	 *
	 * @return transcript covered using  G=\g; C=\c; V=\v
	 */
	public IPATranscript cover() {
		return cover("G=\\g; C=\\c; V=\\v");
	}

	/**
	 * Cover using string encoding symbol map.
	 * 
	 * @param symbolMap
	 */
	public IPATranscript cover(String symbolMap) {
		final CoverVisitor visitor = new CoverVisitor(symbolMap);
		accept(visitor);
		return visitor.getIPATranscript();
	}
	
	/**
	 * Cover the image of {@link IPAElement}s in the transcript using
	 * the given symbol map.  The result will be a new {@link IPATranscript}
	 * whose elements have the same feature set and syllablifiation information
	 * as the original transcript.  Elements which are not matched using
	 * the symbol table are copied as-is.
	 * 
	 * @param matchers
	 * @param symbolMap
	 * @param includeStress include stress marker elements
	 * @param includeSyllableBoundaries keep syllable boundaries (i.e., '.') elements
	 * @param includeDiacritics keep diacritics on elements
	 */
	public IPATranscript cover(List<PhoneMatcher> matchers, Map<PhoneMatcher, Character> symbolMap, boolean includeStress,
			boolean includeSyllableBoundaries, boolean includeDiacritics) {
		final CoverVisitor visitor = new CoverVisitor(matchers, symbolMap, includeStress, includeSyllableBoundaries, includeDiacritics);
		accept(visitor);
		return visitor.getIPATranscript();
	}
	
	/**
	 * Cover IPATranscript using provided cover characters.
	 * 
	 * @param consonantCover
	 * @param vowelCover
	 *
	 * @return
	 */
	public IPATranscript cover(Character consonantCover, Character vowelCover) {
		PhoneMatcher consonantMatcher = new FeatureSetMatcher(FeatureSet.fromArray(new String[]{"consonant"}));
		PhoneMatcher vowelMatcher = new FeatureSetMatcher(FeatureSet.fromArray(new String[]{"vowel"}));
		
		Map<PhoneMatcher, Character> coverMap = new LinkedHashMap<PhoneMatcher, Character>();
		coverMap.put(consonantMatcher, consonantCover);
		coverMap.put(vowelMatcher, vowelCover);
		
		return cover(List.of(consonantMatcher, vowelMatcher), coverMap, true, true, true);
	}
	
	public IPATranscript cover(String consonantCover, String vowelCover) {
		return cover(consonantCover.charAt(0), vowelCover.charAt(0));
	}

	@Override
	public Set<Class<?>> getExtensions() {
		return extSupport.getExtensions();
	}

	@Override
	public <T> T getExtension(Class<T> cap) {
		return extSupport.getExtension(cap);
	}

	@Override
	public <T> T putExtension(Class<T> cap, T impl) {
		return extSupport.putExtension(cap, impl);
	}

	@Override
	public <T> T removeExtension(Class<T> cap) {
		return extSupport.removeExtension(cap);
	}

	@Override
	public String toString() {
		return toString(false);
	}

	public String toString(final boolean includeScType) {
		final ToStringVisitor visitor = new ToStringVisitor(includeScType);
		accept(visitor);
		return visitor.buffer.toString();
	}

	@Override
	public int hashCode() {
		return toString(true).hashCode();
	}

	@Override
	public boolean equals(Object ipa) {
		if(!(ipa instanceof IPATranscript)) return false;
		return toString(true).equals(((IPATranscript)ipa).toString(true));
	}

	/**
	 * Get an immutable list representation of this
	 * IPATranscript.
	 *
	 * @return list
	 */
	public List<IPAElement> toList() {
		return Collections.unmodifiableList(Arrays.asList(transcription));
	}

	@Override
	public Iterator<IPAElement> iterator() {
		return toList().iterator();
	}

	@Override
	public int compareTo(IPATranscript o) {
		final Comparator<IPAElement> comparator = new IPAElementComparator();
		return compareTo(o, comparator);
	}

	public int compareTo(IPATranscript ipa, Comparator<IPAElement> comparator) {
		int retVal = 0;

		final int maxCompareLength = Math.min(length(), ipa.length());
		for(int i = 0; i < maxCompareLength; i++) {
			final IPAElement e1 = elementAt(i);
			final IPAElement e2 = ipa.elementAt(i);
			retVal = comparator.compare(e1, e2);
			if(retVal != 0) break;
		}

		if(retVal == 0) {
			retVal = ((Integer)length()).compareTo(ipa.length());
		}

		return retVal;
	}
	
	public class ToStringVisitor extends VisitorAdapter<IPAElement> {

		final StringBuilder buffer = new StringBuilder();
		
		boolean includeScType = false;
		
		public ToStringVisitor() {
			this(false);
		}
		
		public ToStringVisitor(boolean includeScType) {
			this.includeScType = includeScType;
		}
		
		@Override
		public void fallbackVisit(IPAElement obj) {
			buffer.append(obj.toString());
		}
		
		@Visits
		public void visitPhone(Phone p) {
			buffer.append(p.toString());
			if(includeScType) {
				buffer.append(":");
				final SyllabificationInfo sInfo = p.getExtension(SyllabificationInfo.class);
				if(sInfo.getConstituentType() == SyllableConstituentType.NUCLEUS && sInfo.isDiphthongMember())
					buffer.append("D");
				else
					buffer.append(sInfo.getConstituentType().getIdChar());
			}
		}
		
	}

}
