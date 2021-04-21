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
/*
 * List of tiers entered by the user.
 */
 
importPackage(Packages.ca.phon.query.script.params)

exports.TierList = function(id) {

	var tiersParamInfo = {
		"id": id + ".tiers",
		"title": "Tier names:",
		"prompt": "Enter tier names, separated by ','",
		"def": ""
	};
	this.tiersParam;
	this.tiers = tiersParamInfo.def;

	this.setEnabled = function(enabled) {
		this.tiersParam.setEnabled(enabled == true);
	};

	this.param_setup = function(params) {
		this.tiersParam = new TierListScriptParam(
    		tiersParamInfo.id,
    		tiersParamInfo.title,
    		tiersParamInfo.def);
    	this.tiersParam.setPrompt(tiersParamInfo.prompt);

		params.add(this.tiersParam);
	};

	this.getTiers = function() {
		var retVal = new Array();

		var splits = this.tiers.split(',');
		for(var i = 0; i < splits.length; i++) {
			var tierName = splits[i].trim();
			if(tierName.length() > 0)
				retVal.push(tierName);
		}

		return retVal;
	};

	this.setTiers = function(tiers) {
		this.tiers = tiers;
	};

	this.setTitle = function(title) {
		tiersParamInfo.title = title;
	};
	
	var coverRegex = /Cover (IPA (Target|Actual)) \(([^;]+);\s?(.+)\)/;

	/**
	 * Returns a tuple of tier data.
	 *
	 * @return [resultValues, metadata]
	 */
	this.getAlignedTierData = function(record, obj, label) {
		var resultValues = new Array();
		var metadata = new java.util.LinkedHashMap();
		
		if(typeof obj.getTier !== "function") return [ [], new java.util.HashMap() ];

		var extraTiers = this.getTiers()
		for(var j = 0; j < extraTiers.length; j++) {
			var tierName = extraTiers[j];
			var tierVal = null;
			if(record.hasTier(tierName)) {
				var tierVal = obj.getTier(tierName);

				var tierResultValue = factory.createResultValue();
				tierResultValue.name = tierName + " ("  + label + ")";
				tierResultValue.tierName = tierName;
				tierResultValue.groupIndex = (obj.getGroupIndex ? obj.groupIndex : obj.group.groupIndex);
				tierResultValue.data = tierVal || "";

				var startIndex = 0;
				var length = (tierVal ? tierVal.toString().length() : 0);

				if(tierVal != null && obj.getGroup) {
					var systemTierType = SystemTierType.tierFromString(tierName);
					var wordOffset = 0;

					if(systemTierType == SystemTierType.Orthography) {
						wordOffset = obj.getOrthographyWordLocation();
					} else if(systemTierType == SystemTierType.IPATarget) {
						wordOffset = obj.getIPATargetWordLocation();
					} else if(systemTierType == SystemTierType.IPAActual) {
						wordOffset = obj.getIPAActualWordLocation();
					} else {
						wordOffset = obj.getTierWordLocation(tierName);
					}

					startIndex += wordOffset;
				}

				tierResultValue.range = new Range(startIndex, startIndex + length, true);
				resultValues.push(tierResultValue);
			} else {
				if(tierName == "Phone Alignment") {
					var align = obj.phoneAlignment;
					tierVal = (align != null ? align.toString(false) : "");
				} else if(tierName == "Target CV") {
					var ipaT = obj.IPATarget;
					tierVal = (ipaT != null ? ipaT.cvPattern : "");
				} else if(tierName == "Actual CV") {
					var ipaA = obj.IPAActual;
					tierVal = (ipaA != null ? ipaA.cvPattern : "");
				} else if(tierName == "Target Stress") {
					var ipaT = obj.IPATarget;
					tierVal = (ipaT != null ? ipaT.stressPattern : "");
				} else if(tierName == "Actual Stress") {
					var ipaA = obj.IPAActual;
					tierVal = (ipaA != null ? ipaA.stressPattern : "");
				} else if(tierName == "Target Syllabification") {
					var ipaT = obj.IPATarget;
					tierVal = (ipaT != null ? ipaT.toString(true) : "");
				} else if(tierName == "Actual Syllabification") {
					var ipaA = obj.IPAActual;
					tierVal = (ipaA != null ? ipaA.toString(true) : "");
				} else if(tierName.match(coverRegex)) {
				    var groupData = coverRegex.exec(tierName);
				    
				    var phonTier = groupData[1].trim();
				    var reportTier = groupData[3].trim();
				    var symbolMap = groupData[4].trim();
	
	                var ipa = obj.getTier(phonTier);
	                tierVal = (ipa != null ? ipa.cover(symbolMap) : "");
	                
	                tierName = reportTier;
				}
				if(tierVal != null)
					metadata.put(tierName + " (" + label + ")", tierVal.toString());
			}
		}

		return [resultValues, metadata];
	};

};
