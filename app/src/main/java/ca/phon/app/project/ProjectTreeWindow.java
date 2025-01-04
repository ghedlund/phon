package ca.phon.app.project;

import ca.phon.app.log.LogUtil;
import ca.phon.app.modules.EntryPointArgs;
import ca.phon.app.project.git.ProjectGitController;
import ca.phon.app.session.editor.SessionEditorEP;
import ca.phon.plugin.PluginEntryPointRunner;
import ca.phon.plugin.PluginException;
import ca.phon.project.Project;
import ca.phon.project.ProjectEvent;
import ca.phon.project.ProjectListener;
import ca.phon.session.SessionPath;
import ca.phon.ui.CommonModuleFrame;
import ca.phon.ui.DropDownIcon;
import ca.phon.ui.FlatButton;
import ca.phon.ui.IconStrip;
import ca.phon.ui.action.PhonUIAction;
import ca.phon.ui.menu.MenuBuilder;
import ca.phon.util.icons.IconManager;
import ca.phon.util.icons.IconSize;
import org.jdesktop.swingx.*;
import org.jdesktop.swingx.multisplitpane.DefaultSplitPaneModel;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Project window with contents displayed in two columns.  The left column is the project tree and the right
 * column is the preview of the selected item in the project tree.
 *
 */
public class ProjectTreeWindow extends CommonModuleFrame implements ClipboardOwner {

    private final Project project;

    private UndoManager undoManager;

    private JSplitPane splitPane;

    private ProjectFilesTree projectTree;

    private JPanel previewPanel;

    private IconStrip projectTreeToolbar;

    private JXStatusBar statusBar;
    private JXBusyLabel busyLabel;

    // git controller for git integration
    private ProjectGitController gitController;

    public ProjectTreeWindow(Project project) {
        super("");
        this.project = project;

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

        super.setTitle("Phon : " + project.getName() + " : Project Manager");

        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        init();
    }

