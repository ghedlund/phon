
/*
 * Calculates PCC/PVC
 */
importPackage(Packages.ca.phon.ipa.features)

exports.Pcc = {
	
	/**
     * Perform PCC (aligned) calculation for an aligned pair of
     * IPA values
     *
     * @param record
     * @param targetGroup
     * @param actualGroup
     * @param features - comma separated list of features
     * @param ignoreDiacritics
     *
     * @return percent correct as a string with format
     *  'numCorrect/numAttempted;numDeleted;numEpenthesized'
     */
    calc_pc_aligned: function(group, features, ignoreDiacritics) {
    	var numTarget = 0;
    	var numDeleted = 0;
    	var numActual = 0;
    	var numEpenthesized = 0;
    	var numCorrect = 0;
    	
    	var targetGroup = group.getIPATarget();
        var actualGroup = group.getIPAActual();
        var alignment = group.getPhoneAlignment();
    
        var featureSet = FeatureSet.fromArray(features.split(","));
    
    	// check target side for numTarget, numDeleted and numCorrect
    	for(pIdx = 0; pIdx < targetGroup.length(); pIdx++) {
    		var phone = targetGroup.elementAt(pIdx);
    
    		if(phone.featureSet.intersects(featureSet)) {
    			numTarget++;
    
    			// check aligned phone
    			var alignedData = alignment["getAligned(java.lang.Iterable)"]([phone]);
    			if(alignedData != null) {
    				var actualPhone = alignedData[0];
    				if(actualPhone != null) {
    					var targetPhoneString = 
    						(ignoreDiacritics ? (new IPATranscript([phone])).removePunctuation().toString() : phone.toString());
    					var actualPhoneString =
    						(ignoreDiacritics ? (new IPATranscript([actualPhone])).removePunctuation().toString() : actualPhone.toString());
    
    					if( targetPhoneString == actualPhoneString ) {
    						numCorrect++;
    					}
    				} else {
    					numDeleted++;
    				}
    			} else {
    				numDeleted++;
    			}
    		}
    	}
    
    	// check actual side for numActual, numEpenthesized
    	// check target side for numTarget, numDeleted and numCorrect
    	for(pIdx = 0; pIdx < actualGroup.length(); pIdx++) {
    		var phone = actualGroup.elementAt(pIdx);
    
    		if(phone.featureSet.intersects(featureSet)) {
    			numActual++;
    
    			// check aligned phone
    			var alignedData = alignment["getAligned(java.lang.Iterable)"]([phone]);
    			if(alignedData != null) {
    				var targetPhone = alignedData[0];
    				if(targetPhone == null) {
    					numEpenthesized++;
    				}
    			} else {
    				numEpenthesized++;
    			}
    		}
    	}
    
    	// format PCC string
    	// (numCorrect)/(numTarget-numDeleted);numDeleted;numEpenthesized
    	var retVal = 
    		numCorrect + "/" + (numTarget-numDeleted) + ";" + numDeleted + ";" + numEpenthesized;
    	return retVal;
    },
    
    /**
     * Calculates PCC (standard) for a pair of ipa transcriptions.
     * In this version, direct phone alignment is not considered.
     *
     * @param targetIpa
     * @param acutalIpa
     * @param features
     * @param ignoreDiacritics
     *
     * @return PCC (standard) in the format x/y
     */
    calc_pc_standard: function(group, features, ignoreDiacritics) {
        var numTarget = 0;
        var numActual = 0;
        var targetVals = new Array();
    	var numCorrect = 0;
    	var numEpenthesized = 0;
    	var numProduced = 0;
    	
    	var targetGroup = group.getIPATarget();
        var actualGroup = group.getIPAActual();
        var alignment = group.getPhoneAlignment();
    
        var featureSet = FeatureSet.fromArray(features.split(","));
    
    	// check target side for numTarget, numDeleted and numCorrect
    	for(pIdx = 0; pIdx < targetGroup.length(); pIdx++) {
    		var phone = targetGroup.elementAt(pIdx);
    
    		if(phone.featureSet.intersects(featureSet)) {
    			numTarget++;
                var targetPhoneString = 
                	(ignoreDiacritics ? (new IPATranscript([phone])).removePunctuation().toString() : phone.toString());
    		    targetVals[targetPhoneString] = 
    		        ( targetVals[targetPhoneString] ? targetVals[targetPhoneString] + 1 : 1 );
    		}
    	}
    
    	// check actual side for numActual, numEpenthesized
    	// check target side for numTarget, numDeleted and numCorrect
    	for(pIdx = 0; pIdx < actualGroup.length(); pIdx++) {
    		var phone = actualGroup.elementAt(pIdx);
   
    		if(phone.featureSet.intersects(featureSet)) {
    		    numActual++;
                var actualPhoneString = 
                	(ignoreDiacritics ? (new IPATranscript([phone])).removePunctuation().toString() : phone.toString());
    			    
    			var amountInTarget = targetVals[actualPhoneString];
    			if(amountInTarget != null && amountInTarget > 0) {
    			    numCorrect++;
    			    targetVals[actualPhoneString] = --amountInTarget;
    			}
    		}
    	}
    	if(numActual > numTarget)
    	    numEpenthesized = numActual - numTarget;
    
    	// format PCC string
    	// (numCorrect)/(numTarget-numDeleted);numDeleted;numEpenthesized
    	var retVal = 
    		numCorrect + "/" + (numTarget+numEpenthesized);
    	return retVal;
    }

};

