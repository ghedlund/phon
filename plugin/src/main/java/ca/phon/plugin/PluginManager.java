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
package ca.phon.plugin;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Reads the plugin def xml files and provides methods
 * for adding plugin defined menu entries to window menus.
 *
 */
public class PluginManager extends URLClassLoader {
	
	private final static org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger(PluginManager.class.getName());
	
	/**
	 * Plugin folder
	 */
	public final static String PLUGIN_FOLDER = "plugins";
	
	/**
	 * Singleton instance
	 */
	private static PluginManager _instance;
	
	private List<ClassLoader> classloaders = new ArrayList<>();
	
	private ClassLoader selfProvider = new ClassLoader() {

		@Override
		public URL getResource(String name) {
			return PluginManager.this.getResource(name, false);
		}

		@Override
		public Enumeration<URL> getResources(String name) throws IOException {
			return PluginManager.this.getResources(name, false);
		}

		@Override
		public InputStream getResourceAsStream(String name) {
			return PluginManager.this.getResourceAsStream(name, false);
		}
		
	};
	
	/**
	 * Get singleton instance
	 */
	public static PluginManager getInstance() {
		if(_instance == null) 
			_instance = new PluginManager(new URL[0]);
		return _instance;
	}
	
	/**
	 * Constructor
	 */
	private PluginManager(URL[] urls) {
		super(urls);
		classloaders.add(this);
		if(getClass().getClassLoader() != ClassLoader.getSystemClassLoader())
			classloaders.add(getClass().getClassLoader());
	}
	
	public void addClassLoader(ClassLoader classloader) {
		classloaders.add(classloader);
	}
	
	public void removeClassLoader(ClassLoader classloader) {
		classloaders.remove(classloader);
	}
	
	/**
	 * Tell the plugin manager to scan plugins folder.
	 * 
	 */
	public void scanPluginFolder() {
		scanPluginFolder(new File(PLUGIN_FOLDER));
	}
	
	public void scanPluginFolder(File pluginFolder) {
		scanFolderForJars(pluginFolder, true);
	}
	
	/**
	 * Add a zip/jar file to the dynamic class loader
	 * 
	 * @param path
	 * @throws MalformedURLException
	 */
	public void addFile (String path) 
		throws MalformedURLException {
		addURL( (new File(path)).toURI().toURL() );
	}
	
	public void addURL (URL url) {
		super.addURL(url);
	}
	
	private void scanFolderForJars(File dir, boolean recursive) {
		if(dir.isDirectory()) {
			for(File f:dir.listFiles()) {
				if(f.isDirectory() && recursive) {
					scanFolderForJars(f, recursive);
				} else if(f.isFile()) {
					if(f.getName().endsWith(".jar") ||
							f.getName().endsWith(".zip")) {
						try {
							LOGGER.info("Adding archive to dynamic class loader: " + f.getAbsolutePath());
							addFile(f.getAbsolutePath());
							
						} catch (MalformedURLException e) {
							LOGGER.error("Could not add archive to dynamic class loader: " + f.getAbsolutePath());
						}
					}
				}
			}
		}
	}
	
	@Override
	public URL getResource(String name) {
		return getResource(name, true);
	}
	
	public URL getResource(String name, boolean deep) {
		URL retVal = null;
		if(!deep) {
			retVal = super.getResource(name);
		} else {
			for(ClassLoader cl:classloaders) {
				retVal = (cl == this ? super.getResource(name) : cl.getResource(name));
				if(retVal != null) break;
			}
		}
		return retVal;
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		return getResources(name, true);
	}
	
	public Enumeration<URL> getResources(String name, boolean deep) throws IOException {
		List<URL> urls = new ArrayList<>();
		if(!deep) {
			return super.getResources(name);
		} else {
			for(ClassLoader cl:classloaders) {
				Enumeration<URL> e = (cl == this ? super.getResources(name) : cl.getResources(name));
				while(e.hasMoreElements()) urls.add(e.nextElement());
			}
		}
		return Collections.enumeration(urls);
	}

	@Override
	public InputStream getResourceAsStream(String name) {
		return getResourceAsStream(name, true);
	}
	
