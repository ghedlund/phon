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
package ca.phon.session.io;

import ca.phon.plugin.*;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Used to create instances of session readers.
 *
 */
public class SessionInputFactory {
	
	private List<IPluginExtensionPoint<SessionReader>> readerExtPts;
		
	/**
	 * Constructor
	 */
	public SessionInputFactory() {
		super();
		readerExtPts = PluginManager.getInstance().getExtensionPoints(SessionReader.class);
	}

	public static Set<String> getSessionExtensions() {
		final Set<String> retVal = new LinkedHashSet<>();

		final SessionInputFactory factory = new SessionInputFactory();
		for(SessionIO sessionIO:factory.availableReaders()) {
			retVal.add(sessionIO.extension());
		}

		return retVal;
	}
	
	/**
	 * Get specified reader.
	 * 
	 * @param sessionIO
	 * @return session reader or <code>null</code> if not found
	 */
	public SessionReader createReader(SessionIO sessionIO) {
		return createReader(sessionIO.id(), sessionIO.version());
	}
	
	/**
	 * Get the list of available session readers.
	 * 
	 * @return list of readers
	 */
	public List<SessionIO> availableReaders() {
		final List<SessionIO> retVal = new ArrayList<SessionIO>();
		
		for(IPluginExtensionPoint<SessionReader> extPt:readerExtPts) {
			final SessionReader reader = extPt.getFactory().createObject(new Object[0]);
			final SessionIO sessionIO = reader.getClass().getAnnotation(SessionIO.class);
			
			if(sessionIO != null)
				retVal.add(sessionIO);
		}
		
		return retVal;
	}
	
	/**
	 * Create a new session reader given the SessionIO version.
	 * 
	 * @param id
	 * @param version
	 * @return the new SessionReader or <code>null</code> if not found
	 */
	public SessionReader createReader(String id, String version) {
		SessionReader retVal = null;
		
		for(IPluginExtensionPoint<SessionReader> extPt:readerExtPts) {
			final SessionReader reader = extPt.getFactory().createObject(new Object[0]);
			final SessionIO sessionIO = reader.getClass().getAnnotation(SessionIO.class);
			if(sessionIO != null && sessionIO.version().equals(version) && sessionIO.id().equals(id)) {
				retVal = reader;
				break;
			}
		}
		
		return retVal;
	}
	
	/**
	 * Create a new session reader for the given file.
	 * 
	 * @param file
	 * @return session reader or <code>null</code> if not found
	 * 
	 * @throws IOException
	 */
	public SessionReader createReaderForFile(File file) {
		SessionReader retVal = null;
		
		for(IPluginExtensionPoint<SessionReader> extPt:readerExtPts) {
			final SessionReader reader = extPt.getFactory().createObject(new Object[0]);
			final SessionIO sessionIO = reader.getClass().getAnnotation(SessionIO.class);
			if(sessionIO != null && file.getName().endsWith(sessionIO.extension())) {
				try {
					if(reader.canRead(file)) {
						retVal = reader;
						break;
					}
				} catch (IOException e) {
					Logger.getLogger(getClass().getName()).log(Level.WARNING, e.getLocalizedMessage(), e);
				}
			}
		}
		
		return retVal;
	}
	
}
