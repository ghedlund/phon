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
package ca.phon.app.opgraph.nodes.query;

import java.awt.*;

import javax.swing.*;
import javax.swing.event.*;

import org.jdesktop.swingx.*;

import ca.phon.app.opgraph.nodes.query.SortNodeSettings.*;
import ca.phon.app.opgraph.nodes.query.SortNodeSettings.SortOrder;
import ca.phon.ui.action.PhonUIAction;
import ca.phon.ui.layout.ButtonBarBuilder;
import ca.phon.ui.text.PromptedTextField;
import ca.phon.util.icons.*;

public class SortNodeSettingsPanel extends JPanel {

	private static final long serialVersionUID = 4289280424233502931L;
	
	private final SortNodeSettings settings;
	
	private JPanel sortByPanel;
	
	private JButton addSortButton;
	
	public SortNodeSettingsPanel(SortNodeSettings settings) {
		super();
		this.settings = settings;
		
		init();
	}
	
	private void init() {
		setLayout(new VerticalLayout());
		
		sortByPanel = new JPanel(new VerticalLayout());
		
		final ImageIcon icon = IconManager.getInstance().getIcon("actions/list-add", IconSize.SMALL);
		final Action onAddAction = new PhonUIAction(this, "onAddColumn");
		onAddAction.putValue(Action.NAME, "Add");
		onAddAction.putValue(Action.SHORT_DESCRIPTION, "Add column to sort");
		onAddAction.putValue(Action.SMALL_ICON, icon);
		addSortButton = new JButton(onAddAction);
		
		int scIdx = 0;
		for(SortColumn sc:settings.getSorting()) {
			final SortColumnPanel scPanel = new SortColumnPanel(sc);
			if(scIdx > 0) {
				final JComponent sep = createSeparator(scPanel);
				sortByPanel.add(sep);
			}
			sortByPanel.add(scPanel);
			++scIdx;
		}
		
		final JPanel btmPanel = new JPanel(new VerticalLayout());
		btmPanel.setBorder(BorderFactory.createTitledBorder("Sort by"));
		btmPanel.add(sortByPanel);
		btmPanel.add(ButtonBarBuilder.buildOkBar(addSortButton));
		add(btmPanel);
	}
	
	public void onAddColumn() {
		final SortColumn sc = new SortColumn();
		settings.getSorting().add(sc);
		final SortColumnPanel scPanel = new SortColumnPanel(sc);
		final JComponent sep = createSeparator(scPanel);
		sortByPanel.add(sep);
		sortByPanel.add(scPanel);
		revalidate();
	}
	
	public void onRemoveColumn(SortColumnPanel scPanel) {
		sortByPanel.remove(scPanel);
		if(scPanel.getSeparator() != null)
			sortByPanel.remove(scPanel.getSeparator());
		settings.getSorting().remove(scPanel.getSortColumn());
		revalidate();
	}

	public SortNodeSettings getSettings() {
		return this.settings;
	}
	
	private JComponent createSeparator(SortColumnPanel scPanel) {
		final ImageIcon removeIcon =
				IconManager.getInstance().getDisabledIcon("actions/list-remove", IconSize.SMALL);
		final PhonUIAction removeAct = new PhonUIAction(this, "onRemoveColumn", scPanel);
		removeAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Remove sort column");
		removeAct.putValue(PhonUIAction.SMALL_ICON, removeIcon);
		final JButton removeButton = new JButton(removeAct);
		removeButton.setBorderPainted(false);
		
		final JPanel sep = new JPanel(new GridBagLayout());
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.insets = new Insets(0, 0, 0, 0);
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		sep.add(new JSeparator(SwingConstants.HORIZONTAL), gbc);
		
		gbc.weightx = 0.0;
		++gbc.gridx;
		sep.add(removeButton, gbc);
		
		scPanel.setSeparator(sep);
		
		return sep;
	}
	
	private PromptedTextField createColumnField() {
		final PromptedTextField retVal = new PromptedTextField();
		retVal.setPrompt("Enter column name or number");
		return retVal;
	}
	
