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

import ca.hedlund.desktopicons.MacOSStockIcon;
import ca.hedlund.desktopicons.StockIcon;
import ca.hedlund.desktopicons.WindowsStockIcon;
import ca.phon.app.log.LogUtil;
import ca.phon.app.menu.edit.EditMenuModifier;
import ca.phon.app.menu.edit.PreferencesCommand;
import ca.phon.app.session.editor.actions.*;
import ca.phon.app.session.editor.undo.SessionEditUndoSupport;
import ca.phon.app.session.editor.view.mediaPlayer.MediaPlayerEditorView;
import ca.phon.extensions.ExtensionSupport;
import ca.phon.extensions.IExtendable;
import ca.phon.media.VolumeModel;
import ca.phon.project.Project;
import ca.phon.session.Record;
import ca.phon.session.*;
import ca.phon.session.io.*;
import ca.phon.syllabifier.SyllabifierLibrary;
import ca.phon.ui.*;
import ca.phon.ui.action.PhonUIAction;
import ca.phon.ui.nativedialogs.MessageDialogProperties;
import ca.phon.ui.nativedialogs.NativeDialogs;
import ca.phon.ui.toast.ToastFactory;
import ca.phon.util.ByteSize;
import ca.phon.util.Language;
import ca.phon.util.OSInfo;
import ca.phon.util.PrefHelper;
import ca.phon.util.icons.IconManager;
import ca.phon.util.icons.IconSize;
import org.jdesktop.swingx.JXStatusBar;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEditSupport;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>Main UI for the application.  This window provides the interface for
 * creating and modifying Phon {@link Session}s.</p>
 *
 * <p>This window supports plug-ins.  Plug-ins can provide custom EditorViews.</p>
 */
public class SessionEditor extends JPanel implements IExtendable, ClipboardOwner {

	public final static String BACKUP_WHEN_SAVING =
			SessionEditor.class.getName() + ".backupWhenSaving";
	private boolean backupWhenSaving = PrefHelper.getBoolean(BACKUP_WHEN_SAVING, Boolean.TRUE);

	private final static String TITLE = "Session Editor";

	/**
	 * Extension support
	 */
	private final ExtensionSupport extensionSupport = new ExtensionSupport(SessionEditor.class, this);

	/**
	 * UI Model
	 */
	private final transient AtomicReference<EditorViewModel> viewModelRef;

	/**
	 * Data model
	 */
	private final transient AtomicReference<EditorDataModel> dataModelRef;

	/**
	 * Event manager
	 */
	private final transient AtomicReference<EditorEventManager> eventManagerRef;

	/**
	 * Selection model
	 */
	private final transient AtomicReference<EditorSelectionModel> selectionModelRef;
	
	/**
	 * Media model
	 */
	private final transient AtomicReference<SessionMediaModel> mediaModelRef;

	/**
	 * Index of the current record
	 *
	 */
	private volatile transient int currentRecord = 0;

	/*
	 * Undo/Redo support
	 */
	/**
	 * Undo support for the editor
	 */
	private final SessionEditUndoSupport undoSupport = new SessionEditUndoSupport();

	/**
	 * Undo manager
	 */
	private final UndoManager undoManager = new UndoManager();

	/**
	 * Save button
	 */
	private FlatButton saveButton;


	/**
	 * Left view icon strip
	 */
	private ViewIconStrip leftIconStrip;

	/**
	 * Right view icon strip
	 */
	private ViewIconStrip rightIconStrip;

	private final UndoableEditListener undoListener = new UndoableEditListener() {

		@Override
		public void undoableEditHappened(UndoableEditEvent e) {
			undoManager.addEdit(e.getEdit());
			setModified(true);
		}

	};
	
	/**
	 * Toolbar
	 */
	private SessionEditorToolbar toolbar;

	/**
	 * Status bar
	 *
	 */
	private SessionEditorStatusBar statusBar;

