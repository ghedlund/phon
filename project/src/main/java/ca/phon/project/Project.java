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

import ca.phon.extensions.IExtendable;
import ca.phon.session.*;
import ca.phon.session.io.*;

import java.io.*;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Interface for a phon project.
 * Projects are responsible for corpus and session
 * lists as well as managing serialization for
 * sessions.
 *
 */
public interface Project extends IExtendable {

	/*
	 * Listeners
	 */
	public List<ProjectListener> getProjectListeners();

	public void addProjectListener(ProjectListener listener);

	public void removeProjectListener(ProjectListener listener);

	/*
	 * Events
	 */
	public void fireProjectStructureChanged(ProjectEvent pe);

	public void fireProjectDataChanged(ProjectEvent pe);

	public void fireProjectWriteLocksChanged(ProjectEvent pe);

	/**
	 * Project version
	 *
	 * @return the project version or 'unk' if not known
	 */
	public String getVersion();

	/**
	 * The location of the project.
	 *
	 * @return the project location
	 */
	public String getLocation();

	/**
	 * The name of the project.
	 *
	 * @return project name
	 */
	public String getName();

	/**
	 * Set project name
	 *
	 * @param name must match pattern '[ \w\d-]+'
	 */
	public void setName(String name);

	/**
	 * Project UUID
	 *
	 * UUID for the project
	 *
	 * @return uuid
	 */
	public UUID getUUID();

	/**
	 * Set project UUID
	 *
	 * @param uuid
	 */
	public void setUUID(UUID uuid);

	/**
	 * Get an iterator over the corpora in this project.
	 *
	 * @return iterator over corpora
	 */
	public Iterator<String> getCorpusIterator();

	/**
	 * Add corpus folder with given name
	 *
	 * @param name
	 * @throws IOException
	 */
	public void addCorpus(String name) throws IOException;

	/**
	 * Add a new corpus with the specified name.
	 *
	 * @param name
	 * @param description
	 * @throws IOException if the corpus could not be
	 *  created
	 */
	public void addCorpus(String name, String description)
		throws IOException;

	/**
	 * Rename a corpus
	 *
	 * @param corpus
	 * @param newName
	 *
	 * @throws IOException if the corpus could not be
	 *  renamed
	 */
	public void renameCorpus(String corpus, String newName)
		throws IOException;

	/**
	 * Delete the specified corpus and all sessions it contains.
	 *
	 * @param corpus
	 *
	 * @throws IOException if the corpus could not be deleted
	 */
	public void removeCorpus(String corpus)
		throws IOException;

	/**
	 * Get the description of the specified corpus.
	 *
	 * @param corpus the corpus name
	 */
	public String getCorpusDescription(String corpus);

	/**
	 * Set the description for the specified corpus.
	 *
	 * @param corpus
	 * @param description
	 */
	public void setCorpusDescription(String corpus, String description);

	/**
	 * Has a custom project media folder been assigned
	 * 
	 * @return <code>true</code> if project folder has been customized
	 * 
	 */
	public boolean hasCustomProjectMediaFolder();

	/**
	 * Get all media folders for the project
	 *
	 * @return list of media folders
	 */
	public List<String> getProjectMediaFolders();

	/**
	 * Set media folder for project, if any are set.  If multiple media folders
	 * are set, then all are removed and the new folder is added.
	 *
	 * @param mediaFolder If <code>null</code> sets the media folder
	 * back to default.
	 */
	public void setProjectMediaFolder(String mediaFolder);

	/**
	 * Add a media folder to the project
	 *
	 * @param mediaFolder
	 */
	public void addProjectMediaFolder(String mediaFolder);

	/**
	 * Add a media folder to the project at the specified index
	 *
	 * @param index
	 * @param mediaFolder
	 */
	public void addProjectMediaFolder(int index, String mediaFolder);

	/**
	 * Remove a media folder from the project
	 *
	 * @param mediaFolder
	 */
	public void removeProjectMediaFolder(String mediaFolder);

	/**
	 * Get the Session template for the given corpus.
	 *
	 * @param corpus
	 *
	 * @return session template or <code>null</code> if not found
	 * @throws IOException
	 */
	public Session getSessionTemplate(String corpus)
		throws IOException;

	/**
	 * Save the Session template for the given corpus.
	 *
	 * @param corpus
	 * @param template
	 *
	 * @throws IOException
	 */
	public void saveSessionTemplate(String corpus, Session template)
			throws IOException;

