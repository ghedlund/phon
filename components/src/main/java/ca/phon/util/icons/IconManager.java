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
package ca.phon.util.icons;

import ca.hedlund.desktopicons.*;
import ca.phon.plugin.PluginManager;
import ca.phon.util.*;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;
import java.io.File;
import java.net.URL;
import java.util.HashMap;

/**
 * Class for obtaining icons.
 *
 */
public class IconManager {

	/** The static instance */
	private static IconManager _instance;
	
	/**
	 * Returns the shared IconManager
	 * instance
	 */
	public static IconManager getInstance() {
		if(_instance == null) {
			_instance = new IconManager();
			IconFontSwing.register(FontAwesome.getIconFont());
			GoogleMaterialIconFont.registerFonts();
		}
		return _instance;
	}

	// region data/icons/
	private final String iconDir = System.getProperty("user.dir") + File.separator + "data/icons";
	
	private final String iconURI = "data/icons";

	private record IconTuple(String iconName, IconSize size) {}

	/** Only load icons once */
	private HashMap<IconTuple, ImageIcon> loadedIcons;
	
	/** Constructor */
	protected IconManager() {
		super();
		
		this.loadedIcons = new HashMap<IconTuple, ImageIcon>();
	}
	
	public ImageIcon getIcon(String iconName, IconSize size) {
		ImageIcon retVal = null;
		if(loadIcon(iconName, size)) {
			IconTuple tuple = new IconTuple(iconName, size);
			retVal = loadedIcons.get(tuple);
		}
		return retVal;
	}

	public ImageIcon getGrayedIcon(String iconName, IconSize size) {
		final ImageIcon icon = getIcon(iconName, size);
		if(icon == null) return null;
		
		return getGrayedIcon(icon);
	}

	public ImageIcon getDisabledIcon(String iconName, IconSize size) {
		final ImageIcon icon = getIcon(iconName, size);
		if(icon == null) return null;

		return getDisabledIcon(icon);
	}

	/**
	 * Load an icon from our classpath
	 * @param iconName
	 * @param s
	 * @return <CODE>true</CODE> if the icon is loaded
	 */
	private boolean loadIconFromCp(String iconName, IconSize s) {
		boolean found = false;
		URL iconURL = null;

		for(int i = s.ordinal(); i < IconSize.values().length; i++) {
			if(found) break;
			IconSize size = IconSize.values()[i];

			String icnURL =
					iconURI + "/" + size.getWidth() + "x" + size.getHeight() + "/" + iconName + ".png";
			iconURL = PluginManager.getInstance().getResource(icnURL);
			if(iconURL == null) {
				icnURL =
						iconURI + "/" + size.getWidth() + "x" + size.getHeight() + "/" + iconName + ".gif";
				iconURL = PluginManager.getInstance().getResource(icnURL);

				if(iconURL != null) {
					found = true;
				}
			} else {
				found = true;
			}
		}

		if(found) {
			ImageIcon iconImage = null;
			iconImage = new ImageIcon(iconURL);

			iconImage = new ImageIcon(
					iconImage.getImage().getScaledInstance(s.getWidth(), s.getHeight(), Image.SCALE_SMOOTH));
			IconTuple tuple = new IconTuple(iconName, s);
			loadedIcons.put(tuple, iconImage);
		}

		return found;
	}

