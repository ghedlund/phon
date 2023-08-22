// param setup

var PatternFilter = require("lib/PatternFilter").PatternFilter;
var WordFilter = require("lib/WordFilter").WordFilter;
var AlignedWordFilter = require("lib/TierFilter").TierFilter;
var TierFilter = require("lib/TierFilter").TierFilter;
var TierList = require("lib/TierList").TierList;
var AlignedTierFilter = require("lib/TierFilter").TierFilter;
var WordFilter = require("lib/WordFilter").WordFilter;
var ParticipantFilter = require("lib/ParticipantFilter").ParticipantFilter;
var SearchByOptions = require("lib/SearchByOptions").SearchByOptions;

/********************************
 * Setup params
 *******************************/

var filters = {
	"primary": new TierFilter("filters.primary"),
	"searchBy": new SearchByOptions("filters.searchBy"),
	"tierFilter": new PatternFilter("filters.tierFilter"),
	"alignedTierFilter": new AlignedTierFilter("filters.alignedTierFilter"),
	"word": new WordFilter("filters.word"),
	"alignedWord": new AlignedWordFilter("filters.alignedWord"),
	"addTiers": new TierList("filters.addTiers"),
	"wordTiers": new TierList("filters.wordTiers"),
	"speaker": new ParticipantFilter("filters.speaker")
};

/*
 * Globals
 */
var session;

function begin_search(s) {
	session = s;
}

function setup_params(params) {
	filters.primary.param_setup(params);
	filters.primary.set_required(true);
	
	var insertIdx = 1;

	var tierFilterSection = new SeparatorScriptParam("tierFilterHeader", "Tier Options", true);
	params.add(tierFilterSection);
	filters.tierFilter.param_setup(params);

	var alignedTierHeader = new LabelScriptParam("", "<html><b>Aligned Tier Filter</b></html>");
	params.add(alignedTierHeader);
	filters.alignedTierFilter.param_setup(params);

	// change default status of word filter for this query
	filters.word.searchByWord = false;
	filters.word.param_setup(params);
	
	filters.searchBy.param_setup(params, filters.word.searchByWordParam, null, insertIdx);

	var alignedWordHeader = new LabelScriptParam("", "<html><b>Aligned Word Filter</b></html>");
	params.add(alignedWordHeader);
	filters.alignedWord.param_setup(params);

	var alignedWordListener = new java.beans.PropertyChangeListener {
		propertyChange: function (e) {
			var enabled = e.source.getValue(e.source.paramId);
			filters.wordTiers.tiersParam.setEnabled(enabled == true);
			filters.alignedWord.setEnabled(enabled);
			filters.wordTiers.setEnabled(enabled);
		}
	};
	filters.word.searchByWordParam.addPropertyChangeListener(alignedWordListener);
	filters.alignedWord.setEnabled(filters.word.searchByWord);

	var otherDataHeader = new SeparatorScriptParam("otherDataHeader", "Additional Tier Data", true);
	params.add(otherDataHeader);
	var sep = new LabelScriptParam("", "<html><b>Add aligned groups</b></html>");
	params.add(sep);
	filters.addTiers.param_setup(params);
	var wordsep = new LabelScriptParam("", "<html><b>Add aligned words</b></html>");
	params.add(wordsep);
	filters.wordTiers.param_setup(params);
	filters.wordTiers.setEnabled(filters.word.searchByWord);

	filters.speaker.param_setup(params);
}

function isGroupedTier(tierName) {
	var retVal = true;

	var systemTierType =
	(SystemTierType.isSystemTier(tierName) ? SystemTierType.tierFromString(tierName): null);

	retVal = (systemTierType != null ? systemTierType.isGrouped(): false);

	if (systemTierType == null) {
		for (var i = 0; i < session.userTierCount; i++) {
			var userTier = session.getUserTier(i);
			if (userTier.name == tierName) {
				retVal = userTier.isGrouped();
				break;
			}
		}
	}

	return retVal;
}

