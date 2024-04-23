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
package ca.phon.app.project.git;

import ca.phon.project.Project;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.PushResult;

import java.io.*;
import java.util.function.Consumer;

/**
 *
 */
public class ProjectGitController {
	
	private final static String GIT_FOLDER = ".git";
	
	private final Project project;
	
	private Git git = null;
	
	public ProjectGitController(Project project) {
		super();
		this.project = project;
	}
	
	public void closeRepository() {
		if(git != null)
			git.close();
	}
	
	public Project getProject() {
		return this.project;
	}
	
	/**
	 * Get the folder for the git repository.
	 * 
	 * @return folder for git repository
	 */
	public File getRepositoryFolder() {
		return new File(getProject().getLocation(), GIT_FOLDER);
	}
	
	/**
	 * Is the project under git control
	 * 
	 * @return <code>true</code> if the .git folder exists, false otherwise
	 */
	public boolean hasGitFolder() {
		return getRepositoryFolder().exists();
	}
	
	/**
	 * Setup a new git repository for the project.
	 * 
	 * @return the new git instance
	 * @throws GitAPIException 
	 * @throws IllegalStateException 
	 */
	public Git init() throws IOException, IllegalStateException, GitAPIException {
		git = Git.init().setDirectory(new File(getProject().getLocation())).call();
		return git;
	}
	
	public void setupDefaultGitIgnore() throws IOException {
		final StringBuilder sb = new StringBuilder();
		sb.append("__autosave").append("\n");
		sb.append("__res/media").append("\n");
		sb.append("backups.zip").append("\n");
		final File gitIgnore = new File(getProject().getLocation(), ".gitignore");
		
		// don't overwrite an existing file
		if(!gitIgnore.exists()) {
			final BufferedWriter out = 
					new BufferedWriter(
							new OutputStreamWriter(new FileOutputStream(gitIgnore), "UTF-8"));
			out.write(sb.toString());
			out.flush();
			out.close();
		}
	}
	
	/**
	 * Open existing repository for project
	 * 
	 * @return git instance
	 * @throws IOException 
	 */
	public Git open() throws IOException {
		final FileRepositoryBuilder builder = new FileRepositoryBuilder();
		final Repository repo = builder.setGitDir(getRepositoryFolder())
				.readEnvironment().findGitDir().build();
		git = new Git(repo);
		return git;
	}
	
	public boolean isOpen() {
		return git != null;
	}
	
	/**
	 * Get status of repository.
	 * 
	 * @return repository status
	 * @throws GitAPIException 
	 * @throws NoWorkTreeException 
	 */
	public Status status() throws NoWorkTreeException, GitAPIException {
		if(git == null)
			throw new NoWorkTreeException();
		return git.status().call();
	}
	
	public Status status(String path) throws NoWorkTreeException, GitAPIException {
		if(git == null)
			throw new NoWorkTreeException();
		return git.status().addPath(path).call();
	}
	
	public Status statis(String ... paths) throws NoWorkTreeException, GitAPIException {
		if(git == null)
			throw new NoWorkTreeException();
		StatusCommand status = git.status();
		for(String path:paths) status.addPath(path);
		return status.call();
	}
	
	public void printStatus(Writer writer) throws IOException, GitAPIException {
		final Status status = status();
		final StringBuffer sb = new StringBuffer();
		
		Consumer<String> appendToBuffer = 
				(s) -> { sb.append("\t").append(s).append("\n"); };
		if(status.hasUncommittedChanges()) {
			if(status.getUntracked().size() > 0) {
				sb.append("Untracked files:\n");
				status.getUntracked().forEach( appendToBuffer );
			}
			if(status.getUntrackedFolders().size() > 0) {
				sb.append("Untracked folders:\n");
				status.getUntrackedFolders().forEach(appendToBuffer);
			}
			if(status.getAdded().size() > 0) {
				sb.append("Added:\n");
				status.getAdded().forEach(appendToBuffer);
			}
			if(status.getMissing().size() > 0) {
				sb.append("Missing:\n");
				status.getMissing().forEach(appendToBuffer);
			}
			if(status.getChanged().size() > 0) {
				sb.append("Changed:\n");
				status.getChanged().forEach(appendToBuffer);
			}
			if(status.getModified().size() > 0) {
				sb.append("Modified:\n");
				status.getModified().forEach(appendToBuffer);
			}
			if(status.getConflicting().size() > 0) {
				sb.append("Conflicting:\n");
				status.getConflicting().forEach(appendToBuffer);
			}
		} else {
			sb.append("Everything up-to-date\n");
		}
		writer.write(sb.toString());
		writer.flush();
	}
	
