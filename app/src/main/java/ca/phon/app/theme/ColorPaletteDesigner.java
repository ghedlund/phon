package ca.phon.app.theme;

import ca.phon.ui.CommonModuleFrame;

import javax.swing.*;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.*;
import java.awt.*;
import java.io.IOException;

public class ColorPaletteDesigner extends JPanel {

	private UndoManager undoManager;

	private UndoableEditSupport undoSupport;

    private final static String[] DEFAULT_UI_MANAGER_KEYS = new String[]{
			"Button.background",
			"Button.darkShadow",
			"Button.disabledText",
			"Button.foreground",
			"Button.highlight",
			"Button.light",
			"Button.select",
			"Button.shadow",
			"CheckBox.background",
			"CheckBox.disabledText",
			"CheckBox.foreground",
			"CheckBox.select",
			"CheckBoxMenuItem.acceleratorForeground",
			"CheckBoxMenuItem.acceleratorSelectionForeground",
			"CheckBoxMenuItem.background",
			"CheckBoxMenuItem.disabledBackground",
			"CheckBoxMenuItem.disabledForeground",
			"CheckBoxMenuItem.foreground",
			"CheckBoxMenuItem.selectionBackground",
			"CheckBoxMenuItem.selectionForeground",
			"ColorChooser.background",
			"ColorChooser.foreground",
			"ColorChooser.swatchesDefaultRecentColor",
			"ComboBox.background",
			"ComboBox.buttonBackground",
			"ComboBox.buttonDarkShadow",
			"ComboBox.buttonHighlight",
			"ComboBox.buttonShadow",
			"ComboBox.disabledBackground",
			"ComboBox.disabledForeground",
			"ComboBox.foreground",
			"ComboBox.selectionBackground",
			"ComboBox.selectionForeground",
			"Desktop.background",
			"EditorPane.background",
			"EditorPane.caretForeground",
			"EditorPane.foreground",
			"EditorPane.inactiveBackground",
			"EditorPane.inactiveForeground",
			"EditorPane.selectionBackground",
			"EditorPane.selectionForeground",
			"Focus.color",
			"FormattedTextField.background",
			"FormattedTextField.caretForeground",
			"FormattedTextField.foreground",
			"FormattedTextField.inactiveBackground",
			"FormattedTextField.inactiveForeground",
			"FormattedTextField.selectionBackground",
			"FormattedTextField.selectionForeground",
			"InternalFrame.activeTitleBackground",
			"InternalFrame.activeTitleForeground",
			"InternalFrame.background",
			"InternalFrame.borderColor",
			"InternalFrame.borderDarkShadow",
			"InternalFrame.borderHighlight",
			"InternalFrame.borderLight",
			"InternalFrame.borderShadow",
			"InternalFrame.inactiveTitleBackground",
			"InternalFrame.inactiveTitleForeground",
			"InternalFrame.optionDialogBackground",
			"InternalFrame.paletteBackground",
			"Label.background",
			"Label.disabledForeground",
			"Label.disabledShadow",
			"Label.foreground",
			"List.background",
			"List.foreground",
			"List.selectionBackground",
			"List.selectionForeground",
			"Menu.acceleratorForeground",
			"Menu.acceleratorSelectionForeground",
			"Menu.background",
			"Menu.disabledBackground",
			"Menu.disabledForeground",
			"Menu.foreground",
			"Menu.selectionBackground",
			"Menu.selectionForeground",
			"MenuBar.background",
			"MenuBar.disabledBackground",
			"MenuBar.disabledForeground",
			"MenuBar.foreground",
			"MenuBar.highlight",
			"MenuBar.selectionBackground",
			"MenuBar.selectionForeground",
			"MenuBar.shadow",
			"MenuItem.acceleratorForeground",
			"MenuItem.acceleratorSelectionForeground",
			"MenuItem.background",
			"MenuItem.disabledBackground",
			"MenuItem.disabledForeground",
			"MenuItem.foreground",
			"MenuItem.selectionBackground",
			"MenuItem.selectionForeground",
			"OptionPane.background",
			"OptionPane.foreground",
			"OptionPane.messageForeground",
			"Panel.background",
			"Panel.foreground",
			"PasswordField.background",
			"PasswordField.caretForeground",
			"PasswordField.foreground",
			"PasswordField.inactiveBackground",
			"PasswordField.inactiveForeground",
			"PasswordField.selectionBackground",
			"PasswordField.selectionForeground",
			"PopupMenu.background",
			"PopupMenu.foreground",
			"PopupMenu.selectionBackground",
			"PopupMenu.selectionForeground",
			"ProgressBar.background",
			"ProgressBar.foreground",
			"ProgressBar.selectionBackground",
			"ProgressBar.selectionForeground",
			"RadioButton.background",
			"RadioButton.darkShadow",
			"RadioButton.disabledText",
			"RadioButton.foreground",
			"RadioButton.highlight",
			"RadioButton.light",
			"RadioButton.select",
			"RadioButton.shadow",
			"RadioButtonMenuItem.acceleratorForeground",
			"RadioButtonMenuItem.acceleratorSelectionForeground",
			"RadioButtonMenuItem.background",
			"RadioButtonMenuItem.disabledBackground",
			"RadioButtonMenuItem.disabledForeground",
			"RadioButtonMenuItem.foreground",
			"RadioButtonMenuItem.selectionBackground",
			"RadioButtonMenuItem.selectionForeground",
			"ScrollBar.background",
			"ScrollBar.foreground",
			"ScrollBar.thumb",
			"ScrollBar.thumbDarkShadow",
			"ScrollBar.thumbHighlight",
			"ScrollBar.thumbShadow",
			"ScrollBar.track",
			"ScrollBar.trackHighlight",
			"ScrollPane.background",
			"ScrollPane.foreground",
			"Separator.foreground",
			"Separator.highlight",
			"Separator.shadow",
			"Slider.background",
			"Slider.focus",
			"Slider.foreground",
			"Slider.highlight",
			"Slider.shadow",
			"Slider.tickColor",
			"Spinner.background",
			"Spinner.foreground",
			"SplitPane.background",
			"SplitPane.darkShadow",
			"SplitPane.highlight",
			"SplitPane.shadow",
			"SplitPaneDivider.draggingColor",
			"TabbedPane.background",
			"TabbedPane.darkShadow",
			"TabbedPane.focus",
			"TabbedPane.foreground",
			"TabbedPane.highlight",
			"TabbedPane.light",
			"TabbedPane.shadow",
			"Table.background",
			"Table.focusCellBackground",
			"Table.focusCellForeground",
			"Table.foreground",
			"Table.gridColor",
			"Table.selectionBackground",
			"Table.selectionForeground",
			"TableHeader.background",
			"TableHeader.foreground",
			"TextArea.background",
			"TextArea.caretForeground",
			"TextArea.foreground",
			"TextArea.inactiveBackground",
			"TextArea.inactiveForeground",
			"TextArea.selectionBackground",
			"TextArea.selectionForeground",
			"TextComponent.selectionBackgroundInactive",
			"TextField.background",
			"TextField.caretForeground",
			"TextField.darkShadow",
			"TextField.foreground",
			"TextField.highlight",
			"TextField.inactiveBackground",
			"TextField.inactiveForeground",
			"TextField.light",
			"TextField.selectionBackground",
			"TextField.selectionForeground",
			"TextField.shadow",
			"TextPane.background",
			"TextPane.caretForeground",
			"TextPane.foreground",
			"TextPane.inactiveBackground",
			"TextPane.inactiveForeground",
			"TextPane.selectionBackground",
			"TextPane.selectionForeground",
			"TitledBorder.titleColor",
			"ToggleButton.background",
			"ToggleButton.darkShadow",
			"ToggleButton.disabledText",
			"ToggleButton.foreground",
			"ToggleButton.highlight",
			"ToggleButton.light",
			"ToggleButton.shadow",
			"ToolBar.background",
			"ToolBar.darkShadow",
			"ToolBar.dockingBackground",
			"ToolBar.dockingForeground",
			"ToolBar.floatingBackground",
			"ToolBar.floatingForeground",
			"ToolBar.foreground",
			"ToolBar.highlight",
			"ToolBar.light",
			"ToolBar.shadow",
			"ToolTip.background",
			"ToolTip.foreground",
			"Tree.background",
			"Tree.foreground",
			"Tree.hash",
			"Tree.line",
			"Tree.selectionBackground",
			"Tree.selectionBorderColor",
			"Tree.selectionForeground",
			"Tree.textBackground",
			"Tree.textForeground",
			"Viewport.background",
			"Viewport.foreground",
			"activeCaption",
			"activeCaptionBorder",
			"activeCaptionText",
			"control",
			"controlDkShadow",
			"controlHighlight",
			"controlLtHighlight",
			"controlShadow",
			"controlText",
			"desktop",
			"inactiveCaption",
			"inactiveCaptionBorder",
			"inactiveCaptionText",
			"info",
			"infoText",
			"menu",
			"menuText",
			"scrollbar",
			"text",
			"textHighlight",
			"textHighlightText",
			"textInactiveText",
			"textText",
			"window",
			"windowBorder",
			"windowText"
	};

