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

package ca.phon.query.db.xml;

import ca.phon.project.Project;
import ca.phon.query.db.*;
import ca.phon.query.db.xml.io.query.QueryType;
import ca.phon.query.db.xml.io.resultset.ResultSetType;
import ca.phon.session.SessionFactory;
import ca.phon.session.SessionPath;
import ca.phon.xml.XMLConstants;
import jakarta.xml.bind.*;
import org.xml.sax.SAXException;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.*;
import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An implementation of {@link ResultSetManager} that implements an XML-based
 * system. XML data is stored on on disk.
 */
public class XMLResultSetManager implements ResultSetManager {

	/**
	 * Folder inside of <code>&lt;project&gt;/__res</code> where
	 * query results are saved.
	 */
	public static final String DEFAULT_QUERY_FOLDER = ".query_results";

	/**
	 * Default constructor.
	 */
	public XMLResultSetManager() { }

	/**
	 * Get the path for storing queries.
	 * @param project
	 * @return
	 */
	static File getQueriesPath(Project project) {
		final File retVal = new File(project.getLocation(), DEFAULT_QUERY_FOLDER);
		return retVal;
	}

	static File getQueryPath(Project project, Query query) {
		final File queriesPath = getQueriesPath(project);

		final File retVal = new File(queriesPath, query.getUUID().toString());
		return retVal;
	}

	@Override
	public List<Query> getQueries(Project project) {
		List<Query> queries = new ArrayList<Query>();

		final File queriesPath = getQueriesPath(project);
		if(queriesPath.exists() && queriesPath.isDirectory()) {
			for(File queryDir : queriesPath.listFiles()) {
				final File testFile = new File(queryDir, "query.xml");
				if(queryDir.isDirectory() && testFile.exists()) {
					try {
						Query query = loadQuery(project, queryDir.getName());
						queries.add(query);
					} catch(IOException exc) {
						Logger.getLogger(getClass().getName()).log(Level.WARNING, exc.getLocalizedMessage(), exc);
					}
				}
			}
		}

		return queries;
	}

