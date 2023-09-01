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
package ca.phon.query;

import ca.phon.project.*;
import ca.phon.project.exceptions.ProjectConfigurationException;
import ca.phon.script.params.EnumScriptParam;
import ca.phon.session.Session;
import org.apache.logging.log4j.LogManager;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.*;
import java.util.*;

/**
 * Tests of the Phones.js query
 *
 */
@RunWith(Parameterized.class)
public class TestPhonesQuery extends TestQuery {

	private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(TestPhonesQuery.class.getName());

	public TestPhonesQuery(Project project, Session session, String scriptPath,
			Map<String, Object> params, int expectedResults) {
		super(project, session, scriptPath, params, expectedResults);
	}

	private final static String TEST_PROJECT = "src/test/resources/test-project";
	private final static String TEST_CORPUS = "corpus";
	private final static String TEST_SESSION = "session";

	private final static String PHONES_SCRIPT = "src/main/resources/ca/phon/query/script/Phones.js";

	@Parameters
	public static Collection<Object[]> testData() {
		final Collection<Object[]> retVal = new ArrayList<Object[]>();

		final ProjectFactory factory = new DefaultProjectFactory();
		try {
			final Project project = factory.openProject(new File(TEST_PROJECT));
			final Session session = project.openSession(TEST_CORPUS, TEST_SESSION);

			retVal.add(basicTestParams(project, session));

			retVal.add(participantNameParams(project, session));
			retVal.add(participantAge1Params(project, session));
			retVal.add(participantAge2Params(project, session));

//			retVal.add(singletonGroupFilterParams(project, session));
//			retVal.add(nonSingletonGroupFilterParams(project, session));
//			retVal.add(initialGroupFilterParams(project, session));
//			retVal.add(medialGroupFilterParams(project, session));
//			retVal.add(finalGroupFilterParams(project, session));
//			retVal.add(groupFilterParams(project, session));
//			retVal.add(alignedGroupFilterParams(project, session));

			retVal.add(allWordFilterParams(project, session));
			retVal.add(singletonWordFilterParams(project, session));
			retVal.add(nonSingletonWordFilterParams(project, session));
			retVal.add(initialWordFilterParams(project, session));
			retVal.add(medialWordFilterParams(project, session));
			retVal.add(finalWordFilterParams(project, session));
			retVal.add(wordFilterParams(project, session));
			retVal.add(alignedWordFilterParams(project, session));

//			retVal.add(allSyllablesByGroupParams(project, session));
//			retVal.add(singletonSyllableByGroupParams(project, session));
//			retVal.add(initialSyllableByGroupParams(project, session));
//			retVal.add(medialSyllableByGroupParams(project, session));
//			retVal.add(finalSyllableByGroupParams(project, session));

			retVal.add(singletonSyllableByWordParams(project, session));
			retVal.add(initialSyllableByWordParams(project, session));
			retVal.add(medialSyllableByWordParams(project, session));
			retVal.add(finalSyllableByWordParams(project, session));

//			retVal.add(primaryStressedSyllableByGroupParams(project, session));
//			retVal.add(secondaryStressedSyllableByGroupParams(project, session));
//			retVal.add(unStressedSyllableByGroupParams(project, session));

			retVal.add(deletions(project, session));
			retVal.add(epenthesis(project, session));

			// TODO metadata tests?
		} catch (IOException e) {
			LOGGER.error( e.getLocalizedMessage(), e);
		} catch (ProjectConfigurationException e) {
			LOGGER.error( e.getLocalizedMessage(), e);
		}

		return retVal;
	}

	/*
	 * Basic Phones.js test
	 */
	private static Object[] basicTestParams(Project project, Session session) {
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("filters.primary.filter", "\\c+");
		params.put("filters.primary.filterType", new EnumScriptParam.ReturnValue("Phonex", 2));

		return new Object[]{ project, session, PHONES_SCRIPT, params, 253 };
	}

	/*
	 * Test participant name filter
	 *
	 */
	private static Object[] participantNameParams(Project project, Session session) {
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("filters.primary.filter", "\\c+");
		params.put("filters.speaker.participantNames", "Anne");

		return new Object[]{ project, session, PHONES_SCRIPT, params, 88 };
	}

	private static Object[] participantAge1Params(Project project, Session session) {
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("filters.primary.filter", "\\c+");
		params.put("filters.speaker.age1Comparator", new EnumScriptParam.ReturnValue("less than", 0));
		params.put("filters.speaker.age1String", "03;00.00");

		return new Object[]{ project, session, PHONES_SCRIPT, params, 88 };
	}

