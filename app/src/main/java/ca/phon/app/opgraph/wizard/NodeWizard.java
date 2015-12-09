package ca.phon.app.opgraph.wizard;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JScrollPane;

import org.jdesktop.swingx.JXBusyLabel;

import ca.gedge.opgraph.OpContext;
import ca.gedge.opgraph.OpGraph;
import ca.gedge.opgraph.OpNode;
import ca.gedge.opgraph.Processor;
import ca.gedge.opgraph.app.extensions.NodeSettings;
import ca.gedge.opgraph.exceptions.ProcessingException;
import ca.phon.app.log.MultiBufferPanel;
import ca.phon.app.opgraph.nodes.log.PrintBufferNode;
import ca.phon.project.Project;
import ca.phon.ui.decorations.DialogHeader;
import ca.phon.ui.wizard.WizardFrame;
import ca.phon.ui.wizard.WizardStep;

public class NodeWizard extends WizardFrame {
	
	private final Processor processor;
	
	private final OpGraph graph;
	
	private MultiBufferPanel bufferPanel;
	
	private JXBusyLabel busyLabel;
	
	private JLabel statusLabel;
	
	public NodeWizard(String title, Processor processor, OpGraph graph) {
		super(title);
		
		this.processor = processor;
		this.graph = graph;
		init();
	}
	
	private void init() {
		bufferPanel = new MultiBufferPanel();
		
		final DialogHeader header = new DialogHeader(super.getTitle(), "");
		add(header, BorderLayout.NORTH);
		
		final WizardExtension nodeWizardList = 
				graph.getExtension(WizardExtension.class);
		int stepIdx = 0;
		if(nodeWizardList != null) {
			for(OpNode node:nodeWizardList) {
				final WizardStep step = createStep(node);
				step.setPrevStep(stepIdx-1);
				step.setNextStep(stepIdx+1);
				++stepIdx;
				
				addWizardStep(step);
			}
		}
		
		final WizardStep reportStep = createReportStep();
		reportStep.setPrevStep(stepIdx-1);
		reportStep.setNextStep(-1);
		addWizardStep(reportStep);
	}
	
	public MultiBufferPanel getBufferPanel() {
		return this.bufferPanel;
	}
	
	public OpGraph getGraph() {
		return this.graph;
	}
	
	protected void executeGraph() throws ProcessingException {
		
	}

	protected void setupContext(OpContext ctx) {
		ctx.put(PrintBufferNode.BUFFERS_KEY, bufferPanel);
	}
	
	protected WizardStep createStep(OpNode node) {
		final NodeSettings settings = node.getExtension(NodeSettings.class);
		if(settings != null) {
			try {
				final Component comp = settings.getComponent(null);
				
				final WizardStep step = new WizardStep();
				step.setLayout(new BorderLayout());
				step.add(new JScrollPane(comp), BorderLayout.CENTER);
				
				return step;
			} catch (NullPointerException e) {
				// we have no document, this may cause an exception
				// depending on implementation - ignore it.
			}
		}
		return null;
	}
	
	protected WizardStep createReportStep() {
		final WizardStep retVal = new WizardStep();
		
		retVal.setLayout(new BorderLayout());
		
		retVal.add(getBufferPanel(), BorderLayout.CENTER);
		
		return retVal;
	}
	
}
