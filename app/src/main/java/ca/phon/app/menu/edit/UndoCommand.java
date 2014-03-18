package ca.phon.app.menu.edit;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.swing.undo.UndoManager;

/**
 * Undo command template.  Requires a {@link UndoManager} be provided.
 *
 */
public class UndoCommand extends AbstractAction {

	private static final long serialVersionUID = 9100941568698565601L;

	private UndoManager undoManager;
	
	private final static String PREFIX = "Undo";
	
	public UndoCommand(UndoManager manager) {
		super();
		this.undoManager = manager;
		putValue(NAME, PREFIX + manager.getPresentationName());
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Z,
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if(undoManager.canUndo())
			undoManager.undo();
	}
	
}
