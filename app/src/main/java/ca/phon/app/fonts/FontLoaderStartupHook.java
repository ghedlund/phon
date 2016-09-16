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
package ca.phon.app.fonts;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import ca.phon.app.hooks.PhonStartupHook;
import ca.phon.plugin.IPluginExtensionFactory;
import ca.phon.plugin.IPluginExtensionPoint;
import ca.phon.plugin.PluginException;
import ca.phon.ui.nativedialogs.MessageDialogProperties;
import ca.phon.ui.nativedialogs.NativeDialogs;
import ca.phon.util.OpenFileLauncher;

public class FontLoaderStartupHook implements PhonStartupHook, IPluginExtensionPoint<PhonStartupHook> {
	
	private static final Logger LOGGER = Logger
			.getLogger(FontLoaderStartupHook.class.getName());
	
	private final static String FONT_LIST = "data/fonts/fonts.list";
	
	private final static String FONT_INFO_PAGE = "https://www.phon.ca/phontrac/wiki/CompleteInstall";

	@Override
	public void startup() throws PluginException {
		checkFonts();
//		loadFonts();
	}
	
	private void checkFonts() {
		final String[] fontNames = new String[] {
			"Liberation Sans", "Liberation Mono", "Charis SIL", "Charis SIL Compact"	
		};
		final Set<String> fontSet = new HashSet<String>();
		fontSet.addAll(Arrays.asList(fontNames));
		final String[] allFonts = 
				GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		for(String systemFont:allFonts) {
			if(fontSet.contains(systemFont)) {
				fontSet.remove(systemFont);
			}
		}
		
		if(fontSet.size() > 0) {
			String message = "The following fonts were not found on your computer:";
			for(String fontName:fontSet) {
				message += " " + fontName;
			}
			message += ". For best results, please install these fonts.";
			final MessageDialogProperties props = new MessageDialogProperties();
			props.setMessage(message);
			props.setTitle("Unable to find required fonts");
			props.setHeader("Unable to find required fonts");
			props.setOptions(new String[]{"More Information", "Ok"});
			props.setRunAsync(false);
			
			int retVal = NativeDialogs.showMessageDialog(props);
			if(retVal == 0) {
				// show information page
				try {
					OpenFileLauncher.openURL(new URL(FONT_INFO_PAGE));
				} catch (MalformedURLException e) {
					LOGGER
							.log(Level.SEVERE, e.getLocalizedMessage(), e);
				}
			}
		}
	}
	
	private void loadFonts() throws PluginException {
		final InputStream fontListStream = getClass().getClassLoader().getResourceAsStream(FONT_LIST);
		if(fontListStream != null) {
			final GraphicsEnvironment ge = 
					GraphicsEnvironment.getLocalGraphicsEnvironment();
			final BufferedReader in = new BufferedReader(new InputStreamReader(fontListStream));
			String line = null;
			try {
				while((line = in.readLine()) != null) {
					LOGGER.info("Loading font " + line);
					final InputStream fontInputStream = getClass().getClassLoader().getResourceAsStream(line);
					if(fontInputStream != null) {
						try {
							if(!ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, fontInputStream))) {
								LOGGER.info(line + " provided by system");
							}
							fontInputStream.close();
						} catch (FontFormatException e) {
							LOGGER.log(Level.SEVERE,
									e.getLocalizedMessage(), e);
						}
					} else {
						LOGGER.warning("Font not found: " + line);
					}
				}
				fontListStream.close();
			} catch (IOException e) {
				throw new PluginException(e);
			}
		} else {
			throw new PluginException(new FileNotFoundException(FONT_LIST));
		}
	}

	@Override
	public Class<?> getExtensionType() {
		return PhonStartupHook.class;
	}

	@Override
	public IPluginExtensionFactory<PhonStartupHook> getFactory() {
		return factory;
	}
	
	private final IPluginExtensionFactory<PhonStartupHook> factory = new IPluginExtensionFactory<PhonStartupHook>() {
		
		@Override
		public PhonStartupHook createObject(Object... args) {
			return FontLoaderStartupHook.this;
		}
	};

}
