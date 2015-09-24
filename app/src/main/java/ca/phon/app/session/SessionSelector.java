/*
 * Phon - An open source tool for research in phonology.
 * Copyright (C) 2005 - 2015, Gregory Hedlund <ghedlund@mun.ca> and Yvan Rose <yrose@mun.ca>
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.tree.TreePath;

import ca.phon.project.Project;
import ca.phon.session.SessionPath;
import ca.phon.ui.CheckedTreeNode;
import ca.phon.util.CollatorFactory;
import ca.phon.util.icons.IconManager;
import ca.phon.util.icons.IconSize;
import it.cnr.imaa.essi.lablib.gui.checkboxtree.CheckboxTree;

/**
 * Displays an interface for selection one or more
 * sessions in a given project.
 *
 */
public class SessionSelector extends CheckboxTree {
	
	private static final long serialVersionUID = 5336741342440773144L;

	/** The project */
	private Project project;
	
	/** GUI */
	private CheckedTreeNode rootNode;
	
	/** Constructor */
	public SessionSelector(Project project) {
		super(new CheckedTreeNode(project.getName()));
	
		rootNode = (CheckedTreeNode)super.getModel().getRoot();
		
		this.project = project;
		
		init();
	}
	
	private void init() {
		createTree();
		
		super.expandRow(0);
	}
	
	private void createTree() {
		// create new tree structure
		Collator collator = CollatorFactory.defaultCollator();
		List<String> corpora = project.getCorpora();
		Collections.sort(corpora, collator);
		for(String corpus:corpora) {
			CheckedTreeNode corpusNode = new CheckedTreeNode(corpus);
			
			List<String> sessions = project.getCorpusSessions(corpus);
			Collections.sort(sessions, collator);
			for(String session:sessions) {
				CheckedTreeNode sessionNode = new CheckedTreeNode(session);
				corpusNode.add(sessionNode);
			}
			rootNode.add(corpusNode);
		}
		
		final CheckboxTree projectTree = this;
		
		it.cnr.imaa.essi.lablib.gui.checkboxtree.DefaultCheckboxTreeCellRenderer
			renderer = new it.cnr.imaa.essi.lablib.gui.checkboxtree.DefaultCheckboxTreeCellRenderer();
		ImageIcon icn = IconManager.getInstance().getIcon(
				"blank", IconSize.SMALL);
		
		renderer.setLeafIcon(icn);
		renderer.setClosedIcon(icn);
		renderer.setOpenIcon(icn);
		projectTree.setCellRenderer(renderer);
		revalidate();
	}
	
	public List<SessionPath> getSelectedSessions() {
		List<SessionPath> retVal = 
			new ArrayList<SessionPath>();
		
		TreePath[] checkPaths = getCheckingPaths();
	
		
		for(TreePath checkPath:checkPaths) {
			if(checkPath.getPath().length != 3)
				continue;
			String corpus = checkPath.getPath()[1].toString();
			String session = checkPath.getPath()[2].toString();
			
			SessionPath loc = new SessionPath(corpus, session);
			retVal.add(loc);
		}
		
		return retVal;
	}
}
