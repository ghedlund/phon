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
package ca.phon.app.session.editor.actions;

import ca.phon.app.log.LogUtil;
import ca.phon.app.session.editor.*;
import ca.phon.audio.AudioFileType;
import ca.phon.media.*;
import ca.phon.session.Record;
import ca.phon.session.*;
import ca.phon.session.position.SegmentCalculator;
import ca.phon.ui.nativedialogs.FileFilter;
import ca.phon.ui.nativedialogs.*;
import ca.phon.util.icons.*;
import ca.phon.worker.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ExportSegmentAction extends SessionEditorAction {

	public enum SegmentType {
		CURRENT_RECORD,
		SPEAKER_TURN,
		CONVERSATION_PERIOD,
		CUSTOM
	};
	
	private SegmentType segmentType = SegmentType.CUSTOM;
	
	private long startTime = -1L;
	
	private long endTime = -1L;
	
	private String outputPath = null;

	private final FileFilter wavFilter = FileFilter.wavFilter;
	private final FileFilter aifFilter = 
			new FileFilter("Aif files (*.aif;*.aiff;*.aifc)", "aif;aiff;aifc");
	
	public ExportSegmentAction(SessionEditor editor) {
		this(editor, SegmentType.CURRENT_RECORD);
		
		putValue(Action.NAME, "Export segment...");
		putValue(Action.SHORT_DESCRIPTION, "Export segment for current record");
		putValue(Action.SMALL_ICON, IconManager.getInstance().getIcon("actions/document-save-as", IconSize.SMALL));
	}
	
	public ExportSegmentAction(SessionEditor editor, SegmentType segmentType) {
		super(editor);
		
		this.segmentType = segmentType;
		putValue(Action.NAME, "Export segment...");
		putValue(Action.SHORT_DESCRIPTION, "Export segment for current record");
		putValue(Action.SMALL_ICON, IconManager.getInstance().getIcon("actions/document-save-as", IconSize.SMALL));
	}
	
	/**
	 * 
	 * @param editor
	 * @param startTime in s
	 * @param endTime in s
	 */
	public ExportSegmentAction(SessionEditor editor, float startTime, float endTime) {
		this(editor, startTime, endTime, null);
	}
	
	public ExportSegmentAction(SessionEditor editor, float startTime, float endTime, String outputPath) {
		this(editor, Float.valueOf(startTime * 1000.0f).longValue(), Float.valueOf(endTime * 1000.0f).longValue(), outputPath);
	}
	
	/**
	 * 
	 * @param editor
	 * @param startTime in ms
	 * @param endTime in ms
	 */
	public ExportSegmentAction(SessionEditor editor, long startTime, long endTime) {
		this(editor, startTime, endTime, null);
	}
	
	public ExportSegmentAction(SessionEditor editor, long startTime, long endTime, String outputPath) {
		super(editor);
		
		this.segmentType = SegmentType.CUSTOM;
		this.startTime = startTime;
		this.endTime = endTime;
		this.outputPath = outputPath;
	}
	
	private MediaSegment getMediaSegment() {
		if(segmentType == SegmentType.CURRENT_RECORD) {
			Record r = getEditor().currentRecord();
			return (r != null ? r.getMediaSegment() : null);
		} else if(segmentType == SegmentType.SPEAKER_TURN) {
			return SegmentCalculator.contiguousSegment(getEditor().getSession(), getEditor().getCurrentRecordIndex());
		} else if(segmentType == SegmentType.CONVERSATION_PERIOD) {
			return SegmentCalculator.conversationPeriod(getEditor().getSession(), getEditor().getCurrentRecordIndex());
		} else if(segmentType == SegmentType.CUSTOM) {
			if(startTime < 0 || endTime < 0 || endTime - startTime <= 0) {
				CustomSegmentDialog dlg = new CustomSegmentDialog(getEditor());
				dlg.pack();
				
				dlg.setLocationRelativeTo(getEditor());
				
				dlg.setModal(true);
				dlg.setVisible(true);

				return dlg.getSegment();
			} else {
				MediaSegment retVal = SessionFactory.newFactory().createMediaSegment();
				retVal.setStartValue(startTime);
				retVal.setEndValue(endTime);
				
				return retVal;
			}
		} else {
			return null;
		}
	}

	public String getOutputPath() {
		return outputPath;
	}

	public void setOutputPath(String outputPath) {
		this.outputPath = outputPath;
	}
	
	private ExportSegment getExportSegment() throws IOException {
		SessionMediaModel mediaModel = getEditor().getMediaModel();
		if(!mediaModel.isSessionAudioAvailable()) return null;
		
		LongSound sharedSound = mediaModel.getSharedSessionAudio();
		if(sharedSound == null) return null;
		
		ExportSegment exportSegment = sharedSound.getExtension(ExportSegment.class);
		return exportSegment;
	}
	
	private FileFilter getFileFilter() {
		try {
			ExportSegment exportSegment = getExportSegment();
			if(exportSegment != null) {
				if(exportSegment.getFileType() == AudioFileType.WAV) {
					return wavFilter;
				} else if(exportSegment.getFileType() == AudioFileType.AIFC
						|| exportSegment.getFileType() == AudioFileType.AIFF) {
					return aifFilter;
				} else {
					FileFilter filter = new FileFilter(exportSegment.getFileType().getName(), 
							Arrays.asList(exportSegment.getFileType().getExtensions()).stream().collect(Collectors.joining(";")));
					return filter;
				}
			}
		} catch (IOException e) {}
		return wavFilter;
	}

	@Override
	public void hookableActionPerformed(ActionEvent ae) {
		if(outputPath == null) {
			SaveDialogProperties saveProps = new SaveDialogProperties();
			saveProps.setParentWindow(getEditor());
			saveProps.setCanCreateDirectories(true);
			saveProps.setFileFilter(getFileFilter());
			saveProps.setMessage("Export audio");
			saveProps.setTitle("Export audio segment");
			saveProps.setRunAsync(true);
			saveProps.setListener(saveListener);
			saveProps.setPrompt("Export");
			
			NativeDialogs.showSaveDialog(saveProps);
		} else {
			doExportTask();
		}
	}
	
	private void doExportTask() {
		ExportTask task = new ExportTask();
		PhonWorker.getInstance().invokeLater(task);
		getEditor().getStatusBar().watchTask(task);
	}
	
	private NativeDialogListener saveListener = new NativeDialogListener() {
		
		@Override
		public void nativeDialogEvent(NativeDialogEvent event) {
			if(event.getDialogData() != null) {
				outputPath = event.getDialogData().toString();
				doExportTask();
			}
		}
	};
	
	private class ExportTask extends PhonTask {

		@Override
		public void performTask() {
			setStatus(TaskStatus.RUNNING);
			
			final File outputFile = new File(outputPath);
			
			MediaSegment mediaSegment = getMediaSegment();
			if(mediaSegment != null) {
				try {					
					ExportSegment exportSegment = getExportSegment();
					if(exportSegment == null) throw new IOException("Export segment extension not found");
					exportSegment.exportSegment(outputFile, mediaSegment.getStartValue() / 1000.0f, mediaSegment.getEndValue() / 1000.0f);
					
					LogUtil.info("Export segment complete: " + outputFile + " " + outputFile.length() + "B");
					SwingUtilities.invokeLater( () -> {
						getEditor().showOkDialog("Export segment", "Export segment complete: " + outputFile + " " + outputFile.length() + "B");
					});
				} catch (IOException e) {
					LogUtil.severe(e);
					super.err = e;
					setStatus(TaskStatus.ERROR);
					SwingUtilities.invokeLater( () -> {
						getEditor().showOkDialog("Export segment failed", "Export segment failed: " + e.getLocalizedMessage());
					});
					return;
				}
			}
		
			setStatus(TaskStatus.FINISHED);
		}
		
	}
	
}
