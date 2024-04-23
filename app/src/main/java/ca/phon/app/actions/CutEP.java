/*
 * Copyright (C) 2005-2020 Gregory Hedlund & Yvan Rose
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
package ca.phon.app.actions;

import ca.phon.app.log.LogUtil;
import ca.phon.plugin.*;

import javax.swing.text.JTextComponent;
import java.awt.*;
import java.lang.reflect.*;
import java.util.Map;


@PhonPlugin(name="default")
public class CutEP implements IPluginEntryPoint {

	private final static String EP_NAME = "Cut";
	@Override
	public String getName() {
		return EP_NAME;
	}
	
	private void begin() {
		Component keyboardComp = 
			KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
		if(keyboardComp == null) return;
		
		if(keyboardComp instanceof JTextComponent) {
			JTextComponent textComp = (JTextComponent)keyboardComp;
			textComp.cut();
		} else {
			// if it was not a text component, see if we have the cut
			// method available
			Method cutMethod = null;
			try {
				cutMethod = keyboardComp.getClass().getMethod("cut", new Class[0]);
			} catch (SecurityException | NoSuchMethodException ex) {
				LogUtil.warning(ex);
			}
			
			if(cutMethod != null) {
				try {
					cutMethod.invoke(keyboardComp, new Object[0]);
				} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException ex) {
					LogUtil.warning(ex);
				}
			}
		}
	}

	@Override
	public void pluginStart(Map<String, Object> initInfo) {
		begin();
	}
}