	@Override
	public List<ResultSet> getResultSetsForQuery(Project project, Query query) {
		File queryPath = getQueryPath(project, query);
		List<ResultSet> resultSets = new ArrayList<ResultSet>();
		if(!queryPath.exists() || !queryPath.isDirectory())
			return resultSets;
		final List<SessionPath> resultSessionPaths = scanResultSets(queryPath.toPath(), queryPath);
		for(SessionPath sp:resultSessionPaths) {
			try {
				ResultSet resultSet = loadResultSet(project, query, sp.toString());
				resultSets.add(resultSet);
			} catch(IOException exc) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, exc.getLocalizedMessage(), exc);
			}
		}
		return resultSets;
	}

	private List<SessionPath> scanResultSets(Path queryPath, File folder) {
		List<SessionPath> retVal = new ArrayList<>();

		for(File child:folder.listFiles()) {
			if(child.isDirectory() && !child.isHidden()) {
				retVal.addAll(scanResultSets(queryPath, child));
			} else if(child.isFile() && !child.isHidden() && !"query.xml".equals(child.getName()) && child.getName().endsWith(".xml")) {
				final Path rsPath = child.toPath();
				final Path relPath = queryPath.relativize(rsPath);
				retVal.add(new SessionPath(relPath.getParent() != null ? relPath.getParent().toString() : "",
						relPath.getFileName().toString().substring(0, relPath.getFileName().toString().length()-4)));
			}
		}

		return retVal;
	}

	@Override
	public void saveQuery(Project project, Query query) throws IOException {
		final File queryPath = getQueryPath(project, query);

		// Create the directory, if necessary
		if(!queryPath.exists() && !queryPath.mkdirs())
			return;

		final File queryFile = new File(queryPath, "query.xml");
		try {
			// Use JAXBElement wrapper around object because they do not have
			// the XMLRootElement annotation
			final QueryType qt = ((XMLQuery)query).getXMLObject();
			final JAXBElement<QueryType> jaxbElem = (new ca.phon.query.db.xml.io.query.ObjectFactory()).createQuery(qt);

			// Initialize marshaller and write to disk
			final JAXBContext context = JAXBContext.newInstance("ca.phon.query.db.xml.io.query");
			final Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.marshal(jaxbElem, queryFile);
		} catch (JAXBException exc) {
			throw new IOException("Could not save query to disk", exc);
		}
	}

	@Override
	public Query loadQuery(Project project, String queryName) throws IOException {
		File queriesPath = getQueriesPath(project);
		File queryPath = new File(queriesPath, queryName);
		File queryFile = new File(queryPath, "query.xml");
		return new XMLLazyQuery(this, queryFile);
	}

	@SuppressWarnings("unchecked")
	QueryType loadQuery(File queryFile) throws IOException {
		Schema schema = null;
		try {
			SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			schema = sf.newSchema(new StreamSource(getClass().getClassLoader().getResourceAsStream("xml/xsd/query.xsd")) );
		} catch(SAXException exc) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, exc.getLocalizedMessage(), exc);
		}

		QueryType query = null;
		try {
			JAXBContext context = JAXBContext.newInstance("ca.phon.query.db.xml.io.query");
			Unmarshaller unmarshaller = context.createUnmarshaller();
			unmarshaller.setSchema(schema);
			query = ((JAXBElement<QueryType>)unmarshaller.unmarshal(queryFile)).getValue();
		} catch(JAXBException exc) {
			//PhonLogger.severe(XMLResultSetManager.class, "Could not load query file.");
			//PhonLogger.severe(XMLResultSetManager.class, "JAXBException: " + exc.getLocalizedMessage());
			throw new IOException("Could not load query file", exc);
		}

		return query;
	}

	@Override
	public void saveResultSet(Project project, Query query, ResultSet resultSet) throws IOException {
		File queryPath = getQueryPath(project, query);
		File resultSetFile = new File(queryPath, resultSet.getSessionPath() + ".xml");

		// ensure the list of metadata keys is created for quick reference later
		resultSet.getMetadataKeys();

		try {
			if(!resultSetFile.getParentFile().exists()) {
				resultSetFile.getParentFile().mkdirs();
			}

			// Use JAXBElement wrapper around object because they do not have
			// the XMLRootElement annotation
			ResultSetType rst = ((XMLResultSet)resultSet).getXMLObject();
			JAXBElement<ResultSetType> jaxbElem = (new ca.phon.query.db.xml.io.resultset.ObjectFactory()).createResultSet(rst);

			// Initialize marshaller and write to disk
			JAXBContext context = JAXBContext.newInstance("ca.phon.query.db.xml.io.resultset");
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.marshal(jaxbElem, resultSetFile);
		} catch (JAXBException exc) {
			throw new IOException("Could not save result set to disk", exc);
		}
	}

	@Override
	public ResultSet loadResultSet(Project project, Query query, String sessionPath) throws IOException {
		File queryPath = getQueryPath(project, query);
		File resultSetFile = new File(queryPath, sessionPath + ".xml");
		return new XMLLazyResultSet(this, resultSetFile);
	}

	/**
	 * Load XML data for a result set.
	 */
	@SuppressWarnings("unchecked")
	ResultSetType loadResultSet(File resultSetFile) throws IOException {
		Schema schema = null;
		try {
			SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			schema = sf.newSchema(new StreamSource(getClass().getClassLoader().getResourceAsStream("xml/xsd/resultset.xsd")));
		} catch(SAXException exc) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, exc.getLocalizedMessage(), exc);
		}

		ResultSetType resultSet = null;
		try {
			JAXBContext context = JAXBContext.newInstance("ca.phon.query.db.xml.io.resultset");
			Unmarshaller unmarshaller = context.createUnmarshaller();
			unmarshaller.setSchema(schema);
			resultSet = ((JAXBElement<ResultSetType>)unmarshaller.unmarshal(resultSetFile)).getValue();
		} catch(JAXBException exc) {
			throw new IOException("Could not load result set file", exc);
		}

		return resultSet;
	}

	@Override
	public void deleteQuery(Project project, Query query)
			throws IOException {
		final File queryFile = getQueryPath(project, query);
		if(queryFile.exists() && queryFile.isDirectory()) {
			for(File qFile:queryFile.listFiles()) {
				if(!qFile.delete()) {
					throw new IOException("File " + qFile.getAbsolutePath() + " could not be removed from storage device.");
				}

			}
			if(!queryFile.delete()) {
				throw new IOException("Folder " + queryFile.getAbsolutePath() + " could not be removed from storage device.");
			}
		}
	}

	@Override
	public void deleteResultSet(Project project, Query query,
			ResultSet resultset) throws IOException {
		final File queryFile = getQueryPath(project, query);
		final File rsFile = new File(queryFile, resultset.getSessionPath() + ".xml");
		if(rsFile.exists()) {
			if(!rsFile.delete()) {
				throw new IOException("Unable to delete '"  + rsFile.getAbsolutePath() + "'");
			}
		}
	}

	@Override
	public void renameQuery(Project project, Query query, String newName)
			throws IOException {
		final File oldQueryFile = getQueryPath(project, query);
		final String oldQueryName = query.getName();
		if(oldQueryFile.exists() && oldQueryFile.isDirectory()) {
			// attempt to rename folder
			query.setName(newName);
			final File newQueryFile = getQueryPath(project, query);
			if(!oldQueryFile.renameTo(newQueryFile)) {
				query.setName(oldQueryName);
				throw new IOException("Unable not re-name query.");
			}
		}
	}

}
