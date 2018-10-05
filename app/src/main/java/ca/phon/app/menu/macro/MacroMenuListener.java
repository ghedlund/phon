/*
 * Copyright (C) 2012-2018 Gregory Hedlund & Yvan Rose
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
package ca.phon.app.menu.macro;

import javax.swing.JMenu;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import ca.phon.app.opgraph.macro.MacroLibrary;
import ca.phon.project.Project;
import ca.phon.ui.CommonModuleFrame;

public class MacroMenuListener implements MenuListener {

	@Override
	public void menuSelected(MenuEvent e) {
		final JMenu menu = (JMenu)e.getSource();
		menu.removeAll();
		
		final CommonModuleFrame currentFrame = CommonModuleFrame.getCurrentFrame();
		if(currentFrame == null) return;
		final Project project = CommonModuleFrame.getCurrentFrame().getExtension(Project.class);
		if(project == null) return;
		
		final MacroLibrary library = new MacroLibrary();
		library.setupMenu(project, menu.getPopupMenu());
	}

	@Override
	public void menuDeselected(MenuEvent e) {
	}

	@Override
	public void menuCanceled(MenuEvent e) {
	}

}
