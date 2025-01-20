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
import ca.phon.app.modules.EntryPointArgs;
import ca.phon.app.project.DesktopProjectFactory;
import ca.phon.app.workspace.Workspace;
import ca.phon.plugin.PluginEntryPointRunner;
import ca.phon.project.*;
import ca.phon.project.exceptions.ProjectConfigurationException;
import ca.phon.session.format.DateFormatter;
import ca.phon.ui.*;
import ca.phon.ui.action.*;
import ca.phon.ui.nativedialogs.*;
import ca.phon.util.OSInfo;
import ca.phon.util.icons.*;
import ca.phon.worker.*;
import ca.phon.worker.PhonTask.TaskStatus;
import com.jgoodies.forms.layout.*;
import org.jdesktop.swingx.*;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.*;

/**
 * List for displaying project in a given directory.
 * @author ghedlund
 *
 */
public class FolderProjectList extends JPanel {
	
	private enum SortBy {
		NAME,
		MOD_DATE;

		private String[] displayNames = {
				"Name", "Modified"
		};
		
		public String getDisplayName() {
			return displayNames[ordinal()];
		}
		
		@Override
		public String toString() {
			return getDisplayName();
		}

	};
	
	/**
	 * The folder we are displaying project from
	 */
	private File projectFolder;
	
	/**
	 * Place to put project buttons
	 */
	private JPanel listPanel;
	private JScrollPane listScroller;
	
	private SortPanel sortBar;
	
	/**
	 * List of project files
	 */
	private List<MultiActionButton> projectButtons =
		new ArrayList<MultiActionButton>();
	
	/**
	 * Constructor
	 */
	public FolderProjectList() {
		this(Workspace.userWorkspaceFolder());
	}
	
	public FolderProjectList(File f) {
		this.projectFolder = f;
		init();
	}
	
	private void init() {
		setLayout(new BorderLayout());
		
		listPanel = new ButtonPanel();
		BoxLayout bl = new BoxLayout(listPanel, BoxLayout.Y_AXIS);
		listPanel.setLayout(bl);
		
		listScroller = new JScrollPane(listPanel);
		listScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		listScroller.getViewport().setBackground(Color.white);
		
		add(listScroller, BorderLayout.CENTER);
		
		sortBar = new SortPanel();
		add(sortBar, BorderLayout.NORTH);
		
		SwingUtilities.invokeLater(this::scanDirectory);
	}
	
	public void setFolder(File f) {
		this.projectFolder = f;
		scanDirectory();
		updateProjectList();
	}
	
	public File getFolder() {
		return this.projectFolder;
	}

	
	private void updateProjectList() {
		listPanel.removeAll();
		listPanel.revalidate();
		listPanel.repaint();

		boolean stripRow = false;
		for(MultiActionButton btn:projectButtons) {
			listPanel.add(btn);

			if(stripRow) {
				btn.setBackground(PhonGuiConstants.PHON_UI_STRIP_COLOR);
				stripRow = false;
			} else {
				btn.setBackground(Color.white);
				stripRow = true;
			}
		}

		revalidate();
		listPanel.revalidate();
		listPanel.repaint();
	}
	
	private void scanDirectory() {
		projectButtons.clear();
		var worker = new ScanFolderWorker();
		worker.execute();
	}

