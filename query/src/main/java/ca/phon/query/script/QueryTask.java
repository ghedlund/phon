/*
 * Phon - An open source tool for research in phonology.
 * Copyright (C) 2008 The Phon Project, Memorial University <http://phon.ling.mun.ca>
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
package ca.phon.query.script;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrappedException;

import ca.phon.project.Project;
import ca.phon.query.db.QueryFactory;
import ca.phon.query.db.QueryManager;
import ca.phon.query.db.ResultSet;
import ca.phon.script.js.ExtendableWrapFactory;
import ca.phon.script.params.ScriptParam;
import ca.phon.session.Record;
import ca.phon.session.Session;
import ca.phon.worker.PhonTask;

/**
 *
 */
public class QueryTask extends PhonTask {
	
	private final static Logger LOGGER = Logger.getLogger(QueryTask.class.getName());
	
	/** The project */
	private Project project;
	
	/** The corpus */
	private String corpus;
	
	/** The session name */
	private String session;
	
	/** The session */
	private Session transcript;
	
	/** The script text */
	private QueryScript queryScript;
	
	/** The search result */
	private ResultSet searchResults;
	
	/** Include excluded records? */
	private boolean includeExcludedRecords;
	
	/**
	 * writer for stdout
	 */
	private Writer stdoutWriter = null;
	
	/**
	 * wrtier for stderr
	 */
	private Writer stderrWriter = null;
	
	/** Errors encounterd during the query */
	private List<Exception> errors = new ArrayList<Exception>();
	
	/** Search results property */
	public final static String PROG_PROP = "SearchProgress";
	
	public QueryTask(Project p, QueryScript script) {
		super();
		
		this.project = p;
		this.queryScript = script;
	}
	
	public QueryTask(Project p, String c, String s, String scriptText, ScriptParam[] scriptParams,
			ResultSet s1) {
		super();
		
		this.project = p;
		this.corpus = c;
		this.session = s;
		this.queryScript = new QueryScript(scriptText);
		
		ScriptParam.copyParams(scriptParams, queryScript.getScriptParams());
//		this.params = scriptParams;
		
		this.searchResults = s1;
	}
	
	public QueryTask(Project p, Session t, String scriptText, ScriptParam[] scriptParams,
			ResultSet s1) {
		super();
		
		this.project = p;
		this.transcript = t;
		this.corpus = t.getCorpus();
		this.session = t.getName();
		this.queryScript = new QueryScript(scriptText);
//		this.params = scriptParams;
		ScriptParam.copyParams(scriptParams, queryScript.getScriptParams());
		
		this.searchResults = s1;
	}

	/* (non-Javadoc)
	 * @see ca.phon.application.PhonTask#performTask()
	 */
	@Override
	public void performTask() {
//		ITranscript t = loadTranscript();
//		
		if(getStatus() == TaskStatus.ERROR)
			return;
		
		if(transcript == null) {
			// load transcript
			try {
				transcript = project.openSession(corpus, session);
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
				super.setStatus(TaskStatus.ERROR);
				err = e;
				return;
			}
		}
		
		runSearch(transcript);
		
		super.setStatus(TaskStatus.FINISHED);
	}
	
	public void setResultSet(ResultSet rs) {
		this.searchResults = rs;
	}
	
	public Writer getStdoutWriter() {
		return stdoutWriter;
	}

	public void setStdoutWriter(Writer stdoutWriter) {
		this.stdoutWriter = stdoutWriter;
	}

	public Writer getStderrWriter() {
		return stderrWriter;
	}

	public void setStderrWriter(Writer stderrWriter) {
		this.stderrWriter = stderrWriter;
	}
	
	public Session getTranscript() {
		return transcript;
	}

	public void setSession(Session transcript) {
		this.transcript = transcript;
	}

	public void setSearchResults(ResultSet searchResults) {
		this.searchResults = searchResults;
	}

