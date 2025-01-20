package ca.phon.app.workspace;

import ca.hedlund.desktopicons.*;
import ca.phon.app.log.LogUtil;
import ca.phon.app.menu.workspace.SelectWorkspaceCommand;
import ca.phon.app.welcome.WorkspaceTextStyler;
import ca.phon.ui.*;
import ca.phon.ui.action.*;
import ca.phon.ui.fonts.FontPreferences;
import ca.phon.ui.menu.MenuBuilder;
import ca.phon.util.*;
import ca.phon.util.icons.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.io.*;
import java.util.Iterator;

public class WorkspaceButton extends MultiActionButton {

	public WorkspaceButton() {
		super();

		init();
	}

	private void init() {
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		PhonUIAction<Void> selectHistoryAct = PhonUIAction.eventConsumer(this::onShowHistory);
		selectHistoryAct.putValue(Action.NAME, "Select workspace");
		selectHistoryAct.putValue(Action.SHORT_DESCRIPTION, "Change workspace folder...");

		ImageIcon workspaceIcnL = IconManager.getInstance().getFontIcon(
				IconManager.GoogleMaterialDesignIconsFontName, "folder", IconSize.MEDIUM, UIManager.getColor("Button.foreground")
		);
		DropDownIcon icn = new DropDownIcon(workspaceIcnL, 0, SwingConstants.BOTTOM);

		setTopLabelText(WorkspaceTextStyler.toHeaderText("Workspace Folder"));
		getTopLabel().setIcon(icn);
		getTopLabel().setFont(FontPreferences.getTitleFont());
		addAction(createShowWorkspaceAction());
		setDefaultAction(selectHistoryAct);
		update();
		PrefHelper.getUserPreferences().addPreferenceChangeListener((e) -> {
			if(e.getKey().equals(Workspace.WORKSPACE_FOLDER))
				update();
		});
	}

	public void update() {
		File workspaceFolder = Workspace.userWorkspaceFolder();
		setBottomLabelText(workspaceFolder.getAbsolutePath() + " (click to change)");
	}

	public void onShowHistory(PhonActionEvent pae) {
		final WorkspaceHistory history = new WorkspaceHistory();

		final JPopupMenu menu = new JPopupMenu();
		final MenuBuilder builder = new MenuBuilder(menu);

		for(File workspaceFolder:history) {
			if(workspaceFolder.equals(Workspace.userWorkspaceFolder()) && !workspaceFolder.exists()) {
				final PhonUIAction<Void> createWorkspaceFolderAct = PhonUIAction.runnable(this::onCreateWorkspace);
				createWorkspaceFolderAct.putValue(PhonUIAction.NAME, "Create workspace folder");
				createWorkspaceFolderAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Create workspace folder on disk");
				builder.addItem(".", createWorkspaceFolderAct);
			} else {
				ImageIcon workspaceIcn =
						IconManager.getInstance().getSystemIconForPath(
								workspaceFolder.getAbsolutePath(), "places/folder-workspace", IconSize.SMALL);
				final PhonUIAction<File> selectAction = PhonUIAction.consumer(this::onSelectFolder, workspaceFolder);
				selectAction.putValue(PhonUIAction.NAME, workspaceFolder.getAbsolutePath());
				selectAction.putValue(PhonUIAction.SMALL_ICON, workspaceIcn);
				builder.addItem(".", selectAction);
			}
		}

		ImageIcon browseIcn = IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "folder_open", IconSize.SMALL, UIManager.getColor("Button.foreground"));

		builder.addSeparator(".", "clear");

