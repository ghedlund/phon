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
package ca.phon.app.session.editor.search;

import ca.phon.ipa.IPATranscript;
import ca.phon.phonex.*;
import ca.phon.session.Record;
import ca.phon.session.*;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.util.*;
import java.util.regex.*;

public class SessionRowFilter extends RowFilter<TableModel, Integer> {
	
	private final static String ANY_TIER = "*";
	
	// tier name -> expr
	private final Map<String, String> tierExprs =
			new HashMap<String, String>();
	
	private SearchType searchType = SearchType.PLAIN;
	
	private boolean caseSensitive = false;
	
	public SessionRowFilter() {
		
	}
	
	public SessionRowFilter(String filter, SearchType searchType, boolean caseSensitive) {
		super();
		parseFilter(filter);
		this.searchType = searchType;
		this.caseSensitive = caseSensitive;
	}
	
	public void parseFilter(String filter) {
		tierExprs.clear();
		
		final String filterRegex = 
				"(?:\\s*([ a-zA-Z0-9#*,]+):)?\\s*([^;]+)\\s*";
		final String exprRegex = 
				filterRegex + "(?:;" + filterRegex + ")*";
		
		if(filter == null) return;
		if(filter.length() == 0) {
			tierExprs.put(ANY_TIER, "");
		} else if(filter.matches(exprRegex)) {
			final Pattern filterPattern = Pattern.compile(filterRegex);
			final Matcher filterMatcher = filterPattern.matcher(filter);
			while(filterMatcher.find()) {
				final String columnText = (filterMatcher.group(1) == null ? "*" : filterMatcher.group(1));
				final String columns[] = columnText.split(",");
				final String expr = filterMatcher.group(2);
				
				for(String col:columns) {
					tierExprs.put(col, expr);
				}
			}
		}
	}
	
	@Override
	public boolean include(
			RowFilter.Entry<? extends TableModel, ? extends Integer> entry) {
		final int row = entry.getIdentifier();
		final SessionTableModel model = (SessionTableModel)entry.getModel();
		
		final Record r = model.getSession().getRecord(row);
		
		boolean retVal = true;
		for(String column:tierExprs.keySet()) {
			if(column.equals(ANY_TIER)) {
				// search all columns
				boolean anyTier = false;
				
				// check record number
				anyTier = checkRecordNumber(row+1, tierExprs.get(ANY_TIER));
				
				// check speaker
				anyTier |= (r.getSpeaker() != null ? r.getSpeaker().toString().contains(tierExprs.get(column))
						: false);
				
				for(int i = 0; i < entry.getValueCount(); i++) {
					if(anyTier) break;
					anyTier = checkTier(r, model.getColumnName(i));
				}
				retVal &= anyTier;
			} else if(column.equals("Record #")) {
				retVal &= checkRecordNumber(row+1, tierExprs.get(column));
			} else if (column.equals("Speaker")) {
				retVal &= (r.getSpeaker() != null ? r.getSpeaker().toString().contains(tierExprs.get(column))
						: false);
			} else {
				retVal &= checkTier(r, column);
			}
		}
		
		return retVal;
	}
	
	private boolean checkTier(Record record, String tierName) {
		String expr = tierExprs.get(tierName);
		if(expr == null) {
			expr = tierExprs.get(ANY_TIER);
		}
		if(expr == null) return false;
		if(expr.trim().length() == 0) return false;
		
		if(record.getTierType(tierName) == IPATranscript.class && searchType == SearchType.PHONEX) {
			final Tier<IPATranscript> ipaTier = 
					record.getTier(tierName, IPATranscript.class);
			try {
				return checkIPATier(ipaTier, expr);
			} catch (PhonexPatternException e) {
				final Tier<String> tier = record.getTier(tierName, String.class);
				return checkStringTier(tier, expr);
			}
		} else {
			final Tier<String> tier = record.getTier(tierName, String.class);
			if(tier != null)
				return checkStringTier(tier, expr);
			else
				return false;
		}
	}
	
	private boolean checkRecordNumber(int recNum, String expr) {
		final String rangeRegex = "([0-9]+)(?:\\.\\.([0-9]+))?";
		final String exprRegex = rangeRegex + "(," + rangeRegex + ")*";
		
		if(!expr.matches(exprRegex)) return false;
		
		final Pattern pattern = Pattern.compile(rangeRegex);
		final Matcher matcher = pattern.matcher(expr);
		
		boolean retVal = false;
		while(matcher.find()) {
			if(retVal) break;
			final int start = Integer.parseInt(matcher.group(1));
			int end = start;
			if(matcher.group(2) != null) {
				end = Integer.parseInt(matcher.group(2));
			}
			
			retVal = 
					(recNum >= start) && (recNum <= end);
		}
		return retVal;
	}
	
	private boolean checkIPATier(Tier<IPATranscript> ipaTier, String expr)
		throws PhonexPatternException {
		final PhonexPattern pattern = PhonexPattern.compile(expr);
		boolean retVal = false;

		if(ipaTier.hasValue()) {
			final PhonexMatcher matcher = pattern.matcher(ipaTier.getValue());
			retVal = matcher.find();
		}

		return retVal;
	}
	
	private boolean checkStringTier(Tier<String> tier, String expr) {
		boolean retVal = false;

		if(tier.hasValue()) {
			if (searchType == SearchType.REGEX) {
				try {
					final Pattern pattern = Pattern.compile(expr, (caseSensitive ? 0 : Pattern.CASE_INSENSITIVE));
					final Matcher matcher = pattern.matcher(tier.getValue());
					retVal = matcher.find();
				} catch (PatternSyntaxException e) {

				}
			} else if (searchType == SearchType.PLAIN) {
				String grpVal = (caseSensitive ? tier.getValue() : tier.getValue().toLowerCase());
				String exprVal = (caseSensitive ? expr : expr.toString().toLowerCase());

				retVal = grpVal.contains(exprVal);
			}
		}

		return retVal;
	}
	
}
