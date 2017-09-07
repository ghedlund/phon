/*
 * Phon - An open source tool for research in phonology.
 * Copyright (C) 2005 - 2017, Gregory Hedlund <ghedlund@mun.ca> and Yvan Rose <yrose@mun.ca>
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
package ca.phon.app.fonts;

import java.awt.*;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.ImageIcon;

import ca.phon.ui.*;
import ca.phon.ui.action.PhonUIAction;
import ca.phon.ui.nativedialogs.*;
import ca.phon.util.PrefHelper;
import ca.phon.util.icons.*;

public class FontSelectionButton extends MultiActionButton {
	
	public final static String FONT_CHANGE_PROP = "_font_changed_";

	private static final long serialVersionUID = -3873581234740139307L;
	
	private AtomicReference<Font> selectedFont = new AtomicReference<Font>();
	
	private String fontProp;
	
	private String defaultVal;
	
	public FontSelectionButton() {
		super();
		
		setSelectedFont(getFont());
		
		init();
	}
	
	private void init() {
		final ImageIcon icon = 
			IconManager.getInstance().getIcon("apps/preferences-desktop-font", IconSize.SMALL);
		final ImageIcon reloadIcon = 
			IconManager.getInstance().getIcon("actions/reload", IconSize.SMALL);
		
		final PhonUIAction defaultAct = new PhonUIAction(this, "onSelectFont");
		defaultAct.putValue(PhonUIAction.NAME, "Select font");
		defaultAct.putValue(PhonUIAction.LARGE_ICON_KEY, icon);
		
		final PhonUIAction reloadAct = new PhonUIAction(this, "onReload");
		reloadAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Reset to default");
		reloadAct.putValue(PhonUIAction.LARGE_ICON_KEY, reloadIcon);
		
		addAction(reloadAct);
		
		setDefaultAction(defaultAct);
		
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}

	public Font getSelectedFont() {
		return selectedFont.get();
	}
	
	public void setSelectedFont(Font font) {
		selectedFont.set(font);
		setBottomLabelText((new FontFormatter()).format(font));
	}
	
	public String getFontProp() {
		return fontProp;
	}

	public void setFontProp(String fontProp) {
		this.fontProp = fontProp;
	}

	public String getDefaultVal() {
		return defaultVal;
	}

	public void setDefaultVal(String defaultVal) {
		this.defaultVal = defaultVal;
	}

	public void onSelectFont() {
		final FontDialogProperties props = new FontDialogProperties();
		props.setRunAsync(true);
		props.setListener(fontDlgListener);
		props.setFontName(getSelectedFont().getName());
		props.setFontSize(getSelectedFont().getSize());
		props.setBold(getSelectedFont().isBold());
		props.setItalic(getSelectedFont().isItalic());
		props.setParentWindow(CommonModuleFrame.getCurrentFrame());
	
		NativeDialogs.showFontDialog(props);
	}
	
	public void onReload() {
		PrefHelper.getUserPreferences()
			.put(getFontProp(), getDefaultVal());
		setBottomLabelText(getDefaultVal());
	}
	
	private final NativeDialogListener fontDlgListener = new NativeDialogListener() {
		
		@Override
		public void nativeDialogEvent(NativeDialogEvent arg0) {
			if(arg0.getDialogData() != null) {
				final Font font = (Font)arg0.getDialogData();
				setSelectedFont(font);
				
				PrefHelper.getUserPreferences()
					.put(getFontProp(), (new FontFormatter()).format(font));
			}
		}
		
	};
	
}
