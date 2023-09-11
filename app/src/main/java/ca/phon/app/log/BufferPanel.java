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
package ca.phon.app.log;

import ca.phon.app.JCefHelper;
import ca.phon.app.excel.WorkbookUtils;
import ca.phon.app.modules.EntryPointArgs;
import ca.phon.app.session.editor.*;
import ca.phon.csv.CSVReader;
import ca.phon.extensions.*;
import ca.phon.plugin.*;
import ca.phon.project.Project;
import ca.phon.query.db.*;
import ca.phon.query.report.csv.CSVTableDataWriter;
import ca.phon.query.report.datasource.DefaultTableDataSource;
import ca.phon.session.*;
import ca.phon.ui.CommonModuleFrame;
import ca.phon.ui.action.PhonUIAction;
import ca.phon.ui.fonts.FontPreferences;
import ca.phon.util.*;
import jxl.Workbook;
import jxl.write.*;
import jxl.write.biff.RowsExceededException;
import org.cef.CefClient;
import org.cef.browser.*;
import org.cef.handler.*;
import org.cef.network.CefRequest;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

/**
 * A panel showing a single {@link LogBuffer} with options
 * for displaying the data in different formats.  CSV data
 * may be displayed in a table, while HTML data may be
 * rendered inside a {@link JEditorPane}.
 *
 */
public class BufferPanel extends JPanel implements IExtendable {

	private final static String TABLE_VIEW_ID = "table";

	public final static String SHOW_TABLE_CODE = "SHOW_TABLE";

	public final static String PACK_TABLE_COLUMNS = "PACK_COLUMNS";

	private final static String BUFFER_VIEW_ID = "buffer";

	public final static String SHOW_BUFFER_CODE = "SHOW_BUFFER";

	private final static String HTML_VIEW_ID = "html";

	public final static String SHOW_HTML_CODE = "SHOW_HTML";

	public final static String SHOW_BUSY = "SHOW_BUSY";

	public final static String STOP_BUSY = "STOP_BUSY";

	private final static double MAX_ZOOM_LEVEL = 4.0;

	private final static double MIN_ZOOM_LEVEL = -1.0;

	private final static double ZOOM_LEVEL_INCR = 0.5;

	private final static double DEFAULT_ZOOM_LEVEL = 0.0;

	private final ExtensionSupport extSupport = new ExtensionSupport(BufferPanel.class, this);

	private final String name;

	private JPanel contentPanel;
	private CardLayout cardLayout;

	private LogBuffer logBuffer;

	/* Views */
	private JXTable dataTable;

	private CefClient cefClient;

	private CefBrowser browser;

	private CefBrowser debugBrowser;

	private List<CefLoadHandler> cefLoadHandlers = Collections.synchronizedList(new ArrayList<>());

	private Component htmlView;
	
	private JPanel htmlPanel;
	private JSplitPane htmlSplitPane;
	private JButton zoomInBtn;
	private JButton zoomOutBtn;
	private JButton zoomResetBtn;
	
	public final static String SHOWING_BUFFER_PROP = BufferPanel.class.getName() + ".showingBuffer";

	private JComponent currentView;

	private WeakReference<Object> userObjectRef = new WeakReference<Object>(null);

	public BufferPanel(String name) {
		super();

		this.name = name;

		init();
		extSupport.initExtensions();
	}