function query_record(recordIndex, record) {
	if (! filters.speaker.check_speaker(record.speaker, session.date)) return;

	var searchTier = filters.primary.tier;
	var tierGrouped = isGroupedTier(searchTier);

	var groups = filters.group.getRequestedGroups(record);

	if (! tierGrouped && groups.length > 0) {
		// take only first group
		var tmp = new Array();
		tmp.push(groups[0]);
		groups = tmp;
	}

	// check aligned group for each group returned
	if (filters.alignedGroup.isUseFilter()) {
		groups = filters.alignedGroup.filter_groups(record, groups);
	}

	for (var gIdx = 0; gIdx < groups.length; gIdx++) {
		var group = groups[gIdx];
		var groupAlignedResults = new Array();

		if (filters.alignedGroup.isUseFilter()) {
			var tierList = new TierList("group");
			tierList.setTiers(filters.alignedGroup.tier);

			var alignedGroupData = tierList.getAlignedTierData(record, group, "Group");
			for(var j = 0; j < alignedGroupData[0].length; j++) {
				groupAlignedResults.push(alignedGroupData[0][j]);
			}
		}

		if (filters.word.isUseFilter()) {
			var words = filters.word.getRequestedWords(group, searchTier);

			for (var wIdx = 0; wIdx < words.length; wIdx++) {
				var wordData = words[wIdx];
				var word = wordData.word;
				var checkWord = true;
				var wordAlignedResults = new Array();

				if (filters.alignedWord.isUseFilter()) {
					var aligned = word.getTier(filters.alignedWord.tier);
					checkWord = filters.alignedWord.patternFilter.check_filter(aligned);

					if(checkWord == true) {
						var tierList = new TierList("word");
						tierList.setTiers(filters.alignedWord.tier);

						var alignedWordData = tierList.getAlignedTierData(record, word, "Word");
						for(var k = 0; k < alignedWordData[0].length; k++) {
							wordAlignedResults.push(alignedWordData[0][k]);
						}
					}
				}
				if (checkWord == true) {
					var vals = filters.primary.patternFilter.find_pattern(word.getTier(searchTier));

					// get start index of word
					var wordOffset = 0;
					if (searchTier == "Orthography") {
						wordOffset = word.getOrthographyWordLocation();
					} else if (searchTier == "IPA Target") {
						wordOffset = word.getIPATargetWordLocation();
					} else if (searchTier == "IPA Actual") {
						wordOffset = word.getIPAActualWordLocation();
					} else {
						wordOffset = word.getTierWordLocation(searchTier);
					}

					for (var i = 0; i < vals.length; i++) {
						var v = vals[i];

						var result = factory.createResult();
						result.recordIndex = recordIndex;
						result.schema = "LINEAR";

						var startIdx = wordOffset + v.start;
						var endIdx = wordOffset + v.end;

						if (v.value instanceof IPATranscript) {
							startIdx = wordOffset + word.getTier(searchTier).stringIndexOf(v.value);
							endIdx = startIdx + v.value.toString().length();
						}

						var rv = factory.createResultValue();
						rv.tierName = searchTier;
						rv.groupIndex = group.groupIndex;
						rv.range = new Range(startIdx, endIdx, true);
						rv.data = v.value;
						result.addResultValue(rv);

						for(var j = 0; j < groupAlignedResults.length; j++) {
							result.addResultValue(groupAlignedResults[j]);
						}
						for(var j = 0; j < wordAlignedResults.length; j++) {
							result.addResultValue(wordAlignedResults[j]);
						}

						if(filters.searchBy.includePositionalInfo == true) {
							var searchBy = (filters.searchBy.searchBySyllable == true ? "Syllable" : filters.searchBy.searchBy);
							result.metadata.put("Position in " + searchBy, v.position);
							result.metadata.put("Word Position", wordData.position);
						}

						var alignedGroupData = filters.groupTiers.getAlignedTierData(record, group, "Group");
						for(var j = 0; j < alignedGroupData[0].length; j++) {
							result.addResultValue(alignedGroupData[0][j]);
						}
						result.metadata.putAll(alignedGroupData[1]);

						// add metadata
						var alignedWordData = filters.wordTiers.getAlignedTierData(record, word, "Word");
						for(var j = 0; j < alignedWordData[0].length; j++) {
							result.addResultValue(alignedWordData[0][j]);
						}
						result.metadata.putAll(alignedWordData[1]);

						results.addResult(result);
					}
				}
			}
		} else {
			var vals = filters.primary.patternFilter.find_pattern(group.getTier(searchTier));

			for (var i = 0; i < vals.length; i++) {
				var v = vals[i];

				var result = factory.createResult();
				result.recordIndex = recordIndex;
				result.schema = "LINEAR";

				var startIdx = v.start;
				var endIdx = v.end;

				if (v.value instanceof IPATranscript) {
					// we need to convert phone to string range
					startIdx = group.getTier(searchTier).stringIndexOf(v.value);
					endIdx = startIdx + v.value.toString().length();
				}

				var rv = factory.createResultValue();
				rv.tierName = searchTier;
				rv.groupIndex = group.groupIndex;
				rv.range = new Range(startIdx, endIdx, true);
				rv.data = v.value;
				result.addResultValue(rv);

				for(var j = 0; j < groupAlignedResults.length; j++) {
					result.addResultValue(groupAlignedResults[j]);
				}

				var alignedGroupData = filters.groupTiers.getAlignedTierData(record, group, "Group");
				for(var j = 0; j < alignedGroupData[0].length; j++) {
					result.addResultValue(alignedGroupData[0][j]);
				}

				if(filters.searchBy.includePositionalInfo == true) {
					var searchBy = (filters.searchBy.searchBySyllable == true ? "Syllable" : filters.searchBy.searchBy);
					result.metadata.put("Position in " + searchBy, v.position);
				}

				result.metadata.putAll(alignedGroupData[1]);

				results.addResult(result);
			}
		}
	}
}