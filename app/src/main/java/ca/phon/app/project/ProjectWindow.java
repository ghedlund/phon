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
package ca.phon.app.project;

import ca.hedlund.desktopicons.*;
import ca.phon.app.log.LogUtil;
import ca.phon.app.modules.EntryPointArgs;
import ca.phon.app.project.actions.*;
import ca.phon.app.project.git.ProjectGitController;
import ca.phon.app.project.git.actions.*;
import ca.phon.app.session.check.SessionCheckEP;
import ca.phon.app.welcome.WorkspaceTextStyler;
import ca.phon.media.MediaLocator;
import ca.phon.plugin.PluginAction;
import ca.phon.project.*;
import ca.phon.project.ProjectEvent.ProjectEventType;
import ca.phon.session.SessionPath;
import ca.phon.ui.*;
import ca.phon.ui.action.*;
import ca.phon.ui.decorations.*;
import ca.phon.ui.fonts.FontPreferences;
import ca.phon.ui.menu.*;
import ca.phon.ui.nativedialogs.FileFilter;
import ca.phon.ui.text.*;
import ca.phon.ui.toast.ToastFactory;
import ca.phon.util.*;
import ca.phon.util.icons.*;
import ca.phon.worker.PhonWorker;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.jdesktop.swingx.*;
import org.jdesktop.swingx.JXCollapsiblePane.Direction;
import org.jdesktop.swingx.JXStatusBar.Constraint.ResizeBehavior;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.*;
import java.util.List;
import java.util.*;

/**
 * The project window.
 *
 */
public class ProjectWindow extends CommonModuleFrame {

	private DialogHeader header;

	private JPanel projectInfoPanel;
	private JLabel projectFolderLabel;
	private JLabel projectMediaFolderLabel;
	
	/** The corpus list */
	private TitledPanel corpusPanel;
	private MultiActionButton createCorpusButton;
	private JTextField corpusNameField;
	private JList<String> corpusList;
	private CorpusListModel corpusModel;
	private CorpusDetails corpusDetails;

	/** The session list */
	private TitledPanel sessionPanel;
	private MultiActionButton createSessionButton;
	private JTextField sessionNameField;
	private SessionNameTextCompleter sessionNameCompleter;
	private TreeTextCompleterModel<String> sessionNameCompleterModel;
	private JList<String> sessionList;
	private SessionListModel sessionModel;
	private SessionDetails sessionDetails;

	public static final String BLIND_MODE_PROPERTY = ProjectWindow.class.getName() + ".blindMode";
	public static final boolean DEFAULT_BLIND_MODE = false;
	public boolean blindMode =
			PrefHelper.getBoolean(BLIND_MODE_PROPERTY, DEFAULT_BLIND_MODE);
	private JCheckBox blindModeBox;

	private JXStatusBar statusBar;
	private JXBusyLabel busyLabel;
	private JLabel statusLabel;

	private final ProjectListener myProjectListener;

	private ProjectGitController gitController;

	/** Constructor */
	public ProjectWindow(Project project, String projectPath) {
		super("");

		setWindowName("Project Manager");

		putExtension(Project.class, project);
		gitController = new ProjectGitController(project);
		if(gitController.hasGitFolder()) {
			try {
				gitController.open();
			} catch (IOException e) {
				LogUtil.warning(e);
			}
		}

		myProjectListener = new ProjectWindowProjectListener(this);
		project.addProjectListener(myProjectListener);
		this.setTitle("Phon : " + project.getName() + " : Project Manager");

		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		init();

		final JMenuBar menuBar = MenuManager.createWindowMenuBar(this);
		setJMenuBar(menuBar);
	}

	public MultiActionButton getCorpusButton() {
		return this.createCorpusButton;
	}

	public JList<String> getCorpusList() {
		return this.corpusList;
	}

	public MultiActionButton getSessionButton() {
		return this.createSessionButton;
	}

	public JList<String> getSessionList() {
		return this.sessionList;
	}

	public String getSelectedCorpus() {
		return corpusList.getSelectedValue();
	}

	public List<String> getSelectedCorpora() {
		return corpusList.getSelectedValuesList();
	}

	public String getSelectedSessionName() {
		return sessionList.getSelectedValue();
	}

	public List<String> getSelectedSessionNames() {
		return sessionList.getSelectedValuesList();
	}

	public SessionPath getSelectedSessionPath() {
		return new SessionPath(getSelectedCorpus(), getSelectedSessionName());
	}

	public boolean isBlindMode() {
		return this.blindMode;
	}

	public void setBlindMode(boolean blindMode) {
		this.blindMode = blindMode;
		PrefHelper.getUserPreferences().putBoolean(BLIND_MODE_PROPERTY, blindMode);
	}

	/**
	 * Get the project
	 * @return project
	 */
	public Project getProject() {
		return getExtension(Project.class);
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(700, 500);
	}

	public TextCompleterModel<String> getCompleterModel() {
		return this.sessionNameCompleterModel;
	}
	
	@Override
	public void setJMenuBar(JMenuBar menu) {
		super.setJMenuBar(menu);

		JMenu projectMenu = new JMenu("Project");

		int projectMenuIndex = -1;
		// get the edit menu and add view commands
		for(int i = 0; i < menu.getMenuCount(); i++) {
			JMenu currentBar = menu.getMenu(i);

			if(currentBar != null && currentBar.getText() != null && currentBar.getText().equals("Edit")) {
				projectMenuIndex = i+1;
			}
		}

		if(projectMenuIndex > 0) {
			menu.add(projectMenu, projectMenuIndex);
		}

		projectMenu.addMenuListener(new MenuListener() {
			
			@Override
			public void menuSelected(MenuEvent e) {
				projectMenu.removeAll();
				setupProjectMenu(new MenuBuilder(projectMenu));
			}
			
			@Override
			public void menuDeselected(MenuEvent e) {
			}
			
			@Override
			public void menuCanceled(MenuEvent e) {
			}
			
		});
	}

	private void init() {
		/* Layout */
		setLayout(new BorderLayout());

		final ProjectDataTransferHandler transferHandler = new ProjectDataTransferHandler(this);
		
		setupProjectInformationPanel();
		
		/* Create components */
		createCorpusButton = createCorpusButton();
		createCorpusButton.setVisible(false);
		corpusList = new JList<String>();
		corpusModel = new CorpusListModel(getProject());
		corpusList.setModel(corpusModel);
		corpusList.setCellRenderer(new CorpusListCellRenderer());
		corpusList.setVisibleRowCount(20);
		corpusList.addListSelectionListener( e -> {
			if(getSelectedCorpus() != null) {
				String corpus = getSelectedCorpus();
				sessionModel.setCorpus(corpus);
				sessionList.clearSelection();
				corpusDetails.setCorpus(corpus);
				sessionDetails.setCorpus(corpus);
				sessionDetails.setSession(null);

				if(getProject().getCorpusSessions(corpus).size() == 0) {
					onShowCreateSessionButton();
				} else {
					onHideCreateSessionButton();
				}
			}
		});
		corpusList.addMouseListener(new MouseInputAdapter() {

			@Override
			public void mousePressed(MouseEvent e) {
				doPopup(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				doPopup(e);
			}

			public void doPopup(MouseEvent e) {
				if(e.isPopupTrigger()) {
					int clickedIdx = corpusList.locationToIndex(e.getPoint());
					if(clickedIdx >= 0 &&
							Arrays.binarySearch(corpusList.getSelectedIndices(), clickedIdx) < 0) {
						corpusList.setSelectedIndex(clickedIdx);
					}
					showCorpusListContextMenu(e.getPoint());
				}
			}
		});

		final DragSource corpusDragSource = new DragSource();
		corpusDragSource.createDefaultDragGestureRecognizer(corpusList, DnDConstants.ACTION_COPY, (event) -> {
			final List<ProjectPath> paths = new ArrayList<>();
			for(String corpus:getSelectedCorpora()) {
				final ProjectPath corpusPath = new ProjectPath(getProject(), corpus, null);
				paths.add(corpusPath);
			}
			final ProjectPathTransferable transferable = new ProjectPathTransferable(paths);
			event.startDrag(DragSource.DefaultCopyDrop, transferable);
		});

		corpusList.setDragEnabled(true);
		corpusList.setTransferHandler(transferHandler);

		corpusDetails = new CorpusDetails(getProject(), null);
		corpusDetails.setBackground(Color.white);
		corpusDetails.setOpaque(true);
		corpusDetails.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));

