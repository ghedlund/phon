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
package ca.phon.app.opgraph.editor.actions.debug;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

import ca.gedge.opgraph.Processor;
import ca.gedge.opgraph.app.GraphDocument;
import ca.phon.app.opgraph.editor.OpgraphEditor;
import ca.phon.app.opgraph.editor.actions.OpgraphEditorAction;
import ca.phon.util.icons.IconManager;
import ca.phon.util.icons.IconSize;

public class StepOutOfAction extends OpgraphEditorAction {

	private static final long serialVersionUID = 770626929405993471L;

	public static final String TXT = "Step out";
	
	public static final String DESC = "Step out of subgraph";
	
	public final static KeyStroke KS = KeyStroke.getKeyStroke(KeyEvent.VK_UP,
			Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
	
	public final static ImageIcon ICON = 
			IconManager.getInstance().getIcon("actions/debug-step-out", IconSize.SMALL);
	
	public StepOutOfAction(OpgraphEditor editor) {
		super(editor);
		
		putValue(NAME, TXT);
		putValue(SHORT_DESCRIPTION, DESC);
		putValue(ACCELERATOR_KEY, KS);
		putValue(SMALL_ICON, ICON);
	}

	@Override
	public void hookableActionPerformed(ActionEvent arg0) {
		final GraphDocument document = getEditor().getModel().getDocument();
		if(document != null) {
			Processor context = document.getProcessingContext();
			if(context == null) {
				context = new Processor(document.getGraph());
				document.setProcessingContext(context);
				
				context.getContext().setDebug(true);
				getEditor().getModel().setupContext(context.getContext());
			}

			if(context.hasNext()) {
				context.stepOutOf();
				document.updateDebugState(context);
			}
		}
	}

}
