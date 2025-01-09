package ca.phon.project;

import ca.phon.extensions.ExtensionSupport;
import ca.phon.project.exceptions.ProjectConfigurationException;
import ca.phon.project.io.*;
import jakarta.xml.bind.*;

import javax.xml.stream.*;
import java.io.*;
import java.util.*;

public abstract class AbstractProject implements Project {

	/**
	 * Local projects no longer create this file but will read from it
	 * Remote projects require this file exists
	 */
	@Deprecated
	protected final static String PROJECT_XML_FILE = "project.xml";

	private final ExtensionSupport extSupport;

	private final List<ProjectListener> projectListeners =
			Collections.synchronizedList(new ArrayList<ProjectListener>());

	public AbstractProject() {
		super();

		extSupport = new ExtensionSupport(Project.class, this);
		extSupport.initExtensions();
	}

	/**
	 * Load project data from the project.xml file.
	 * If not found, empty project data is created.
	 *
	 * @return projectData
	 *
	 * @deprecated Since Phon 3
	 */
	@Deprecated
	protected ProjectType loadProjectData(InputStream inputStream) throws ProjectConfigurationException {
		try {
			final JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
			final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

			final XMLInputFactory inputFactory = XMLInputFactory.newFactory();
			final XMLEventReader eventReader = inputFactory.createXMLEventReader(inputStream);

			final JAXBElement<ProjectType> projectEle =
					unmarshaller.unmarshal(eventReader, ProjectType.class);
			return projectEle.getValue();
		} catch (JAXBException | XMLStreamException jaxbEx) {
			throw new ProjectConfigurationException(jaxbEx);
		}
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

	@Override
	public List<ProjectListener> getProjectListeners() {
		return Collections.unmodifiableList(projectListeners);
	}

	@Override
	public void addProjectListener(ProjectListener listener) {
		if(!projectListeners.contains(listener)) {
			projectListeners.add(listener);
		}
	}

	@Override
	public void removeProjectListener(ProjectListener listener) {
		projectListeners.remove(listener);
	}

	@Override
	public void fireProjectStructureChanged(ProjectEvent pe) {
		final List<ProjectListener> listeners = getProjectListeners();
		for(ProjectListener listener:listeners) {
			listener.projectStructureChanged(pe);
		}
	}

	@Override
	public void fireProjectDataChanged(ProjectEvent pe) {
		final List<ProjectListener> listeners = getProjectListeners();
		for(ProjectListener listener:listeners) {
			listener.projectDataChanged(pe);
		}
	}

	@Override
	public void fireProjectWriteLocksChanged(ProjectEvent pe) {
		final List<ProjectListener> listeners = getProjectListeners();
		for(ProjectListener listener:listeners) {
			listener.projectWriteLocksChanged(pe);
		}
	}

}
