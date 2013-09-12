/*
 * Phon - An open source tool for research in phonology.
 * Copyright (C) 2008 The Phon Project, Memorial University <http://phon.ling.mun.ca>
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
package ca.phon.app.project.checkwizard;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ca.phon.app.prefs.PhonProperties;
import ca.phon.app.session.SessionSelector;
import ca.phon.media.player.PhonPlayerCanvas;
import ca.phon.project.Project;
import ca.phon.session.SessionLocation;
import ca.phon.syllabifier.Syllabifier;
import ca.phon.syllabifier.SyllabifierLibrary;
import ca.phon.ui.decorations.DialogHeader;
import ca.phon.ui.toast.Toast;
import ca.phon.ui.toast.ToastFactory;
import ca.phon.ui.wizard.WizardStep;
import ca.phon.util.Language;
import ca.phon.util.PrefHelper;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Step 1 - Check IPA wizard
 * 
 * Select sessions and actions to perform.
 *
 */
public class CheckWizardStep1 extends WizardStep {
	
	private static final long serialVersionUID = -1527112442759878465L;

	public static enum Operation {
		CHECK_IPA,
		RESET_SYLLABIFICATION,
		RESET_ALIGNMENT;
	}
	
	private DialogHeader header;
	
	private SessionSelector sessionSelector;
	
	private JRadioButton checkIPAButton;
	
	private JRadioButton resetSyllabificationButton;
	
	private JCheckBox resetAlignmentBox;
	
	private JRadioButton resetAlignmentButton;
	
	private JComboBox<Language> syllabifierList;
	
	private Project project;
	
	public CheckWizardStep1(Project project) {
		super();
		
		this.project = project;
		
		init();
	}
	
	private void init() {
		setLayout(new BorderLayout());
		
		header = new DialogHeader("Check Transcriptions", 
				"Select sessions and operations    to perform.");
		add(header, BorderLayout.NORTH);
		
		JPanel topPanel = new JPanel();
		FormLayout topLayout = new FormLayout(
				"20px, pref, pref:grow", 
				"pref, pref, pref, pref, pref");
		CellConstraints cc = new CellConstraints();
		topPanel.setLayout(topLayout);
		
		final SyllabifierLibrary syllabifierLibrary = SyllabifierLibrary.getInstance();
		final List<Language> orderedSyllabifiers = new ArrayList<>(syllabifierLibrary.availableSyllabifierLanguages());
	    Collections.sort(orderedSyllabifiers);
	    
	    syllabifierList = new JComboBox<>(orderedSyllabifiers.toArray(new Language[0]));
	    syllabifierList.setEnabled(false);
	   
	    final String preferredSyllabifier = PrefHelper.get(
	    		PhonProperties.SYLLABIFIER_LANGUAGE,
	    		PhonProperties.DEFAULT_SYLLABIFIER_LANGUAGE);
	    syllabifierList.setSelectedItem(preferredSyllabifier);
		
		checkIPAButton = new JRadioButton("Check IPA Tiers");
		checkIPAButton.setToolTipText("Check IPA tiers for invalid transcriptions.");
		resetSyllabificationButton = new JRadioButton("Reset syllabification");
		
		resetSyllabificationButton.setToolTipText("Reset syllabification for all IPA tiers in selected sessions.");
		resetAlignmentBox = new JCheckBox("also reset phone alignment");
		resetAlignmentBox.setEnabled(false);
		
		resetAlignmentButton = new JRadioButton("Reset phone alignment");
		resetAlignmentButton.setToolTipText("Reset alignment for all records in selected sessions.");
		
		ButtonGroup btnGroup = new ButtonGroup();
		btnGroup.add(checkIPAButton);
		btnGroup.add(resetSyllabificationButton);
		btnGroup.add(resetAlignmentButton);
		
		resetSyllabificationButton.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent arg0) {
				resetAlignmentBox.setEnabled(resetSyllabificationButton.isSelected());
				syllabifierList.setEnabled(resetSyllabificationButton.isSelected());
			}
		});

		checkIPAButton.setSelected(true);
		
		topPanel.add(checkIPAButton, cc.xyw(1,1,3));
		topPanel.add(resetSyllabificationButton, cc.xyw(1,2,2));
		topPanel.add(syllabifierList, cc.xy(3, 2));
		topPanel.add(resetAlignmentBox, cc.xy(2,4));
		topPanel.add(resetAlignmentButton, cc.xyw(1,5,3));
		
		topPanel.setBorder(BorderFactory.createTitledBorder("Operation"));
		
		JPanel centerPanel = new JPanel(new BorderLayout());
		sessionSelector = new SessionSelector(project);
		centerPanel.add(new JScrollPane(sessionSelector), BorderLayout.CENTER);
		
		centerPanel.setBorder(BorderFactory.createTitledBorder("Select sessions"));
		
		JPanel innerPanel = new JPanel(new BorderLayout());
		innerPanel.add(topPanel, BorderLayout.NORTH);
		innerPanel.add(centerPanel, BorderLayout.CENTER);
		
		super.add(innerPanel, BorderLayout.CENTER);
	}

	public Operation getOperation() {
		Operation retVal = Operation.CHECK_IPA;
		
		if(resetSyllabificationButton.isSelected()) {
			retVal = Operation.RESET_SYLLABIFICATION;
		} else if(resetAlignmentButton.isSelected()) {
			retVal = Operation.RESET_ALIGNMENT;
		}
		
		return retVal;
	}
	
	public boolean isResetAlignment() {
		return resetAlignmentBox.isSelected();
	}
	
	public List<SessionLocation> getSelectedSessions() {
		List<SessionLocation> locations = sessionSelector.getSelectedSessions();
		Collections.sort(locations);
		return locations;
	}
	
	public Syllabifier getSyllabifier() {
		final SyllabifierLibrary library = SyllabifierLibrary.getInstance();
		return library.getSyllabifierForLanguage((Language)syllabifierList.getSelectedItem());
	}
	
	@Override
	public boolean validateStep() {
		boolean retVal = true;
		
		if(getSelectedSessions().size() == 0) {
			final Toast toast = ToastFactory.makeToast("Please select at least one session");
			toast.start(sessionSelector);
			retVal = false;
		}
		
		return retVal;
	}
}
