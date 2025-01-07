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
package ca.phon.project;

import java.util.*;

/**
 * Wrapper for project events.
 *
 *
 */
public class ProjectEvent {

	/**
	 * Event type
	 */
	public static enum ProjectEventType {
		CORPUS_ADDED,
		CORPUS_REMOVED,
		@Deprecated
		PROJECT_MEDIAFOLDER_CHANGED,
		PROJECT_MEDIAFOLDER_ADDED,
		PROJECT_MEDIAFOLDER_REMOVED,
		CORPUS_DESCRIPTION_CHANGED,
		@Deprecated
		CORPUS_MEDIAFOLDER_CHANGED,
		SESSION_ADDED,
		SESSION_REMOVED,
		SESSION_CHANGED,
		PROJECT_NAME_CHANGED,
		PROJECT_UUID_CHANGED;
	};

	/**
	 * Event properties
	 */
	public static enum ProjectEventProp {
		CORPUS,
		SESSION,
		INDEX,
		OLD_CORPUS_DESCRIPTION,
		NEW_CORPUS_DESCRIPTION,
		@Deprecated
		OLD_MEDIAFOLDER,
		@Deprecated
		NEW_MEDIAFOLDER,
		MEDIAFOLDER,
		OLD_PROJECT_NAME,
		NEW_PROJECT_NAME,
		OLD_PROJECT_UUID,
		NEW_PROJECT_UUID;
	};

	/**
	 * Event type
	 */
	private ProjectEventType eventType;

	/**
	 * Properties
	 *
	 */
	private final Map<ProjectEventProp, String> eventProps =
			new HashMap<ProjectEvent.ProjectEventProp, String>();

	/*
	 * Static initializers
	 */

	/**
	 * Create a new corpus added event.
	 *
	 * @param corpus the name of the added corpus
	 *
	 * @return
	 */
	public static ProjectEvent newCorpusAddedEvent(String corpus) {
		final ProjectEvent retVal = new ProjectEvent(ProjectEventType.CORPUS_ADDED);
		retVal.setProperty(ProjectEventProp.CORPUS, corpus);
		return retVal;
	}

	/**
	 * Create a new corpus removed event
	 *
	 * @param corpus the name of the removed corpus
	 *
	 * @return
	 */
	public static ProjectEvent newCorpusRemovedEvent(String corpus) {
		final ProjectEvent retVal = new ProjectEvent(ProjectEventType.CORPUS_REMOVED);
		retVal.setProperty(ProjectEventProp.CORPUS, corpus);
		return retVal;
	}

	/**
	 * Create a new corpus description changed event.
	 *
	 * @param corpus
	 * @param oldDescription
	 * @param newDescription
	 *
	 * @return
	 */
	public static ProjectEvent newCorpusDescriptionChangedEvent(String corpus, String oldDescription, String newDescription) {
		final ProjectEvent retVal = new ProjectEvent(ProjectEventType.CORPUS_DESCRIPTION_CHANGED);
		retVal.setProperty(ProjectEventProp.CORPUS, corpus);
		retVal.setProperty(ProjectEventProp.OLD_CORPUS_DESCRIPTION, oldDescription);
		retVal.setProperty(ProjectEventProp.NEW_CORPUS_DESCRIPTION, newDescription);
		return retVal;
	}

	@Deprecated
	public static ProjectEvent newCorpusMediaFolderChangedEvent(String corpus, String oldFolder, String newFolder) {
		final ProjectEvent retVal = new ProjectEvent(ProjectEventType.CORPUS_MEDIAFOLDER_CHANGED);
		retVal.setProperty(ProjectEventProp.CORPUS, corpus);
		retVal.setProperty(ProjectEventProp.OLD_MEDIAFOLDER, oldFolder);
		retVal.setProperty(ProjectEventProp.NEW_MEDIAFOLDER, newFolder);
		return retVal;
	}

	@Deprecated
	public static ProjectEvent newProjectMediaFolderChangedEvent(String oldFolder, String newFolder) {
		final ProjectEvent retVal = new ProjectEvent(ProjectEventType.PROJECT_MEDIAFOLDER_CHANGED);
		retVal.setProperty(ProjectEventProp.OLD_MEDIAFOLDER, oldFolder);
		retVal.setProperty(ProjectEventProp.NEW_MEDIAFOLDER, newFolder);
		return retVal;
	}

