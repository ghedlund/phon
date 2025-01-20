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

import ca.hedlund.desktopicons.*;
import ca.phon.app.VersionInfo;
import ca.phon.app.menu.edit.PreferencesCommand;
import ca.phon.app.menu.file.*;
import ca.phon.app.project.*;
import ca.phon.extensions.*;
import ca.phon.plugin.*;
import ca.phon.ui.*;
import ca.phon.ui.action.PhonUIAction;
import ca.phon.ui.decorations.*;
import ca.phon.ui.fonts.FontPreferences;
import ca.phon.util.*;
import ca.phon.util.icons.*;
import org.jdesktop.swingx.*;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.painter.effects.GlowPathEffect;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Set;

/**
 * Entry window for the application.  This window provides access to
 * common project actions - create, browse, extract, preferences,
 * recent projects, and workspace setup.
 *
 */
public class WelcomeWindow extends CommonModuleFrame implements IExtendable {

	public final static String WORKSPACE_PROJECTS_COLLAPSED = WelcomeWindow.class.getName() + ".workspaceProjectCollapsed";
	public final static boolean DEFAULT_COLLAPSE_WORKSPACE_PROJECTS = true;
	private boolean showWorkspaceProjects = PrefHelper.getBoolean(WORKSPACE_PROJECTS_COLLAPSED, DEFAULT_COLLAPSE_WORKSPACE_PROJECTS);

	private DialogHeader header;

	private final static String HEADER_TITLE = "Welcome to Phon " + VersionInfo.getInstance().getVersionNoBuild();
	private final static String HEADER_MESSAGE = "";

	private JXMultiSplitPane splitPane;

	// action panel
	private TitledPanel actionsContainer;
	private JPanel actionsPanel;

	// action buttons
	private MultiActionButton newProjectButton;
	private MultiActionButton browseProjectButton;
	private MultiActionButton openPrefsButton;
	private MultiActionButton mediaPrefsButton;

	// recent projects
	private TitledPanel recentProjectsContainer;
	private JPanel recentProjectsPanel;
	private RecentProjectsList recentProjectsList;

	// workspace projects
	private TitledPanel workspaceContainer;
	private WorkspaceProjectsPanel workspaceProjectsPanel;
	
	private final ExtensionSupport extSupport = new ExtensionSupport(WelcomeWindow.class, this);

	public WelcomeWindow() {
		super();

		setWindowName("Welcome");
		init();
		
		extSupport.initExtensions();
	}

	private void init() {
		setLayout(new BorderLayout());

		header = new DialogHeader(HEADER_TITLE, HEADER_MESSAGE);
		add(header, BorderLayout.NORTH);

		// setup actions
		actionsPanel = new JPanel();

		newProjectButton = createNewButton();
		browseProjectButton = createBrowseButton();
		openPrefsButton = createPrefsButton();
		mediaPrefsButton = createMediaButton();

		actionsPanel.setLayout(new VerticalLayout());
//		actionsPanel.setPreferredSize(new Dimension(250, 0));
		actionsPanel.add(newProjectButton);
		actionsPanel.add(browseProjectButton);
		actionsPanel.add(openPrefsButton);

		// add plug-in actions
		int pluginActions = 0;
		for(IPluginExtensionPoint<WelcomeWindowAction> extPt:PluginManager.getInstance().getExtensionPoints(WelcomeWindowAction.class)) {
			final IPluginExtensionFactory<WelcomeWindowAction> factory = extPt.getFactory();
			final WelcomeWindowAction actionExtension = factory.createObject();
			final MultiActionButton btn = actionExtension.createButton(this);
			if(pluginActions++ == 0) {
				actionsPanel.add(new JXTitledSeparator("Other"));
			}
			actionsPanel.add(btn);
		}

		actionsContainer = new TitledPanel("Actions", actionsPanel);
//		add(actionsContainer, BorderLayout.WEST);
		
		workspaceProjectsPanel = new WorkspaceProjectsPanel();
		final JPanel cpane = new JPanel(new BorderLayout());
		cpane.add(workspaceProjectsPanel, BorderLayout.CENTER);
		workspaceContainer = new TitledPanel("Workspace", cpane);

		recentProjectsPanel = new JPanel();
		recentProjectsPanel.setLayout(new BorderLayout());
		recentProjectsList = new RecentProjectsList();
		final JScrollPane recentProjectsScroller = new JScrollPane(recentProjectsList);
		recentProjectsPanel.add(recentProjectsScroller, BorderLayout.CENTER);
		recentProjectsContainer = new TitledPanel("Recent Projects", recentProjectsPanel);

		PrefHelper.getUserPreferences().addPreferenceChangeListener( (p) -> {
			if(p.getKey().equals(RecentProjects.PROJECT_HISTORY_PROP)) {
				SwingUtilities.invokeLater( () -> recentProjectsList.updateProjectList() );
			}
		});

		String layoutDef = "(ROW (LEAF name=left weight=0.2) (COLUMN weight=0.8 (LEAF name=top weight=0.5) (LEAF name=bottom weight=0.5)))";
		MultiSplitLayout.Node modelRoot = MultiSplitLayout.parseModel(layoutDef);

		// Create the JXMultiSplitPane
		splitPane = new JXMultiSplitPane();
		splitPane.getMultiSplitLayout().setModel(modelRoot);

		splitPane.add(actionsContainer, "left");
		splitPane.add(workspaceContainer, "top");
		splitPane.add(recentProjectsContainer, "bottom");
		splitPane.setDividerSize(2);
		add(splitPane, BorderLayout.CENTER);

		addWindowFocusListener(new WindowAdapter() {
			@Override
			public void windowGainedFocus(WindowEvent e) {
//				refreshWorkspaceProjects();
			}
		});
	}

