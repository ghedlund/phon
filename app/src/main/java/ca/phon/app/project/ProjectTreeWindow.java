package ca.phon.app.project;

import ca.phon.app.log.LogUtil;
import ca.phon.app.project.git.ProjectGitController;
import ca.phon.project.Project;
import ca.phon.project.ProjectEvent;
import ca.phon.project.ProjectListener;
import ca.phon.ui.CommonModuleFrame;
import ca.phon.ui.DropDownIcon;
import ca.phon.ui.FlatButton;
import ca.phon.ui.IconStrip;
import ca.phon.ui.action.PhonUIAction;
import ca.phon.ui.menu.MenuBuilder;
import ca.phon.util.icons.IconManager;
import ca.phon.util.icons.IconSize;
import org.jdesktop.swingx.JXBusyLabel;
import org.jdesktop.swingx.JXMultiSplitPane;
import org.jdesktop.swingx.JXTitledSeparator;
import org.jdesktop.swingx.VerticalLayout;
import org.jdesktop.swingx.multisplitpane.DefaultSplitPaneModel;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;

/**
 * Project window with contents displayed in two columns.  The left column is the project tree and the right
 * column is the preview of the selected item in the project tree.
 *
 */
public class ProjectTreeWindow extends CommonModuleFrame {

    private final Project project;

    private UndoManager undoManager;

    private JSplitPane splitPane;

    private ProjectFilesTree projectTree;

    private JPanel previewPanel;

    private IconStrip projectTreeToolbar;
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
        setLayout(new BoxLayout(getContentPane(), BoxLayout.X_AXIS));

        projectTree = new ProjectFilesTree(project);
        projectTree.addTreeSelectionListener(e -> {
            final Object selectedNode = projectTree.getLastSelectedPathComponent();
            previewPanel.removeAll();
            if(selectedNode == projectTree.getModel().getRoot()) {
                previewPanel.add(createProjectInfoPanel(), BorderLayout.CENTER);
            } else {

            }
            previewPanel.revalidate();
            previewPanel.repaint();


//            if(selectedNode instanceof DefaultMutableTreeNode) {
//                final DefaultMutableTreeNode node = (DefaultMutableTreeNode)selectedNode;
//                final Object userObject = node.getUserObject();
//                if(userObject instanceof Project) {
//                    previewPanel.removeAll();
//                    previewPanel.add(new JLabel("Project: " + project.getName()));
//                } else if(userObject instanceof ProjectFile) {
//                    final ProjectFile file = (ProjectFile)userObject;
//                    previewPanel.removeAll();
//                    previewPanel.add(new JLabel("File: " + file.getName()));
//                }
//                previewPanel.revalidate();
//                previewPanel.repaint();
//            }
        });

        final JPanel leftPanel = new JPanel(new BorderLayout());
        final IconStrip projectTreeToolbar = new IconStrip(SwingConstants.HORIZONTAL);

        busyLabel = new JXBusyLabel(new Dimension(IconSize.SMALL.width(), IconSize.SMALL.height()));
        busyLabel.setBusy(false);
        projectTreeToolbar.add(busyLabel, IconStrip.IconStripPosition.RIGHT);
        projectTree.addPropertyChangeListener("scanning", e -> {
            busyLabel.setBusy((Boolean)e.getNewValue());
        });

        leftPanel.add(projectTreeToolbar, BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(projectTree), BorderLayout.CENTER);

        splitPane = new JSplitPane();
        splitPane.setLeftComponent(leftPanel);
        previewPanel = new JPanel(new BorderLayout());
        splitPane.setRightComponent(new JScrollPane(previewPanel));
        splitPane.setDividerLocation(350);
        add(splitPane);

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
     * Create project information panel when selected item is project root.
     *
     * @return project information panel
     */
    private JPanel createProjectInfoPanel() {
        JPanel projectInfoPanel = new JPanel(new VerticalLayout());
        JPanel mediaFolderPanel = new JPanel(new GridBagLayout());
        mediaFolderPanel.setOpaque(false);
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

        mediaFolderPanel.add(new JLabel("Project folder:"), gbc);
        ++gbc.gridx;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        final JLabel projectFolderLabel = new JLabel(getProject().getLocation());
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
        mediaFolderPanel.add(projectFolderLabel, gbc);

        ++gbc.gridy;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;

//        final JPopupMenu projectMediaFolderMenu = new JPopupMenu();
//        projectMediaFolderMenu.addPopupMenuListener(new PopupMenuListener() {
//
//            @Override
//            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
//                projectMediaFolderMenu.removeAll();
//                //setupProjectMediaFolderMenu(new MenuBuilder(projectMediaFolderMenu));
//            }
//
//            @Override
//            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
//
//            }
//
//            @Override
//            public void popupMenuCanceled(PopupMenuEvent e) {
//
//            }
//
//        });
//
//        mediaFolderPanel.add(new JLabel("Media folder:"), gbc);
//        final JLabel projectMediaFolderLabel = new JLabel();
//        //updateProjectMediaLabel();
//        projectMediaFolderLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
//        projectMediaFolderLabel.addMouseListener(new MouseListener() {
//
//            @Override
//            public void mouseReleased(MouseEvent e) {
//            }
//
//            @Override
//            public void mousePressed(MouseEvent e) {
//                projectMediaFolderMenu.show(projectMediaFolderLabel, 0, projectMediaFolderLabel.getHeight());
//            }
//
//            @Override
//            public void mouseExited(MouseEvent e) {
//            }
//
//            @Override
//            public void mouseEntered(MouseEvent e) {
//            }
//
//            @Override
//            public void mouseClicked(MouseEvent e) {
//            }
//
//        });



//        ++gbc.gridx;
//        gbc.fill = GridBagConstraints.HORIZONTAL;
//        gbc.weightx = 1.0;
//        mediaFolderPanel.add(projectMediaFolderLabel, gbc);

        projectInfoPanel.add(mediaFolderPanel);
        projectInfoPanel.add(new JXTitledSeparator("Actions"));
        final IconStrip projectActionsPanel = createProjectActionsStrip(getProject());
        projectInfoPanel.add(projectActionsPanel);

        return projectInfoPanel;
    }

    // endregion PreviewPanels

    // region Actions

    private IconStrip createProjectActionsStrip(Project project) {
        final IconStrip iconStrip = new IconStrip(SwingConstants.HORIZONTAL);

        iconStrip.add(createNewSessionButton(), IconStrip.IconStripPosition.LEFT);

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

    // endregion Actions

    // region UndoableEdits

    // endregion UndoableEdits
}
