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
package ca.phon.app.project;

import ca.phon.app.log.LogUtil;
import ca.phon.plugin.*;
import ca.phon.project.Project;
import ca.phon.util.icons.*;
import com.jgoodies.forms.factories.DefaultComponentFactory;
import com.jgoodies.forms.layout.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;

public class NewSessionPanel extends JPanel {

	private JTextField txtName = new JTextField();
	private JComboBox cmbCorpus = new JComboBox();
	private JButton btnCreateCorpus = new JButton();

	private Project proj;
		
	/**
	 * Default constructor
	 */
	public NewSessionPanel(Project project) {
		super();
		this.proj = project;
		initializePanel();
	}
	
	/**
	 * Constructor. Default selects the corpus.
	 */
	public NewSessionPanel(Project project, String corpusName) {
		this(project);
		cmbCorpus.setSelectedItem(corpusName);
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		txtName.setEnabled(enabled);
		cmbCorpus.setEnabled(enabled);
		btnCreateCorpus.setEnabled(enabled);
	}

	/**
	 * Adds fill components to empty cells in the first row and first column
	 * of the grid. This ensures that the grid spacing will be the same as
	 * shown in the designer.
	 * 
	 * @param cols
	 *            an array of column indices in the first row where fill
	 *            components should be added.
	 * @param rows
	 *            an array of row indices in the first column where fill
	 *            components should be added.
	 */
	void addFillComponents(Container panel, int[] cols, int[] rows) {
		Dimension filler = new Dimension(10, 10);

		boolean filled_cell_11 = false;
		CellConstraints cc = new CellConstraints();
		if (cols.length > 0 && rows.length > 0) {
			if (cols[0] == 1 && rows[0] == 1) {
				/** add a rigid area */
				panel.add(Box.createRigidArea(filler), cc.xy(1, 1));
				filled_cell_11 = true;
			}
		}

		for (int index = 0; index < cols.length; index++) {
			if (cols[index] == 1 && filled_cell_11)
				continue;
			panel.add(Box.createRigidArea(filler), cc.xy(cols[index], 1));
		}

		for (int index = 0; index < rows.length; index++) {
			if (rows[index] == 1 && filled_cell_11)
				continue;
			panel.add(Box.createRigidArea(filler), cc.xy(1, rows[index]));
		}
	}

	public JPanel createPanel() {
		JPanel jpanel1 = new JPanel();
		FormLayout formlayout1 = new FormLayout(
				"CENTER:25PX:NONE,FILL:DEFAULT:GROW(1.0),CENTER:DEFAULT:NONE",
				"CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:20PX:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE,CENTER:20PX:NONE,CENTER:DEFAULT:NONE");
		CellConstraints cc = new CellConstraints();
		jpanel1.setLayout(formlayout1);
		jpanel1.setBorder(new EmptyBorder(5, 5, 5, 5));

		DefaultComponentFactory fac = DefaultComponentFactory.getInstance();

		JComponent titledseparator1 = fac.createSeparator("Step 1");
		jpanel1.add(titledseparator1, cc.xywh(1, 1, 3, 1));

		JComponent titledseparator2 = fac.createSeparator("Step 2");
		jpanel1.add(titledseparator2, cc.xywh(1, 5, 3, 1));

		JLabel jlabel1 = new JLabel();
		jlabel1.setText("Enter a name for the new session:");
		jpanel1.add(jlabel1, cc.xywh(2, 2, 2, 1));

		txtName.setName("txtName");
		jpanel1.add(txtName, cc.xywh(2, 3, 2, 1));

		cmbCorpus.setName("cmbCorpus");
		jpanel1.add(cmbCorpus, cc.xy(2, 7));

//			ImageFactory imgFactory = ImageFactory.getInstance();
//			ImageIcon im = new ImageIcon(imgFactory.getImage("new_corpus", 16, 16));
		ImageIcon im = IconManager.getInstance().getIcon(
				"actions/list-add", IconSize.SMALL);
		btnCreateCorpus.setIcon(im);
		btnCreateCorpus.setName("btnCreateCorpus");
		btnCreateCorpus.addActionListener(new CreateCorpusListener());
		jpanel1.add(btnCreateCorpus, cc.xy(3, 7));

		JLabel jlabel2 = new JLabel();
		jlabel2.setText("Select a corpus to use for this session:");
		jpanel1.add(jlabel2, cc.xy(2, 6));

		addFillComponents(jpanel1, new int[] { 2,3 }, new int[] { 2,3,4,6,7,8 });
		return jpanel1;
	}

	/**
	 * Initializer
	 */
	protected void initializePanel() {
		setLayout(new BorderLayout());
		add(createPanel(), BorderLayout.CENTER);
		
		updateCorporaList();
	}

	/**
	 * Updates the corpora list with the current project's corpora names.
	 */
	private void updateCorporaList() {
		// Clear out the combo box
		cmbCorpus.removeAllItems();
		
		final List<String> corporaNames = proj.getCorpora();
		for (String corpusName : corporaNames)
			cmbCorpus.addItem(corpusName);
	}

	public void setSelectedCorpus(String corpus) {
		cmbCorpus.setSelectedItem(corpus);
	}
	
	public String getSelectedCorpus() {
		return cmbCorpus.getSelectedItem().toString();
	}

	public String getSessionName() {
		return txtName.getText();
	}
	
	/**
	 * Create corpus button listener.
	 */
	private class CreateCorpusListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent evt) {
			HashMap<String, Object> initInfo = new HashMap<String, Object>();
			initInfo.put("project", proj);
			
			try {
				PluginEntryPointRunner.executePlugin("NewCorpus", initInfo);
			} catch (PluginException e) {
				LogUtil.warning(e);
			}

			updateCorporaList();
		}
	}

}