		sessionList = new JList<String>();
		createSessionButton = createSessionButton();
		createSessionButton.setVisible(false);
		sessionModel = new SessionListModel(getProject());
		sessionList.setModel(sessionModel);
		sessionList.setCellRenderer(new SessionListCellRenderer());
		sessionList.setVisibleRowCount(20);
		sessionList.addListSelectionListener( e -> {
			if(sessionList.getSelectedValue() != null && !e.getValueIsAdjusting()) {
				String corpus = getSelectedCorpus();
				String session = getSelectedSessionName();

				// clear details if more than one session is selected
				if(sessionList.getSelectedIndices().length > 1)
					session = null;

				sessionDetails.setSession(corpus, session);
			}
		});
		sessionList.addMouseListener(new MouseInputAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getClickCount() == 2 &&
						e.getButton() == 1) {
					// get the clicked item
					int clickedItem = sessionList.locationToIndex(e.getPoint());
					if(sessionList.getModel().getElementAt(clickedItem) == null)
						return;

					final String session =
						sessionList.getModel().getElementAt(clickedItem).toString();
					final String corpus =
						((SessionListModel)sessionList.getModel()).getCorpus();

					busyLabel.setBusy(true);
					statusLabel.setText("Opening " + corpus + "." + session + "...");

					PhonWorker.getInstance().invokeLater(() -> {
						final ActionEvent ae = new ActionEvent(sessionList, -1, "openSession");
						(new OpenSessionAction(ProjectWindow.this, corpus, session, isBlindMode())).actionPerformed(ae);

						SwingUtilities.invokeLater( () -> {
							statusLabel.setText("");
							busyLabel.setBusy(false);
						});
					});
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
				doPopup(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				doPopup(e);
			}

			public void doPopup(MouseEvent e) {
				if(e.isPopupTrigger()) {
					int clickedIdx = sessionList.locationToIndex(e.getPoint());
					if(clickedIdx >= 0 &&
							Arrays.binarySearch(sessionList.getSelectedIndices(), clickedIdx) < 0) {
						sessionList.setSelectedIndex(clickedIdx);
					}
					showSessionListContextMenu(e.getPoint());
				}
			}
		});

		sessionList.setDragEnabled(true);
		sessionList.setTransferHandler(transferHandler);

		final ActionMap sessionListAM = sessionList.getActionMap();
		final InputMap sessionListIM = sessionList.getInputMap(JComponent.WHEN_FOCUSED);

		final PhonUIAction<Void> openSessionAct = PhonUIAction.eventConsumer(this::onOpenSelectedSession);
		sessionListAM.put("openSelectedSession", openSessionAct);
		sessionListIM.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "openSelectedSession");

		sessionList.setActionMap(sessionListAM);
		sessionList.setInputMap(JComponent.WHEN_FOCUSED, sessionListIM);

		final DragSource sessionDragSource = new DragSource();
		sessionDragSource.createDefaultDragGestureRecognizer(sessionList, DnDConstants.ACTION_COPY, (event) -> {
			final List<ProjectPath> paths = new ArrayList<>();
			final String corpus = getSelectedCorpus();
			if(corpus == null) return;
			for(String session:getSelectedSessionNames()) {
				final ProjectPath sessionPath = new ProjectPath(getProject(), corpus, session);
				paths.add(sessionPath);
			}
			final ProjectPathTransferable transferable = new ProjectPathTransferable(paths);
			event.startDrag(DragSource.DefaultCopyDrop, transferable);
		});

		sessionDetails = new SessionDetails(getProject());
		sessionDetails.setBackground(Color.white);
		sessionDetails.setOpaque(true);
		sessionDetails.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));

		JScrollPane corpusScroller = new JScrollPane(corpusList);
		JScrollPane sessionScroller = new JScrollPane(sessionList);

		final PhonUIAction<Void> showCreateCorpusAct = PhonUIAction.runnable(this::onShowCreateCorpusButton);
		showCreateCorpusAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "New corpus...");
		showCreateCorpusAct.putValue(PhonUIAction.SMALL_ICON,
				IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "create_new_folder", IconSize.MEDIUM, UIManager.getColor("titledpanel.foreground")));
		final JButton showCreateCorpusBtn = new JButton(showCreateCorpusAct);
		showCreateCorpusBtn.setMargin(new Insets(0, 0, 0, 0));
		showCreateCorpusBtn.setOpaque(false);
		showCreateCorpusBtn.setBorderPainted(false);

		corpusPanel = new TitledPanel("Corpus folders");
		final ImageIcon folderIcon = IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "folder", IconSize.MEDIUM, UIManager.getColor("titledpanel.foreground"));
		DropDownIcon corpusDdIcn = new DropDownIcon(folderIcon, 0, SwingConstants.BOTTOM);
		corpusPanel.setIcon(corpusDdIcn);
		corpusPanel.getTitleLabel().addMouseListener(new MouseInputAdapter() {

			@Override
			public void mousePressed(MouseEvent e) {
				JPopupMenu ctxMenu = new JPopupMenu();
				setupCorpusListContextMenu(new MenuBuilder(ctxMenu));
				ctxMenu.show(corpusPanel.getTitleLabel(), 0, corpusPanel.getTitleLabel().getHeight());
			}
			
		});
		corpusPanel.getTitleLabel().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		corpusPanel.getTitleLabel().setToolTipText("Click for corpus contextual menu");
			
		corpusPanel.getContentContainer().add(createCorpusButton, BorderLayout.NORTH);
		corpusPanel.getContentContainer().add(corpusScroller, BorderLayout.CENTER);
		corpusPanel.setRightDecoration(showCreateCorpusBtn);

		final PhonUIAction<Void> showCreateSessionAct = PhonUIAction.runnable(this::onShowCreateSessionButton);
		showCreateSessionAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "New session...");
		showCreateSessionAct.putValue(PhonUIAction.SMALL_ICON,
				IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "note_add", IconSize.MEDIUM, UIManager.getColor("titledpanel.foreground")));
		final JButton showCreateSessionBtn = new JButton(showCreateSessionAct);
		showCreateSessionBtn.setMargin(new Insets(0, 0, 0, 0));
		showCreateSessionBtn.setOpaque(false);
		showCreateSessionBtn.setBorderPainted(false);

		blindModeBox = new JCheckBox("Blind mode");
		blindModeBox.setOpaque(false);
		blindModeBox.setMargin(new Insets(0, 0, 0, 0));
		blindModeBox.setSelected(this.blindMode);
		blindModeBox.setToolTipText("When selected default action will be to open session as transcriber (blind mode)");
		blindModeBox.addActionListener( (e) -> setBlindMode(blindModeBox.isSelected()) );
		blindModeBox.setForeground(Color.white);
		
		final OpenSessionAction openSessionButtonAct = new OpenSessionAction(this);
		final OpenSessionAction openBlindModeAct = new OpenSessionAction(this, true);
		openBlindModeAct.putValue(PhonUIAction.NAME, "Open session as transcriber... (blind mode)");
		openBlindModeAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Open session as a blind transcriber");
		
		final JPopupMenu openSessionMenu = new JPopupMenu();
		openSessionMenu.add(openSessionButtonAct);
		openSessionMenu.add(openBlindModeAct);
		
		openSessionButtonAct.putValue(DropDownButton.BUTTON_POPUP, openSessionMenu);
		openSessionButtonAct.putValue(DropDownButton.ARROW_ICON_POSITION, SwingConstants.BOTTOM);
		openSessionButtonAct.putValue(DropDownButton.ARROW_ICON_GAP, 3);
		
		final JPanel sessionDecoration = new JPanel(new HorizontalLayout());
		sessionDecoration.setOpaque(false);
		sessionDecoration.add(blindModeBox);
		sessionDecoration.add(showCreateSessionBtn);

		sessionPanel = new TitledPanel("Session");
		ImageIcon draftIcon = IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "draft", IconSize.MEDIUM, UIManager.getColor("titledpanel.foreground"));
		DropDownIcon xmlDdIcn = new DropDownIcon(draftIcon, 0, SwingConstants.BOTTOM);
		sessionPanel.setIcon(xmlDdIcn);
		sessionPanel.setRightDecoration(sessionDecoration);
		sessionPanel.getContentContainer().add(createSessionButton, BorderLayout.NORTH);
		sessionPanel.getContentContainer().add(sessionScroller, BorderLayout.CENTER);
		sessionPanel.getTitleLabel().addMouseListener(new MouseInputAdapter() {

			@Override
			public void mousePressed(MouseEvent e) {
				// show context menu
				JPopupMenu ctxMenu = new JPopupMenu();
				setupSessionListContextMenu(new MenuBuilder(ctxMenu));
				ctxMenu.show(sessionPanel.getTitleLabel(), 0, sessionPanel.getTitleLabel().getHeight());
			}
			
		});
		sessionPanel.getTitleLabel().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		sessionPanel.getTitleLabel().setToolTipText("Click for session list contextual menu");

		final JXCollapsiblePane bottomPanel = new JXCollapsiblePane(Direction.UP);
		bottomPanel.setLayout(new GridLayout(1, 2));
		bottomPanel.add(corpusDetails);
		bottomPanel.add(sessionDetails);

		final TitledPanel detailsPanel = new TitledPanel("Details", bottomPanel);
		detailsPanel.setIcon(IconManager.getInstance()
				.getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "info", IconSize.MEDIUM, UIManager.getColor("titledpanel.foreground")));

		final JXMultiSplitPane multiSplitPane = new JXMultiSplitPane();
		final String multiSplitLayout = "(COLUMN "