	private LocalProjectButton getProjectButton(File f) {
		LocalProjectButton retVal = new LocalProjectButton(f);
		
		ImageIcon icon = IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "folder_open", IconSize.SMALL, UIManager.getColor(FlatButtonUIProps.ICON_COLOR_PROP));
		ImageIcon iconL = IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "folder_open", IconSize.MEDIUM, UIManager.getColor(FlatButtonUIProps.ICON_COLOR_PROP));
		
		PhonUIAction<LocalProjectButton> openAction = PhonUIAction.eventConsumer(this::onOpenProject, retVal);
		
		openAction.putValue(Action.NAME, "Open project");
		openAction.putValue(Action.SHORT_DESCRIPTION, "Open: " + f.getAbsolutePath());
		openAction.putValue(Action.SMALL_ICON, icon);
		openAction.putValue(Action.LARGE_ICON_KEY, iconL);
		retVal.setDefaultAction(openAction);
		
		ImageIcon fsIcon = IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "open_in_browser", IconSize.SMALL, UIManager.getColor(FlatButtonUIProps.ICON_COLOR_PROP));
		ImageIcon fsIconL = IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "open_in_browser", IconSize.MEDIUM, UIManager.getColor(FlatButtonUIProps.ICON_COLOR_PROP));

		String fsName = "file system viewer";
		if(OSInfo.isWindows()) {
			fsName = "File Explorer";
		} else if(OSInfo.isMacOs()) {
			fsName = "Finder";
		}
		
		PhonUIAction<LocalProjectButton> showAction = PhonUIAction.eventConsumer(this::onShowProject, retVal);
		showAction.putValue(Action.NAME, "Show project in " + fsName);
		showAction.putValue(Action.SMALL_ICON, fsIcon);
		showAction.putValue(Action.LARGE_ICON_KEY, fsIconL);
		showAction.putValue(Action.SHORT_DESCRIPTION, "Show project in " + fsName);
		retVal.addAction(showAction);
		
		ImageIcon archiveIcn = IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "folder_zip", IconSize.SMALL, UIManager.getColor(FlatButtonUIProps.ICON_COLOR_PROP));
		ImageIcon archiveIcnL = IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "folder_zip", IconSize.MEDIUM, UIManager.getColor(FlatButtonUIProps.ICON_COLOR_PROP));

		
		PhonUIAction<LocalProjectButton> archiveAction = PhonUIAction.eventConsumer(this::onArchiveProject, retVal);
		archiveAction.putValue(Action.NAME, "Zip project");
		archiveAction.putValue(Action.SHORT_DESCRIPTION, "Create .zip archive of phon project...");
		archiveAction.putValue(Action.SMALL_ICON, archiveIcn);
		archiveAction.putValue(Action.LARGE_ICON_KEY, archiveIcnL);
		retVal.addAction(archiveAction);

		final ImageIcon folderIcon = IconManager.getInstance().getFontIcon(
				IconManager.GoogleMaterialDesignIconsFontName, "folder", IconSize.MEDIUM, UIManager.getColor(FlatButtonUIProps.ICON_COLOR_PROP));
		retVal.getTopLabel().setIcon(folderIcon);
		
		return retVal;
	}
	
	public void refresh() {
		scanDirectory();
	}
	
	private class ProjectComparator implements Comparator<MultiActionButton> {

		private SortBy sortBy = SortBy.NAME;
		
		public ProjectComparator() {
			this(SortBy.NAME);
		}
		
		public ProjectComparator(SortBy by) {
			sortBy = by;
		}
		
		@Override
		public int compare(MultiActionButton mo1, MultiActionButton mo2) {
			int retVal = 0;
			
			LocalProjectButton o1 = (LocalProjectButton)mo1;
			LocalProjectButton o2 = (LocalProjectButton)mo2;
			
			if(sortBy == SortBy.NAME) {
				String o1Name = o1.getProjectFile().getName();
				String o2Name = o2.getProjectFile().getName();
				
				retVal = o1Name.compareTo(o2Name);
			} else if(sortBy == SortBy.MOD_DATE) {
				Long o1Mod = o1.getProjectFile().lastModified();
				Long o2Mod = o2.getProjectFile().lastModified();
				
				retVal = o2Mod.compareTo(o1Mod);
			}
			
			return retVal;
		}
		
	}
	
	private class SortPanel extends JPanel {
		
		private JXRadioGroup<SortBy> sortByGrp
			= new JXRadioGroup<SortBy>();
		
		public SortPanel() {
			init();
		}
		
		private void init() {
			// create border
			MatteBorder lineBorder = 
				BorderFactory.createMatteBorder(1, 1, 1, 1, Color.lightGray);
			setBorder(lineBorder);
			
			FormLayout layout = new FormLayout(
					"2dlu, left:pref, fill:pref:grow, right:pref, 2dlu", "pref");
			CellConstraints cc = new CellConstraints();
			setLayout(layout);
			
			JXLabel titleLabel = new JXLabel("Sort by:");
			add(titleLabel, cc.xy(2,1));
			
			for(SortBy sortBy:SortBy.values()) {
				sortByGrp.add(sortBy);
			}
			
			sortByGrp.setOpaque(false);
			sortByGrp.setSelectedValue(SortBy.NAME);
			
			for(int compIdx = 0; compIdx < sortByGrp.getChildButtonCount(); compIdx++) {
				AbstractButton btn = sortByGrp.getChildButton(compIdx);
				btn.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						JRadioButton btn = (JRadioButton)e.getSource();
						if(btn.isSelected()) {
							Collections.sort(projectButtons, new ProjectComparator(sortByGrp.getSelectedValue()));
							updateProjectList();
						}
					}
				});
			}
			add(sortByGrp, cc.xy(3,1));
			
			ImageIcon icn = IconManager.getInstance().getFontIcon(
					IconManager.GoogleMaterialDesignIconsFontName, "refresh", IconSize.SMALL, UIManager.getColor(FlatButtonUIProps.ICON_COLOR_PROP)
			);
			JLabel refreshLabel = new JLabel("<html><u style='color: rgb(0, 90, 140);'>Refresh</u></html>");
			refreshLabel.setIcon(icn);
			refreshLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			refreshLabel.setToolTipText("Refresh project list");
			refreshLabel.addMouseListener(new MouseInputAdapter() {
				
				@Override
				public void mouseClicked(MouseEvent me) {
					refresh();
				}
				
			});
			add(refreshLabel, cc.xy(4,1));
		}
		
	}
	
	/*
	 * UI Actions
	 */
	public void onArchiveProject(PhonActionEvent<LocalProjectButton> pae) {
		final LocalProjectButton btn = pae.getData();
		final ProjectFactory factory = new DesktopProjectFactory();
		try {
			final Project project = factory.openProject(btn.getProjectFile());
			final String today = DateFormatter.dateTimeToString(LocalDate.now());
			
			File backupsDir = new File(Workspace.userWorkspaceFolder(), "backups");
			if(!backupsDir.exists()) {
				backupsDir.mkdirs();
			}
			File destFile = new File(backupsDir, project.getName() + "-" + today + ".zip");
			
			int fIdx = 1;
			while(destFile.exists()) {
				destFile = 
					new File(Workspace.userWorkspaceFolder(), 
							"backups" + File.separator + project.getName() + "-" + today + "(" + (fIdx++) + ").zip");
			}
			
			ProjectArchiveTask task = new ProjectArchiveTask(project, destFile, true, false);
			task.setName("Archiving: " + project.getName());
			task.addTaskListener(new PhonTaskListener() {
				
				@Override
				public void statusChanged(PhonTask task, TaskStatus oldStatus,
						TaskStatus newStatus) {
					if(newStatus == TaskStatus.FINISHED) {
						long curTime = System.currentTimeMillis();
						long totalTime = task.getStartTime() - curTime;
						
						if(totalTime < 500) {
							try {
								Thread.sleep(500 - totalTime);
							} catch (InterruptedException e) {}
						}
						
						// refresh
						refresh();
					} else if(newStatus == TaskStatus.ERROR) {
						// display dialog
						NativeDialogs.showMessageDialog(CommonModuleFrame.getCurrentFrame(), new NativeDialogListener() {
							@Override
							public void nativeDialogEvent(NativeDialogEvent event) {
							}
						}, null, "Error archiving project", "Reason: " + 
							(task.getException() != null ? task.getException().getMessage() : "no reason given"));
						
						refresh();
					}
				}
				
				@Override
				public void propertyChanged(PhonTask task, String property,
						Object oldValue, Object newValue) {
				}
			});
			
			PhonTaskButton newBtn = new PhonTaskButton(task);
			
			int idx = projectButtons.indexOf(btn);
			projectButtons.remove(idx);
			projectButtons.add(idx, newBtn);
			updateProjectList();
			
			PhonWorker.getInstance().invokeLater(task);
		} catch (IOException | ProjectConfigurationException e) {
			LogUtil.warning(e);
		}
	}
	
	public void onOpenProject(PhonActionEvent<LocalProjectButton> pae) {
		LocalProjectButton btn = pae.getData();
		
		final EntryPointArgs args = new EntryPointArgs();
		args.put(EntryPointArgs.PROJECT_LOCATION, btn.getProjectFile().getAbsolutePath());
		
		PluginEntryPointRunner.executePluginInBackground("OpenProject", args);
	}

	public void onShowProject(PhonActionEvent<LocalProjectButton> pae) {
		LocalProjectButton btn = pae.getData();
		if(Desktop.isDesktopSupported()) {
			try {
				Desktop.getDesktop().open(btn.getProjectFile());
			} catch (IOException e) {
				LogUtil.warning(e);
				Toolkit.getDefaultToolkit().beep();
			}
		}
	}

	private class ScanFolderWorker extends SwingWorker<List<File>, File> {

		@Override
		protected List<File> doInBackground() throws Exception {
			List<File> projectFiles = new ArrayList<>();
			if(projectFolder == null || projectFolder.listFiles() == null) return projectFiles;

			// use java nio to list files
			final Path projectFolderPath = projectFolder.toPath();
			// stream files using java nio
			try(final var stream = java.nio.file.Files.list(projectFolderPath)) {
				stream.forEach( (p) -> {
					if(java.nio.file.Files.isDirectory(p)
						&& !p.getFileName().startsWith("~")
						&& !p.getFileName().endsWith("~")
						&& !p.getFileName().startsWith("__")
						&& !p.getFileName().equals("backups")) {
						final var projectFile = p.toFile();
						if(projectFile.exists()) {
							publish(projectFile);
							projectFiles.add(projectFile);
						}
					}
				});
			} catch (IOException e) {
				LogUtil.warning(e);
			}

			return projectFiles;
		}

		@Override
		protected void process(List<File> chunks) {
			for(File f:chunks) {
				projectButtons.add(getProjectButton(f));
			}
		}

		@Override
		protected void done() {
//			Collections.sort(projectButstons, new ProjectComparator(sortBar.sortByGrp.getSelectedValue()));
            updateProjectList();
		}

	}

	private class ButtonPanel extends JPanel implements Scrollable {

		@Override
		public Dimension getPreferredScrollableViewportSize() {
			return null;
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle arg0, int arg1,
				int arg2) {
			return 20;
		}

		@Override
		public boolean getScrollableTracksViewportHeight() {
			return false;
		}

		@Override
		public boolean getScrollableTracksViewportWidth() {
			return true;
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle arg0, int arg1, int arg2) {
			return 10;
		}
		
	}
}
