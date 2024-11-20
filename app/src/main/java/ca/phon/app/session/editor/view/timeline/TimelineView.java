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
import ca.phon.app.session.EditorViewAdapter;
import ca.phon.app.session.editor.*;
import ca.phon.app.session.editor.actions.*;
import ca.phon.app.session.editor.view.mediaPlayer.MediaPlayerEditorView;
import ca.phon.app.session.editor.view.speechAnalysis.*;
import ca.phon.app.session.editor.view.timeline.actions.ZoomAction;
import ca.phon.media.*;
import ca.phon.media.TimeUIModel.*;
import ca.phon.media.export.VLCWavExporter;
import ca.phon.plugin.PluginManager;
import ca.phon.session.Record;
import ca.phon.session.*;
import ca.phon.ui.*;
import ca.phon.ui.action.PhonUIAction;
import ca.phon.ui.fonts.FontPreferences;
import ca.phon.ui.menu.MenuBuilder;
import ca.phon.ui.nativedialogs.MessageDialogProperties;
import ca.phon.ui.nativedialogs.NativeDialogs;
import ca.phon.util.PrefHelper;
import ca.phon.util.icons.*;
import ca.phon.worker.*;
import ca.phon.worker.PhonTask.TaskStatus;
import org.jdesktop.swingx.*;
import uk.co.caprica.vlcj.player.base.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.*;
import java.util.function.*;

/**
 * View for displaying session records along a timeline with the audio waveform if available.
 *
 */
public final class TimelineView extends EditorView {

	static {
		TimelineViewColors.install();
	}

	public static final String VIEW_NAME = "Timeline";

	public static final String VIEW_ICON = IconManager.GoogleMaterialDesignIconsFontName + ":view_timeline";
	
	private final static String PLABACK_FPS = "TimelineView.playbackFps";
	private final float DEFAULT_PLAYBACK_FPS = 30.0f;
	private float playbackMarkerFps = PrefHelper.getFloat(PLABACK_FPS, DEFAULT_PLAYBACK_FPS);
	
	private IconStrip toolbar;
	
	private FlatButton playButton;
	private FlatButton exportButton;
	
	private JButton zoomOutButton;
	
	private JButton zoomInButton;
	
	private JButton segmentationButton;
	
	private FlatButton speakerButton;
	private JPopupMenu speakerMenu;
	
	private FlatButton tierVisibilityButton;
	private JPopupMenu tierVisibilityMenu;

	private FlatButton fontSizeButton;
	private JPopupMenu fontSizeMenu;
	
	private JScrollPane tierScrollPane;
	private TierPanel tierPanel;
	
	private JPanel errorPanel;
	private ErrorBanner messageButton = new ErrorBanner();
	private PhonTaskButton generateButton = null;

	private VolumeSlider volumeSlider;

	/* Additional menu handlers */
	private final List<BiConsumer<MenuBuilder, Boolean>> menuHandlers = new ArrayList<>();
	
	/**
	 * Default {@link TimeUIModel} which should be
	 * used by most tier components
	 */
	private TimeUIModel timeModel;
	
	private Timebar timebar;
	
	private TimelineWaveformTier wavTier;
	
	private TimelineRecordTier recordGrid;

	// playback marker for segment playback
	private Marker segmentPlaybackMarker;
	
	// playback marker for media player
	private Marker mediaPlayerPlaybackMarker;
	private PlaybackMarkerSyncListener playbackMarkerSyncListener = new PlaybackMarkerSyncListener();
	
	public TimelineView(SessionEditor editor) {
		super(editor);
		addEditorViewListener(editorViewListener);
		
		init();
		update();
		
		editor.getMediaModel().getSegmentPlayback().addPropertyChangeListener(segmentPlaybackListener);
	}
	
