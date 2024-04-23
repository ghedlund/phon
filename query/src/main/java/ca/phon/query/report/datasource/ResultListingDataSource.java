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
package ca.phon.query.report.datasource;

import ca.phon.project.Project;
import ca.phon.query.db.*;
import ca.phon.query.report.io.*;
import ca.phon.script.*;
import ca.phon.script.params.*;
import ca.phon.script.scripttable.AbstractScriptTableModel;
import ca.phon.session.Record;
import ca.phon.session.*;
import org.mozilla.javascript.Scriptable;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A data source which lists each
 * result in a search in a table.  The value
 * of each column is given by a script.
 *
 * @deprecated
 */
@Deprecated
public class ResultListingDataSource extends AbstractScriptTableModel implements TableDataSource {

	private static final long serialVersionUID = 6508115371509706432L;

	/**
	 * Section information
	 */
	private ResultListing invData;

	/**
	 * Project
	 */
	private Project project;

	/**
	 * Search
	 */
	private ResultSet resultSet;

	/**
	 * Include excluded results?
	 */
	private boolean includeExcluded;

//	/**
//	 * Default pkg and class imports
//	 */
//	private final String scriptPkgImports[] = {
//			"Packages.ca.phon.engines.search.script",
//			"Packages.ca.phon.engines.search.db",
//			"Packages.ca.phon.engines.search.script.params",
//			"Packages.ca.phon.featureset"
//	};
//
//	private final String scriptClazzImports[] = {
//			"Packages.ca.phon.featureset.FeatureSet",
//			"Packages.ca.phon.util.Range",
//			"Packages.ca.phon.util.StringUtils"
//	};

	/**
	 * Session
	 */
	private Session session;


	public ResultListingDataSource(Project project, ResultSet s, ResultListing section) {
		this.project = project;
		this.resultSet = s;
		this.invData = section;
		this.includeExcluded = section.isIncludeExcluded();

		try {
			session = project.openSession(s.getCorpus(), s.getSession());
		} catch (IOException e) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, e.getLocalizedMessage(), e);
		}
		setupColumns();
	}

	public ResultListingDataSource(Session session, ResultSet rs, ResultListing section) {
		this.resultSet = rs;
		this.session = session;
		this.invData = section;
		this.includeExcluded = section.isIncludeExcluded();
		setupColumns();
	}

	public ResultListingFormatType getFormat() {
		return invData.getFormat();
	}

	public void setResultSet(ResultSet rs) {
		this.resultSet = rs;
	}

	public void setListing(ResultListing listing) {
		this.invData = listing;
		setupColumns();
	}

	private void setupColumns() {
		int colIdx = 0;
		for(ResultListingField field:invData.getField()) {
			ScriptContainer sc = field.getFieldValue();
			final PhonScript ps = new BasicScript(sc.getScript());
			try {
				setColumnScript(colIdx, ps);

				// setup static column mappings
				final Map<String, Object> bindings = new HashMap<String, Object>();

				final PhonScriptContext ctx = ps.getContext();
				final Scriptable scope = ctx.getEvaluatedScope();
				final ScriptParameters params = ctx.getScriptParameters(scope);
//				final ScriptParam[] params = ps.getScriptParams();

				// setup script parameters
				for(ScriptParam param:params) {
					for(String paramId:param.getParamIds()) {

						Object paramVal = null;
						ScriptParameter savedParam = null;
						for(ScriptParameter sp:sc.getParam()) {
							if(sp.getName().equals(paramId)) {
								savedParam = sp;
								break;
							}
						}

						if(savedParam != null) {
							try {
								if(param.getParamType().equals("bool")) {
									paramVal = Boolean.parseBoolean(savedParam.getContent());
								} else if (param.getParamType().equals("enum")) {
									EnumScriptParam esp = (EnumScriptParam)param;
									EnumScriptParam.ReturnValue rVal = null;
									for(EnumScriptParam.ReturnValue v:esp.getChoices()) {
										if(v.toString().equals(savedParam.getContent())) {
											rVal = v;
											break;
										}
									}
									if(rVal != null)
										paramVal = rVal;
									else
										paramVal = esp.getDefaultValue(paramId);
								} else {
									paramVal = savedParam.getContent();
								}
							} catch (Exception e) {
								Logger.getLogger(getClass().getName()).log(Level.WARNING, e.getLocalizedMessage(), e);
							}
						} else {
							paramVal = param.getDefaultValue(paramId);
						}

						bindings.put(paramId, paramVal);
					}
				}
				if(bindings.size() > 0)
					setColumnMappings(colIdx, bindings);
				colIdx++;
			} catch (PhonScriptException e) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, e.getLocalizedMessage(), e);
			}
		}
	}

	@Override
	public int getColumnCount() {
		int retVal = 0;

		if(invData.getFormat() == ResultListingFormatType.LIST)
			retVal = 2;
		else
			retVal = invData.getField().size();

		return retVal;
	}

	@Override
	public int getRowCount() {
		int retVal = 0;

		final int numResults = resultSet.numberOfResults(includeExcluded);
		if(invData.getFormat() == ResultListingFormatType.LIST)
			retVal = invData.getField().size() * numResults +
				(numResults - 1);
		else
			retVal = numResults;

		return retVal;
	}

	@Override
	public Object getValueAt(int row, int col) {
		Object retVal = null;
		if(invData.getFormat() == ResultListingFormatType.LIST) {
			final int rowsPerResult = invData.getField().size() + 1;
			final int result = row / rowsPerResult;
			final int fieldIdx = row % rowsPerResult;

			// space between results
			if(fieldIdx == rowsPerResult-1) return "";

			if(col == 0) {
				retVal = invData.getField().get(fieldIdx).getTitle();
			} else {
				retVal = super.getValueAt(result, fieldIdx);
			}
		} else {
			retVal = super.getValueAt(row, col);
		}
		return retVal;
	}

	@Override
	public String getColumnName(int col) {
		String retVal = super.getColumnName(col);

		if(invData.getFormat() == ResultListingFormatType.LIST) {
			if(col == 0) {
				retVal = "Field Name";
			} else if(col == 1) {
				retVal = "Value";
			}
		} else {
			retVal = invData.getField().get(col).getTitle();
		}

		return retVal;
	}

	public boolean isIncludeExcluded() {
		return includeExcluded;
	}

	public void setIncludeExcluded(boolean includeExcluded) {
		this.includeExcluded = includeExcluded;
	}