    private void init() {
        setLayout(new BorderLayout());

        projectTree = new ProjectFilesTree(project);
        projectTree.addTreeSelectionListener(e -> {
            final Object selectedNode = projectTree.getLastSelectedPathComponent();
            previewPanel.removeAll();
            if(selectedNode == null) return;
            if(selectedNode == projectTree.getModel().getRoot()) {
                previewPanel.add(createProjectInfoPanel(), BorderLayout.CENTER);
            } else {
                final DefaultMutableTreeNode node = (DefaultMutableTreeNode)selectedNode;
                final Object userObject = node.getUserObject();
                final Path projectPath = Path.of(project.getLocation());
                if(userObject instanceof Path path) {
                    final Path fullPath = projectPath.resolve(path);
                    if(Files.isDirectory(fullPath)) {
                        previewPanel.add(createFolderInfoPanel(path), BorderLayout.CENTER);
                    } else {
                        previewPanel.add(createSessionInfoPanel(path), BorderLayout.CENTER);
                    }
                }
            }
            previewPanel.revalidate();
            previewPanel.repaint();
        });

        final JPanel leftPanel = new JPanel(new BorderLayout());
        final IconStrip projectTreeToolbar = new IconStrip(SwingConstants.HORIZONTAL);

        busyLabel = new JXBusyLabel(new Dimension(IconSize.SMALL.width(), IconSize.SMALL.height()));
        busyLabel.setBusy(false);
        projectTree.addPropertyChangeListener("scanning", e -> {
            busyLabel.setBusy((Boolean)e.getNewValue());
        });
        statusBar = new JXStatusBar();
        statusBar.add(busyLabel, JXStatusBar.Constraint.ResizeBehavior.FIXED);

        leftPanel.add(projectTreeToolbar, BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(projectTree), BorderLayout.CENTER);

        splitPane = new JSplitPane();
        splitPane.setLeftComponent(leftPanel);
        previewPanel = new JPanel(new BorderLayout());
        splitPane.setRightComponent(new JScrollPane(previewPanel));
        splitPane.setDividerLocation(350);
        add(splitPane, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        projectTree.setSelectionRow(0);
    }

    public ProjectFilesTree getProjectTree() {
        return this.projectTree;
    }

    public JSplitPane getSplitPane() {
        return this.splitPane;
    }

    public Project getProject() {
        return this.project;
    }


    // region PreviewPanels

    /**
     * Create info panel for provided mutable tree node.
     *
     * @param node
     */
    private JPanel createInfoPanel(DefaultMutableTreeNode node) {
        final JPanel infoPanel = new JPanel(new VerticalLayout());

        if (node == null) return infoPanel;

        return infoPanel;
    }

    /**
     * Create project information panel when selected item is project root.
     *
     * @return project information panel
     */
    private JPanel createProjectInfoPanel() {
        JPanel projectInfoPanel = new JPanel(new VerticalLayout());

        projectInfoPanel.add(new JXTitledSeparator("Actions"));
        final IconStrip projectActionsPanel = createProjectActionsStrip(getProject());
        projectInfoPanel.add(projectActionsPanel);

        return projectInfoPanel;
    }

    private JPanel createFolderInfoPanel(Path folderPath) {
        JPanel folderInfoPanel = new JPanel(new VerticalLayout());

        folderInfoPanel.add(new JXTitledSeparator("Actions"));
        final IconStrip folderActionsPanel = createFolderActionStrip(getProject(), folderPath);
        folderInfoPanel.add(folderActionsPanel);

        return folderInfoPanel;
    }

    private JPanel createSessionInfoPanel(Path sessionPath) {
        JPanel sessionInfoPanel = new JPanel(new VerticalLayout());

        sessionInfoPanel.add(new JXTitledSeparator("Actions"));
        final IconStrip sessionActionsPanel = createSessionActionStrip(getProject(), sessionPath);
        sessionInfoPanel.add(sessionActionsPanel);

        return sessionInfoPanel;
    }

    // endregion PreviewPanels

    // region Actions

    private IconStrip createProjectActionsStrip(Project project) {
        final IconStrip iconStrip = new IconStrip(SwingConstants.HORIZONTAL);

        iconStrip.add(createNewSessionButton(), IconStrip.IconStripPosition.LEFT);

        return iconStrip;
    }

    private IconStrip createFolderActionStrip(Project project, Path folderPath) {
        final IconStrip iconStrip = new IconStrip(SwingConstants.HORIZONTAL);

        iconStrip.add(createNewSessionButton(), IconStrip.IconStripPosition.LEFT);

        return iconStrip;
    }

    private IconStrip createSessionActionStrip(Project project, Path sessionPath) {
        final IconStrip iconStrip = new IconStrip(SwingConstants.HORIZONTAL);

        iconStrip.add(createOpenSessionButton(sessionPath), IconStrip.IconStripPosition.LEFT);

        return iconStrip;
    }

    private JButton createNewSessionButton() {
        final PhonUIAction<Void> newSessionAction = PhonUIAction.runnable(this::onNewSession);
        newSessionAction.putValue(PhonUIAction.NAME, "New Session");
        newSessionAction.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
        newSessionAction.putValue(FlatButton.ICON_NAME_PROP, "add_circle_outline");
        newSessionAction.putValue(FlatButton.ICON_SIZE_PROP, IconSize.XLARGE);
        final FlatButton newSessionButton = new FlatButton(newSessionAction);
        return newSessionButton;
    }

    private void onNewSession() {
        // show new session dialog
        final NewSessionDialog newSessionDialog = new NewSessionDialog(getProject());
        newSessionDialog.setSize(400, 300);
        newSessionDialog.setModal(true);
        newSessionDialog.setLocationRelativeTo(CommonModuleFrame.getCurrentFrame());
        newSessionDialog.setVisible(true);
    }

    private JButton createOpenSessionButton(Path sessionPath) {
        final PhonUIAction<Path> openSessionAction = PhonUIAction.consumer(this::onOpenSession, sessionPath);
        openSessionAction.putValue(PhonUIAction.NAME, "Open Session");
        openSessionAction.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
        openSessionAction.putValue(FlatButton.ICON_NAME_PROP, "folder_open");
        openSessionAction.putValue(FlatButton.ICON_SIZE_PROP, IconSize.XLARGE);
        final FlatButton openSessionButton = new FlatButton(openSessionAction);
        return openSessionButton;
    }

    private void onOpenSession(Path sessionPath) {
        final EntryPointArgs epArgs = new EntryPointArgs();
        epArgs.put(EntryPointArgs.PROJECT_OBJECT, getProject());
        epArgs.put(EntryPointArgs.SESSION_NAME, sessionPath.getFileName().toString());
        epArgs.put(EntryPointArgs.CORPUS_NAME, sessionPath.getParent().toString());
        try {
            PluginEntryPointRunner.executePlugin(SessionEditorEP.EP_NAME, epArgs);
        } catch (PluginException e) {
            LogUtil.severe(e);
        }
    }

    // endregion Actions

    // region UndoableEdits

    // endregion UndoableEdits

    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        // nothing to do
    }

}
