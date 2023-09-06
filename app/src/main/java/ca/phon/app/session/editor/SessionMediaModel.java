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
package ca.phon.app.session.editor;

import ca.phon.app.log.LogUtil;
import ca.phon.app.session.editor.actions.GenerateSessionAudioAction;
import ca.phon.audio.*;
import ca.phon.media.*;
import ca.phon.project.Project;
import ca.phon.session.*;
import ca.phon.ui.CommonModuleFrame;
import ca.phon.ui.nativedialogs.*;
import ca.phon.util.PrefHelper;
import org.apache.commons.io.FilenameUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.beans.*;
import java.io.*;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Media model for a session editor.
 * 
 */
public class SessionMediaModel {
	
	private final SessionEditor editor;
	
	private GenerateSessionAudioAction generateSessionAudioAction;
	
	private File loadedAudioFile = null;
	
	private LongSound sharedAudio;
	
	private SegmentPlayback segmentPlayback;

	/*
	 * Shared volume model for playback
	 */
	private VolumeModel volumeModel;

	public final static float MIN_PLAYBACK_RATE = 0.25f;
	public final static float MAX_PLAYBACK_RATE = 2.0f;
	public final static float DEFAULT_PLAYBACK_RATE = 1.0f;
	private float playbackRate = DEFAULT_PLAYBACK_RATE;

	private final PropertyChangeSupport propSupport = new PropertyChangeSupport(this);

	private enum AudioFileStatus {
		UNKONWN,
		OK,
		ERROR;
	}
	
	public static final String PERFORM_MEDIA_CHECK_PROP = SessionMediaModel.class.getName() + ".performMediaCheck";
	public static final boolean DEFAULT_PERFORM_MEDIA_CHECK = true;
	private boolean performMediaCheck = PrefHelper.getBoolean(PERFORM_MEDIA_CHECK_PROP, DEFAULT_PERFORM_MEDIA_CHECK);
	
	// has the audio file been checked (so it does not cause an application crash)
	private ReentrantLock checkLock = new ReentrantLock();
	private AudioFileStatus audioFileStatus = (performMediaCheck ? AudioFileStatus.UNKONWN : AudioFileStatus.OK);
	
	/**
	 * Editor action key generated when session audio file becomes available
	 * The session audio file should be provided as event data
	 */
	public final static EditorEventType<SessionMediaModel> SessionAudioAvailable =
			new EditorEventType<>("session_audio_available", SessionMediaModel.class);
	
	public SessionMediaModel(SessionEditor editor) {
		super();
		
		this.editor = editor;
		this.volumeModel = new VolumeModel();
		this.volumeModel.addPropertyChangeListener((evt) -> {
			propSupport.firePropertyChange(evt);
		});
	}
	
	public Project getProject() {
		return this.editor.getProject();
	}
	
	public SessionEditor getEditor() {
		return this.editor;
	}
	
	public Session getSession() {
		return this.editor.getSession();
	}

	public VolumeModel getVolumeModel() {
		return this.volumeModel;
	}

	public float getPlaybackRate() {
		return this.playbackRate;
	}

	public void setPlaybackRate(float playbackRate) {
		if(playbackRate < MIN_PLAYBACK_RATE)
			playbackRate = MIN_PLAYBACK_RATE;
		if(playbackRate > MAX_PLAYBACK_RATE)
			playbackRate = MAX_PLAYBACK_RATE;
		var oldVal = this.playbackRate;
		this.playbackRate = playbackRate;
		propSupport.firePropertyChange("playbackRate", oldVal, playbackRate);
	}
	
	public SegmentPlayback getSegmentPlayback() {
		if(segmentPlayback == null) {
			segmentPlayback = new SegmentPlayback(getEditor());
		}
		return segmentPlayback;
	}
	
	/**
	 * Is session media available?
	 * 
	 * @retrun <code>true</code> if the session media field has been set and
	 *  the media file is available
	 */
	public boolean isSessionMediaAvailable() {
		File sessionMediaFile = getSessionMediaFile();
		return sessionMediaFile != null && sessionMediaFile.exists() && sessionMediaFile.canRead();
	}
	
	/**
	 * Return the media location for the session.
	 * 
	 * @return
	 */
	public File getSessionMediaFile() {
		return MediaLocator.findMediaFile(getProject(), getSession());
	}
	
	public void resetAudioCheck() {
		checkLock.lock();
		audioFileStatus = AudioFileStatus.UNKONWN;
		checkLock.unlock();
	}
	
	/**
	 * Check audio file (if exists) to see if we can load it using 
	 * the java sound system.
	 * 
	 * @return
	 */
	public boolean checkAudioFile() {
		if(audioFileStatus == AudioFileStatus.UNKONWN) {
			File audioFile = getSessionAudioFile();
			if(audioFile != null) {
				checkLock.lock();
				boolean fileOk = false;
				String err = "";
				try {
					AudioIO.checkHeaders(audioFile);
					fileOk = true;
				} catch (AudioIOException | IOException e) {
					LogUtil.warning(e);
					err = e.getLocalizedMessage();
				}
				audioFileStatus = (fileOk ? AudioFileStatus.OK : AudioFileStatus.ERROR);
				checkLock.unlock();
				
				if(audioFileStatus != AudioFileStatus.OK) {
					final MessageDialogProperties props = new MessageDialogProperties();
					props.setParentWindow(CommonModuleFrame.getCurrentFrame());
					String[] options = {"Re-encode audio", "Do nothing"};
					props.setOptions(options);
					props.setHeader("Audio File Error");
					props.setMessage("There was an issue reading this audio file." +
					 (err.length() > 0 ? " The reported issue was " + err : " See log for more details."));
					props.setRunAsync(true);
					props.setListener(new NativeDialogListener() {
						
						@Override
						public void nativeDialogEvent(NativeDialogEvent event) {
							if(event.getDialogResult() == 0) {
								SwingUtilities.invokeLater(() -> {
									GenerateSessionAudioAction act = getGenerateSessionAudioAction();
									act.actionPerformed(new ActionEvent(this, -1, "reencode_audio"));
								});
							}
						}
						
					});
					
					NativeDialogs.showMessageDialog(props);
				}
			}
		}
		return (audioFileStatus == AudioFileStatus.OK);
	}
	
