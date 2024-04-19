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
package ca.phon.stresspattern.fsa;

import ca.phon.fsa.*;
import ca.phon.stresspattern.StressMatcherType;

import java.text.ParseException;

/**
 * Compiles a given stress pattern string into
 * a finite state automata.
 *
 */
public class StressPatternCompiler {
	
	private enum Quantifier {
		ZeroOrOne,
		ZeroOrMore,
		OneOrMore;
		
		private char[] images = {
				'?',
				'*',
				'+'
		};
		
		public static char getImage(Quantifier q) {
			return q.getImage();
		}
		
		public char getImage() {
			return images[ordinal()];
		}
	}
	
//	public static SimpleFSA<StressMatcherType> compile(String matcherString) {
//		StressPatternCompiler compiler = new StressPatternCompiler();
//		return compiler.compile(matcherString);
//	}
	
	/** The state prefix */
	private final String statePrefix = "q";
	/** Variable for keeping track of state number - reset on call to compile(String) */
	private int stateIndex = 0;
	/** The current matcher string */
	private String currentMatcher;
	
	public StressPatternCompiler() {
		super();
	}
	
	public SimpleFSA<StressMatcherType> compile(String matcherString) 
		throws ParseException {
		
		stateIndex = 0;
		currentMatcher = matcherString;
		
//		Tokenizer tokenizer = getTokenizer();
//		TokenizerSource source = new StringSource(matcherString);
//		tokenizer.setSource(source);

		return new SimpleFSA<>();
	}
	
//	private SimpleFSA<StressMatcherType> tokensToFSA(Tokenizer tokenizer)
//		throws ParseException {
//		SimpleFSA<StressMatcherType> fsa = new SimpleFSA<StressMatcherType>();
//
//		// add initial state
//		String initialState = getNextStateName();
//		fsa.addState(initialState);
//		fsa.setInitialState(initialState);
//
//		// test first token
//		Token token = null;
//		try {
//			token = tokenizer.nextToken();
//		} catch (TokenizerException e) {
//			LOGGER.error( e.getLocalizedMessage(), e);
//		}
//		if(token != null
//				&& !token.getImage().equals("#"))
//			tokenizer.setReadPositionAbsolute(0);
//
//		StressMatcherType currentType = null;
//		while((currentType = readMatcher(tokenizer)) != null) {
//			newTransition(fsa, currentType);
//
//			// attempt to read a quantifier
//			if(tokenizer.hasMoreToken()) {
//				Token nextToken = null;
//				try {
//					nextToken = tokenizer.nextToken();
//				} catch (TokenizerException e) {
//					throw new ParseException(currentMatcher,
//							tokenizer.getReadPosition());
//				}
//
//				if(nextToken.getCompanion() != null) {
//					if(nextToken.getCompanion() instanceof Quantifier) {
//						// handle quantifier
//						Quantifier q = (Quantifier)nextToken.getCompanion();
//						if(q == Quantifier.OneOrMore)
//							makeOneOrMore(fsa, currentType);
//						else if(q == Quantifier.ZeroOrMore)
//							makeZeroOrMore(fsa, currentType);
//						else if(q == Quantifier.ZeroOrOne)
//							makeZeroOrOne(fsa, currentType);
//						else
//							// should never get here
//							throw new ParseException(currentMatcher, tokenizer.getReadPosition());
//
//					} else {
//						// reset tokenzier position
//						tokenizer.setReadPositionRelative(-1*nextToken.getImage().length());
//					}
//				} else {
//					if(nextToken.getType() == Token.EOF)
//						break;
//					else if(nextToken.getType() == Token.WHITESPACE) {
//						// should actually be a WordBoundary type
//						// reset position
//						tokenizer.setReadPositionRelative(-1*nextToken.getImage().length());
//					} else if(nextToken.getImage().equals("#")) {
//						makeHashAtEnd(fsa);
//						break;
//					} else {
//						// token should not be here without a companion otherwise
//						// throw an exception
//						throw new ParseException(currentMatcher, tokenizer.getReadPosition());
//					}
//				}
//			}
//		}
//
//
//		return fsa;
//	}
//
//	private StressMatcherType readMatcher(Tokenizer tokenizer)
//		throws ParseException {
//
//		if(!tokenizer.hasMoreToken())
//			return null;
//
//		Token token = null;
//		try {
//			token = tokenizer.nextToken();
//		} catch (TokenizerException e) {
//			throw new ParseException(currentMatcher,
//					tokenizer.getReadPosition());
//		}
//
//		if(token.getImage().length() == 0)
//			return null;
//
//		if(token.getImage().equals("#"))
//			return null;
//
//		if(token.getCompanion() != null) {
//			if(token.getCompanion() instanceof StressMatcherType) {
//				return (StressMatcherType)token.getCompanion();
//			} else {
//				throw new ParseException(currentMatcher, tokenizer.getReadPosition());
//			}
//		} else {
//			if(token.getType() == Token.WHITESPACE) {
//				return StressMatcherType.WordBoundary;
//			} else
//				throw new ParseException(currentMatcher, tokenizer.getReadPosition());
//		}
//
//	}
	
