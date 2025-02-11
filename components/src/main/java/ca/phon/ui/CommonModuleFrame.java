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
package ca.phon.ui;


import ca.phon.extensions.*;
import ca.phon.plugin.*;
import ca.phon.project.Project;
import ca.phon.ui.menu.MenuManager;
import ca.phon.ui.nativedialogs.*;
import ca.phon.util.OSInfo;
import ca.phon.util.*;
import ca.phon.util.icons.*;
import ca.phon.worker.PhonWorker;
import com.jgoodies.forms.layout.*;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * The common class for a gui window
 */
public class CommonModuleFrame extends JFrame implements IExtendable {

	/**
	 * Property to enable fullscreen on macos
	 */
	public static final String MACOS_ENABLE_FULLSCREEN = "macos.enableFullscreen";

	public static final boolean DEFAULT_MACOS_ENABLE_FULLSCREEN = true;

	/** The list of open module frames */
	private static List<CommonModuleFrame> openFrames = Collections.synchronizedList(new ArrayList<>());
	
	private static final WeakHashMap<Object, CommonModuleFrameCreatedListener> newWindowListeners = new WeakHashMap<>();

	/**
	 * Add a new window create listener to the list of static listeners.
	 * The weak key is used to determine when the listener should be removed from the
	 * listener queue.
	 * 
	 * @param weakKey
	 * @param listener
	 */
	public static void addNewWindowListener(Object weakKey, CommonModuleFrameCreatedListener listener) {
		synchronized(newWindowListeners) {
			newWindowListeners.put(weakKey, listener);
		}
	}
	private static void newWindowCreated(CommonModuleFrame f) {
		openFrames.add(f);
		synchronized (newWindowListeners) {
			for(var weakKey:newWindowListeners.keySet()) {
				var listener = newWindowListeners.get(weakKey);
				listener.newWindow(f);
			}
		}
	}

	public static List<CommonModuleFrame> getOpenWindows() {
		return openFrames;
	}
	
	/**
	 * Return a map of projects and open windows for
	 * each project.
	 * 
	 * @return project window map
	 */
	public static Map<Project, List<CommonModuleFrame>> getProjectWindows() {
		final Map<Project, List<CommonModuleFrame>> projectWindows = 
				new LinkedHashMap<>();
		final List<CommonModuleFrame> strayWindows = new ArrayList<>();
		for(CommonModuleFrame cmf:CommonModuleFrame.getOpenWindows()) {
			final Project project = cmf.getExtension(Project.class);
			if(project != null) {
				List<CommonModuleFrame> windows = projectWindows.get(project);
				if(windows == null) {
					windows = new ArrayList<>();
					projectWindows.put(project, windows);
				}
				windows.add(cmf);
			}
		}
		return projectWindows;
	}

	private static void removeWindow(CommonModuleFrame f) {
		if(openFrames.contains(f))
			openFrames.remove(f);
	}

	public static CommonModuleFrame getCurrentFrame() {
		CommonModuleFrame[] openWindows =
			CommonModuleFrame.getOpenWindows().toArray(new CommonModuleFrame[0]);
		for(CommonModuleFrame f:openWindows)
			if(f.isActive())
				return f;
		return null;
	}

	protected String title;

	private CommonModuleFrame parentFrame;

	private boolean showInWindowMenu = true;

	/**
	 * Window name
	 */
	private String windowName = null;

	private volatile transient boolean modified = false;

	/**
	 * Messages for unsaved data
	 */
	private String unsavedChangesTitle = "Save data?";

	private String unsavedChangesMessage = "Save changes before close?";

	/**
	 * Creates a new CommonModuleFrame
	 *
	 */
	public CommonModuleFrame() {
		this(" -- untitled -- ");
	}