	/**
	 * Loads (ensures) an icon.
	 *
	 * @param iconName
	 * @param s
	 * @return <CODE>true</CODE> if the icon is loaded (including
	 * if it was already loaded.) <CODE>false</CODE> otherwise.
	 */
	private boolean loadIcon(String iconName, IconSize s) {
		boolean found = loadIconFromCp(iconName, s);
		String iconPath = "";

		if(!found) {

			iconPath =
					iconDir + "/" + s.getWidth() + "x" + s.getHeight() + "/" + iconName + ".png";
			if(new File(iconPath).exists()) {
				found = true;
			} else {
				for(int i = IconSize.XXLARGE.ordinal(); i >= 0; i--) {
					if(found) break;
					IconSize size = IconSize.values()[i];

					iconPath =
							iconDir + "/" + size.getWidth() + "x" + size.getHeight() + "/" + iconName + ".png";
					if(!(new File(iconPath)).exists()) {
						iconPath =
								iconDir + "/" + size.getWidth() + "x" + size.getHeight() + "/" + iconName + ".gif";

						if(new File(iconPath).exists()) {
							found = true;
						}
					} else {
						found = true;
					}
				}
			}

			if(found) {
				ImageIcon iconImage = null;
				iconImage = new ImageIcon(iconPath);

				iconImage = new ImageIcon(
						iconImage.getImage().getScaledInstance(s.getWidth(), s.getHeight(), Image.SCALE_SMOOTH));

				IconTuple tuple = new IconTuple(iconName, s);
				loadedIcons.put(tuple, iconImage);
			}
		}

		return found;
	}
	// endregion

