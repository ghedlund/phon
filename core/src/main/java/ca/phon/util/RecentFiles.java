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
package ca.phon.util;

import java.io.File;
import java.util.*;

public class RecentFiles implements Iterable<File> {
	
	private String propertyKey;
	
	private final static int DEFAULT_MAX_FILES = 10;
	
	private int maxFiles = 10;

	private boolean removeDeadEntries = false;
	
	private List<File> fileHistory = new ArrayList<>();
	
	public RecentFiles() {
		super();
	}
	
	public RecentFiles(String propertyKey) {
		this(propertyKey, DEFAULT_MAX_FILES);
	}

	public RecentFiles(String propertyKey, int maxFiles) {
		this(propertyKey, maxFiles, false);
	}

	public RecentFiles(String propertyKey, int maxFiles, boolean removeDeadEntries) {
		super();
		setRemoveDeadEntries(removeDeadEntries);
		setMaxFiles(maxFiles);
		setPropertyKey(propertyKey);
	}
	
	public void setMaxFiles(int maxFiles) {
		this.maxFiles = Math.max(0, maxFiles);
	}
	
	public int getMaxFiles() {
		return this.maxFiles;
	}
	
	public int getFileCount() {
		return fileHistory.size();
	}
	
	public File getFileAt(int idx) {
		return fileHistory.get(idx);
	}

	public boolean isRemoveDeadEntries() {
		return this.removeDeadEntries;
	}

	public void setRemoveDeadEntries(boolean removeDeadEntries) {
		this.removeDeadEntries = removeDeadEntries;
	}
	
	/**
	 * Remove any files which cannot be located from the
	 * history.
	 * 
	 */
	public void purgeFilesNotFound() {
		final Iterator<File> itr = iterator();
		while(itr.hasNext()) {
			final File f = itr.next();
			if(!f.exists())
				itr.remove();
		}
	}
	
	public void setPropertyKey(String key) {
		this.propertyKey = key;
		loadHistory();
	}
	
	public String getPropertyKey() {
		return this.propertyKey;
	}
	
	private void loadHistory() {
		String folderList = PrefHelper.get(propertyKey, "");
		final String folders[] = folderList.split(";");
		
		for(String folderName:folders) {
			if(folderName.trim().length() == 0) continue;
			final File folder = new File(folderName);
			fileHistory.add(0, folder);
		}

		if(isRemoveDeadEntries())
			purgeFilesNotFound();
	}
	
	public void saveHistory() {
		final StringBuffer sb = new StringBuffer();
		
		int num = 0;
		for(int i = fileHistory.size()-1; i >= 0 && num++ < maxFiles; i--) {
			final File projectFolder = fileHistory.get(i);
			if(i < (fileHistory.size()-1)) sb.append(';');
			sb.append(projectFolder.getAbsolutePath());
		}
		
		PrefHelper.getUserPreferences().put(propertyKey, sb.toString());
	}
	
	public void addToHistory(File workspaceFolder) {
		if(fileHistory.contains(workspaceFolder))
			fileHistory.remove(workspaceFolder);
		fileHistory.add(0, workspaceFolder);
		while(fileHistory.size() > getMaxFiles()) {
			fileHistory.remove(fileHistory.size()-1);
		}
		saveHistory();
	}
	
	public void clearHistory() {
		fileHistory.clear();
		saveHistory();
	}

	public void removeFromHistory(File f) {
		Iterator<File> itr = fileHistory.iterator();
		while(itr.hasNext()) {
			File tf = itr.next();
			if(tf.equals(f)) {
				itr.remove();
				break;
			}
		}
		saveHistory();
	}
	
	public int size() {
		return this.fileHistory.size();
	}

	@Override
	public Iterator<File> iterator() {
		return fileHistory.iterator();
	}
}