	/**
	 * Creates a new CommonModuleFrame
	 *
	 * @param title the title string for the frame
	 *
	 */
	public CommonModuleFrame(String title) {
		super(title);
		this.windowName = title;
		
		super.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		// assign the title
		this.title = title;

		// make the frame resizable
		this.setResizable(true);

		// defer menu creation until after window construction
		// some menu handlers may require information not created
		// until sub-class construction is complete
		SwingUtilities.invokeLater(() -> {
			final JMenuBar menuBar = MenuManager.createWindowMenuBar(this);
			setJMenuBar(menuBar);
		});

		this.addWindowListener(new WindowListener() {

			@Override
			public void windowActivated(WindowEvent arg0) {
			}

			@Override
			public void windowClosed(WindowEvent arg0) {
				removeWindowFromActiveList();
				CommonModuleFrame.this.removeWindowListener(this);
				
				if(CommonModuleFrame.getOpenWindows().size() == 0) {
					final String nextEntryPt = arg0.getWindow().getClass().getName().endsWith("WelcomeWindow") ? "Exit" : "WelcomeWindow";
					// exit application
					try {
						PluginEntryPointRunner.executePlugin(nextEntryPt);
					} catch (PluginException e) {
						Logger.getLogger(getClass().getName()).log(Level.WARNING, e.getLocalizedMessage(), e);
						System.exit(1);
					}
				}
			}

			@Override
			public void windowClosing(WindowEvent arg0) {
				((CommonModuleFrame)arg0.getWindow()).close();
			}

			@Override
			public void windowDeactivated(WindowEvent arg0) {
			}

			@Override
			public void windowDeiconified(WindowEvent arg0) {
			}

			@Override
			public void windowIconified(WindowEvent arg0) {
			}

			@Override
			public void windowOpened(WindowEvent arg0) {
				newWindowCreated(CommonModuleFrame.this);
			}

		});

		if(!OSInfo.isMacOs()) {
			// set window icon
			ImageIcon icon =
				IconManager.getInstance().getIcon("apps/database-phon", IconSize.SMALL);
			ImageIcon largeIcon =
				IconManager.getInstance().getIcon("apps/database-phon", IconSize.LARGE);
			ImageIcon xLargIcon =
				IconManager.getInstance().getIcon("apps/database-phon", IconSize.XLARGE);
			final Image icons[] = new Image[]{ icon.getImage(), largeIcon.getImage(), xLargIcon.getImage() };
			if(icon != null) {
				super.setIconImages(Arrays.asList(icons));
			}
		} else {
			// fullscreen support
			getRootPane().putClientProperty("apple.awt.fullscreenable",
					PrefHelper.getBoolean(MACOS_ENABLE_FULLSCREEN, DEFAULT_MACOS_ENABLE_FULLSCREEN));
		}

	}

	public String getUnsavedChangesTitle() {
		return this.unsavedChangesTitle;
	}

	public void setUnsavedChangesTitle(String title) {
		this.unsavedChangesTitle = title;
	}

	public String getUnsavedChangesMessage() {
		return this.unsavedChangesMessage;
	}

	public void setUnsavedChangesMessage(String message) {
		this.unsavedChangesMessage = message;
	}

	public void close() {
		if(hasUnsavedChanges()) {
			final MessageDialogProperties props = new MessageDialogProperties();
			props.setParentWindow(this);
			props.setOptions(MessageDialogProperties.yesNoCancelOptions);
			props.setTitle(getTitle());
			props.setHeader(getUnsavedChangesTitle());
			props.setMessage(getUnsavedChangesMessage());
			props.setRunAsync(true);
			props.setListener( (e) -> {
				int retVal = e.getDialogResult();
				if(retVal == 0) {
					PhonWorker.getInstance().invokeLater( this::saveAndClose );
				} else if(retVal == 1) {
					SwingUtilities.invokeLater( () -> dispose() );
				}
			});
			NativeDialogs.showMessageDialog(props);
		} else {
			dispose();
		}
	}

	/**
	 * Overridden to provide consistent
	 * naming in all windows.  Use setWindowName()
	 * to setup the custom name for the window.
	 */
	@Override
	public String getTitle() {
		String retVal = "";
		if(getWindowName() != null) {
			retVal += (retVal.length() > 0 ? " : " : "") + getWindowName();
		}
		return retVal;
	}

	/**
	 * Set window name
	 */
	public void setWindowName(String name) {
		this.windowName = name;
		setTitle(getTitle());
	}