	// region Icon utils
	/**
	 * Create a new icon with a single character
	 * using the given foreground, background.  The
	 * size of the icon will be the size of the
	 * bounding rectangle returned by {@link FontMetrics#getStringBounds(String, java.awt.Graphics)}
	 *
	 * @param c
	 * @param font
	 * @param foreground
	 * @param background
	 */
	public ImageIcon createGlyphIcon(Character c, Font font, Color foreground, Color background) {
		final BufferedImage bufferedImage = new BufferedImage(1, 1,
				BufferedImage.TYPE_4BYTE_ABGR);
		final Graphics2D g = (Graphics2D)bufferedImage.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		g.setFont(font);
		final FontMetrics fm = g.getFontMetrics(font);
		final Rectangle2D charRect = fm.getStringBounds(c.toString(), g);

		final BufferedImage img = new BufferedImage((int)charRect.getWidth()+1, (int)charRect.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
		final Graphics2D g2 = (Graphics2D)img.createGraphics();

		// get baseline of character
		g2.setColor(foreground);
		g2.drawString(c.toString(), 0, (int)(charRect.getHeight() - fm.getDescent()));
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		return new ImageIcon(img);
	}

	/**
	 * Combine icons into an icon strip.
	 *
	 * @param icons
	 */
	public ImageIcon createIconStrip(Icon[] icons) {
		return createIconStrip(icons, 0);
	}

	public ImageIcon createIconStrip(Icon[] icons, int offset) {
		// determine size
		int width = 0;
		int height = 0;

		for(Icon icon:icons) {
			width += icon.getIconWidth();
			height = Math.max(height, icon.getIconHeight());
		}
		width += (icons.length - 1) * offset;
		final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
		final Graphics2D g = (Graphics2D)img.createGraphics();

		int x = 0;
		int y = 0;
		for(Icon icon:icons) {
			icon.paintIcon(null, g, x, y);
//			g.drawImage(icon.getImage(), x, y, null);
			x += icon.getIconWidth() + offset;
		}

		return new ImageIcon(img);
	}

	public ImageIcon getGrayedIcon(Icon icon) {
		final int w = icon.getIconWidth();
		final int h = icon.getIconHeight();
		
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice gd = ge.getDefaultScreenDevice();
		GraphicsConfiguration gc = gd.getDefaultConfiguration();
		BufferedImage img = gc.createCompatibleImage(w, h, Transparency.TRANSLUCENT);
		Graphics2D g2d = img.createGraphics();
		g2d.setColor(Color.decode("#00f0f0f0"));
		g2d.fillRect(0, 0, w, h);
		icon.paintIcon(null, g2d, 0, 0);
		
		BufferedImage gray = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
		ColorConvertOp op = new ColorConvertOp(
				img.getColorModel().getColorSpace(),
				gray.getColorModel().getColorSpace(), null);
		op.filter(img, gray);
		
		return new ImageIcon(gray);
	}

	public ImageIcon getDisabledIcon(Icon icon) {
		final int w = icon.getIconWidth();
		final int h = icon.getIconHeight();

		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice gd = ge.getDefaultScreenDevice();
		GraphicsConfiguration gc = gd.getDefaultConfiguration();
		BufferedImage img = gc.createCompatibleImage(w, h, Transparency.TRANSLUCENT);
		Graphics2D g2d = img.createGraphics();
		icon.paintIcon(null, g2d, 0, 0);
		Image gray = GrayFilter.createDisabledImage(img);

		return new ImageIcon(gray);
	}

	/**
	 * Create a new ImageIcon from an Icon.
	 *
	 * @param icon
	 * @return icon image
	 */
	private ImageIcon iconToImage(IconSize size, Icon icon) {
		BufferedImage img = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g2 = (Graphics2D)img.createGraphics();
		icon.paintIcon(null, g2, 0, 0);

		// create a new buffered image of size and draw icon centered and scaled keeping perspective
		final BufferedImage scaledImg = new BufferedImage(size.getWidth(), size.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D scaledImgGraphics = (Graphics2D)scaledImg.createGraphics();
		scaledImgGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		final double scale = Math.min((double)size.getWidth() / (double)icon.getIconWidth(), (double)size.getHeight() / (double)icon.getIconHeight());
		final int w = (int)(icon.getIconWidth() * scale);
		final int h = (int)(icon.getIconHeight() * scale);
		scaledImgGraphics.drawImage(img, (size.getWidth() - w) / 2, (size.getHeight() - h) / 2, w, h, null);



		return new ImageIcon(scaledImg);
	}
	// endregion

	// region System icons
	/**
	 * Load system icon for given path.
	 * 
	 * @param path
	 * @param size
	 * @return icon for given path or <code>null</code> if not found
	 */
	public ImageIcon getSystemIconForPath(String path, IconSize size) {
		return getSystemIconForPath(path, "mimetypes/text-x-generic", size);
	}
	
	/**
	 * Load system icon for given path.
	 * 
	 * @param path
	 * @param backupIcon icon name used as backup if system icon not found. May be <code>null</code>
	 * @param size

	 * @return icon for given path or backup icon if not found
	 */
	public ImageIcon getSystemIconForPath(String path, String backupIcon, IconSize size) {
		ImageIcon retVal = null;
		
		if(OSInfo.isNix()) {
			retVal = getIcon(backupIcon, size);
		} else {
			try {
				final Image img = DesktopIcons.getIconForPath(path, size.getWidth(), size.getHeight());
				retVal = new ImageIcon(img);
			} catch (DesktopIconException e) {
				if(backupIcon != null && backupIcon.length() > 0) {
					retVal = getIcon(backupIcon, size);
				}
			}
		}
		
		return retVal;
	}
	
	/**
	 * Load system icon for given file type.  File type should be an extension
	 * without a leading '.'
	 * 
	 * @param filetype
	 * @param size
	 * 
	 * @return icon for given file type or <code>null</code> if not found
	 */
	public ImageIcon getSystemIconForFileType(String filetype, IconSize size) {
		return getSystemIconForFileType(filetype, "mimetypes/text-x-generic", size);
	}
	
	/**
	 * Load system icon for given file type.  File type should be an extension
	 * without a leading '.'
	 * 
	 * @param filetype
	 * @param size
	 * @param backupIcon icon name used as backup if system icon not found, may be <code>null</code>
	 * 
	 * @return icon for given file type or backup icon if not found
	 */
	public ImageIcon getSystemIconForFileType(String filetype, String backupIcon, IconSize size) {
		ImageIcon retVal = null;
		
		if(OSInfo.isNix()) {
			retVal = getIcon(backupIcon, size);
		} else {
			try {
				final Image img = DesktopIcons.getIconForFileType(filetype, size.getWidth(), size.getHeight());
				retVal = new ImageIcon(img);
			} catch (DesktopIconException e) {
				if(backupIcon != null && backupIcon.length() > 0) {
					retVal = getIcon(backupIcon, size);
				}
			}
		}
		
		return retVal;
	}
	
	/**
	 * Load system stock icon.  Use a value from either MacOSStockIcon or
	 * WindowsStockIcon enums.
	 * 
	 * @param stockIcon
	 * @param size
	 * 
	 * @return icon for given file type or backup icon if not found
	 */
	public ImageIcon getSystemStockIcon(StockIcon stockIcon, IconSize size) {
		return getSystemStockIcon(stockIcon, "blank", size);
	}
	
	/**
	 * Load system stock icon.  Use a value from either MacOSStockIcon or
	 * WindowsStockIcon enums.
	 * 
	 * @param stockIcon
	 * @param size
	 * @param backupIcon icon name used as backup if system icon not found
	 * 
	 * @return icon for given file type or backup icon if not found
	 */
	public ImageIcon getSystemStockIcon(StockIcon stockIcon, String backupIcon, IconSize size) {
		ImageIcon retVal = null;
		
		// Phon 3.1 - return default icons on linux
		if(OSInfo.isNix()) {
			retVal = getIcon(backupIcon, size);
		} else {
			try {
				final Image img = DesktopIcons.getStockIcon(stockIcon, size.getWidth(), size.getHeight());
				retVal = new ImageIcon(img);
			} catch (DesktopIconException e) {
				if(backupIcon != null && backupIcon.length() > 0) {
					retVal = getIcon(backupIcon, size);
				}
			}
		}
		
		return retVal;
	}
	// endregion

	// region Font icons
	public final static String FontAwesomeFontName = "FontAwesome";
	public final static String GoogleMaterialDesignIconsFontName = "MaterialIconsRounded";

	private record FontIconInfo(String fontName, String iconName, IconSize size, Color color) {}

	private HashMap<FontIconInfo, ImageIcon> fontIconCache = new HashMap<FontIconInfo, ImageIcon>();

	/**
	 * Get the specified icon with the given size and color
	 *
	 * @param fontName
	 * @param iconName
	 * @param size
	 * @param color
	 * @return icon
	 */
	public ImageIcon getFontIcon(String fontName, String iconName, IconSize size, Color color) {
		final FontIconInfo info = new FontIconInfo(fontName, iconName, size, color);
		if(fontIconCache.containsKey(info)) {
			return fontIconCache.get(info);
		} else {
			final ImageIcon icon = buildFontIcon(fontName, iconName, size, color);
			fontIconCache.put(info, icon);
			return icon;
		}
	}

	/**
	 * Create a new icon from the jIconFont library using the given font name,
	 * icon name, size, and colour.
	 *
	 * @param fontName
	 * @param iconName
	 * @param size
	 * @param color
	 * @return
	 */
	public ImageIcon buildFontIcon(String fontName, String iconName, IconSize size, Color color) {
		Icon icon = null;
		if(FontAwesomeFontName.equals(fontName)) {
			icon = IconFontSwing.buildIcon(FontAwesome.valueOf(iconName), size.getWidth(), color);
		} else {
			final GoogleMaterialFonts googleMaterialStaticFont = GoogleMaterialFonts.fromString(fontName);
			if(googleMaterialStaticFont != null) {
				GoogleMaterialIconFont iconFont = GoogleMaterialIconFont.getIconFont(googleMaterialStaticFont);
				try {
					icon = iconFont.buildIcon(iconName, (float) size.getWidth() * 2, color);
				} catch (Exception e) {
					icon = null;
				}
			}
		}
		if(icon == null) {
			return getIcon("blank", size);
		} else {
			return iconToImage(size, icon);
		}
	}

	/**
	 * Get the specified icon with the given size and color
	 *
	 * @param name in the format of 'font:icon'.  If 'font' is left out GoogleMaterialDesignIconsFontName is used.
	 * @param size
	 * @param color
	 * @return icon
	 */
	public ImageIcon getFontIcon(String name, IconSize size, Color color) {
		final String[] iconData = name.split(":");
		if(iconData.length == 1) {
			return getFontIcon(GoogleMaterialDesignIconsFontName, iconData[0], size, color);
		} else {
			return getFontIcon(iconData[0], iconData[1], size, color);
		}
	}
	// endregion
}