	private void init() {
		toolbar = setupToolbar();

		// the shared time model
		timeModel = new TimeUIModel();
		
		timeModel.setPixelsPerSecond(100.0f);
		timeModel.setStartTime(0.0f);
		timeModel.setEndTime(100.0f);
		timeModel.addPropertyChangeListener("endTime", (e) -> {
			timebar.revalidate();
			wavTier.revalidate();
			recordGrid.revalidate();
		});
		
		timebar = new Timebar(timeModel);
		timebar.setOpaque(true);
		timebar.setBackground(Color.WHITE);
		timebar.addMouseListener(new MouseInputAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getButton() == MouseEvent.BUTTON1 &&
						e.getClickCount() == 1) {
					// goto position in media
					MediaPlayerEditorView mediaPlayerView = 
							(MediaPlayerEditorView)getEditor().getViewModel().getView(MediaPlayerEditorView.VIEW_NAME);
					if(mediaPlayerView != null) {
						float time = getTimeModel().timeAtX(e.getX());
						long timeMS = (long)(time * 1000.0f);
						
						mediaPlayerView.getPlayer().setTime(timeMS);
					}
				}
			}
			
		});
		
		tierPanel = new TierPanel(new GridBagLayout());
		tierScrollPane = new JScrollPane(tierPanel);
		tierScrollPane.setColumnHeaderView(timebar);
		
		// Order here matters - for the purpose of
		// editor events the record tier object must be created before the
		// wav tier
		recordGrid = new TimelineRecordTier(this);
		recordGrid.getRecordGrid().addMouseListener(contextMenuListener);
		
		wavTier = new TimelineWaveformTier(this);
		wavTier.getPreferredSize();
		wavTier.getWaveformDisplay().addMouseListener(contextMenuListener);
		
		addTier(wavTier);
		addTier(recordGrid);
		
		for(var extPt:PluginManager.getInstance().getExtensionPoints(TimelineTier.class)) {
			var tier = extPt.getFactory().createObject(this);
			addTier(tier);
		}
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.gridx = 0;
		gbc.gridy = tierIdx++;
		tierPanel.add(Box.createVerticalGlue(), gbc);

		setLayout(new BorderLayout());

		volumeSlider = new VolumeSlider(getEditor().getMediaModel().getVolumeModel());
		volumeSlider.setFocusable(false);
		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.add(toolbar, BorderLayout.CENTER);
		topPanel.add(volumeSlider, BorderLayout.EAST);
		add(topPanel, BorderLayout.NORTH);
		
		add(tierScrollPane, BorderLayout.CENTER);
		
		errorPanel = new JPanel(new VerticalLayout());
		errorPanel.add(messageButton);
		
		final GenerateSessionAudioAction generateAct = getEditor().getMediaModel().getGenerateSessionAudioAction();
		generateAct.putValue(PhonUIAction.LARGE_ICON_KEY, generateAct.getValue(PhonUIAction.SMALL_ICON));
		generateAct.addTaskListener(new PhonTaskListener() {
			
			@Override
			public void statusChanged(PhonTask task, TaskStatus oldStatus, TaskStatus newStatus) {
				if(TaskStatus.RUNNING == newStatus) {
					final VLCWavExporter wavExportTask = (VLCWavExporter)task;
					generateButton = new PhonTaskButton(wavExportTask);
					generateButton.getTopLabel().setFont(generateButton.getTopLabel().getFont().deriveFont(Font.BOLD));
					generateButton.getBusyLabel().setBusy(true);
					generateButton.setTopLabelText("Export audio - 0%");
					generateButton.setBottomLabelText(wavExportTask.getOutputFile().getAbsolutePath());
					
					messageButton.setVisible(false);
					errorPanel.add(generateButton);
				} else {
					if(generateButton != null) {
						errorPanel.remove(generateButton);
					}
					if(TaskStatus.FINISHED != newStatus) {
						messageButton.setVisible(true);
					}
				}
			}
			
			@Override
			public void propertyChanged(PhonTask task, String property, Object oldValue, Object newValue) {
				if(PhonTask.PROGRESS_PROP.contentEquals(property)) {
					generateButton.setTopLabelText(String.format("Export audio - %d%%", (int)Math.round(100.0*(float)newValue)));
				}
			}
		});

		add(errorPanel, BorderLayout.SOUTH);
		
		SessionMediaModel mediaModel = getEditor().getMediaModel();
		if(mediaModel.isSessionMediaAvailable()) {
			MediaPlayerEditorView mediaPlayerView = 
					(MediaPlayerEditorView)getEditor().getViewModel().getView(MediaPlayerEditorView.VIEW_NAME);
			if(mediaPlayerView.getPlayer().getMediaFile() != null) {
				mediaPlayerView.getPlayer().addMediaPlayerListener(playbackMarkerSyncListener);
			}
		}
	}

	public Timebar getTimebar() { return this.timebar; }
	
	public IconStrip getToolbar() {
		return this.toolbar;
	}
	
	public JComponent getTierPanel() {
		return this.tierPanel;
	}
	
	public TimelineWaveformTier getWaveformTier() {
		return this.wavTier;
	}
	
	public TimelineRecordTier getRecordTier() {
		return this.recordGrid;
	}
	
	private IconStrip setupToolbar() {
		IconStrip toolbar = new IconStrip(SwingConstants.HORIZONTAL);

		PhonUIAction<Void> segmentationAction = PhonUIAction.runnable(this::toggleSegmentation);
		segmentationAction.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
		segmentationAction.putValue(FlatButton.ICON_NAME_PROP, "splitscreen_vertical_add");
		segmentationAction.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);
		segmentationAction.putValue(PhonUIAction.NAME, "Start Segmentation...");
		segmentationButton = new FlatButton(segmentationAction);
		segmentationButton.setFocusable(false);
		segmentationButton.setEnabled(false);
		
		speakerMenu = new JPopupMenu();
		speakerMenu.addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				speakerMenu.removeAll();
				recordGrid.setupSpeakerMenu(new MenuBuilder(speakerMenu));
			}
			
			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			}
			
			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {
			}
			
		});
		
		final JPopupMenu playMenu = new JPopupMenu();
		playMenu.addPopupMenuListener(new PopupMenuListener() {
			
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				playMenu.removeAll();
				setupPlaybackMenu(new MenuBuilder(playMenu));
			}
			
			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			}
			
			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {
			}
			
		});
		
		final PhonUIAction<Void> playAct = PhonUIAction.runnable(this::playPause);
		playAct.putValue(PhonUIAction.NAME, "Play segment");
		playAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Play selection/segment");
//		final ImageIcon playIcon =
//				IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "play_arrow", IconSize.MEDIUM, UIManager.getColor("Button.foreground"));
//		playAct.putValue(PhonUIAction.SMALL_ICON, playIcon);
		playAct.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
		playAct.putValue(FlatButton.ICON_NAME_PROP, "play_arrow");
		playAct.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);
		playAct.putValue(DropDownButton.BUTTON_POPUP, playMenu);
		playButton = new FlatButton(playAct);
		playButton.setFocusable(false);
		playButton.setEnabled(false);

		final JPopupMenu saveMenu = new JPopupMenu();
		saveMenu.addPopupMenuListener(new PopupMenuListener() {
			
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				saveMenu.removeAll();
				setupExportMenu(new MenuBuilder(saveMenu));
			}
			
			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				
			}
			
			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {
				
			}
			
		});
		
		final PhonUIAction<Void> exportAct = PhonUIAction.runnable(this::onExportSelectionOrSegment);
//		final ImageIcon exportIcon =
//				IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "file_export", IconSize.MEDIUM, UIManager.getColor("Button.foreground"));
//		exportAct.putValue(PhonUIAction.SMALL_ICON, exportIcon);
		exportAct.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
		exportAct.putValue(FlatButton.ICON_NAME_PROP, "file_export");
		exportAct.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);
		exportAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Export selection/segment (audio only)");
		exportAct.putValue(PhonUIAction.NAME, "Export segment...");
		exportAct.putValue(DropDownButton.BUTTON_POPUP, saveMenu);
		
		exportButton = new FlatButton(exportAct);
		exportButton.setFocusable(false);
		exportButton.setEnabled(false);

		final PhonUIAction<Void> speakerVisibilityAct = PhonUIAction.runnable(() -> {
			speakerMenu.show(speakerButton, 0, speakerButton.getHeight());
		});
		speakerVisibilityAct.putValue(PhonUIAction.NAME, "Participants");
		speakerVisibilityAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Show participant visibility menu");
//		final ImageIcon userIcon =
//				IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "group", IconSize.MEDIUM, UIManager.getColor("Button.foreground"));
//		speakerVisibilityAct.putValue(PhonUIAction.SMALL_ICON, userIcon);
		speakerVisibilityAct.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
		speakerVisibilityAct.putValue(FlatButton.ICON_NAME_PROP, "group");
		speakerVisibilityAct.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);
		speakerVisibilityAct.putValue(DropDownButton.BUTTON_POPUP, speakerMenu);
		speakerVisibilityAct.putValue(DropDownButton.ARROW_ICON_POSITION, SwingUtilities.BOTTOM);
		speakerVisibilityAct.putValue(DropDownButton.ARROW_ICON_GAP, 2);
		
		speakerButton = new FlatButton(speakerVisibilityAct);
		speakerButton.setFocusable(false);

		tierVisibilityMenu = new JPopupMenu();
		tierVisibilityMenu.addPopupMenuListener(new PopupMenuListener() {
			
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				tierVisibilityMenu.removeAll();
				recordGrid.setupTierMenu(new MenuBuilder(tierVisibilityMenu));
			}
			
			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			}
			
			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {
			}
			
		});
		
		final PhonUIAction<Void> tierVisibilityAct = PhonUIAction.runnable(() -> {
			tierVisibilityMenu.show(tierVisibilityButton, 0, tierVisibilityButton.getHeight());
		});
		tierVisibilityAct.putValue(PhonUIAction.NAME, "Tiers");
		tierVisibilityAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Show tier visibility menu");
