package ca.phon.app.session.check;

import java.awt.*;
import java.util.*;

import ca.phon.app.modules.*;
import ca.phon.plugin.*;

@PhonPlugin(name="Session Check")
public class SessionCheckEP implements IPluginEntryPoint {

	public final static String EP_NAME = "Session Check";
	
	@Override
	public String getName() {
		return EP_NAME;
	}

	@Override
	public void pluginStart(Map<String, Object> args) {
		final EntryPointArgs epArgs = new EntryPointArgs(args);
		var project = epArgs.getProject();
		
		if(project == null) return;
		
		final SessionCheckWizard wizard = SessionCheckWizard.newWizard(project);
		wizard.pack();
		wizard.setSize(new Dimension(1024, 768));
		wizard.centerWindow();
		wizard.setVisible(true);
	}

}
