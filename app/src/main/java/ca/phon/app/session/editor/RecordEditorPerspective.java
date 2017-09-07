/*
 * Phon - An open source tool for research in phonology.
 * Copyright (C) 2005 - 2017, Gregory Hedlund <ghedlund@mun.ca> and Yvan Rose <yrose@mun.ca>
 * Dept of Linguistics, Memorial University <https://phon.ca>
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
package ca.phon.app.session.editor;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Logger;

import ca.phon.util.PrefHelper;
import ca.phon.util.resources.*;

/**
 * Perspective for record editor docking views.  This class
 * is just a pointer to the name and location of where the
 * perspective lives.  It also provides methods for managing
 * the list of available perspectives.
 */
public class RecordEditorPerspective {

	private static final Logger LOGGER = Logger
			.getLogger(RecordEditorPerspective.class.getName());

	public final static String DEFAULT_PERSPECTIVE_NAME = "Default";

	public final static String LAST_USED_PERSPECTIVE_NAME = "Previous";

	private final static String PERSPECTIVE_LIST = "META-INF/layouts/layouts.list";

	public final static File PERSPECTIVES_FOLDER = new File(PrefHelper.getUserDataFolder() +
			File.separator + "layouts");

	/**
	 * Resource loader
	 */
	private static ResourceLoader<RecordEditorPerspective> resourceLoader;

	private static ResourceLoader<RecordEditorPerspective> getLoader() {
		if(resourceLoader == null) {
			resourceLoader = new ResourceLoader<RecordEditorPerspective>();
			// add handlers
			resourceLoader.addHandler(new PerspectiveClassLoaderHandler());
			resourceLoader.addHandler(new PerspectiveFolderHandler());
		}
		return resourceLoader;
	}

	/**
	 * Get the list of available perspectives
	 */
	public static RecordEditorPerspective[] availablePerspectives() {
		final List<RecordEditorPerspective> retVal =
				new ArrayList<RecordEditorPerspective>();
		final Iterator<RecordEditorPerspective> itr =
				getLoader().iterator();
		while(itr.hasNext()) {
			retVal.add(itr.next());
		}
		return retVal.toArray(new RecordEditorPerspective[0]);
	}

	/**
	 * Get stock perspectives.
	 *
	 * @return resource loader for stock perspectives
	 */
	public static ResourceLoader<RecordEditorPerspective> getStockPerspectives() {
		final ResourceLoader<RecordEditorPerspective> retVal = new ResourceLoader<>();
		retVal.addHandler(new PerspectiveClassLoaderHandler());
		return retVal;
	}

	/**
	 * Get user perspectives.
	 *
	 * @return resource loader for user perspectives
	 */
	public static ResourceLoader<RecordEditorPerspective> getUserPerspectives() {
		final ResourceLoader<RecordEditorPerspective> retVal = new ResourceLoader<>();
		retVal.addHandler(new PerspectiveFolderHandler());
		return retVal;
	}

	/**
	 * Attempt to delete the given perspective
	 *
	 * @param perspective
	 */
	public static void deletePerspective(RecordEditorPerspective perspective) {
		try {
			final File file = new File(perspective.location.toURI());
			if(file.canWrite()) {
				if(!file.delete()) {
					LOGGER.warning("Could not remove file: " + file.getAbsolutePath());
				}
			}
		} catch (URISyntaxException e) {
		}
	}

	/**
	 * Get the perspective with the given name.
	 *
	 * @param name
	 */
	public static RecordEditorPerspective getPerspective(String name) {
		RecordEditorPerspective retVal = null;

		for(RecordEditorPerspective perspective:availablePerspectives()) {
			if(perspective.getName().equalsIgnoreCase(name)) {
				retVal = perspective;
			}
		}

		return retVal;
	}

	/**
	 * perspective name
	 */
	private String name;

	/**
	 * perspective location
	 */
	private URL location;

	public RecordEditorPerspective() {
		super();
	}

	public RecordEditorPerspective(String name) {
		super();
		this.name = name;
	}

	public RecordEditorPerspective(String name, URL location) {
		super();
		this.name = name;
		this.location = location;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public URL getLocation() {
		return location;
	}

	public void setLocation(URL location) {
		this.location = location;
	}

	/**
	 * Loads perspectives from the classpath.  Each perspective
	 * file should be listed in the file {@link #PERSPECTIVE_LIST}
	 */
	private static class PerspectiveClassLoaderHandler extends ClassLoaderHandler<RecordEditorPerspective> {

		public PerspectiveClassLoaderHandler() {
			super();
			super.loadResourceFile(PERSPECTIVE_LIST);
		}

		@Override
		public RecordEditorPerspective loadFromURL(URL url) throws IOException {
			String name = URLDecoder.decode(url.getPath(), "UTF-8");
			name = name.substring(0, name.lastIndexOf('.'));
			name = name.substring(name.lastIndexOf('/')+1);

			return new RecordEditorPerspective(name, url);
		}

	}

	/**
	 * Scans for .xml files in $USER_PREF_DIR/perspectives.
	 *
	 */
	private static class PerspectiveFolderHandler extends FolderHandler<RecordEditorPerspective> {

		public PerspectiveFolderHandler() {
			super(PERSPECTIVES_FOLDER);
			if(!PERSPECTIVES_FOLDER.exists()) {
				PERSPECTIVES_FOLDER.mkdirs();
			}
			super.setFileFilter(new FileFilter() {

				@Override
				public boolean accept(File pathname) {
					return pathname.getName().endsWith(".xml");
				}
			});
		}

		@Override
		public RecordEditorPerspective loadFromFile(File f) throws IOException {
			final String name = f.getName().substring(0, f.getName().lastIndexOf('.'));
			return new RecordEditorPerspective(name, f.toURI().toURL());
		}

	}
}
