/**
 * pmlu.js
 * 
 * Report results generated by the ../script/PMLU.js query script.
 * 
 * This script groups results for target productions.
 */
 
importClass(Packages.java.util.ArrayList)
importPackage(Packages.ca.phon.app.log)
importPackage(Packages.ca.phon.ipa.tree)
importPackage(Packages.ca.phon.ipa.features)
importPackage(Packages.ca.phon.query)
importPackage(Packages.ca.phon.query.db)
importPackage(Packages.ca.phon.query.analysis)

var comparator = new CompoundFeatureComparator(FeatureComparator.createPlaceComparator());
var ipaTree = new IpaTernaryTree(comparator);

var nf = java.text.NumberFormat.getNumberInstance();
nf.setMaximumFractionDigits(6);

var defaultColumns = ["Target Word", "Actual Word"];
var PMLUcolumns = ["target PMLU", "actual PMLU", "PWP"];
var EPMLUcolumns = ["target ePMLU-F","target ePMLU-S","target ePMLU","actual ePMLU-F","actual ePMLU-S","actual ePMLU","ePWP"];

var keySet = queryAnalysisResult.resultSetKeys;
var project = queryAnalysisResult.input.project;
var sessionPathItr = keySet.iterator();

// check to see what information we need to print out

var queryScript = queryAnalysisResult.input.queryScript;
var ctx = queryScript.context;
var scriptParams = ctx.getScriptParameters(ctx.getEvaluatedScope());

includePMLU = scriptParams.getParamValue("includePMLU");
includeEPMLU = scriptParams.getParamValue("includeEPMLU");
closedSyllBonus = scriptParams.getParamValue("closedSyllBonus");

while(sessionPathItr.hasNext()) {
	var sessionPath = sessionPathItr.next();
	var session = project.openSession(sessionPath.corpus, sessionPath.session);
	
	var resultSet = queryAnalysisResult.getResultSet(sessionPath);
	for(i = 0; i < resultSet.numberOfResults(true); i++) {
		var result = resultSet.getResult(i);
		var record = session.getRecord(result.recordIndex);
		
		var wordStr = result.metadata.get("Word");
		var wordIdx = java.lang.Integer.parseInt(wordStr);
		
		var word = record.getGroup(result.getResultValue(0).groupIndex).getAlignedWord(wordIdx);
	
		var ipaTarget = word.getIPATarget().removePunctuation();
		
		var wordList = ipaTree.get(ipaTarget);
		if(wordList == null) {
			wordList = new ArrayList();
			ipaTree.put(ipaTarget, wordList);
		}
		wordList.add(word);
	}
}

// table header
var line = new Array();
for(i = 0; i < defaultColumns.length; i++) line.push(defaultColumns[i]);
if(includePMLU) for(i = 0; i < PMLUcolumns.length; i++) line.push(PMLUcolumns[i]);
if(includeEPMLU) for(i = 0; i < EPMLUcolumns.length; i++) line.push(EPMLUcolumns[i]);
csvWriter.writeNext(line);

// print table with target values sorted by feature
var keyItr = ipaTree.keySet().iterator();
while(keyItr.hasNext()) {
	line = new Array();
	var target = keyItr.next();
	line.push(target.toString());
	
	var targetPMLU = 0;
	var sumPMLU = 0;
	
	var targetEPMLUF = 0;
	var targetEPMLUS = 0;
	var sumEPMLUF = 0;
	var sumEPMLUS = 0;
	
	var wordList = ipaTree.get(target);
	for(i = 0; i < wordList.size(); i++) {
		var word = wordList.get(i);
		var alignment = word.getPhoneAlignment();
		var pmlu = alignment.PMLU;
		var epmlu = alignment.EPMLU;
		if(i > 0) {
			csvWriter.writeNext(line);
			line = new Array();
			line.push("");
		}
		
		var actual = alignment.actualRep.removePunctuation();
		line.push(actual)
		
		// print PMLU
		if(includePMLU) {
			if(i > 0) {
				line.push("");
			} else {
				line.push(nf.format(pmlu.targetPMLU()));
			}
			
			targetPMLU = pmlu.targetPMLU();
			sumPMLU += pmlu.actualPMLU();
			
			line.push(nf.format(pmlu.actualPMLU()));
			line.push(nf.format(pmlu.PWP()));
		}
	
		// print ePMLU
		if(includeEPMLU) {
			if(i > 0) {
				line.push("");
				line.push("");
				line.push("");
			} else {
				line.push(nf.format(epmlu.targetEPMLUFeatures()));
				line.push(nf.format(epmlu.targetEPMLUSyllables(closedSyllBonus)));
				line.push(nf.format(epmlu.targetEPMLU(closedSyllBonus)));
			}
			
			targetEPMLUF = epmlu.targetEPMLUFeatures();
			targetEPMLUS = epmlu.targetEPMLUSyllables(closedSyllBonus);
			sumEPMLUF += epmlu.actualEPMLUFeatures();
			sumEPMLUS += epmlu.actualEPMLUSyllables(closedSyllBonus);
			
			line.push(nf.format(epmlu.actualEPMLUFeatures()));
			line.push(nf.format(epmlu.actualEPMLUSyllables(closedSyllBonus)));
			line.push(nf.format(epmlu.actualEPMLU(closedSyllBonus)));
			line.push(nf.format(epmlu.ePWP(closedSyllBonus)));
		}
	}
	csvWriter.writeNext(line);
	
	line = new Array();
//	if(wordList.size() > 1) {
//		if(includePMLU) {
//			// print average
//			var avgPMLU = sumPMLU/wordList.size();
//			var avgPWP = avgPMLU/targetPMLU;
//			
//			line.push("");
//			line.push("Average");
//			line.push("");
//			line.push(avgPMLU);
//			line.push(avgPWP);
//		}
//		
//		if(includeEPMLU) {
//			line.push("");
//			line.push("");
//			line.push("");
//		
//			var avgEPMLUF = sumEPMLUF/wordList.size();
//			var avgEPMLUS = sumEPMLUS/wordList.size();
//			var avgEPMLU = avgEPMLUF + avgEPMLUS;
//			var avgEPWP = avgEPMLU/(targetEPMLUF+targetEPMLUS);
//			
//			line.push(nf.format(avgEPMLUF));
//			line.push(nf.format(avgEPMLUS));
//			line.push(nf.format(avgEPMLU));
//			line.push(nf.format(avgEPWP));
//		}		
//		
//		csvWriter.writeNext(line);
//		line = new Array();
//	}
	
	csvWriter.flush();
}