	public InputStream getResourceAsStream(String name, boolean deep) {
		InputStream retVal = null;
		if(!deep) {
			retVal = super.getResourceAsStream(name);
		} else {
			for(ClassLoader cl:classloaders) {
				retVal = (cl == this ? super.getResourceAsStream(name) : cl.getResourceAsStream(name));
				if(retVal != null) break;
			}
		}
		return retVal;
	}

	/**
	 * Get entry point names
	 * 
	 * @return list of entry point ids
	 */
	public List<String> getEntryPoints() {
		List<String> epIds = new ArrayList<String>();
		
		for(ClassLoader cl:classloaders) {
			ServiceLoader<IPluginEntryPoint> entryPointProvider = 
				ServiceLoader.load(IPluginEntryPoint.class, (cl == this ? selfProvider : cl));
			Iterator<IPluginEntryPoint> epItr = entryPointProvider.iterator();
			while(epItr.hasNext()) {
				IPluginEntryPoint pluginEp = epItr.next();
				
				try {
					checkVersionInfo(pluginEp.getClass());
					String epId = pluginEp.getName();
					
					epIds.add(epId);
				} catch (PluginException e) {
					LOGGER.warn(e.getMessage());
					e.printStackTrace();
				}
			}
		}
		
		Collections.sort(epIds);
		return epIds;
	}
	
	/**
	 * Get the entry point identified by the
	 * given plugin id.
	 * 
	 * @param name the name of the entry point
	 * to retrieve
	 * @return the plugin entry point or NULL if
	 * not found
	 */
	public IPluginEntryPoint getEntryPoint(String name) {
		IPluginEntryPoint retVal = null;
		
		for(ClassLoader cl:classloaders) {
			ServiceLoader<IPluginEntryPoint> entryPointProvider = 
				ServiceLoader.load(IPluginEntryPoint.class, (cl == this ? selfProvider : cl));
			Iterator<IPluginEntryPoint> epItr = entryPointProvider.iterator();
			while(epItr.hasNext()) {
				IPluginEntryPoint pluginEp = epItr.next();
				
				try {
					checkVersionInfo(pluginEp.getClass());
					String epId = pluginEp.getName();
					
					if(epId.equals(name)) {
						retVal = pluginEp;
						break;
					}
				} catch (PluginException e) {
					LOGGER.warn(e.getMessage());
					e.printStackTrace();
				}
			}
		}
		
		return retVal;
	}
		
	/**
	 * Get initilized extension points for the given class.
	 * This method should only be used when there is a default
	 * constructor available for the given class.
	 * 
	 * @param clazz 
	 * 
	 * @throws PluginException if there is a problem with
	 *  creating the list of objects.
	 */
	public <T> List<T> getExtensions(Class<T> clazz) 
		throws PluginException {
		final List<T> retVal = new ArrayList<T>();
		
		final List<IPluginExtensionPoint<T>> extPts = 
				getExtensionPoints(clazz);
		for(IPluginExtensionPoint<T> extPt:extPts) {
			final IPluginExtensionFactory<T> factory = extPt.getFactory();
			try {
				final T obj = factory.createObject();
				retVal.add(obj);
			} catch (Exception e) {
				throw new PluginException(e);
			}
		}
		
		return retVal;
	}
	
