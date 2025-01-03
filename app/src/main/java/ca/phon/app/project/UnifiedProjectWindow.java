package ca.phon.app.project;

import bibliothek.gui.dock.common.*;
import bibliothek.gui.dock.common.event.CFocusListener;
import bibliothek.gui.dock.common.intern.CDockable;
import ca.phon.app.log.LogUtil;
import ca.phon.app.session.editor.EditorEvent;
import ca.phon.app.session.editor.EditorEventType;
import ca.phon.app.session.editor.RecordEditorPerspective;
import ca.phon.app.session.editor.SessionEditor;
import ca.phon.project.Project;
import ca.phon.session.Session;
import ca.phon.session.SessionPath;
import ca.phon.ui.menu.MenuManager;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Part of Phon 4.0+'s unified single window per project UI.  This window will provide access to project files
 * and support editing multiple sessions within the same window.
 *
 */
public final class UnifiedProjectWindow extends ProjectFrame {

    private CControl dockControl;

    private CWorkingArea workArea;

    private ProjectFilesTree projectTree;

    private Map<String, JComponent> viewMap = new HashMap<>();

    /**
     * Constructor
     *
     * @param project
     */
    public UnifiedProjectWindow(Project project) {
        super(project);
        init();
    }

    private void init() {
        projectTree = new ProjectFilesTree(getProject());
        viewMap.put("Project", new JScrollPane(projectTree));
        projectTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(!e.isPopupTrigger() && e.getClickCount() == 2) {
                    final TreePath tp = projectTree.getPathForLocation(e.getX(), e.getY());
                    if(tp.getLastPathComponent() instanceof DefaultMutableTreeNode mutableTreeNode) {
                        if(mutableTreeNode.isLeaf() && mutableTreeNode.getUserObject() instanceof Path path) {
                            openPath(path);
                        }
                    }
                }
            }
        });

        final ProjectStartPage startPage = new ProjectStartPage(getProject());
        viewMap.put("Start", startPage);

        initDockingView();
    }

    private void initDockingView() {
        setLayout(new BorderLayout());

        dockControl = new CControl(this);
        add(dockControl.getContentArea(), BorderLayout.CENTER);

        setupDefaultPerspective();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dockControl.destroy();
            }
        });
    }

    private void setupDefaultPerspective() {
        workArea = dockControl.createWorkingArea( "work" );

        CGrid grid = new CGrid( dockControl );
        grid.add( 1, 1, 4, 4, workArea );
        grid.add( 0, 0, 1, 4, new DefaultSingleCDockable( "Project", "Project", viewMap.get("Project") ));
        dockControl.getContentArea().deploy( grid );

        workArea.show(new DefaultSingleCDockable("Start", "Start", viewMap.get("Start")));
    }

    private void updateMenuBar(SessionEditor editor) {
        final JMenuBar menuBar = MenuManager.createWindowMenuBar(this);
        editor.setupMenu(menuBar);
        setJMenuBar(menuBar);
    }

    public void showSessionEditor(final SessionEditor editor) {
        final SessionPath sessionPath = editor.getSession().getSessionPath();
        final DefaultSingleCDockable singleCDockable = new DefaultSingleCDockable(sessionPath.toString(), sessionPath.toString(), editor);
        singleCDockable.setCloseable(true);
        singleCDockable.addFocusListener(new CFocusListener() {
            @Override
            public void focusGained(CDockable cDockable) {
                updateMenuBar(editor);
            }

            @Override
            public void focusLost(CDockable cDockable) {

            }
        });

        workArea.show(singleCDockable);
        singleCDockable.toFront();
    }

    private void openPath(Path path) {
        Path parentPath = path.getParent();
        final String corpusName = parentPath != null ? parentPath.toString() : ".";
        final String sessionName = path.getFileName().toString();
        try {
            final Session session = getProject().openSession(corpusName, sessionName);
            openSession(session);
        } catch (IOException e) {
            Toolkit.getDefaultToolkit().beep();
            LogUtil.severe(e);
            showErrorMessage(e.getMessage());
        }
    }

    public void openSession(SessionPath sessionPath) {
        try {
            final Session session = getProject().openSession(sessionPath.getFolder(), sessionPath.getSessionFile());
            openSession(session);
        } catch (IOException e) {
            Toolkit.getDefaultToolkit().beep();
            LogUtil.severe(e);
            showErrorMessage(e.getMessage());
        }
    }

    private void openSession(Session session) {
        final SessionEditor editor = new SessionEditor(getProject(), session, null);
        //editor.getViewModel().applyPerspective(RecordEditorPerspective.getPerspective("test"));
        showSessionEditor(editor);
        SwingUtilities.invokeLater(() -> {
            final EditorEvent<Void> ee = new EditorEvent<>(EditorEventType.EditorFinishedLoading, this, null);
            editor.getEventManager().queueEvent(ee);
        });
    }

}