	/**
	 * Get window name
	 */
	public String getWindowName() {
		return this.windowName;
	}

	private void removeWindowFromActiveList() {
		CommonModuleFrame.removeWindow(this);
	}

	public void display() {
		this.pack();
		this.setVisible(true);
	}

	public CommonModuleFrame getParentFrame() {
		return parentFrame;
	}

	private WindowListener parentFrameListener = null;
	public void setParentFrame(CommonModuleFrame frame) {
		if(this.parentFrame != null) {
			this.parentFrame.removeWindowListener(parentFrameListener);
		}
		this.parentFrame = frame;

		if(parentFrameListener == null) {
			parentFrameListener = new WindowListener() {

				@Override
				public void windowActivated(WindowEvent e) {

				}

				@Override
				public void windowClosed(WindowEvent e) {
					if(e.getWindow() == parentFrame && isVisible()) {
						close();
						parentFrame.removeWindowListener(parentFrameListener);
					}
				}

				@Override
				public void windowClosing(WindowEvent e) {

				}

				@Override
				public void windowDeactivated(WindowEvent e) {

				}

				@Override
				public void windowDeiconified(WindowEvent e) {

				}

				@Override
				public void windowIconified(WindowEvent e) {

				}

				@Override
				public void windowOpened(WindowEvent e) {

				}

			};
		}
		this.parentFrame.addWindowListener(parentFrameListener);

//		updateWindowMenus();
	}

	public boolean isShowInWindowMenu() {
		return showInWindowMenu;
	}

	public void setShowInWindowMenu(boolean showInWindowMenu) {
		this.showInWindowMenu = showInWindowMenu;

//		updateWindowMenus();
	}

	/**
	 * Center the window in the middle of the
	 * display.  If size is not defined preferred size
	 * is used.  If the window has already been
	 * realized, it will not be resized.
	 */
	public void centerWindow() {
		Dimension ss =
			Toolkit.getDefaultToolkit().getScreenSize();

		Dimension size = getSize();
		if(size.width == 0 && size.height == 0)
			size = getPreferredSize();

		int xPos = ss.width / 2 - (size.width/2);
		int yPos = ss.height / 2 - (size.height/2);

		setBounds(xPos, yPos, size.width, size.height);
	}

	public void cascadeWindow(JFrame frame) {
		int x = 50;
		int y = 50;

		if(frame != null) {
			final int titleHeight = frame.getInsets().top;
			final int borderWidth = frame.getInsets().left;
			final int x2 = frame.getX();
			final int y2 = frame.getY();
			y = y2 + titleHeight;
			x = x2 + borderWidth;
		}

		Dimension size = getSize();
		if(size.width == 0 && size.height == 0)
			size = getPreferredSize();

		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		if(y + size.height > screenSize.height) {
			y = screenSize.height - size.height;
		}
		if(x + size.width > screenSize.width) {
			x = screenSize.width - size.width;
		}

		setBounds(x, y, size.width, size.height);
	}

	/**
	 * Places window in top-right corner of screen.
	 *
	 */
	public void placeTopRight() {
		Dimension ss =
			Toolkit.getDefaultToolkit().getScreenSize();

		Dimension size = getSize();
		if(size.width == 0 && size.height == 0)
			size = getPreferredSize();

		int xPos = ss.width - size.width;
		int yPos = 0;

		setBounds(xPos, yPos, size.width, size.height);
	}

	// a status display to use on the glass pane
	private class StatusDisplay extends JComponent {
		private JLabel messageLabel;
		private Color backColor;
		private Color foreColor;

		public StatusDisplay(String message) {
			this(message, Color.lightGray, Color.black);
		}

		public StatusDisplay(String message, Color backColor, Color foreColor) {
			super();

			this.backColor = backColor;
			this.foreColor = foreColor;

			init();
			setMessage(message);
		}

