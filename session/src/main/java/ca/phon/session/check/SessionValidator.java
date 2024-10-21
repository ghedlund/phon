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

/**
 * Session validator with plug-in support.  This class maintains the
 * list of available validator plug-ins as well as a set of
 * validation listeners.
 * 
 * @author Greg
 */
public class SessionValidator implements IExtendable {
	
	private final ExtensionSupport extSupport = new ExtensionSupport(SessionValidator.class, this);
	
	private final List<SessionCheck> sessionChecks = new ArrayList<>();
	
	private final List<ValidationListener> listeners = new ArrayList<>();
	
	private final List<ValidationEvent> events = new ArrayList<>();
	
	public SessionValidator() {
		this(SessionCheck.availableChecks());
	}
	
	public SessionValidator(SessionCheck[] sessionChecks) {
		this(Arrays.asList(sessionChecks));
	}
	
	public SessionValidator(List<SessionCheck> sessionChecks) {
		super();
		
		this.sessionChecks.addAll(sessionChecks);
	}

	public void fireValidationEvent(ValidationEvent.Severity severity, Session session, String message) {
		fireValidationEvent(new ValidationEvent(severity, session, message));
	}

	public void fireValidationEvent(Session session, String message) {
		fireValidationEvent(new ValidationEvent(session, message));
	}

	public void fireValidationEvent(ValidationEvent.Severity severity, Session session, int record, String message) {
		fireValidationEvent(new ValidationEvent(severity, session, record, message));
	}

	public void fireValidationEvent(Session session, int record, String message) {
		fireValidationEvent(new ValidationEvent(session, record, message));
	}

	public void fireValidationEvent(ValidationEvent.Severity severity, Session session, int record, String tierName, String message) {
		fireValidationEvent(new ValidationEvent(severity, session, record, tierName, message));
	}

	public void fireValidationEvent(Session session, int record, String tierName, String message) {
		fireValidationEvent(new ValidationEvent(session, record, tierName, message));
	}

	public void fireValidationEvent(final ValidationEvent evt) {
		events.add(evt);
		listeners.forEach( (l) -> { l.validationInfo(evt); } );
	}
	
	public boolean validate(Session session) {
		boolean modified = false;
		for(SessionCheck check:sessionChecks) {
			modified |= check.checkSession(this, session);
		}
		return modified;
	}

	public boolean validate(Session session, int elementIndex) {
		boolean modified = false;
		for(SessionCheck check:sessionChecks) {
			modified |= check.checkTranscriptElement(this, session, elementIndex);
		}
		return modified;
	}
	
	public void addValidationListener(ValidationListener listener) {
		this.listeners.add(listener);
	}
	
	public void removeValidationListener(ValidationListener listener) {
		this.listeners.remove(listener);
	}
	
	public List<ValidationListener> getValidationListeners() {
		return this.listeners;
	}
	
	public List<ValidationEvent> getValidationEvents() {
		return this.events;
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
