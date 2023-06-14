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
package ca.phon.session.check;

import ca.phon.extensions.*;
import ca.phon.session.Session;

import java.util.*;

public class ValidationEvent implements IExtendable {

	public static enum Severity {
		INFO,
		WARNING,
		ERROR
	};

	private final ExtensionSupport extSupport = new ExtensionSupport(ValidationEvent.class, this);

	private Severity severity = Severity.WARNING;

	private Session session;
	
	private int record;
	
	private String tierName;
	
	private String message;
	
	private List<SessionQuickFix> quickFixes;

	public ValidationEvent(Severity severity, Session session, String message) {
		this(severity, session, message, new SessionQuickFix[0]);
	}

	public ValidationEvent(Session session, String message) {
		this(Severity.WARNING, session, message);
	}

	public ValidationEvent(Severity severity, Session session, String message, SessionQuickFix ... quickFixes) {
		super();
		this.severity = severity;
		this.session = session;
		this.message = message;
		this.quickFixes = List.of(quickFixes);

	}

	public ValidationEvent(Session session, String message, SessionQuickFix ... quickFixes) {
		this(Severity.WARNING, session, message, quickFixes);
	}

	public ValidationEvent(Severity severity, Session session, int record, String message) {
		this(severity, session, record, message, new SessionQuickFix[0]);
	}

	public ValidationEvent(Session session, int record, String message) {
		this(Severity.WARNING, session, record, message);
	}

	public ValidationEvent(Severity severity, Session session, int record, String message, SessionQuickFix ... quickFixes) {
		super();
		this.severity = severity;
		this.session = session;
		this.record = record;
		this.message = message;
		this.quickFixes = List.of(quickFixes);
	}

	public ValidationEvent(Session session, int record, String message, SessionQuickFix ... quickFixes) {
		this(Severity.WARNING, session, record, message, quickFixes);
	}

	public ValidationEvent(Session session, int record, String tierName, String message) {
		this(session, record, tierName, message, new SessionQuickFix[0]);
	}

	public ValidationEvent(Session session, int record, String tierName,
	                       String message, SessionQuickFix ... quickFixes) {
		this(Severity.WARNING, session, record, tierName, message, quickFixes);
	}

	public ValidationEvent(Severity severity, Session session, int record, String tierName,
			String message, SessionQuickFix ... quickFixes) {
		super();
		this.severity = severity;
		this.session = session;
		this.record = record;
		this.tierName = tierName;
		this.message = message;
		this.quickFixes = List.of(quickFixes);
	}

	public Severity getSeverity() {
		return this.severity;
	}

	public void setSeverity(Severity severity) {
		this.severity = severity;
	}

	public Session getSession() {
		return session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public int getRecord() {
		return record;
	}

	public void setRecord(int record) {
		this.record = record;
	}

	public String getTierName() {
		return tierName;
	}

	public void setTierName(String tierName) {
		this.tierName = tierName;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
	/**
	 * Can this event be automatically fixed?  Sub-classes
	 * should override this method.
	 * 
	 * @return can the validation event be fixed
	 */
	public boolean canFix() {
		return quickFixes.size() > 0;
	}
	
	/**
	 * Options for fixing the problem identified by this
	 * validation event.
	 * 
	 * @return a list of validation options or an empty
	 *  list if this problem does not have a quick fix
	 */
	public List<SessionQuickFix> getQuickFixes() {
		return quickFixes;
	}
	
	@Override
	public String toString() {
		return getMessage();
	}

	@Override
	public Set<Class<?>> getExtensions() {
		return extSupport.getExtensions();
	}

	@Override
	public <T> T getExtension(Class<T> cap) {
		return extSupport.getExtension(cap);
	}

	@Override
	public <T> T putExtension(Class<T> cap, T impl) {
		return extSupport.putExtension(cap, impl);
	}

	@Override
	public <T> T removeExtension(Class<T> cap) {
		return extSupport.removeExtension(cap);
	}

}
