package ca.phon.app.project;

import ca.phon.app.actions.OpenFileHandler;
import ca.phon.app.log.LogUtil;
import ca.phon.app.modules.EntryPointArgs;
import ca.phon.plugin.IPluginExtensionFactory;
import ca.phon.plugin.IPluginExtensionPoint;
import ca.phon.plugin.PluginEntryPointRunner;
import ca.phon.plugin.PluginException;
import ca.phon.project.LocalProject;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * File handler for opening phon project files (4.x and later)
 */
public class ProjectOpenFileHandler implements OpenFileHandler, IPluginExtensionPoint<OpenFileHandler> {

    @Override
    public Set<String> supportedExtensions() {
        return Set.of(LocalProject.PROJECT_FILE_EXT.substring(1));
    }

    @Override
    public boolean canOpen(File file) throws IOException {
        // only check extension
        final String ext = FilenameUtils.getExtension(file.getName());
        return supportedExtensions().contains(ext);
    }

    @Override
    public void openFile(File file, Map<String, Object> args) throws IOException {
        // open the project folder which contains the project file
        final File projectFolder = file.getParentFile();

        // run open project plugin
        final EntryPointArgs epArgs = new EntryPointArgs();
        epArgs.put(EntryPointArgs.PROJECT_LOCATION, projectFolder.getAbsolutePath());
        try {
            PluginEntryPointRunner.executePlugin(OpenProjectEP.EP_NAME, epArgs);
        } catch (PluginException e) {
            LogUtil.severe(e);
        }
    }

    @Override
    public Class<?> getExtensionType() {
        return OpenFileHandler.class;
    }

    @Override
    public IPluginExtensionFactory<OpenFileHandler> getFactory() {
        return (args) -> this;
    }
}
