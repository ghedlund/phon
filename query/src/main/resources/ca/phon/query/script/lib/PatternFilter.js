/**
 * Options for filtering/searching objects based on various patterns.
 *
 * Pattern languages supported:
 *  - plain text
 *  - regex
 *  - phonex
 *  - cv pattern
 *  - stress pattern
 *
 * phonex, cv, and stress pattern matchers are only
 * applicable to ipa tier values
 */

var HelpText = require("lib/PhonScriptConstants").HelpText;

exports.PatternType = {
    PLAIN: 0,
    REGEX: 1,
    PHONEX: 2,
    STRESS: 3,
    CV: 4
};

exports.PatternFilter = function (id) {
    
    var filterParamInfo = {
        "id": id + ".filter",
        "title": "Expression:",
        "prompt": "Enter expression",
        "def": ""
    };
    var filterParam;
    this.filter = filterParamInfo.def;
    
    var filterTypeParamInfo = {
        "id": id + ".filterType",
        "title": "Expression type:",
        "desc":[ "Plain text", "Regular expression", "Phonex", "Stress pattern", "CGV pattern"],
        "def": 2
    };
    var filterTypeParam;
    this.filterType = {
        "index": 2, "toString": "Phonex"
    };
    
    var matchGroupParamInfo = {
        "id":[id + ".caseSensitive", id + ".exactMatch"],
        "title": "",
        "desc":[ "Case sensitive", "Exact match"],
        "def":[ false, false],
        "numCols": 2
    };
    var matchGroupParam;
    this.caseSensitive = matchGroupParamInfo.def[0];
    this.exactMatch = matchGroupParamInfo.def[1];
    
    var helpLabelParamInfo = {
        "title": "",
        "desc": ""
    };
    var helpLabelParam;
    
    var filterTypePromptText =[
    "Enter plain text expression",
    "Enter regular expression",
    "Enter phonex",
    "Enter stress pattern",
    "Enter CGV pattern"];
    
    var filterTypeHelpText =[
    HelpText.plainTextHelpText,
    HelpText.regexHelpText,
    HelpText.phonexHelpText,
    HelpText.stressPatternHelpText,
    HelpText.cvPatternHelpText];
    
    this.setEnabled = function (e) {
        var enabled = (e == true);
        filterParam.setEnabled(enabled);
        filterTypeParam.setEnabled(enabled);
        matchGroupParam.setEnabled(enabled);
    };
    
    this.setVisible = function (v) {
        var visible = (v == true);
        filterParam.setVisible(visible);
        filterTypeParam.setVisible(visible);
        matchGroupParam.setVisible(visible);
    };
    
    var setPatternFilterInvalid = function (textField, message, loc) {
        var msg = (loc >= 0 ?
        "Error at index " + loc + ": " + message:
        message);
        
        //        textField.setToolTipText(msg);
        //        textField.setState("UNDEFINED");
    };
    
    var setPatternFilterOk = function (textField) {
        //        textField.setToolTipText("");
        //        textField.setState("INPUT");
    };
    
    /**
     * Set selected pattern type
     *
     * type should be one of
     * PATTERN_TYPE_PLAIN, PATTERN_TYPE_REGEX, PATTERN_TYPE_PHONEX,
     * PATTERN_TYPE_STRESS, PATTERN_TYPE_CV
     *
     * @param type
     */
    this.setSelectedPatternType = function (type) {
        filterTypeParamInfo.def = type;
    };
    
    /* Check filter */
    var stressPatternRegex = /^([ ABUabu12][?+*]?)+$/;
    var cvPatternRegex = /^([ ABCVGcvg][?+*]?)+$/;
    
    var checkRegexFilter = function (filter) {
        var retVal = {
            valid: false,
            message: "",
            loc: 0
        };
        try {
            var testPattern = java.util.regex.Pattern.compile(filter);
            retVal.valid = true;
        }
        catch (e) {
            retVal.valid = false;
            retVal.message = e.message;
            retVal.loc = e.javaException.index;
        }
        return retVal;
    };
    
    var checkPhonexFilter = function (filter) {
        var retVal = {
            valid: false,
            message: "",
            loc: 0
        };
        try {
            Packages.ca.phon.phonex.PhonexPattern.compile(filter);
            retVal.valid = true;
        }
        catch (e) {
            retVal.valid = false;
            retVal.messge = e.message;
            retVal.loc = e.javaException.index;
        }
        return retVal;
    };
    
    var checkStressPatternFilter = function (filter) {
        var retVal = {
            valid: false,
            message: "",
            loc: 0
        };
        retVal.valid = stressPatternRegex.test(filter);
        if (! retVal.valid) {
            retVal.messgae = "";
            retVal.loc = -1;
        }
        return retVal;
    };
    
    var checkCVPatternFilter = function (filter) {
        var retVal = {
            valid: false,
            message: "",
            loc: 0
        };
        retVal.valid = cvPatternRegex.test(filter);
        if (! retVal.valid) {
            retVal.message = "";
            retVal.loc = -1;
        }
        return retVal;
    };
    
    // check the filter for errors
    // return value will have three properties
    //   valid:boolean, message:string, loc:int
    var checkFilter = function (filter, filterType) {
        var retVal = {
            valid: false,
            message: "",
            loc: 0
        };
        
        
        switch (filterType) {
            case 0:
            retVal.valid = true;
            break;
            // plain text is always ok
            
            case 1:
            retVal = checkRegexFilter(filter);
            break;
            
            case 2:
            retVal = checkPhonexFilter(filter);
            break;
            
            case 3:
            retVal = checkStressPatternFilter(filter);
            break;
            
            case 4:
            retVal = checkCVPatternFilter(filter);
            break;
            
            default:
            retVal.valid = false;
            retVal.loc = -1;
            break;
        }
        
        return retVal;
    };
    
    var validateTextField = function (textField) {
        var txt = textField.getText();
        if (textField.getState() == Packages.ca.phon.gui.components.PromptedTextField.FieldState.PROMPT)
        return;
        
        var filterType = filterTypeParam.getValue(filterTypeParamInfo.id);
        var filterCheck = checkFilter(txt, filterType.index);
        if (! filterCheck.valid) {
            setPatternFilterInvalid(textField, filterCheck.message, filterCheck.loc);
        } else {
            setPatternFilterOk(textField);
        }
    };
    
    this.param_setup = function (params) {
        // don't add a separator as this filter may be used inside a parent filter
        matchGroupParam = new MultiboolScriptParam(
        matchGroupParamInfo.id,
        matchGroupParamInfo.def,
        matchGroupParamInfo.desc,
        matchGroupParamInfo.title,
        matchGroupParamInfo.numCols);
        matchGroupParam.setEnabled(0, false);
        
        filterParam = new StringScriptParam(
        filterParamInfo.id,
        filterParamInfo.title,
        filterParamInfo.def);
        //        var textField = filterParam.getEditorComponent();
        //        textField.setPrompt(filterParamInfo.prompt);
        filterParam.setPrompt(filterParamInfo.prompt);
        
        //        var filterListener = new java.awt.event.KeyListener() {
        //            keyPressed: function(e) {
        //            },
        //
        //            keyReleased: function(e) {
        //            },
        //
        //            keyTyped: function(e) {
        //                var textField = e.getSource();
        //                validateTextField(textField);
        //            }
        //       };
        //       filterParam.getEditorComponent().addKeyListener(filterListener);
        
        filterTypeParam = new EnumScriptParam(
        filterTypeParamInfo.id,
        filterTypeParamInfo.title,
        filterTypeParamInfo.def,
        filterTypeParamInfo.desc);
        var filterTypeListener = new java.beans.PropertyChangeListener() {
            propertyChange: function(e) {
                // setup help label
                var idx = e.source.getValue(e.source.paramId).index;
                
                // PHONEX
                if(idx >= exports.PatternType.PHONEX) {
                    matchGroupParam.setEnabled(0, false); 
                } else {
                    matchGroupParam.setEnabled(0, true);
                }
                
                var filterPrompt = filterTypePromptText[idx];
                var filterHelp = filterTypeHelpText[idx];
                
                filterParam.setPrompt(filterPrompt);
                helpLabelParam.setText(filterHelp);
            }  
        };
        filterTypeParam.addPropertyChangeListener(filterTypeParamInfo.id, filterTypeListener);
        
        var helpLabelDesc = filterTypeHelpText[filterTypeParamInfo.def];
        helpLabelParam = new LabelScriptParam(
        helpLabelDesc,
        helpLabelParamInfo.title);
        
        params.add(filterTypeParam);
        params.add(filterParam);
        
        params.add(matchGroupParam);
        
        var sepLine = new LabelScriptParam("<html>&nbsp;</html>", "");
        //   params.add(sepLine);
        
        params.add(helpLabelParam);
    };
    
    this.isUseFilter = function () {
        var txt = this.filter;
        
        if (txt.length() > 0) {
            var filterCheck = checkFilter(this.filter, this.filterType.index);
            return filterCheck.valid;
        } else {
            return false;
        }
    };
    
    /* Check for occurances (or exact match) of entered filter */
    var checkPlain = function (obj, filter, caseSensitive, exactMatch) {
        var strA = (caseSensitive == true ? obj.toString(): obj.toString().toLowerCase());
        var strB = (caseSensitive == true ? filter: filter.toLowerCase());
        
        if (exactMatch == true) {
            return (strA == strB);
        } else {
            return (strA.indexOf(strB) >= 0);
        }
    };
    
    var checkRegex = function (obj, filter, caseSensitive, exactMatch) {
        var regexPattern = java.util.regex.Pattern.compile(filter, (caseSensitive ? 0: java.util.regex.Pattern.CASE_INSENSITIVE));
        var regexMatcher = regexPattern.matcher(obj.toString());
        if (exactMatch) {
            return matcher.matches();
        } else {
            return matcher.find();
        }
    };
    
    var checkPhonex = function (obj, filter, exactMatch) {
        if (!(obj instanceof IPATranscript)) return false;
        
        if (exactMatch) {
            return obj.matches(filter);
        } else {
            return obj.contains(filter);
        }
    };
    
    var checkStressPattern = function (obj, filter, exactMatch) {
        // TODO
        return false;
    };
    
    var checkCVPattern = function (obj, filter, exactMatch) {
        // TODO
        return false;
    };
    
    /**
     * Check object for occurances (or exact match)
     * of filter.
     *
     * @param obj
     * @return true if filter matches, false otherwise
     */
    this.check_filter = function (obj) {
        var retVal = true;
        
        if (obj == null) return false;
        
        switch (this.filterType.index) {
            case 0:
            retVal = checkPlain(obj, this.filter, this.caseSensitive, this.exactMatch);
            break;
            
            case 1:
            retVal = checkRegex(obj, this.filter, this.caseSensitive, this.exactMatch);
            break;
            
            case 2:
            retVal = checkPhonex(obj, this.filter, this.exactMatch);
            break;
            
            case 3:
            retVal = checkStressPattern(obj, this.filter, this.exactMatch);
            break;
            
            case 4:
            retVal = checkCVPattern(obj, this.filter, this.exactMatch);
            break;
            
            default:
            retVal = false;
            break;
        };
        
        return retVal;
    };
    
    var findPlain = function (obj, filter, caseSensitive, exactMatch) {
        var retVal = new Array();
        
        var strA = (caseSensitive ? obj.toString(): obj.toString().toLowerCase());
        var strB = (caseSensitive ? filter: filter.toLowerCase());
        if (exactMatch) {
            if (strA == strB) {
                var v = {
                    start: 0, end: strA.length, value: obj
                };
                retVal.append(v);
            }
        } else {
            var i = 0;
            while (strA.indexOf(strB, i) >= 0) {
                var v = {
                    start: i, end: i + strB.length, value: strA.substring(i, i + strB.length)
                };
                retVal.push(v);
                i += strB.length;
            }
        }
        
        return retVal;
    };
    
    var findRegex = function (obj, filter, caseSensitive, exactMatch) {
        var regexPattern = java.util.regex.Pattern.compile(filter, (caseSensitive ? 0: java.util.regex.Pattern.CASE_INSENSITIVE));
        var regexMatcher = regexPattern.matcher(obj.toString());
        var retVal = new Array();
        
        if (exactMatch) {
            if (regexMatcher.matches()) {
                v = {
                    start: 0, end: obj.toString().length(), value: obj
                };
                retVal.push(v);
            } else {
                return new Array();
            }
        } else {
            while(regexMatcher.find()) {
                v = {
                    start: regexMatcher.start(), end: regexMatcher.end(), value: regexMatcher.group()
                };
                retVal.push(v);
            }
        }
        
        return retVal;
    };
    
    var findPhonex = function (obj, filter, exactMatch) {
        var retVal = new Array();
        
        if (!(obj instanceof IPATranscript)) return retVal;
        
        if (exactMatch == true) {
            if (obj.matches(filter)) {
                v = {
                    start: 0, end: obj.size(), value: obj
                };
                retVal.push(v);
            }
        } else {
            var phonexPattern = PhonexPattern.compile(filter);
            var phonexMatcher = phonexPattern.matcher(obj);
            
            while (phonexMatcher.find()) {
                v = {
                    start: phonexMatcher.start(), end: phonexMatcher.end(), value: new IPATranscript(phonexMatcher.group())
                };
                //   java.lang.System.out.println(phonexMatcher.group());
                retVal.push(v);
            }
        }
        
        return retVal;
    };
    
    var findCVPattern = function (obj, filter, exactMatch) {
        return new Array();
    };
    
    var findStressPattern = function (obj, filter, exactMatch) {
        return new Array();
    };
    
    /**
     * Returns all occurances of the indicated pattern
     * within the given object.
     *
     * @param obj
     * @return occurances of the pattern.  The results will be a list
     *  of items conforming to the following protocol:
     *
     *  {
     *     start:int // the start index of the match
     *     end:int   // the end index of the match
     *     value:object // the value of the match
     *  }
     */
    this.find_pattern = function (obj) {
        var retVal = new Array();
        
        if (obj == null) return retVal;
        
        switch (this.filterType.index) {
            case 0:
            retVal = findPlain(obj, this.filter, this.caseSensitive, this.exactMatch);
            break;
            
            case 1:
            retVal = findRegex(obj, this.filter, this.caseSensitive, this.exactMatch);
            break;
            
            case 2:
            retVal = findPhonex(obj, this.filter, this.exactMatch);
            break;
            
            case 3:
            retVal = findStressPattern(obj, this.filter, this.exactMatch);
            break;
            
            case 4:
            retVal = findCVPattern(obj, this.filter, this.exactMatch);
            break;
            
            default:
            break;
        };
        
        return retVal;
    };
    
    /**
     * Filters a list of group objects given a tier name and list of groups.
     *
     * @param groups
     * @return a list of filtered groups based on the setup of this filter
     */
    this.filter_groups = function (groups, tierName) {
        var retVal = new Array();
        
        for (var gIdx = 0; gIdx < groups.length; gIdx++) {
            var group = groups[gIdx];
            var groupVal = group.getTier(tierName);
            if (this.check_filter(groupVal) == true) {
                retVal.push(group);
            }
        }
        
        return retVal;
    };
}