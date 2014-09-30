package ca.phon.app.session.editor.view.session_information;

import java.io.File;

import ca.phon.media.util.MediaLocator;
import ca.phon.project.Project;
import ca.phon.ui.nativedialogs.FileFilter;
import ca.phon.ui.text.FileSelectionField;
import ca.phon.ui.text.PromptedTextField.FieldState;

public class MediaSelectionField extends FileSelectionField {
	
	private static final long serialVersionUID = 5171333221664140205L;
	
	private Project project;
	
	public MediaSelectionField() {
		super();
		setFileFilter(FileFilter.mediaFilter);
	}

	public MediaSelectionField(Project project) {
		super();
		this.project = project;
		setFileFilter(FileFilter.mediaFilter);
		textField.setPrompt("Session media location");
	}

	public void setProject(Project project) {
		this.project = project;
	}
	
	public Project getProject() {
		return this.project;
	}
	
	@Override
	public void setFile(File f) {
		if(f == null) {
			textField.setText("");
		} else {
			textField.setState(FieldState.INPUT);
			
			String txt = f.getPath();
			final File parentFolder = f.getParentFile();
			if(parentFolder != null) {
				for(String includePath:MediaLocator.getMediaIncludePaths(project)) {
					if(includePath.equals(parentFolder.getAbsolutePath())) {
						txt = f.getName();
						break;
					}
				}
			}
			textField.setText(txt);
		}
		super.firePropertyChange(FILE_PROP, lastSelectedFile, f);
		lastSelectedFile = f;
	}
	
	@Override
	public File getSelectedFile() {
		final String txt = super.getText();
		File retVal = null;
		
//		final PathExpander pe = new PathExpander();
		
		if(getTextField().getState() == FieldState.INPUT && txt.length() > 0) {
			File mediaLocatorFile = MediaLocator.findMediaFile(txt, project, null);
			if(mediaLocatorFile != null) {
				retVal = mediaLocatorFile;
			} else {
				retVal = new File(txt);
//						new File(pe.expandPath(txt));
			}
		}
		
		return retVal;
	}
	
}
