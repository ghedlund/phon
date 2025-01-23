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
package ca.phon.app.session.editor;

import bibliothek.gui.DockStation;
import bibliothek.gui.dock.StackDockStation;
import bibliothek.gui.dock.action.*;
import bibliothek.gui.dock.action.actions.SimpleButtonAction;
import bibliothek.gui.dock.common.*;
import bibliothek.gui.dock.common.action.*;
import bibliothek.gui.dock.common.event.*;
import bibliothek.gui.dock.common.grouping.PlaceholderGrouping;
import bibliothek.gui.dock.common.intern.CDockable;
import bibliothek.gui.dock.common.intern.action.CDecorateableAction;
import bibliothek.gui.dock.common.mode.ExtendedMode;
import bibliothek.gui.dock.common.perspective.*;
import bibliothek.gui.dock.common.theme.ThemeMap;
import bibliothek.gui.dock.themes.color.TitleColor;
import bibliothek.gui.dock.util.*;
import bibliothek.gui.dock.util.color.*;
import bibliothek.util.Filter;
import bibliothek.util.Path;
import bibliothek.util.xml.*;
import ca.phon.app.log.LogUtil;
import ca.phon.app.session.ViewPosition;
import ca.phon.app.session.editor.view.transcript.TranscriptView;
import ca.phon.plugin.*;
import ca.phon.project.Project;
import ca.phon.session.Session;
import ca.phon.ui.CommonModuleFrame;
import ca.phon.ui.action.PhonUIAction;
import ca.phon.ui.menu.MenuManager;
import ca.phon.ui.nativedialogs.*;
import ca.phon.util.OSInfo;
import ca.phon.util.icons.IconManager;
import ca.phon.util.icons.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.ref.WeakReference;
import java.net.*;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

public class WorkingAreaEditorViewModel implements EditorViewModel {

	/* Since there is not one but many main-Frames, it is hard to specify which one is the root-window. The
     * FocusedWindowProvider always assumes that the window that is or was focused is the root-window. */
	private FocusedWindowProvider windows = new FocusedWindowProvider();

	/**
	 * Dock control
	 */
	private CControl dockControl;

	/**
	 * Primary content area - one used for SessionEditor
	 */
	private CContentArea rootArea;

	/**
	 * Work area
	 */
	private CWorkingArea workingArea;

	/**
	 * Dockables
	 */
	private Map<String, CDockablePerspective> dockables;

	private Map<String, ViewPosition> dockPositions;

	/**
	 * Views
	 */
	private final Map<String, EditorView> registeredViews =
			Collections.synchronizedMap(new HashMap<String, EditorView>());

	private final Map<String, JComponent> dynamicViews =
			Collections.synchronizedMap(new HashMap<>());

	/**
	 * View names by category
	 */
	private final Map<EditorViewCategory, List<String>> viewsByCategory =
			Collections.synchronizedMap(new TreeMap<EditorViewCategory, List<String>>());

	/**
	 * Weak reference to editor
	 */
	private final WeakReference<SessionEditor> editorRef;

	/**
	 * Editor view extension points
	 */
	private List<IPluginExtensionPoint<EditorView>> extPts;
	
	// flag for initial perspective loading
	private volatile boolean perspectiveFinishedLoading = false;

	public WorkingAreaEditorViewModel(SessionEditor editor) {
		super();

		editorRef = new WeakReference<SessionEditor>(editor);
		getDockControl();

		getEditor().getEventManager().registerActionForEvent(EditorEventType.EditorFinishedLoading,
				(e) -> perspectiveFinishedLoading = true, EditorEventManager.RunOn.AWTEventDispatchThread);
	}

	// region Dockable control setup
	private CControl getDockControl() {
		if(dockControl == null) {
			dockControl = new CControl( windows );
			//windows.add(SwingUtilities.getWindowAncestor(getEditor()));
			setupDockControl();
		}
		return dockControl;
	}

