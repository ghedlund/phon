package ca.phon.app.project;

import ca.phon.app.log.LogUtil;
import ca.phon.project.Project;
import ca.phon.session.io.SessionInputFactory;
import ca.phon.ui.fonts.FontPreferences;
import ca.phon.util.icons.IconManager;
import ca.phon.util.icons.IconSize;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.*;
import java.awt.*;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ProjectFilesTree extends JTree implements TreeWillExpandListener {

    /**
     * Special folder for media folders.  Global and project media folders will be added
     * as children of this node.
     */
    public static final ProjectTreeSpecialFolder MEDIA_FOLDER = new ProjectTreeSpecialFolder() {
        @Override
        public String getName() {
            return "Media";
        }

        @Override
        public String getDescription() {
            return "Media folders";
        }

        @Override
        public ImageIcon getIcon() {
            return IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "video_library", IconSize.MEDIUM, Color.DARK_GRAY);
        }
    };

    private boolean showHiddenFiles = false;

    private boolean showProjectFiles = false;

    private final Project project;

    /**
     * Create tree for project
     *
     * @param project
     * @return tree for project
     */
    public static TreeNode treeForProject(Project project, boolean includeAllFiles, boolean includeHidden) {
        final DefaultMutableTreeNode root = new DefaultMutableTreeNode(project);
        final Path projectFolderPath = Path.of(project.getLocation());
        scanFolder(projectFolderPath, projectFolderPath, includeAllFiles, includeHidden, false, root);

        // add special folders
        final DefaultMutableTreeNode mediaFolders = new DefaultMutableTreeNode(MEDIA_FOLDER);
        root.add(mediaFolders);

        return root;
    }

    private static boolean setupScriptFolder(Path projectFolder, DefaultMutableTreeNode parent) {
        final Path scriptsFolder = Path.of(projectFolder.toString(), "__res/scripts");
        if(!Files.exists(scriptsFolder)) {
            return false;
        }

        // add all .js and .groovy files
        try(DirectoryStream<Path> stream = Files.newDirectoryStream(scriptsFolder)) {
            for(Path p:stream) {
                final DefaultMutableTreeNode fileNode = new DefaultMutableTreeNode(p.getFileName());
                parent.add(fileNode);
            }
        } catch (IOException e) {
            LogUtil.severe(e);
        }

        return false;
    }

    private static void scanFolder(Path rootPath, Path folderPath, boolean includeAllFiles, boolean includeHidden, boolean recursive, TreeNode parent) {
        try(DirectoryStream<Path> stream = Files.newDirectoryStream(folderPath)) {
            List<Path> pathList = new ArrayList<>();
            for(Path p:stream) {
                pathList.add(p);
            }
            Comparator<Path> pathComparator = (p1, p2) -> {
                if(Files.isDirectory(p1) && Files.isRegularFile(p2)) {
                    return 1;
                } else if(Files.isRegularFile(p1) && Files.isDirectory(p2)) {
                    return -1;
                } else {
                    return p1.toString().compareTo(p2.toString());
                }
            };
            Collections.sort(pathList, pathComparator);
            for(Path p:pathList) {
                final Path relativePath = rootPath.relativize(p);
                if(Files.isDirectory(p) && folderFilter(p, includeHidden)) {
                    final DefaultMutableTreeNode folderNode = new DefaultMutableTreeNode(relativePath);
                    ((DefaultMutableTreeNode)parent).add(folderNode);
                    if(recursive)
                        scanFolder(rootPath, p, includeAllFiles, includeHidden, true, folderNode);
                    else {
                        // add a dummy node
                        folderNode.add(new DefaultMutableTreeNode("..."));
                    }
                } else if(fileFilter(p, includeAllFiles, includeHidden)) {
                    final DefaultMutableTreeNode fileNode = new DefaultMutableTreeNode(relativePath);
                    ((DefaultMutableTreeNode)parent).add(fileNode);
                }
            }
        } catch (IOException e) {
            LogUtil.severe(e);
        }
    }

    private static boolean folderFilter(Path projectFolder, boolean includeHidden) throws IOException {
        if(Files.isHidden(projectFolder) && !includeHidden) return false;
        final String folderName = projectFolder.getFileName().toString();
        if(!folderName.startsWith("~")
                && !folderName.endsWith("~")
                && !folderName.startsWith(".")
                && !folderName.startsWith("__"))
            return true;
        else
            return false;
    }

    private static boolean fileFilter(Path projectFile, boolean includeProjectFiles, boolean includeHidden) throws IOException {
        if(Files.isHidden(projectFile) && !includeHidden) return false;
        final String fileName = projectFile.getFileName().toString();
        if(!fileName.startsWith("~")
                && !fileName.endsWith("~")
                && !fileName.startsWith(".")
                && !fileName.startsWith("__")) {
            final SessionInputFactory inputFactory = new SessionInputFactory();
            if(inputFactory.createReaderForFile(projectFile.toFile()) == null) {
                return includeProjectFiles;
            }
            return true;
        } else
            return false;
    }

    public ProjectFilesTree(Project project) {
        super(treeForProject(project,  false,false));
        this.project = project;
        setCellRenderer(new ProjectFilesCellRenderer());
        setRowHeight(IconSize.MEDIUM.height());

        addTreeWillExpandListener(this);
    }

    @Override
    public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
        final DefaultMutableTreeNode node = (DefaultMutableTreeNode)event.getPath().getLastPathComponent();
        if(node.getChildCount() == 1 && node.getChildAt(0).isLeaf()
            && ((DefaultMutableTreeNode)node.getChildAt(0)).getUserObject().toString().equals("...")) {

            final Path path = Path.of(project.getLocation(), node.getUserObject().toString());
            firePropertyChange("scanning", false, true);
            new FolderScanner(path, false, false, false, node).execute();
        }
    }

    @Override
    public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {

    }

    private final class ProjectFilesCellRenderer extends DefaultTreeCellRenderer {

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            JLabel retVal = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            retVal.setFont(FontPreferences.getTitleFont().deriveFont(14.0f));
            if(value instanceof DefaultMutableTreeNode node) {
                if(node.getUserObject() instanceof Path path) {
                    final Path fullPath = Path.of(project.getLocation(), path.toString());
                    retVal.setText(path.getFileName().toString());
                    retVal.setIcon(getIcon(fullPath));
                } else if(node.getUserObject() instanceof ProjectTreeSpecialFolder specialFolder) {
                    retVal.setText(specialFolder.getName());
                    retVal.setToolTipText(specialFolder.getDescription());
                    retVal.setIcon(specialFolder.getIcon());
                } else {
                    retVal.setIcon(IconManager.getInstance().getFontIcon(
                            IconManager.GoogleMaterialDesignIconsFontName, "folder", IconSize.MEDIUM, Color.DARK_GRAY)
                    );
                }
            }

            return retVal;
        }

        private ImageIcon getIcon(Path path) {
            if(Files.isDirectory(path)) {
                return IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "folder", IconSize.MEDIUM, Color.DARK_GRAY);
            } else {
                final String ext = path.getFileName().toString().substring(path.getFileName().toString().lastIndexOf('.')+1);

                // if ext is a media file, use a media icon.  Otherwise use file icon

                if(ext.matches("wav|mp3|aiff|flac|ogg|mp4|mov|avi|wmv|mpg|mpeg|flv|mkv|webm")) {
                    return IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "audiotrack", IconSize.MEDIUM, Color.DARK_GRAY);
                } else {
                    return IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "draft", IconSize.MEDIUM, Color.DARK_GRAY);
                }
            }
        }

    }

    private class FolderScanner extends SwingWorker<Void, Void> {

        private final Path folderPath;

        private final boolean includeAllFiles;

        private final boolean includeHidden;

        private final boolean recursive;

        private final DefaultMutableTreeNode parent;

        public FolderScanner(Path folderPath, boolean includeAllFiles, boolean includeHidden, boolean recursive, DefaultMutableTreeNode parent) {
            this.folderPath = folderPath;
            this.includeAllFiles = includeAllFiles;
            this.includeHidden = includeHidden;
            this.recursive = recursive;
            this.parent = parent;
        }

        @Override
        protected Void doInBackground() throws Exception {
            scanFolder(Path.of(project.getLocation()), folderPath, includeAllFiles, includeHidden, recursive, parent);
            return null;
        }

        @Override
        protected void done() {
            // remove '...' node
            parent.remove(0);
            ((DefaultTreeModel)getModel()).nodeStructureChanged(parent);

            ProjectFilesTree.this.firePropertyChange("scanning", true, false);
        }
    }
}