	private JComboBox<SortType> createSortTypeBox() {
		final JComboBox<SortType> retVal = new JComboBox<>(SortType.values());
		retVal.setSelectedItem(null);
		return retVal;
	}
	
	private JComboBox<FeatureFamily> createFeatureBox() {
		final FeatureFamily[] boxVals = new FeatureFamily[FeatureFamily.values().length + 1];
		int idx = 0;
		boxVals[idx++] = null;
		for(FeatureFamily v:FeatureFamily.values()) boxVals[idx++] = v;
		
		final JComboBox<FeatureFamily> retVal = new JComboBox<>(boxVals);
		retVal.setSelectedItem(null);
		return retVal;
	}
	
	class SortColumnPanel extends JPanel {
		private PromptedTextField columnField = createColumnField();
		private JComboBox<SortType> typeBox = createSortTypeBox();
		
		// plain text options
		private JPanel orderOptions = new JPanel();
		private JRadioButton ascendingBox = new JRadioButton("Ascending");
		private JRadioButton descendingBox = new JRadioButton("Descending");
		
		private final SortColumn sortColumn;
		
		private JComponent separator;
		
		public SortColumnPanel(SortColumn sortColumn) {
			super();
			
			this.sortColumn = sortColumn;
			init();
		}
		
		private void init() {
			setLayout(new GridBagLayout());
			
			final GridBagConstraints gbc = new GridBagConstraints();
			gbc.anchor = GridBagConstraints.EAST;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.insets = new Insets(2, 2, 5, 2);
			
			columnField.setText(sortColumn.getColumn());
			columnField.getDocument().addDocumentListener(new DocumentListener() {
				
				@Override
				public void removeUpdate(DocumentEvent e) {
					updateColumn();
				}
				
				@Override
				public void insertUpdate(DocumentEvent e) {
					updateColumn();
				}
				
				@Override
				public void changedUpdate(DocumentEvent e) {
					
				}
			});
			gbc.gridy = 0;
			gbc.gridx = 0;
			gbc.weightx = 0.0;
			add(new JLabel("Column:"), gbc);
			
			gbc.gridx++;
			gbc.weightx = 1.0;
			add(columnField, gbc);
			
			gbc.weightx = 0.0;
			gbc.gridx = 0;
			gbc.gridy++;
			add(new JLabel("Sort type:"), gbc);
			
			typeBox.addItemListener( (e) -> {
				sortColumn.setType((SortType)typeBox.getSelectedItem());
			});
			typeBox.setSelectedItem(sortColumn.getType());
			gbc.gridx++;
			gbc.weightx = 1.0;
			add(typeBox, gbc);

			final ButtonGroup grp = new ButtonGroup();
			grp.add(ascendingBox);
			grp.add(descendingBox);
			ascendingBox.setSelected(sortColumn.getOrder() == SortOrder.ASCENDING);
			descendingBox.setSelected(sortColumn.getOrder() == SortOrder.DESCENDING);
			orderOptions.setLayout(new HorizontalLayout());
			
			final ChangeListener l = (e) -> {
				if(ascendingBox.isSelected())
					sortColumn.setOrder(SortOrder.ASCENDING);
				else
					sortColumn.setOrder(SortOrder.DESCENDING);
			};
			orderOptions.add(ascendingBox);
			orderOptions.add(descendingBox);
			ascendingBox.addChangeListener(l);
			descendingBox.addChangeListener(l);
			
			gbc.gridx = 1;
			gbc.gridy++;
			gbc.insets = new Insets(0, 0, 0, 0);
			add(orderOptions, gbc);
			
			add(new JSeparator(SwingConstants.HORIZONTAL));
		}
		
		void setSeparator(JComponent sep) {
			this.separator = sep;
		}
		
		JComponent getSeparator() {
			return this.separator;
		}
		
		public SortColumn getSortColumn() {
			return sortColumn;
		}
		
		public void updateColumn() {
			sortColumn.setColumn(columnField.getText().trim());
		}
	}
}
