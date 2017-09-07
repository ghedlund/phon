/*
 * Phon - An open source tool for research in phonology.
 * Copyright (C) 2005 - 2016, Gregory Hedlund <ghedlund@mun.ca> and Yvan Rose <yrose@mun.ca>
 * Dept of Linguistics, Memorial University <https://phon.ca>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ca.phon.ipamap;

import java.awt.event.*;
import java.util.Map;

import ca.phon.plugin.*;

@PhonPlugin(name="default")
public class IPAMapEP implements IPluginEntryPoint {

	/** The static window instance */
	private static IpaMapFrame _window;
//	private static int numCalls = 0;
	
	public  final static String EP_NAME = "IPAMap";
	
	@Override
	public String getName() {
		return EP_NAME;
	}
	
	private void begin() {
		IpaMapFrame window = _window;
		if(window == null) {
			window = new IpaMapFrame();
			
			window.addWindowListener(new WindowAdapter() {

				@Override
				public void windowClosed(WindowEvent e) {
					_window = null;
				}
				
			});
			_window = window;
		}

		if(_window.isVisible())
			_window.setVisible(false);
		else {
			_window.showWindow();
			_window.toFront();
		}
	}

	@Override
	public void pluginStart(Map<String, Object> initInfo) {	
		begin();
	}

}