	private LogBuffer createLogBuffer() {
		LogBuffer retVal = new LogBuffer(getName());
		retVal.addEscapeCodeHandler(new LogEscapeCodeHandler() {

			@Override
			public void handleEscapeCode(String code) {
				if(SHOW_TABLE_CODE.equals(code)) {
					SwingUtilities.invokeLater(BufferPanel.this::showTable);
				} else if(SHOW_BUFFER_CODE.equals(code)) {
					SwingUtilities.invokeLater(BufferPanel.this::showBuffer);
				} else if(SHOW_HTML_CODE.equals(code))  {
					SwingUtilities.invokeLater(BufferPanel.this::showHtml);
				} else if(PACK_TABLE_COLUMNS.equals(code) && !isShowingBuffer()) {
					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {
							getDataTable().packAll();
						}
					});
				} else if(SHOW_BUSY.equals(code)) {
					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {
							setBusy(true);
						}
					});
				} else if(STOP_BUSY.equals(code)) {
					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {
							setBusy(false);
						}
					});
				}
			}

		});
		return retVal;
	}
	
	public String getName() {
		return this.name;
	}

	public Object getUserObject() {
		return userObjectRef.get();
	}

	public void setUserObject(Object obj) {
		userObjectRef = new WeakReference<Object>(obj);
	}

	public boolean isShowingBuffer() {
		return currentView != null && currentView == logBuffer;
	}

	public void showBuffer() {
		JComponent oldComp = currentView;

		if(logBuffer == null) {
			logBuffer = createLogBuffer();
		}

		if(logBuffer.getText().length() == 0 && htmlView != null) {
			final String docLocation = browser.getURL();
			if(docLocation != null && docLocation.trim().length() > 0) {
				logBuffer.setText(getHTML());
			}
		}

		cardLayout.show(contentPanel, BUFFER_VIEW_ID);
		currentView = logBuffer;

		logBuffer.scrollRectToVisible(new Rectangle(0, 0, 0, 0));

		firePropertyChange(SHOWING_BUFFER_PROP, oldComp, currentView);
	}

	public void clear() {
		getLogBuffer().setText("");
		showBuffer();
	}

	private JXTable createTable() {
		final JXTable retVal = new JXTable();

		retVal.setColumnControlVisible(true);
		retVal.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		retVal.setFont(FontPreferences.getTierFont());

		final ActionMap am = retVal.getActionMap();
		final InputMap im = retVal.getInputMap(JComponent.WHEN_FOCUSED);

		final String deleteRowsKey = "__delete_rows__";
		final PhonUIAction<Void> deleteRowsAct = PhonUIAction.runnable(this::deleteSelectedRows);
		am.put(deleteRowsKey, deleteRowsAct);

		final KeyStroke delKs = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
		im.put(delKs, deleteRowsKey);

		final String defaultActKey = "__default_act__";
		final TableAction defaultAct = new TableAction(tableAct);
		am.put(defaultActKey, defaultAct);

		final KeyStroke defKs = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
		im.put(defKs, defaultActKey);

		retVal.setActionMap(am);
		retVal.setInputMap(JComponent.WHEN_FOCUSED, im);
		retVal.addMouseListener(new TableMouseAdapter(tableAct));

		return retVal;
	}

	public boolean isShowingTable() {
		return currentView != null && currentView == dataTable;
	}

	public void showTable() {
		JComponent oldComp = currentView;

		if(dataTable == null) {
			dataTable = createTable();
			final JScrollPane tableScroller = new JScrollPane(dataTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			contentPanel.add(tableScroller, TABLE_VIEW_ID);
		}
		final CSVReader reader = new CSVReader(new StringReader(logBuffer.getText()));
		final CSVTableModel model = new CSVTableModel(reader);
		model.setUseFirstRowAsHeader(true);
		dataTable.setModel(model);

		dataTable.scrollCellToVisible(0, 0);

		cardLayout.show(contentPanel, TABLE_VIEW_ID);
		currentView = dataTable;

		firePropertyChange(SHOWING_BUFFER_PROP, oldComp, currentView);
	}

	public void copyTextToClipboard(String text) {
		final StringSelection stringSelection = new StringSelection(text);
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(stringSelection, stringSelection);
	}

	public boolean isShowingHtml() {
		return currentView != null && currentView == htmlPanel;
	}

	public void showHtml() {
		showHtml(true);
	}
	
	public void showHtml(boolean loadTextContent) {
		JComponent oldComp = currentView;
		
		if(htmlView == null) {
			Component view = getWebView();
			htmlPanel = new JPanel(new BorderLayout());
			htmlPanel.add(view, BorderLayout.CENTER);
			contentPanel.add(htmlPanel, HTML_VIEW_ID);
		}
		
		if(loadTextContent) {
			addBrowserLoadHandler(new CefLoadHandlerAdapter() {
	            @Override
	            public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
					if(!isLoading) {
						try {
							final File tempFile = File.createTempFile("phon", ".html");
							tempFile.deleteOnExit();

							final PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(tempFile), "UTF-8"));
							writer.write(logBuffer.getText());
							writer.flush();
							writer.close();

							SwingUtilities.invokeLater(() -> {
								removeBrowserLoadHandler(this);
								browser.loadURL(tempFile.toURI().toString());
							});
						} catch (IOException e) {
							LogUtil.warning(e);
						}
					}
	            }
	        });
		}
		
		currentView = htmlPanel;
		cardLayout.show(contentPanel, HTML_VIEW_ID);
		firePropertyChange(SHOWING_BUFFER_PROP, oldComp, currentView);
	}
	
	public void onZoomIn() {
		if(!isShowingHtml() || browser == null) return;
		double zoomLevel = browser.getZoomLevel();
		if(zoomLevel < MAX_ZOOM_LEVEL) {
			zoomLevel += ZOOM_LEVEL_INCR;
		}
		browser.setZoomLevel(zoomLevel);
	}
	
	public void onZoomOut() {
		if(!isShowingHtml() || browser == null) return;
		double zoomLevel = browser.getZoomLevel();
		if(zoomLevel > MIN_ZOOM_LEVEL) {
			zoomLevel -= ZOOM_LEVEL_INCR;
		}
		browser.setZoomLevel(zoomLevel);
	}
	
	public void onZoomReset() {
		if(!isShowingHtml()) return;
		browser.setZoomLevel(DEFAULT_ZOOM_LEVEL);
	}

	public void addBrowserLoadHandler(CefLoadHandler loadHandler) {
		cefLoadHandlers.add(loadHandler);
	}

	public void removeBrowserLoadHandler(CefLoadHandler loadHandler) {
		cefLoadHandlers.remove(loadHandler);
	}

	public boolean isShowingHtmlDebug() {
		return isShowingHtml() && htmlSplitPane != null && htmlPanel.getComponent(0) == htmlSplitPane;
	}
	
	public void hideHtmlDebug() {
		if(!isShowingHtmlDebug()) return;
		
		htmlPanel.removeAll();
		htmlPanel.add(getWebView(), BorderLayout.CENTER);
		htmlPanel.revalidate();
	}
	
	public void showHtmlDebug() {
		if(browser != null) {
			debugBrowser = browser.getDevTools();

			htmlSplitPane = new JSplitPane();
			htmlSplitPane.setLeftComponent(getWebView());
			htmlSplitPane.setRightComponent(debugBrowser.getUIComponent());
			htmlSplitPane.setResizeWeight(1.0);

			htmlPanel.removeAll();
			htmlPanel.add(htmlSplitPane, BorderLayout.CENTER);
			htmlPanel.revalidate();

			SwingUtilities.invokeLater( () -> htmlSplitPane.setDividerLocation(0.6) );
		}
	}

	private void init() {
		setLayout(new BorderLayout());

		cardLayout = new CardLayout();
		contentPanel = new JPanel(cardLayout);

		logBuffer = createLogBuffer();
		final JScrollPane logScroller = new RTextScrollPane(logBuffer, true);
		contentPanel.add(logScroller, BUFFER_VIEW_ID);

		currentView = logBuffer;
		add(contentPanel, BorderLayout.CENTER);
		
		ActionMap am = getActionMap();
		InputMap im = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		
		final PhonUIAction<Void> zoomInAct = PhonUIAction.runnable(this::onZoomIn);
		zoomInAct.setRunInBackground(true);
		am.put("zoomIn", zoomInAct);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_0, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "zoomIn");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "zoomIn");
		
		final PhonUIAction<Void> zoomOutAct = PhonUIAction.runnable(this::onZoomOut);
		zoomOutAct.setRunInBackground(true);
		am.put("zoomOut", zoomOutAct);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_9, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "zoomOut");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "zoomOut");
	}

	public String getBufferName() {
		return logBuffer.getBufferName();
	}

	public LogBuffer getLogBuffer() {
		return logBuffer;
	}

	public CefBrowser getBrowser() {
		if(browser == null) {
			final Tuple<CefClient, CefBrowser> clientAndBrowser = createBrowser();
			this.cefClient = clientAndBrowser.getObj1();
			this.browser = clientAndBrowser.getObj2();

			final JFrame parentFrame = (JFrame)SwingUtilities.getAncestorOfClass(JFrame.class, this);
			if(parentFrame != null) {
				parentFrame.addWindowListener(new WindowAdapter() {
					@Override
					public void windowClosed(WindowEvent e) {
						LogUtil.info("Disposing browser");
						if(browser != null) {
							browser.getClient().removeLoadHandler();
							if(debugBrowser != null)
								debugBrowser.close(true);
							browser.close(true);
							cefClient.dispose();
						}
					}
				});
			}
		}
		return browser;
	}
	
	private Tuple<CefClient, CefBrowser> createBrowser() {
		final Tuple<CefClient, CefBrowser> clientAndBrowser = JCefHelper.getInstance().createClientAndBrowser();

		final CefClient client = clientAndBrowser.getObj1();
		client.addLoadHandler(new CefLoadHandler() {
			@Override
			public void onLoadingStateChange(CefBrowser cefBrowser, boolean b, boolean b1, boolean b2) {
				for(CefLoadHandler loadHandler:cefLoadHandlers) {
					loadHandler.onLoadingStateChange(cefBrowser, b, b1, b2);
				}
			}

			@Override
			public void onLoadStart(CefBrowser cefBrowser, CefFrame cefFrame, CefRequest.TransitionType transitionType) {
				for(CefLoadHandler loadHandler:cefLoadHandlers) {
					loadHandler.onLoadStart(cefBrowser, cefFrame, transitionType);
				}
			}

			@Override
			public void onLoadEnd(CefBrowser cefBrowser, CefFrame cefFrame, int i) {
				for(CefLoadHandler loadHandler:cefLoadHandlers) {
					loadHandler.onLoadEnd(cefBrowser, cefFrame, i);
				}
			}

			@Override
			public void onLoadError(CefBrowser cefBrowser, CefFrame cefFrame, ErrorCode errorCode, String s, String s1) {
				for(CefLoadHandler loadHandler:cefLoadHandlers) {
					loadHandler.onLoadError(cefBrowser, cefFrame, errorCode, s, s1);
				}
			}
		});

		return clientAndBrowser;
	}
	
	public Component getWebView() {
		if(htmlView == null) {
			browser = getBrowser();
			htmlView = browser.getUIComponent();
		}
		return this.htmlView;
	}

	public JXTable getDataTable() {
		return dataTable;
	}

	public void setBusy(boolean busy) {
	}

	public void setFirstRowIsHeader(boolean firstRowIsColumnHeader) {
		final CSVTableModel model = (CSVTableModel)getDataTable().getModel();
		model.setUseFirstRowAsHeader(firstRowIsColumnHeader);
		model.fireTableStructureChanged();
	}

	public void onToggleFirstRowAsHeader() {
		boolean isFirstRowHeader = ((CSVTableModel)getDataTable().getModel()).isUseFirstRowAsHeader();
		setFirstRowIsHeader(!isFirstRowHeader);
	}

	/**
	 * Returns the appropriate extension based on the current
	 * view of the data.
	 *
	 * @return one of 'txt', 'csv', or 'html'
	 */
	public String getDefaultExtension() {
		String retVal = "txt";
		if(isShowingTable()) {
			retVal = "csv";
		} else if(isShowingHtml()) {
			retVal = "html";
		}
		return retVal;
	}

	public void writeToTextFile(String file, String encoding) throws IOException {
		final File f = new File(file);
		final BufferedWriter out =
				new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), encoding));
		out.write(logBuffer.getText());
		out.flush();
		out.close();
	}
	
	public String getHTML() {
		final StringBuffer buffer = new StringBuffer();
		
		final String docLocation = browser.getURL();
		if(docLocation != null && docLocation.trim().length() > 0) {
			try {
				final URL docURL = new URL(docLocation);
				final BufferedReader in = new BufferedReader(new InputStreamReader(docURL.openStream(), "UTF-8"));
				String line = null;
				while((line = in.readLine()) != null) {
					buffer.append(line).append("\n");
				}
				in.close();
			} catch (IOException e) {
				LogUtil.warning(e);
			}
		} else {
			buffer.append(logBuffer.getText());
		}
		
		return buffer.toString();
	}
	
	public void writeHMTLFile(String file, String encoding) throws IOException {
		final File f = new File(file);
		final BufferedWriter out =
				new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), encoding));
		out.write(getHTML());
		out.flush();
		out.close();
	}
	
	public void writeToCSV(String file, String encoding) throws IOException {
		final CSVTableDataWriter writer = new CSVTableDataWriter(encoding);
		writer.writeTableToFile(dataTable, new File(file));
	}
	
	public void writeToExcelWorkbook(String file) throws IOException {
		// create a new workbook at the file
		final WritableWorkbook workbook = Workbook.createWorkbook(new File(file));
		try {
			createSheetInExcelWorkbook(workbook);
			workbook.write();
			workbook.close();
		} catch (WriteException e) {
			throw new IOException(e);
		}
	}
	
	public void createSheetInExcelWorkbook(WritableWorkbook workbook) throws RowsExceededException, WriteException {
		final WritableSheet sheet = workbook.createSheet(WorkbookUtils.sanitizeTabName(getName()), workbook.getNumberOfSheets());
		
		if(getUserObject() != null && getUserObject() instanceof DefaultTableDataSource) {
			final DefaultTableDataSource table = (DefaultTableDataSource)getUserObject();
			WorkbookUtils.addTableToSheet(sheet, 0, table);
		} else {
			final CSVTableModel tableModel = (CSVTableModel)dataTable.getModel();
			WorkbookUtils.addTableToSheet(sheet, 0, tableModel);			
		}
	}

	public void deleteSelectedRows() {
		final int selected[] = dataTable.getSelectedRows();
		final List<Integer> selectedRows = new ArrayList<Integer>();
		for(int viewSelected:selected) {
			int modelSelected = dataTable.convertRowIndexToModel(viewSelected);
			selectedRows.add(modelSelected);
		}
		Collections.sort(selectedRows);

		for(int rowIdx = selectedRows.size() - 1; rowIdx >= 0; rowIdx--) {
			final int row = selectedRows.get(rowIdx);
			final CSVTableModel model = (CSVTableModel)dataTable.getModel();
			model.deleteRow(row);
		}
	}

	/**
	 * Listener action for double clicks in the data table.
	 *
	 * Returns: Void
	 * Params: Integer (table row index)
	 */
	private final Consumer<Integer> tableAct = (row) -> {
		final JXTable tbl = getDataTable();
		if(tbl == null) return;

		final TableModel tblModel = tbl.getModel();
		if(tblModel == null) return;
		if(row < 0 || row >= tblModel.getRowCount()) return;

		// fix row number if sorted
		row = tbl.convertRowIndexToModel(row);

		// get project reference from parent window
		final CommonModuleFrame cmf =
				(CommonModuleFrame)SwingUtilities.getAncestorOfClass(CommonModuleFrame.class, BufferPanel.this);
		if(cmf == null) return;

		final Project project = cmf.getExtension(Project.class);
		if(project == null) return;

		// look for a session reference, if not found try to find a column
		// with name 'session'
		final Session primarySession = cmf.getExtension(Session.class);
		int sessionColumn = -1;
		int recordColumn = -1;

		for(int i = 0; i < tblModel.getColumnCount(); i++) {
			final String colName = tblModel.getColumnName(i);

			if(colName.equalsIgnoreCase("session")) {
				sessionColumn = i;
			} else if(colName.equalsIgnoreCase("record")
					|| colName.equalsIgnoreCase("record #")) {
				recordColumn = i;
			}
		}

		// check for required items
		if(primarySession == null && sessionColumn == -1) return;
		if(recordColumn == -1) return;

		// load session
		SessionEditor editor = null;
		// get values for each column

		SessionPath sp = primarySession != null ? primarySession.getSessionPath() : null;
		if(sessionColumn >= 0 && primarySession == null) {
			String sessionTxt = tblModel.getValueAt(row, sessionColumn).toString();
			if(sessionTxt == null || sessionTxt.isEmpty()) return;
			sp = SessionFactory.newFactory().createSessionPath(sessionTxt);
		}
		// load session editor (if necessary)
		final EntryPointArgs epArgs = new EntryPointArgs();
		epArgs.put(EntryPointArgs.PROJECT_OBJECT, project);
		epArgs.put(EntryPointArgs.CORPUS_NAME, sp.getFolder());
		epArgs.put(EntryPointArgs.SESSION_NAME, sp.getSessionFile());
		try {
			PluginEntryPointRunner.executePlugin(SessionEditorEP.EP_NAME, epArgs);
		} catch (PluginException e) {
			LogUtil.severe(e);
			return;
		}

		// find session editor
		for(CommonModuleFrame openWindow:CommonModuleFrame.getOpenWindows()) {
			if(openWindow instanceof SessionEditorWindow) {
				final SessionEditorWindow currentEditor = (SessionEditorWindow)openWindow;
				if(currentEditor.getSession().getCorpus().equals(sp.getFolder())
						&& currentEditor.getSession().getName().equals(sp.getSessionFile())) {
					editor = ((SessionEditorWindow)openWindow).getSessionEditor();
					break;
				}
			}
		}
		if(editor == null) return;

		// get record index
		String recordTxt = tblModel.getValueAt(row, recordColumn).toString();
		int recordNum = Integer.parseInt(recordTxt) - 1;
		if(recordNum < 0 || recordNum >= editor.getDataModel().getRecordCount()) return;

		editor.setCurrentRecordIndex(recordNum);

		// setup result highlighting if we have the appropriate column and user object
		if(getUserObject() != null && getUserObject() instanceof DefaultTableDataSource) {
			final DefaultTableDataSource tableData = (DefaultTableDataSource)getUserObject();
			final int resultColumn = tableData.getColumnIndex("Result");
			if(resultColumn >= 0) {
				final Object resultVal = tableData.getValueAt(row, resultColumn);
				if(resultVal != null && resultVal instanceof Result) {
					final Result result = (Result)resultVal;

					// setup highlighting
					final EditorSelectionModel selectionModel = editor.getSelectionModel();
					selectionModel.clear();
					for(ResultValue rv:result) {
						final Range range = new Range(rv.getRange().getFirst(), rv.getRange().getLast(), false);
						final SessionEditorSelection selection =
								new SessionEditorSelection(result.getRecordIndex(), rv.getTierName(), range);
						selectionModel.addSelection(selection);
					}
				}
			}
		}
	};

	private final class TableMouseAdapter extends MouseInputAdapter {

		private Consumer<Integer> functor;

		public TableMouseAdapter(Consumer<Integer> functor) {
			this.functor = functor;
		}

		@Override
		public void mouseClicked(MouseEvent me) {
			if(me.getClickCount() == 2 && me.getButton() == MouseEvent.BUTTON1) {
				functor.accept(getDataTable().getSelectedRow());
			}
		}

	}

	private final class TableAction extends AbstractAction {

		private Consumer<Integer> functor;

		public TableAction(Consumer<Integer> functor) {
			this.functor = functor;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			functor.accept(getDataTable().getSelectedRow());
		}

	}

	public void setBufferName(String string) {
		logBuffer.setBufferName(string);
	}

	public Object getExtension(String classname) {
		Object retVal = null;

		for(Class<?> ext:getExtensions()) {
			if(ext.toString().equals("class " + classname)) {
				retVal = getExtension(ext);
				break;
			}
		}

		return retVal;
	}

	public Set<Class<?>> getExtensions() {
		return extSupport.getExtensions();
	}

	public <T> T getExtension(Class<T> cap) {
		return extSupport.getExtension(cap);
	}

	public <T> T putExtension(Class<T> cap, T impl) {
		return extSupport.putExtension(cap, impl);
	}

	public <T> T removeExtension(Class<T> cap) {
		return extSupport.removeExtension(cap);
	}

}