	private static Object[] participantAge2Params(Project project, Session session) {
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("filters.primary.filter", "\\c+");
		params.put("filters.speaker.age1Comparator", new EnumScriptParam.ReturnValue("greater than", 2));
		params.put("filters.speaker.age1String", "03;00.00");

		return new Object[]{ project, session, PHONES_SCRIPT, params, 165 };
	}

	/*
	 * Test group position filter
	 */
	private static Object[] singletonGroupFilterParams(Project project, Session session) {
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("filters.primary.filter", "\\c+");
		params.put("filters.group.gSingleton", true);
		params.put("filters.group.gInitial", false);
		params.put("filters.group.gMedial", false);
		params.put("filters.group.gFinal", false);

		return new Object[] { project, session, PHONES_SCRIPT, params, 69 };
	}


	private static Object[] nonSingletonGroupFilterParams(Project project, Session session) {
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("filters.primary.filter", "\\c+");
		params.put("filters.group.gSingleton", false);
		params.put("filters.group.gInitial", true);
		params.put("filters.group.gMedial", true);
		params.put("filters.group.gFinal", true);

		return new Object[] { project, session, PHONES_SCRIPT, params, 184 };
	}

	private static Object[] initialGroupFilterParams(Project project, Session session) {
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("filters.primary.filter", "\\c+");
		params.put("filters.group.gSingleton", false);
		params.put("filters.group.gInitial", true);
		params.put("filters.group.gMedial", false);
		params.put("filters.group.gFinal", false);

		return new Object[] { project, session, PHONES_SCRIPT, params, 63 };
	}

	private static Object[] medialGroupFilterParams(Project project, Session session) {
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("filters.primary.filter", "\\c+");
		params.put("filters.group.gSingleton", false);
		params.put("filters.group.gInitial", false);
		params.put("filters.group.gMedial", true);
		params.put("filters.group.gFinal", false);

		return new Object[] { project, session, PHONES_SCRIPT, params, 21 };
	}

	private static Object[] finalGroupFilterParams(Project project, Session session) {
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("filters.primary.filter", "\\c+");
		params.put("filters.group.gSingleton", false);
		params.put("filters.group.gInitial", false);
		params.put("filters.group.gMedial", false);
		params.put("filters.group.gFinal", true);

		return new Object[] { project, session, PHONES_SCRIPT, params, 100 };
	}

	/*
	 * Test group filter
	 */
	private static Object[] groupFilterParams(Project project, Session session) {
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("filters.primary.filter", "\\c+");
		params.put("filters.groupPattern.filter", "d");

		return new Object[] { project, session, PHONES_SCRIPT, params, 70 };
	}

	/*
	 * Test aligned group filter
	 */
	private static Object[] alignedGroupFilterParams(Project project, Session session) {
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("filters.primary.filter", "\\c+");
		params.put("filters.alignedGroup.tier", "Orthography");
		params.put("filters.alignedGroup.patternFilter.filter", "happy");
		params.put("filters.alignedGroup.patternFilter.caseSensitive", false);

		return new Object[] { project, session, PHONES_SCRIPT, params, 10 };
	}

	/*
	 * Word position tests
	 *
	 */

	private static Object[] allWordFilterParams(Project project, Session session) {
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("filters.primary.filter", "\\c+");
		params.put("filters.word.searchByWord", true);
		params.put("filters.word.wSingleton", true);
		params.put("filters.word.wInitial", true);
		params.put("filters.word.wMedial", true);
		params.put("filters.word.wFinal", true);

		return new Object[] { project, session, PHONES_SCRIPT, params, 253 };
	}

	private static Object[] singletonWordFilterParams(Project project, Session session) {
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("filters.primary.filter", "\\c+");
		params.put("filters.word.searchByWord", true);
		params.put("filters.word.wSingleton", true);
		params.put("filters.word.wInitial", false);
		params.put("filters.word.wMedial", false);
		params.put("filters.word.wFinal", false);

		return new Object[] { project, session, PHONES_SCRIPT, params, 17 };
	}

	private static Object[] nonSingletonWordFilterParams(Project project, Session session) {
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("filters.primary.filter", "\\c+");
		params.put("filters.word.searchByWord", true);
		params.put("filters.word.wSingleton", false);
		params.put("filters.word.wInitial", true);
		params.put("filters.word.wMedial", true);
		params.put("filters.word.wFinal", true);

		return new Object[] { project, session, PHONES_SCRIPT, params, 236 };
	}

