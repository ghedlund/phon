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
package ca.phon.app.log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import ca.phon.util.OSInfo;
import ca.phon.util.PrefHelper;

/**
 * Method for setup and management of application logs.
 */
public class LogManager {

	/**
	 * Log manager shared instance
	 */
	private final static LogManager _instance = new LogManager();
	
	public final static String PROPERTIES_FILE_LOCATION = LogManager.class.getName() + ".logProps";
	
	private final static String DEFAULT_PROPERTIES_FILE = 
			"ca/phon/app/log/phonlog.properties";
	
	public final static String LOG_FILE = 
			PrefHelper.getUserDataFolder() + File.separator + "phon0.log";
	
	public final static String PREV_LOG_FILE =
			PrefHelper.getUserDataFolder() + File.separator + "phon1.log";
		
	public static LogManager getInstance() {
		return _instance;
	}
	
	private LogManager() {
	}
	
	private InputStream getLogProps() {
		return getClass().getClassLoader().getResourceAsStream(
				PrefHelper.get(PROPERTIES_FILE_LOCATION, DEFAULT_PROPERTIES_FILE));
	}
	
	private String getLogFilenamePattern() {
		String userHomePath = System.getProperty("user.home");
		
		String retVal = "";
		if(OSInfo.isMacOs()) {
			retVal = 
				"%h" + File.separator + "Library" + File.separator + "Application Support" + File.separator + "Phon";
		} else if(OSInfo.isWindows()) {
			retVal = System.getenv("APPDATA") + File.separator + "Phon";
			if(retVal.startsWith(userHomePath)) {
				retVal = "%h/" + retVal.substring(userHomePath.length()+1);
			}
			retVal = retVal.replace("\\", "/");
		} else {
			retVal = "%h" + File.separator + ".phon";
		}	
	
		String pattern = retVal + "/phon%g.log";
		return pattern;
	}
	
	public void setupLogging() {
		//final java.util.logging.LogManager manager = java.util.logging.LogManager.getLogManager();
		
		// create temporary properties configuration file
		// with updated pattern for FileHandler log location
//		try {
//			final File tempFile = File.createTempFile("phon", "logprops");
//			
//			final PrintWriter fout = new PrintWriter(new FileOutputStream(tempFile));
//			final BufferedReader reader = new BufferedReader(new InputStreamReader(getLogProps()));
//			
//			String line = null;
//			while((line = reader.readLine()) != null) {
//				fout.write(line);
//				fout.write("\n");
//			}
//			
//			fout.write("java.util.logging.FileHandler.pattern=" + getLogFilenamePattern() + "\n");
//			fout.flush();
//			
//			fout.close();
//			reader.close();
//			
//			manager.readConfiguration(new FileInputStream(tempFile));
//			tempFile.deleteOnExit();
//		} catch (IOException | SecurityException e) {
//			e.printStackTrace();
//		}
	}
	
	public void shutdownLogging() {
//		final java.util.logging.LogManager manager = java.util.logging.LogManager.getLogManager();
//		manager.reset();
	}
	
	public String readLogFile() throws IOException {
		return readFile(LOG_FILE);
	}
	
	public String readPreviousLogFile() throws IOException {
		return readFile(PREV_LOG_FILE);
	}
	
	private String readFile(String filename) throws IOException {
		final StringBuffer buffer = new StringBuffer();
		
		final BufferedReader reader = 
				new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
		String line = null;
		while((line = reader.readLine()) != null) {
			buffer.append(line).append('\n');
		}
		reader.close();
		
		return buffer.toString();
	}
	
}