//		final ImageIcon tierIcon =
//				IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "view_list", IconSize.MEDIUM, UIManager.getColor("Button.foreground"));
//		tierVisibilityAct.putValue(PhonUIAction.SMALL_ICON, tierIcon);
		tierVisibilityAct.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
		tierVisibilityAct.putValue(FlatButton.ICON_NAME_PROP, "data_table");
		tierVisibilityAct.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);
		tierVisibilityAct.putValue(DropDownButton.BUTTON_POPUP, tierVisibilityMenu);
		tierVisibilityAct.putValue(DropDownButton.ARROW_ICON_POSITION, SwingConstants.BOTTOM);
		tierVisibilityAct.putValue(DropDownButton.ARROW_ICON_GAP, 2);
		
		tierVisibilityButton = new FlatButton(tierVisibilityAct);
		tierVisibilityButton.setFocusable(false);
		tierVisibilityButton.setBorderPainted(false);

		fontSizeMenu = new JPopupMenu();
		fontSizeMenu.addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				fontSizeMenu.removeAll();

				// setup font scaler
				final JLabel smallLbl = new JLabel("A");
				smallLbl.setFont(getFont().deriveFont(FontPreferences.getDefaultFontSize()));
				smallLbl.setHorizontalAlignment(SwingConstants.CENTER);
				JLabel largeLbl = new JLabel("A");
				largeLbl.setFont(getFont().deriveFont(FontPreferences.getDefaultFontSize()*2));
				largeLbl.setHorizontalAlignment(SwingConstants.CENTER);

				final JSlider scaleSlider = new JSlider(-8, 24);
				scaleSlider.setValue((int)recordGrid.getRecordGrid().getFontSizeDelta());
				scaleSlider.setMajorTickSpacing(8);
				scaleSlider.setMinorTickSpacing(2);
				scaleSlider.setSnapToTicks(true);
				scaleSlider.setPaintTicks(true);
				scaleSlider.addChangeListener( changeEvent -> {
					int sliderVal = scaleSlider.getValue();
					recordGrid.getRecordGrid().setFontSizeDelta(sliderVal);
				});

				JComponent fontComp = new JPanel(new HorizontalLayout());
				fontComp.add(smallLbl);
				fontComp.add(scaleSlider);
				fontComp.add(largeLbl);

				fontSizeMenu.add(fontComp);

				fontSizeMenu.addSeparator();

				final PhonUIAction<Float> useDefaultFontSizeAct = PhonUIAction.consumer(recordGrid.getRecordGrid()::setFontSizeDelta, 0.0f);
				useDefaultFontSizeAct.putValue(PhonUIAction.NAME, "Use default font size");
				useDefaultFontSizeAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Reset font size");
				fontSizeMenu.add(useDefaultFontSizeAct);
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {

			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {

			}
		});

		final PhonUIAction<Void> fontSizeAct = PhonUIAction.runnable(() -> {
			fontSizeMenu.show(fontSizeButton, 0, fontSizeButton.getHeight());
		});
		fontSizeAct.putValue(PhonUIAction.NAME, "Font size");
		fontSizeAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Show font size menu");
//		final ImageIcon fontIcon =
//				IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "text_increase", IconSize.MEDIUM, UIManager.getColor("Button.foreground"));
//		fontSizeAct.putValue(PhonUIAction.SMALL_ICON, fontIcon);
		fontSizeAct.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
		fontSizeAct.putValue(FlatButton.ICON_NAME_PROP, "text_increase");
		fontSizeAct.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);
		fontSizeAct.putValue(DropDownButton.BUTTON_POPUP, fontSizeMenu);
		fontSizeAct.putValue(DropDownButton.ARROW_ICON_POSITION, SwingConstants.BOTTOM);
		fontSizeAct.putValue(DropDownButton.ARROW_ICON_GAP, 2);

		fontSizeButton = new FlatButton(fontSizeAct);
		fontSizeButton.setFocusable(false);
		fontSizeButton.setText("");

		final ZoomAction zoomInAct = new ZoomAction(this, true);
		zoomInAct.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
		zoomInAct.putValue(FlatButton.ICON_NAME_PROP, "zoom_in");
		zoomInAct.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);
		zoomInButton = new FlatButton(zoomInAct);
		zoomInButton.setText("");

		final ZoomAction zoomOutAct = new ZoomAction(this, false);
		zoomOutAct.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
		zoomOutAct.putValue(FlatButton.ICON_NAME_PROP, "zoom_out");
		zoomOutAct.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);
		zoomOutButton = new FlatButton(zoomOutAct);
		zoomOutButton.setText("");