	/**
	 * Get the extension points for the given class.
	 * 
	 * @param clazz the class for extensions
	 * @return the list of loaded extension points for the
	 * given class
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T> List<IPluginExtensionPoint<T>> getExtensionPoints(Class<T> clazz) {
		List<IPluginExtensionPoint<T>> retVal = 
			new ArrayList<IPluginExtensionPoint<T>>();
		
		// load extension point from specific files - preferred method!
		retVal.addAll(loadExtensionPoints(clazz));
		
		for(ClassLoader cl:classloaders) {
			// deprecated method, supported while things get converted in plug-ins
			ServiceLoader<IPluginExtensionPoint> extPtProvider = 
				ServiceLoader.load(IPluginExtensionPoint.class, (cl == this ? selfProvider : cl));
			Iterator<IPluginExtensionPoint> extPtItr = extPtProvider.iterator();
			
			while(extPtItr.hasNext()) {
				IPluginExtensionPoint<?> extPt = 
					(IPluginExtensionPoint<?>)extPtItr.next();
				
				try {
					checkVersionInfo(extPt.getClass());
					Class<?> extPtClass = extPt.getExtensionType();
					
					if(extPtClass == clazz) {
						retVal.add((IPluginExtensionPoint<T>)extPt);
					}
				} catch (PluginException e) {
					LOGGER.warn(e.getMessage());
					e.printStackTrace();
				}
			}
		}
		
		// sort extensions
		Collections.sort(retVal, (c1, c2) -> {
			Rank c1Rank = c1.getClass().getAnnotation(Rank.class);
			Rank c2Rank = c2.getClass().getAnnotation(Rank.class);
			
			int c1Val = (c1Rank != null ? c1Rank.value() : 1);
			int c2Val = (c2Rank != null ? c2Rank.value() : 1);
			
			return Integer.valueOf(c1Val).compareTo(c2Val);
		});
		
		return retVal;
	}
	
	/**
	 * Load extension point implementations for the given class.
	 * The extension point class list is located at
	 * META-INF/extpts/<classname>
	 * 
	 * This is the preferred method of defining extension point
	 * implementations, the {@link ServiceLoader} method is still
	 * supported but deprecated.
	 */
	@SuppressWarnings("unchecked")
	private <T> List<IPluginExtensionPoint<T>> loadExtensionPoints(Class<T> clazz) {
		List<IPluginExtensionPoint<T>> retVal = 
				new ArrayList<IPluginExtensionPoint<T>>();
		
		final String extPtFile = "META-INF/extpts/" + clazz.getName();
		
		for(ClassLoader cl:classloaders) {
			try {
				final Enumeration<URL> urlList = (cl == this ? super.getResources(extPtFile) : cl.getResources(extPtFile));
				while(urlList.hasMoreElements()) {
					final URL url = urlList.nextElement();
					final InputStream in = url.openStream();
					final BufferedReader reader = new BufferedReader(new InputStreamReader(in));
					String line = null;
					while((line = reader.readLine()) != null) {
						if(line.startsWith("#") || line.trim().length() == 0) continue;
						
						final String extPtName = line.trim();
						try {
							final Class<?> extPtClazz = Class.forName(extPtName, true, cl);
							
							// make sure we have an extension point
							if(IPluginExtensionPoint.class.isAssignableFrom(extPtClazz)) {
								try {
									final IPluginExtensionPoint<?> extPt = 
											IPluginExtensionPoint.class.cast(extPtClazz.newInstance());
									
									if(extPt.getExtensionType().isAssignableFrom(clazz)) {
										retVal.add((IPluginExtensionPoint<T>)extPt);
									}
								} catch (InstantiationException e) {
									LOGGER.error(
											e.getLocalizedMessage(), e);
								} catch (IllegalAccessException e) {
									LOGGER.error(
											e.getLocalizedMessage(), e);
								}
							}
						} catch (ClassNotFoundException e) {
							LOGGER.error(
									e.getLocalizedMessage(), e);
						}
					}
				}
			} catch (IOException e) {
				LOGGER.error( e.getLocalizedMessage(), e);
			}
		}
		
		return retVal;
	}
	
	/**
	 * Checks the PhonPlugin annotation of the given
	 * class to ensure minimum version of Phon is met.
	 * 
	 * @param clazz
	 * @throws PhonException if the given class does
	 * not have a PhonPlugin annotation or the version 
	 * information does not match
	 */
	private void checkVersionInfo(Class<?> clazz)
		throws PluginException {
		// TODO fix me
//		// check for the PhonPlugin annotation
//		PhonPlugin pluginAnnotation = clazz.getAnnotation(PhonPlugin.class);
//		if(pluginAnnotation == null) {
//			throw new PluginException("[PluginManager] Class " + 
//					clazz.getName() + " must declare the @PhonPlugin annotation.");
//		}
//		
//		String minVersion = pluginAnnotation.minPhonVersion();
//		String currentVersion = VersionInfo.getInstance().getVersion();
//		
//		if(currentVersion.compareTo(minVersion) < 0) {
//			throw new PluginException("[PluginManager] Class " + 
//					clazz.getName() + " requires Phon version " + minVersion + 
//					". Current version is " + currentVersion + ".");
//		}
	}
}