	/**
	 * Editor title
	 */
	private String title;

	/**
	 * Modified
	 */
	private boolean modified = false;

	/**
	 * Editor finished loading.  This is expected to be set to true after all initial views
	 * have been loaded and the editor is on screen.  Classes which use SessionEditor should
	 * fire an {@link EditorEventType#EditorFinishedLoading} event.
	 */
	private volatile boolean finishedLoading = false;

	/**
	 * Constructor
	 */
	public SessionEditor(Project project, Session session, Transcriber transcriber) {
		super();
		putExtension(Session.class, session);

		this.dataModelRef =
				new AtomicReference<EditorDataModel>(new DefaultEditorDataModel(project, session));
		if(transcriber != null)
			getDataModel().setTranscriber(transcriber);
		this.eventManagerRef =
				new AtomicReference<EditorEventManager>(new EditorEventManager());
		this.selectionModelRef =
				new AtomicReference<EditorSelectionModel>(new DefaultEditorSelectionModel());
		this.viewModelRef =
				new AtomicReference<EditorViewModel>(new WorkingAreaEditorViewModel(this));
		this.mediaModelRef = 
				new AtomicReference<SessionMediaModel>(new SessionMediaModel(this));

		// check to ensure that the session has a tier view
		if(session.getTierView() == null || session.getTierView().size() == 0) {
			session.setTierView(SessionFactory.newFactory().createDefaultTierView(session));
		}

		// add default undo listener
		undoSupport.addUndoableEditListener(undoListener);

		// setup undo support and manager extensions
		putExtension(UndoManager.class, undoManager);
		putExtension(UndoableEditSupport.class, undoSupport);

		// setup syllabification info
		final SyllabifierInfo info = new SyllabifierInfo(session);
		final Language defaultSyllabifierLanguage = SyllabifierLibrary.getInstance().defaultSyllabifierLanguage();
		if(info.getSyllabifierLanguageForTier(SystemTierType.IPATarget.getName()) == null)
			info.setSyllabifierLanguageForTier(SystemTierType.IPATarget.getName(), defaultSyllabifierLanguage);
		if(info.getSyllabifierLanguageForTier(SystemTierType.IPAActual.getName()) == null)
			info.setSyllabifierLanguageForTier(SystemTierType.IPAActual.getName(), defaultSyllabifierLanguage);
		session.putExtension(SyllabifierInfo.class, info);

		init();
	}

	public String getTitle() {
		if (this.title == null) {
			return generateTitle();
		}
		return this.title;
	}

	private void init() {
		final BorderLayout layout = new BorderLayout();
		setLayout(layout);

		// window has 3 main elements: toolbar, dock area, status bar
		// each element is retrieved using the view model
		final EditorViewModel viewModel = getViewModel();

//		// toolbar
//		final SessionEditorToolbar tb = getToolbar();
//		add(tb, BorderLayout.NORTH);

		final SaveSessionAction saveSessionAction = new SaveSessionAction(this);
		saveSessionAction.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
		saveSessionAction.putValue(FlatButton.ICON_NAME_PROP, "SAVE");
		saveSessionAction.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM_LARGE);
		saveButton = new FlatButton(saveSessionAction);
		saveButton.setPadding(0);
		saveButton.setIconColor(UIManager.getColor(IconStripUIProps.ICON_STRIP_ICON_COLOR));
		saveButton.setIconHoverColor(UIManager.getColor(IconStripUIProps.ICON_STRIP_HOVER_COLOR));
		saveButton.setBgSelectedColor(UIManager.getColor(IconStripUIProps.ICON_STRIP_ICON_SELECTED_BACKGROUND));
		saveButton.setBgPressedColor(UIManager.getColor(IconStripUIProps.ICON_STRIP_ICON_PRESSED_BACKGROUND));
		saveButton.setIconSelectedColor(UIManager.getColor(IconStripUIProps.ICON_STRIP_ICON_SELECTED_COLOR));
		saveButton.setText("");
		saveButton.setToolTipText(null);
		saveButton.setPopupText("Save session");
		saveButton.setPopupLocation(SwingConstants.EAST);
		saveButton.setEnabled(isModified());