	private static Object[] initialWordFilterParams(Project project, Session session) {
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("filters.primary.filter", "\\c+");
		params.put("filters.word.searchByWord", true);
		params.put("filters.word.wSingleton", false);
		params.put("filters.word.wInitial", true);
		params.put("filters.word.wMedial", false);
		params.put("filters.word.wFinal", false);

		return new Object[] { project, session, PHONES_SCRIPT, params, 47 };
	}

	private static Object[] medialWordFilterParams(Project project, Session session) {
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("filters.primary.filter", "\\c+");
		params.put("filters.word.searchByWord", true);
		params.put("filters.word.wSingleton", false);
		params.put("filters.word.wInitial", false);
		params.put("filters.word.wMedial", true);
		params.put("filters.word.wFinal", false);

		return new Object[] { project, session, PHONES_SCRIPT, params, 116 };
	}

	private static Object[] finalWordFilterParams(Project project, Session session) {
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("filters.primary.filter", "\\c+");
		params.put("filters.word.searchByWord", true);
		params.put("filters.word.wSingleton", false);
		params.put("filters.word.wInitial", false);
		params.put("filters.word.wMedial", false);
		params.put("filters.word.wFinal", true);

		return new Object[] { project, session, PHONES_SCRIPT, params, 73 };
	}

	private static Object[] wordFilterParams(Project project, Session session) {
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("filters.primary.filter", "\\c+");
		params.put("filters.word.searchByWord", true);
		params.put("filters.word.wSingleton", true);
		params.put("filters.word.wInitial", true);
		params.put("filters.word.wMedial", true);
		params.put("filters.word.wFinal", true);
		params.put("filters.wordPattern.filter", "d");

		return new Object[] { project, session, PHONES_SCRIPT, params, 35 };
	}

	private static Object[] alignedWordFilterParams(Project project, Session session) {
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("filters.primary.filter", "\\c+");
		params.put("filters.word.searchByWord", true);
		params.put("filters.word.wSingleton", true);
		params.put("filters.word.wInitial", true);
		params.put("filters.word.wMedial", true);
		params.put("filters.word.wFinal", true);
		params.put("filters.alignedWord.tier", "Orthography");
		params.put("filters.alignedWord.patternFilter.filter", "happy");
		params.put("filters.alignedWord.patternFilter.caseSensitive", false);

		return new Object[] { project, session, PHONES_SCRIPT, params, 4 };
	}

	// syllable filter tests
	private static Object[] allSyllablesByGroupParams(Project project, Session session) {
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("filters.primary.filter", "\\c+");
		params.put("filters.syllable.searchBySyllable", true);
		params.put("filters.syllable.sSingleton", true);
		params.put("filters.syllable.sInitial", true);
		params.put("filters.syllable.sMedial", true);
		params.put("filters.syllable.sFinal", true);

		// the number of results is higher because of syllable break-up of consonant clusters
		return new Object[] { project, session, PHONES_SCRIPT, params, 263 };
	}

	private static Object[] singletonSyllableByGroupParams(Project project, Session session) {
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("filters.primary.filter", "\\c+");
		params.put("filters.word.searchByWord", false);
		params.put("filters.syllable.searchBySyllable", true);
		params.put("filters.syllable.sSingleton", true);
		params.put("filters.syllable.sInitial", false);
		params.put("filters.syllable.sMedial", false);
		params.put("filters.syllable.sFinal", false);

		return new Object[] { project, session, PHONES_SCRIPT, params, 22 };
	}

	private static Object[] singletonSyllableByWordParams(Project project, Session session) {
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("filters.primary.filter", "\\c+");
		params.put("filters.word.searchByWord", true);
		params.put("filters.syllable.searchBySyllable", true);
		params.put("filters.syllable.sSingleton", true);
		params.put("filters.syllable.sInitial", false);
		params.put("filters.syllable.sMedial", false);
		params.put("filters.syllable.sFinal", false);

		return new Object[] { project, session, PHONES_SCRIPT, params, 135 };
	}

	private static Object[] initialSyllableByGroupParams(Project project, Session session) {
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("filters.primary.filter", "\\c+");
		params.put("filters.word.searchByWord", false);
		params.put("filters.syllable.searchBySyllable", true);
		params.put("filters.syllable.sSingleton", false);
		params.put("filters.syllable.sInitial", true);
		params.put("filters.syllable.sMedial", false);
		params.put("filters.syllable.sFinal", false);

		return new Object[] { project, session, PHONES_SCRIPT, params, 78 };
	}

