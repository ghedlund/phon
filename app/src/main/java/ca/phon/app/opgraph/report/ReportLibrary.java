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
package ca.phon.app.opgraph.report;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.MenuElement;

import ca.gedge.opgraph.OpGraph;
import ca.gedge.opgraph.nodes.general.MacroNode;
import ca.phon.app.opgraph.editor.OpGraphLibrary;
import ca.phon.app.opgraph.editor.OpgraphEditor;
import ca.phon.app.opgraph.editor.SimpleEditor;
import ca.phon.app.opgraph.nodes.ReportNodeInstantiator;
import ca.phon.app.opgraph.wizard.WizardExtension;
import ca.phon.opgraph.OpgraphIO;
import ca.phon.project.Project;
import ca.phon.ui.CommonModuleFrame;
import ca.phon.ui.action.PhonActionEvent;
import ca.phon.ui.action.PhonUIAction;
import ca.phon.ui.menu.MenuBuilder;
import ca.phon.ui.nativedialogs.FileFilter;
import ca.phon.ui.nativedialogs.MessageDialogProperties;
import ca.phon.ui.nativedialogs.NativeDialogs;
import ca.phon.ui.nativedialogs.OpenDialogProperties;
import ca.phon.util.OpenFileLauncher;
import ca.phon.util.Tuple;
import ca.phon.util.resources.ResourceLoader;
import ca.phon.worker.PhonWorker;

/**
 * <p>Library of query reports. These reports are available
 * from the 'Report' menu button in the query and query history
 * dialogs.</p>
 *
 * <p>Reports are stored in <code>~/Documents/Phon/reports</code>
 * by default. Reports can also be stored in the project
 * <code>__res/reports/</code> folder.</p>
 */
public class ReportLibrary implements OpGraphLibrary {

	private final static String LEGACY_REPORT_DOCUMENT = "reports/Legacy Report Design.xml";

	private final static String REPORT_FOLDER = "reports";

	private final static Logger LOGGER = Logger.getLogger(ReportLibrary.class.getName());

	/**
	 * Report loader
	 */
	private ResourceLoader<URL> loader = new ResourceLoader<>();

	public ReportLibrary() {
		super();
		loader.addHandler(new StockReportHandler());
		loader.addHandler(new UserReportHandler());
	}

	public List<URL> getAvailableReports() {
		List<URL> retVal = new ArrayList<URL>();

		final Iterator<URL> reportIterator = loader.iterator();
		while(reportIterator.hasNext()) {
			final URL url = reportIterator.next();
			if(url == null) continue;
			retVal.add(url);
		}

		return retVal;
	}

	public ResourceLoader<URL> getStockGraphs() {
		final ResourceLoader<URL> retVal = new ResourceLoader<>();
		retVal.addHandler(new StockReportHandler());
		return retVal;
	}

	public ResourceLoader<URL> getUserGraphs() {
		final ResourceLoader<URL> retVal = new ResourceLoader<>();
		retVal.addHandler(new UserReportHandler());
		return retVal;
	}

	public ResourceLoader<URL> getProjectGraphs(Project project) {
		final ResourceLoader<URL> retVal = new ResourceLoader<>();
		retVal.addHandler(new UserReportHandler(new File(project.getResourceLocation(), REPORT_FOLDER)));
		return retVal;
	}

	public File getProjectReportFolder(Project project) {
		return new File(project.getResourceLocation(), REPORT_FOLDER);
	}

