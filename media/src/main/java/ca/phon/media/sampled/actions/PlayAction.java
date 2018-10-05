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
package ca.phon.media.sampled.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import ca.phon.media.sampled.PCMSegmentView;

public class PlayAction extends PCMSegmentViewAction {

	private static final long serialVersionUID = 3592378313027362147L;
	
	public final static String TXT = "Play segment/selection";
	
	public final static KeyStroke KS = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0);
	
	public PlayAction(PCMSegmentView view) {
		super(view);
		
		putValue(NAME, TXT);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		getView().play();
	}

}
