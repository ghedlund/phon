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
package ca.phon.app.opgraph.editor.actions.graph;

import ca.phon.app.log.LogUtil;
import ca.phon.app.opgraph.editor.OpgraphEditor;
import ca.phon.app.opgraph.editor.actions.OpgraphEditorAction;
import ca.phon.opgraph.*;
import ca.phon.opgraph.app.GraphDocument;
import ca.phon.opgraph.app.edits.graph.*;
import ca.phon.opgraph.app.util.GraphUtils;
import ca.phon.opgraph.dag.*;
import ca.phon.opgraph.exceptions.ItemMissingException;
import ca.phon.opgraph.extensions.NodeMetadata;

import javax.swing.*;
import javax.swing.undo.CompoundEdit;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class DuplicateAction extends OpgraphEditorAction {
	
	public final static String TXT = "Duplicate nodes";
	
	public final static String DESC = "Duplicate selected nodes";
	
	public final static KeyStroke KS = KeyStroke.getKeyStroke(KeyEvent.VK_D, 
			Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());

	public DuplicateAction(OpgraphEditor editor) {
		super(editor);
		
		putValue(NAME, TXT);
		putValue(SHORT_DESCRIPTION, DESC);
		putValue(ACCELERATOR_KEY, KS);
	}

	@Override
	public void hookableActionPerformed(ActionEvent arg0) {
		final GraphDocument document = getEditor().getModel().getDocument();
		if(document != null) {
			// Check to make sure the clipboard has something we can paste
			final Collection<OpNode> selectedNodes = document.getSelectionModel().getSelectedNodes();
			if(isEnabled() && selectedNodes.size() > 0) {
				final CompoundEdit cmpEdit = new CompoundEdit();
				final OpGraph graph = document.getGraph();
				final Map<String, String> nodeMap = new HashMap<String, String>();

				// Create a new node edit for each node in the contents
				final Collection<OpNode> newNodes = new ArrayList<OpNode>();
				for(OpNode node : selectedNodes) {
					// Clone the node
					final OpNode newNode = GraphUtils.cloneNode(node);
					newNodes.add(newNode);
					nodeMap.put(node.getId(), newNode.getId());

					// Offset to avoid pasting on top of current nodes
					final NodeMetadata metadata = newNode.getExtension(NodeMetadata.class);
					if(metadata != null) {
						metadata.setX(metadata.getX() + 50);
						metadata.setY(metadata.getY() + 30);
					}

					// Add an undoable edit for this node
					cmpEdit.addEdit(new AddNodeEdit(graph, newNode));
				}

				// Duplicated nodes become the selection
				document.getSelectionModel().setSelectedNodes(newNodes);

				// For each selected node, copy outgoing links if they fully contained in the selection
				for(OpNode selectedNode : selectedNodes) {
					final Collection<OpLink> outgoingLinks = graph.getOutgoingEdges(selectedNode);
					for(OpLink link : outgoingLinks) {
						if(selectedNodes.contains(link.getDestination())) {
							final OpNode srcNode = graph.getNodeById(nodeMap.get(link.getSource().getId()), false);
							final OutputField srcField = srcNode.getOutputFieldWithKey(link.getSourceField().getKey());
							final OpNode dstNode = graph.getNodeById(nodeMap.get(link.getDestination().getId()), false);
							final InputField dstField = dstNode.getInputFieldWithKey(link.getDestinationField().getKey());

							try {
								final OpLink newLink = new OpLink(srcNode, srcField, dstNode, dstField);
								cmpEdit.addEdit(new AddLinkEdit(graph, newLink));
							} catch(VertexNotFoundException | CycleDetectedException | InvalidEdgeException | ItemMissingException exc) {
								LogUtil.warning(exc);
							}
						}
					}
				}

				// Add the compound edit to the undo manager
				cmpEdit.end();
				document.getUndoSupport().postEdit(cmpEdit);
			}
		}
	}

}
