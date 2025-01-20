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
package ca.phon.app.welcome;

import ca.phon.app.log.LogUtil;
import ca.phon.project.Project;
import ca.phon.ui.CommonModuleFrame;
import ca.phon.ui.nativedialogs.*;
import ca.phon.worker.PhonTask;

import java.io.*;
import java.util.*;
import java.util.zip.*;

/** 
 * Task to archive projects.
 */
public class ProjectArchiveTask extends PhonTask {
	
	private Project project;
	
	private boolean includeResources = false;
	
	private boolean includeMedia = false;
	
	private File destFile;
	
	public ProjectArchiveTask(Project project, File destFile) {
		this(project, destFile, true, true);
	}
	
	public ProjectArchiveTask(Project project, File destFile, boolean includeResources, boolean includeMedia) {
		this.project = project;
		this.destFile = destFile;
		this.includeResources = includeResources;
		this.includeMedia = includeMedia;
	}
	
	private List<File> buildFileList() {
		List<File> retVal = new ArrayList<File>();

		// TODO add .phonproj files from root folder


		final Iterator<String> corpusIterator = project.getCorpusIterator();
		while(corpusIterator.hasNext()) {
			String corpus = corpusIterator.next();
			File corpusDir = new File(project.getCorpusPath(corpus));
			if(corpusDir.exists()) {
				final Iterator<String> sessionIterator = project.getSessionIterator(corpus);
				while(sessionIterator.hasNext()) {
					String session = sessionIterator.next();
					File sessionFile = new File(project.getSessionPath(corpus, session));
					if(sessionFile.exists()) {
						retVal.add(sessionFile);
					}
				}
			}
		}
		
		if(includeResources) {
			File resDir = new File(project.getResourceLocation());
			if(resDir.exists()) {
				for(File resFile:resDir.listFiles()) {
					if(resFile.isHidden()) continue;
					if(resFile.isDirectory()) {
						boolean includeFolder = true;
						if(resFile.getName().equals("media")) includeFolder = includeMedia;
						if(includeFolder)
							retVal.addAll(listFilesRecursive(resFile));
					} else {
						retVal.add(resFile);
					}
				}
			}
		}
		
		return retVal;
	}
	
	private List<File> listFilesRecursive(File f) {
		List<File> retVal = new ArrayList<File>();
		
		if(f.isDirectory()) {
			for(File sf:f.listFiles()) {
				if(sf.isHidden()) continue;
				
				if(sf.isFile()) {
					retVal.add(sf);
				} else {
					List<File> subVals = listFilesRecursive(sf);
					retVal.addAll(subVals);
				}
			}
		} else {
			retVal.add(f);
		}
		
		return retVal;
	}
	
	@Override
	public void performTask() {
		super.setStatus(TaskStatus.RUNNING);
		
		super.setProperty(STATUS_PROP, "Writing to file " + destFile.getAbsolutePath());
		if(destFile.exists()) {
			// print warning and delete file
			LogUtil.info("Overwriting file '" + destFile.getAbsolutePath() + "'");
			if(!destFile.delete()) {
				LogUtil.warning("Could not delete file '" + destFile.getAbsolutePath() + "'");
			}
		}
		
		try {
			FileOutputStream fos = new FileOutputStream(destFile);
			ZipOutputStream zos = new ZipOutputStream(fos);
			
			byte[] buffer = new byte[1024];
			int rlen = 0;
			
			// create a zip entry for each project file
			File projectRoot = new File(project.getLocation());
			
			super.setProperty(STATUS_PROP, "Building file list");
			List<File> projectFiles = buildFileList();
			for(File projectFile:projectFiles) {
				
				if(isShutdown()) {
					zos.flush();
					zos.close();
					
					setProperty(STATUS_PROP, "User terminated");
					
					return;
				}
				
				// setup zip entry name
				int projectPathLen = projectRoot.getPath().length();
				String entryName =
					project.getName() + 
					projectFile.getPath().substring(projectPathLen);
				
				if(File.separatorChar != '/') {
					entryName = entryName.replace(File.separatorChar, '/');
				}
				
				ZipEntry entry = new ZipEntry(entryName);
				zos.putNextEntry(entry);
				

				setProperty(STATUS_PROP, "Archiving: " + entryName);
				
				FileInputStream fis = new FileInputStream(projectFile);
				rlen = 0;
				while((rlen = fis.read(buffer)) > 0) {
					zos.write(buffer, 0, rlen);
				}
				fis.close();
				
				zos.closeEntry();
			}
			
			setProperty(STATUS_PROP, "Flushing data");
			zos.flush();
			zos.close();
			
		} catch (IOException e) {
			LogUtil.warning(e);
			super.err = e;
			super.setStatus(TaskStatus.ERROR);
			return;
		}
		
		String msg1 = "Archive created";
		String msg2 = "Archive of " + project.getName() + " created at " + 
			destFile.getAbsolutePath();
		NativeDialogs.showMessageDialog(CommonModuleFrame.getCurrentFrame(), new NativeDialogListener() {
			
			@Override
			public void nativeDialogEvent(NativeDialogEvent event) {
				// TODO Auto-generated method stub
				
			}
		}, null, msg1, msg2);
		
		super.setProperty(STATUS_PROP, "Finished");
		super.setStatus(TaskStatus.FINISHED);
	}
	
}