		private void init() {
			FormLayout layout = new FormLayout(
					"fill:pref:grow, pref, 3dlu",
					"3dlu, pref, fill:pref:grow");
			CellConstraints cc = new CellConstraints();
			this.setLayout(layout);

			messageLabel = new JLabel();
			messageLabel.setBackground(backColor);
			messageLabel.setForeground(foreColor);
			messageLabel.setOpaque(true);
			messageLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			messageLabel.setToolTipText("Click to hide");
			messageLabel.addMouseListener(new MouseInputAdapter() {

				@Override
				public void mouseClicked(MouseEvent e) {
					hideStatusComponent();
				}

			});

			messageLabel.setHorizontalAlignment(SwingConstants.RIGHT);

			add(messageLabel, cc.xy(2, 2));
		}

		public void setMessage(String msg) {
			messageLabel.setText(" " + msg + " ");
		}

		public void setForeColor(Color foreColor) {
			this.foreColor = foreColor;
			messageLabel.setForeground(foreColor);
		}

		public void setBackColor(Color backColor) {
			this.backColor = backColor;
			messageLabel.setBackground(backColor);
		}
	}

	public void showOkDialog(String title, String message) {
		showMessageDialog(title, message, MessageDialogProperties.okOptions);
	}

	public int showOkCancelDialog(String title, String message) {
		return showMessageDialog(title, message, MessageDialogProperties.okCancelOptions);
	}

	public int showYesNoDialog(String title, String message) {
		return showMessageDialog(title, message, MessageDialogProperties.yesNoOptions);
	}

	public int showYesNoCancelDialog(String title, String message) {
		return showMessageDialog(title, message, MessageDialogProperties.yesNoCancelOptions);
	}

	/**
	 * Display a message dialog to the user positioned for
	 * this window.
	 *
	 * @param title
	 * @param message
	 * @param options list of options displayed to the user
	 * @return the selected option
	 */
	public int showMessageDialog(String title, String message, String[] options) {
		final MessageDialogProperties props = new MessageDialogProperties();
		props.setParentWindow(this);
		props.setRunAsync(false);
		props.setTitle(title);
		props.setHeader(title);
		props.setMessage(message);
		props.setOptions(options);

		return NativeDialogs.showMessageDialog(props);
	}

	// TODO Fix these methods and make more useful
	// Glass pane could be used to draw red around components of interest
	private StatusDisplay statusDisplay;
	public void showStatusMessage(String message) {
		if(statusDisplay == null) {
			statusDisplay = new StatusDisplay("");
			((Container)getGlassPane()).setLayout(new BorderLayout());
			((Container)getGlassPane()).add(statusDisplay, BorderLayout.CENTER);
		}
		statusDisplay.setMessage(message);
		statusDisplay.setBackColor(Color.decode("0x3366cc"));
		statusDisplay.setForeColor(Color.white);
		getGlassPane().setVisible(true);
	}

	public void showErrorMessage(String message) {
		if(statusDisplay == null) {
			statusDisplay = new StatusDisplay("");
			((Container)getGlassPane()).setLayout(new BorderLayout());
			((Container)getGlassPane()).add(statusDisplay, BorderLayout.CENTER);
		}
		statusDisplay.setMessage(message);
		statusDisplay.setForeColor(Color.white);
		statusDisplay.setBackColor(Color.red);
		getGlassPane().setVisible(true);
	}

	public void hideStatusComponent() {
		if(getGlassPane() != null
				) {
			getGlassPane().setVisible(false);
		}
	}

	public void resetStatusComponent() {
		if(getGlassPane() != null) {
			((Container)getGlassPane()).remove(statusDisplay);
			statusDisplay = null;
		}
		repaint();
	}

	/**
	 * Does this window have un-saved changes?
	 *
	 * @return true if this window has un-saved changes
	 */
	public boolean hasUnsavedChanges() {
		return this.modified;
	}

	public void setModified(boolean modified) {
		this.modified = modified;

		getRootPane().putClientProperty("Window.documentModified", hasUnsavedChanges());
	}

	/**
	 * Save data and close window if save was successful.
	 * 
	 */
	public void saveAndClose() {
		try {
			if(saveData()) {
				SwingUtilities.invokeLater( this::dispose );
			}
		} catch (IOException e) {
			Toolkit.getDefaultToolkit().beep();
			Logger.getLogger(getClass().getName()).log(Level.WARNING, e.getLocalizedMessage(), e);
		}
	}
	