	/**
	 * Create a new session from the corpus template (if it exists)
	 * This method will also add the session to the specified corpus.
	 *
	 * @param corpus
	 * @param session
	 *
	 * @return new Session object
	 */
	public Session createSessionFromTemplate(String corpus, String session)
		throws IOException;

	/**
	 * Get an iterator over the sessions in the specified corpus.
	 *
	 * @param corpus
	 *
	 * @return iterator over sessions in the corpus
	 */
	public Iterator<String> getSessionIterator(String corpus);

	/**
	 * Return the path to the given corpus.
	 *
	 * @param corpus
	 */
	public String getCorpusPath(String corpus);

	/**
	 * Set path of corpus.
	 *
	 * @param corpus
	 * @param path
	 */
	public void setCorpusPath(String corpus, String path);

	/**
	 * Returns the number of records in a session w/o opening
	 * the session. This method is faster than using
	 * openSession(corpus, session).numberOfRecords()
	 *
	 * @param session
	 * @return number of records in the session
	 * @throws IOException
	 */
	public int numberOfRecordsInSession(String corpus, String session)
		throws IOException;

	/**
	 * Return a set of participants which are found in the
	 * given collection of Sessions.  The participant objects
	 * returned by this method will include the {@link ParticipantHistory}
	 * extension.
	 *
	 * Participants from two sessions are considered to be the same
	 * if their ids, names and roles match.  If the speaker for some records
	 * is unidenified, a clone of Participant.UNKOWN will be added in the returned
	 * set.
	 *
	 * @param sessions
	 * @return a set of participants
	 */
	public Set<Participant> getParticipants(Collection<SessionPath> sessions);

	/**
	 * Open the specified session.  This will create a new session
	 * object with the data currently on the storage device.
	 *
	 * @param corpus
	 * @param session
	 *
	 * @return the session
	 *
	 * @throws IOException
	 */
	public Session openSession(String corpus, String session)
		throws IOException;

	/**
	 * Open specified session using the provided reader.
	 *
	 * @param corpus
	 * @param session
	 * @param reader
	 *
	 * @return the session
	 *
	 * @throws IOException
	 */
	public Session openSession(String corpus, String session, SessionReader reader)
		throws IOException;

	/**
	 * Get path to the given session.
	 *
	 * @param session
	 *
	 * @return path to given session
	 */
	public String getSessionPath(Session session);

	/**
	 * Get path to the given session.
	 *
	 * @param corpus
	 * @param session
	 *
	 * @return path to given session
	 */
	public String getSessionPath(String corpus, String session);

	/**
	 * Get a write lock for a session.  Before writing a write lock
	 * must be obtained from the project.
	 *
	 * @param session
	 *
	 * @return the session write lock or < 0 if a write lock
	 *  was not obtained
	 * @throws IOException
	 */
	public UUID getSessionWriteLock(Session session)
		throws IOException;

	/**
	 * Get a write lock for a session.  Before writing a write lock
	 * must be obtained from the project.
	 *
	 * @param corpus
	 * @param session
	 *
	 * @return the session write lock or <code>null</code>
	 * @throws IOException
	 */
	public UUID getSessionWriteLock(String corpus, String session)
		throws IOException;

	/**
	 * Release the write lock for a session.
	 *
	 * @param session
	 * @param writeLock
	 *
	 * @throws IOException
	 */
	public void releaseSessionWriteLock(Session session, UUID writeLock)
		throws IOException;

	/**
	 * Release the write lock for a session.
	 *
	 * @param session
	 * @param writeLock
	 *
	 * @throws IOException
	 */
	public void releaseSessionWriteLock(String corpus, String session, UUID writeLock)
		throws IOException;

	/**
	 * Tells whether the given session is locked
	 *
	 * @param session
	 * @return <code>true</code> if session is locked, <code>false</code>
	 *  otherwise
	 */
	public boolean isSessionLocked(Session session);

	/**
	 * Tells wheater the given session is locked
	 *
	 * @param corpus
	 * @param session
	 *
	 * @return <code>true</code> if the session is locked, <code>false</code>
	 *  otherwise
	 */
	public boolean isSessionLocked(String corpus, String session);

	/**
	 * Save a session
	 *
	 * @param session
	 * @param writeLock
	 *
	 * @throws IOException
	 */
	public void saveSession(Session session, UUID writeLock)
		throws IOException;

	/**
	 * Save a session to the specified corpus and new
	 * sessionName.
	 *
	 * @param corpus
	 * @param sessionName
	 * @param session
	 * @param writeLock
	 *
	 * @throws IOException
	 */
	public void saveSession(String corpus, String sessionName, Session session, UUID writeLock)
		throws IOException;