	public void setupMenu(Project project, String queryId, MenuElement menu) {
		final MenuBuilder builder = new MenuBuilder(menu);

		final PhonUIAction showGeneratorAct = new PhonUIAction(ReportLibrary.class, "showGenerator", new Tuple<String, Project>(queryId, project));
		showGeneratorAct.putValue(PhonUIAction.NAME, "Composer (simple)...");
		showGeneratorAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Create a new report using simple Composer...");
		builder.addItem(".@composer", showGeneratorAct);

		final PhonUIAction showComposerAct = new PhonUIAction(ReportLibrary.class, "showComposer");
		showComposerAct.putValue(PhonUIAction.NAME, "Composer (advanced)...");
		showComposerAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Create a new report using Composer...");
		builder.addItem(".@Composer (simple)...", showComposerAct);

		builder.addSeparator(".", "composer");

		for(URL reportURL:getStockGraphs()) {
			final ReportAction act = new ReportAction(project, queryId, reportURL);

			try {
				final String fullPath = URLDecoder.decode(reportURL.getPath(), "UTF-8");
				final String relativePath =
						fullPath.substring(fullPath.indexOf("/" + REPORT_FOLDER + "/")+REPORT_FOLDER.length()+2);

				String menuPath = ".";
				int lastFolderIndex = relativePath.lastIndexOf('/');
				if(lastFolderIndex >= 0) {
					menuPath += "/" + relativePath.substring(0, lastFolderIndex);
				}

				builder.addItem(menuPath, act);
			} catch (UnsupportedEncodingException e) {
				LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
		}

		final JMenu userMenu = new JMenu("User Library");
		final MenuBuilder userMenuBuilder = new MenuBuilder(userMenu.getPopupMenu());
		final Iterator<URL> userGraphIterator = getUserGraphs().iterator();
		while(userGraphIterator.hasNext()) {
			try {
				final URL reportURL = userGraphIterator.next();

				final URI relativeURI =
						(new File(UserReportHandler.DEFAULT_USER_REPORT_FOLDER)).toURI().relativize(reportURL.toURI());

				final String relativePath = URLDecoder.decode(relativeURI.getPath(), "UTF-8");
				String menuPath = ".";
				int lastFolderIndex = relativePath.lastIndexOf('/');
				if(lastFolderIndex >= 0) {
					menuPath += "/" + relativePath.substring(0, lastFolderIndex);
				}

				final ReportAction act = new ReportAction(project, queryId, reportURL);
				userMenuBuilder.addItem(menuPath, act);
			} catch (URISyntaxException | UnsupportedEncodingException e) {
				LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
		}
		if(userMenu.getMenuComponentCount() > 0) {
			builder.addSeparator(".", "user_library");
			final JMenuItem userLibItem = builder.addItem(".@user_library", "-- User Library --");
			userLibItem.setFont(userLibItem.getFont().deriveFont(Font.BOLD));
			final File userLibFolder = new File(UserReportHandler.DEFAULT_USER_REPORT_FOLDER);
			userLibItem.setToolTipText("Show folder " + userLibFolder.getAbsolutePath());
			userLibItem.addActionListener( (e) -> {
				try {
					OpenFileLauncher.openURL( userLibFolder.toURI().toURL() );
				} catch (MalformedURLException e1) {
					LOGGER.log(Level.SEVERE, e1.getLocalizedMessage(), e1);
				}
			});
			builder.appendSubItems(".@-- User Library --", userMenu.getPopupMenu());
		}

		final JMenu projectMenu = new JMenu("Project Library");
		final MenuBuilder projectMenuBuilder = new MenuBuilder(projectMenu.getPopupMenu());
		final Iterator<URL> projectGraphIterator = getProjectGraphs(project).iterator();
		while(projectGraphIterator.hasNext()) {
			try {
				final URL reportURL = projectGraphIterator.next();
				final URI relativeURI =
						getProjectReportFolder(project).toURI().relativize(reportURL.toURI());

				final String relativePath = URLDecoder.decode(relativeURI.getPath(), "UTF-8");
				String menuPath = ".";
				int lastFolderIndex = relativePath.lastIndexOf('/');
				if(lastFolderIndex >= 0) {
					menuPath += "/" + relativePath.substring(0, lastFolderIndex);
				}

				final ReportAction act = new ReportAction(project, queryId, reportURL);
				projectMenuBuilder.addItem(menuPath, act);
			} catch (URISyntaxException | UnsupportedEncodingException e) {
				LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
		}
		if(projectMenu.getMenuComponentCount() > 0) {
			builder.addSeparator(".", "project_library");
			final JMenuItem projectSepItem = builder.addItem(".@project_library", "-- Project Library --");
			projectSepItem.setFont(projectSepItem.getFont().deriveFont(Font.BOLD));
			final File projectFolder = getProjectReportFolder(project);
			projectSepItem.addActionListener( (e) -> {
				try {
					OpenFileLauncher.openURL( projectFolder.toURI().toURL() );
				} catch (MalformedURLException e1) {
					LOGGER.log(Level.SEVERE, e1.getLocalizedMessage(), e1);
				}
			});
			projectSepItem.setToolTipText("Show folder " + projectFolder.getAbsolutePath());
			projectMenuBuilder.appendSubItems(".@-- Project Library --", projectMenu.getPopupMenu());
		}

//		builder.addSeparator(".", "legacy");
//		final ReportAction reportAct = new ReportAction(project, queryId,
//				getClass().getClassLoader().getResource(LEGACY_REPORT_DOCUMENT));
//		builder.addItem(".@legacy", reportAct);

		builder.addSeparator(".", "browse");
		final PhonUIAction onBrowseAct = new PhonUIAction(ReportLibrary.class, "onBrowse", new Tuple<String, Project>(queryId, project));
		onBrowseAct.putValue(PhonUIAction.NAME, "Browse...");
		onBrowseAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Browse for query report document.");
		builder.addItem(".@browse", onBrowseAct);


	}

	public static void onBrowse(PhonActionEvent pae) {
		@SuppressWarnings("unchecked")
		final Tuple<String, Project> data = (Tuple<String, Project>)pae.getData();
		final String queryId = data.getObj1();
		final Project project = (Project)data.getObj2();
		final FileFilter filter = new FileFilter("Query Report Documents", "xml;opgraph");
		final OpenDialogProperties props = new OpenDialogProperties();
		props.setParentWindow(CommonModuleFrame.getCurrentFrame());
		props.setFileFilter(filter);
		props.setCanChooseDirectories(false);
		props.setCanChooseFiles(true);
		props.setAllowMultipleSelection(false);
		props.setRunAsync(false);

		final List<String> selectedFiles =
				NativeDialogs.showOpenDialog(props);
		if(selectedFiles != null && selectedFiles.size() == 1) {
			final File selectedFile = new File(selectedFiles.get(0));

			// attempt to run file as an analysis
			try {
				final OpGraph graph = OpgraphIO.read(selectedFile);

				final WizardExtension ext = graph.getExtension(WizardExtension.class);
				if(ext == null || !(ext instanceof ReportWizardExtension)) {
					throw new IOException("Selected document is not a query report");
				}

				final ReportRunner runner = new ReportRunner(graph, project, queryId);
				PhonWorker.getInstance().invokeLater(runner);
			} catch (IOException e) {
				Toolkit.getDefaultToolkit().beep();
				LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);

				final MessageDialogProperties mprops = new MessageDialogProperties();
				mprops.setParentWindow(CommonModuleFrame.getCurrentFrame());
				mprops.setTitle("Report : Error");
				mprops.setHeader("Unable to run selected query report");
				mprops.setMessage(e.getLocalizedMessage());
				mprops.setOptions(MessageDialogProperties.okOptions);
				NativeDialogs.showMessageDialog(mprops);
			}
		}
	}

	public static void showGenerator(PhonActionEvent pae) {
		@SuppressWarnings("unchecked")
		final Tuple<String, Project> data = (Tuple<String, Project>)pae.getData();
		final String queryId = data.getObj1();
		final SimpleEditor frame =
				new SimpleEditor(data.getObj2(),
						new ReportLibrary(), new ReportEditorModelInstantiator(), new ReportNodeInstantiator(),
						(qs) -> new MacroNode(),
						(graph, project) -> new ReportRunner(graph, project, queryId) );
		frame.pack();
		frame.setSize(new Dimension(700, 500));
		frame.centerWindow();
		frame.setVisible(true);
	}

	public static void showComposer() {
		final ReportEditorModelInstantiator instantiator = new ReportEditorModelInstantiator();
		final ReportOpGraphEditorModel editorModel = instantiator.createModel(new OpGraph());
		final OpgraphEditor editor =  new OpgraphEditor(editorModel);

		final Project project = CommonModuleFrame.getCurrentFrame().getExtension(Project.class);
		if(project != null) {
			editor.putExtension(Project.class, project);
		}

		editor.setLocationRelativeTo(CommonModuleFrame.getCurrentFrame());
		editor.pack();
		editor.setSize(1024, 768);
		editor.setVisible(true);
	}

	@Override
	public String getFolderName() {
		return REPORT_FOLDER;
	}

	@Override
	public String getUserFolderPath() {
		return UserReportHandler.DEFAULT_USER_REPORT_FOLDER;
	}

	@Override
	public String getProjectFolderPath(Project project) {
		return getProjectReportFolder(project).getAbsolutePath();
	}

}
