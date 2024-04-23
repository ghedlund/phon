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
package ca.phon.app;

import ca.phon.app.hooks.PhonBootHook;
import ca.phon.app.log.LogUtil;
import ca.phon.plugin.*;
import ca.phon.util.OSInfo;

import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * Setup appliation environment using resource files
 * found in the META-INF folder.
 *
 */
@PhonPlugin(name="default", minPhonVersion="1.6.2")
public class BootHook implements IPluginExtensionPoint<PhonBootHook>, PhonBootHook {

	/*
	 * Resource files
	 */
	private final static String PHON_VM_OPTIONS_FILE = "Phon.vmoptions";
	private final static String VM_OPTIONS_FILE = "META-INF/environment/$OS/vmoptions";
	private final static String VM_ENV_FILE = "META-INF/environment/$OS/env";

	private Enumeration<URL> getResourceURLs(String resource) {
		final String os =
				(OSInfo.isWindows() ? "windows" : (OSInfo.isMacOs() ? "mac" : "unix"));
		final String respath =
				resource.replaceAll("\\$OS", os);

		Enumeration<URL> retVal = null;
		try {
			retVal = ClassLoader.getSystemClassLoader().getResources(respath);
		} catch (IOException e) {
			LogUtil.warning(e);
		}
		return retVal;
	}

	@Override
	public void setupVMOptions(List<String> cmd) {
		loadFromFile(cmd, PHON_VM_OPTIONS_FILE);
		loadFromResourcePath(cmd, VM_OPTIONS_FILE);
	}

	private void loadFromFile(List<String> cmd, String path) {
		final File file = new File(path);
		try {
			final FileInputStream fin = new FileInputStream(file);
			loadFromInputStream(cmd, fin);
		} catch (IOException e) {
			LogUtil.warning(e);
		}
	}

	private void loadFromResourcePath(List<String> cmd, String path) {
		final Enumeration<URL> optURLs = getResourceURLs(path);
		while(optURLs.hasMoreElements()) {
			URL url = optURLs.nextElement();
			LogUtil.info("Loading vmoptions from URL " + url.toString());

			try {
				final InputStream is = url.openStream();
				loadFromInputStream(cmd, is);
			} catch (IOException e) {
				LogUtil.warning( e.getLocalizedMessage(), e);
			}
		}
	}

	private void loadFromInputStream(List<String> cmd, InputStream is)
			throws IOException {
		final BufferedReader isr = new BufferedReader(new InputStreamReader(is));
		String vmopt = null;
		while((vmopt = isr.readLine()) != null) {
			if(vmopt.startsWith("#")) continue;
			if(vmopt.startsWith("-Dca.phon.app.PhonSplasher.fork")) {
				cmd.add("-Dca.phon.app.PhonSplasher.isForked=true");
			} else {
				cmd.add(vmopt);
			}
		}
		isr.close();
	}

	@Override
	public void setupEnvironment(Map<String, String> environment) {
		final String libPath = System.getProperty("java.library.path");
		final Enumeration<URL> envURLs = getResourceURLs(VM_ENV_FILE);
		while(envURLs.hasMoreElements()) {
			URL url = envURLs.nextElement();
			LogUtil.info("Loading environment settings from URL " + url.toString());

			try {
				final InputStream is = url.openStream();
				final BufferedReader isr = new BufferedReader(new InputStreamReader(is));
				String envOpt = null;
				while((envOpt = isr.readLine()) != null) {
					String[] opt = envOpt.split("=");
					if(opt.length != 2) continue;
					String key = opt[0];
					String val = opt[1];
					if(key.endsWith("+")) {
						key = key.substring(0, key.length()-1);
						val = environment.get(key) + val;
					}
					environment.put(key, val);
				}
				isr.close();
			} catch (IOException e) {
				LogUtil.severe(e);
			}
		}

		// windows needs libPath include in the PATH var
		if(OSInfo.isWindows()) {
			String path = environment.get("PATH");
			path += ";\"" + libPath + "\"";
			environment.put("PATH", path);
		}
	}

	@Override
	public Class<?> getExtensionType() {
		return PhonBootHook.class;
	}

	@Override
	public IPluginExtensionFactory<PhonBootHook> getFactory() {
		return (args) -> this;
	}

}