//	@Override
//	public void setColumnScript(int col, PhonScript script)
//		throws PhonScriptException {
//		// append default imports to script
//		final StringBuffer buffer = new StringBuffer();
////		for(String imp:scriptPkgImports) {
////			buffer.append(String.format("importPackage(%s)\n", imp));
////		}
////
////		for(String imp:scriptClazzImports) {
////			buffer.append(String.format("importClass(%s)\n", imp));
////		}
//
//		buffer.append(script);
//
//		super.setColumnScript(col, buffer.toString(), mimetype);
//	}

	@Override
	public Map<String, Object> getMappingsAt(int row, int col) {
		final Map<String, Object> bindings = super.getMappingsAt(row, col);

		Result result = null;
		if(includeExcluded) {
			result = resultSet.getResult(row);
		} else {
			int rIdx = -1;
			for(Result r:resultSet) {
				if(!r.isExcluded()) rIdx++;
				if(rIdx == row) {
					result = r;
					break;
				}
			}
		}

		if(result == null) return bindings;

		Record record = session.getRecord(result.getRecordIndex());

		bindings.put("project", project);
		bindings.put("session", session);
		bindings.put("resultSet", resultSet);
		bindings.put("result", result);
		bindings.put("record", record);
		bindings.put("recordIndex", result.getRecordIndex());
		bindings.put("table", this);

		return bindings;
	}

	@Override
	public String getColumnTitle(int col) {
		return getColumnName(col);
	}

	@Override
	public int getColumnIndex(String columnName) {
		int colIdx = -1;
		for(int c = 0; c < getColumnCount(); c++) {
			if(getColumnTitle(c).equals(columnName)) {
				colIdx = c;
				break;
			}
		}
		return colIdx;
	}

}
