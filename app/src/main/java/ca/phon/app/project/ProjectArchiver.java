package ca.phon.app.project;

import ca.phon.project.Project;

/**
 * Task for archiving a project.
 * Project files are filtered and then zipped.
 */
public class ProjectArchiver {

    private final Project project;

    private final String archivePath;

    public ProjectArchiver(Project project, String archivePath) {
        super();
        this.project = project;
        this.archivePath = archivePath;
    }

    public void archive() {
        // create zip of filtered project files
    }

}
