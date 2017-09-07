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
package ca.phon.app.session;

import java.text.Collator;
import java.util.*;

import javax.swing.ImageIcon;
import javax.swing.tree.*;

import ca.phon.project.Project;
import ca.phon.session.SessionPath;
import ca.phon.ui.tristatecheckbox.*;
import ca.phon.util.CollatorFactory;
import ca.phon.util.icons.*;

/**
 * Displays an interface for selection one or more
 * sessions in a given project.
 *
 */
public class SessionSelector extends TristateCheckBoxTree {

	private static final long serialVersionUID = 5336741342440773144L;

	public static TristateCheckBoxTreeModel createModel(Project project, boolean hideEmptyCorpora) {
		if(project == null)
			return new TristateCheckBoxTreeModel(new DefaultMutableTreeNode("No project"));

		final TristateCheckBoxTreeNode root = new TristateCheckBoxTreeNode(project);
		root.setEnablePartialCheck(false);

		// create new tree structure
		Collator collator = CollatorFactory.defaultCollator();
		List<String> corpora = project.getCorpora();
		Collections.sort(corpora, collator);
		for(String corpus:corpora) {
			TristateCheckBoxTreeNode corpusNode = new TristateCheckBoxTreeNode(corpus);
			corpusNode.setEnablePartialCheck(false);

			List<String> sessions = project.getCorpusSessions(corpus);
			if(sessions.size() == 0 && hideEmptyCorpora) continue;
			Collections.sort(sessions, collator);
			for(String session:sessions) {
				TristateCheckBoxTreeNode sessionNode = new TristateCheckBoxTreeNode(session);
				sessionNode.setEnablePartialCheck(false);
				corpusNode.add(sessionNode);
			}
			root.add(corpusNode);
		}

		return new TristateCheckBoxTreeModel(root);
	}

	/** The project */
	private Project project;

	private boolean hideEmptyCorpora;

	public SessionSelector() {
		this(null, true);
	}

	public SessionSelector(Project project) {
		this(project, true);
	}

	/** Constructor */
	public SessionSelector(Project project, boolean hideEmptyCorpora) {
		super(createModel(project, hideEmptyCorpora));

		this.project = project;
		this.hideEmptyCorpora = hideEmptyCorpora;

		init();
	}

	public Project getProject() {
		return this.project;
	}

	public void setProject(Project project) {
		final Project oldProject = this.project;
		this.project = project;
		super.firePropertyChange("project", oldProject, project);

		setModel(createModel(project, isHideEmptyCorpora()));
	}

	public boolean isHideEmptyCorpora() {
		return this.hideEmptyCorpora;
	}

	public void setHideEmptyCorpora(boolean hideEmptyCorpora) {
		this.hideEmptyCorpora = hideEmptyCorpora;
		setModel(createModel(project, hideEmptyCorpora));
	}

	private void init() {
		ImageIcon sessionIcon = IconManager.getInstance().getIcon(
				"mimetypes/text-xml", IconSize.SMALL);
		final ImageIcon folderIcon = IconManager.getInstance().getIcon("places/folder", IconSize.SMALL);

		final TristateCheckBoxTreeCellRenderer renderer = new TristateCheckBoxTreeCellRenderer();
		renderer.setLeafIcon(sessionIcon);
		renderer.setClosedIcon(folderIcon);
		renderer.setOpenIcon(folderIcon);

		final TristateCheckBoxTreeCellRenderer editorRenderer = new TristateCheckBoxTreeCellRenderer();
		editorRenderer.setLeafIcon(sessionIcon);
		editorRenderer.setClosedIcon(folderIcon);
		editorRenderer.setOpenIcon(folderIcon);
		final TristateCheckBoxTreeCellEditor editor = new TristateCheckBoxTreeCellEditor(this, editorRenderer);

		setCellRenderer(renderer);
		setCellEditor(editor);

		super.expandRow(0);
	}

	public TreePath sessionPathToTreePath(SessionPath sessionPath) {
		final TristateCheckBoxTreeNode root = (TristateCheckBoxTreeNode)getModel().getRoot();
		for(int i = 0; i < root.getChildCount(); i++) {
			final TristateCheckBoxTreeNode corpusNode = (TristateCheckBoxTreeNode)root.getChildAt(i);
			if(corpusNode.getUserObject().equals(sessionPath.getCorpus())) {
				for(int j = 0; j < corpusNode.getChildCount(); j++) {
					final TristateCheckBoxTreeNode sessionNode = (TristateCheckBoxTreeNode)corpusNode.getChildAt(j);
					if(sessionNode.getUserObject().equals(sessionPath.getSession())) {
						final TreePath checkPath = new TreePath(
								new Object[]{ root, corpusNode, sessionNode });
						return checkPath;
					}
				}
			}
		}
		return null;
	}

	public List<SessionPath> getSelectedSessions() {
		List<SessionPath> retVal =
			new ArrayList<SessionPath>();

		List<TreePath> checkPaths = super.getCheckedPaths();


		for(TreePath checkPath:checkPaths) {
			if(checkPath.getPath().length != 3)
				continue;
			String corpus = checkPath.getPath()[1].toString();
			String session = checkPath.getPath()[2].toString();

			SessionPath loc = new SessionPath(corpus, session);
			retVal.add(loc);
		}

		Collections.sort(retVal, (sp1, sp2) -> sp1.toString().compareTo(sp2.toString()) );

		return retVal;
	}

	public void setSelectedSessions(List<SessionPath> selectedSessions) {
		super.clearSelection();

		for(SessionPath sessionPath:selectedSessions) {
			final TreePath path = sessionPathToTreePath(sessionPath);
			super.setCheckingStateForPath(path, TristateCheckBoxState.CHECKED);
			expandPath(path.getParentPath());
		}
	}
}