	/**
	 * Add files to current index.
	 * 
	 * @param filepattern
	 * @throws GitAPIException 
	 * @throws NoFilepatternException 
	 */
	public DirCache addToIndex(String filepattern) throws NoFilepatternException, GitAPIException {
		return git.add().addFilepattern(filepattern).call();
	}
	
	/**
	 * Commit changes to repository.
	 * 
	 * @param message
	 * @throws GitAPIException 
	 * @throws AbortedByHookException 
	 * @throws WrongRepositoryStateException 
	 * @throws ConcurrentRefUpdateException 
	 * @throws UnmergedPathsException 
	 * @throws NoMessageException 
	 * @throws NoHeadException 
	 * 
	 */
	public RevCommit commit(String message) throws NoHeadException,
		NoMessageException, UnmergedPathsException,
		ConcurrentRefUpdateException, WrongRepositoryStateException, 
		AbortedByHookException, GitAPIException {
		return git.commit().setMessage(message).call();
	}
	
	/**
	 * Commit all changes to repository.
	 * 
	 * @param message
	 * @return commit revision
	 * 
	 */
	public RevCommit commitAllChanges(String message) throws NoHeadException,
		NoMessageException, UnmergedPathsException,
		ConcurrentRefUpdateException, WrongRepositoryStateException, 
		AbortedByHookException, GitAPIException {
		return git.commit().setAll(true).setMessage(message).call();
	}
	
	/**
	 * Pull from repository.  This is the same as calling 'git pull'
	 * without args.
	 * 
	 * @return pull result
	 * @throws GitAPIException 
	 * @throws TransportException 
	 * @throws NoHeadException 
	 * @throws RefNotAdvertisedException 
	 * @throws RefNotFoundException 
	 * @throws CanceledException 
	 * @throws InvalidRemoteException 
	 * @throws DetachedHeadException 
	 * @throws InvalidConfigurationException 
	 * @throws WrongRepositoryStateException 
	 */
	public PullResult pull() throws WrongRepositoryStateException, InvalidConfigurationException, DetachedHeadException, InvalidRemoteException, CanceledException, RefNotFoundException, RefNotAdvertisedException, NoHeadException, TransportException, GitAPIException {
		return git.pull().call();
	}
	
	/**
	 * Pull from repository.  This is the same as calling 'git pull'
	 * without args.
	 * 
	 * @param monitor
	 * @return pull result
	 * @throws GitAPIException 
	 * @throws TransportException 
	 * @throws NoHeadException 
	 * @throws RefNotAdvertisedException 
	 * @throws RefNotFoundException 
	 * @throws CanceledException 
	 * @throws InvalidRemoteException 
	 * @throws DetachedHeadException 
	 * @throws InvalidConfigurationException 
	 * @throws WrongRepositoryStateException 
	 */
	public PullResult pull(ProgressMonitor monitor) throws WrongRepositoryStateException, InvalidConfigurationException, DetachedHeadException, InvalidRemoteException, CanceledException, RefNotFoundException, RefNotAdvertisedException, NoHeadException, TransportException, GitAPIException {
		return git.pull().setProgressMonitor(monitor).call();
	}
	
	/**
	 * Pull from repository
	 * 
	 * @param remote
	 * @param branch
	 * @param monitor
	 * @throws GitAPIException 
	 * @throws TransportException 
	 * @throws NoHeadException 
	 * @throws RefNotAdvertisedException 
	 * @throws RefNotFoundException 
	 * @throws CanceledException 
	 * @throws InvalidRemoteException 
	 * @throws DetachedHeadException 
	 * @throws InvalidConfigurationException 
	 * @throws WrongRepositoryStateException 
	 */
	public PullResult pull(String remote, String branch, ProgressMonitor monitor) 
			throws WrongRepositoryStateException, InvalidConfigurationException, 
			DetachedHeadException, InvalidRemoteException, CanceledException,
			RefNotFoundException, RefNotAdvertisedException, NoHeadException,
			TransportException, GitAPIException {
		return git.pull()
				.setRemote(remote)
				.setRemoteBranchName(branch)
				.setProgressMonitor(monitor)
				.call();
	}
	
	/**
	 * Push changes
	 * 
	 * @param remote
	 * @param monitor
	 * @throws GitAPIException 
	 * @throws TransportException 
	 * @throws InvalidRemoteException 
	 * 
	 */
	public Iterable<PushResult> push(String remote, ProgressMonitor monitor) 
			throws InvalidRemoteException, TransportException, GitAPIException {
		return git.push()
				.setRemote(remote)
				.setProgressMonitor(monitor)
				.call();
	}
	
	public Iterable<PushResult> push(ProgressMonitor monitor) 
			throws InvalidRemoteException, TransportException, GitAPIException {
		return git.push()
				.setProgressMonitor(monitor)
				.call();
	}
	
}