	/**
	 * Save a session writing the file using the given writer.
	 *
	 * @param corpus
	 * @param sessionName
	 * @param session
	 * @param writer
	 * @param writeLock
	 *
	 * @throws IOException
	 */
	public void saveSession(String corpus, String sessionName, Session session, SessionWriter writer, UUID writeLock)
			throws IOException;

	/**
	 * Remove a session from the project.  The writeLock
	 * for the session is also released.
	 *
	 * @param session
	 * @param writeLock
	 *
	 * @throws IOException
	 */
	public void removeSession(Session session, UUID writeLock)
		throws IOException;

	/**
	 * Remove a session from the project.  The writeLock
	 * for the session is also released.
	 *
	 * @parma corpus
	 * @param session
	 * @param writeLock
	 *
	 * @throws IOException
	 */
	public void removeSession(String corpus, String session, UUID writeLock)
		throws IOException;

	/**
	 * Returns the modification date for the given session
	 *
	 * @param session
	 *
	 * @return session modification date in system time zone
	 */
	public ZonedDateTime getSessionModificationTime(Session session);

	/**
	 * Returns the modification date for the specified session.
	 *
	 * @param corpus
	 * @param session
	 *
	 * @return session modification date in system time zone
	 */
	public ZonedDateTime getSessionModificationTime(String corpus, String session);

	/**
	 * Returns the size on disk for the given session.
	 *
	 * @param session
	 *
	 * @return session size in bytes
	 */
	public long getSessionByteSize(Session session);

	/**
	 * Returns the size on disk for the given session.
	 *
	 * @param corpus
	 * @param session
	 *
	 * @return session size in bytes
	 */
	public long getSessionByteSize(String corpus, String session);

	/**
	 * Get the location of the project resources folder.
	 *
	 * @return location of the resources folder (default: <code>project_folder/__res</code>)
	 */
	public String getResourceLocation();

	/**
	 * Set the location of the project resources folder.
	 *
	 * @param location
	 */
	public void setResourceLocation(String location);

	/**
	 * Get an input stream for the specified project resource.
	 * The resource name should be a relative path including filename.
	 * E.g., 'ca.phon.myplugin/module/corpus/session.dat'
	 *
	 * @param resourceName
	 *
	 * @return an input stream for the specified resource
	 *
	 * @throws IOException
	 */
	public InputStream getResourceInputStream(String resourceName)
		throws IOException;

	/**
	 * Get an output stream for the specified resource.  If the resource
	 * does not exist it is created.  If the resource already exists
	 * it is overwritten.
	 *
	 * @param resourceName
	 *
	 * @return output stream for the specified resource
	 *
	 * @throws IOException
	 */
	public OutputStream getResourceOutputStream(String resourceName)
		throws IOException;

	// region Deprecated methods
	/**
	 * Get the list of corpora in this project.  Corpus names
	 * are returned in alphabetical order.
	 *
	 * @return list of corpora
	 *
	 * @deprecated use {@link Project::getCorpusIterator()} instead
	 */
	public List<String> getCorpora();

	/**
	 * Get media folder for the project, if any are set.  If multiple media folders
	 * are set, the first one is returned.
	 *
	 * @deprecated Since Phon 4.0 use {@link Project::} instead
	 */
	public String getProjectMediaFolder();

	/**
	 * Has a custom corpus media folder been assigned
	 *
	 * @param corpus
	 * @return <code>true</code> if a custom media folder is assigned for the
	 * given corpus
	 *
	 * @deprecated Since Phon 4.0 will always return <code>false</code>
	 */
	public boolean hasCustomCorpusMediaFolder(String corpus);

	/**
	 * Get the media folder for the specified corpus.
	 *
	 * @return mediaFolder or the project media folder if not specified
	 *
	 * @deprecated Since Phon 4.0 will always return null
	 */
	public String getCorpusMediaFolder(String corpus);

	/**
	 * Set the media folder for the specified corpus.
	 *
	 * @param mediaFolder
	 *
	 * @deprecated Since Phon 4.0 will do nothing
	 */
	public void setCorpusMediaFolder(String corpus, String mediaFolder);

	/**
	 * Get the session names contained in a corpus in alphabetical
	 * order.
	 *
	 * @param corpus
	 *
	 * @return the list of sessions in the specified corpus
	 *
	 * @deprecated use {@link Project::getSessionIterator(String)} instead
	 */
	public List<String> getCorpusSessions(String corpus);
	// endregion
}