//		SplitRecordAction splitRecordAct = new SplitRecordAction(this);
//		JButton splitRecordButton = new JButton(splitRecordAct);
		
		toolbar.add(segmentationButton, IconStrip.IconStripPosition.LEFT);
		toolbar.add(playButton, IconStrip.IconStripPosition.LEFT);
		toolbar.add(exportButton, IconStrip.IconStripPosition.LEFT);
		toolbar.add(speakerButton, IconStrip.IconStripPosition.LEFT);
		toolbar.add(tierVisibilityButton, IconStrip.IconStripPosition.LEFT);
		toolbar.add(fontSizeButton, IconStrip.IconStripPosition.RIGHT);
		toolbar.add(zoomInButton, IconStrip.IconStripPosition.RIGHT);
		toolbar.add(zoomOutButton, IconStrip.IconStripPosition.RIGHT);
		
		return toolbar;
	}
	
	private int tierIdx = 0;
	private void addTier(TimelineTier tier) {
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 1.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = tierIdx++;
		
		tierPanel.add(tier, gbc);
	}
	
	private void update() {
		final SessionMediaModel mediaModel = getEditor().getMediaModel();
		if(mediaModel.isSessionAudioAvailable()) {
			loadSessionAudio();
			wavTier.setVisible(true);
			messageButton.setVisible(false);
		} else {
			wavTier.setVisible(false);
			getTimeModel().clearIntervals();
			getTimeModel().clearMarkers();
			
			messageButton.clearActions();

			// display option to generate audio file if there is session media available
			if(mediaModel.isSessionMediaAvailable()) {
				// show generate audio message
				messageButton.setTopLabelText("<html><b>Session audio file not available</b></html>");
				messageButton.setBottomLabelText("<html>Click here to generate audio (.wav) file from session media.</html>");

				messageButton.setDefaultAction(mediaModel.getGenerateSessionAudioAction());
				messageButton.addAction(mediaModel.getGenerateSessionAudioAction());
			} else {
				// no media, tell user to setup media in Session Information
				final AssignMediaAction browseForMediaAct = new AssignMediaAction(getEditor());
				browseForMediaAct.putValue(AssignMediaAction.LARGE_ICON_KEY, browseForMediaAct.getValue(AssignMediaAction.SMALL_ICON));

				messageButton.setDefaultAction(browseForMediaAct);
				messageButton.addAction(browseForMediaAct);

				messageButton.setTopLabelText("<html><b>Session media not available</b></html>");
				messageButton.setBottomLabelText("<html>Click here to assign media file to session.</html>");
			}
			messageButton.setVisible(true);
			messageButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		}
		setupTimeModel();
	}

	private float getMaxRecordTime() {
		float retVal = 0.0f;
		
		for(Record r:getEditor().getSession().getRecords()) {
			MediaSegment segment = r.getMediaSegment();
			if(segment != null) {
				float segEnd = segment.getEndTime();
				retVal = Math.max(segEnd, retVal);
			}
		}
		
		return retVal;
	}
	
	private void setupTimeModel() {
		float endTime = Math.max(10.0f, getMaxRecordTime());
		
		final SessionMediaModel mediaModel = getEditor().getMediaModel();
		if(mediaModel.isSessionMediaAvailable()) {
			if(mediaModel.isSessionAudioAvailable()) {
				endTime = Math.max(endTime, wavTier.getWaveformDisplay().getLongSound().length());
				timeModel.setMediaEndTime(wavTier.getWaveformDisplay().getLongSound().length());
			} else {
				// check if media is loaded in player, if so use time from player
				MediaPlayerEditorView mediaPlayerView = 
						(MediaPlayerEditorView)getEditor().getViewModel().getView(MediaPlayerEditorView.VIEW_NAME);
				if(mediaPlayerView.getPlayer().getMediaFile() != null) {
					endTime = Math.max(endTime, mediaPlayerView.getPlayer().getLength() / 1000.0f);
					timeModel.setMediaEndTime(mediaPlayerView.getPlayer().getLength()/1000.0f);
				}
			}
		}
		
		timeModel.setEndTime(endTime);
	}
	
	private void loadSessionAudio() {
		final SessionMediaModel mediaModel = getEditor().getMediaModel();
		try {
			LongSound ls = mediaModel.getSharedSessionAudio();
			//timeModel.setEndTime(ls.length());
			wavTier.getWaveformDisplay().setEndTime(ls.length());
			wavTier.getWaveformDisplay().setLongSound(ls);

			segmentationButton.setEnabled(mediaModel.isSessionMediaAvailable());

			PlaySegment playSeg = ls.getExtension(PlaySegment.class);
			playButton.setEnabled( playSeg != null );
			
			ExportSegment exportSeg = ls.getExtension(ExportSegment.class);
			exportButton.setEnabled( exportSeg != null );
		} catch (IOException e) {
			LogUtil.severe(e);
		}
	}
		
	public TimeUIModel getTimeModel() {
		return this.timeModel;
	}
	
	public void scrollToTime(float time) {
		var x = getTimeModel().xForTime(time);
		var rect = tierPanel.getVisibleRect();
		rect.x = (int)x;
		tierPanel.scrollRectToVisible(rect);
	}

	/**
	 * Centers view on given record
	 *
	 * @param r
	 */
	public void scrollToRecord(Record r) {
		MediaSegment seg = r.getMediaSegment();
		float time = seg.getStartTime();
		float windowLen = seg.getLength();
		
		float viewStart = getWindowStart();
		float viewEnd = getWindowEnd();
		float viewLen = viewEnd - viewStart;

		// entire segment can be viewed
		float delta = viewLen - windowLen;
		float newViewStart = Math.max(0, time - (delta/2.0f));

		if(newViewStart + viewLen > getTimeModel().getEndTime()) {
			newViewStart = getTimeModel().getEndTime() - viewLen;
		}

		scrollToTime(newViewStart);
	}
	
	public void scrollRectToVisible(Rectangle rect) {
		tierPanel.scrollRectToVisible(rect);
	}

	/**
	 * Get start time of visible timeline
	 *
	 * @return
	 */
	public float getWindowStart() {
		return getTimeModel().timeAtX(tierPanel.getVisibleRect().getX());
	}

	/**
	 * Get end time of visible timeline
	 *
	 * @return
	 */
	public float getWindowEnd() {
		return getTimeModel().timeAtX(tierPanel.getVisibleRect().getMaxX());
	}

	public double getWindowStartX() {
		return tierPanel.getVisibleRect().getX();
	}
	
	public double getWindowEndX() {
		return tierPanel.getVisibleRect().getMaxX();
	}
	
	public float getWindowLength() {
		return getWindowEnd() - getWindowStart();
	}
	
	private void setupExportMenu(MenuBuilder builder) {
		final PhonUIAction<Void> exportSelectionAct = PhonUIAction.runnable(this::exportSelection);
		exportSelectionAct.putValue(PhonUIAction.NAME, "Export selection...");
		exportSelectionAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Export selection (audio only)");

		final PhonUIAction<Void> exportSegmentAct = PhonUIAction.runnable(this::exportSegment);
		exportSegmentAct.putValue(PhonUIAction.NAME, "Export record segment...");
		exportSegmentAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Export record segment (audio only)");

		builder.addItem(".", exportSelectionAct).setEnabled(getWaveformTier().getSelection() != null);
		builder.addItem(".", exportSegmentAct).setEnabled(getRecordTier().currentRecordInterval() != null);
		builder.addSeparator(".", "s1");
		
		builder.addItem(".", new ExportCustomSegmentAction(getEditor()));
		builder.addItem(".", new ExportSpeechTurnAction(getEditor()));
		builder.addItem(".", new ExportAdjacencySequenceAction(getEditor()));
	}
	
	public void onExportSelectionOrSegment() {
		if(getWaveformTier().getSelection() != null) {
			exportSelection();
		} else {
			exportSegment();
		}
	}
	
	public void exportSelection() {
		if(getWaveformTier().getSelection() != null)
			exportInterval(getWaveformTier().getSelection());
	}
	
	public void exportSegment() {
		if(getRecordTier().currentRecordInterval() != null)
			exportInterval(getRecordTier().currentRecordInterval());
	}

	public void exportInterval(Interval interval) {
		exportInterval(interval.getStartMarker().getTime(), interval.getEndMarker().getTime());
	}
	
	public void exportInterval(float startTime, float endTime) {
		ExportSegmentAction exportAct = new ExportSegmentAction(getEditor(), startTime, endTime);
		exportAct.actionPerformed(new ActionEvent(this, -1, "export"));
	}
	
	private void setupPlaybackMenu(MenuBuilder builder) {
		if(isPlaying()) {
			final PhonUIAction<Void> stopAct = PhonUIAction.runnable(TimelineView.this::stopPlaying);
			stopAct.putValue(PhonUIAction.NAME, "Stop playback");
			stopAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Stop segment playback");
			final ImageIcon stopIcon = IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "stop", IconSize.SMALL, UIManager.getColor("Button.foreground"));
			stopAct.putValue(PhonUIAction.SMALL_ICON, stopIcon);

			builder.addItem(".", stopAct);
			builder.addSeparator(".", "stop");
		}
		
		final PhonUIAction<Void> playSelectionAct = PhonUIAction.runnable(TimelineView.this::playSelection);
		playSelectionAct.putValue(PhonUIAction.NAME, "Play selection");
		playSelectionAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Play current selection");
		final ImageIcon playIcon = IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "play_arrow", IconSize.SMALL, UIManager.getColor("Button.foreground"));
		playSelectionAct.putValue(PhonUIAction.SMALL_ICON, playIcon);
		final JMenuItem playSelectionItem = new JMenuItem(playSelectionAct);
		playSelectionItem.setEnabled( getWaveformTier().getSelection() != null );
		
		final PhonUIAction<Void> playSegmentAct = PhonUIAction.runnable(TimelineView.this::playSegment);
		playSegmentAct.putValue(PhonUIAction.NAME, "Play record segment");
		playSegmentAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Play current record segment");
		final ImageIcon playSegmentIcon = IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "play_arrow", IconSize.SMALL, UIManager.getColor("Button.foreground"));
		playSegmentAct.putValue(PhonUIAction.SMALL_ICON, playSegmentIcon);
		final JMenuItem playSegmentItem = new JMenuItem(playSegmentAct);
		playSegmentItem.setEnabled( getRecordTier().currentRecordInterval() != null );
		
		builder.addItem(".", playSelectionItem);
		builder.addItem(".", playSegmentItem);
		builder.addSeparator(".", "s1");
		
		builder.addItem(".", new PlayCustomSegmentAction(getEditor()));
		builder.addItem(".", new PlayAdjacencySequenceAction(getEditor()));
		builder.addItem(".", new PlaySpeechTurnAction(getEditor()));
	}
	
	public void playPause() {
		if(isPlaying()) {
			stopPlaying();
		} else if(getWaveformTier().getSelection() != null) {
			playSelection();
		} else if(getRecordTier().currentRecordInterval() != null) {
			playSegment();
		}
	}
	
	public void playSelection() {
		if(getWaveformTier().getSelection() != null)
			playInterval(getWaveformTier().getSelection());
	}
	
	public void playSegment() {
		if(getRecordTier().currentRecordInterval() != null)
			playInterval(getRecordTier().currentRecordInterval());
	}
	
	private void playInterval(Interval interval) {
		playInterval(interval.getStartMarker().getTime(), interval.getEndMarker().getTime());
	}
	
	private void playInterval(float startTime, float endTime) {
		SessionMediaModel mediaModel = getEditor().getMediaModel();
		mediaModel.getSegmentPlayback().playSegment(startTime, endTime);
	}
	
	public boolean isPlaying() {
		SessionMediaModel mediaModel = getEditor().getMediaModel();
		return mediaModel.getSegmentPlayback().isPlaying();
	}
	
	public void stopPlaying() {
		SessionMediaModel mediaModel = getEditor().getMediaModel();
		mediaModel.getSegmentPlayback().stopPlaying();
	}
	
	/* Editor actions */
	private void registerEditorEvents() {
		getEditor().getEventManager().registerActionForEvent(EditorEventType.SessionChanged, this::onSessionChanged, EditorEventManager.RunOn.AWTEventDispatchThread);

		getEditor().getEventManager().registerActionForEvent(EditorEventType.RecordChanged, this::onRecordChanged, EditorEventManager.RunOn.AWTEventDispatchThread);
		getEditor().getEventManager().registerActionForEvent(EditorEventType.RecordRefresh, this::onRecordChanged, EditorEventManager.RunOn.AWTEventDispatchThread);
		getEditor().getEventManager().registerActionForEvent(EditorEventType.SessionMediaChanged, this::onMediaChanged, EditorEventManager.RunOn.AWTEventDispatchThread);
		getEditor().getEventManager().registerActionForEvent(SessionMediaModel.SessionAudioAvailable, this::onSessionAudioAvailable, EditorEventManager.RunOn.AWTEventDispatchThread);
		
		getEditor().getEventManager().registerActionForEvent(SegmentationHandler.SegmentationStarted, this::onSegmentationStarted, EditorEventManager.RunOn.AWTEventDispatchThread);
		getEditor().getEventManager().registerActionForEvent(SegmentationHandler.SegmentationStopped, this::onSegmentationEnded, EditorEventManager.RunOn.AWTEventDispatchThread);
		
		getEditor().getEventManager().registerActionForEvent(EditorEventType.RecordAdded, this::onRecordAdded, EditorEventManager.RunOn.AWTEventDispatchThread);
		getEditor().getEventManager().registerActionForEvent(EditorEventType.RecordDeleted, this::onRecordDeleted, EditorEventManager.RunOn.AWTEventDispatchThread);
		getEditor().getEventManager().registerActionForEvent(EditorEventType.TierChange, this::onTierChanged, EditorEventManager.RunOn.AWTEventDispatchThread);
		
		getEditor().getEventManager().registerActionForEvent(MediaPlayerEditorView.MediaLoaded, this::onMediaLoaded, EditorEventManager.RunOn.AWTEventDispatchThread);
	}
	
	private void deregisterEditorEvents() {
		getEditor().getEventManager().removeActionForEvent(EditorEventType.SessionChanged, this::onSessionChanged);

		getEditor().getEventManager().removeActionForEvent(EditorEventType.RecordChanged, this::onRecordChanged);
		getEditor().getEventManager().removeActionForEvent(EditorEventType.SessionMediaChanged, this::onMediaChanged);
		getEditor().getEventManager().removeActionForEvent(SessionMediaModel.SessionAudioAvailable, this::onSessionAudioAvailable);
		
		getEditor().getEventManager().removeActionForEvent(SegmentationHandler.SegmentationStarted, this::onSegmentationStarted);
		getEditor().getEventManager().removeActionForEvent(SegmentationHandler.SegmentationStopped, this::onSegmentationEnded);
		
		getEditor().getEventManager().removeActionForEvent(EditorEventType.RecordAdded, this::onRecordAdded);
		getEditor().getEventManager().removeActionForEvent(EditorEventType.RecordDeleted, this::onRecordDeleted);
		getEditor().getEventManager().removeActionForEvent(EditorEventType.TierChange, this::onTierChanged);
		
		getEditor().getEventManager().removeActionForEvent(MediaPlayerEditorView.MediaLoaded, this::onMediaLoaded);
	}

	private void onSessionChanged(EditorEvent<Session> ee) {
		update();
	}
	
	private void onMediaLoaded(EditorEvent<MediaPlayerEditorView> ee) {
		setupTimeModel();
		MediaPlayerEditorView mediaPlayerView = ee.data();
		if(mediaPlayerView.getPlayer().getMediaFile() != null) {
			mediaPlayerView.getPlayer().addMediaPlayerListener(playbackMarkerSyncListener);
		}
	}
	
	private void onMediaChanged(EditorEvent<EditorEventType.SessionMediaChangedData> ee) {
		update();
	}

	private void onRecordChanged(EditorEvent<EditorEventType.RecordChangedData> ee) {
		Record r = getEditor().currentRecord();
		
		if(getEditor().getViewModel().isShowing(MediaPlayerEditorView.VIEW_NAME)) {
			MediaPlayerEditorView mediaPlayerView = (MediaPlayerEditorView)getEditor().getViewModel().getView(MediaPlayerEditorView.VIEW_NAME);
			// don't adjust scroll position while playing
			if(mediaPlayerView.getPlayer().isPlaying())
				return;
		}

		if(r != null && !recordGrid.isDraggingRecord() ) {
			scrollToRecord(r);
		}
	}
	
	private void onSessionAudioAvailable(EditorEvent<SessionMediaModel> ee) {
		update();
	}
	
	private void onSegmentationStarted(EditorEvent<SegmentationHandler> ee) {
		segmentationButton.setText("Stop Segmentation");
		final ImageIcon stopIcon = IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "stop", IconSize.MEDIUM, UIManager.getColor("Button.foreground"));
		segmentationButton.setIcon(stopIcon);
		
		if(mediaPlayerPlaybackMarker != null)
			timeModel.removeMarker(mediaPlayerPlaybackMarker);
	}
	
	private void onSegmentationEnded(EditorEvent<SegmentationHandler> ee) {
		segmentationButton.setText("Start Segmentation");
		final ImageIcon segmentationIcon = IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "splitscreen_vertical_add", IconSize.MEDIUM, UIManager.getColor("Button.foreground"));
		segmentationButton.setIcon(segmentationIcon);

		getEditor().putExtension(SegmentationHandler.class, null);

		repaint();
	}
	
	private void onRecordAdded(EditorEvent<EditorEventType.RecordAddedData> ee) {
		setupTimeModel();
		repaint();
	}
	
	private void onRecordDeleted(EditorEvent<EditorEventType.RecordDeletedData> ee) {
		setupTimeModel();
		repaint();
	}
	
	private void onTierChanged(EditorEvent<EditorEventType.TierChangeData> ee) {
		if(ee.data().valueAdjusting()) return;
		String tierName = ee.data().tier().getName();
		if(SystemTierType.Segment.getName().equals(tierName)) {
			setupTimeModel();
		}
	}
	
	public void toggleSegmentation() {
		SegmentationHandler handler = getEditor().getExtension(SegmentationHandler.class);
		if(handler != null) {
			handler.stopSegmentation();
		} else {
			SessionMediaModel mediaModel = getEditor().getMediaModel();
			if(!mediaModel.isSessionMediaAvailable()) {
				CommonModuleFrame.getCurrentFrame().showMessageDialog("Unable to start segmentation",
						"Please assign session media file before starting segmentation.",
						MessageDialogProperties.okOptions);
				return;
			}

			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			SegmentationDialog startDialog = new SegmentationDialog(getEditor());
			startDialog.setModal(true);
			startDialog.pack();
			startDialog.setSize(Math.min(800, screenSize.width), Math.min(600, screenSize.height));
			startDialog.setLocationRelativeTo(getEditor());
			startDialog.setVisible(true);
			
			if(!startDialog.wasCanceled()) {
				handler = new SegmentationHandler(getEditor());
				handler.setMediaStart(startDialog.getMediaStart());
				handler.setParticipantForMediaStart(startDialog.getSelectedParticipant());
				handler.setSegmentationMode(startDialog.getSegmentationMode());
				handler.getWindow().setBackwardWindowLengthMs(startDialog.getWindowLength());

				// check for speech analysis view
				if(getEditor().getViewModel().isShowing(SpeechAnalysisEditorView.VIEW_NAME)) {
					final MessageDialogProperties props = new MessageDialogProperties();
					props.setHeader("Segmentation Performance");
					props.setMessage("The Speech Analysis view may cause performance issues with segmentation.");
					props.setOptions(new String[] {
							"Close Speech Analysis and continue", "Continue without closing", "Cancel" });
					props.setRunAsync(false);
					int result = NativeDialogs.showMessageDialog(props);
					switch(result) {
					case 0:
						getEditor().getViewModel().getCloseAction(SpeechAnalysisEditorView.VIEW_NAME).actionPerformed(new ActionEvent(this, -1, "close"));
						break;
						
					case 1:
						break;
						
					case 2:
					default:
						return;
					}
				}
				
				getEditor().putExtension(SegmentationHandler.class, handler);
				handler.startSegmentation();
			}
		}
	}

	public MouseListener getContextMenuListener() {
		return this.contextMenuListener;
	}

	public void addMenuHandler(BiConsumer<MenuBuilder, Boolean> handler) {
		this.menuHandlers.add(handler);
	}

	public boolean removeMenuHandler(Consumer<MenuBuilder> handler) {
		return this.menuHandlers.remove(handler);
	}
	
	@Override
	public JMenu getMenu() {
		JMenu menu = new JMenu();
		
		MenuBuilder builder = new MenuBuilder(menu);

		// setup media actions
		SessionMediaModel mediaModel = getEditor().getMediaModel();
		if(mediaModel.isSessionMediaAvailable()) {
			if(!mediaModel.isSessionAudioAvailable()) {
				// Add generate audio action
				GenerateSessionAudioAction genAudioAct = new GenerateSessionAudioAction(getEditor());
				builder.addItem(".", genAudioAct);
			} else {
				wavTier.setupContextMenu(builder, false);
			}
		} else {
			// Add browse for media action
			builder.addItem(".", new AssignMediaAction(getEditor()));
		}
		
		builder.addSeparator(".", "record_grid");
		
		recordGrid.setupContextMenu(builder, false);
		
		builder.addSeparator(".", "segmentation");
		
		PhonUIAction<Void> segmentationAction = PhonUIAction.runnable(this::toggleSegmentation);
		if(getEditor().getExtension(SegmentationHandler.class) != null) {
			segmentationAction.putValue(PhonUIAction.NAME, "Stop Segmentation");
			final ImageIcon stopIcon = IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "stop", IconSize.SMALL, UIManager.getColor("Button.foreground"));
			segmentationAction.putValue(PhonUIAction.SMALL_ICON, stopIcon);
		} else {
			segmentationAction.putValue(PhonUIAction.NAME, "Start Segmentation");
			final ImageIcon segmentationIcon = IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "splitscreen_vertical_add", IconSize.SMALL, UIManager.getColor("Button.foreground"));
			segmentationAction.putValue(PhonUIAction.SMALL_ICON, segmentationIcon);
		}
		builder.addItem(".", segmentationAction);
		
		builder.addSeparator(".", "visiblity");

		JMenu participantMenu = builder.addMenu(".", "Participants");
		final ImageIcon usersIcon = IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "group", IconSize.SMALL, UIManager.getColor("Button.foreground"));
		participantMenu.setIcon(usersIcon);
		recordGrid.setupSpeakerMenu(new MenuBuilder(participantMenu));

		JMenu tierMenu = builder.addMenu(".", "Tiers");
		final ImageIcon tierIcon = IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "data_table", IconSize.SMALL, UIManager.getColor("Button.foreground"));
		tierMenu.setIcon(tierIcon);
		recordGrid.setupTierMenu(new MenuBuilder(tierMenu));
		
		builder.addSeparator(".", "zoom");
		
		builder.addItem(".", new ZoomAction(this, true));
		builder.addItem(".", new ZoomAction(this, false));

		for(int i = 0; i < menuHandlers.size(); i++) {
			if(i == 0) builder.addSeparator(".", "plugins");
			menuHandlers.get(i).accept(builder, false);
		}
		
		return menu;
	}

	private void showContextMenu(MouseEvent me) {
		JPopupMenu contextMenu = new JPopupMenu();
		
		MenuBuilder builder = new MenuBuilder(contextMenu);
		
		wavTier.setupContextMenu(builder, true);
		builder.addSeparator(".", "wav");
		recordGrid.setupContextMenu(builder, true);
		
		builder.addSeparator(".", "segmentation");
		
		PhonUIAction<Void> segmentationAction = PhonUIAction.runnable(this::toggleSegmentation);
		if(getEditor().getExtension(SegmentationHandler.class) != null) {
			segmentationAction.putValue(PhonUIAction.NAME, "Stop Segmentation");
			final ImageIcon stopIcon = IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "stop", IconSize.SMALL, UIManager.getColor("Button.foreground"));
			segmentationAction.putValue(PhonUIAction.SMALL_ICON, stopIcon);
		} else {
			segmentationAction.putValue(PhonUIAction.NAME, "Start Segmentation");
			final ImageIcon segmentationIcon = IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "splitscreen_vertical_add", IconSize.SMALL, UIManager.getColor("Button.foreground"));
			segmentationAction.putValue(PhonUIAction.SMALL_ICON, segmentationIcon);
		}
		builder.addItem(".", segmentationAction);
		
		builder.addSeparator(".", "visiblity");

		JMenu participantMenu = builder.addMenu(".", "Participants");
		final ImageIcon usersIcon = IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "group", IconSize.SMALL, UIManager.getColor("Button.foreground"));
		participantMenu.setIcon(usersIcon);
		recordGrid.setupSpeakerMenu(new MenuBuilder(participantMenu));

		JMenu tierMenu = builder.addMenu(".", "Tiers");
		final ImageIcon tierIcon = IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "data_table", IconSize.SMALL, UIManager.getColor("Button.foreground"));
		tierMenu.setIcon(tierIcon);
		recordGrid.setupTierMenu(new MenuBuilder(tierMenu));
		
		builder.addSeparator(".", "zoom");
		
		builder.addItem(".", new ZoomAction(this, true));
		builder.addItem(".", new ZoomAction(this, false));

		for(int i = 0; i < menuHandlers.size(); i++) {
			if(i == 0) builder.addSeparator(".", "plugins");
			menuHandlers.get(i).accept(builder, true);
		}
		
		contextMenu.show(me.getComponent(), me.getX(), me.getY());
	}
		
	@Override
	public String getName() {
		return VIEW_NAME;
	}

	@Override
	public ImageIcon getIcon() {
		final String[] iconData = VIEW_ICON.split(":");
		return IconManager.getInstance().getFontIcon(iconData[0], iconData[1], IconSize.MEDIUM, Color.darkGray);
	}
	
	public void repaint() {
		super.repaint();
	}
	
	public void repaintVisible() {
		timebar.repaint(timebar.getVisibleRect());
		getWaveformTier().repaint(getWaveformTier().getVisibleRect());
		getRecordTier().repaint(getRecordTier().getVisibleRect());
	}
	
	public void repaintInterval(Interval interval) {
		timebar.repaintInterval(interval);
		getWaveformTier().repaintInterval(interval);
		getRecordTier().repaintInterval(interval);
	}
	
	public void repaint(long tn, float startTime, float endTime) {
		timebar.repaint(tn, startTime, endTime);
		getWaveformTier().getWaveformDisplay().repaint(tn, startTime, endTime);
		getRecordTier().getRecordGrid().repaint(tn, startTime, endTime);
	}
	
	private final EditorViewAdapter editorViewListener = new EditorViewAdapter() {

		@Override
		public void onOpened(EditorView view) {
			registerEditorEvents();
			
			// scroll to current record position
			SwingUtilities.invokeLater(() -> {
				Record r = getEditor().currentRecord();
				if(r != null) {
					scrollToRecord(r);
//					MediaSegment seg = r.getSegment().getGroup(0);
//					if(seg != null && seg.getEndValue() - seg.getStartValue() > 0) {
//						
//						Rectangle2D segRect = new Rectangle2D.Double(
//								timeModel.xForTime(seg.getStartValue()/1000.0f), 0,
//								timeModel.xForTime(seg.getEndValue()/1000.0f) - timeModel.xForTime(seg.getStartValue()/1000.0f), 1);
//						if(!segRect.intersects(recordGrid.getVisibleRect())) {
//							scrollToTime(seg.getStartValue() / 1000.0f);
//						}
//					}
				}
			});
		}

		@Override
		public void onClosed(EditorView view) {
			deregisterEditorEvents();
		}
		
	};
	
	private PropertyChangeListener segmentPlaybackListener = new PropertyChangeListener() {
		
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			SegmentPlayback segmentPlayback = (SegmentPlayback)evt.getSource();
			if(SegmentPlayback.PLAYBACK_PROP.contentEquals(evt.getPropertyName())) {
				if(segmentPlayback.isPlaying()) {
					segmentPlaybackMarker = timeModel.addMarker(segmentPlayback.getTime(), UIManager.getColor(SpeechAnalysisViewColors.PLAYBACK_MARKER_COLOR));
					segmentPlaybackMarker.setOwner(getWaveformTier().getWaveformDisplay());
					segmentPlaybackMarker.setDraggable(false);
					
					if(mediaPlayerPlaybackMarker != null) {
						timeModel.removeMarker(mediaPlayerPlaybackMarker);
					}

					final ImageIcon stopIcon = IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "stop", IconSize.SMALL, UIManager.getColor("Button.foreground"));
					playButton.setIcon(stopIcon);
					playButton.setText("Stop playback");
				} else {
					if(segmentPlaybackMarker != null)
						timeModel.removeMarker(segmentPlaybackMarker);
					segmentPlaybackMarker = null;

					if(mediaPlayerPlaybackMarker != null)
						timeModel.addMarker(mediaPlayerPlaybackMarker);

					final ImageIcon playIcon = IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "play_arrow", IconSize.SMALL, UIManager.getColor("Button.foreground"));
					playButton.setIcon(playIcon);
					playButton.setText("Play segment");
				}
			} else if(SegmentPlayback.TIME_PROP.contentEquals(evt.getPropertyName())) {
				if(segmentPlaybackMarker != null) {
					segmentPlaybackMarker.setTime((float)evt.getNewValue());
				}
			}
		}
		
	};
	
	private class PlaybackMarkerSyncListener extends MediaPlayerEventAdapter {

		private Timer playbackTimer;
		
		private PlaybackMarkerTask playbackMarkerTask;
		
		@Override
		public void playing(MediaPlayer mediaPlayer) {
			if(mediaPlayerPlaybackMarker == null && getEditor().getExtension(SegmentationHandler.class) == null) {
				// only show playback marker if media player is playing audio
				if(mediaPlayer.audio().isMute()) return;
				
				float currentTime = (float)TimeUIModel.roundTime(mediaPlayer.status().time() / 1000.0f);
				
				mediaPlayerPlaybackMarker = timeModel.addMarker(currentTime, Color.darkGray);
				mediaPlayerPlaybackMarker.setOwner(wavTier.getWaveformDisplay());
				
				playbackMarkerTask = new PlaybackMarkerTask(mediaPlayerPlaybackMarker);
				playbackMarkerTask.mediaSyncTime = mediaPlayer.status().time();
				playbackMarkerTask.startTime = System.currentTimeMillis();
			}
		}

		@Override
		public void paused(MediaPlayer mediaPlayer) {
			if(mediaPlayerPlaybackMarker != null)
				SwingUtilities.invokeLater( () -> {
					timeModel.removeMarker(mediaPlayerPlaybackMarker);
					mediaPlayerPlaybackMarker = null;
					playbackTimer = null;
					
					if(playbackMarkerTask != null) {
						playbackMarkerTask.cancel();
						playbackMarkerTask = null;
					}
				});
		}
		
		@Override
		public void timeChanged(MediaPlayer mediaPlayer, long newTime) {
			if(playbackMarkerTask != null) {
				// sync playback time
				playbackMarkerTask.mediaSyncTime = newTime;
				playbackMarkerTask.startTime = System.currentTimeMillis();
				if(playbackTimer == null) {
					playbackTimer = new Timer(true);
					playbackTimer.schedule(playbackMarkerTask, 0L, (long)(1/playbackMarkerFps * 1000.0f));
				}
			}
		}
		
	}
	
	private class PlaybackMarkerTask extends TimerTask {

		volatile long startTime;
		
		volatile long mediaSyncTime = 0L;
		
		TimeUIModel.Marker playbackMarker;
		
		public PlaybackMarkerTask(TimeUIModel.Marker playbackMarker) {
			super();
			this.playbackMarker = playbackMarker;
		}
		
		@Override
		public void run() {
			if(playbackMarker != null) {
				long currentTime = System.currentTimeMillis();
				long newTime = mediaSyncTime + (long)Math.round((currentTime - startTime) * getEditor().getMediaModel().getPlaybackRate());
				
				playbackMarker.setTime((float)TimeUIModel.roundTime(newTime / 1000.0f));
			} else {
				cancel();
			}
		}
		
	}

	private class SeparatorMouseListener extends MouseInputAdapter {
		
		private TimelineTier tier;
		
		private boolean valueAdjusting = false;
		
		public SeparatorMouseListener(TimelineTier tier) {
			super();
			
			this.tier = tier;
		}
		
		public TimelineTier getTier() {
			return this.tier;
		}
		
		@Override
		public void mousePressed(MouseEvent e) {
			valueAdjusting = true;
			((JComponent)e.getSource()).firePropertyChange("valueAdjusting", false, valueAdjusting);
		}
		
		@Override
		public void mouseReleased(MouseEvent e) {
			valueAdjusting = false;
			((JComponent)e.getSource()).firePropertyChange("valueAdjusting", true, valueAdjusting);
		}
		
		@Override
		public void mouseDragged(MouseEvent e) {
			Dimension currentSize = tier.getSize();
			Dimension prefSize = tier.getPreferredSize();

			prefSize.height = currentSize.height + e.getY();
			if(prefSize.height < 0) prefSize.height = 0;
			
			tier.setPreferredSize(prefSize);
			tierPanel.revalidate();
		}
		
	}
	
	private MouseListener contextMenuListener = new MouseAdapter() {

		@Override
		public void mousePressed(MouseEvent e) {
			if(e.isPopupTrigger()) {
				showContextMenu(e);
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			if(e.isPopupTrigger()) {
				showContextMenu(e);
			}
		}
		
	};
	
	private class TierPanel extends JPanel implements Scrollable {
		
		public TierPanel() {
			super();
			setOpaque(false);
		}

		public TierPanel(boolean isDoubleBuffered) {
			super(isDoubleBuffered);
		}

		public TierPanel(LayoutManager layout, boolean isDoubleBuffered) {
			super(layout, isDoubleBuffered);
		}

		public TierPanel(LayoutManager layout) {
			super(layout);
		}

		@Override
		public Dimension getPreferredScrollableViewportSize() {
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
			return 10;
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
			return (int)visibleRect.getWidth();
		}

		@Override
		public boolean getScrollableTracksViewportWidth() {
			return false;
		}

		@Override
		public boolean getScrollableTracksViewportHeight() {
			return false;
		}
		
	}
	
}
