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
package ca.phon.phonex.opgraph.nodes;

import ca.phon.ipa.IPATranscript;
import ca.phon.opgraph.*;
import ca.phon.opgraph.app.GraphDocument;
import ca.phon.opgraph.app.extensions.NodeSettings;
import ca.phon.opgraph.exceptions.ProcessingException;
import ca.phon.opgraph.extensions.NodeMetadata;
import ca.phon.opgraph.nodes.general.MacroNode;
import ca.phon.phonex.PhonexMatcher;
import ca.phon.phonex.PhonexPattern;
import ca.phon.phonex.PhonexPatternException;

import java.awt.*;
import java.util.List;
import java.util.*;

@OpNodeInfo(
		name="Phonex Find",
		description="Find occurrences of a phonex pattern.",
		category="Phonex"
)
public class PhonexFindNode extends MacroNode implements PhonexNode {
	public static final String CURRENT_ITERATION_KEY = "currentIteration";
	public static final String MAX_ITERATIONS_KEY = "maxIterations";
	
	// input field
	private final static InputField ipaInput = 
			new InputField("ipa", "ipa input", IPATranscript.class);
	// pass-through output
	private final static OutputField ipaOut =
			new OutputField("ipa out", "pass-through ipa ouptput", true, IPATranscript.class);
	
	/**
	 * Compiled phonex pattern
	 */
	private PhonexPattern pattern;
	
	/**
	 * Current matcher
	 */
	private PhonexMatcher matcher;
	
	public PhonexFindNode() {
		this(new OpGraph());
	}
	
	public PhonexFindNode(OpGraph graph) {
		super(graph);
		
		super.putField(ipaInput);
		super.putField(ipaOut);
		
		super.putExtension(NodeSettings.class, this);
	}

	@Override
	public void operate(OpContext context) throws ProcessingException {
		// Process
		if(graph != null) {
			final Processor processor = new Processor(graph);
			
			final IPATranscript ipa = 
					(IPATranscript)context.get(ipaInput);
			matcher = pattern.matcher(ipa);
			
			int iteration = 0;
			while(matcher.find()) {
				processor.reset(context);
				
//				mapGroupValues(context);
				context.put("__matcher__", matcher);
				
				// Now run the graph
				processor.stepAll();
				if(processor.getError() != null)
					throw processor.getError();
				
				// Map the published outputs from the child vertices back into context
				mapOutputs(context, iteration++);
			}
			
			context.put(ipaOut, ipa);
		}
	}

	/**
	 * Maps published outputs from a given context mapping to a given context.
	 * 
	 * @param contextsMap  the context mapping to map outputs from
	 * @param context  the context to map outputs to
	 */
	private void mapOutputs(OpContext context, int iteration) {
		// Grab mapped outputs and put them in our context
		for(PublishedOutput publishedOutput : publishedOutputs) {
			final OpContext sourceContext = context.findChildContext(publishedOutput.sourceNode);
			if(sourceContext != null) {
				final Object result = sourceContext.get(publishedOutput.nodeOutputField);
				if(context.containsKey(publishedOutput)) {
					final ArrayList<Object> objects = new ArrayList<Object>((ArrayList<?>)context.get(publishedOutput));
					objects.add(result);
					context.put(publishedOutput, objects);
				} else {
					final ArrayList<Object> objects = new ArrayList<Object>();
					objects.add(result);
					context.put(publishedOutput, objects);
				}
			}
		}
	}
	
	@Override
	public OutputField publish(String key, OpNode source, OutputField field) {
		final OutputField published = super.publish(key, source, field);
	    published.setOutputType(Collection.class);
	    return published;
	}

	@Override
	public void setPhonex(String phonex) throws PhonexPatternException {
		// attempt to compile new pattern
		pattern = PhonexPattern.compile(phonex);
		
		// find current group nodes
		final List<PhonexGroupNode> groupNodes = 
				new ArrayList<PhonexGroupNode>();
		for(OpNode vertex:getGraph().getVertices()) {
			if(vertex instanceof PhonexGroupNode) {
				groupNodes.add((PhonexGroupNode)vertex);
			}
		}
		Collections.sort(groupNodes, new Comparator<PhonexGroupNode>() {

			@Override
			public int compare(PhonexGroupNode o1, PhonexGroupNode o2) {
				Integer o1Grp = o1.getGroup();
				Integer o2Grp = o2.getGroup();
				return o1Grp.compareTo(o2Grp);
			}
			
		});
		// adjust group nodes
		for(int gIdx = 0; gIdx <= pattern.numberOfGroups(); gIdx++) {
			PhonexGroupNode node = null;
			
			if(gIdx < groupNodes.size()) {
				node = groupNodes.get(gIdx);
			} else {
				node = new PhonexGroupNode(gIdx);
				getGraph().add(node);
			}
			
			final String grpName = 
					(pattern.groupName(gIdx) != null ? pattern.groupName(gIdx) : "g" + gIdx);
			node.setName(grpName);
			
			// adjust position
			final int xpos = 100;
			final int ypos = 50 + (gIdx * 100) + 20;
			node.putExtension(NodeMetadata.class, new NodeMetadata(xpos, ypos));
		}
		// remove left-overs
		for(int i = pattern.numberOfGroups()+1; i < groupNodes.size(); i++) {
			getGraph().remove(groupNodes.get(i));
		}
	}

	@Override
	public String getPhonex() {
		String retVal = "";
		if(pattern != null)
			retVal = pattern.pattern();
		return retVal;
	}

	private PhonexSettingsPanel settingsPanel;
	@Override
	public Component getComponent(GraphDocument arg0) {
		if(settingsPanel == null) {
			settingsPanel = new PhonexSettingsPanel(arg0, this);
		}
		return settingsPanel;
	}
	
	private final String PHONEX_KEY = 
			getClass().getName() + ".phonex";

	@Override
	public Properties getSettings() {
		final Properties retVal = new Properties();
		if(pattern != null) {
			retVal.setProperty(PHONEX_KEY, pattern.pattern());
		}
		return retVal;
	}

	@Override
	public void loadSettings(Properties arg0) {
		if(arg0.containsKey(PHONEX_KEY)) {
			setPhonex(arg0.getProperty(PHONEX_KEY));
		}
	}

//	@Override
//	public CustomProcessor getCustomProcessor() {
//		return new ForEachFindProcessor();
//	}
//	
//	private class ForEachFindProcessor implements CustomProcessor {
//		
//		private OperableVertex nextVertex;
//		private Iterator<OperableVertex> vertexItr;
//
//		@Override
//		public boolean hasNext() {
//			// we already have a queued vertex
//			if(nextVertex != null) return true;
//			
//			// at start
//			if(vertexItr == null) {
//				vertexItr = getGraph().getVertices().iterator();
//			}
//			
//		}
//
//		@Override
//		public OperableVertex next() {
//			if(!hasNext())
//				throw new NoSuchElementException();
//			
//			final OperableVertex retVal = nextVertex;
//			nextVertex = null;
//			return retVal;
//		}
//
//		@Override
//		public void remove() {
//			throw new UnsupportedOperationException();
//		}
//
//		@Override
//		public void initialize(OperableContext context) {
//			// get ipa input
//			IPATranscript ipa = 
//					(IPATranscript)context.get(ipaInput);
//			matcher = pattern.matcher(ipa);
//		}
//
//		@Override
//		public void terminate(OperableContext context) {
//		}
//		
//	}
}
