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
import ca.phon.app.session.editor.view.transcript.TranscriptEditor;
import ca.phon.app.session.editor.view.transcript.TranscriptView;
import ca.phon.plugin.*;
import ca.phon.session.Session;
import ca.phon.session.Transcript;
import ca.phon.session.check.*;
import ca.phon.session.position.TranscriptElementLocation;
import ca.phon.ui.FlatButton;
import ca.phon.ui.IconStrip;
import ca.phon.ui.action.PhonUIAction;
import ca.phon.util.icons.*;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class SessionCheckView extends EditorView {
	
	public final static String VIEW_NAME = "Session Check";
	
	public final static String ICON_NAME = IconManager.GoogleMaterialDesignIconsFontName + ":error";

	private SessionCheckTableModel tableModel;
	private JXTable sessionCheckTable;

	private FlatButton toggleWarningsBtn;
	private FlatButton toggleErrorsBtn;
	private FlatButton toggleCurrentElementBtn;

	public SessionCheckView(SessionEditor editor) {
		super(editor);

		init();

		setupEditorActions();
	}

	private void setupEditorActions() {
		getEditor().getEventManager().registerActionForEvent(TranscriptEditor.transcriptDocumentPopulated, this::onSessionFinishedLoading, EditorEventManager.RunOn.AWTEventDispatchThread);
		getEditor().getEventManager().registerActionForEvent(EditorEventType.TierChange, this::onTierChange, EditorEventManager.RunOn.AWTEventDispatchThread);
		getEditor().getEventManager().registerActionForEvent(EditorEventType.CommentChanged, this::onCommentChanged, EditorEventManager.RunOn.AWTEventDispatchThread);
		getEditor().getEventManager().registerActionForEvent(EditorEventType.GemChanged, this::onGemChanged, EditorEventManager.RunOn.AWTEventDispatchThread);

		// TODO update for header information (e.g, media, date, etc.)
	}

	private void onSessionFinishedLoading(EditorEvent<Void> ee) {
		refresh();
	}

	private void onSessionChanged(EditorEvent<Session> ee) {
		refresh();
	}

	private void onTierChange(EditorEvent<EditorEventType.TierChangeData> ee) {
		EditorEventType.TierChangeData data = ee.getData().orElse(null);
		if(data == null) return;
		if(data.valueAdjusting()) return;
		final int elementIndex = getEditor().getSession().getRecordElementIndex(data.record());
		if(elementIndex >= 0) {
			tableModel.removeEventsForElement(elementIndex);
		}
		SessionCheckWorker worker = new SessionCheckWorker(elementIndex);
		worker.execute();
	}

	private void onCommentChanged(EditorEvent<EditorEventType.CommentChangedData> ee) {
		EditorEventType.CommentChangedData data = ee.getData().orElse(null);
		if(data == null) return;
		final int elementIndex = getEditor().getSession().getTranscript().getElementIndex(data.comment());
		if(elementIndex >= 0) {
			tableModel.removeEventsForElement(elementIndex);
		}
		SessionCheckWorker worker = new SessionCheckWorker(elementIndex);
		worker.execute();
	}

	private void onGemChanged(EditorEvent<EditorEventType.GemChangedData> ee) {
		EditorEventType.GemChangedData data = ee.getData().orElse(null);
		if(data == null) return;
		final int elementIndex = getEditor().getSession().getTranscript().getElementIndex(data.gem());
		if(elementIndex >= 0) {
			tableModel.removeEventsForElement(elementIndex);
		}
		SessionCheckWorker worker = new SessionCheckWorker(elementIndex);
		worker.execute();
	}

	private void init() {
		setLayout(new BorderLayout());

		final IconStrip iconStrip = new IconStrip(SwingConstants.VERTICAL);

		final PhonUIAction refreshAct = PhonUIAction.runnable(this::refresh);
		refreshAct.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
		refreshAct.putValue(FlatButton.ICON_NAME_PROP, "refresh");
		refreshAct.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);
		final FlatButton refreshBtn = new FlatButton(refreshAct);
		refreshBtn.setFocusable(false);
		iconStrip.add(refreshBtn, IconStrip.IconStripPosition.LEFT);

		final PhonUIAction toggleWarningsAct = PhonUIAction.runnable(() -> {
			toggleWarningsBtn.setSelected(!toggleWarningsBtn.isSelected());
			sessionCheckTable.setRowFilter(new SessionCheckRowFilter(toggleWarningsBtn.isSelected(),
					toggleErrorsBtn.isSelected(), -1));
		});
		toggleWarningsAct.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
		toggleWarningsAct.putValue(FlatButton.ICON_NAME_PROP, "warning");
		toggleWarningsAct.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);
		toggleWarningsBtn = new FlatButton(toggleWarningsAct);
		toggleWarningsBtn.setFocusable(false);
		toggleWarningsBtn.setIconColor(UIManager.getColor("textInactiveText"));
		toggleWarningsBtn.setIconSelectedColor(UIManager.getColor("Phon.darkBlue"));
		toggleWarningsBtn.setSelected(true);
		toggleWarningsBtn.setToolTipText("Toggle warnings");
		iconStrip.add(toggleWarningsBtn, IconStrip.IconStripPosition.LEFT);

		final PhonUIAction toggleErrorsAct = PhonUIAction.runnable(() -> {
			toggleErrorsBtn.setSelected(!toggleErrorsBtn.isSelected());
			sessionCheckTable.setRowFilter(new SessionCheckRowFilter(toggleWarningsBtn.isSelected(),
					toggleErrorsBtn.isSelected(), -1));
		});
		toggleErrorsAct.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
		toggleErrorsAct.putValue(FlatButton.ICON_NAME_PROP, "error");
		toggleErrorsAct.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);
		toggleErrorsBtn = new FlatButton(toggleErrorsAct);
		toggleErrorsBtn.setFocusable(false);
		toggleErrorsBtn.setIconColor(UIManager.getColor("textInactiveText"));
		toggleErrorsBtn.setIconSelectedColor(UIManager.getColor("Phon.darkBlue"));
		toggleErrorsBtn.setSelected(true);
		toggleErrorsBtn.setToolTipText("Toggle errors");
		iconStrip.add(toggleErrorsBtn, IconStrip.IconStripPosition.LEFT);



		tableModel = new SessionCheckTableModel();
		sessionCheckTable = new JXTable(tableModel);
		sessionCheckTable.setRowFilter(new SessionCheckRowFilter(true, true, -1));
		sessionCheckTable.setDefaultRenderer(ValidationEvent.Severity.class, new SessionCheckSeverityRenderer());
		sessionCheckTable.getColumn(0).setMaxWidth(30);
		sessionCheckTable.getColumn(1).setMaxWidth(50);
		sessionCheckTable.getColumn(2).setPreferredWidth(150);
		sessionCheckTable.getColumn(2).setMaxWidth(300);
		sessionCheckTable.addMouseListener(new MouseInputAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getClickCount() == 2) {
					int row = sessionCheckTable.rowAtPoint(e.getPoint());
					if(row >= 0) {
						ValidationEvent ve = tableModel.events.get(row);
						TranscriptView tv = (TranscriptView) getEditor().getViewModel().getView(TranscriptView.VIEW_NAME);
						if(ve.getElementIndex() >= -1) {
							final TranscriptElementLocation loc = new TranscriptElementLocation(ve.getElementIndex(), ve.getTierName(), 0);

							int elementIndex = ve.getElementIndex();
							while(elementIndex < getEditor().getDataModel().getSession().getTranscript().getNumberOfElements()
									&& !getEditor().getDataModel().getSession().getTranscript().getElementAt(elementIndex).isRecord()) {
								elementIndex++;
							}
							int recordIndex = -1;
							if(elementIndex < getEditor().getDataModel().getSession().getTranscript().getNumberOfElements()) {
								recordIndex = getEditor().getDataModel().getSession().getTranscript().getRecordIndex(elementIndex);
							}

							if(ve.getElementIndex() >= 0 && tv.isSingleRecordView()) {
								getEditor().getEventManager().registerActionForEvent(TranscriptEditor.recordChangedInSingleRecordMode,
										new EditorAction<Void>() {
											@Override
											public void eventOccurred(EditorEvent<Void> ee) {
												int charPos = tv.getTranscriptEditor().sessionLocationToCharPos(loc);
												if (charPos >= 0) {
													tv.getTranscriptEditor().setCaretPosition(charPos);
													tv.getTranscriptEditor().requestFocus();
												}

												getEditor().getEventManager().removeActionForEvent(TranscriptEditor.recordChangedInSingleRecordMode, this);
											}
										}, EditorEventManager.RunOn.AWTEventDispatchThread);

								getEditor().getSelectionModel().requestSwitchToRecord(recordIndex);
							} else {
								int charPos = tv.getTranscriptEditor().sessionLocationToCharPos(loc);
								if (charPos >= 0) {
									tv.getTranscriptEditor().setCaretPosition(charPos);
									tv.getTranscriptEditor().requestFocus();
								}
							}
						}
					}
				}
			}
		});

		add(iconStrip, BorderLayout.WEST);
		add(new JScrollPane(sessionCheckTable), BorderLayout.CENTER);
	}

	public void refresh() {
		if(tableModel.events.size() > 0)
			tableModel.reset();
		SessionCheckWorker worker = new SessionCheckWorker();
		worker.execute();
	}
	
	@Override
	public String getName() {
		return VIEW_NAME;
	}

	@Override
	public ImageIcon getIcon() {
		return IconManager.getInstance().getFontIcon(ICON_NAME, IconSize.MEDIUM, Color.darkGray);
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
			final ImageIcon icn = IconManager.getInstance().getFontIcon(ICON_NAME, IconSize.SMALL, Color.darkGray);
			lbl.setIcon(icn);
			statusBar.getExtrasPanel().add(lbl);

			lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			lbl.addMouseListener(new MouseInputAdapter() {

				@Override
				public void mouseClicked(MouseEvent e) {
					getEditor().getViewModel().showView(VIEW_NAME);
				}

			});
		}

		final StringBuilder html = new StringBuilder();
		if(events.size() > 0) {
			int numErrors = (int)events.stream().filter( (e) -> e.getSeverity() == ValidationEvent.Severity.ERROR).count();
			int numWarnings = (int)events.stream().filter( (e) -> e.getSeverity() == ValidationEvent.Severity.WARNING).count();

			html.append("<html>");
			if(numErrors > 0) {
				html.append("<font color='red'>");
				html.append(numErrors);
				html.append(" error");
				if(numErrors > 1) html.append("s");
				html.append("</font>");
			}
			if(numWarnings > 0) {
				if(numErrors > 0) html.append(", ");
				html.append("<font color='orange'>");
				html.append(numWarnings);
				html.append(" warning");
				if(numWarnings > 1) html.append("s");
				html.append("</font>");
			}
			html.append("</html>");
			lbl.setText(html.toString());
			lbl.setToolTipText("Show warnings");


		} else {
			lbl.setText("");
			lbl.setToolTipText("No warnings");
		}
		statusBar.revalidate();
	}
	
	private class SessionCheckWorker extends SwingWorker<List<ValidationEvent>, ValidationEvent> {

		private final int elementIndex;

		public SessionCheckWorker() {
			this(-1);
		}

		public SessionCheckWorker(int elementIndex) {
			super();
			this.elementIndex = elementIndex;
		}
		
		@Override
		protected List<ValidationEvent> doInBackground() throws Exception {
			List<ValidationEvent> events = new ArrayList<>();

			List<SessionCheck> checks = new ArrayList<>();
			for(IPluginExtensionPoint<SessionCheck> extPt:PluginManager.getInstance().getExtensionPoints(SessionCheck.class)) {
				SessionCheck check = extPt.getFactory().createObject();
				checks.add(check);
			}

			SessionValidator validator = new SessionValidator(checks);
			validator.putExtension(SessionEditor.class, getEditor());
			validator.addValidationListener( (e) -> {
				events.add(e);
				publish(e);
			});

			if(elementIndex >= 0) {
				validator.validate(getEditor().getSession(), elementIndex);
			} else {
				validator.validate(getEditor().getSession());
			}
			
			return events;
		}

		@Override
		protected void process(List<ValidationEvent> chunks) {
			tableModel.addEvents(chunks);
		}

		@Override
		protected void done() {
			setupStatusBar(tableModel.events);
		}
		
	}

	private class SessionCheckSeverityRenderer extends DefaultTableCellRenderer {

		@Override
		protected void setValue(Object value) {
			if(value instanceof ValidationEvent.Severity ves) {
				switch (ves) {
					case ERROR:
						setIcon(IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName,
								"error", IconSize.MEDIUM, Color.red));
						break;

					case WARNING:
						setIcon(IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName,
								"warning", IconSize.MEDIUM, Color.orange));
						break;

					default:
						setIcon(null);
						break;
				}
			} else {
				super.setValue(value);
			}
		}

	}

	private class SessionCheckTableModel extends DefaultTableModel {

		private List<ValidationEvent> events = new ArrayList<>();

		public SessionCheckTableModel() {
			super(new String[] { "", "#", "Tier", "Message" }, 0);
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			return false;
		}

		@Override
		public int getRowCount() {
			if(this.events == null) return 0; // on super initialization
			return events.size();
		}

		@Override
		public Object getValueAt(int row, int col) {
			Object retVal = null;

			ValidationEvent ve = events.get(row);
			switch(col) {
			case 0:
				retVal = ve.getSeverity();
				break;

			case 1:
				int elementIndex = ve.getElementIndex();
				Transcript.Element element = getEditor().getSession().getTranscript().getElementAt(elementIndex);
				if(element.isRecord()) {
					retVal = getEditor().getSession().getTranscript().getRecordIndex(elementIndex)+1;
				} else {
					retVal = -1;
				}
				break;

			case 2:
				retVal = ve.getTierName();
				break;

			case 3:
				retVal = ve.getMessage();
				break;
			}

			return retVal;
		}

		public Class<?> getColumnClass(int col) {
			Class<?> retVal = Object.class;

			switch(col) {
			case 0:
				retVal = ValidationEvent.Severity.class;
				break;

			case 1:
				retVal = Integer.class;
				break;
			}

			return retVal;
		}

		public void addEvents(List<ValidationEvent> events) {
			events.stream().forEach(this::insertEvent);
		}

		public void reset() {
			this.events.clear();
			fireTableDataChanged();
		}

		public void insertEvent(ValidationEvent ve) {
			// insert event at correct position in list based on element index
			int idx = 0;
			for(; idx < events.size(); idx++) {
				if(events.get(idx).getElementIndex() > ve.getElementIndex()) {
					break;
				}
			}
			events.add(idx, ve);
			fireTableRowsInserted(idx, idx);
		}

		public void removeEventsForElement(int elementIdx) {
			for(int i = 0; i < events.size(); i++) {
				if(events.get(i).getElementIndex() == elementIdx) {
					events.remove(i);
					fireTableRowsDeleted(i, i);
					i--;
				}
			}
		}

	}

	private class SessionCheckRowFilter extends RowFilter<DefaultTableModel, Integer> {
		private final boolean includeWarnings;

		private final boolean includeErrors;

		private final int elementIndex;

		public SessionCheckRowFilter(boolean includeWarnings, boolean includeErrors, int elementIndex) {
			this.includeWarnings = includeWarnings;
			this.includeErrors = includeErrors;
			this.elementIndex = elementIndex;
		}

		@Override
		public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
			boolean retVal = false;

			final DefaultTableModel model = entry.getModel();
			if(model != tableModel) return retVal;
			final int row = entry.getIdentifier();

			final ValidationEvent validationEvent = (ValidationEvent) tableModel.events.get(row);
			if(validationEvent.getSeverity() == ValidationEvent.Severity.ERROR && includeErrors) {
				retVal = true;
			} else if(validationEvent.getSeverity() == ValidationEvent.Severity.WARNING && includeWarnings) {
				retVal = true;
			}
			if(elementIndex >= 0) {
				if(validationEvent.getElementIndex() != elementIndex) {
					retVal = false;
				}
			}

			return retVal;
		}
	}
}