		// status bar
		final JXStatusBar sb = (JXStatusBar) getStatusBar();
		add(sb, BorderLayout.SOUTH);

		// setup content/dock area
		final Container dock = viewModel.getRoot();
		add(dock, BorderLayout.CENTER);

		// setup view icon strip
		leftIconStrip = new ViewIconStrip(viewModel) {
			@Override
			protected void initButtons() {
				add(saveButton, IconStripPosition.LEFT);
				add(new JToolBar.Separator(new Dimension(IconSize.MEDIUM_LARGE.getWidth(), 10)), IconStripPosition.LEFT);
				super.initButtons();
			}
		};
		leftIconStrip.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 0));
		add(leftIconStrip, BorderLayout.WEST);

		rightIconStrip = new ViewIconStrip(SwingConstants.RIGHT, viewModel);
		rightIconStrip.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 2));
		add(rightIconStrip, BorderLayout.EAST);

		setupEditorActions();
	}

	private void setupEditorActions() {
		final EditorAction<Session> sessionChangedAct = this::onSessionChanged;
		getEventManager().registerActionForEvent(EditorEventType.SessionChanged, sessionChangedAct, EditorEventManager.RunOn.AWTEventDispatchThread);

		final EditorAction<Boolean> modifiedChangedAct = this::onModifiedChanged;
		getEventManager().registerActionForEvent(EditorEventType.ModifiedFlagChanged, modifiedChangedAct, EditorEventManager.RunOn.AWTEventDispatchThread);

		final EditorAction<EditorEventType.RecordDeletedData> recordDeletedAct = this::onRecordDeleted;
		getEventManager().registerActionForEvent(EditorEventType.RecordDeleted, recordDeletedAct, EditorEventManager.RunOn.AWTEventDispatchThread);

		final EditorAction<Void> reloadFromDiskAct = this::onReloadSessionFromDisk;
		getEventManager().registerActionForEvent(EditorEventType.EditorReloadFromDisk, reloadFromDiskAct, EditorEventManager.RunOn.AWTEventDispatchThread);

		final EditorAction<Void> onClosingAct = this::onEditorClosing;
		getEventManager().registerActionForEvent(EditorEventType.EditorClosing, onClosingAct, EditorEventManager.RunOn.AWTEventDispatchThread);

		getEventManager().registerActionForEvent(EditorEventType.SessionMediaChanged, this::onSessionMediaChanged, EditorEventManager.RunOn.AWTEventDispatchThread);

		getEventManager().registerActionForEvent(EditorEventType.EditorFinishedLoading, this::onEditorFinishedLoading, EditorEventManager.RunOn.EditorEventDispatchThread);
	}

	private void onEditorFinishedLoading(EditorEvent<Void> ee) {
		var oldVal = this.finishedLoading;
		this.finishedLoading = true;
		firePropertyChange("finishedLoading", oldVal, finishedLoading);
	}

	/**
	 * Has the editor finished loading all initial views
	 *
	 * @return true if EditorEventType.EditorFinishedLoading has been called
	 */
	public boolean isFinishedLoading() {
		return this.finishedLoading;
	}

	public void setTitle(String title) {
		var oldTitle = this.title;
		this.title = title;
		firePropertyChange("title", oldTitle, title);
	}

