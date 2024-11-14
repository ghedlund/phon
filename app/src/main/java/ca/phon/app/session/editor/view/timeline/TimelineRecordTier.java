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
package ca.phon.app.session.editor.view.timeline;

import ca.phon.app.log.LogUtil;
import ca.phon.app.query.QueryAndReportWizard;
import ca.phon.app.session.RecordsTransferable;
import ca.phon.app.session.editor.*;
import ca.phon.app.session.editor.actions.*;
import ca.phon.app.session.editor.undo.*;
import ca.phon.app.session.editor.view.mediaPlayer.MediaPlayerEditorView;
import ca.phon.app.session.editor.actions.NewParticipantAction;
import ca.phon.app.session.editor.view.timeline.RecordGrid.GhostMarker;
import ca.phon.app.session.editor.view.timeline.actions.*;
import ca.phon.formatter.MsFormatter;
import ca.phon.media.TimeUIModel;
import ca.phon.media.TimeUIModel.*;
import ca.phon.query.db.ResultSet;
import ca.phon.query.script.*;
import ca.phon.session.Record;
import ca.phon.session.*;
import ca.phon.ui.FlatButton;
import ca.phon.ui.IconStrip;
import ca.phon.ui.action.*;
import ca.phon.ui.fonts.FontPreferences;
import ca.phon.ui.layout.ButtonBarBuilder;
import ca.phon.ui.menu.MenuBuilder;
import ca.phon.ui.text.FormatterTextField;
import ca.phon.util.*;
import ca.phon.util.icons.*;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.beans.*;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class TimelineRecordTier extends TimelineTier implements ClipboardOwner {

	private RecordGrid recordGrid;

	private Map<String, Boolean> tierVisibility = new HashMap<>();

	private TimeUIModel.Interval currentRecordInterval = null;

	private SplitMarker splitMarker = null;
	
	private JButton splitButton;
	private JButton cancelSplitButton;
	private JButton acceptSplitButton;

	private JButton moveSegmentsButton;

	public final static String FONT_SIZE_DELTA_PROP = TimelineRecordTier.class.getName() + ".fontSizeDelta";
	public final static float DEFAULT_FONT_SIZE_DELTA = 0.0f;
	public float fontSizeDelta = PrefHelper.getFloat(FONT_SIZE_DELTA_PROP, DEFAULT_FONT_SIZE_DELTA);

	public TimelineRecordTier(TimelineView parent) {
		super(parent);

		init();
		addToolbarButtons();
		setupEditorEvents();
	}

	private void init() {
		Session session = getParentView().getEditor().getSession();
		recordGrid = new RecordGrid(getTimeModel(), session);
		if (getParentView().getEditor().currentRecord() != null) {
			recordGrid.getSelectionModel().setSelectionInterval(getParentView().getEditor().getCurrentRecordIndex(), getParentView().getEditor().getCurrentRecordIndex());
			setupRecord(getParentView().getEditor().currentRecord());
		}
		recordGrid.addParticipantMenuHandler(this::setupSpeakerContextMenu);

		recordGrid.addPropertyChangeListener("splitMode", e -> {
			if (!((boolean) e.getNewValue())) {
				endSplitMode(recordGrid.isSplitModeAccept());
			}
		});
		// add listener for changes in selected record which occur from the record grid
		recordGrid.addPropertyChangeListener("currentRecordIndex", e -> {
			int newIdx = (int) e.getNewValue();
			if(newIdx != getParentView().getEditor().getCurrentRecordIndex()) {
				getParentView().getEditor().setCurrentRecordIndex(newIdx);
			}
		});
		recordGrid.addFocusListener(selectionFocusListener);

		recordGrid.setFont(FontPreferences.getTierFont());
		setupRecordGridActions();
		setupSpeakers();

		recordGrid.setFontSizeDelta(fontSizeDelta);
		recordGrid.addPropertyChangeListener("fontSizeDelta", e -> {
			this.fontSizeDelta = recordGrid.getFontSizeDelta();
			PrefHelper.getUserPreferences().putFloat(FONT_SIZE_DELTA_PROP, this.fontSizeDelta);
		});

		// add ortho by default
		tierVisibility.put(SystemTierType.Orthography.getName(), Boolean.TRUE);
		tierVisibility.put(SystemTierType.Segment.getName(), Boolean.TRUE);
		setupTiers();

		recordGrid.addRecordGridMouseListener(mouseListener);

		setLayout(new BorderLayout());
		add(recordGrid, BorderLayout.CENTER);
	}

	private void addToolbarButtons() {
		IconStrip toolbar = getParentView().getToolbar();
		
		toolbar.addSeparator(IconStrip.IconStripPosition.LEFT);
		
		SplitRecordAction splitAct = new SplitRecordAction(getParentView());
		splitAct.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
		splitAct.putValue(FlatButton.ICON_NAME_PROP, "split_scene");
		splitAct.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);
		splitButton = new FlatButton(splitAct);
		toolbar.add(splitButton, IconStrip.IconStripPosition.LEFT);
		splitButton.addActionListener( (e) -> {
			splitButton.setVisible(false);
			cancelSplitButton.setVisible(true);
			acceptSplitButton.setVisible(true);
		});
		
		final PhonUIAction<Boolean> endSplitModeAct = PhonUIAction.eventConsumer(this::onEndSplitRecord, false);
		endSplitModeAct.putValue(PhonUIAction.NAME, "Exit split record");
		endSplitModeAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Exit split record mode without accepting split");
		endSplitModeAct.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
		endSplitModeAct.putValue(FlatButton.ICON_NAME_PROP, "close");
		endSplitModeAct.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);
		cancelSplitButton = new FlatButton(endSplitModeAct);
		cancelSplitButton.setVisible(false);
		toolbar.add(cancelSplitButton, IconStrip.IconStripPosition.LEFT);
		
		final PhonUIAction<Boolean> acceptSplitAct = PhonUIAction.eventConsumer(this::onEndSplitRecord, true);
		acceptSplitAct.putValue(PhonUIAction.NAME, "Accept record split");
		acceptSplitAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "");
		acceptSplitAct.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
		acceptSplitAct.putValue(FlatButton.ICON_NAME_PROP, "check");
		acceptSplitAct.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);
		acceptSplitButton = new FlatButton(acceptSplitAct);
		acceptSplitButton.setVisible(false);
		toolbar.add(acceptSplitButton, IconStrip.IconStripPosition.LEFT);

		toolbar.addSeparator(IconStrip.IconStripPosition.LEFT);

		final PhonUIAction<Void> moveSegmentsAct = PhonUIAction.eventConsumer(this::onMoveSegments);
		moveSegmentsAct.putValue(PhonUIAction.NAME, "Move records");
		moveSegmentsAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Move selected records a specified amount of time");
		moveSegmentsAct.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
		moveSegmentsAct.putValue(FlatButton.ICON_NAME_PROP, "arrows_outward");
		moveSegmentsAct.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);
		moveSegmentsButton = new FlatButton(moveSegmentsAct);
		toolbar.add(moveSegmentsButton, IconStrip.IconStripPosition.LEFT);

		addComponentListener(new ComponentListener() {
			@Override
			public void componentResized(ComponentEvent e) {

			}

			@Override
			public void componentMoved(ComponentEvent e) {

			}

			@Override
			public void componentShown(ComponentEvent e) {
				if(splitButton != null) splitButton.setEnabled(true);
				if(moveSegmentsButton != null) moveSegmentsButton.setEnabled(true);
			}

			@Override
			public void componentHidden(ComponentEvent e) {
				if(splitButton != null) splitButton.setEnabled(false);
				if(moveSegmentsButton != null) moveSegmentsButton.setEnabled(false);
			}
		});
	}

	public RecordGrid getRecordGrid() {
		return this.recordGrid;
	}

	public ListSelectionModel getSelectionModel() {
		return recordGrid.getSelectionModel();
	}

	public Interval currentRecordInterval() {
		return this.currentRecordInterval;
	}

	private void setupRecordGridActions() {
		final InputMap inputMap = recordGrid.getInputMap();
		final ActionMap actionMap = recordGrid.getActionMap();

		final String selectAllKey = "select_all";
		final PhonUIAction<Void> selectAllAct = PhonUIAction.eventConsumer(this::onSelectAll);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), selectAllKey);
		actionMap.put(selectAllKey, selectAllAct);

		final String escapeKey = "escape";
		final PhonUIAction<Boolean> escapeAction = PhonUIAction.eventConsumer(this::onEscape, false);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), escapeKey);
		actionMap.put(escapeKey, escapeAction);

		final String deleteRecordKey = "delete_record";
		final DeleteRecordsAction deleteRecordAction = new DeleteRecordsAction(getParentView());
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), deleteRecordKey);
		actionMap.put(deleteRecordKey, deleteRecordAction);

		final String playSegmentKey = "play_segment";
		final PlaySegmentAction playSegmentAction = new PlaySegmentAction(getParentView().getEditor());
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), playSegmentKey);
		actionMap.put(playSegmentKey, playSegmentAction);

		final String moveRight = "move_segments_right";
		final PhonUIAction<Integer> moveRightAct = PhonUIAction.eventConsumer(this::onMoveSegmentsRight, 5);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, KeyEvent.CTRL_DOWN_MASK), moveRight);
		actionMap.put(moveRight, moveRightAct);

		final String moveRightSlow = "move_segments_right_slow";
		final PhonUIAction<Integer> moveRightSlowAct = PhonUIAction.eventConsumer(this::onMoveSegmentsRight, 1);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK), moveRightSlow);
		actionMap.put(moveRightSlow, moveRightSlowAct);

		final String moveLeft = "move_segments_left";
		final PhonUIAction<Integer> moveLeftAct = PhonUIAction.eventConsumer(this::onMoveSegmentsLeft, 5);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, KeyEvent.CTRL_DOWN_MASK), moveLeft);
		actionMap.put(moveLeft, moveLeftAct);

		final String moveLeftSlow = "move_segments_left_slow";
		final PhonUIAction<Integer> moveLeftSlowAct = PhonUIAction.eventConsumer(this::onMoveSegmentsLeft, 1);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK), moveLeftSlow);
		actionMap.put(moveLeftSlow, moveLeftSlowAct);

		final String move = "move_segments";
		final PhonUIAction<Void> moveAct = PhonUIAction.eventConsumer(this::onMoveSegments);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, KeyEvent.CTRL_DOWN_MASK), move);
		actionMap.put(move, moveAct);

		final String growSegments = "grow_segments";
		final PhonUIAction<Integer> growSegmentsAct = PhonUIAction.eventConsumer(this::onGrowSegments, 3);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.CTRL_DOWN_MASK ), growSegments);
		actionMap.put(growSegments, growSegmentsAct);

		final String growSegmentsSlow = "grow_segments_slow";
		final PhonUIAction<Integer> growSegmentsSlowAct = PhonUIAction.eventConsumer(this::onGrowSegments, 1);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK), growSegmentsSlow);
		actionMap.put(growSegmentsSlow, growSegmentsSlowAct);

		final String shrinkSegments = "shrink_segments";
		final PhonUIAction<Integer> shrinkSegmentsAct = PhonUIAction.eventConsumer(this::onShrinkSegments, 3);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_K, KeyEvent.CTRL_DOWN_MASK ), shrinkSegments);
		actionMap.put(shrinkSegments, shrinkSegmentsAct);

		final String shrinkSegmentsSlow = "shrink_segments_slow";
		final PhonUIAction<Integer> shrinkSegmentsSlowAct = PhonUIAction.eventConsumer(this::onShrinkSegments, 1);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_K, KeyEvent.CTRL_DOWN_MASK |KeyEvent.SHIFT_DOWN_MASK), shrinkSegmentsSlow);
		actionMap.put(shrinkSegmentsSlow, shrinkSegmentsSlowAct);

		final PhonUIAction<Void> copyRecordsAct = PhonUIAction.runnable(this::copy);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "copy");
		actionMap.put("copy", copyRecordsAct);

		final PhonUIAction<Void> pasteRecordsAct = PhonUIAction.eventConsumer(this::paste);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "paste");
		actionMap.put("paste", pasteRecordsAct);

		final PhonUIAction<Void> cutRecordsAct = PhonUIAction.eventConsumer(this::cut);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "cut");
		actionMap.put("cut", cutRecordsAct);

		for (int i = 0; i < 10; i++) {
			final PhonUIAction<Integer> chSpeakerAct = PhonUIAction.consumer(this::onChangeSpeakerByIndex, i);
			KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_0 + i,
					Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
			chSpeakerAct.putValue(PhonUIAction.ACCELERATOR_KEY, ks);
			String id = "change_speaker_" + i;

			actionMap.put(id, chSpeakerAct);
			inputMap.put(ks, id);
		}

		// split record
		final String splitRecordId = "split_record";
		final SplitRecordAction splitRecordAct = new SplitRecordAction(getParentView());
		final KeyStroke splitRecordKs = KeyStroke.getKeyStroke(KeyEvent.VK_S, 0);
		inputMap.put(splitRecordKs, splitRecordId);
		actionMap.put(splitRecordId, splitRecordAct);

		final String acceptSplitId = "accept_split_record";
		final PhonUIAction<Boolean> acceptSplitRecordAct = PhonUIAction.eventConsumer(this::onEndSplitRecord, true);
		final KeyStroke acceptSplitRecordKs = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
		inputMap.put(acceptSplitRecordKs, acceptSplitId);
		actionMap.put(acceptSplitId, acceptSplitRecordAct);

		recordGrid.setInputMap(WHEN_FOCUSED, inputMap);
		recordGrid.setActionMap(actionMap);
	}

	public void onChangeSpeakerByIndex(Integer speakerIdx) {
		if (speakerIdx != 0 && (speakerIdx - 1) >= recordGrid.getSpeakers().size()) {
			Toolkit.getDefaultToolkit().beep();
			return;
		}
		var speaker = (speakerIdx == 0 ? Participant.UNKNOWN : recordGrid.getSpeakers().get(speakerIdx - 1));

		getParentView().getEditor().getUndoSupport().beginUpdate();
		for(int recordIndex:getSelectionModel().getSelectedIndices()) {
			final ChangeSpeakerEdit edit = new ChangeSpeakerEdit(getParentView().getEditor(),
					getParentView().getEditor().getSession().getRecord(recordIndex), speaker);
			getParentView().getEditor().getUndoSupport().postEdit(edit);
		}
		getParentView().getEditor().getUndoSupport().endUpdate();
	}

	public void onMoveSegmentsRight(PhonActionEvent<Integer> pae) {
		int amount = Integer.parseInt(pae.getData().toString());
		float secondsPerPixel = getTimeModel().timeAtX(getTimeModel().getTimeInsets().left+1);
		float secondsToAdd = amount * secondsPerPixel;

		getParentView().getEditor().getUndoSupport().beginUpdate();
		for(int recordIdx:getSelectionModel().getSelectedIndices()) {
			Record r = getParentView().getEditor().getSession().getRecord(recordIdx);

			MediaSegment recordSeg = r.getMediaSegment();
			MediaSegment seg = SessionFactory.newFactory().createMediaSegment();
			float startValue = recordSeg.getStartValue() + (1000.0f * secondsToAdd);
			float endValue = recordSeg.getEndValue() + (1000.0f * secondsToAdd);
			if(endValue/1000.0f <= getTimeModel().getEndTime()) {
				seg.setStartValue(startValue);
				seg.setEndValue(endValue);

				final RecordSegmentEdit changeSeg = new RecordSegmentEdit(getParentView().getEditor(), r, seg);
				changeSeg.setFireHardChangeOnUndo(true);
				getParentView().getEditor().getUndoSupport().postEdit(changeSeg);
			}
		}
		getParentView().getEditor().getUndoSupport().endUpdate();
		recordGrid.repaint(recordGrid.getVisibleRect());
	}

	public void onGrowSegments(PhonActionEvent<Integer> pae) {
		int amount = Integer.parseInt(pae.getData().toString());
		float secondsPerPixel = getTimeModel().timeAtX(getTimeModel().getTimeInsets().left+1);
		float secondsToAdd = amount * secondsPerPixel;

		getParentView().getEditor().getUndoSupport().beginUpdate();
		for(int recordIdx:getSelectionModel().getSelectedIndices()) {
			Record r = getParentView().getEditor().getSession().getRecord(recordIdx);

			MediaSegment recordSeg = r.getMediaSegment();
			MediaSegment seg = SessionFactory.newFactory().createMediaSegment();
			float startValue =  Math.max(0, recordSeg.getStartTime() - secondsToAdd);
			float endValue = Math.min(getTimeModel().getEndTime(), recordSeg.getEndTime() + secondsToAdd);

			seg.setStartTime(startValue);
			seg.setEndTime(endValue);

			final RecordSegmentEdit changeSeg = new RecordSegmentEdit(getParentView().getEditor(), r, seg);
			changeSeg.setFireHardChangeOnUndo(true);
			getParentView().getEditor().getUndoSupport().postEdit(changeSeg);
		}
		getParentView().getEditor().getUndoSupport().endUpdate();
		recordGrid.repaint(recordGrid.getVisibleRect());
	}

	public void onShrinkSegments(PhonActionEvent<Integer> pae) {
		int amount = Integer.parseInt(pae.getData().toString());
		float secondsPerPixel = getTimeModel().timeAtX(getTimeModel().getTimeInsets().left+1);
		float secondsToSubtract = amount * secondsPerPixel;

		getParentView().getEditor().getUndoSupport().beginUpdate();
		for(int recordIdx:getSelectionModel().getSelectedIndices()) {
			Record r = getParentView().getEditor().getSession().getRecord(recordIdx);

			MediaSegment recordSeg = r.getMediaSegment();
			MediaSegment seg = SessionFactory.newFactory().createMediaSegment();
			float startValue = recordSeg.getStartValue() + (1000.0f * secondsToSubtract);
			float endValue = recordSeg.getEndValue() - (1000.0f * secondsToSubtract);

			if(startValue <= endValue) {
				seg.setStartValue(startValue);
				seg.setEndValue(endValue);

				final RecordSegmentEdit changeSeg = new RecordSegmentEdit(getParentView().getEditor(), r, seg);
				changeSeg.setFireHardChangeOnUndo(true);
				getParentView().getEditor().getUndoSupport().postEdit(changeSeg);
			}
		}
		getParentView().getEditor().getUndoSupport().endUpdate();
		recordGrid.repaint(recordGrid.getVisibleRect());
	}

	public void onMoveSegmentsLeft(PhonActionEvent<Integer> pae) {
		int amount = Integer.parseInt(pae.getData().toString());
		float secondsPerPixel = getTimeModel().timeAtX(getTimeModel().getTimeInsets().left+1);
		float secondsToAdd = amount * secondsPerPixel;

		getParentView().getEditor().getUndoSupport().beginUpdate();
		for(int recordIdx:getSelectionModel().getSelectedIndices()) {
			Record r = getParentView().getEditor().getSession().getRecord(recordIdx);

			MediaSegment recordSeg = r.getMediaSegment();
			MediaSegment seg = SessionFactory.newFactory().createMediaSegment();
			float startValue =  recordSeg.getStartValue() - (1000.0f * secondsToAdd);
			float endValue = recordSeg.getEndValue() - (1000.0f * secondsToAdd);

			if(startValue >= 0) {
				seg.setStartValue(startValue);
				seg.setEndValue(endValue);

				final RecordSegmentEdit changeSeg = new RecordSegmentEdit(getParentView().getEditor(), r, seg);
				changeSeg.setFireHardChangeOnUndo(true);
				getParentView().getEditor().getUndoSupport().postEdit(changeSeg);
			}
		}
		getParentView().getEditor().getUndoSupport().endUpdate();
		recordGrid.repaint(recordGrid.getVisibleRect());
	}

	public void onEscape(PhonActionEvent<Boolean> pae) {
		if (isSplitModeActive()) {
			onEndSplitRecord(pae);
		} else if (getParentView().getEditor().getViewModel().isShowing(MediaPlayerEditorView.VIEW_NAME)) {
			SegmentPlayback segmentPlayback = getParentView().getEditor().getMediaModel().getSegmentPlayback();
			if(segmentPlayback != null && segmentPlayback.isPlaying()) {
				segmentPlayback.stopPlaying();
			}
		}
		if(getSelectionModel().getSelectedItemsCount() > 1) {
			// reset record selection
			getSelectionModel().setSelectionInterval(getParentView().getEditor().getCurrentRecordIndex(),
					getParentView().getEditor().getCurrentRecordIndex());
			recordGrid.repaint(recordGrid.getVisibleRect());
		}
	}

	public void onSelectSpeaker(PhonActionEvent<Participant> pae) {
		Participant speaker = pae.getData();
		if(speaker == null) return;

		List<Integer> participantRecords = new ArrayList<>();
		for(int rIdx = 0; rIdx < getParentView().getEditor().getSession().getRecordCount(); rIdx++) {
			Record r = getParentView().getEditor().getSession().getRecord(rIdx);
			if(r.getSpeaker() == speaker) {
				participantRecords.add(rIdx);
			}
		}

		if(participantRecords.size() == 0) return;

		getSelectionModel().clearSelection();
		for(int recordIndex:participantRecords) getSelectionModel().addSelectionInterval(recordIndex, recordIndex);
		if(!participantRecords.contains(recordGrid.getCurrentRecordIndex())) {
			recordGrid.setCurrentRecordIndex(participantRecords.get(0));
		}
	}

	public void onSelectResultRecords(PhonActionEvent<ResultSet> pae) {
		ResultSet rs = pae.getData();
		Set<Integer> recordSet = new LinkedHashSet<>();
		for(int i = 0; i < rs.numberOfResults(false); i++) {
			recordSet.add(rs.getResult(i).getRecordIndex());
		}

		getSelectionModel().clearSelection();
		for(int recordIndex:recordSet) getSelectionModel().addSelectionInterval(recordIndex, recordIndex);
		if(recordSet.size() > 0 && !recordSet.contains(recordGrid.getCurrentRecordIndex()))
			recordGrid.setCurrentRecordIndex(recordSet.iterator().next());
	}

	public void onSelectAll(PhonActionEvent<Void> pae) {
		List<Integer> visibleRecords = new ArrayList<>();
		List<Participant> visibleSpeakers = getSpeakerList();
		if(isSpeakerVisible(Participant.UNKNOWN))
			visibleSpeakers.add(Participant.UNKNOWN);
		for(int i = 0; i < getParentView().getEditor().getSession().getRecordCount(); i++) {
			if(visibleSpeakers.contains(getParentView().getEditor().getSession().getRecord(i).getSpeaker())) {
				visibleRecords.add(i);
			}
		}
		if(visibleRecords.size() == 0) return;

		if(getSelectionModel().getSelectedItemsCount() == visibleRecords.size()) {
			getSelectionModel().setSelectionInterval(getParentView().getEditor().getCurrentRecordIndex(),
					getParentView().getEditor().getCurrentRecordIndex());
		} else {
			for(int visibleRecord:visibleRecords) {
				getSelectionModel().addSelectionInterval(visibleRecord, visibleRecord);
			}
		}
		recordGrid.repaint(recordGrid.getVisibleRect());
	}

	private JFrame timeSelectionPopup;
	/**
	 * Display a UI for moving records a specific amount of time
	 *
	 * @param pae
	 */
	public void onMoveSegments(PhonActionEvent<Void> pae) {
		final FormatterTextField<Long> msField = new FormatterTextField<>(new MsFormatter());
		msField.setPrompt("###:##.###");
		msField.setToolTipText("Enter number, may use ###:##.### format. Negative values allowed.");

		String lblText = """
				<html><p>Enter time in ms (###:##.### format allowed)<br/>Negative values move records left</p></html>
				""";
		JLabel lbl = new JLabel(lblText);

		JPanel movePanel = new JPanel(new BorderLayout());
		movePanel.add(lbl, BorderLayout.NORTH);
		movePanel.add(msField, BorderLayout.CENTER);

		final PhonUIAction<FormatterTextField<Long>> okAct = PhonUIAction.eventConsumer(this::onConfirmMoveSegments, msField);
		okAct.putValue(PhonUIAction.NAME, "Ok");
		okAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Move segments given amount of time");
		JButton okBtn = new JButton(okAct);
		msField.setAction(okAct);

		final PhonUIAction<Void> cancelAct = PhonUIAction.eventConsumer(this::onCancelMoveSegments);
		cancelAct.putValue(PhonUIAction.NAME, "Cancel");
		cancelAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Cancel move segments");
		JButton cancelBtn = new JButton(cancelAct);

		msField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
		msField.getActionMap().put("cancel", cancelAct);

		JComponent btnPanel = ButtonBarBuilder.buildOkCancelBar(okBtn, cancelBtn);
		movePanel.add(btnPanel, BorderLayout.SOUTH);

		timeSelectionPopup = new JFrame();
		timeSelectionPopup.setUndecorated(true);
		timeSelectionPopup.addWindowFocusListener(new WindowFocusListener() {

			@Override
			public void windowGainedFocus(WindowEvent we) {
			}

			@Override
			public void windowLostFocus(WindowEvent we) {
				if (timeSelectionPopup != null) {
					timeSelectionPopup.setVisible(false);
					timeSelectionPopup = null;
				}
			}
		});
		timeSelectionPopup.getContentPane().add(movePanel);
		timeSelectionPopup.getRootPane().setDefaultButton(okBtn);

		timeSelectionPopup.pack();
		Point p = moveSegmentsButton.getLocation();
		SwingUtilities.convertPointToScreen(p, moveSegmentsButton.getParent());
		// setup bounds
		Rectangle windowBounds = new Rectangle(
				p.x,
				p.y + moveSegmentsButton.getHeight(),
				timeSelectionPopup.getPreferredSize().width,
				timeSelectionPopup.getPreferredSize().height);
		timeSelectionPopup.setBounds(windowBounds);
		timeSelectionPopup.setVisible(true);
	}

	public void onConfirmMoveSegments(PhonActionEvent<FormatterTextField<Long>> pae) {
		FormatterTextField<Long> msField = pae.getData();
		Long confirmedValue = msField.getValue();
		if(confirmedValue == null) {
			Toolkit.getDefaultToolkit().beep();
			return;
		}
		float offset = confirmedValue.floatValue() / 1000.0f;

		boolean firstChange = true;
		getParentView().getEditor().getUndoSupport().beginUpdate();
		for(int recordIndex:getSelectionModel().getSelectedIndices()) {
			Record r = getParentView().getEditor().getSession().getRecord(recordIndex);
			MediaSegment seg = r.getMediaSegment();
			float segLength = seg.getLength();
			MediaSegment newSeg = SessionFactory.newFactory().createMediaSegment();

			if (offset > 0) {
				newSeg.setEndTime(Math.min(getTimeModel().getEndTime(), seg.getEndTime()+offset));
				newSeg.setStartTime(newSeg.getEndTime()-segLength);
			} else if (offset < 0) {
				newSeg.setStartTime(Math.max(0, seg.getStartTime() + offset));
				newSeg.setEndTime(newSeg.getStartTime() + segLength);
			}

			final RecordSegmentEdit segEdit = new RecordSegmentEdit(getParentView().getEditor(), r, newSeg);
			segEdit.setFireHardChangeOnUndo(firstChange);
			getParentView().getEditor().getUndoSupport().postEdit(segEdit);
			firstChange = false;
		}
		getParentView().getEditor().getUndoSupport().endUpdate();

		EditorEvent<EditorEventType.RecordChangedData> ee = new EditorEvent(EditorEventType.RecordRefresh, this,
				new EditorEventType.RecordChangedData(getParentView().getEditor().currentRecord(),
						getParentView().getEditor().getSession().getRecordElementIndex(getParentView().getEditor().currentRecord()),
						getParentView().getEditor().getCurrentRecordIndex()));
		getParentView().getEditor().getEventManager().queueEvent(ee);

		onCancelMoveSegments(new PhonActionEvent<>(pae.getActionEvent()));
	}

	public void onCancelMoveSegments(PhonActionEvent<Void> pae) {
		timeSelectionPopup.setVisible(false);
		timeSelectionPopup = null;
	}

	private void setupEditorEvents() {
		getParentView().getEditor().getEventManager().registerActionForEvent(EditorEventType.RecordChanged, this::onRecordChange, EditorEventManager.RunOn.AWTEventDispatchThread);
		getParentView().getEditor().getEventManager().registerActionForEvent(EditorEventType.SpeakerChanged, this::onSpeakerChange, EditorEventManager.RunOn.AWTEventDispatchThread);
		getParentView().getEditor().getEventManager().registerActionForEvent(EditorEventType.TierChange, this::onTierChanged, EditorEventManager.RunOn.AWTEventDispatchThread);
		getParentView().getEditor().getEventManager().registerActionForEvent(EditorEventType.TierViewChanged, this::onTierViewChanged, EditorEventManager.RunOn.AWTEventDispatchThread);
		getParentView().getEditor().getEventManager().registerActionForEvent(EditorEventType.ParticipantRemoved, this::onParticipantRemoved, EditorEventManager.RunOn.AWTEventDispatchThread);
		getParentView().getEditor().getEventManager().registerActionForEvent(EditorEventType.ParticipantAdded, this::onParticipantAdded, EditorEventManager.RunOn.AWTEventDispatchThread);
		getParentView().getEditor().getEventManager().registerActionForEvent(EditorEventType.RecordDeleted, this::onRecordDeleted, EditorEventManager.RunOn.AWTEventDispatchThread);
	}

	private void deregisterEditorEvents() {
		getParentView().getEditor().getEventManager().removeActionForEvent(EditorEventType.RecordChanged, this::onRecordChange);
		getParentView().getEditor().getEventManager().removeActionForEvent(EditorEventType.SpeakerChanged, this::onSpeakerChange);
		getParentView().getEditor().getEventManager().removeActionForEvent(EditorEventType.TierChange, this::onTierChanged);
		getParentView().getEditor().getEventManager().removeActionForEvent(EditorEventType.TierViewChanged, this::onTierViewChanged);
		getParentView().getEditor().getEventManager().removeActionForEvent(EditorEventType.ParticipantRemoved, this::onParticipantRemoved);
		getParentView().getEditor().getEventManager().removeActionForEvent(EditorEventType.ParticipantAdded, this::onParticipantAdded);
		getParentView().getEditor().getEventManager().removeActionForEvent(EditorEventType.RecordDeleted, this::onRecordDeleted);
	}

	private void updateCurrentRecordInterval(Record r) {
		if(r == null) return;

		boolean wasVisible = true;
		if(currentRecordInterval != null) {
			wasVisible = currentRecordInterval.isVisible();
			getTimeModel().removeInterval(currentRecordInterval);
		}

		int rIdx = getParentView().getEditor().getCurrentRecordIndex();

		MediaSegment segment = r.getMediaSegment();
		var segStartTime = segment.getStartTime();
		var segEndTime = segment.getEndTime();

		// check for 'GhostMarker's which, if present, will
		// become the start/end marker for the record interval
		Optional<RecordGrid.GhostMarker> ghostMarker = recordGrid.getTimeModel().getMarkers().parallelStream()
				.filter((m) -> m instanceof GhostMarker).map((m) -> GhostMarker.class.cast(m)).findAny();
		if (ghostMarker.isPresent() && recordGrid.getUI().getCurrentlyDraggedMarker() == ghostMarker.get()) {
			Marker startMarker = ghostMarker.get().isStart() ? ghostMarker.get() : new Marker(segStartTime);
			Marker endMarker = ghostMarker.get().isStart() ? new Marker(segEndTime) : ghostMarker.get();
			currentRecordInterval = getTimeModel().addInterval(startMarker, endMarker);
			currentRecordInterval.setRepaintEntireInterval(true);
			currentRecordInterval.addPropertyChangeListener(new RecordIntervalListener());
			recordGrid.getUI().beginDrag(currentRecordInterval, ghostMarker.get());
		} else {
			currentRecordInterval = getTimeModel().addInterval(segStartTime, segEndTime);
			currentRecordInterval.setRepaintEntireInterval(true);
			currentRecordInterval.addPropertyChangeListener(new RecordIntervalListener());
		}
		currentRecordInterval.getStartMarker()
				.setColor(UIManager.getColor(recordGrid.hasFocus() ? TimelineViewColors.FOCUSED_INTERVAL_MARKER_COLOR
						: TimelineViewColors.INTERVAL_MARKER_COLOR));
		currentRecordInterval.getEndMarker()
				.setColor(UIManager.getColor(recordGrid.hasFocus() ? TimelineViewColors.FOCUSED_INTERVAL_MARKER_COLOR
						: TimelineViewColors.INTERVAL_MARKER_COLOR));
		currentRecordInterval
				.setColor(UIManager.getColor(recordGrid.hasFocus() ? TimelineViewColors.FOCUSED_INTERVAL_BACKGROUND
						: TimelineViewColors.INTERVAL_BACKGROUND));
		currentRecordInterval.setVisible(wasVisible);
	}

	public void setupRecord(Record r) {
		if(r != null) {
			recordGrid.setCurrentRecord(r);
		} else {
			getSelectionModel().clearSelection();
		}
		updateCurrentRecordInterval(r);

		mouseListener.waitForRecordChange = false;
	}

	/* Editor events */
	private void onRecordChange(EditorEvent<EditorEventType.RecordChangedData> evt) {
		Record r = evt.data().record();
		// don't update if already switched to record
		if(recordGrid.getCurrentRecordIndex() == getParentView().getEditor().getCurrentRecordIndex()) {
			updateCurrentRecordInterval(r);
			return;
		}

		if (recordGrid.isSplitMode()) {
			recordGrid.setSplitModeAccept(false);
			recordGrid.setSplitMode(false);
		}
		setupRecord(r);

		getSelectionModel().setSelectionInterval(getParentView().getEditor().getCurrentRecordIndex(),
				getParentView().getEditor().getCurrentRecordIndex());

		recordGrid.repaint(recordGrid.getVisibleRect());
	}

	private void onRecordDeleted(EditorEvent<EditorEventType.RecordDeletedData> evt) {
		if (currentRecordInterval != null) {
			getTimeModel().removeInterval(currentRecordInterval);

			Record currentRecord = getParentView().getEditor().currentRecord();
			if (currentRecord != null) {
				setupRecord(currentRecord);
			}
		}
	}

	private void onSpeakerChange(EditorEvent<EditorEventType.SpeakerChangedData> evt) {
		recordGrid.repaint(recordGrid.getVisibleRect());
	}

	private void onParticipantRemoved(EditorEvent<Participant> ee) {
		recordGrid.removeSpeaker(ee.data());
	}

	private void onParticipantAdded(EditorEvent<Participant> ee) {
		setSpeakerVisible(ee.data(), true);
	}

	private void onTierViewChanged(EditorEvent<EditorEventType.TierViewChangedData> ee) {
		revalidate();
		repaint();
	}

	private void onTierChanged(EditorEvent<EditorEventType.TierChangeData> ee) {
		if(ee.data().valueAdjusting()) return;
		final String tierName = ee.data().tier().getName();
		if (SystemTierType.Orthography.getName().equals(tierName)
				|| SystemTierType.Segment.getName().equals(tierName)) {
			updateCurrentRecordInterval(getParentView().getEditor().currentRecord());
			recordGrid.repaint(recordGrid.getVisibleRect());
		}
	}

	/**
	 * Is the speaker visible?
	 * 
	 * @param speaker
	 * @return
	 */
	public boolean isSpeakerVisible(Participant speaker) {
		return recordGrid.getSpeakers().contains(speaker);
	}

	public void setSpeakerVisible(Participant speaker, boolean visible) {
		List<Participant> currentSpeakers = new ArrayList<>(recordGrid.getSpeakers());
		List<Participant> allSpeakers = new ArrayList<>();
		for(Participant p:recordGrid.getSession().getParticipants()) allSpeakers.add(p);
		allSpeakers.add(Participant.UNKNOWN);

		List<Participant> newSpeakerList = new ArrayList<>();
		for(Participant sessionSpeaker:allSpeakers) {
			if(sessionSpeaker == speaker) {
				if(visible)
					newSpeakerList.add(sessionSpeaker);
			} else {
				if(currentSpeakers.contains(sessionSpeaker))
					newSpeakerList.add(sessionSpeaker);
			}
		}

		recordGrid.setSpeakers(newSpeakerList);
	}

	public void toggleSpeaker(PhonActionEvent<Participant> pae) {
		Participant speaker = pae.getData();
		setSpeakerVisible(speaker, !isSpeakerVisible(speaker));
	}

	public List<Participant> getSpeakerList() {
		List<Participant> retVal = new ArrayList<>();

		Session session = getParentView().getEditor().getSession();
		for (var speaker : session.getParticipants()) {
			if (isSpeakerVisible(speaker)) {
				retVal.add(speaker);
			}
		}

		return retVal;
	}

	private void setupSpeakers() {
		Session session = getParentView().getEditor().getSession();

		var speakerList = new ArrayList<Participant>();
		for (var speaker : session.getParticipants()) {
			speakerList.add(speaker);
		}
		speakerList.add(Participant.UNKNOWN);

		recordGrid.setSpeakers(speakerList);
	}

	public boolean isDraggingRecord() {
		return dragData.mouseDragOffset >= 0;
	}

	public boolean isTierVisible(String tierName) {
		boolean retVal = false;

		if (tierVisibility.containsKey(tierName))
			retVal = tierVisibility.get(tierName);

		return retVal;
	}

	public void setTierVisible(String tierName, boolean visible) {
		tierVisibility.put(tierName, visible);
		setupTiers();
	}

	public void toggleTier(String tierName) {
		setTierVisible(tierName, !isTierVisible(tierName));
	}

	private void setupTiers() {
		Session session = getParentView().getEditor().getSession();
		recordGrid.setTiers(session.getTierView().stream().map(TierViewItem::getTierName).filter(this::isTierVisible)
				.collect(Collectors.toList()));
	}

	public boolean isSplitModeActive() {
		return this.splitMarker != null;
	}

	public void beginSplitMode() {
		if (this.splitMarker != null) {
			endSplitMode(false);
		}

		final TimelineView timelineView = getParentView();
		final TimeUIModel timeModel = timelineView.getTimeModel();

		final Record record = timelineView.getEditor().currentRecord();
		if (record == null)
			return;

		final MediaSegment segment = record.getMediaSegment();
		if (segment == null)
			return;

		float segLength = segment.getLength();
		if (segLength <= 0.0f)
			return;

		float middleOfRecord = (float) TimeUIModel.roundTime((segment.getEndTime() - (segLength / 2.0f)));
		SplitMarker splitMarker = new SplitMarker(currentRecordInterval(), middleOfRecord);

		splitMarker.addPropertyChangeListener("time", (e) -> {
			updateSplitRecordTimes((float) e.getNewValue());
		});

		timeModel.addMarker(splitMarker);

		setSplitMarker(splitMarker);
		var splitRecords = getRecordSplit(middleOfRecord);

		getRecordGrid().beginSplitMode(splitRecords.getObj1(), splitRecords.getObj2());
		getRecordGrid().repaintInterval(currentRecordInterval);
		
		splitButton.setVisible(false);
		acceptSplitButton.setVisible(true);
		cancelSplitButton.setVisible(true);
	}

	private void endSplitMode(boolean acceptSplit) {
		if (splitMarker != null) {
			getTimeModel().removeMarker(splitMarker);
			this.splitMarker = null;
		}

		// getRecordGrid().setSplitMode(false);
		if (acceptSplit && getRecordGrid().getLeftRecordSplit() != null
				&& getRecordGrid().getRightRecordSplit() != null) {
			getParentView().getEditor().getUndoSupport().beginUpdate();

			final Record record = getParentView().getEditor().currentRecord();
			int recordIdx = getParentView().getEditor().getCurrentRecordIndex();
			int recordElementIdx = getParentView().getEditor().getSession().getRecordElementIndex(recordIdx);

			AddRecordEdit rightRecordEdit = new AddRecordEdit(getParentView().getEditor(),
					getRecordGrid().getRightRecordSplit(), recordElementIdx+1);
			getParentView().getEditor().getUndoSupport().postEdit(rightRecordEdit);

			TierEdit<MediaSegment> segEdit = new TierEdit<>(getParentView().getEditor(),
					record.getSegmentTier(), getRecordGrid().getLeftRecordSplit().getMediaSegment());
			getParentView().getEditor().getUndoSupport().postEdit(segEdit);

			getParentView().getEditor().getUndoSupport().endUpdate();
		}
		if (currentRecordInterval != null)
			getRecordGrid().repaintInterval(currentRecordInterval);

		// update record interval
		updateCurrentRecordInterval(getParentView().getEditor().currentRecord());
		
		splitButton.setVisible(true);
		cancelSplitButton.setVisible(false);
		acceptSplitButton.setVisible(false);
	}

	public void onEndSplitRecord(PhonActionEvent<Boolean> pae) {
		recordGrid.setSplitModeAccept((boolean) pae.getData());
		recordGrid.setSplitMode(false);
	}

	private void updateSplitRecords() {
		if (splitMarker == null)
			return;

		var recordSplit = getRecordSplit(splitMarker.getTime());
		getRecordGrid().setLeftRecordSplit(recordSplit.getObj1());
		getRecordGrid().setRightRecordSplit(recordSplit.getObj2());

		getRecordGrid().repaintInterval(currentRecordInterval);
	}

	private void updateSplitRecordTimes(float splitTime) {
		Record leftRecord = recordGrid.getLeftRecordSplit();
		MediaSegment leftSeg = leftRecord.getMediaSegment();
		leftSeg.setEndTime(splitTime);

		Record rightRecord = recordGrid.getRightRecordSplit();
		MediaSegment rightSeg = rightRecord.getMediaSegment();

		rightSeg.setStartTime(splitTime);

		final Rectangle intervalRect = getRecordGrid().rectForInterval(currentRecordInterval.getStartMarker().getTime(), currentRecordInterval.getEndMarker().getTime());
		// include record number, actions, and some padding
		intervalRect.width += 80;

		getRecordGrid().repaint(intervalRect.x, 0, intervalRect.width, getRecordGrid().getHeight());
	}

	private Tuple<Record, Record> getRecordSplit(float splitTime) {
		final SessionFactory sessionFactory = SessionFactory.newFactory();

		Record recordToSplit = getParentView().getEditor().currentRecord();
		MediaSegment seg = recordToSplit.getMediaSegment();

		Record leftRecord = sessionFactory.cloneRecord(recordToSplit);
		leftRecord.getMediaSegment().setEndTime(splitTime);

		Record rightRecord = sessionFactory.createRecord(getParentView().getEditor().getSession());
		MediaSegment rightSeg = sessionFactory.createMediaSegment();
		rightSeg.setUnitType(seg.getUnitType());
//		float segOffset = (float) Math.ceil(timeAtX(getTimeModel().getTimeInsets().left + 1));
		rightSeg.setStartTime(splitTime);
		rightSeg.setEndTime(seg.getEndTime());
		rightRecord.setMediaSegment(rightSeg);

		for (String tierName : leftRecord.getUserDefinedTierNames()) {
			Tier<?> tier = leftRecord.getTier(tierName);
			rightRecord.putTier(sessionFactory.createTier(tierName, tier.getDeclaredType(), tier.getTierParameters(), tier.isExcludeFromAlignment()));
		}

		leftRecord.setSpeaker(recordToSplit.getSpeaker());
		rightRecord.setSpeaker(recordToSplit.getSpeaker());

		return new Tuple<>(leftRecord, rightRecord);
	}

	public SplitMarker getSplitMarker() {
		return this.splitMarker;
	}

	public void setSplitMarker(SplitMarker splitMarker) {
		this.splitMarker = splitMarker;
	}

	@Override
	public void setupContextMenu(MenuBuilder builder, boolean includeAccel) {
		final PhonUIAction<Void> copyAct = PhonUIAction.runnable(this::copy);
		copyAct.putValue(PhonUIAction.NAME, "Copy record" + (getSelectionModel().getSelectedItemsCount()>1 ? "s" : ""));
		copyAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Copy selected records");
		if(includeAccel)
			copyAct.putValue(PhonUIAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
		builder.addItem(".", copyAct);

		Transferable clipboardContents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this);
		if(clipboardContents != null && clipboardContents.isDataFlavorSupported(RecordsTransferable.FLAVOR)) {
			try {
				RecordsTransferable recordsTransferable = (RecordsTransferable) clipboardContents.getTransferData(RecordsTransferable.FLAVOR);
				final PhonUIAction<Void> pasteAct = PhonUIAction.eventConsumer(this::paste);
				pasteAct.putValue(PhonUIAction.NAME, "Paste record" + (recordsTransferable.getRecords().size() > 1 ? "s" : ""));
				pasteAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Paste records");
				if(includeAccel)
					pasteAct.putValue(PhonUIAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
				builder.addItem(".", pasteAct);
			} catch (UnsupportedFlavorException | IOException e) {
				LogUtil.warning(e);
			}
		}

		final PhonUIAction<Void> cutAct = PhonUIAction.eventConsumer(this::cut);
		cutAct.putValue(PhonUIAction.NAME, "Cut record" + (getSelectionModel().getSelectedItemsCount()>1 ? "s" : ""));
		cutAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Cut selected records");
		if(includeAccel)
				cutAct.putValue(PhonUIAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
		builder.addItem(".", cutAct);

		builder.addSeparator(".", "copy_paste");

		final PhonUIAction<Void> selectAllAct = PhonUIAction.eventConsumer(this::onSelectAll);
		selectAllAct.putValue(PhonUIAction.NAME, "Select all");
		selectAllAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Select all visible records");
		if(includeAccel)
			selectAllAct.putValue(PhonUIAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
		builder.addItem(".", selectAllAct);

		JMenu speakerMenu = builder.addMenu(".", "Select all for participant");
		for(Participant speaker:getSpeakerList()) {
			PhonUIAction<Participant> selectSpeakerAct = PhonUIAction.eventConsumer(this::onSelectSpeaker, speaker);
			selectSpeakerAct.putValue(PhonUIAction.NAME, speaker.toString());
			speakerMenu.add(selectSpeakerAct);
		}
		if(isSpeakerVisible(Participant.UNKNOWN)) {
			PhonUIAction<Participant> selectSpeakerAct = PhonUIAction.eventConsumer(this::onSelectSpeaker, Participant.UNKNOWN);
			selectSpeakerAct.putValue(PhonUIAction.NAME, Participant.UNKNOWN.toString());
			speakerMenu.add(selectSpeakerAct);
		}

		var openResultSets = QueryAndReportWizard.findOpenResultSets(getParentView().getEditor().getSession());
		if(openResultSets.size() > 0) {
			JMenu selectResultsMenu = builder.addMenu(".", "Select records from query results");
			for(int i = 0; i < openResultSets.size(); i++) {
				var tuple = openResultSets.get(i);
				QueryAndReportWizard wizard = tuple.getObj1();
				QueryScript queryScript = wizard.getQueryScript();
				QueryName queryName = queryScript.getExtension(QueryName.class);
				String queryNum = tuple.getObj2().getObj1();
				ResultSet rs = tuple.getObj2().getObj2();

				String queryItemName = String.format("%s: %s (%d results)", queryName.getName(), queryNum, rs.numberOfResults(false));
				final PhonUIAction<ResultSet> selectResultRecordsAct = PhonUIAction.eventConsumer(this::onSelectResultRecords, rs);
				selectResultRecordsAct.putValue(PhonUIAction.NAME, queryItemName);
				selectResultsMenu.add(selectResultRecordsAct);
			}
		}

		builder.addSeparator(".", "selection");

		setupSplitModeMenu(builder, includeAccel);

		var delAction = new DeleteRecordsAction(getParentView());
		if (includeAccel)
			delAction.putValue(PhonUIAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
		builder.addItem(".", new DeleteRecordsAction(getParentView()));

		// change speaker menu
		JMenu changeSpeakerMenu = builder.addMenu(".", "Change participant");
		final ImageIcon speakerIcon =
				IconManager.getInstance().buildFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "person", IconSize.MEDIUM, UIManager.getColor("Button.foreground"));
		changeSpeakerMenu.setIcon(speakerIcon);
		int speakerNum = 1;
		for (Participant speaker : recordGrid.getSpeakers()) {
			KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_0 + speakerNum,
					Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
			if (speaker == Participant.UNKNOWN) {
				ks = KeyStroke.getKeyStroke(KeyEvent.VK_0, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
				changeSpeakerMenu.addSeparator();
			}

			final PhonUIAction<Integer> onChangeSpeakerByIndexAct = PhonUIAction.consumer(this::onChangeSpeakerByIndex, speakerNum);
			onChangeSpeakerByIndexAct.putValue(PhonUIAction.NAME, speaker.toString());
			onChangeSpeakerByIndexAct.putValue(PhonUIAction.SHORT_DESCRIPTION,
					"Change record speaker to " + speaker.toString());
			if (includeAccel)
				onChangeSpeakerByIndexAct.putValue(PhonUIAction.ACCELERATOR_KEY, ks);
			if (getParentView().getEditor().currentRecord() != null
					&& getParentView().getEditor().currentRecord().getSpeaker() == speaker) {
				onChangeSpeakerByIndexAct.putValue(PhonUIAction.SELECTED_KEY, true);
			} else {
				onChangeSpeakerByIndexAct.putValue(PhonUIAction.SELECTED_KEY, false);
			}

			JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(onChangeSpeakerByIndexAct);
			changeSpeakerMenu.add(menuItem);

			++speakerNum;
		}

		builder.addSeparator(".", "record_actions");

		final PhonUIAction<Void> moveSegmentsAct = PhonUIAction.eventConsumer(this::onMoveSegments);
		moveSegmentsAct.putValue(PhonUIAction.NAME, "Move records...");
		moveSegmentsAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Move selected records a specified amount of time");
		if(includeAccel)
			moveSegmentsAct.putValue(PhonUIAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, KeyEvent.CTRL_DOWN_MASK));
		builder.addItem(".", moveSegmentsAct);

		final PhonUIAction<Integer> moveSegmentsRightAct = PhonUIAction.eventConsumer(this::onMoveSegmentsRight, 5);
		moveSegmentsRightAct.putValue(PhonUIAction.NAME, "Move record" + (getSelectionModel().getSelectedItemsCount() > 1 ? "s" : "") + " right");
		moveSegmentsRightAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Move selected records right on the timeline");
		if(includeAccel)
			moveSegmentsRightAct.putValue(PhonUIAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, KeyEvent.CTRL_DOWN_MASK));
		builder.addItem(".", moveSegmentsRightAct);

		final PhonUIAction<Integer> moveSegmentsLeftAct = PhonUIAction.eventConsumer(this::onMoveSegmentsLeft, 5);
		moveSegmentsLeftAct.putValue(PhonUIAction.NAME, "Move record" + (getSelectionModel().getSelectedItemsCount() > 1 ? "s" : "") + " left");
		moveSegmentsLeftAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Move selected records left on the timeline");
		if(includeAccel)
			moveSegmentsLeftAct.putValue(PhonUIAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, KeyEvent.CTRL_DOWN_MASK));
		builder.addItem(".", moveSegmentsLeftAct);

		final PhonUIAction<Integer> growSegmentsAct = PhonUIAction.eventConsumer(this::onGrowSegments, 3);
		growSegmentsAct.putValue(PhonUIAction.NAME, "Grow record" + (getSelectionModel().getSelectedItemsCount() > 1 ? "s" : ""));
		growSegmentsAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Grow selected records");
		if(includeAccel)
			growSegmentsAct.putValue(PhonUIAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.CTRL_DOWN_MASK));
		builder.addItem(".", growSegmentsAct);

		final PhonUIAction<Integer> shrinkSegmentsAct = PhonUIAction.eventConsumer(this::onShrinkSegments, 3);
		shrinkSegmentsAct.putValue(PhonUIAction.NAME, "Shrink record" + (getSelectionModel().getSelectedItemsCount() > 1 ? "s" : ""));
		shrinkSegmentsAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Shrink selected records");
		if(includeAccel)
			shrinkSegmentsAct.putValue(PhonUIAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_K, KeyEvent.CTRL_DOWN_MASK));
		builder.addItem(".", shrinkSegmentsAct);

		builder.addSeparator(".", "other_actions");
		builder.addItem(".", new DistributeRecordsAction(getParentView()));
	}

	private void setupSplitModeMenu(MenuBuilder builder, boolean includeAccel) {
		if (splitMarker != null) {
			final PhonUIAction<Boolean> acceptSplitAct = PhonUIAction.eventConsumer(this::onEndSplitRecord, true);
			acceptSplitAct.putValue(PhonUIAction.NAME, "Accept record split");
			acceptSplitAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "");
			acceptSplitAct.putValue(PhonUIAction.SMALL_ICON,
					IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "check", IconSize.SMALL, UIManager.getColor("Button.foreground")));
			if (includeAccel)
				acceptSplitAct.putValue(PhonUIAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
			builder.addItem(".", acceptSplitAct);

			final PhonUIAction<Boolean> endSplitModeAct = PhonUIAction.eventConsumer(this::onEndSplitRecord, false);
			endSplitModeAct.putValue(PhonUIAction.NAME, "Exit split record");
			endSplitModeAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Exit split record mode without accepting split");
			endSplitModeAct.putValue(PhonUIAction.SMALL_ICON,
					IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "close", IconSize.SMALL, UIManager.getColor("Button.foreground")));
			if (includeAccel)
				endSplitModeAct.putValue(PhonUIAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
			builder.addItem(".", endSplitModeAct);

			builder.addSeparator(".", "split_actions");
		} else {
			final PhonUIAction<Void> enterSplitModeAct = PhonUIAction.runnable(this::beginSplitMode);
			enterSplitModeAct.putValue(PhonUIAction.NAME, "Split record");
			enterSplitModeAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Enter split record mode for current record");
			if (includeAccel)
				enterSplitModeAct.putValue(PhonUIAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, 0));
			builder.addItem(".", enterSplitModeAct);
		}
	}
	
	public void setupSpeakerContextMenu(Participant participant, MenuBuilder builder) {
		builder.addSeparator(".", "select");
		final PhonUIAction<Participant> selectAllAct = PhonUIAction.eventConsumer(this::onSelectSpeaker, participant);
		selectAllAct.putValue(PhonUIAction.NAME, "Select all");
		selectAllAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Select all records for " + participant.toString());
		builder.addItem(".", selectAllAct);

		Session session = getParentView().getEditor().getSession();
		JMenu reassignMenu = new JMenu("Reassign records to");
		for(Participant speaker:session.getParticipants()) {
			if(speaker != participant) {
				final ReassignRecordsAction reassignAct = new ReassignRecordsAction(getParentView(), participant, speaker);
				reassignAct.putValue(Action.NAME, speaker.toString());
				reassignAct.putValue(Action.SHORT_DESCRIPTION, String.format("Reassign all records from %s to %s", participant, speaker));
				reassignMenu.add(reassignAct);
			}
		}
		if(Participant.UNKNOWN != participant) {
			final ReassignRecordsAction reassignAct = new ReassignRecordsAction(getParentView(), participant, Participant.UNKNOWN);
			reassignAct.putValue(Action.NAME, Participant.UNKNOWN.toString());
			reassignAct.putValue(Action.SHORT_DESCRIPTION, String.format("Reassign all records from %s to %s", participant, Participant.UNKNOWN));
			reassignMenu.add(reassignAct);
		}
		builder.addSeparator(".", "reassign");
		if(reassignMenu.getItemCount() > 0) {
			builder.addMenu(".", reassignMenu);
		}
	}

	public void setupSpeakerMenu(MenuBuilder builder) {
		Session session = getParentView().getEditor().getSession();
		for (var speaker : session.getParticipants()) {
			final PhonUIAction<Participant> toggleSpeakerAct = PhonUIAction.eventConsumer(this::toggleSpeaker, speaker);
			toggleSpeakerAct.putValue(PhonUIAction.NAME, speaker.toString());
			toggleSpeakerAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Toggle speaker " + speaker);
			toggleSpeakerAct.putValue(PhonUIAction.SELECTED_KEY, isSpeakerVisible(speaker));
			final JCheckBoxMenuItem toggleSpeakerItem = new JCheckBoxMenuItem(toggleSpeakerAct);
			builder.addItem(".", toggleSpeakerItem);
		}

		final PhonUIAction<Participant> toggleUnknownAct = PhonUIAction.eventConsumer(this::toggleSpeaker, Participant.UNKNOWN);
		toggleUnknownAct.putValue(PhonUIAction.NAME, Participant.UNKNOWN.toString());
		toggleUnknownAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Toggle speaker " + Participant.UNKNOWN);
		toggleUnknownAct.putValue(PhonUIAction.SELECTED_KEY, isSpeakerVisible(Participant.UNKNOWN));
		final JCheckBoxMenuItem toggleUnknownItem = new JCheckBoxMenuItem(toggleUnknownAct);
		builder.addItem(".", toggleUnknownItem);

		builder.addSeparator(".", "speaker_actions");

		final NewParticipantAction newParticipantAct = new NewParticipantAction(getParentView().getEditor());
		builder.addItem(".", newParticipantAct);
	}

	public void setupTierMenu(MenuBuilder builder) {
		Session session = getParentView().getEditor().getSession();
		for (var tierViewItem : session.getTierView()) {
			final PhonUIAction<String> toggleTierAct = PhonUIAction.consumer(this::toggleTier, tierViewItem.getTierName());
			toggleTierAct.putValue(PhonUIAction.NAME, tierViewItem.getTierName());
			toggleTierAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Toggle tier " + tierViewItem.getTierName());
			toggleTierAct.putValue(PhonUIAction.SELECTED_KEY, isTierVisible(tierViewItem.getTierName()));
			final JCheckBoxMenuItem toggleTierItem = new JCheckBoxMenuItem(toggleTierAct);
			builder.addItem(".", toggleTierItem);
		}
	}

	public boolean isResizeable() {
		return false;
	}

	@Override
	public void onClose() {
		deregisterEditorEvents();
	}

	@Override
	public void lostOwnership(Clipboard clipboard, Transferable contents) {
	}

	private class RecordIntervalListener implements PropertyChangeListener {

		private boolean isFirstChange = true;

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			Record r = recordGrid.getCurrentRecord();
			if(r == null) return;
			
			MediaSegment segment = r.getMediaSegment();
			final SessionFactory factory = SessionFactory.newFactory();

			if(evt.getPropertyName().equals("valueAdjusting")) {
				if(recordGrid.isFocusable()) {
					recordGrid.requestFocusInWindow();
				}
				// exit split mode if active
				recordGrid.setSplitModeAccept(false);
				recordGrid.setSplitMode(false);

				if((boolean)evt.getNewValue()) {
					isFirstChange = true;
					getParentView().getEditor().getUndoSupport().beginUpdate();
				} else {
					getParentView().getEditor().getUndoSupport().endUpdate();
					getParentView().getEditor().getEventManager().queueEvent(
							new EditorEvent<>(EditorEventType.TierChange, TimelineRecordTier.this,
									new EditorEventType.TierChangeData(Transcriber.VALIDATOR, r, r.getSegmentTier(), segment, segment, false)));
				}
			} else if(evt.getPropertyName().endsWith("time")) {
				MediaSegment newSegment = factory.createMediaSegment();
				newSegment.setUnitType(segment.getUnitType());
				newSegment.setStartValue(segment.getStartValue());
				newSegment.setEndValue(segment.getEndValue());

				if(evt.getPropertyName().startsWith("startMarker")) {
					newSegment.setStartTime((float)evt.getNewValue());
				} else if(evt.getPropertyName().startsWith("endMarker")) {
					newSegment.setEndTime((float)evt.getNewValue());
				}

				final RecordSegmentEdit segmentEdit = new RecordSegmentEdit(getParentView().getEditor(), r, newSegment);
				getParentView().getEditor().getUndoSupport().postEdit(segmentEdit);
				segmentEdit.setFireHardChangeOnUndo(isFirstChange);
				isFirstChange = false;

				recordGrid.repaint(recordGrid.getVisibleRect());
			}
		}

	}

	private FocusListener selectionFocusListener = new FocusListener() {

		@Override
		public void focusLost(FocusEvent e) {
			if (currentRecordInterval != null) {
				currentRecordInterval.setColor(UIManager.getColor(TimelineViewColors.INTERVAL_BACKGROUND));
				currentRecordInterval.getStartMarker()
						.setColor(UIManager.getColor(TimelineViewColors.INTERVAL_MARKER_COLOR));
				currentRecordInterval.getEndMarker()
						.setColor(UIManager.getColor(TimelineViewColors.INTERVAL_MARKER_COLOR));
			}
		}

		@Override
		public void focusGained(FocusEvent e) {
			if (currentRecordInterval != null) {
				currentRecordInterval.setColor(UIManager.getColor(TimelineViewColors.FOCUSED_INTERVAL_BACKGROUND));
				currentRecordInterval.getStartMarker()
						.setColor(UIManager.getColor(TimelineViewColors.FOCUSED_INTERVAL_MARKER_COLOR));
				currentRecordInterval.getEndMarker()
						.setColor(UIManager.getColor(TimelineViewColors.FOCUSED_INTERVAL_MARKER_COLOR));
			}
		}
	};