	/**
	 * Save window changes
	 *
	 * @boolean true on success
	 * @throws IOException
	 */
	public boolean saveData()
		throws IOException {
		return true;
	}

	/* window positioning */
	/**
	 * Position the window relative to another window
	 * using the boxSize and position parameters.
	 *
	 * @param boxSide one of SwingConstants.TOP,BOTTOM,RIGHT,LEFT.
	 * The window will be positioned on this side of the given window.
	 *
	 * @param position one of SwingConstants.LEADING,CENTER,TRAILING.
	 * The window will be positioned using standard left-to-right, top-to-bottom
	 * positioning based on this parameter.
	 *
	 * @param component  The frame to use as the anchor.
	 *
	 * @throws IllegalArgumentException if boxSize or position are not
	 * one of the accept values.
	 *
	 */
	public void positionRelativeTo(int boxSide, int position,
			CommonModuleFrame component) {

		// check params
		if(boxSide != SwingConstants.TOP
				&& boxSide != SwingConstants.BOTTOM
				&& boxSide != SwingConstants.LEFT
				&& boxSide != SwingConstants.RIGHT)
			throw new IllegalArgumentException("Invalid boxSide given.");

		if(position != SwingConstants.LEADING
				&& position != SwingConstants.CENTER
				&& position != SwingConstants.TRAILING)
			throw new IllegalArgumentException("Invalid position given.");

		Rectangle compBounds = component.getBounds();

		Dimension size = getPreferredSize();
		if(isVisible()) {
			size = getSize();
		}

		// get the screen width
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		int newX, newY, newW, newH;
		newX = newY  = 0;
		newW = size.width;
		newH = size.height;

		if(boxSide == SwingConstants.LEFT) {
			newX = compBounds.x - newW - 5;
		} else if(boxSide == SwingConstants.RIGHT) {
			newX = compBounds.x + compBounds.width + 5;
		} else if(boxSide == SwingConstants.TOP) {
			newY = compBounds.y - newH - 5;
		} else if(boxSide == SwingConstants.BOTTOM) {
			newY = compBounds.y + compBounds.height + 5;
		}

		if(boxSide == SwingConstants.LEFT
				|| boxSide == SwingConstants.RIGHT) {
			if(position == SwingConstants.LEADING) {
				newY = compBounds.y;
			} else if(position == SwingConstants.CENTER) {
				int halfComp = compBounds.height / 2;
				int halfSelf = newH / 2;
				newY = compBounds.y + halfComp - halfSelf;
			} else if(position == SwingConstants.TRAILING) {
				newY = compBounds.y + compBounds.height - newH;
			}
		} else {
			if(position == SwingConstants.LEADING) {
				newX = compBounds.x;
			} else if(position == SwingConstants.CENTER) {
				int halfComp = compBounds.width / 2;
				int halfSelf = newW / 2;
				newX = compBounds.x + halfComp - halfSelf;
			} else if(position == SwingConstants.TRAILING) {
				newX = compBounds.x + compBounds.width - newW;
			}
		}

		// make sure we are still on the screen
		if(newX < 0)
			newX = 0;
		if(newY < 0)
			newY = 0;

		if(newX + newW > screenSize.width) {
			newX = screenSize.width - newW;
		}

		if(newY + newH > screenSize.height) {
			newY = screenSize.height - newH;
		}

		setBounds(newX, newY, newW, newH);
	}

	/*
	 * Extension support
	 */
	private final ExtensionSupport extSupport = new ExtensionSupport(CommonModuleFrame.class, this);

	@Override
	public Set<Class<?>> getExtensions() {
		return extSupport.getExtensions();
	}

	@Override
	public <T> T getExtension(Class<T> cap) {
		return extSupport.getExtension(cap);
	}

	@Override
	public <T> T putExtension(Class<T> cap, T impl) {
		return extSupport.putExtension(cap, impl);
	}

	@Override
	public <T> T removeExtension(Class<T> cap) {
		return extSupport.removeExtension(cap);
	}
	
}