	public static ProjectEvent newProjectMediaFolderAddedEvent(int index, String mediaFolder) {
		final ProjectEvent retVal = new ProjectEvent(ProjectEventType.PROJECT_MEDIAFOLDER_ADDED);
		retVal.setProperty(ProjectEventProp.INDEX, Integer.toString(index));
		retVal.setProperty(ProjectEventProp.MEDIAFOLDER, mediaFolder);
		return retVal;
	}

	public static ProjectEvent newProjectMediaFolderRemovedEvent(int index, String mediaFolder) {
		final ProjectEvent retVal = new ProjectEvent(ProjectEventType.PROJECT_MEDIAFOLDER_REMOVED);
		retVal.setProperty(ProjectEventProp.INDEX, Integer.toString(index));
		retVal.setProperty(ProjectEventProp.MEDIAFOLDER, mediaFolder);
		return retVal;
	}

	/**
	 * Create a new session added event
	 *
	 * @param corpus
	 * @param session
	 *
	 * @return
	 */
	public static ProjectEvent newSessionAddedEvent(String corpus, String session) {
		final ProjectEvent retVal = new ProjectEvent(ProjectEventType.SESSION_ADDED);
		retVal.setProperty(ProjectEventProp.CORPUS, corpus);
		retVal.setProperty(ProjectEventProp.SESSION, session);
		return retVal;
	}

	/**
	 * Create a new session removed event.
	 *
	 * @param corpus
	 * @param session
	 *
	 * @return
	 */
	public static ProjectEvent newSessionRemovedEvent(String corpus, String session) {
		final ProjectEvent retVal = new ProjectEvent(ProjectEventType.SESSION_REMOVED);
		retVal.setProperty(ProjectEventProp.CORPUS, corpus);
		retVal.setProperty(ProjectEventProp.SESSION, session);
		return retVal;
	}

	/**
	 * Create a new session chagned event.
	 *
	 * @param corpus
	 * @param session
	 *
	 * @return
	 */
	public static ProjectEvent newSessionChangedEvent(String corpus, String session) {
		final ProjectEvent retVal = new ProjectEvent(ProjectEventType.SESSION_CHANGED);
		retVal.setProperty(ProjectEventProp.CORPUS, corpus);
		retVal.setProperty(ProjectEventProp.SESSION, session);
		return retVal;
	}

	/**
	 * Create a new project name changed event
	 *
	 * @param oldName
	 * @param newName
	 *
	 * @return
	 */
	public static ProjectEvent newNameChangedEvent(String oldName, String newName) {
		final ProjectEvent retVal = new ProjectEvent(ProjectEventType.PROJECT_NAME_CHANGED);
		retVal.setProperty(ProjectEventProp.OLD_PROJECT_NAME, oldName);
		retVal.setProperty(ProjectEventProp.NEW_PROJECT_NAME, newName);
		return retVal;
	}

	/**
	 * Create a new project uuid chagned event
	 *
	 * @param oldUUID
	 * @param newUUID
	 *
	 * @return
	 */
	public static ProjectEvent newUUIDChangedEvent(String oldUUID, String newUUID) {
		final ProjectEvent retVal = new ProjectEvent(ProjectEventType.PROJECT_UUID_CHANGED);
		retVal.setProperty(ProjectEventProp.OLD_PROJECT_UUID, oldUUID);
		retVal.setProperty(ProjectEventProp.NEW_PROJECT_UUID, newUUID);
		return retVal;
	}

	/**
	 * Hidden constructor
	 */
	private ProjectEvent(ProjectEventType eventType) {
		super();
		this.eventType = eventType;
	}

	/**
	 * Get the event type
	 *
	 * @return the project event type
	 */
	public ProjectEventType getEventType() {
		return this.eventType;
	}

	/**
	 * Set the event type
	 */
	public void setEventType(ProjectEventType eventType) {
		this.eventType = eventType;
	}

	/**
	 * Get the value of the specified property.
	 *
	 * @param prop
	 * @return value
	 */
	public String getProperty(ProjectEventProp prop) {
		return eventProps.get(prop);
	}

	/**
	 * Set the value of an event property
	 *
	 * @param prop
	 * @param value
	 */
	public void setProperty(ProjectEventProp prop, String value) {
		eventProps.put(prop, value);
	}

}