//	/**
//	 * Get the editor toolbar.
//	 *
//	 * @return toolbar
//	 */
//	public SessionEditorToolbar getToolbar() {
//		if(this.toolbar == null) {
//			this.toolbar = new SessionEditorToolbar(this);
//		}
//		return this.toolbar;
//	}

	/**
	 * Get the editor status bar
	 *
	 * @return statusBar
	 */
	public SessionEditorStatusBar getStatusBar() {
		if(this.statusBar == null) {
			this.statusBar = new SessionEditorStatusBar(this);
		}
		return this.statusBar;
	}

	/*---- Menu Setup ------------------------------*/
	/**
	 * Retrieve the data model
	 *
	 * @return the editor data model
	 */
	public EditorDataModel getDataModel() {
		return dataModelRef.get();
	}

	/**
	 * Retrieve the view model
	 *
	 * @return the editor view model
	 */
	public EditorViewModel getViewModel() {
		return (viewModelRef != null ? viewModelRef.get() : null);
	}

	/**
	 * Get the event manager
	 *
	 * @return the editor event model
	 */
	public EditorEventManager getEventManager() {
		return (eventManagerRef != null ? eventManagerRef.get() : null);
	}

	/**
	 * Get the selection model
	 *
	 *
	 * @return editor selection model
	 */
	public EditorSelectionModel getSelectionModel() {
		return (selectionModelRef != null ? selectionModelRef.get() : null);
	}

	/**
	 * Get the media model
	 * 
	 * @return session media model
	 */
	public SessionMediaModel getMediaModel() {
		return (mediaModelRef != null ? mediaModelRef.get() : null);
	}

	public Project getProject() {
		return getDataModel().getProject();
	}
	
	/**
	 * Get session
	 *
	 * @return session
	 */
	public Session getSession() {
		final EditorDataModel dataModel = getDataModel();
		return (dataModel != null ? dataModel.getSession() : null);
	}

	/*
	 * RECORD POSITION
	 */
	/**
	 * Return the index of the current record.
	 *
	 * @return index of current record
	 */
	public int getCurrentRecordIndex() {
		return this.currentRecord;
	}

	/**
	 * Set the index of the current record.
	 *
	 * @return index
	 * @throws ArrayIndexOutOfBoundsException
	 */
	public void setCurrentRecordIndex(int index) {
		if(getDataModel().getRecordCount() > 0 && (index < 0 || index >= getDataModel().getRecordCount())) {
			throw new ArrayIndexOutOfBoundsException(index);
		}
		this.currentRecord = index;
		final EditorEvent<EditorEventType.RecordChangedData> ee = new EditorEvent<>(EditorEventType.RecordChanged, this,
				new EditorEventType.RecordChangedData(currentRecord(), getSession().getRecordElementIndex(this.currentRecord), this.currentRecord));
		getEventManager().queueEvent(ee);
	}

	/**
	 * Return the current record
	 *
	 * @return current record
	 */
	public Record currentRecord() {
		final EditorDataModel dataModel = getDataModel();
		if(dataModel == null) return null;
		return (getCurrentRecordIndex() >= 0 && getCurrentRecordIndex() < dataModel.getRecordCount()
				? dataModel.getRecord(getCurrentRecordIndex())
				: null);
	}

	/**
	 * Get undo support for the editor
	 *
	 * @return undo support
	 */
	public SessionEditUndoSupport getUndoSupport() {
		return this.undoSupport;
	}

	/**
	 * Get undo manager for the editor
	 *
	 * @return undo manager
	 */
	public UndoManager getUndoManager() {
		return this.undoManager;
	}

	/**
	 * Set the modified flag
	 *
	 * @param modified
	 */
	public void setModified(boolean modified) {
		final boolean lastVal = this.modified;
		this.modified = modified;
		if(lastVal != modified) {
			final EditorEvent<Boolean> ee = new EditorEvent(EditorEventType.ModifiedFlagChanged, this, modified);
			getEventManager().queueEvent(ee);
		}
		firePropertyChange("modified", lastVal, modified);
	}

	public boolean isModified() {
		return this.modified;
	}

	/**
	 * Generate the window title
	 *
	 * @return window title
	 */
	public String generateTitle() {
		final Session session = getSession();
		String retVal = TITLE;
		if(session != null) {
			final SessionPath sessionPath = getSession().getSessionPath();
			retVal += " : " + (sessionPath.toString());
			if(isModified())
				retVal += "*";
		}
		if(getDataModel().getTranscriber() != Transcriber.VALIDATOR) {
			retVal += " (Transcriber: " + getDataModel().getTranscriber().toString() + ")";
		}
		return retVal;
	}

	/*
	 * Editor actions
	 */
	private void onEditorClosing(EditorEvent<Void> ee) {
		if(getMediaModel().isSessionAudioAvailable()) {
			try {
				getMediaModel().getSharedSessionAudio().close();
			} catch (IOException e) {
				LogUtil.severe(e);
			}
		}
	}

	private void onSessionChanged(EditorEvent<Session> ee) {
		// reset media model
		this.mediaModelRef.set(new SessionMediaModel(this));

		// clear undo model
		undoManager.discardAllEdits();

		// update toolbar and record index
		if(this.currentRecord >= getDataModel().getRecordCount()) {
			this.currentRecord = getDataModel().getRecordCount()-1;
		}
		if(this.currentRecord < 0) {
			this.currentRecord = 0;
		}
//		remove(getToolbar());
//		this.toolbar = new SessionEditorToolbar(this);
//		add(this.toolbar, BorderLayout.NORTH);

		setTitle(generateTitle());
	}

	private void onModifiedChanged(EditorEvent<Boolean> eee) {
		final String title = generateTitle();
		setTitle(title);
		saveButton.setEnabled(isModified());
	}

	private void onRecordDeleted(EditorEvent ee) {
		if(getDataModel().getRecordCount() > 0 && getCurrentRecordIndex() >= getDataModel().getRecordCount()) {
			setCurrentRecordIndex(getDataModel().getRecordCount()-1);
		} else if(getDataModel().getRecordCount() == 0) {
			setCurrentRecordIndex(-1);
		} else {
			final EditorEvent<EditorEventType.RecordChangedData> refreshAct = new EditorEvent<>(EditorEventType.RecordRefresh, this,
					new EditorEventType.RecordChangedData(currentRecord(), getSession().getRecordElementIndex(currentRecord), this.currentRecord));
			getEventManager().queueEvent(refreshAct);
		}
	}

	/**
	 * Reload session data from  disk, this method does not display a warning dialog
	 * This method is called when EditorEventType.SESSION_CHANGED_ON_DISK is fired
	 *
	 * @param ee
	 */
	private void onReloadSessionFromDisk(EditorEvent<Void> ee) {
		final Project project = getDataModel().getProject();
		final Session currentSession = getSession();

		try {
			final Session reloadedSession = project.openSession(currentSession.getCorpus(), currentSession.getName());
			getDataModel().setSession(reloadedSession);
			getEventManager().queueEvent(new EditorEvent<>(EditorEventType.SessionChanged, this, reloadedSession));

			setModified(false);
		} catch (IOException e) {
			Toolkit.getDefaultToolkit().beep();
			LogUtil.severe(e);
			Objects.requireNonNull(CommonModuleFrame.getCurrentFrame()).showMessageDialog("Unable to reload session", e.getLocalizedMessage(), MessageDialogProperties.okOptions);
		}
	}

	public void onSessionMediaChanged(EditorEvent<EditorEventType.SessionMediaChangedData> ee) {
		getMediaModel().resetAudioCheck();
	}

	public void setupMenu(JMenuBar menuBar) {
		// get 'File' menu reference
		final JMenu fileMenu = menuBar.getMenu(0);
		final SaveSessionAction saveAct = new SaveSessionAction(this);
		final JMenuItem saveItem = new JMenuItem(saveAct);
		fileMenu.add(saveItem, 0);
		fileMenu.addMenuListener(new MenuListener() {

			@Override
			public void menuSelected(MenuEvent e) {
				saveItem.setEnabled(isModified());
			}

			@Override
			public void menuDeselected(MenuEvent e) {
			}

			@Override
			public void menuCanceled(MenuEvent e) {
			}

		});

		// save as.. menu
		final JMenu saveAsMenu = new JMenu("Save as...");
		final SessionOutputFactory factory = new SessionOutputFactory();
		for(SessionIO sessionIO:factory.availableSessionIOs()) {
			saveAsMenu.add(new JMenuItem(new SaveAsAction(this, sessionIO)));
		}
		fileMenu.add(saveAsMenu, 1);
		fileMenu.add(new JSeparator(), 2);

		fileMenu.add(new JMenuItem(new ExportAsHTMLAction(this)), 3);

		putExtension(EditMenuModifier.class, (editMenu) -> {
			editMenu.add(new JMenuItem(new FindAndReplaceAction(this)), 3);
			editMenu.add(new JSeparator(), 4);
		});

		// setup 'Session' menu
		final JMenu sessionMenu = new JMenu("Session");
		sessionMenu.add(new NewRecordAction(this));
		sessionMenu.add(new DuplicateRecordAction(this));
		sessionMenu.add(new DeleteRecordAction(this));
		sessionMenu.addSeparator();

		sessionMenu.add(new MoveRecordToBeginningAction(this));
		sessionMenu.add(new MoveRecordBackwardAction(this));
		sessionMenu.add(new MoveRecordForwardAction(this));
		sessionMenu.add(new MoveRecordToEndAction(this));
		sessionMenu.add(new SortRecordsAction(this));
		sessionMenu.addSeparator();

		sessionMenu.add(new CutRecordAction(this));
		sessionMenu.add(new CopyRecordAction(this));
		sessionMenu.add(new PasteRecordAction(this));
		sessionMenu.addSeparator();

		sessionMenu.add(new FirstRecordAction(this));
		sessionMenu.add(new PreviousRecordAction(this));
		sessionMenu.add(new NextRecordAction(this));
		sessionMenu.add(new LastRecordAction(this));

		if(this.getSession() != null) {
			sessionMenu.addSeparator();
			JMenuItem itrItem = new JMenuItem(new ITRAction(this));
			itrItem.setEnabled(this.getDataModel().getTranscriber() == Transcriber.VALIDATOR && this.getSession().getTranscriberCount() > 1);
			sessionMenu.add(itrItem);
		}

		// setup 'View' menu, this menu must be created dynamically
		// as the view model is not available when the menu bar is
		// setup
		final JMenu viewMenu = new JMenu("View");
		final MenuListener viewMenuListener = new MenuListener() {
			@Override
			public void menuSelected(MenuEvent e) {
				viewMenu.removeAll();
				SessionEditor.this.getViewModel().setupPerspectiveMenu(viewMenu);
				viewMenu.addSeparator();
				SessionEditor.this.getViewModel().setupViewMenu(viewMenu);
			}

			@Override
			public void menuDeselected(MenuEvent e) {
			}

			@Override
			public void menuCanceled(MenuEvent e) {
			}
		};
		viewMenu.addMenuListener(viewMenuListener);

		if(getViewModel() != null) {
			final MenuEvent me = new MenuEvent(viewMenu);
			viewMenuListener.menuSelected(me);
		}

		final JMenu mediaMenu = new JMenu("Media");
		final MenuListener mediaMenuListener = new MenuListener() {

			@Override
			public void menuSelected(MenuEvent e) {
				mediaMenu.removeAll();

				SessionMediaModel mediaModel = SessionEditor.this.getMediaModel();
				mediaMenu.add(new AssignMediaAction(SessionEditor.this));
				mediaMenu.add(new UnassignMediaAction(SessionEditor.this)).setEnabled(mediaModel.isSessionMediaAvailable());
				JMenuItem genAudioItem = new JMenuItem(mediaModel.getGenerateSessionAudioAction());
				genAudioItem.setEnabled(mediaModel.isSessionMediaAvailable());
				mediaMenu.add(genAudioItem);
				mediaMenu.add(new ShowMediaInfoAction(SessionEditor.this)).setEnabled(mediaModel.isSessionMediaAvailable());;
				mediaMenu.addSeparator();

				JMenu volumeMenu = new JMenu("Volume");
				volumeMenu.add(new JCheckBoxMenuItem(new ToggleMuteAction(SessionEditor.this)));
				volumeMenu.addSeparator();
				for(float level = 0.25f; level <= VolumeModel.MAX_LEVEL; level += 0.25f) {
					volumeMenu.add(new JMenuItem(new AdjustVolumeAction(SessionEditor.this, level)));
				}
				mediaMenu.add(volumeMenu);

				JMenu playbackRateMenu = new JMenu("Playback rate");
				for(float rate = 0.25f; rate <= 2.0f; rate += 0.25f) {
					playbackRateMenu.add(new JCheckBoxMenuItem(new AdjustPlaybackRate(SessionEditor.this, rate)));
				}
				mediaMenu.add(playbackRateMenu);

				mediaMenu.addSeparator();

				boolean enabled = (mediaModel.isSessionAudioAvailable() ||
						(mediaModel.isSessionMediaAvailable() && SessionEditor.this.getViewModel().isShowing(MediaPlayerEditorView.VIEW_NAME)));
				mediaMenu.add(new PlaySegmentAction(SessionEditor.this)).setEnabled(enabled);
				mediaMenu.add(new PlayCustomSegmentAction(SessionEditor.this)).setEnabled(enabled);
				mediaMenu.add(new PlaySpeechTurnAction(SessionEditor.this)).setEnabled(enabled);
				mediaMenu.add(new PlayAdjacencySequenceAction(SessionEditor.this)).setEnabled(enabled);
				mediaMenu.addSeparator();

				mediaMenu.add(new ExportSegmentAction(SessionEditor.this)).setEnabled(mediaModel.isSessionAudioAvailable());
				mediaMenu.add(new ExportCustomSegmentAction(SessionEditor.this)).setEnabled(mediaModel.isSessionAudioAvailable());
				mediaMenu.add(new ExportSpeechTurnAction(SessionEditor.this)).setEnabled(mediaModel.isSessionAudioAvailable());
				mediaMenu.add(new ExportAdjacencySequenceAction(SessionEditor.this)).setEnabled(mediaModel.isSessionAudioAvailable());
				mediaMenu.addSeparator();

				final StockIcon prefIcon =
						OSInfo.isMacOs() ? MacOSStockIcon.ToolbarCustomizeIcon
								: OSInfo.isWindows() ?  WindowsStockIcon.APPLICATION : null;
				final String defIcn = "categories/preferences";
				ImageIcon prefsIcn = IconManager.getInstance().getSystemStockIcon(prefIcon, defIcn, IconSize.SMALL);
				final PreferencesCommand prefsAct = new PreferencesCommand("Media");
				prefsAct.putValue(PhonUIAction.NAME, "Edit media folders...");
				prefsAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Modify global media folders...");
				prefsAct.putValue(PhonUIAction.SMALL_ICON, prefsIcn);
				mediaMenu.add(prefsAct);
			}

			@Override
			public void menuDeselected(MenuEvent e) {
			}

			@Override
			public void menuCanceled(MenuEvent e) {
			}

		};
		mediaMenu.addMenuListener(mediaMenuListener);

		menuBar.add(mediaMenu, 3);
		menuBar.add(viewMenu, 3);
		menuBar.add(sessionMenu, 3);
	}

	public boolean saveData()
			throws IOException {
		final Project project = getDataModel().getProject();
		final Session session = getSession();

		/*
		 * Check for an OriginalFormat extension, if found and not in Phon format
		 * ask if the users wishes to save in the original format or Phon's format.
		 */
		final SessionOutputFactory outputFactory = new SessionOutputFactory();

		// get default session writer
		SessionWriter sessionWriter = outputFactory.createWriter();
		OriginalFormat origFormat = session.getExtension(OriginalFormat.class);

		// check for non-native format
		if(origFormat != null && !origFormat.getSessionIO().group().equals("ca.phon")) {

			// only issue the format warning once...
			if(origFormat.isIssueWarning()) {
				final MessageDialogProperties props = new MessageDialogProperties();
				final String[] opts = {
						"Use original format (" + origFormat.getSessionIO().name() + ")",
						"Use phon format",
						"Cancel"
				};
				props.setOptions(opts);
				props.setDefaultOption(opts[0]);
				props.setHeader("Save session");
				props.setMessage("Use original format or save in Phon format? Some information such as tier font and ordering may not be saved if using the original format.");
				props.setRunAsync(false);
				props.setParentWindow(CommonModuleFrame.getCurrentFrame());
				props.setTitle(props.getHeader());

				int retVal = NativeDialogs.showMessageDialog(props);
				if(retVal == 0) {
					// save in original format
					sessionWriter = outputFactory.createWriter(origFormat.getSessionIO());
				} else if(retVal == 1) {
					// change original format to new Phon's default SessionIO
					origFormat = new OriginalFormat(sessionWriter.getClass().getAnnotation(SessionIO.class));
					session.putExtension(OriginalFormat.class, origFormat);
				} else {
					// cancelled
					return false;
				}
				origFormat.setIssueWarning(false);
			} else {
				sessionWriter = outputFactory.createWriter(origFormat.getSessionIO());
			}
		}

		UUID writeLock = null;
		try {
			LogUtil.info("Saving " + session.getCorpus() + "." + session.getName() + "...");
			writeLock = project.getSessionWriteLock(session);
			project.saveSession(session.getCorpus(), session.getName(), session, sessionWriter, writeLock);

			final long byteSize = project.getSessionByteSize(session);

			final String msg = "Save finished.  " +
					ByteSize.humanReadableByteCount(byteSize, true) + " written to disk.";
			LogUtil.info(msg);

			final SerializationWarnings warnings = session.getExtension(SerializationWarnings.class);
			ToastFactory.makeToast(msg).start(leftIconStrip);
			setModified(false);
			if(warnings != null && warnings.size() > 0) {
				warnings.clear();

				// show message
				int retVal = CommonModuleFrame.getCurrentFrame().showMessageDialog("Save", "Session saved with errors, see log for details.", MessageDialogProperties.okCancelOptions);
				if(retVal == 1) return false;
			}

			final EditorEvent<Session> ee = new EditorEvent<>(EditorEventType.SessionSaved, this, session);
			getEventManager().queueEvent(ee);

			// show a short message next to the save button to indicate save completed
			return true;
		} catch (IOException e) {
			final MessageDialogProperties props = new MessageDialogProperties();
			props.setRunAsync(false);
			props.setTitle("Save failed");
			props.setMessage(e.getLocalizedMessage());
			props.setHeader("Unable to save session");
			props.setOptions(MessageDialogProperties.okOptions);
			NativeDialogs.showMessageDialog(props);

			throw e;
		} finally {
			if(writeLock != null)
				project.releaseSessionWriteLock(session, writeLock);
		}
	}

	@Override
	public void lostOwnership(Clipboard clipboard, Transferable contents) {}

	@Override
	public Set<Class<?>> getExtensions() {
		return extensionSupport.getExtensions();
	}

	@Override
	public <T> T getExtension(Class<T> cap) {
		return extensionSupport.getExtension(cap);
	}

	@Override
	public <T> T putExtension(Class<T> cap, T impl) {
		return extensionSupport.putExtension(cap, impl);
	}

	@Override
	public <T> T removeExtension(Class<T> cap) {
		return extensionSupport.removeExtension(cap);
	}

}