	private void runSearch(Session t) {
		errors.clear();
		
		Context scriptContext = 
			(new ContextFactory()).enterContext();
		scriptContext.setWrapFactory(new ExtendableWrapFactory());
		
		Scriptable scope = queryScript.setupScope(scriptContext);
		Script script = queryScript.getCompiledScript();
		
		if(script == null) {
			LOGGER.severe("Compilation error.");
			super.setStatus(TaskStatus.ERROR);
			err = new Exception("Compilation error");
			errors.add(err);
			super.setStatus(TaskStatus.ERROR);
			return;
		}
		
		// make sure script has a query_record method
		if(!queryScript.hasQueryRecord()) {
			LOGGER.severe("Script must implement the function query_record(session, record)");
			super.setStatus(TaskStatus.ERROR);
			err = new Exception("function query_record not found");
			errors.add(err);
			super.setStatus(TaskStatus.ERROR);
			return;
		}
		
		// load top-level objects
		try {
			script.exec(scriptContext, scope);
		} catch (Exception e) {
			errors.add(e);
			String prefix = "[" + getName() + "]\t";
			LOGGER.severe(prefix + 
					"Runtime error: " + e.toString());
			if(e instanceof WrappedException) {
				WrappedException we = (WrappedException)e;
				
				
				LOGGER.severe(prefix + we.details());
			} else if(e instanceof EcmaError) {
				EcmaError ecmaError = (EcmaError)e;
				
				LOGGER.severe(prefix + ecmaError.details());
			}
			
			LOGGER.warning(e.toString());
			err = e;
			
			// stop processing transcript
			super.setStatus(TaskStatus.ERROR);
			return;
		}
		
		/*
		 * Install script params
		 */
		queryScript.installScriptParams(scope);
		
		/* setup scope variables */
		// results
		final QueryFactory factory = QueryManager.getSharedInstance().createQueryFactory();
//		final ResultSet resultSet = factory.createResultSet();
//		setResultSet(resultSet);
		
		Object wrappedFactory = Context.javaToJS(factory, scope);
		ScriptableObject.putProperty(scope, "factory", wrappedFactory);
		
		Object wrappedResults = Context.javaToJS(searchResults, scope);
		ScriptableObject.putProperty(scope, "results", wrappedResults);
		
		final PrintWriter outWriter = new PrintWriter(
				new OutputStreamWriter(System.out) );
		final PrintWriter errWriter = new PrintWriter(
				new OutputStreamWriter(System.err) );
		
		//PrintWriter outOut = new PrintWriter(outWriter);
		Object wrappedOut = Context.javaToJS(outWriter, scope);
		ScriptableObject.putProperty(scope, "out", wrappedOut);
		
		//PrintWriter errOut = new PrintWriter(errWriter);
		Object wrappedErr = Context.javaToJS(errWriter, scope);
		ScriptableObject.putProperty(scope, "err", wrappedErr);
		
		int numRecords = t.getRecordCount();
		setProperty(PROG_PROP, 0);
		
		// run begin search if found
		if(queryScript.hasBeginSearch()) {
			try {
				ScriptableObject.callMethod(scriptContext, scope, "begin_search", new Object[]{t});
			} catch (Exception e) {
				errors.add(e);
				LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
				err = e;
				
				// stop processing transcript
				super.setStatus(TaskStatus.ERROR);
				return;
			}
		}
		
//		List<IUtterance> utts = t.getUtterances();
		for(int i = 0; i < numRecords; i++) {
			// check shutdown hook
			if(isShutdown())
				break;
			Record record = t.getRecord(i);
			
			// skip excluded records
			if(!isIncludeExcludedRecords() && record.isExcludeFromSearches())
				continue;
			
			// evaluate the script
			try {
				ScriptableObject.callMethod(scriptContext, scope, "query_record", new Object[]{i, record});
//				script.exec(scriptContext, scope);
			} catch (Exception e) {
				errors.add(e);
				String prefix = "[" + getName() + "]\t";
				LOGGER.severe(prefix + "Error at record #" + (i+1));
				LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
				
				err = e;
			}
			
			double percentDone = ((double)i+1)/numRecords * 100.0;
			int pDone = (int)Math.round(percentDone);
			setProperty(PROG_PROP, pDone);
		}
		
		// run end search if found
		if(queryScript.hasEndSearch()) {
			try {
				ScriptableObject.callMethod(scriptContext, scope, "end_search", new Object[]{t});
			} catch (Exception e) {
				errors.add(e);
				LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
	
				super.setStatus(TaskStatus.ERROR);
				err = e;
				return;
			}
		}
		
		Context.exit();
	}
	
	@Override
	public String getName() {
		return corpus + "." + session;
	}

	public ResultSet getSearchResults() {
		return searchResults;
	}

	public Project getProject() {
		return project;
	}
	
	public boolean isIncludeExcludedRecords() {
		return includeExcludedRecords;
	}

	public void setIncludeExcludedRecords(boolean includeExcludedRecords) {
		this.includeExcludedRecords = includeExcludedRecords;
	}
}