	private MultiActionButton createNewButton() {
		MultiActionButton retVal = new MultiActionButton();
		final ImageIcon newIcn =
				IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "create_new_folder",
						IconSize.MEDIUM, UIManager.getColor("Button.foreground"));

		String s1 = "Create Project";
		String s2 = "Create a new project";

		retVal.getTopLabel().setText(WorkspaceTextStyler.toHeaderText(s1));
		retVal.getTopLabel().setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
		retVal.getTopLabel().setFont(FontPreferences.getTitleFont());
		retVal.getTopLabel().setIcon(newIcn);
		retVal.getBottomLabel().setText(WorkspaceTextStyler.toDescText(s2));
		retVal.setOpaque(false);

		final NewProjectCommand newProjectCmd = new NewProjectCommand();
		newProjectCmd.putValue(NewProjectCommand.SMALL_ICON, newIcn);
		newProjectCmd.putValue(NewProjectCommand.LARGE_ICON_KEY, newIcn);
		retVal.setDefaultAction(newProjectCmd);

		BtnBgPainter bgPainter = new BtnBgPainter();
		retVal.setBackgroundPainter(bgPainter);
		retVal.addMouseListener(bgPainter);

		retVal.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		return retVal;
	}

	private MultiActionButton createBrowseButton() {
		MultiActionButton retVal = new MultiActionButton();
		final ImageIcon browseIcn =
				IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "folder_open",
						IconSize.MEDIUM, UIManager.getColor("Button.foreground"));

		String s1 = "Browse for Project";
		String s2 = "Browse for project folder on disk";

		retVal.getTopLabel().setText(WorkspaceTextStyler.toHeaderText(s1));
		retVal.getTopLabel().setIcon(browseIcn);
		retVal.getTopLabel().setFont(FontPreferences.getTitleFont());
		retVal.getTopLabel().setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
		retVal.getBottomLabel().setText(WorkspaceTextStyler.toDescText(s2));

		final OpenProjectCommand browseAct = new OpenProjectCommand();
		browseAct.putValue(Action.SMALL_ICON, browseIcn);
		browseAct.putValue(PhonUIAction.LARGE_ICON_KEY, browseIcn);
		browseAct.putValue(PhonUIAction.NAME, "Browse...");
		browseAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Browse for project folder on disk...");
		retVal.setOpaque(false);

		retVal.setDefaultAction(browseAct);

		BtnBgPainter bgPainter = new BtnBgPainter();
		retVal.setBackgroundPainter(bgPainter);
		retVal.addMouseListener(bgPainter);

		retVal.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		return retVal;
	}

	private MultiActionButton createPrefsButton() {
		MultiActionButton retVal = new MultiActionButton();
		final ImageIcon prefsIcn =
				IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "settings",
						IconSize.MEDIUM, UIManager.getColor("Button.foreground"));

		String s1 = "Edit Preferences";
		String s2 = "Modify application settings";

		retVal.setTopLabelText(WorkspaceTextStyler.toHeaderText(s1));
		retVal.getTopLabel().setIcon(prefsIcn);
		retVal.getTopLabel().setFont(FontPreferences.getTitleFont());
		retVal.getTopLabel().setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
		retVal.setBottomLabelText(WorkspaceTextStyler.toDescText(s2));

		final PreferencesCommand prefsAct = new PreferencesCommand();
		prefsAct.putValue(PhonUIAction.NAME, "Open preferences...");
		prefsAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Modify application settings...");
		prefsAct.putValue(PhonUIAction.SMALL_ICON, prefsIcn);
		prefsAct.putValue(PhonUIAction.LARGE_ICON_KEY, prefsIcn);

		BtnBgPainter bgPainter = new BtnBgPainter();
		retVal.setBackgroundPainter(bgPainter);
		retVal.addMouseListener(bgPainter);
		retVal.setDefaultAction(prefsAct);
		retVal.setOpaque(false);

		retVal.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		return retVal;
	}

	private MultiActionButton createMediaButton() {
		MultiActionButton retVal = new MultiActionButton();
		final ImageIcon newIcn = IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName,
				"video_library", IconSize.MEDIUM, UIManager.getColor("Button.foreground"));

		String s1 = "Select Media Folders";
		String s2 = "Set up a list of folders where media can be found";

		retVal.setTopLabelText(WorkspaceTextStyler.toHeaderText(s1));
		retVal.getTopLabel().setIcon(newIcn);
		retVal.getTopLabel().setFont(FontPreferences.getTitleFont());
		retVal.getTopLabel().setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
		retVal.setBottomLabelText(WorkspaceTextStyler.toDescText(s2));

		final PreferencesCommand prefsAct = new PreferencesCommand("Media");
		prefsAct.putValue(PhonUIAction.NAME, "Select media folders...");
		prefsAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Choose where Phon looks for media files...");
		prefsAct.putValue(PhonUIAction.SMALL_ICON, newIcn);
		prefsAct.putValue(PhonUIAction.LARGE_ICON_KEY, newIcn);

		BtnBgPainter bgPainter = new BtnBgPainter();
		retVal.setBackgroundPainter(bgPainter);
		retVal.addMouseListener(bgPainter);
		retVal.setOpaque(false);
		retVal.setDefaultAction(prefsAct);

		retVal.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		return retVal;
	}

	public static class BtnBgPainter extends MouseInputAdapter implements Painter<MultiActionButton> {

		private Color selectedColor = new Color(0, 100, 200, 100);

		private boolean useSelected = false;
		
		@Override
		public void paint(Graphics2D g, MultiActionButton obj, int width,
				int height) {
			if(useSelected) {
				GlowPathEffect effect = new GlowPathEffect();
				effect.setRenderInsideShape(true);
				effect.setBrushColor(selectedColor);

				// get rectangle
				Rectangle2D.Double boundRect =
					new Rectangle2D.Double(0.0f, 0.0f, (double)width, (double)height);

				effect.apply(g, boundRect, 0, 0);
			}
		}

		@Override
		public void mouseEntered(MouseEvent me) {
			useSelected = true;
		}

		@Override
		public void mouseExited(MouseEvent me) {
			useSelected = false;
		}

	}

	public void refreshWorkspaceProjects() {
		workspaceProjectsPanel.refresh();
	}

	public Container getActionList() {
		return actionsPanel;
	}

	public Set<Class<?>> getExtensions() {
		return extSupport.getExtensions();
	}

	public <T> T getExtension(Class<T> cap) {
		return extSupport.getExtension(cap);
	}

	public <T> T putExtension(Class<T> cap, T impl) {
		return extSupport.putExtension(cap, impl);
	}

	public <T> T removeExtension(Class<T> cap) {
		return extSupport.removeExtension(cap);
	}
	
	
}