		JMenu removeItemMenu = builder.addMenu(".", "Remove item from history");
		removeItemMenu.addMenuListener(new MenuListener() {
			@Override
			public void menuSelected(MenuEvent e) {
				removeItemMenu.removeAll();
				Iterator<File> itr = history.iterator();
				while(itr.hasNext()) {
					File f = itr.next();
					PhonUIAction<File> removeAct = PhonUIAction.consumer(WorkspaceButton.this::removeFromHistory, f);
					removeAct.putValue(PhonUIAction.NAME, f.getAbsolutePath());
					removeAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Remove " + f.getAbsolutePath() + " from workspace history");

					removeItemMenu.add(removeAct).setEnabled(!f.equals(Workspace.userWorkspaceFolder()));

				}
			}

			@Override
			public void menuDeselected(MenuEvent e) {}

			@Override
			public void menuCanceled(MenuEvent e) {}
		});

		final PhonUIAction<Void> clearHistoryAct = PhonUIAction.runnable(this::onClearHistory);
		clearHistoryAct.putValue(PhonUIAction.NAME, "Clear history");
		clearHistoryAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Clear workspace history");
		builder.addItem(".@clear", clearHistoryAct);

		builder.addSeparator(".", "browse");

		final Action showWorkspaceAct = createShowWorkspaceAction();
		builder.addItem(".@browse", showWorkspaceAct);

		final SelectWorkspaceCommand cmd = new SelectWorkspaceCommand();
		cmd.putValue(Action.NAME, "Browse for workspace folder...");
		cmd.putValue(Action.SMALL_ICON, browseIcn);
		builder.addItem(".", cmd);


		menu.show(this, 0, getHeight());
	}

	public void removeFromHistory(File f) {
		WorkspaceHistory history = new WorkspaceHistory();
		Iterator<File> itr = history.iterator();
		while(itr.hasNext()) {
			File tf = itr.next();
			if(tf.equals(f)) {
				itr.remove();
				break;
			}
		}
		history.saveHistory();
	}

	public void onCreateWorkspace() {
		File workspaceFolder = Workspace.userWorkspaceFolder();
		if(!workspaceFolder.exists()) {
			boolean created = workspaceFolder.mkdirs();
			if(!created) {
				Toolkit.getDefaultToolkit().beep();
				LogUtil.warning("Workspace folder not created");
			}
		}
	}

	public void onShowWorkspace() {
		if(Desktop.isDesktopSupported()) {
			try {
				Desktop.getDesktop().open(Workspace.userWorkspaceFolder());
			} catch (IOException e) {
				LogUtil.warning(e);
				Toolkit.getDefaultToolkit().beep();
			}
		}
	}

	private Action createShowWorkspaceAction() {
		ImageIcon fsIcon = IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "open_in_browser", IconSize.SMALL, UIManager.getColor(FlatButtonUIProps.ICON_COLOR_PROP));
		ImageIcon fsIconL = IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "open_in_browser", IconSize.MEDIUM, UIManager.getColor(FlatButtonUIProps.ICON_COLOR_PROP));

		String fsName = "file system viewer";
		if(OSInfo.isWindows()) {
			fsName = "File Explorer";
		} else if(OSInfo.isMacOs()) {
			fsName = "Finder";
		}

		final PhonUIAction<Void> act = PhonUIAction.runnable(this::onShowWorkspace);
		act.putValue(PhonUIAction.NAME, "Show workspace in " + fsName);
		act.putValue(PhonUIAction.SHORT_DESCRIPTION, "Show workspace folder in " + fsName);
		act.putValue(PhonUIAction.SMALL_ICON, fsIcon);
		act.putValue(PhonUIAction.LARGE_ICON_KEY, fsIconL);

		return act;
	}

	public void onClearHistory() {
		final WorkspaceHistory history = new WorkspaceHistory();
		history.clearHistory();
		history.addToHistory(Workspace.userWorkspaceFolder());
	}

	public void onSelectFolder(File workspaceFolder) {
		Workspace.setUserWorkspaceFolder(workspaceFolder);
	}

	public void onResetWorkspace(PhonActionEvent pae) {
		final File defaultWorkspace = Workspace.defaultWorkspaceFolder();
		Workspace.setUserWorkspaceFolder(defaultWorkspace);
	}

}