	/**
	 * Strips the final states from the fsa and returns the
	 * stripped values.
	 * 
	 * @param fsa
	 */
	private String[] stripFinalStates(SimpleFSA<StressMatcherType> fsa) {
		String[] currentFinals = fsa.getFinalStates();
		for(String finalState:currentFinals)
			fsa.removeFinalState(finalState);
		return currentFinals;
	}
	
	/**
	 * Returns the next state name
	 * 
	 */
	private String getNextStateName() {
		return statePrefix + (stateIndex++);
	}
	
	/**
	 * This method does the following:
	 *  * removes all current final states
	 *  * creates a new state
	 *  * create a transition from all old final states to the new one
	 *  * makes the new state final
	 * @param fsa
	 * @param matcher the matcher to use for transitions
	 */
	private void newTransition(SimpleFSA<StressMatcherType> fsa, StressMatcherType matcher) {
		// strip final states
		String[] oldFinals = stripFinalStates(fsa);
		
		// create a new state and make it final
		String newState = getNextStateName();
		fsa.addState(newState);
		fsa.addFinalState(newState);
		
		for(String oldFinal:oldFinals) {
			StressMatcherTransition transition = new StressMatcherTransition(matcher);
			transition.setFirstState(oldFinal);
			transition.setToState(newState);
			
			fsa.addTransition(transition);
		}
		
		if(oldFinals.length == 0) {
			StressMatcherTransition	transition = new StressMatcherTransition(matcher);
			transition.setFirstState(fsa.getInitialState());
			transition.setToState(newState);
			
			fsa.addTransition(transition);
		}
	}
	
	private void makeZeroOrOne(SimpleFSA<StressMatcherType> fsa, StressMatcherType matcher) {
		// for each final state, find the transitions to it and
		// make the first state final as well
		for(String finalState:fsa.getFinalStates()) {
			// get transtions to the final state
			for(FSATransition<StressMatcherType> trans:fsa.getTransitionsToState(finalState)) {
				fsa.addFinalState(trans.getFirstState());
			}
		}
	}

	private void makeZeroOrMore(SimpleFSA<StressMatcherType> fsa, StressMatcherType matcher) {
		// for each final state, find the transitions to it and
		// make the first state final as well
		for(String finalState:fsa.getFinalStates()) {
			// get transtions to the final state
			for(FSATransition<StressMatcherType> trans:fsa.getTransitionsToState(finalState)) {
				fsa.addFinalState(trans.getFirstState());
			}
			
			StressMatcherTransition	transition = new StressMatcherTransition(matcher);
			transition.setFirstState(finalState);
			transition.setToState(finalState);
			fsa.addTransition(transition);
		}
	}
	
	private void makeOneOrMore(SimpleFSA<StressMatcherType> fsa, StressMatcherType matcher) {
//		newTransition(fsa, matcher);
		
		for(String finalState:fsa.getFinalStates()) {
			FSATransition<StressMatcherType> transition = null;
			
			transition = new StressMatcherTransition(matcher);
			transition.setFirstState(finalState);
			transition.setToState(finalState);
			fsa.addTransition(transition);
		}
	}
	
	private void makeHashAtEnd(SimpleFSA<StressMatcherType> fsa) {
		// add the anything matcher to all final states
		String newState = getNextStateName();
		
//		FeatureSetMatcher fsm = new FeatureSetMatcher();
		
		fsa.addState(newState);
		
		for(String finalState:fsa.getFinalStates()) {
			StressMatcherTransition smt = new StressMatcherTransition(StressMatcherType.DontCare);
			smt.setFirstState(finalState);
			smt.setToState(newState);
			fsa.addTransition(smt);
		}
	}
	
//	/**
//	 * Get the tokenizer
//	 */
//	private Tokenizer getTokenizer() {
//		TokenizerProperties props = new StandardTokenizerProperties();
//
//		int parseFlags =
//			Flags.F_COUNT_LINES | // count lines and cols
//			Flags.F_NO_CASE | // case insensitive
//			Flags.F_RETURN_SIMPLE_WHITESPACES; // spaces are important
//
//		props.setParseFlags(parseFlags);
//
//		props.addSpecialSequence("#");
//
//		// add stress matcher type
//		for(StressMatcherType stType:StressMatcherType.values()) {
//			props.addSpecialSequence(""+stType.getImage(), stType);
//		}
//
//		// add quantifiers
//		for(Quantifier q:Quantifier.values()) {
//			props.addSpecialSequence(""+q.getImage(), q);
//		}
//
//		return new StandardTokenizer(props);
//	}
}