exports.PccOptions = function(id, aligned) {
    
    var includePccParamInfo = {
        "id": id+(".includePcc"),
        "title": "",
        "desc": "Include percent consonants correct (" + (aligned ? "A" : "") + "PCC)",
        "def": false
    };
    var includePccParam;
    this.includePcc;
    
    var includePvcParamInfo = {
        "id": id+(".includePvc"),
        "title": "",
        "desc": "Include percent vowels correct (" + (aligned ? "A" : "") + "PVC)",
        "def": false
    };
    var includePvcParam;
    this.includePvc;
    
    var ignoreDiacriticsParamInfo = {
        "id": id+(".ignoreDiacritics"),
        "title": "",
        "desc": "Ignore diacritics",
        "def": true
    };
    var ignoreDiacriticsParam;
    this.ignoreDicacritic;
    
    this.param_setup = function(params) {
        includePccParam = new BooleanScriptParam(
            includePccParamInfo.id,
            includePccParamInfo.desc,
            includePccParamInfo.title,
            includePccParamInfo.def);
            
        includePvcParam = new BooleanScriptParam(
            includePvcParamInfo.id,
            includePvcParamInfo.desc,
            includePvcParamInfo.title,
            includePvcParamInfo.def);
        
        ignoreDiacriticsParam = new BooleanScriptParam(
            ignoreDiacriticsParamInfo.id,
            ignoreDiacriticsParamInfo.desc,
            ignoreDiacriticsParamInfo.title,
            ignoreDiacriticsParamInfo.def);
            
        params.add(includePccParam);
        params.add(includePvcParam);
        params.add(ignoreDiacriticsParam);
    };
    
    this.setup_pcc_aligned_metadata = function(group, metadata) {
        if(this.includePcc == true) {
            var pccAligned = Pcc.calc_pc_aligned(group, "Consonant", this.ignoreDiacritics);
            metadata.put("APCC", pccAligned);
        }
        if(this.includePvc == true) {
            var pvcAligned = Pcc.calc_pc_aligned(group, "Vowel", this.ignoreDiacritics);
            metadata.put("APVC", pvcAligned);
        }
    };
    
    this.setup_pcc_standard_metadata = function(group, metadata) {
        if(this.includePcc == true) {
            var pccStandard = Pcc.calc_pc_standard(group, "Consonant", this.ignoreDiacritics);
            metadata.put("PCC", pccStandard);
        }
        if(this.includePvc == true) {
            var pvcStandard = Pcc.calc_pc_standard(group, "Vowel", this.ignoreDiacritics);
            metadata.put("PVC", pvcStandard);
        }
    };
    
};