//				+ "(LEAF weight=0.0 name=project)" 
				+ "(ROW weight=1.0 (LEAF weight=0.5 name=corpus) (LEAF weight=0.5 name=session) ) "
				+ "(LEAF weight=0.0 name=details))";
		final MultiSplitLayout.Node rootLayoutNode = MultiSplitLayout.parseModel(multiSplitLayout);

		corpusPanel.setPreferredSize(new Dimension(500, 600));
		sessionPanel.setPreferredSize(new Dimension(500, 600));

		multiSplitPane.setModel(rootLayoutNode);
		multiSplitPane.setDividerSize(2);
//		multiSplitPane.add(projectInfoPanel, "project");
		multiSplitPane.add(corpusPanel, "corpus");
		multiSplitPane.add(sessionPanel, "session");
		multiSplitPane.add(detailsPanel, "details");
		
		statusBar = new JXStatusBar();
		busyLabel = new JXBusyLabel(new Dimension(16, 16));
		statusBar.add(busyLabel, new JXStatusBar.Constraint(16));

		statusLabel = new JLabel();
		statusBar.add(statusLabel, new JXStatusBar.Constraint(ResizeBehavior.FILL));
//		statusBar.add(blindModeBox, new JXStatusBar.Constraint(ResizeBehavior.FIXED));

		String projectName = null;
		projectName = getProject().getName();

		header = new DialogHeader(projectName,"");
		header.replaceBottomLabel(projectInfoPanel);
		
		add(header, BorderLayout.NORTH);
		add(multiSplitPane, BorderLayout.CENTER);
		add(statusBar, BorderLayout.SOUTH);

		// if no corpora are currently available, 'prompt' the user to create a new one
		if(getProject().getCorpora().size() == 0) {
			SwingUtilities.invokeLater( () -> {
				onShowCreateCorpusButton();
			});
		} else {
			SwingUtilities.invokeLater( () -> {
				corpusList.setSelectedIndex(0);
				corpusList.requestFocusInWindow();
			});
		}

		addWindowFocusListener(new WindowAdapter() {
			@Override
			public void windowGainedFocus(WindowEvent e) {
				refreshProject();
			}
		});
	}
	
	private void setupProjectInformationPanel() {
		projectInfoPanel = new JPanel(new GridBagLayout());
		projectInfoPanel.setOpaque(false);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.insets = new Insets(0, 0, 0, 5);
		
		projectInfoPanel.add(new JLabel("Project folder:"), gbc);
		++gbc.gridx;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		projectFolderLabel = new JLabel(getProject().getLocation());
		projectFolderLabel.setForeground(Color.blue);
		projectFolderLabel.setToolTipText("Click to show project folder");
		projectFolderLabel.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent me) {
				if(Desktop.isDesktopSupported()) {
					try {
						Desktop.getDesktop().open(new File(getProject().getLocation()));
					} catch (IOException e) {
						LogUtil.warning(e);
						Toolkit.getDefaultToolkit().beep();
					}
				}
			}

		});

		projectFolderLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		projectInfoPanel.add(projectFolderLabel, gbc);
		
		++gbc.gridy;
		gbc.gridx = 0;
		gbc.weightx = 0.0;
		gbc.fill = GridBagConstraints.NONE;
		
		final JPopupMenu projectMediaFolderMenu = new JPopupMenu();
		projectMediaFolderMenu.addPopupMenuListener(new PopupMenuListener() {
			
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				projectMediaFolderMenu.removeAll();
				setupProjectMediaFolderMenu(new MenuBuilder(projectMediaFolderMenu));
			}
			
			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				
			}
			
			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {
				
			}
			
		});
		
		projectInfoPanel.add(new JLabel("Media folder:"), gbc);
		projectMediaFolderLabel = new JLabel();
		updateProjectMediaLabel();
		projectMediaFolderLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		projectMediaFolderLabel.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) {
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
				projectMediaFolderMenu.show(projectMediaFolderLabel, 0, projectMediaFolderLabel.getHeight());
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {
			}
			
		});
		
		
		
		++gbc.gridx;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		projectInfoPanel.add(projectMediaFolderLabel, gbc);
		
		getProject().addProjectListener(new ProjectListener() {
			
			@Override
			public void projectWriteLocksChanged(ProjectEvent pe) {
				
			}
			
			@Override
			public void projectStructureChanged(ProjectEvent pe) {
				
			}
			
			@Override
			public void projectDataChanged(ProjectEvent pe) {
				if(pe.getEventType() == ProjectEventType.PROJECT_MEDIAFOLDER_CHANGED) {
					updateProjectMediaLabel();
				} else if(pe.getEventType() == ProjectEventType.PROJECT_NAME_CHANGED) {
					updateProjectNameAndLocation();
				}
			}
		});
	}
	
	private void setupProjectMenu(MenuBuilder builder) {
		final RenameProjectAction renameProjectAction = new RenameProjectAction(this);
		builder.addItem(".", renameProjectAction);

		// refresh lists
		final RefreshAction refreshItem = new RefreshAction(this);
		builder.addItem(".", refreshItem);
		builder.addSeparator(".", "refresh");

		JMenu corpusMenu = builder.addMenu(".", "Corpus");
		setupCorpusListContextMenu(new MenuBuilder(corpusMenu));
		
		JMenu sessionMenu = builder.addMenu(".", "Session");
		setupSessionListContextMenu(new MenuBuilder(sessionMenu));

		builder.addSeparator(".", "newcmds");
				
		final PluginAction checkSessionsAct = new PluginAction(SessionCheckEP.EP_NAME);
		checkSessionsAct.putArg(EntryPointArgs.PROJECT_OBJECT, getProject());
		checkSessionsAct.putValue(PluginAction.NAME, "Check sessions...");
		checkSessionsAct.putValue(PluginAction.SHORT_DESCRIPTION, "Check sessions for warnings");
		builder.addItem(".", checkSessionsAct);

		final AnonymizeAction anonymizeParticipantInfoItem = new AnonymizeAction(this);
		builder.addItem(".", anonymizeParticipantInfoItem);
		
		// merge/split sessions
		final DeriveSessionAction deriveItem = new DeriveSessionAction(this);
		builder.addItem(".", deriveItem);
		
		builder.addSeparator(".", "project_actions");
		
		setupProjectMediaFolderMenu(builder);
		builder.addSeparator(".", "project_media_folder");

		builder.addSeparator(".", "team");
		final JMenu teamMenu = builder.addMenu(".", "Team");
		teamMenu.addMenuListener(new MenuListener() {

			@Override
			public void menuSelected(MenuEvent e) {
				teamMenu.removeAll();
				if(getProject() != null) {
					final ProjectGitController gitController = new ProjectGitController(getProject());
					if(gitController.hasGitFolder()) {
						teamMenu.add(new CommitAction(ProjectWindow.this));

						teamMenu.addSeparator();

						teamMenu.add(new PullAction(ProjectWindow.this));
						teamMenu.add(new PushAction(ProjectWindow.this));

					} else {
						final InitAction initRepoAct =
								new InitAction(ProjectWindow.this);
						teamMenu.add(initRepoAct);
					}
				}
			}

			@Override
			public void menuDeselected(MenuEvent e) {

			}

			@Override
			public void menuCanceled(MenuEvent e) {

			}
		});
	}

	private void updateProjectNameAndLocation() {
		this.setTitle("Phon : " + getProject().getName() + " : Project Manager");
		if(header != null)
			header.setHeaderText(getProject().getName());
		projectFolderLabel.setText(getProject().getLocation());
		updateProjectMediaLabel();
	}

	private void updateProjectMediaLabel() {
		File projectMediaFolder = new File(getProject().getProjectMediaFolder());
		File absoluteProjectMediaFolder = projectMediaFolder.isAbsolute() ? projectMediaFolder : new File(getProject().getLocation(), getProject().getProjectMediaFolder());
		
		if(!getProject().hasCustomProjectMediaFolder() && !absoluteProjectMediaFolder.exists()) {
			projectMediaFolderLabel.setText("(click to select)");
			projectMediaFolderLabel.setForeground(Color.blue);
		} else {
			projectMediaFolderLabel.setText(getProject().getProjectMediaFolder());
			if(absoluteProjectMediaFolder.exists()) {
				projectMediaFolderLabel.setForeground(Color.blue);
				projectMediaFolderLabel.setToolTipText("Click to change project media folder");
			} else {
				projectMediaFolderLabel.setForeground(Color.red);
				projectMediaFolderLabel.setToolTipText("Media folder not found, click to create or select new project media folder");
			}
		}
	}

	private void openFolder(File folder) {
		try {
			Desktop.getDesktop().open(folder);
		} catch (UnsupportedOperationException | IOException e) {
			LogUtil.warning(e);
		}
	}

	private void setupProjectMediaFolderMenu(MenuBuilder builder) {
		File projectMediaFolder = new File(getProject().getProjectMediaFolder());
		File absoluteProjectMediaFolder = projectMediaFolder.isAbsolute() ? projectMediaFolder : new File(getProject().getLocation(), getProject().getProjectMediaFolder());
		
		final PhonUIAction<File> showProjectFolderAct = PhonUIAction.consumer(this::openFolder, absoluteProjectMediaFolder);
		showProjectFolderAct.putValue(PhonUIAction.NAME, "Show media folder");
		showProjectFolderAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Open file system browser with project media folder selected");
		JMenuItem showProjectFolderItem = new JMenuItem(showProjectFolderAct);
		showProjectFolderItem.setEnabled(absoluteProjectMediaFolder.exists());
		builder.addItem(".", showProjectFolderItem);
		
		builder.addSeparator(".", "s1");
		
		final SelectProjectMediaFolder selectFolderAct = new SelectProjectMediaFolder(this);
		builder.addItem(".", selectFolderAct);
		
		if(getProject().hasCustomProjectMediaFolder()) {
			final PhonUIAction<Void> resetProjectFolderAct = PhonUIAction.eventConsumer(this::onResetProjectMediaFolder);
			resetProjectFolderAct.putValue(PhonUIAction.NAME, "Clear media folder selection");
			resetProjectFolderAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Clear media folder selection");
			builder.addItem(".", resetProjectFolderAct);
			
			if(!absoluteProjectMediaFolder.exists()) {
				final PhonUIAction<Void> createProjectFolderAct = PhonUIAction.runnable(() -> absoluteProjectMediaFolder.mkdirs());
				createProjectFolderAct.putValue(PhonUIAction.NAME, (getProject().hasCustomProjectMediaFolder() ? "Create media folder" : "Create default media folder"));
				createProjectFolderAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Create folder " + getProject().getProjectMediaFolder());
				final JMenuItem createProjectFolderItem = new JMenuItem(createProjectFolderAct);
				createProjectFolderItem.addActionListener( (e) -> SwingUtilities.invokeLater(ProjectWindow.this::updateProjectMediaLabel) );
				builder.addItem(".", createProjectFolderItem);
			}
		}
		
		if(getProject().hasCustomProjectMediaFolder()) {
			if(projectMediaFolder.isAbsolute()) {
				final PhonUIAction<Void> makeRelativeAct = PhonUIAction.runnable(this::onMakeProjectMediaFolderRelative);
				makeRelativeAct.putValue(PhonUIAction.NAME,	"Make media folder path relative to project");
				makeRelativeAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Make media folder path relative to project folder");
				builder.addItem(".", makeRelativeAct);
			} else {
				final PhonUIAction<Void> makeAbsoluteAct = PhonUIAction.runnable(this::onMakeProjectMediaFolderAbsolute);
				makeAbsoluteAct.putValue(PhonUIAction.NAME, "Make media folder path absolute");
				makeAbsoluteAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Make project media folder an absolute filename");
				builder.addItem(".", makeAbsoluteAct);
			}
		}
	}
	
	void setupCorpusFolderMenu(String corpus, MenuBuilder builder) {
		boolean enabled = (corpus != null);
		
		String corpusMediaPath = (enabled ? getProject().getCorpusMediaFolder(corpus) : getProject().getProjectMediaFolder());
		if(corpusMediaPath == null) return;
		
		File corpusMediaFolder = new File(corpusMediaPath);
		File absoluteCorpusMediaFolder = corpusMediaFolder.isAbsolute() ? corpusMediaFolder : new File(getProject().getLocation(), corpusMediaPath);
		
		final PhonUIAction<File> showProjectFolderAct = PhonUIAction.consumer(this::openFolder, absoluteCorpusMediaFolder);
		showProjectFolderAct.putValue(PhonUIAction.NAME, "Show media folder");
		showProjectFolderAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Open file system browser with project media folder selected");
		JMenuItem showProjectFolderItem = new JMenuItem(showProjectFolderAct);
		showProjectFolderItem.setEnabled(enabled && absoluteCorpusMediaFolder.exists());
		builder.addItem(".", showProjectFolderItem);
		
		builder.addSeparator(".", "s1");
		
		final SelectCorpusMediaFolder selectFolderAct = new SelectCorpusMediaFolder(this);
		builder.addItem(".", selectFolderAct).setEnabled(enabled);
		
		if(getProject().hasCustomCorpusMediaFolder(corpus)) {
			final PhonUIAction<String> resetCorpusFolderAct = PhonUIAction.eventConsumer(this::onResetCorpusMediaFolder, corpus);
			resetCorpusFolderAct.putValue(PhonUIAction.NAME, "Clear media folder selection");
			resetCorpusFolderAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Clear corpus media folder selection (use project media folder)");
			builder.addItem(".", resetCorpusFolderAct).setEnabled(enabled);
			
			if(!absoluteCorpusMediaFolder.exists()) {
				final PhonUIAction<Void> createCorpusFolderAct = PhonUIAction.runnable(() -> absoluteCorpusMediaFolder.mkdirs());
				createCorpusFolderAct.putValue(PhonUIAction.NAME, (getProject().hasCustomCorpusMediaFolder(corpus) ? "Create media folder" : "Create default media folder"));
				createCorpusFolderAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Create folder " + getProject().getProjectMediaFolder());
				final JMenuItem createCorpusFolderItem = new JMenuItem(createCorpusFolderAct);
				createCorpusFolderItem.addActionListener( (e) -> SwingUtilities.invokeLater(ProjectWindow.this::updateProjectMediaLabel) );
				createCorpusFolderItem.setEnabled(enabled);
				builder.addItem(".", createCorpusFolderItem);
			}
		}
		
		if(getProject().hasCustomCorpusMediaFolder(corpus)) {
			if(corpusMediaFolder.isAbsolute()) {
				final PhonUIAction<String> makeRelativeAct = PhonUIAction.consumer(this::onMakeCorpusMediaFolderRelative, corpus);
				makeRelativeAct.putValue(PhonUIAction.NAME,	"Make media folder path relative to project");
				makeRelativeAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Make corpus media folder path relative to project folder");
				builder.addItem(".", makeRelativeAct).setEnabled(enabled);
			} else {
				final PhonUIAction<String> makeAbsoluteAct = PhonUIAction.consumer(this::onMakeCorpusMediaFolderAbsolute, corpus);
				makeAbsoluteAct.putValue(PhonUIAction.NAME, "Make media folder path absolute");
				makeAbsoluteAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Make corpus media folder an absolute filename");
				builder.addItem(".", makeAbsoluteAct).setEnabled(enabled);
			}
		}
	}
	
	private String makeRelativetoProject(String filename) {
		File file = new File(filename);
		String retVal = filename;
		if(file.isAbsolute()) {
			File projectFolder = new File(getProject().getLocation());
			Path projectPath = projectFolder.toPath();
			Path path = file.toPath();
			if(projectPath.getRoot().equals(path.getRoot())) {				
				path = projectPath.relativize(path);
			} else {
				try {
					path = path.toRealPath();
				} catch (IOException e) {
					LogUtil.warning(e);
				}
			}
			retVal = path.toString();
		}
		return retVal;
	}
	
	public void onMakeCorpusMediaFolderRelative(String corpus) {
		if(getProject().hasCustomCorpusMediaFolder(corpus)) {
			String relativeProjectMediaFolder = makeRelativetoProject(getProject().getCorpusMediaFolder(corpus));
			getProject().setCorpusMediaFolder(corpus, relativeProjectMediaFolder);
		}
	}
	
	public void onMakeCorpusMediaFolderAbsolute(String corpus) {
		if(getProject().hasCustomCorpusMediaFolder(corpus)) {
			String currentValue = getProject().getCorpusMediaFolder(corpus);
			File file = new File(currentValue);
			if(!file.isAbsolute()) {
				File absoluteFile = new File(getProject().getLocation(), currentValue);
				try {
					Path absolutePath = absoluteFile.toPath().toRealPath();
					getProject().setCorpusMediaFolder(corpus, absolutePath.toString());
				} catch (IOException e) {
					LogUtil.warning(e);
				}
			}
		}
	}
	
	public void onMakeProjectMediaFolderRelative() {
		if(getProject().hasCustomProjectMediaFolder()) {
			String relativeProjectMediaFolder = makeRelativetoProject(getProject().getProjectMediaFolder());
			getProject().setProjectMediaFolder(relativeProjectMediaFolder);
		}
	}
	
	public void onMakeProjectMediaFolderAbsolute() {
		if(getProject().hasCustomProjectMediaFolder()) {
			String currentValue = getProject().getProjectMediaFolder();
			File file = new File(currentValue);
			if(!file.isAbsolute()) {
				File absoluteFile = new File(getProject().getLocation(), currentValue);
				try {
					Path absolutePath = absoluteFile.toPath().toRealPath();
					getProject().setProjectMediaFolder(absolutePath.toString());
				} catch (IOException e) {
					LogUtil.warning(e);
				}
			}
		}
	}
	
	public void onResetProjectMediaFolder(PhonActionEvent<Void> pae) {
		getProject().setProjectMediaFolder(null);
	}

	public void onResetCorpusMediaFolder(PhonActionEvent<String> pae) {
		getProject().setCorpusMediaFolder(pae.getData(), null);
	}
	
	public void onOpenSelectedSession(PhonActionEvent<Void> pae) {
		final PhonWorker worker = PhonWorker.createWorker();
		busyLabel.setBusy(true);

		for(int selectedIdx:sessionList.getSelectedIndices()) {
			final String session = ((SessionListModel)sessionList.getModel()).getSessions().get(selectedIdx);
			final String corpus =
				((SessionListModel)sessionList.getModel()).getCorpus();

			worker.invokeLater( () -> {
				try {
					SwingUtilities.invokeAndWait( () -> statusLabel.setText("Opening " + corpus + "." + session + "...") );
				} catch (InvocationTargetException | InterruptedException e) {
				}
			});

			worker.invokeLater(() -> {
				final ActionEvent ae = new ActionEvent(sessionList, -1, "openSession");
				(new OpenSessionAction(ProjectWindow.this, corpus, session, isBlindMode())).actionPerformed(ae);
			});
		}

		worker.invokeLater( () -> {;
			SwingUtilities.invokeLater( () -> {
				statusLabel.setText("");
				busyLabel.setBusy(false);
			});
		});
		worker.start();
	}

	public void onRenameCorpus(PhonActionEvent pae) {
		if(getSelectedCorpus() == null) {
			Toolkit.getDefaultToolkit().beep();
			ToastFactory.makeToast("Please select a corpus").start(corpusList);
			return;
		}
		(new RenameCorpusAction(this)).actionPerformed(pae.getActionEvent());
	}

	private MultiActionButton createCorpusButton() {
		MultiActionButton retVal = new MultiActionButton();

		final ImageIcon folderNewIcn = IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "create_new_folder", IconSize.MEDIUM, UIManager.getColor("Button.foreground"));
		String s1 = "New Corpus";
		String s2 = "Enter corpus name and press enter.  Press escape to cancel.";

		retVal.getTopLabel().setText(WorkspaceTextStyler.toHeaderText(s1));
		retVal.getTopLabel().setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
		retVal.getTopLabel().setFont(FontPreferences.getTitleFont());
		retVal.getTopLabel().setIcon(folderNewIcn);
		retVal.setAlwaysDisplayActions(true);

		retVal.setOpaque(false);

		ImageIcon cancelIcn = IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "close", IconSize.SMALL, UIManager.getColor("Button.foreground"));
		ImageIcon cancelIcnL = IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "close", IconSize.MEDIUM, UIManager.getColor("Button.foreground"));

		PhonUIAction<Void> btnSwapAct = PhonUIAction.runnable(this::onHideCreateCorpusButton);
		btnSwapAct.putValue(Action.ACTION_COMMAND_KEY, "CANCEL_CREATE_ITEM");
		btnSwapAct.putValue(Action.NAME, "Cancel create");
		btnSwapAct.putValue(Action.SHORT_DESCRIPTION, "Cancel create");
		btnSwapAct.putValue(Action.SMALL_ICON, cancelIcn);
		btnSwapAct.putValue(Action.LARGE_ICON_KEY, cancelIcnL);
		retVal.addAction(btnSwapAct);

		JPanel corpusNamePanel = new JPanel(new BorderLayout());
		corpusNamePanel.setOpaque(false);

		corpusNameField = new JTextField();
		corpusNameField.setDocument(new NameDocument());
		corpusNameField.setText("Corpus Name");
		corpusNamePanel.add(corpusNameField, BorderLayout.CENTER);

		ActionMap actionMap = retVal.getActionMap();
		actionMap.put(btnSwapAct.getValue(Action.ACTION_COMMAND_KEY), btnSwapAct);
		InputMap inputMap = retVal.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);

		inputMap.put(ks, btnSwapAct.getValue(Action.ACTION_COMMAND_KEY));

		retVal.setActionMap(actionMap);
		retVal.setInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, inputMap);

		PhonUIAction<JTextField> createNewCorpusAct = PhonUIAction.eventConsumer(this::onCreateCorpus, corpusNameField);
		createNewCorpusAct.putValue(Action.SHORT_DESCRIPTION, "Create new corpus folder");
		createNewCorpusAct.putValue(Action.SMALL_ICON, IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "add", IconSize.MEDIUM, UIManager.getColor("Button.foreground")));

		JButton createBtn = new JButton(createNewCorpusAct);
		corpusNamePanel.add(createBtn, BorderLayout.EAST);

		corpusNameField.setAction(createNewCorpusAct);

		// swap bottom component in new project button
		retVal.setBottomLabelText(WorkspaceTextStyler.toDescText(s2));
		retVal.add(corpusNamePanel, BorderLayout.CENTER);

		retVal.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
			}

			@Override
			public void focusGained(FocusEvent e) {
				corpusNameField.requestFocus();
			}
		});

		return retVal;
	}

	public void onShowCreateCorpusButton() {
		createCorpusButton.setVisible(true);
		corpusPanel.revalidate();

		corpusNameField.requestFocusInWindow();
		corpusNameField.selectAll();
	}

	public void onHideCreateCorpusButton() {
		createCorpusButton.setVisible(false);
		corpusPanel.revalidate();
	}

	public void onCreateCorpus(PhonActionEvent<JTextField> pae) {
		final JTextField textField = pae.getData();
		final String corpusName = textField.getText().trim();
		if(corpusName.length() == 0) {
			Toolkit.getDefaultToolkit().beep();
			ToastFactory.makeToast("Corpus name cannot be empty").start(textField);
			return;
		}

		final NewCorpusAction newCorpusAct = new NewCorpusAction(this, corpusName);
		newCorpusAct.actionPerformed(pae.getActionEvent());
		if(newCorpusAct.isCorpusCreated()) {
			onHideCreateCorpusButton();
			corpusList.setSelectedValue(corpusName, true);
		}
	}

	public void onShowCreateSessionButton() {
		createSessionButton.setVisible(true);
		sessionPanel.getContentContainer().revalidate();

		sessionNameField.requestFocusInWindow();
		sessionNameField.selectAll();
	}

	public void onHideCreateSessionButton() {
		createSessionButton.setVisible(false);
		sessionPanel.getContentContainer().revalidate();
	}

	private void addTextCompletion(Path path) {
		String name = path.getFileName().toString();
		name = FilenameUtils.removeExtension(name);
				
		if(!sessionNameCompleter.getModel().containsCompletion(name)) {
			sessionNameCompleterModel.addCompletion(name, path.normalize().toString());
		}
	}

	private void scanPath(Path mediaPath, boolean recursive) {
		scanPath(mediaPath, mediaPath, true);
	}

	private void scanPath(Path rootPath, Path path, boolean recursive) {
		try(DirectoryStream<Path> dirStream = Files.newDirectoryStream(path)) {
			final Iterator<Path> childItr = dirStream.iterator();
			while(childItr.hasNext()) {
				final Path child = childItr.next();

				if(Files.isHidden(child)) continue;
				final File file = child.toFile();
				if(!FileFilter.mediaFilter.accept(file)) continue;

				if(Files.isDirectory(child) && recursive) {
					scanPath(rootPath, child, recursive);
				} else {
					final Path pathToAdd = rootPath.relativize(child);
					addTextCompletion(pathToAdd);
				}
			}
		} catch (IOException e) {
			LogUtil.severe(e);
		}
	}
	
	private void setupTextCompleter(String corpus) {
		final List<String> mediaIncludePaths = MediaLocator.getMediaIncludePaths(getProject(), corpus);
		
		sessionNameCompleterModel.clearCompletions();
		sessionNameCompleterModel.setIncludeInfixEntries(true);
		
		for(String path:mediaIncludePaths) {
			final Path mediaFolder = Paths.get(path);
			if(!Files.exists(mediaFolder)) continue;

			scanPath(mediaFolder, true);
		}
		
	}
	
	private MultiActionButton createSessionButton() {
		MultiActionButton retVal = new MultiActionButton();

		final ImageIcon xmlNewIcn =
				IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "note_add", IconSize.MEDIUM, UIManager.getColor("Button.foreground"));

		String s1 = "New Session";
		String s2 = "Enter session name and press enter.  Press escape to cancel.";

		retVal.getTopLabel().setText(WorkspaceTextStyler.toHeaderText(s1));
		retVal.getTopLabel().setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
		retVal.getTopLabel().setFont(FontPreferences.getTitleFont());
		retVal.getTopLabel().setIcon(xmlNewIcn);
		retVal.setAlwaysDisplayActions(true);

		retVal.setOpaque(false);

		ImageIcon cancelIcn = IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "close", IconSize.SMALL, UIManager.getColor("Button.foreground"));
		ImageIcon cancelIcnL = IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "close", IconSize.MEDIUM, UIManager.getColor("Button.foreground"));;

		PhonUIAction<Void> btnSwapAct = PhonUIAction.runnable(this::onHideCreateSessionButton);
		btnSwapAct.putValue(Action.ACTION_COMMAND_KEY, "CANCEL_CREATE_ITEM");
		btnSwapAct.putValue(Action.NAME, "Cancel create");
		btnSwapAct.putValue(Action.SHORT_DESCRIPTION, "Cancel create");
		btnSwapAct.putValue(Action.SMALL_ICON, cancelIcn);
		btnSwapAct.putValue(Action.LARGE_ICON_KEY, cancelIcnL);
		retVal.addAction(btnSwapAct);

		JPanel sessionNamePanel = new JPanel(new BorderLayout());
		sessionNamePanel.setOpaque(false);

		sessionNameField = new JTextField();
		sessionNameField.setDocument(new NameDocument());
		sessionNameField.getDocument().addDocumentListener(new DocumentListener() {
			
			@Override
			public void removeUpdate(DocumentEvent e) {
				if(sessionNameCompleter != null) {
					sessionNameCompleter.selectedMedia = null;
				}
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				if(sessionNameCompleter != null) {
					sessionNameCompleter.selectedMedia = null;
				}
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				
			}
		});
		sessionNameField.setText("Session Name");
		sessionNameField.addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent e) {
				
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				setupTextCompleter(getSelectedCorpus());
			}
			
		});

		sessionNameCompleterModel = new TreeTextCompleterModel<String>();
		sessionNameCompleter = new SessionNameTextCompleter(sessionNameCompleterModel);
		sessionNameCompleter.install(sessionNameField);
		
		sessionNamePanel.add(sessionNameField, BorderLayout.CENTER);

		ActionMap actionMap = retVal.getActionMap();
		actionMap.put(btnSwapAct.getValue(Action.ACTION_COMMAND_KEY), btnSwapAct);
		InputMap inputMap = retVal.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);

		inputMap.put(ks, btnSwapAct.getValue(Action.ACTION_COMMAND_KEY));

		retVal.setActionMap(actionMap);
		retVal.setInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, inputMap);

		PhonUIAction<JTextField> createNewSessionAct = PhonUIAction.eventConsumer(this::onCreateSession, sessionNameField);
		createNewSessionAct.putValue(Action.SHORT_DESCRIPTION, "Create new session in selected corpus");
		createNewSessionAct.putValue(Action.SMALL_ICON, IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "add", IconSize.MEDIUM, UIManager.getColor("Button.foreground")));

		JButton createBtn = new JButton(createNewSessionAct);
		sessionNamePanel.add(createBtn, BorderLayout.EAST);

		sessionNameField.setAction(createNewSessionAct);

		// swap bottom component in new project button
		retVal.setBottomLabelText(WorkspaceTextStyler.toDescText(s2));
		retVal.add(sessionNamePanel, BorderLayout.CENTER);

		retVal.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {

			}

			@Override
			public void focusGained(FocusEvent e) {
				sessionNameField.requestFocus();
			}
		});

		return retVal;
	}

	public void onCreateSession(PhonActionEvent<JTextField> pae) {
		final JTextField textField = pae.getData();
		final String sessionName = textField.getText().trim();
		if(sessionName.length() == 0) {
			Toolkit.getDefaultToolkit().beep();
			ToastFactory.makeToast("Session name cannot be empty").start(textField);
			return;
		}

		final NewSessionAction newSessionAct = new NewSessionAction(this, getSelectedCorpus(), sessionName);
		newSessionAct.setSessionMedia(sessionNameCompleter.selectedMedia);
		newSessionAct.actionPerformed(pae.getActionEvent());
		if(newSessionAct.isSessionCreated()) {
			onHideCreateSessionButton();
			SwingUtilities.invokeLater( () -> {
				sessionList.setSelectedValue(sessionName, true);
				sessionList.requestFocus();
			});
		}
	}

	/**
	 * Displays the corpus list menu
	 *
	 * @param clickPoint
	 */
	private void showCorpusListContextMenu(Point clickPoint) {
		JPopupMenu contextMenu = new JPopupMenu();
		setupCorpusListContextMenu(new MenuBuilder(contextMenu));

		contextMenu.show(corpusList, clickPoint.x, clickPoint.y);
	}
	
	private void setupCorpusListContextMenu(MenuBuilder builder) {
		List<String> corpora = getSelectedCorpora();
		
		PhonUIAction<Void> createNewCorpusAct = PhonUIAction.runnable(this::onShowCreateCorpusButton);
		createNewCorpusAct.putValue(Action.NAME, "New corpus...");
		createNewCorpusAct.putValue(Action.SHORT_DESCRIPTION, "Create a new corpus");
		createNewCorpusAct.putValue(Action.SMALL_ICON, IconManager.getInstance()
						.getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "create_new_folder", IconSize.MEDIUM, UIManager.getColor("Button.foreground")));
		builder.addItem(".", createNewCorpusAct);
		
		final boolean enabled = corpora.size() > 0;
		builder.addSeparator(".", "s1");

		JMenuItem dupItem = new JMenuItem(new DuplicateCorpusAction(this));
		dupItem.setEnabled(enabled);
		if(corpora.size() > 1) {
			dupItem.setText("Duplicate Corpora");
		}
		builder.addItem(".", dupItem);

			JMenuItem renameItem = new JMenuItem(new RenameCorpusAction(this));
			renameItem.setEnabled(enabled);
			builder.addItem(".", renameItem);

		// delete
		JMenuItem deleteItem = new JMenuItem(new DeleteCorpusAction(this));
		deleteItem.setEnabled(enabled);
		if(corpora.size() > 1) {
			deleteItem.setText("Delete Corpora");
		}
		builder.addItem(".", deleteItem);
		
		builder.addSeparator(".", "s3");
		JMenuItem templateItem = new JMenuItem(new OpenSessionTemplateAction(this));
		templateItem.setEnabled(enabled);
		builder.addItem(".", templateItem);
		
		builder.addSeparator(".", "s4");
		setupCorpusFolderMenu(getSelectedCorpus(), builder);
	}

	/**
	 * Displays the session list menu
	 *
	 * @param clickPoint
	 */
	private void showSessionListContextMenu(Point clickPoint) {
		JPopupMenu contextMenu = new JPopupMenu();
		setupSessionListContextMenu(new MenuBuilder(contextMenu));

		contextMenu.show(sessionList, clickPoint.x, clickPoint.y);
	}
	
	private void setupSessionListContextMenu(MenuBuilder builder) {
		// add 'new session item'
		PhonUIAction<Void> createNewSessionAct = PhonUIAction.runnable(this::onShowCreateSessionButton);
		createNewSessionAct.putValue(Action.NAME, "New session...");
		createNewSessionAct.putValue(Action.SHORT_DESCRIPTION, "Create new session in selected corpus");
		createNewSessionAct.putValue(Action.SMALL_ICON, IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "note_add", IconSize.MEDIUM, UIManager.getColor("Button.foreground")));
		builder.addItem(".", createNewSessionAct).setEnabled(getSelectedCorpus() != null);
		
		List<String> selectedSessions = getSelectedSessionNames();
		boolean enabled = selectedSessions.size() > 0;
		
		builder.addSeparator(".", "s1");

		// open item
		OpenSessionAction openAct = new OpenSessionAction(this, false);
		if(builder.getRoot() instanceof JPopupMenu && !isBlindMode()) {
			openAct.putValue(OpenSessionAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
		}
		JMenuItem openItem = new JMenuItem(openAct);
		if(isBlindMode()) {
			openItem.setText(openItem.getText() + " (not blind mode)");
		}
		openItem.setEnabled(enabled);
		builder.addItem(".", openItem);
		
		final OpenSessionAction openBlindModeAct = new OpenSessionAction(this, true);
		openBlindModeAct.putValue(PhonUIAction.NAME, "Open session as transcriber... (blind mode)");
		openBlindModeAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Open session as a blind transcriber");
		if(builder.getRoot() instanceof JPopupMenu && isBlindMode()) {
			openBlindModeAct.putValue(OpenSessionAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
		}
		builder.addItem(".", openBlindModeAct).setEnabled(enabled);
		
		builder.addSeparator(".", "s2");

		// rename item
		JMenuItem duplicateItem = new JMenuItem(new DuplicateSessionAction(this));
		duplicateItem.setEnabled(enabled);
		if(selectedSessions.size() > 1) {
			duplicateItem.setText("Duplicate Sessions");
		}
		builder.addItem(".", duplicateItem);

		JMenuItem renameItem = new JMenuItem(new RenameSessionAction(this));
		renameItem.setEnabled(enabled);
		builder.addItem(".", renameItem);

		// delete item
		JMenuItem deleteItem = new JMenuItem(new DeleteSessionAction(this));
		deleteItem.setEnabled(enabled);
		if(selectedSessions.size() > 1) {
			deleteItem.setText("Delete Sessions");
		}
		builder.addItem(".", deleteItem);
	}

	@Override
	public void dispose() {
		super.dispose();
	}

	public void updateLists() {
		CorpusListModel corpusListModel =
			(CorpusListModel)corpusList.getModel();
		corpusListModel.refresh();
		corpusList.repaint();

		if(getSelectedCorpus() != null)
			corpusDetails.setCorpus(getSelectedCorpus());

		SessionListModel sessionListModel =
			(SessionListModel)sessionList.getModel();
		sessionListModel.refresh();
		if(corpusList.getSelectedIndex() >= 0 && corpusList.getSelectedIndex() < corpusList.getModel().getSize()
				&& corpusList.getSelectedValue() != null)
			sessionListModel.setCorpus(corpusList.getSelectedValue().toString());
		else
			sessionListModel.setCorpus(null);
		sessionList.repaint();
	}

	public void refreshProject() {
		final Project project = getProject();
		final ProjectRefresh impl = project.getExtension(ProjectRefresh.class);
		if(impl != null) {
			impl.refresh();
		}
		updateLists();
		updateProjectMediaLabel();
	}
	
	private class SessionNameTextCompleter extends TextCompleter {
		
		private String selectedMedia = null;
		
		public SessionNameTextCompleter(TextCompleterModel<?> model) {
			super(model);
		}
		
		@Override
		public void valueChanged(ListSelectionEvent e) {
			if(e.getValueIsAdjusting()) return;
			final int selectedIdx = getCompletionLiist().getSelectedIndex();
			if(selectedIdx >= 0 && selectedIdx < getCompletions().size()) {
				getCompletionLiist().ensureIndexIsVisible(selectedIdx);
				
				String completion = getCompletions().get(selectedIdx);
				String data = sessionNameCompleterModel.getData(completion);
				
				String text = getTextComponent().getText();
				final String replacementText = getModel().completeText(text, completion);
				
				SwingUtilities.invokeLater( () -> { getTextComponent().setText(replacementText); selectedMedia = data; } );
			}			
		}
		
	}
	
	private class CorpusListCellRenderer extends DefaultListCellRenderer {

		@Override
		public Component getListCellRendererComponent(
				JList list, Object value, int index,
				boolean isSelected, boolean cellHasFocus) {
			JLabel comp = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected,
					cellHasFocus);

			final String corpus = comp.getText();
			if(".".equals(corpus)) {
				comp.setText("<html><i>default</i></html>");
			}
			final String corpusPath = getProject().getCorpusPath(corpus);

			ImageIcon icon = IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "folder", IconSize.MEDIUM, comp.getForeground());

			if(gitController.hasGitFolder() && gitController.isOpen()) {
				try {
					final Status status = gitController.status(corpusPath);

					if(status.hasUncommittedChanges() || !status.getUntracked().isEmpty()) {
						ImageIcon modifiedIcn =
								IconManager.getInstance().createGlyphIcon('*',
										FontPreferences.getTitleFont(), comp.getForeground(), comp.getBackground());
						icon =
								IconManager.getInstance().createIconStrip(new ImageIcon[] { icon, modifiedIcn });
					}
				} catch (NoWorkTreeException | GitAPIException e) {
					LogUtil.warning(e);
				}
			}
			comp.setIcon(icon);

			return comp;
		}

	}

	private class SessionListCellRenderer extends DefaultListCellRenderer {

			private static final long serialVersionUID = 576253657524546120L;

			@Override
			public Component getListCellRendererComponent(
					JList list, Object value, int index,
					boolean isSelected, boolean cellHasFocus) {
				JLabel comp = (JLabel)
					super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

				final String corpus = getCorpusList().getSelectedValue();
				final String session = comp.getText();

				final String projectLocation = getProject().getLocation();
				final String sessionLocation = getProject().getSessionPath(corpus, session);

				// get relative path for session
				final Path projectPath = FileSystems.getDefault().getPath(projectLocation);
				final Path sessionPath = FileSystems.getDefault().getPath(sessionLocation);
				final Path relPath = projectPath.relativize(sessionPath);
				final String sessionRelPath = relPath.toString();

				ImageIcon icon =
						IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "draft", IconSize.MEDIUM, comp.getForeground());

				if(gitController.hasGitFolder() && gitController.isOpen()) {
					try {
						final Status status = gitController.status(sessionRelPath);

						if(status.hasUncommittedChanges()) {
							ImageIcon modifiedIcn =
									IconManager.getInstance().createGlyphIcon('*', FontPreferences.getTitleFont(), comp.getForeground(), comp.getBackground());
							icon =
									IconManager.getInstance().createIconStrip(new ImageIcon[] { icon, modifiedIcn });
						} else if(status.getUntracked().contains(sessionRelPath)) {
							ImageIcon modifiedIcn =
									IconManager.getInstance().createGlyphIcon('?', FontPreferences.getTitleFont(), comp.getForeground(), comp.getBackground());
							icon =
									IconManager.getInstance().createIconStrip(new ImageIcon[] { icon, modifiedIcn });
						} else if(status.getConflicting().contains(sessionRelPath)) {
							ImageIcon modifiedIcn =
									IconManager.getInstance().createGlyphIcon('C', FontPreferences.getTitleFont(), comp.getForeground(), comp.getBackground());
							icon =
									IconManager.getInstance().createIconStrip(new ImageIcon[] { icon, modifiedIcn });
						}
					} catch (NoWorkTreeException | GitAPIException e) {
						LogUtil.warning(e);
					}
				}

				// see if the transcript it locked...
				SessionListModel model = (SessionListModel)list.getModel();
				if(model.getProject().isSessionLocked(model.getCorpus(), value.toString())) {
					comp.setIcon(
							IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "lock", IconSize.MEDIUM, comp.getForeground()));
				}

				comp.setIcon(icon);

				return comp;
			}
		}

	public class NameDocument extends PlainDocument {

		@Override
		public void insertString(int offs, String str, AttributeSet a)
				throws BadLocationException {
			// only allow valid inputs
			String p1 = super.getText(0, offs);
			String p2 = super.getText(offs, getLength()-offs);
			String val = p1 + str + p2;

			if(!val.contains("/") && !val.contains("\\")) {
				File testFile = new File(val);
				try {
					testFile.getCanonicalPath();
					super.insertString(offs, str, a);
				} catch (IOException e) {
				}
			}
		}
	}
}