	private void setupDockControl() {
		// theme
		dockControl.setTheme(ThemeMap.KEY_FLAT_THEME);

		bibliothek.gui.dock.util.IconManager icons = dockControl.getIcons();
		icons.setIconClient("close", IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "minimize", IconSize.SMALL, Color.darkGray));
		icons.setIconClient("locationmanager.maximize", IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "fullscreen", IconSize.SMALL, Color.darkGray));
		icons.setIconClient("locationmanager.minimize", IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "minimize", IconSize.SMALL, Color.darkGray));
		icons.setIconClient("locationmanager.normalize", IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "fullscreen_exit", IconSize.SMALL, Color.darkGray));

		dockControl.addControlListener(new CControlListener() {
			
			@Override
			public void removed(CControl arg0, CDockable arg1) {
			}
			
			@Override
			public void opened(CControl arg0, CDockable arg1) {
				String viewName = arg1.intern().getTitleText();
				if(viewName.trim().length() > 0) {
					EditorView view = getView(viewName);
					if(view != null) {
						view.onOpen();
					}
				}
			}
			
			@Override
			public void closed(CControl arg0, CDockable arg1) {
				String viewName = arg1.intern().getTitleText();
				if(viewName.trim().length() > 0) {
					EditorView view = getView(viewName);
					if(view != null) {
						view.onClose();
					}
				}
			}
			
			@Override
			public void added(CControl arg0, CDockable arg1) {
				
			}
			
		});
		
		dockControl.addFocusListener(new CFocusListener() {
			
			@Override
			public void focusLost(CDockable arg0) {
				
			}
			
			@Override
			public void focusGained(CDockable arg0) {
				EditorView focusedView = getFocusedView();
				if(focusedView != null) {
					focusedView.onFocused();
				}
			}
			
		});
		
		// fix accelerators on non-mac systems
		if(!OSInfo.isMacOs()) {
			// fix accelerators for non-mac systems
			dockControl.putProperty( CControl.KEY_MAXIMIZE_CHANGE, KeyStroke.getKeyStroke( KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK ) );
			dockControl.putProperty( CControl.KEY_GOTO_EXTERNALIZED, KeyStroke.getKeyStroke( KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK ) );
			dockControl.putProperty( CControl.KEY_GOTO_NORMALIZED, KeyStroke.getKeyStroke( KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK ) );
		}

		// setup factory
		dockControl.addSingleDockableFactory(dockableFilter, dockFactory);

		rootArea = dockControl.getContentArea();
		workingArea = dockControl.createWorkingArea("work");
		setupDockables();

		// fix title colours using substance theme on windows/linux
		final TitleColorBridge bridge = new TitleColorBridge();
		final ColorManager colorManager = dockControl.getController().getColors();
		colorManager.publish(Priority.CLIENT, TitleColor.KIND_TITLE_COLOR, bridge);
		colorManager.publish(Priority.CLIENT, TitleColor.KIND_FLAP_BUTTON_COLOR, bridge);
	}

	private SessionEditor getEditor() {
		return editorRef.get();
	}

	private List<IPluginExtensionPoint<EditorView>> getExtensionPoints() {
		if(extPts == null) {
			extPts = PluginManager.getInstance().getExtensionPoints(EditorView.class);
		}
		return extPts;
	}

	private Map<String, CDockablePerspective> getDockables() {
		return dockables;
	}

	private void setupDockables() {
		dockables = new TreeMap<String, CDockablePerspective>();
		dockPositions = new LinkedHashMap<>();

		// first add all ViewPosition placeholders
		for(ViewPosition pos:ViewPosition.values()) {
			final SingleCDockablePerspective dockable =
					new SingleCDockablePerspective(pos.getName());
			dockables.put(pos.getName(), dockable);
			dockPositions.put(pos.getName(), pos);
		}

		for(IPluginExtensionPoint<EditorView> extPt:getExtensionPoints()) {
			final EditorViewInfo viewInfo = extPt.getClass().getAnnotation(EditorViewInfo.class);
			if(viewInfo == null) continue; // should never happen
			final String dockableName = viewInfo.name();
			List<String> categoryDockables = viewsByCategory.get(viewInfo.category());
			if(categoryDockables == null) {
				categoryDockables = new ArrayList<String>();
				viewsByCategory.put(viewInfo.category(), categoryDockables);
			}
			categoryDockables.add(dockableName);
			final SingleCDockablePerspective dockable =
					new SingleCDockablePerspective(dockableName);
			dockables.put(dockableName, dockable);
			dockPositions.put(dockableName, viewInfo.dockPosition());
		}
	}
	// endregion

	private CDockable getViewDockable(String viewName) {
		CDockable retVal = null;
		final CControlRegister register = dockControl.getRegister();
		for(CDockable currentDockable:register.getDockables()) {
			if(currentDockable.intern().getTitleText().equals(viewName)) {
				retVal = currentDockable;
				break;
			}
		}
		return retVal;
	}

	// region EditorViewModel
	@Override
	public Container getRoot() {
		return rootArea;
	}

	/**
	 * Return focused view
	 *
	 * @return currently focused view or <code>null</code>
	 */
	@Override
	public EditorView getFocusedView() {
		CDockable focusedDockable = getDockControl().getFocusedCDockable();
		if(focusedDockable != null) {
			String viewName = focusedDockable.intern().getTitleText();
			if(viewName.trim().length() > 0) {
				return getView(viewName);
			}
		}
		return null;
	}

	@Override
	public EditorView getView(String viewName) {

		EditorView retVal = registeredViews.get(viewName);
		if(retVal == null) {
			// attempt to load editor view
			for(IPluginExtensionPoint<EditorView> extPt:extPts) {
				final EditorViewInfo pluginAnnotation = extPt.getClass().getAnnotation(EditorViewInfo.class);
				if(pluginAnnotation != null && pluginAnnotation.name().equals(viewName)) {
					final IPluginExtensionFactory<EditorView> viewFactory = extPt.getFactory();
					try {
						retVal = viewFactory.createObject(getEditor());
						registeredViews.put(viewName, retVal);
					} catch (Exception e) {
						LogUtil.severe( e.getLocalizedMessage(),
								e);
					}
					break;
				}
			}
		}
		return retVal;
	}

	@Override
	public JComponent getDynamicView(String viewName) {
		JComponent retVal = null;
		if(dynamicViews.containsKey(viewName)) {
			retVal = dynamicViews.get(viewName);
		}
		return retVal;
	}

	@Override
	public Action getCloseAction(String viewName) {
		final CDockable dockable = getViewDockable(viewName);
		if(dockable != null) {
			final CloseActionFactory factory = dockControl.getController().getProperties().get( CControl.CLOSE_ACTION_FACTORY );
			final CAction closeAct = factory.create(dockControl, dockable);

			final CActionWrapper wrapper = new CActionWrapper(dockable, closeAct);
			wrapper.putValue(CActionWrapper.NAME, "Close");
			return wrapper;
		}
		return null;
	}

	@Override
	public ImageIcon getViewIcon(String viewName) {
		for(IPluginExtensionPoint<EditorView> extPt:extPts) {
			final EditorViewInfo pluginAnnotation = extPt.getClass().getAnnotation(EditorViewInfo.class);
			if(pluginAnnotation != null && pluginAnnotation.name().equals(viewName)) {
				final String iconName = pluginAnnotation.icon();
				final String[] iconData = iconName.split(":");
				if(iconData.length == 1) {
					return IconManager.getInstance().getIcon(iconName, IconSize.SMALL);
				} else {
					// setup colour as defined by system theme (dark/light)
					return IconManager.getInstance().getFontIcon(iconData[0], iconData[1], IconSize.SMALL, Color.darkGray);
				}
			}
		}
		return null;
	}

	@Override
	public Set<String> getViewNames() {
		return getDockables().keySet();
	}

	@Override
	public Map<EditorViewCategory, List<String>> getViewsByCategory() {
		if(this.dockables == null) {
			getDockables();
		}
		return this.viewsByCategory;
	}

	@Override
	public boolean isShowing(String viewName) {
		boolean retVal = false;
		final CControlRegister register = dockControl.getRegister();
		for(CDockable currentDockable:register.getDockables()) {
			if(currentDockable.intern().getTitleText().equals(viewName)) {
				retVal = currentDockable.isVisible();
			}
		}
		return retVal;
	}

	@Override
	public boolean isShowingInStack(String viewName) {
		boolean retVal = false;
		final CControlRegister register = dockControl.getRegister();
		for(CDockable currentDockable:register.getDockables()) {
			if(currentDockable.intern().getTitleText().equals(viewName)) {
				retVal = currentDockable.isVisible();
				
				DockStation station = currentDockable.intern().getDockParent();
				if(station instanceof StackDockStation) {
					retVal = ((StackDockStation)station).isChildShowing(currentDockable.intern());
				}
			}
		}
		return retVal;
	}

	// region Listeners
	private final List<EditorViewModelListener> listeners = new ArrayList<EditorViewModelListener>();

	@Override
	public void addEditorViewModelListener(EditorViewModelListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeEditorViewModelListener(EditorViewModelListener listener) {
		listeners.remove(listener);
	}

	@Override
	public List<EditorViewModelListener> getEditorViewModelListeners() {
		return Collections.unmodifiableList(listeners);
	}

	public void fireViewShown(String viewName) {
		for(EditorViewModelListener listener:getEditorViewModelListeners()) {
			listener.viewShown(viewName);
		}
	}

	public void fireViewHidden(String viewName) {
		for(EditorViewModelListener listener:getEditorViewModelListeners()) {
			listener.viewHidden(viewName);
		}
	}

	public void fireViewMinimized(String viewName) {
		for(EditorViewModelListener listener:getEditorViewModelListeners()) {
			listener.viewMinimized(viewName);
		}
	}

	public void fireViewMaximized(String viewName) {
		for(EditorViewModelListener listener:getEditorViewModelListeners()) {
			listener.viewMaximized(viewName);
		}
	}

	public void fireViewNormalized(String viewName) {
		for(EditorViewModelListener listener:getEditorViewModelListeners()) {
			listener.viewNormalized(viewName);
		}
	}

	public void fireViewExternalized(String viewName) {
		for(EditorViewModelListener listener:getEditorViewModelListeners()) {
			listener.viewExternalized(viewName);
		}
	}

	public void fireViewFocused(String viewName) {
		for(EditorViewModelListener listener:getEditorViewModelListeners()) {
			listener.viewFocused(viewName);
		}
	}
	// endregion

	@Override
	public void cleanup() {
		for(int i = 0; i < dockControl.getCDockableCount(); i++) {
			final CDockable dockable = dockControl.getCDockable(i);
			dockable.removeCDockableLocationListener(dockableLocationListener);
		}

		dockControl.getController().kill();
		dockControl = null;

		registeredViews.clear();
		dockables.clear();

		windows.remove(SwingUtilities.getWindowAncestor(getEditor()));
	}

	@Override
	public void showView(String viewName) {
		if(isShowing(viewName)) {
			CDockable dockable = getViewDockable(viewName);
			if(dockable != null) {
				dockControl.getController().setAtLeastFocusedDockable(dockable.intern(), null);
			}
			return;
		}

		EditorViewDockable dockable = (EditorViewDockable) dockControl.getSingleDockable(viewName);
		if(dockable == null) {
			final SingleCDockableFactory factory = dockControl.getSingleDockableFactory(viewName);
			dockable = (EditorViewDockable) factory.createBackup(viewName);
		}

		if(dockable != null) {
			ViewPosition dockPosition = dockPositions.get(viewName);
			if(dockPosition == ViewPosition.WORK) {
				workingArea.show(dockable);
			} else {
				dockable.setGrouping(new PlaceholderGrouping(dockControl, new Path("dock", "single", dockPosition.getName())));
				dockControl.addDockable(dockable);
			}
			dockable.setVisible(true);
			savePreviousPerspective();

			Window parentWin = SwingUtilities.getWindowAncestor(getEditor());
			if(parentWin instanceof CommonModuleFrame commonModuleFrame) {
				commonModuleFrame.setJMenuBar(MenuManager.createWindowMenuBar(commonModuleFrame));
				for (AccessoryWindow accWin : accessoryWindows) {
					accWin.setJMenuBar(MenuManager.createWindowMenuBar(accWin));
				}
			}
		}
	}

	@Override
	public void hideView(String viewName) {
		if(!isShowing(viewName)) return;
		if(!dockControl.getSingleDockable(viewName).isCloseable()) return;

		dockControl.removeDockable(dockControl.getSingleDockable(viewName));
	}

	@Override
	public void showDynamicFloatingDockable(String title, JComponent comp,
			int x, int y, int w, int h) {
		throw new UnsupportedOperationException("Dynamic floating dockables not supported by this view model");
	}

	@Override
	public void showDynamicDockable(String title, JComponent comp, ViewPosition position) {
		final WorkingAreaEditorViewModel.DynamicViewFactory factory = new WorkingAreaEditorViewModel.DynamicViewFactory(comp);
		final DefaultSingleCDockable dockable =  (DefaultSingleCDockable) factory.createBackup(title);
		dockable.setGrouping(new PlaceholderGrouping(dockControl, new Path("dock", "single", position.getName())));
		dockControl.addDockable(dockable);
		dockable.setVisible(true);
	}

	@Override
	public void setupWindows(RecordEditorPerspective editorPerspective) {
		final AccessoryWindow[] windows = accessoryWindows.toArray(new AccessoryWindow[0]);
		for(AccessoryWindow window:windows) {
			window.setVisible(false);
			accessoryWindows.remove(window);
			dockControl.removeStationContainer(window.getArea());
			window.dispose();
		}

		if(editorPerspective != null) {
			try (InputStream is = editorPerspective.getLocation().openStream()) {
				if (is != null) {
					final XElement xele = XIO.readUTF(is);

					final XElement boundsEle = xele.getElement("bounds");
					Window win = SwingUtilities.getWindowAncestor(getEditor());
					if (boundsEle != null) {
						int x = boundsEle.getAttribute("x").getInt();
						int y = boundsEle.getAttribute("y").getInt();
						int width = boundsEle.getAttribute("width").getInt();
						int height = boundsEle.getAttribute("height").getInt();
						final XAttribute extendedStateAttr = boundsEle.getAttribute("extendedState");
						int extendedState = JFrame.NORMAL;
						if (extendedStateAttr != null) {
							extendedState = extendedStateAttr.getInt();
						}

						if (win instanceof CommonModuleFrame commonModuleFrame) {
							if (width >= 0 && height >= 0) {
								commonModuleFrame.setSize(width, height);
							}
							commonModuleFrame.setLocation(x, y);
							commonModuleFrame.setExtendedState(extendedState);
						}
					} else {
						if (!getEditor().isVisible() && win instanceof CommonModuleFrame cmf)
							cmf.cascadeWindow(CommonModuleFrame.getCurrentFrame());
					}

					final XElement windowsEle = xele.getElement("windows");
					if (windowsEle != null) {
						for (int i = 0; i < windowsEle.getElementCount(); i++) {
							final XElement winEle = windowsEle.getElement(i);

							final String uuid = winEle.getAttribute("uid").getString();

							final AccessoryWindow window = (AccessoryWindow) createAccessoryWindow(
									UUID.fromString(uuid));
							int x = winEle.getAttribute("x").getInt();
							int y = winEle.getAttribute("y").getInt();
							int width = winEle.getAttribute("width").getInt();
							int height = winEle.getAttribute("height").getInt();
							final XAttribute extendedStateAttr = winEle.getAttribute("extendedState");
							int extendedState = JFrame.NORMAL;
							if (extendedStateAttr != null) {
								extendedState = extendedStateAttr.getInt();
							}

							if (width >= 0 && height >= 0) {
								window.setSize(width, height);
							}
							window.setLocation(x, y);
							window.setExtendedState(extendedState);
							window.setVisible(true);
						}
					}
				}
			} catch (IOException e) {
				LogUtil.severe(e);
			}
		} else {
			// default window layout
			final CommonModuleFrame cmf = getFrameForEditor();
			cmf.setSize(1024, 768);
			if(cmf != null) {
				cmf.cascadeWindow(CommonModuleFrame.getCurrentFrame());
			}
		}
	}

	@Override
	public void setupDefaultPerspective() {
		CControlPerspective perspectives = dockControl.getPerspectives();
		CPerspective perspective = perspectives.createEmptyPerspective();

		CGridPerspective center = perspective.getContentArea().getCenter();

		CWorkingPerspective workingPerspective = (CWorkingPerspective) perspective.getStation("work");
		center.gridAdd( ViewPosition.WORK.getX(), ViewPosition.WORK.getY(), ViewPosition.WORK.getWidth(), ViewPosition.WORK.getHeight(), workingPerspective );

		for(String viewName:dockables.keySet()) {
			final ViewPosition dockPosition = dockPositions.get(viewName);
			if (dockPosition == ViewPosition.WORK) {
				if(TranscriptView.VIEW_NAME.equals(viewName)) {
					workingPerspective.gridAdd(0, 0, ViewPosition.WORK.getWidth(), ViewPosition.WORK.getHeight(), dockables.get(viewName));
				} else {
					workingPerspective.gridPlaceholder(0, 0, ViewPosition.WORK.getWidth(), ViewPosition.WORK.getHeight(), new Path("dock", "single", dockPosition.getName()));
				}
			} else {
				center.gridPlaceholder(dockPosition.getX(), dockPosition.getY(), dockPosition.getWidth(), dockPosition.getHeight(), new Path("dock", "single", dockPosition.getName()));
			}
		}
		perspective.storeLocations();

		perspectives.setPerspective("default", perspective, true);
		dockControl.load("default", true);
	}

	@Override
	public void applyPerspective(RecordEditorPerspective editorPerspective) {
		CPerspective perspective = editorPerspective.getPerspective(dockControl.getPerspectives());
		if(perspective != null) {
			dockControl.getPerspectives().setPerspective(editorPerspective.getName(), perspective, true);
			perspective.storeLocations();
			dockControl.load(editorPerspective.getName(), true);

			Window win = SwingUtilities.getWindowAncestor(getEditor());
			if (win instanceof CommonModuleFrame cmf) {
				cmf.setJMenuBar(MenuManager.createWindowMenuBar(cmf));
				for (AccessoryWindow accWin : accessoryWindows) {
					accWin.setJMenuBar(MenuManager.createWindowMenuBar(accWin));
				}
			}
		}
	}

	@Override
	public void savePerspective(RecordEditorPerspective editorPerspective) {
		if(dockControl == null) return;
		final CPerspective perspective = dockControl.getPerspectives().getPerspective(true);
		if(perspective != null) {
			try {
				final XElement root = new XElement("root");

				// add position of window as attributes
				final XElement rootBoundsEle = root.addElement("bounds");

				final CommonModuleFrame cmf = getFrameForEditor();
				if(cmf != null)
					writeBoundsInfo(rootBoundsEle, cmf);

				final XElement accessoryWindowsEle = root.addElement("windows");
				for(AccessoryWindow window:accessoryWindows) {
					final XElement winEle = accessoryWindowsEle.addElement("window");
					final XAttribute uuid = new XAttribute("uid");
					uuid.setString(window.uuid.toString());
					winEle.addAttribute(uuid);

					writeBoundsInfo(winEle, window);
				}

				dockControl.getPerspectives().writeXML(root, perspective, true);

				final File f = new File(editorPerspective.getLocation().toURI());
				XIO.writeUTF(root, new FileOutputStream(f));
			} catch (IOException | URISyntaxException e) {
				LogUtil.severe(e);
			}
		}
	}

	// endregion

	/**
	 * Delete given perspective.
	 *
	 * @param perspective
	 */
	public void onDeletePerspective(RecordEditorPerspective perspective) {
		final MessageDialogProperties props = new MessageDialogProperties();
		props.setParentWindow(CommonModuleFrame.getCurrentFrame());
		props.setTitle("Delete Layout");
		props.setHeader("Delete Layout");
		props.setMessage("Delete layout '" + perspective.getName() + "'?");
		props.setOptions(MessageDialogProperties.okCancelOptions);
		props.setRunAsync(false);

		final int retVal = NativeDialogs.showMessageDialog(props);
		if(retVal == 0) {
			RecordEditorPerspective.deletePerspective(perspective);
			removePrespective(perspective);
		}
	}

	public void loadPerspective(RecordEditorPerspective editorPerspective) {
		setupWindows(editorPerspective);
		applyPerspective(editorPerspective);
	}

	private CommonModuleFrame getFrameForEditor() {
		final Window window = SwingUtilities.getWindowAncestor(getEditor());
		if(window instanceof  CommonModuleFrame commonModuleFrame)
			return commonModuleFrame;
		else
			return null;
	}

	private void writeBoundsInfo(XElement rootBoundsEle, JFrame frame) {
		final XAttribute xAttr = new XAttribute("x");
		xAttr.setInt(frame.getX());
		rootBoundsEle.addAttribute(xAttr);

		final XAttribute yAttr = new XAttribute("y");
		yAttr.setInt(frame.getY());
		rootBoundsEle.addAttribute(yAttr);

		final XAttribute widthAttr = new XAttribute("width");
		widthAttr.setInt(frame.getWidth());
		rootBoundsEle.addAttribute(widthAttr);

		final XAttribute heightAttr = new XAttribute("height");
		heightAttr.setInt(frame.getHeight());
		rootBoundsEle.addAttribute(heightAttr);

		final XAttribute extendedStateAttr = new XAttribute("extendedState");
		extendedStateAttr.setInt(frame.getExtendedState());
		rootBoundsEle.addAttribute(extendedStateAttr);
	}
	
	private void savePreviousPerspective() {
		// XXX Only save previous perspective when running as
		// a 'Session Editor' window
		if(!getEditor().getTitle().startsWith("Session Editor")) return;
		if(!perspectiveFinishedLoading) return;

		final File prevPerspetiveFile = new File(RecordEditorPerspective.PERSPECTIVES_FOLDER,
				RecordEditorPerspective.LAST_USED_PERSPECTIVE_NAME + ".xml");
		try {
			final RecordEditorPerspective prevPerspective =
					new RecordEditorPerspective(RecordEditorPerspective.LAST_USED_PERSPECTIVE_NAME,
							prevPerspetiveFile.toURI().toURL());
			savePerspective(prevPerspective);
		} catch (MalformedURLException e1) {
			LogUtil.severe(e1.getLocalizedMessage(), e1);
		}
	}

	@Override
	public void removePrespective(RecordEditorPerspective editorPerspective) {
		dockControl.getPerspectives().removePerspective(editorPerspective.getName());
	}

	// filter for dockable factory
	private final Filter<String> dockableFilter = new Filter<String>() {

		@Override
		public boolean includes(String item) {
			return getDockables().containsKey(item);
		}

	};

	// dockable factory
	private final SingleCDockableFactory dockFactory = new SingleCDockableFactory() {

		@Override
		public SingleCDockable createBackup(String id) {
			SingleCDockable retVal = null;
			final EditorView editorView = getView(id);
			if(editorView != null) {
				retVal = new EditorViewDockable(editorView.getName(), editorView, new CAction[0]);
				retVal.addCDockableLocationListener(dockableLocationListener);
			}
			return retVal;
		}

	};

	/**
	 * View factory for dynamically added dockables.
	 */
	private class DynamicViewFactory implements SingleCDockableFactory {

		private JComponent content;

		public DynamicViewFactory(JComponent content) {
			this.content = content;
		}

		@Override
		public SingleCDockable createBackup(String arg0) {
			final String title = arg0;
			final DefaultSingleCDockable retVal = new DefaultSingleCDockable(title, null, title, content, new CAction[0]);
			retVal.setCloseable(true);
			retVal.addCDockableStateListener(new CDockableStateListener() {

				@Override
				public void visibilityChanged(CDockable arg0) {
					if(!arg0.isVisible()) {
						dockControl.removeDockable(retVal);
						dynamicViews.remove(title);
					}
				}

				@Override
				public void extendedModeChanged(CDockable arg0, ExtendedMode arg1) {
				}
			});
			return retVal;
		}

	}

	// dockable wrapper for editor views
	private class EditorViewDockable extends DefaultSingleCDockable {

		private final WeakReference<EditorView> viewRef;

		public EditorViewDockable(String id, EditorView editorView, CAction[] actions) {
			super(id, editorView.getIcon(), editorView.getName(), editorView, actions);
			super.setCloseable(true);
			super.setExternalizable(false);
			super.setMinimizable(false);
			if(TranscriptView.VIEW_NAME.equals(id)) {
				super.setCloseable(false);
			}

			addCDockableStateListener(new CDockableStateListener() {
				@Override
				public void visibilityChanged(CDockable cDockable) {
					if(cDockable.isVisible()) {
						fireViewShown(id);
					} else {
						fireViewHidden(id);
					}
				}

				@Override
				public void extendedModeChanged(CDockable cDockable, ExtendedMode extendedMode) {
					if(extendedMode == ExtendedMode.MAXIMIZED) {
						fireViewMaximized(id);
					} else if(extendedMode == ExtendedMode.MINIMIZED) {
						fireViewMinimized(id);
					} else if(extendedMode == ExtendedMode.NORMALIZED) {
						fireViewNormalized(id);
					}
				}
			});

			final SimpleButtonAction menuAct = new SimpleButtonAction();
			menuAct.setText("View menu");
			menuAct.setIcon(IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "MORE_VERT", IconSize.SMALL, Color.darkGray));
			menuAct.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					final JMenu viewMenu = editorView.getMenu();
					if(viewMenu == null) return;
					final JPopupMenu popupMenu = viewMenu.getPopupMenu();
					popupMenu.show(editorView, editorView.getWidth() - popupMenu.getPreferredSize().width, 0);
				}

			});

			final DefaultDockActionSource actionSource = new DefaultDockActionSource(
					new LocationHint(LocationHint.DOCKABLE, LocationHint.LEFT));
			actionSource.add(menuAct);

			if(!TranscriptView.VIEW_NAME.equals(id)) {
				final SimpleButtonAction externalizeAct = new SimpleButtonAction();
				externalizeAct.setText("Open view in new window");
				externalizeAct.setIcon(IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "open_in_new", IconSize.SMALL, Color.darkGray));
				externalizeAct.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {

						final AccessoryWindow window = (AccessoryWindow) createAccessoryWindow(UUID.randomUUID());
						window.getArea().getCenter().drop(EditorViewDockable.this.intern());
						window.pack();
						window.setVisible(true);
					}

				});

				actionSource.add(externalizeAct);
			}
			super.intern().setActionOffers(actionSource);


			viewRef = new WeakReference<EditorView>(editorView);
		}

		@SuppressWarnings("unused")
		public EditorView getView() {
			return viewRef.get();
		}

	}

	// wrapp class for CActions
	private class CActionWrapper extends AbstractAction {

		// dockable
		private final CDockable dockable;

		// action
		private final CAction action;

		public CActionWrapper(CDockable dockable, CAction action) {
			super();
			this.dockable = dockable;
			this.action = action;

			if(action instanceof CDecorateableAction) {
				final CDecorateableAction<?> decAct = (CDecorateableAction<?>)action;
				putValue(NAME, decAct.getText());
				putValue(SMALL_ICON, decAct.getIcon());
				putValue(ACCELERATOR_KEY, decAct.getAccelerator());
			}
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			action.intern().trigger(dockable.intern());
		}

	}

	private class TitleColorBridge implements ColorBridge {

		@Override
		public void add(String id, DockColor uiValue) {
		}

		@Override
		public void remove(String id, DockColor uiValue) {
		}

		@Override
		public void set(String id, Color value, DockColor uiValue) {
			switch(id) {
			case "title.flap.active.text":
				uiValue.set(UIManager.getColor(SessionEditorUIProps.VIEW_FLAP_ACTIVE_TEXT));
				break;

			case "title.active.text":
				uiValue.set(UIManager.getColor(SessionEditorUIProps.VIEW_ACTIVE_TEXT));
				break;

			case "title.flap.inactive.text":
				uiValue.set(UIManager.getColor(SessionEditorUIProps.VIEW_TITLE_FLAP_INACTIVE_TEXT));
				break;

			case "title.inactive.text":
				uiValue.set(UIManager.getColor(SessionEditorUIProps.VIEW_INACTIVE_TEXT));
				break;

			case "title.flap.active.left":
				uiValue.set(UIManager.getColor(SessionEditorUIProps.VIEW_FLAP_ACTIVE_LEFT));
				break;

			case "title.active.left":
				uiValue.set(UIManager.getColor(SessionEditorUIProps.VIEW_ACTIVE_LEFT));
				break;

			case "title.flap.active.right":
				uiValue.set(UIManager.getColor(SessionEditorUIProps.VIEW_FLAP_ACTIVE_RIGHT));
				break;

			case "title.active.right":
				uiValue.set(UIManager.getColor(SessionEditorUIProps.VIEW_ACTIVE_RIGHT));
				break;

			case "title.flap.inactive.left":
				uiValue.set(UIManager.getColor(SessionEditorUIProps.VIEW_FLAP_INACTIVE_LEFT));

			case "title.inactive.left":
				uiValue.set(UIManager.getColor(SessionEditorUIProps.VIEW_INACTIVE_LEFT));
				break;

			case "title.flap.inactive.right":
				uiValue.set(UIManager.getColor(SessionEditorUIProps.VIEW_FLAP_INACTIVE_RIGHT));

			case "title.inactive.right":
				uiValue.set(UIManager.getColor(SessionEditorUIProps.VIEW_INACTIVE_RIGHT));
				break;
			}
		}
	}

	@Override
	public void setupViewMenu(MenuElement ele) {
		final Map<EditorViewCategory, List<String>> viewsByCategory =
				getViewsByCategory();
		for(EditorViewCategory category:viewsByCategory.keySet()) {
			final JMenuItem categoryItem = new JMenuItem("-- " + category.title + " --");
			categoryItem.setEnabled(false);
			if(ele.getComponent() instanceof JMenu) {
				final JMenu menu = (JMenu)ele;
				menu.add(categoryItem);
			} else if(ele.getComponent() instanceof JPopupMenu) {
				final JPopupMenu menu = (JPopupMenu)ele;
				menu.add(categoryItem);
			}

			for(String view:viewsByCategory.get(category)) {
				final PhonUIAction<String> toggleViewAct = PhonUIAction.consumer(this::showView, view);
				toggleViewAct.putValue(PhonUIAction.NAME, view);
				toggleViewAct.putValue(PhonUIAction.SMALL_ICON, getViewIcon(view));

				JComponent viewItem = new JMenuItem(toggleViewAct);

				if(isShowing(view)) {
					JMenu menu = getView(view).getMenu();
					if(menu != null) {
						menu.addSeparator();
					} else {
						menu = new JMenu();
					}
					menu.setText(toggleViewAct.getValue(PhonUIAction.NAME).toString());
					menu.setIcon(getViewIcon(view));
					final Action closeAct = getCloseAction(view);
					menu.add(closeAct);

					viewItem = menu;
				}

				if(ele.getComponent() instanceof JMenu) {
					final JMenu menu = (JMenu)ele;
					menu.add(viewItem);
				} else if(ele.getComponent() instanceof JPopupMenu) {
					final JPopupMenu menu = (JPopupMenu)ele;
					menu.add(viewItem);
				}
			}
		}
	}

	@Override
	public void setupPerspectiveMenu(MenuElement menuElement) {
		final JMenu layoutMenu = new JMenu("Load layout");
		ImageIcon loadLayoutIcon = IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName,
				"view_quilt", IconSize.SMALL, UIManager.getColor("MenuItem.foreground"));
		layoutMenu.setIcon(loadLayoutIcon);
		layoutMenu.addMenuListener(new MenuListener() {

			@Override
			public void menuSelected(MenuEvent e) {
				layoutMenu.removeAll();
				setupLayoutMenu(layoutMenu);
			}

			@Override
			public void menuDeselected(MenuEvent e) {
			}

			@Override
			public void menuCanceled(MenuEvent e) {
			}
		});

		if(menuElement instanceof JPopupMenu)
			((JPopupMenu)menuElement).add(layoutMenu);
		else if(menuElement instanceof JMenu)
			((JMenu)menuElement).add(layoutMenu);

		final JMenu deleteMenu = new JMenu("Delete layout");
		ImageIcon deleteLayoutIcon = IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName,
				"delete", IconSize.SMALL, UIManager.getColor("MenuItem.foreground"));
		deleteMenu.setIcon(deleteLayoutIcon);
		deleteMenu.addMenuListener(new MenuListener() {

			@Override
			public void menuSelected(MenuEvent arg0) {
				setupDeleteLayoutMenu(deleteMenu);
			}

			@Override
			public void menuDeselected(MenuEvent arg0) {
			}

			@Override
			public void menuCanceled(MenuEvent arg0) {
			}
		});

		if(menuElement instanceof JPopupMenu)
			((JPopupMenu)menuElement).add(deleteMenu);
		else if(menuElement instanceof JMenu)
			((JMenu)menuElement).add(deleteMenu);

		// save current layout
		final PhonUIAction<Void> saveLayoutAct = PhonUIAction.runnable(this::onSaveLayout);
		ImageIcon saveLayoutIcon = IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName,
				"save", IconSize.SMALL, UIManager.getColor("MenuItem.foreground"));
		saveLayoutAct.putValue(PhonUIAction.SMALL_ICON, saveLayoutIcon);
		saveLayoutAct.putValue(PhonUIAction.NAME, "Save current layout...");
		saveLayoutAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Save current layout as a preset.");
		final JMenuItem saveLayoutItem = new JMenuItem(saveLayoutAct);

		if(menuElement instanceof JPopupMenu)
			((JPopupMenu)menuElement).add(saveLayoutItem);
		else if(menuElement instanceof JMenu)
			((JMenu)menuElement).add(saveLayoutItem);
	}

	/**
	 * Adds a menu item for all available editor perspectives.
	 *
	 * @param menu
	 */
	@Override
	public void setupLayoutMenu(MenuElement menu) {
		if(menu.getComponent() instanceof JMenu) {
			((JMenu)menu).removeAll();
		} else if(menu.getComponent() instanceof JPopupMenu) {
			((JPopupMenu)menu).removeAll();
		}

		final Consumer<RecordEditorPerspective> addPerspectiveToMenu = (editorPerspective) -> {
			final PhonUIAction<RecordEditorPerspective> showPerspectiveAct = PhonUIAction.consumer(this::loadPerspective, editorPerspective);
			showPerspectiveAct.putValue(PhonUIAction.NAME, editorPerspective.getName());
			showPerspectiveAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Load perspective: " + editorPerspective.getName());
			final JMenuItem showPerspectiveItem = new JMenuItem(showPerspectiveAct);

			if(menu.getComponent() instanceof JMenu) {
				final JMenu m = (JMenu)menu;
				m.add(showPerspectiveItem);
			} else if(menu.getComponent() instanceof JPopupMenu) {
				final JPopupMenu m = (JPopupMenu)menu;
				m.add(showPerspectiveItem);
			}
		};

		final Iterator<RecordEditorPerspective> stockItr = RecordEditorPerspective.getStockPerspectives().iterator();
		while(stockItr.hasNext()) {
			addPerspectiveToMenu.accept(stockItr.next());
		}

		final PhonUIAction<Void> showPerspectivesFolderAct = PhonUIAction.runnable(this::onShowLayoutFolder);
		showPerspectivesFolderAct.putValue(PhonUIAction.NAME, "-- User Library --");
		showPerspectivesFolderAct.putValue(PhonUIAction.SHORT_DESCRIPTION, RecordEditorPerspective.PERSPECTIVES_FOLDER.getAbsolutePath());
		final JMenuItem showFolderItem = new JMenuItem(showPerspectivesFolderAct);
		showFolderItem.setFont(showFolderItem.getFont().deriveFont(Font.BOLD));
		if(menu.getComponent() instanceof JMenu) {
			((JMenu)menu).addSeparator();
			((JMenu)menu).add(showFolderItem);
		} else if(menu.getComponent() instanceof JPopupMenu) {
			((JPopupMenu)menu).addSeparator();
			((JPopupMenu)menu).add(showFolderItem);
		}

		final Iterator<RecordEditorPerspective> userItr = RecordEditorPerspective.getUserPerspectives().iterator();
		while(userItr.hasNext()) {
			addPerspectiveToMenu.accept(userItr.next());
		}
	}

	public void onShowLayoutFolder() {
		if(Desktop.isDesktopSupported()) {
			try {
				Desktop.getDesktop().open(RecordEditorPerspective.PERSPECTIVES_FOLDER);
			} catch (IOException e) {
				LogUtil.warning(e);
				Toolkit.getDefaultToolkit().beep();
			}
		}
	}

	/**
	 * Add a menu to delete user-defined editor perspectives.
	 *
	 * @param ele
	 */
	private void setupDeleteLayoutMenu(MenuElement ele) {
		if(ele.getComponent() instanceof JMenu) {
			((JMenu)ele).removeAll();
		} else if(ele.getComponent() instanceof JPopupMenu) {
			((JPopupMenu)ele).removeAll();
		}
		for(RecordEditorPerspective editorPerspective:RecordEditorPerspective.availablePerspectives()) {
			try {
				final File perspectiveFile = new File(editorPerspective.getLocation().toURI());
				if(perspectiveFile.canWrite()) {
					// add delete item
					final PhonUIAction<RecordEditorPerspective> delPerspectiveAct =  PhonUIAction.consumer(this::onDeletePerspective, editorPerspective);
					delPerspectiveAct.putValue(PhonUIAction.NAME, editorPerspective.getName());
					delPerspectiveAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Delete layout " + editorPerspective.getName());
					final JMenuItem delPerspectiveItem = new JMenuItem(delPerspectiveAct);

					if(ele.getComponent() instanceof JMenu) {
						final JMenu menu = (JMenu)ele;
						menu.add(delPerspectiveItem);
					} else if(ele.getComponent() instanceof JPopupMenu) {
						final JPopupMenu menu = (JPopupMenu)ele;
						menu.add(delPerspectiveItem);
					}
				}
			} catch (URISyntaxException e) {

			} catch (IllegalArgumentException e) {
				// thrown when URI is not heirarchical (i.e., is in a jar)
			}
		}
	}

	public void onSaveLayout() {
		// get a perspective name
		final String layoutName = JOptionPane.showInputDialog("Enter layout name:");

		final MessageDialogProperties props = new MessageDialogProperties();
		props.setTitle("Unable to save layout");
		props.setHeader("Unable to save layout");
		props.setParentWindow(CommonModuleFrame.getCurrentFrame());
		props.setOptions(MessageDialogProperties.okOptions);
		if(layoutName == null || layoutName.trim().length() == 0) {
			props.setMessage("You must enter a layout name");
			NativeDialogs.showMessageDialog(props);
			return;
		}
		if(RecordEditorPerspective.getPerspective(layoutName) != null) {
			props.setMessage("Layout named " + layoutName + " already exists");
			NativeDialogs.showMessageDialog(props);
			return;
		}

		final File perspectiveFile = new File(RecordEditorPerspective.PERSPECTIVES_FOLDER, layoutName + ".xml");
		try {
			final RecordEditorPerspective perspective = new RecordEditorPerspective(layoutName, perspectiveFile.toURI().toURL());
			savePerspective(perspective);
		} catch (MalformedURLException e) {
			LogUtil.severe( e.getLocalizedMessage(), e);
		}
	}

	private final WindowListener windowChangeListener = new WindowListener() {

		@Override
		public void windowOpened(WindowEvent e) {
		}

		@Override
		public void windowIconified(WindowEvent e) {
		}

		@Override
		public void windowDeiconified(WindowEvent e) {
		}

		@Override
		public void windowDeactivated(WindowEvent e) {
		}

		@Override
		public void windowClosing(WindowEvent e) {
			savePreviousPerspective();
		}

		@Override
		public void windowClosed(WindowEvent e) {
		}

		@Override
		public void windowActivated(WindowEvent e) {
		}
	};

	private final CDockableLocationListener dockableLocationListener = new CDockableLocationListener() {

		@Override
		public void changed(CDockableLocationEvent event) {
			savePreviousPerspective();
		}

	};

	public CommonModuleFrame createAccessoryWindow(UUID uuid) {
		final AccessoryWindow retVal = new AccessoryWindow(uuid);
		return retVal;
	}

	private List<AccessoryWindow> accessoryWindows = new ArrayList<AccessoryWindow>();

	/**
	 * Accessory window for editor views
	 */
	private class AccessoryWindow extends CommonModuleFrame {

		private CContentArea contentArea;

		private UUID uuid;

		public AccessoryWindow() {
			this(UUID.randomUUID());
		}

		public AccessoryWindow(UUID uuid) {
			super();

			this.uuid = uuid;

			setWindowName("(" + (accessoryWindows.size()+1) + ") " + getEditor().getTitle());

			setShowInWindowMenu(false);
			setParentFrame(getFrameForEditor());

			putExtension(Project.class, getEditor().getProject());
			putExtension(UndoManager.class, getEditor().getUndoManager());

			accessoryWindows.add(this);

			addWindowListener(new WindowListener() {

				@Override
				public void windowOpened(WindowEvent e) {
				}

				@Override
				public void windowIconified(WindowEvent e) {
				}

				@Override
				public void windowDeiconified(WindowEvent e) {
				}

				@Override
				public void windowDeactivated(WindowEvent e) {
				}

				@Override
				public void windowClosing(WindowEvent e) {
					accessoryWindows.remove(AccessoryWindow.this);
					dockControl.removeStationContainer(getArea());
				}

				@Override
				public void windowClosed(WindowEvent e) {
					accessoryWindows.remove(AccessoryWindow.this);
					if(dockControl != null)
						dockControl.removeStationContainer(getArea());
				}

				@Override
				public void windowActivated(WindowEvent e) {

				}
			});
			init();
		}

		@Override
		public void setJMenuBar(JMenuBar menuBar) {
			getEditor().setupMenu(menuBar);
			super.setJMenuBar(menuBar);
		}

		@Override
		public String getTitle() {
			final Session session = getEditor().getSession();
			String retVal = "(" + (accessoryWindows.indexOf(this)+1) + ") Session Editor";
			if(session != null) {
				retVal += " : " + session.getCorpus() + "." + session.getName();
				if(getEditor().isModified())
					retVal += "*";
			}
			return retVal;
		}


		private void init() {
			contentArea = dockControl.createContentArea(uuid.toString());

			add(contentArea, BorderLayout.CENTER);
		}

		public CContentArea getArea() {
			return this.contentArea;
		}

	}

}