//
//	private Participant cancelParticipant = Participant.UNKNOWN;
//
//	private float cancelStartTime = -1.0f;
//
//	private float cancelEndTime = -1.0f;
//
//	private int currentDraggedRecord = -1;
//
//	private float mouseDragOffset = -1.0f;

	private class DragData {
		/*
		 * Participant being dragged or Participant.ALL if
		 * multiple participants are being dragged
		 */
		Participant participant;

		int draggedRecord = -1;
		float mouseDragOffset = -1.0f;

		//Map<Integer, TierEdit<MediaSegment>> tierEdits = new LinkedHashMap<>();
		Map<Integer, MediaSegment> editSegments = new LinkedHashMap<>();
		Map<Integer, MediaSegment> originalSegments = new LinkedHashMap<>();

		boolean isFirstChange = true;
	}
	private final DragData dragData = new DragData();

	private void beingRecordDrag(int recordIndex) {
		Toolkit.getDefaultToolkit().addAWTEventListener(cancelDragListener, AWTEvent.KEY_EVENT_MASK);
		dragData.draggedRecord = recordIndex;
		Set<Participant> participants = new HashSet<>();

		dragData.originalSegments.clear();
		dragData.editSegments.clear();
		for(int selectedRecord:getSelectionModel().getSelectedIndices()) {
			Record r = getRecordGrid().getSession().getRecord(selectedRecord);
			participants.add(r.getSpeaker());

			MediaSegment recordSeg = r.getMediaSegment();
			MediaSegment origSeg = SessionFactory.newFactory().createMediaSegment();
			origSeg.setUnitType(recordSeg.getUnitType());
			origSeg.setStartValue(recordSeg.getStartValue());
			origSeg.setEndValue(recordSeg.getEndValue());
			dragData.originalSegments.put(selectedRecord, origSeg);

			MediaSegment editSegment = SessionFactory.newFactory().createMediaSegment();
			editSegment.setUnitType(origSeg.getUnitType());
			editSegment.setStartValue(origSeg.getStartValue());
			editSegment.setEndValue(origSeg.getEndValue());
			dragData.editSegments.put(selectedRecord, editSegment);
		}
		dragData.participant = (participants.size() == 1 ? participants.iterator().next() : Participant.ALL);
		currentRecordInterval.setValueAdjusting(true);

		dragData.isFirstChange = true;
	}
	
	private void endRecordDrag() {
		Toolkit.getDefaultToolkit().removeAWTEventListener(cancelDragListener);
		if (currentRecordInterval != null)
			currentRecordInterval.setValueAdjusting(false);
		dragData.draggedRecord = -1;
		dragData.mouseDragOffset = -1.0f;
	}
	
	private void cancelRecordDrag() {
		for(int recordIndex:getSelectionModel().getSelectedIndices()) {
			Record r = getParentView().getEditor().getSession().getRecord(recordIndex);
			if(dragData.participant != Participant.ALL)
				r.setSpeaker(dragData.participant);
			MediaSegment origSegment = dragData.originalSegments.get(recordIndex);
			if (currentRecordInterval != null && getRecordGrid().getCurrentRecordIndex() == recordIndex) {
				currentRecordInterval.getStartMarker().setTime(origSegment.getStartTime());
				currentRecordInterval.getEndMarker().setTime(origSegment.getEndTime());
			} else {
				MediaSegment recordSeg = r.getMediaSegment();
				recordSeg.setUnitType(origSegment.getUnitType());
				recordSeg.setStartValue(origSegment.getStartValue());
				recordSeg.setEndValue(origSegment.getEndValue());
			}
		}
		dragData.mouseDragOffset = -1.0f;
		endRecordDrag();
	}

	/*
	 * Copy & Paste
	 */

	/**
	 * Copy selected records to clipboard.
	 */
	public void copy() {
		RecordsTransferable transferable = new RecordsTransferable(getParentView().getEditor().getSession(),
				getSelectionModel().getSelectedIndices());
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferable, this);
	}

	public void paste(PhonActionEvent<Void> pae) {
		int prevNumRecords = getParentView().getEditor().getSession().getRecordCount();
		PasteRecordAction pasteAct = new PasteRecordAction(getParentView().getEditor());
		pasteAct.actionPerformed(pae.getActionEvent());
		int numRecords = getParentView().getEditor().getSession().getRecordCount();

		// select all new records
		getSelectionModel().setSelectionInterval(prevNumRecords, numRecords-1);
	}

	public void cut(PhonActionEvent<Void> pae) {
		copy();
		DeleteRecordsAction deleteRecordsAction = new DeleteRecordsAction(getParentView(), false);
		deleteRecordsAction.actionPerformed(pae.getActionEvent());
	}

	private final AWTEventListener cancelDragListener = new AWTEventListener() {
		
		@Override
		public void eventDispatched(AWTEvent event) {
			if(event instanceof KeyEvent) {
				KeyEvent ke = (KeyEvent)event;
				if(ke.getID() == KeyEvent.KEY_PRESSED && 
						ke.getKeyChar() == KeyEvent.VK_ESCAPE) {
					cancelRecordDrag();
					((KeyEvent) event).consume();
				}
			}
		}
	};

	private final RecordMouseListener mouseListener = new RecordMouseListener();
	
	private class RecordMouseListener extends RecordGridMouseAdapter {

		volatile boolean waitForRecordChange = false;

		volatile boolean draggingRecord = false;

		@Override
		public void recordClicked(int recordIndex, MouseEvent me) {
		}

		@Override
		public void recordPressed(int recordIndex, MouseEvent me) {
			Record r = getParentView().getEditor().getSession().getRecord(recordIndex);
			MediaSegment seg = r.getMediaSegment();
			dragData.mouseDragOffset = getTimeModel().timeAtX(me.getX()) - seg.getStartTime();
		}

		@Override
		public void recordReleased(int recordIndex, MouseEvent me) {
			endRecordDrag();
		}

		@Override
		public void recordDragged(int recordIndex, MouseEvent me) {
			if (!getSelectionModel().isSelectedIndex(recordIndex)) {
				if((me.getModifiersEx() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()) == Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()) {
					getSelectionModel().addSelectionInterval(recordIndex, recordIndex);
				} else {
					getSelectionModel().setSelectionInterval(recordIndex, recordIndex);
					waitForRecordChange = true;
					getParentView().getEditor().setCurrentRecordIndex(recordIndex);
				}
				return;
			} else if(waitForRecordChange) {
				return;
			} else {
				// shouldn't happen
				if (currentRecordInterval == null)
					return;

				if (dragData.draggedRecord != recordIndex) {
					// don't adjust an already changing interval
					if (currentRecordInterval.isValueAdjusting())
						return;
					
					if(dragData.mouseDragOffset < 0) return;

					beingRecordDrag(recordIndex);
				}

				// scroll to mouse position if outside of visible rect
				Rectangle visibleRect = recordGrid.getVisibleRect();
				if (me.getX() < visibleRect.getX()) {
					visibleRect.translate(-10, 0);
					getParentView().scrollRectToVisible(visibleRect);
				} else if (me.getX() > visibleRect.getMaxX()) {
					visibleRect.translate(10, 0);
					getParentView().scrollRectToVisible(visibleRect);
				}

				Participant mouseOverSpeaker = recordGrid.getUI().getSpeakerAtPoint(me.getPoint());
				if (dragData.participant != Participant.ALL
						&& mouseOverSpeaker != null
						&& mouseOverSpeaker != getParentView().getEditor().currentRecord().getSpeaker()) {
					// change participants if all selections are in the same speaker tier
					for(int rIdx:getSelectionModel().getSelectedIndices()) {
						// change speakers
						ChangeSpeakerEdit chEdit = new ChangeSpeakerEdit(getParentView().getEditor(),
								getParentView().getEditor().getSession().getRecord(rIdx), mouseOverSpeaker);
						getParentView().getEditor().getUndoSupport().postEdit(chEdit);
					}
				}

				Record dragRecord = getRecordGrid().getSession().getRecord(dragData.draggedRecord);
				MediaSegment dragSeg = dragRecord.getMediaSegment();

				float startTime = dragSeg.getStartTime();
				float oldOffsetTime = startTime + dragData.mouseDragOffset;
				float newOffsetTime = recordGrid.timeAtX(me.getX());
				int direction = (oldOffsetTime < newOffsetTime ? 1 : -1);
				float delta = (direction < 0 ? oldOffsetTime - newOffsetTime : newOffsetTime - oldOffsetTime);

				for(int rIdx:getSelectionModel().getSelectedIndices()) {
					Record selectedRecord = getRecordGrid().getSession().getRecord(rIdx);
					MediaSegment seg = selectedRecord.getMediaSegment();

					float st = (rIdx == getRecordGrid().getCurrentRecordIndex() ?
							currentRecordInterval.getStartMarker().getTime() : seg.getStartTime());
					float et = (rIdx == getRecordGrid().getCurrentRecordIndex() ?
							currentRecordInterval.getEndMarker().getTime() : seg.getEndTime());
					float intervalDuration = et - st;

					float newStartTime = 0.0f;
					float newEndTime = 0.0f;

					if (direction < 0) {
						newStartTime = Math.max(st - delta, getTimeModel().getStartTime());
						newEndTime = newStartTime + intervalDuration;
					} else {
						newEndTime = Math.min(et + delta, getTimeModel().getEndTime());
						newStartTime = newEndTime - intervalDuration;
					}
					if(rIdx == getRecordGrid().getCurrentRecordIndex()) {
						currentRecordInterval.getStartMarker().setTime(newStartTime);
						currentRecordInterval.getEndMarker().setTime(newEndTime);
					} else {
						MediaSegment editSeg = dragData.editSegments.get(rIdx);
						editSeg.setStartTime(newStartTime);
						editSeg.setEndTime(newEndTime);

//						if(dragData.isFirstChange) {
							final RecordSegmentEdit tierEdit = new RecordSegmentEdit(getParentView().getEditor(), selectedRecord, editSeg);
							getParentView().getEditor().getUndoSupport().postEdit(tierEdit);
//						}
					}
				}
				dragData.isFirstChange = false;
			}
		}

	};

	/**
	 * The SplitMarker is used when splitting the current record
	 *
	 */
	public static class SplitMarker extends TimeUIModel.Marker {

		private Interval parentInterval;

		public SplitMarker(Interval parentInterval, float startTime) {
			super(startTime);
			setColor(UIManager.getColor(TimelineViewColors.SPLIT_MARKER_COLOR));
			setDraggable(true);

			this.parentInterval = parentInterval;
		}

		@Override
		public void setTime(float time) {
			if (time <= parentInterval.getStartMarker().getTime() || time >= parentInterval.getEndMarker().getTime())
				return;
			super.setTime(time);
		}

	}

}
