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
package ca.phon.app.opgraph.nodes.query;

import ca.phon.app.log.LogUtil;
import ca.phon.app.query.report.ReportEditor;
import ca.phon.opgraph.*;
import ca.phon.opgraph.app.GraphDocument;
import ca.phon.opgraph.app.extensions.NodeSettings;
import ca.phon.opgraph.exceptions.ProcessingException;
import ca.phon.project.Project;
import ca.phon.query.db.*;
import ca.phon.query.report.*;
import ca.phon.query.report.io.ReportDesign;
import ca.phon.util.PrefHelper;
import jakarta.xml.bind.*;

import javax.swing.*;
import javax.xml.namespace.QName;
import java.awt.*;
import java.io.*;
import java.util.Properties;

/**
 * 
 */
@Deprecated
@OpNodeInfo(
		name="Report",
		category="Report",
		description="Create report from a pre-defined design file",
		showInLibrary=true
)
public class ReportDesignNode extends OpNode implements NodeSettings {
	
	public final static String AUTOSAVE_FILENAME = "lastreport.xml";
	
	private InputField projectInputField = 
			new InputField("project", "Project", false, true, Project.class);
	
	private InputField queryInputField =
			new InputField("query", "Query", false, true, Query.class);

	private InputField resultSetsField =
			new InputField("results", "Result sets from query", false, true, ResultSet[].class);
	
	private OutputField projectOutputField = 
			new OutputField("project", "Project", true, Project.class);
	

	private OutputField reportField = 
			new OutputField("report", "Generated report as a string", true, String.class);
	
	private ReportDesign reportDesign;
	
	/**
	 * If <code>true</code> the report created using this node will be
	 * saved as the 'last report.'  The 'last report' will also be loaded
	 * with this node
	 */
	private boolean useLastReport = false;
	
	private ReportEditor reportEditor;
	
	private JCheckBox useLastReportBox;
	
	private JPanel settingsPanel;
	
	public ReportDesignNode() {
		this(new ReportDesign());
	}
	
	public ReportDesignNode(ReportDesign reportDesign) {
		super();

		this.reportDesign = reportDesign;
		
		putField(projectInputField);
		putField(queryInputField);
		putField(resultSetsField);
		putField(projectOutputField);
		putField(reportField);
		
		putExtension(NodeSettings.class, this);
	}
	
	@Override
	public void operate(OpContext context) throws ProcessingException {
		final Project project = (Project)context.get(projectInputField);
		if(project == null) throw new ProcessingException(null, "Project cannot be null");
		
		final Query query = (Query)context.get(queryInputField);
		if(query == null) throw new ProcessingException(null, "Query cannot be null");
		
		final ResultSet[] resultSets = (ResultSet[])context.get(resultSetsField);
		if(resultSets == null || resultSets.length == 0)
			throw new ProcessingException(null, "No result sets given");
		
		final ReportBuilder builder = ReportBuilderFactory.getInstance().getBuilder("CSV");
		try {
			final ByteArrayOutputStream bout = new ByteArrayOutputStream();
			builder.buildReport(getReportDesign(), project, query, resultSets, bout);
			
			context.put(reportField, new String(bout.toByteArray(), "UTF-8"));
		} catch (ReportBuilderException | UnsupportedEncodingException e) {
			throw new ProcessingException(null, e);
		}
		
		context.put(projectOutputField, project);
		
		if(isUseLastReport()) {
			// save report
			// use jaxb to save to element
			try {
				JAXBContext ctx = JAXBContext.newInstance("ca.phon.query.report.io");
				Marshaller marshaller = ctx.createMarshaller();
				marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
				marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
				
				QName reportDesignQName = new QName("http://phon.ling.mun.ca/ns/report", "report-design");
				JAXBElement<ReportDesign> reportDesignEle = 
						new JAXBElement<ReportDesign>(reportDesignQName, ReportDesign.class, reportDesign);
				marshaller.marshal(reportDesignEle, new File(PrefHelper.getUserDataFolder(), AUTOSAVE_FILENAME));
			} catch (JAXBException e) {
				LogUtil.warning(e);
			}
		}
	}
	
	public boolean isUseLastReport() {
		return (this.useLastReportBox != null ? this.useLastReportBox.isSelected() : this.useLastReport);
	}
	
	public void setUseLastReport(boolean useLastReport) {
		this.useLastReport = useLastReport;
		if(this.useLastReportBox != null)
			this.useLastReportBox.setSelected(this.useLastReport);
	}
	
	public ReportDesign getReportDesign() {
		return 
				(this.reportEditor != null ? this.reportEditor.getReportDesign() : this.reportDesign);
	}

	@Override
	public Component getComponent(GraphDocument document) {
		if(this.settingsPanel == null) {
			this.settingsPanel = new JPanel(new BorderLayout());
			
			useLastReportBox = new JCheckBox("Remember outline");
			useLastReportBox.setSelected(useLastReport);

			this.settingsPanel.add(useLastReportBox, BorderLayout.NORTH);
			
			this.reportEditor = 
					(this.reportDesign == null ? new ReportEditor() : new ReportEditor(reportDesign));
			
			this.settingsPanel.add(this.reportEditor, BorderLayout.CENTER);
		}
		return this.settingsPanel;
	}

	@Override
	public Properties getSettings() {
		return new Properties();
	}

	@Override
	public void loadSettings(Properties properties) {
		
	}

}