	/**
	 * Is there an audio file (wav) available for the session?
	 * 
	 * @return <code>true</code> if session media field has been set and a 
	 *  wav file is present for the media
	 */
	public boolean isSessionAudioAvailable() {
		File sessionAudioFile = getSessionAudioFile();
		return sessionAudioFile != null && sessionAudioFile.exists() && sessionAudioFile.canRead() && checkAudioFile();
	}
	
	/**
	 * Return the audio (wav) file for the session. This may be the same
	 * as the session media file.
	 * 
	 * @return session audio (wav) file
	 */
	public File getSessionAudioFile() {
		if(!isSessionMediaAvailable()) return null;
		
		File selectedMedia = getSessionMediaFile();
		File audioFile = null;

		int lastDot = selectedMedia.getName().lastIndexOf('.');
		String mediaName = selectedMedia.getName();
		if(lastDot >= 0) {
			mediaName = mediaName.substring(0, lastDot);
		}
		if(!selectedMedia.isAbsolute()) selectedMedia =
			MediaLocator.findMediaFile(getSession().getMediaLocation(), getProject(), getSession().getCorpus());

		if(selectedMedia != null) {
			String ext = FilenameUtils.getExtension(selectedMedia.getName());
			
			if(getAudioFileExtensions().contains(ext)) {
				return selectedMedia;
			} else {
				File parentFile = selectedMedia.getParentFile();
	
				boolean foundFile = false;
				for(String ex:getAudioFileExtensions()) {
					audioFile = new File(parentFile, mediaName + "." + ex);
					if(audioFile.exists()) {
						foundFile = true;
						break;
					}
				}
				if(!foundFile)
					audioFile = null;
			}
		}
		return audioFile;
	}
	
	/**
	 * Return a list of valid audio file extensions (excluding the '.')
	 * 
	 * @return list of valid audio file extensions
	 */
	public List<String> getAudioFileExtensions() {
		return List.of("aif", "AIF", "aiff", "AIFF", "aifc", "AIFC", "wav", "WAV");
	}
	
	/**
	 * Return (and load if necessary) the shared session audio 
	 * file instance.
	 * 
	 * @return shared audio file instance
	 * @throws IOException
	 */
	public LongSound getSharedSessionAudio() throws IOException {
		if(this.sharedAudio == null || 
				(this.loadedAudioFile != null
					&& isSessionAudioAvailable() && !this.loadedAudioFile.equals(getSessionAudioFile())) ) {
			this.sharedAudio = loadSessionAudio();
		}
		
		if(this.sharedAudio == null) {
			this.sharedAudio = loadSessionAudio();
		} else {
			if(isSessionAudioAvailable()) {
				if(!this.loadedAudioFile.equals(getSessionAudioFile())) {
					this.sharedAudio = loadSessionAudio();
				}
			} else {
				this.sharedAudio = null;
				this.loadedAudioFile = null;
			}
		}
		
		return this.sharedAudio;
	}
	
	/**
	 * Load session audio file as a {@link LongSound} object
	 * 
	 * @return
	 * @throws IOException
	 */
	public LongSound loadSessionAudio() throws IOException {
		if(isSessionAudioAvailable()) {
			File sessionAudio = getSessionAudioFile();
			LongSound retVal = LongSound.fromFile(sessionAudio, volumeModel);
			this.loadedAudioFile = sessionAudio;
			propSupport.firePropertyChange("sessionAudioLoaded", false, true);
			return retVal;
		} else {
			throw new FileNotFoundException();
		}
	}
	
	/**
	 * Return this shared action for generating session audio.
	 * Shared access is necessary as multiple views will use
	 * this same action and will want to watch for progress
	 * simultaneously.
	 * 
	 * @return shared generate session audio action
	 */
	public GenerateSessionAudioAction getGenerateSessionAudioAction() {
		if(generateSessionAudioAction == null) {
			generateSessionAudioAction = new GenerateSessionAudioAction(editor);
		}
		return generateSessionAudioAction;
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propSupport.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propSupport.removePropertyChangeListener(listener);
	}

	public PropertyChangeListener[] getPropertyChangeListeners() {
		return propSupport.getPropertyChangeListeners();
	}

	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		propSupport.addPropertyChangeListener(propertyName, listener);
	}

	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		propSupport.removePropertyChangeListener(propertyName, listener);
	}

	public PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
		return propSupport.getPropertyChangeListeners(propertyName);
	}

	public boolean hasListeners(String propertyName) {
		return propSupport.hasListeners(propertyName);
	}
}
