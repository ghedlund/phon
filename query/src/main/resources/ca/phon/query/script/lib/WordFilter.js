/*
 * Phon - An open source tool for research in phonology.
 * Copyright (C) 2005 - 2015, Gregory Hedlund <ghedlund@mun.ca> and Yvan Rose <yrose@mun.ca>
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
exports.WordFilter = function (id) {

	var sectionTitle = "Word Options";

	var singletonParamInfo = {
		"id": id + ".wSingleton",
		"def": true,
		"title": "Singleton words:",
		"desc": "(groups with only one word)"
	};
	this.wSingleton = singletonParamInfo.def;

	var posParamInfo = {
		"id":[id + ".wInitial", id + ".wMedial", id + ".wFinal"],
		"def":[ true, true, true],
		"title": "Multiple words:",
		"desc":[ "Initial", "Medial", "Final"],
		"numCols": 0
	};
	this.wInitial = posParamInfo.def[0];
	this.wMedial = posParamInfo.def[1];
	this.wFinal = posParamInfo.def[2];

	this.searchByWordEnabled = true;
	var searchByWordParamInfo = {
		"id": id + ".searchByWord",
		"def": true,
		"title": "",
		"desc": "Search by word"
	};
	this.searchByWord = searchByWordParamInfo.def;
	this.searchByWordParam;
	// @deprecated - kept for backwards compatibility
	this.searchByWordOpt;

	var singletonGroupOpt;
	var posGroupOpt;

	/**
	 * Add params for the group, called automatically when needed.
	 *
	 * @param params
	 */
	this.param_setup = function (params) {
		// create a new section (collapsed by default)
		var sep = new SeparatorScriptParam(id+".sectionHeader", sectionTitle, true);
		params.add(sep);

		// search singleton groups.
		singletonGroupOpt = new BooleanScriptParam(
        		singletonParamInfo.id,
        		singletonParamInfo.desc,
        		singletonParamInfo.title,
        		singletonParamInfo.def);

		posGroupOpt = new MultiboolScriptParam(
        		posParamInfo.id,
        		posParamInfo.def,
        		posParamInfo.desc,
        		posParamInfo.title,
        		posParamInfo.numCols);

		var searchByWordOpt = new BooleanScriptParam(
        		searchByWordParamInfo.id,
        		searchByWordParamInfo.desc,
        		searchByWordParamInfo.title,
        		this.searchByWord);
		var searchByWordListener = new java.beans.PropertyChangeListener {
			propertyChange: function (e) {
				var enabled = e.source.getValue(e.source.paramId) == true;
				singletonGroupOpt.setEnabled(enabled);
				posGroupOpt.setEnabled(enabled);
				
				sep.setCollapsed(enabled == false);
			}
		};
		searchByWordOpt.addPropertyChangeListener(searchByWordOpt.paramId, searchByWordListener);
		var enabled = searchByWordOpt.getValue(searchByWordOpt.paramId) == true;
		singletonGroupOpt.setEnabled(enabled);
		posGroupOpt.setEnabled(enabled);
		if(this.searchByWordEnabled == true)
			params.add(searchByWordOpt);

        this.searchByWordOpt = searchByWordOpt;
		this.searchByWordParam = searchByWordOpt;

		params.add(singletonGroupOpt);
		params.add(posGroupOpt);
	};

	/**
	 * Returns a list of words for the given
	 * group which match the criteria in the form.
	 *
	 * @param tier
	 *
	 * @return array of objects conforming to the following
	 *  protocol
	 *
	 * {
	 *   wordIndex: int,
	 *   position: (initial|medial|final|singleton),
	 *   word: obj
	 * }
	 */
	this.getRequestedWords = function (words) {
		var retVal = new java.util.ArrayList();

		var wordCount = words.size();
		for (var wIndex = 0; wIndex < wordCount; wIndex++) {
			var word = words.get(wIndex);
			var position = "unknown";

			var posOk = false;
			if (wIndex == 0 && this.wInitial == true) {
				posOk = true;
				position = "initial";
			}
			if (wIndex > 0 && wIndex < wordCount -1 && this.wMedial == true) {
				posOk = true;
				position = "medial";
			}
			if (wIndex == wordCount -1 && this.wFinal == true) {
				posOk = true;
				position = "final";
			}

			if (wIndex == 0 && wordCount == 1) {
				posOk = this.wSingleton;
				position = "singleton";
			}

			if (posOk == true) {
				retVal.add({
					wordIndex: wIndex,
					position: position,
					word: word
				});
			}
		}

		return retVal.toArray();
	};

	this.isUseFilter = function () {
    		return this.searchByWord == true;
	};
};