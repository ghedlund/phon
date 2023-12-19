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
package ca.phon.app.session.editor.view.check;

import ca.phon.app.log.*;
import ca.phon.app.session.editor.*;
import ca.phon.app.session.editor.view.check.actions.SessionCheckRefreshAction;
import ca.phon.csv.CSVWriter;
import ca.phon.plugin.*;
import ca.phon.session.Session;
import ca.phon.session.check.*;
import ca.phon.ui.DropDownButton;
import ca.phon.ui.action.PhonUIAction;
import ca.phon.util.PrefHelper;
import ca.phon.util.icons.*;
import org.jdesktop.swingx.JXBusyLabel;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class SessionCheckView extends EditorView {
	
	public final static String VIEW_NAME = "Session Check";
	
	public final static String ICON_NAME = IconManager.GoogleMaterialDesignIconsFontName + ":error";

	private DropDownButton settingsButton;

	private JXBusyLabel busyLabel;
	private JButton refreshButton;

	private BufferPanel bufferPanel;

	public SessionCheckView(SessionEditor editor) {
		super(editor);

		init();
		refresh();

		setupEditorActions();
	}

	private void setupEditorActions() {
		getEditor().getEventManager().registerActionForEvent(EditorEventType.SessionChanged, this::onSessionChanged, EditorEventManager.RunOn.AWTEventDispatchThread);
	}

	private void onSessionChanged(EditorEvent<Session> ee) {
		refresh();
	}

	private boolean doCheck(SessionCheck check) {
		String checkProp = check.getClass().getName() + ".checkByDefault";
		boolean doCheck = PrefHelper.getBoolean(checkProp, check.performCheckByDefault());
		return doCheck;
	}

	private void setDoCheck(SessionCheck check, boolean doCheck) {
		String checkProp = check.getClass().getName() + ".checkByDefault";
		PrefHelper.getUserPreferences().putBoolean(checkProp, doCheck);
	}
	
	private void init() {
		setLayout(new BorderLayout());
		
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);

		final JPopupMenu settingsMenu = new JPopupMenu();
		settingsMenu.addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				settingsMenu.removeAll();

				for(IPluginExtensionPoint<SessionCheck> extPt:PluginManager.getInstance().getExtensionPoints(SessionCheck.class)) {
					SessionCheck check = extPt.getFactory().createObject();
					PhonPlugin pluginInfo = check.getClass().getAnnotation(PhonPlugin.class);
					if(pluginInfo != null) {
						JCheckBoxMenuItem item = new JCheckBoxMenuItem(pluginInfo.name());
						item.setSelected(doCheck(check));
						item.addActionListener( (evt) -> {
							setDoCheck(check, item.isSelected());
							refresh();
						});
						settingsMenu.add(item);
					}
				}
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {

			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {

			}

		});

		final PhonUIAction<Void> dropDownAct = PhonUIAction.runnable(() -> {});
		dropDownAct.putValue(PhonUIAction.SMALL_ICON, IconManager.getInstance().buildFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "SETTINGS", IconSize.SMALL, Color.darkGray));
		dropDownAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Select session checks...");
		dropDownAct.putValue(DropDownButton.BUTTON_POPUP, settingsMenu);
		dropDownAct.putValue(DropDownButton.ARROW_ICON_GAP, 0);
		dropDownAct.putValue(DropDownButton.ARROW_ICON_POSITION, SwingConstants.BOTTOM);

		settingsButton = new DropDownButton(dropDownAct);
		settingsButton.setOnlyPopup(true);
		toolBar.add(settingsButton);

		toolBar.addSeparator();

		busyLabel = new JXBusyLabel(new Dimension(16, 16));
		toolBar.add(busyLabel);

		refreshButton = new JButton(new SessionCheckRefreshAction(this));
		toolBar.add(refreshButton);
		
		bufferPanel = new BufferPanel(VIEW_NAME);
		
		add(toolBar, BorderLayout.NORTH);
		add(bufferPanel, BorderLayout.CENTER);
	}

	public void refresh() {
		bufferPanel.clear();
		busyLabel.setBusy(true);
		
		SessionCheckWorker worker = new SessionCheckWorker();
		worker.execute();
	}
	
	@Override
	public String getName() {
		return VIEW_NAME;
	}

	@Override
	public ImageIcon getIcon() {
		return IconManager.getInstance().buildFontIcon(ICON_NAME, IconSize.MEDIUM, Color.darkGray);
	}

	@Override
	public JMenu getMenu() {
		JMenu retVal = new JMenu();
		
		retVal.add(new SessionCheckRefreshAction(this));
		
		return retVal;
	}
	
	public void setupStatusBar(List<ValidationEvent> events) {
		final SessionEditorStatusBar statusBar = getEditor().getStatusBar();
		
		JLabel lbl = null;
		for(Component comp:statusBar.getExtrasPanel().getComponents()) {
			if(comp.getName() != null && comp.getName().equals(VIEW_NAME)) {
				lbl = (JLabel)comp;
				break;
			}
		}
		
		if(lbl == null) {
			lbl = new JLabel();
			lbl.setName(VIEW_NAME);
			final ImageIcon icn = IconManager.getInstance().buildFontIcon(ICON_NAME, IconSize.XSMALL, Color.darkGray);
			lbl.setIcon(icn);
			if(events.size() > 0) {
				lbl.setText("<html><u>" +
						events.size() + " warning" + 
						(events.size() == 1 ? "" : "s") + "</u></html>");
				lbl.setToolTipText("Show warnings");
				lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				lbl.addMouseListener(new MouseInputAdapter() {
	
					@Override
					public void mouseClicked(MouseEvent e) {
						getEditor().getViewModel().showView(VIEW_NAME);
					}
	
				});
				statusBar.getExtrasPanel().add(lbl);
			}
		} else {
			if(events.size() == 0)
				statusBar.getExtrasPanel().remove(lbl);
			else {
				lbl.setText("<html><u>" +
						events.size() + " warning" + 
						(events.size() == 1 ? "" : "s") + "</u></html>");
			}
		}
		statusBar.revalidate();
	}
	
	private class SessionCheckWorker extends SwingWorker<List<ValidationEvent>, ValidationEvent> {

		private OutputStreamWriter out;
		private CSVWriter writer;
		
		final String[] cols = new String[] { "Session", "Record #", "Tier", "Message" };
		
		public SessionCheckWorker() {
			try {
				out = new OutputStreamWriter(bufferPanel.getLogBuffer().getStdOutStream(), StandardCharsets.UTF_8);
				
				out.flush();
				out.write(LogBuffer.ESCAPE_CODE_PREFIX + BufferPanel.SHOW_BUSY);
				out.flush();

				writer = new CSVWriter(out);
				writer.writeNext(cols);
				writer.flush();
			} catch (IOException e) {
				LogUtil.warning(e);
			}
		}
		
		@Override
		protected List<ValidationEvent> doInBackground() throws Exception {
			List<ValidationEvent> events = new ArrayList<>();

			List<SessionCheck> checks = new ArrayList<>();
			for(IPluginExtensionPoint<SessionCheck> extPt:PluginManager.getInstance().getExtensionPoints(SessionCheck.class)) {
				SessionCheck check = extPt.getFactory().createObject();
				if(doCheck(check))
					checks.add(check);
			}

			SessionValidator validator = new SessionValidator(checks);
			validator.putExtension(SessionEditor.class, getEditor());
			validator.addValidationListener( (e) -> {
				events.add(e);
				publish(e);
			});
					
			validator.validate(getEditor().getSession());
			
			return events;
		}

		@Override
		protected void process(List<ValidationEvent> chunks) {
			String[] row = new String[cols.length];
			for(var ve:chunks) {
				row[0] = ve.getSession().getCorpus() + "." + ve.getSession().getName();
				row[1] = "" + (ve.getRecord()+1);
				row[3] = ve.getTierName();
				row[4] = ve.getMessage();

				try {
					writer.writeNext(row);
					writer.flush();
				} catch (IOException e) {
					LogUtil.warning(e);
				}
			}
		}

		@Override
		protected void done() {
			try {
				out.flush();
				out.write(LogBuffer.ESCAPE_CODE_PREFIX + BufferPanel.STOP_BUSY);
				out.flush();
				out.write(LogBuffer.ESCAPE_CODE_PREFIX + BufferPanel.SHOW_TABLE_CODE);
				out.flush();
				out.write(LogBuffer.ESCAPE_CODE_PREFIX + BufferPanel.PACK_TABLE_COLUMNS);
				out.flush();
			} catch (IOException e) {
				LogUtil.severe( e.getLocalizedMessage(), e);
			} finally {
				try {
					writer.close(); 
					out.close();
				} catch (IOException e) {
					LogUtil.warning( e.getLocalizedMessage(), e);
				}
			}
			
			try {
				setupStatusBar(get());
			} catch (InterruptedException | ExecutionException e) {
				LogUtil.warning(e);
			}

			SwingUtilities.invokeLater( () -> {
				bufferPanel.getDataTable().packAll();
			});

			busyLabel.setBusy(false);
		}
		
	}

}