	private boolean modified;

	public ColorPaletteDesigner() {
		super();

		this.undoManager = new UndoManager();
		this.undoSupport = new UndoableEditSupport();
		final var undoListener = new UndoableEditListener() {

			@Override
			public void undoableEditHappened(UndoableEditEvent e) {
				undoManager.addEdit(e.getEdit());
				setModified(true);
			}

		};
		this.undoSupport.addUndoableEditListener(undoListener);
	}

	public boolean isModified() {
		return modified;
	}

	public void setModified(boolean modified) {
		var oldVal = this.modified;
		this.modified = modified;
		firePropertyChange("modified", oldVal, modified);
	}

	public UndoManager getUndoManager() {
		return undoManager;
	}

	private void repaintWindows() {
		CommonModuleFrame.getOpenWindows().forEach(CommonModuleFrame::repaint);
	}

	/**
	 * Save all ui manager properties to file
	 * @param filename
	 * @throws IOException
	 */
	public void saveProperties(String filename) throws IOException {

	}

	/**
	 * Load all ui manager properties from file
	 *
	 * @param filename
	 * @throws IOException
	 */
	public void loadProperties(String filename) throws IOException {

	}

	/**
	 * Change color of given UIManager key
	 *
	 * @param key
	 * @param color
	 */
	public void changeColor(String key, Color color) {
		final ChangeColorEdit edit = new ChangeColorEdit(key, color);
		undoSupport.postEdit(edit);
	}

	private class ChangeColorEdit extends AbstractUndoableEdit {

		private String key;

		private Color color;

		private Color oldColor;

		public ChangeColorEdit(String key, Color color) {
			this.key = key;
			this.color = color;
			this.oldColor = UIManager.getColor(key);
			UIManager.put(key, color);
		}

		@Override
		public void undo() throws CannotUndoException {
			UIManager.put(key, oldColor);
		}

		@Override
		public boolean canUndo() {
			return true;
		}

		@Override
		public void redo() throws CannotRedoException {
			UIManager.put(key, color);
		}

		@Override
		public boolean canRedo() {
			return true;
		}

		@Override
		public boolean isSignificant() {
			return super.isSignificant();
		}

		@Override
		public String getPresentationName() {
			return String.format("Change color for %s to %s", key, color.toString());
		}
	}

}
