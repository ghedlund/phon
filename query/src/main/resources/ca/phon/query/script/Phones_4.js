/*
params =
{enum, searchTier, "IPA Target"|"IPA Actual", 0, "Search Tier"}
;
 */

var AlignedTierFilter = require("lib/TierFilter").TierFilter;
var WordFilter = require("lib/WordFilter").WordFilter;
var TierList = require("lib/TierList").TierList;
var AlignedWordFilter = require("lib/TierFilter").TierFilter;
var SyllableFilter = require("lib/SyllableFilter").SyllableFilter;
var ParticipantFilter = require("lib/ParticipantFilter").ParticipantFilter;
var PatternFilter = require("lib/PatternFilter").PatternFilter;
var PatternType = require("lib/PatternFilter").PatternType;
// var ResultType = require("lib/PhonScriptConstants").ResultType;
var SearchByOptions = require("lib/SearchByOptions").SearchByOptions;

var filters = {
    "primary": new PatternFilter("filters.primary"),
    "searchBy": new SearchByOptions("filters.searchBy"),
    "targetResultFilter": new PatternFilter("filters.targetResultFilter"),
    "actualResultFilter": new PatternFilter("filters.actualResultFilter"),
    "alignedTierFilter": new AlignedTierFilter("filters.alignedTierFilter"),
    "tierFilter": new PatternFilter("filters.tierFilter"),
    "word": new WordFilter("filters.word"),
    "addTiers": new TierList("filters.addTiers"),
    "wordTiers": new TierList("filters.wordTiers"),
    "wordPattern": new PatternFilter("filters.wordPattern"),
    "alignedWord": new AlignedWordFilter("filters.alignedWord"),
    "syllable": new SyllableFilter("filters.syllable"),
    "speaker": new ParticipantFilter("filters.speaker")
};

var includeAlignedParamInfo = {
    "id": "includeAligned",
    "title": "",
    "desc": "Include aligned phones",
    "def": true
};
var includeAlignedParam;
var includeAligned = includeAlignedParamInfo.def;

function setup_params(params) {
    filters.primary.setSelectedPatternType(PatternType.PHONEX);
    filters.primary.param_setup(params);
    filters.primary.set_required(true);

    var insertIdx = 1;

    var resultFilterSection = new SeparatorScriptParam("alignedPhonesHeader", "Aligned Phones", true);
    var targetLbl = new LabelScriptParam("", "<html><b>IPA Target Matcher</b></html>");
    var actualLbl = new LabelScriptParam("", "<html><b>IPA Actual Matcher</b></html>");

    includeAlignedParam = new BooleanScriptParam(
        includeAlignedParamInfo.id,
        includeAlignedParamInfo.desc,
        includeAlignedParamInfo.title,
        includeAlignedParamInfo.def
        );

    params.add(resultFilterSection);
    params.add(includeAlignedParam);
    params.add(targetLbl);
    filters.targetResultFilter.setSelectedPatternType(PatternType.PHONEX);
    filters.targetResultFilter.param_setup(params);
    params.add(actualLbl);
    filters.actualResultFilter.setSelectedPatternType(PatternType.PHONEX);
    filters.actualResultFilter.param_setup(params);

    var tierFilterSection = new SeparatorScriptParam("tierFilterHeader", "Tier Options", true);
    params.add(tierFilterSection);
    filters.tierFilter.param_setup(params);

    var alignedTierHeader = new LabelScriptParam("", "<html><b>Aligned Tier Filter</b></html>");
    params.add(alignedTierHeader);
    filters.alignedTierFilter.param_setup(params);

    filters.word.param_setup(params);
    filters.wordPattern.param_setup(params);
    filters.wordPattern.setEnabled(false);

    var alignedWordHeader = new LabelScriptParam("", "<html><b>Aligned Word Filter</b></html>");
    params.add(alignedWordHeader);
    filters.alignedWord.param_setup(params);
    var searchByWordListener = new java.beans.PropertyChangeListener {
        propertyChange: function (e) {
            var enabled = e.source.getValue(e.source.paramId);
            filters.wordPattern.setEnabled(enabled);
            filters.alignedWord.setEnabled(enabled);
            filters.wordTiers.setEnabled(enabled);
        }
    };
    filters.word.searchByWordParam.addPropertyChangeListener(filters.word.searchByWordParam.paramId, searchByWordListener);
    var enabled = filters.word.searchByWordParam.getValue(filters.word.searchByWordParam.paramId);
    filters.wordPattern.setEnabled(enabled);
    filters.alignedWord.setEnabled(enabled);

    filters.syllable.param_setup(params);

    filters.searchBy.includeSyllableOption = true;
    filters.searchBy.param_setup(params, filters.word.searchByWordParam, filters.syllable.searchBySyllableParam, insertIdx);

    var otherDataHeader = new SeparatorScriptParam("otherDataHeader", "Additional Tier Data", true);
    params.add(otherDataHeader);
    var sep = new LabelScriptParam("", "<html><b>Add aligned tiers</b></html>");
    params.add(sep);
    filters.addTiers.param_setup(params);
    var wordsep = new LabelScriptParam("", "<html><b>Add aligned words</b></html>");
    params.add(wordsep);
    filters.wordTiers.param_setup(params);
    filters.wordTiers.setEnabled(enabled);

    filters.speaker.param_setup(params);
}

/*
 * Globals
 */
var session;

function begin_search(s) {
    session = s;
}


/**
 * query_record
 * @param recordIndex
 * @param record
 */
function query_record(recordIndex, record) {
    // check participant filter
    if (! filters.speaker.check_speaker(record.speaker, session.date)) return;

    const tier = searchTier === "IPA Target" ? record.getIPATargetTier() : record.getIPAActualTier();
    const alignedTier = searchTier === "IPA Target" ? record.getIPAActualTier() : record.getIPATargetTier();
    const phoneAlignment = record.getPhoneAlignment();

    if(filters.tierFilter.isUseFilter()) {
        if(!filters.tierFilter.check_filter(tier.getValue())) return;
    }

    if(filters.alignedTierFilter.isUseFilter()) {
        if(!filters.alignedTierFilter.check_filter(record)) return;
    }

    // search by word
    if(filters.word.isUseFilter()) {
        const crossTierAlignment = TierAligner.calculateCrossTierAlignment(record, tier);
        const topElements = crossTierAlignment.getTopAlignmentElements();
        const selectedElements = filters.word.getRequestedWords(topElements);
        for(eleIdx = 0; eleIdx < selectedElements.length; eleIdx++) {
            var elementData = selectedElements[eleIdx];
            var element = elementData.word;

            var elementAlignedMeta = new java.util.LinkedHashMap();

            var elementAlignedResults = new Array();
            var elementAlignedData = filters.wordTiers.getAlignedTierData(crossTierAlignment, element, "Word");
            var test = "";

        }
    }

}
