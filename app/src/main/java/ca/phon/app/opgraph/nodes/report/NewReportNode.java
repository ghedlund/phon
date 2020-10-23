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
package ca.phon.app.opgraph.nodes.report;

import java.awt.*;
import java.util.*;

import javax.swing.*;

import ca.phon.app.opgraph.report.tree.*;
import ca.phon.opgraph.*;
import ca.phon.opgraph.app.*;
import ca.phon.opgraph.app.extensions.*;
import ca.phon.opgraph.exceptions.*;

@OpNodeInfo(name="New Report", description="New report tree", category="Report", showInLibrary=true)
public class NewReportNode extends OpNode implements NodeSettings {

	public final static String REPORT_TREE_KEY = "_reportTree";

	private final InputField reportNameInput =
			new InputField("reportName", "Report name", true, true, String.class);
	
	private final InputField reportRootInput =
			new InputField("root", "Report root (overrides name)", true, true, ReportTreeNode.class);

	private final OutputField reportOutput =
			new OutputField("reportTree", "Report tree", true, ReportTree.class);

	private final OutputField reportRootOutput =
			new OutputField("root", "Root node of report tree", true, ReportTreeNode.class);

	private JPanel settingsPanel;

	public NewReportNode() {
		super();

		putField(reportRootInput);
		putField(reportNameInput);
		putField(reportOutput);
		putField(reportRootOutput);

		putExtension(NodeSettings.class, this);
	}

	@Override
	public void operate(OpContext context) throws ProcessingException {
		final ReportTreeNode root =
				(context.get(reportRootInput) != null ? (ReportTreeNode)context.get(reportRootInput) : 
						( context.get(reportNameInput) != null ? new SectionHeaderNode(context.get(reportNameInput).toString()) : new SectionHeaderNode("root") ) );

		final ReportTree reportTree = new ReportTree(root);
		// may need to set name
		if(context.get(reportNameInput) != null)
			root.setTitle(context.get(reportNameInput).toString());
		
		context.put(reportOutput, reportTree);
		context.put(reportRootOutput, root);
		
		if(context.containsKey(REPORT_TREE_KEY)) {
			final ReportTree masterReport = (ReportTree)context.get(REPORT_TREE_KEY);
			masterReport.getRoot().add(root);
		}
	}

	@Override
	public Component getComponent(GraphDocument document) {
		if(settingsPanel == null) {
			settingsPanel = new JPanel(new BorderLayout());
		}
		return settingsPanel;
	}

	@Override
	public Properties getSettings() {
		Properties retVal = new Properties();
		return retVal;
	}

	@Override
	public void loadSettings(Properties properties) {
	}

}