	private static Object[] initialSyllableByWordParams(Project project, Session session) {
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("filters.primary.filter", "\\c+");
		params.put("filters.word.searchByWord", true);
		params.put("filters.syllable.searchBySyllable", true);
		params.put("filters.syllable.sSingleton", false);
		params.put("filters.syllable.sInitial", true);
		params.put("filters.syllable.sMedial", false);
		params.put("filters.syllable.sFinal", false);

		return new Object[] { project, session, PHONES_SCRIPT, params, 60 };
	}

	private static Object[] medialSyllableByGroupParams(Project project, Session session) {
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("filters.primary.filter", "\\c+");
		params.put("filters.word.searchByWord", false);
		params.put("filters.syllable.searchBySyllable", true);
		params.put("filters.syllable.sSingleton", false);
		params.put("filters.syllable.sInitial", false);
		params.put("filters.syllable.sMedial", true);
		params.put("filters.syllable.sFinal", false);

		return new Object[] { project, session, PHONES_SCRIPT, params, 78 };
	}

	private static Object[] medialSyllableByWordParams(Project project, Session session) {
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("filters.primary.filter", "\\c+");
		params.put("filters.word.searchByWord", true);
		params.put("filters.syllable.searchBySyllable", true);
		params.put("filters.syllable.sSingleton", false);
		params.put("filters.syllable.sInitial", false);
		params.put("filters.syllable.sMedial", true);
		params.put("filters.syllable.sFinal", false);

		return new Object[] { project, session, PHONES_SCRIPT, params, 0 };
	}

	private static Object[] finalSyllableByGroupParams(Project project, Session session) {
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("filters.primary.filter", "\\c+");
		params.put("filters.word.searchByWord", false);
		params.put("filters.syllable.searchBySyllable", true);
		params.put("filters.syllable.sSingleton", false);
		params.put("filters.syllable.sInitial", false);
		params.put("filters.syllable.sMedial", false);
		params.put("filters.syllable.sFinal", true);

		return new Object[] { project, session, PHONES_SCRIPT, params, 85 };
	}

	private static Object[] finalSyllableByWordParams(Project project, Session session) {
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("filters.primary.filter", "\\c+");
		params.put("filters.word.searchByWord", true);
		params.put("filters.syllable.searchBySyllable", true);
		params.put("filters.syllable.sSingleton", false);
		params.put("filters.syllable.sInitial", false);
		params.put("filters.syllable.sMedial", false);
		params.put("filters.syllable.sFinal", true);

		return new Object[] { project, session, PHONES_SCRIPT, params, 68 };
	}

	private static Object[] primaryStressedSyllableByGroupParams(Project project, Session session) {
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("filters.primary.filter", "\\c+");
		params.put("filters.syllable.searchBySyllable", true);
		params.put("filters.syllable.sPrimary", true);
		params.put("filters.syllable.sSecondary", false);
		params.put("filters.syllable.sNone", false);

		return new Object[] { project, session, PHONES_SCRIPT, params, 191 };
	}

	private static Object[] secondaryStressedSyllableByGroupParams(Project project, Session session) {
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("filters.primary.filter", "\\c+");
		params.put("filters.syllable.searchBySyllable", true);
		params.put("filters.syllable.sPrimary", false);
		params.put("filters.syllable.sSecondary", true);
		params.put("filters.syllable.sNone", false);

		return new Object[] { project, session, PHONES_SCRIPT, params, 9 };
	}

	private static Object[] unStressedSyllableByGroupParams(Project project, Session session) {
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("filters.primary.filter", "\\c+");
		params.put("filters.syllable.searchBySyllable", true);
		params.put("filters.syllable.sPrimary", false);
		params.put("filters.syllable.sSecondary", false);
		params.put("filters.syllable.sNone", true);

		return new Object[] { project, session, PHONES_SCRIPT, params, 63 };
	}

	private static Object[] deletions(Project project, Session session) {
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("filters.primary.filter", "[\\c\\v]");
		params.put("filters.actualResultFilter.filter", "^$");
		params.put("filters.actualResultFilter.exactMatch", true);

		return new Object[] { project, session, PHONES_SCRIPT, params, 400 };
	}

	private static Object[] epenthesis(Project project, Session session) {
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("searchTier", new Object() {
			@SuppressWarnings("unused")
			public int getIndex() { return 1; }
			@Override
			public String toString() { return "IPA Actual";}
		});
		params.put("filters.primary.filter", "[\\c\\v]");
		params.put("filters.targetResultFilter.filter", "^$");
		params.put("filters.targetResultFilter.exactMatch", true);

		return new Object[] { project, session, PHONES_SCRIPT, params, 14 };
	}

}
