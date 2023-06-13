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
package ca.phon.app.session.editor.view.record_data;

import ca.phon.app.session.editor.*;
import ca.phon.app.session.editor.search.*;
import ca.phon.app.session.editor.search.FindManager.*;
import ca.phon.app.session.editor.search.actions.*;
import ca.phon.app.session.editor.undo.TierEdit;
import ca.phon.app.session.editor.view.common.*;
import ca.phon.ipa.IPATranscript;
import ca.phon.ipa.alignment.*;
import ca.phon.session.Record;
import ca.phon.session.*;
import ca.phon.session.position.*;
import ca.phon.syllabifier.*;
import ca.phon.ui.toast.*;
import ca.phon.util.Language;
import ca.phon.util.Range;

import javax.swing.*;
import javax.swing.undo.CompoundEdit;
import java.awt.*;
import java.util.List;
import java.util.*;

public class FindAndReplacePanel extends JPanel {

	private final static String ANY_TIER_NAME = "__any_tier__";
	
	private final static String FIND_TIER_NAME = "__find__";
	
	private final static String REPLACE_TIER_NAME = "__replace_tier__";
	
	// center panel
	private TierDataLayoutPanel tierView;
	private JComboBox<String> tierNameBox;
	private GroupField<String> findField;
	private Tier<String> findTier;
	private FindOptionsPanel findOptsPanel;
	private GroupField<String> replaceField;
	private Tier<String> replaceTier;
	
	// rigt-hand side
	private JPanel findActionsPanel;
	private JCheckBox wrapBox;
	private JButton nextBtn;
	private JButton prevBtn;
	private JButton replaceBtn;
	private JButton replaceFindBtn;
	private JButton replaceAllBtn;
	
	private SessionEditor editor;
	
	// find manager
	private FindManager findManager;
	
	public FindAndReplacePanel(SessionEditor sessionEditor) {
		super();
		
		this.editor = sessionEditor;
		findManager = new FindManager(sessionEditor.getSession());
		
		init();
	}
	
	private void init() {
		setLayout(new BorderLayout());
		
		final SessionFactory factory = SessionFactory.newFactory();
		findTier = factory.createTier(FIND_TIER_NAME, String.class);
		findField = new GroupField<>(findTier);
		findField.addTierEditorListener(tierEditorListener);
		
		replaceTier = factory.createTier(REPLACE_TIER_NAME, String.class);
		replaceField = new GroupField<>(replaceTier);
		replaceField.addTierEditorListener(tierEditorListener);
		
		tierNameBox = new JComboBox<>();
		tierNameBox.setRenderer(new TierNameCellRenderer());
		updateTierNameBox();
		
		findOptsPanel = new FindOptionsPanel();
		
		tierView = new TierDataLayoutPanel();
		updateTierView();
		
		setupSidePanel();
		
		add(tierView, BorderLayout.CENTER);
		add(findActionsPanel, BorderLayout.EAST);
	}
	
	public FindManager getFindManager() {
		return this.findManager;
	}
	
	public SessionEditor getEditor() {
		return this.editor;
	}
	
	private void setupSidePanel() {
		findActionsPanel = new JPanel(new GridLayout(3, 2));
		
		nextBtn = new JButton(new FindNextAction(getEditor(), this));
		prevBtn = new JButton(new FindPrevAction(getEditor(), this));
		replaceBtn = new JButton(new ReplaceAction(getEditor(), this, false));
		replaceFindBtn = new JButton(new ReplaceAction(getEditor(), this, true));
		replaceAllBtn = new JButton(new ReplaceAllAction(getEditor(), this));
		
		wrapBox = new JCheckBox("Wrap search");
		wrapBox.setToolTipText("Wrap search when beginning/end of session is reached");
		wrapBox.setSelected(false);
		
		findActionsPanel.add(nextBtn);
		findActionsPanel.add(prevBtn);
		findActionsPanel.add(replaceBtn);
		findActionsPanel.add(replaceFindBtn);
		findActionsPanel.add(replaceAllBtn);
		findActionsPanel.add(wrapBox);
	}
	
	private void updateTierNameBox() {
		// update tier name combo box
		int selectedIdx = tierNameBox.getSelectedIndex();
		String selected = (selectedIdx >= 0 ? tierNameBox.getItemAt(selectedIdx).toString() : null);
		List<String> tierNames = new ArrayList<>();
		tierNames.add("Any tier");
		getEditor().getSession().getTierView()
			.stream()
			.filter( TierViewItem::isVisible )
			.filter( (tvi) -> !SystemTierType.Segment.getName().equals(tvi.getTierName()) )
			.map( TierViewItem::getTierName )
			.forEach( tierNames::add );
		ComboBoxModel<String> comboBoxModel = new DefaultComboBoxModel<>(tierNames.toArray(new String[0]));
		tierNameBox.setModel(comboBoxModel);
		if(selected != null && tierNames.contains(selected)) {
			tierNameBox.setSelectedItem(selected);
		} else {
			tierNameBox.setSelectedIndex(0);
		}
	}
	
	private void updateTierView() {
		tierView.removeAll();
		
		tierView.add(tierNameBox, new TierDataConstraint(TierDataConstraint.TIER_LABEL_COLUMN, 0));
		tierView.add(findField, new TierDataConstraint(TierDataConstraint.FLAT_TIER_COLUMN, 0));
		tierView.add(findOptsPanel, new TierDataConstraint(TierDataConstraint.FLAT_TIER_COLUMN, 1));

		JLabel replaceLbl = new JLabel("Replace");
		replaceLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		tierView.add(replaceLbl, new TierDataConstraint(TierDataConstraint.TIER_LABEL_COLUMN, 2));
		tierView.add(replaceField, new TierDataConstraint(TierDataConstraint.FLAT_TIER_COLUMN, 2));
	}
	
	/* Find and replace methods */
	private FindExpr getFindExpr() {
		final Tier<String> tier = findTier;
		if(tier == null || tier.getValue().length() == 0) return null;
				
		final FindExpr retVal = new FindExpr();
		retVal.setExpr(tier.getValue());
		retVal.setCaseSensitive(findOptsPanel.caseSensitiveBox.isSelected());
		retVal.setType((SearchType)findOptsPanel.typeBox.getSelectedItem());
		
		return retVal;
	}
	
	private List<String> getSearchTiers() {
		if(tierNameBox.getSelectedIndex() == 0) {
			List<String> tierNames = new ArrayList<>();
			for(int i = 1; i < tierNameBox.getModel().getSize(); i++) {
				tierNames.add(tierNameBox.getModel().getElementAt(i));
			}
			return tierNames;
		} else {
			return List.of(tierNameBox.getSelectedItem().toString());
		}
	}
		
	private void setupFindManager(FindManager findManager) {
		String queryText = findTier.getValue();
		
		if(tierNameBox.getSelectedIndex() == 0) {
			findManager.setAnyExpr(getFindExpr());
		} else {
			findManager.setAnyExpr(null);
			findManager.setExprForTier(tierNameBox.getSelectedItem().toString(), getFindExpr());
		}
		
		findManager.setSearchTier(getSearchTiers());
	}
	
	/*
	 * Return the 'start' location of the session.
	 * 
	 * @return startLocation
	 */
	private SessionLocation startLocation() {
		final FindManager findManager = getFindManager();
		
		final String tier = 
				(findManager.getSearchTiers().length > 0 ? findManager.getSearchTiers()[0] :
					SystemTierType.Orthography.getName());
		
		return new SessionLocation(0, new RecordLocation(tier, 0));
	}

	/*
	 * Return the end location when searching this session.
	 *  
	 * @return endLocation
	 */
	private SessionLocation endLocation() {
		final Session session = getEditor().getSession();
		final FindManager findManager = getFindManager();
		
		final String tierName = 
				(findManager.getSearchTiers().length > 0 ? findManager.getSearchTiers()[findManager.getSearchTiers().length-1] :
					SystemTierType.Notes.getName());
		final Record r = session.getRecord(session.getRecordCount()-1);
		final Tier<String> tier = r.getTier(tierName, String.class);
		return new SessionLocation(session.getRecordCount()-1, new RecordLocation(tierName, tier.getValue().length()));
	}
	
	private void setupSessionSelection(SessionRange sessionRange) {
		final SessionEditorSelection selection = 
				new SessionEditorSelection(sessionRange.getRecordIndex(), sessionRange.getRecordRange().getTier(),
						sessionRange.getRecordRange().getRange());
		getEditor().getSelectionModel().setSelection(selection);
		getEditor().setCurrentRecordIndex(sessionRange.getRecordIndex());
	}
	
	public void findNext() {
		final FindManager findManager = getFindManager();
		setupFindManager(findManager);
		
		if(findManager.getCurrentLocation() == null) {
			findManager.setCurrentLocation(
					findManager.getDirection() == FindDirection.FORWARDS ? startLocation() : endLocation());
		}
		
		SessionRange nextInstance = findManager.findNext();
		if(nextInstance != null) {
			setupSessionSelection(nextInstance);
		} else if(findManager.getDirection() == FindDirection.FORWARDS &&
				findManager.getStatus() == FindStatus.HIT_END) {
			if(wrapBox.isSelected()) {
				findManager.setCurrentLocation(startLocation());
				nextInstance = findManager.findNext();
				if(nextInstance == null) {
					Toolkit.getDefaultToolkit().beep();
				} else {
					setupSessionSelection(nextInstance);
				}
			} else {
				Toolkit.getDefaultToolkit().beep();
			}
		} else if(findManager.getDirection() == FindDirection.BACKWARDS &&
				findManager.getStatus() == FindStatus.HIT_BEGINNING) {
			if(wrapBox.isSelected()) {
				findManager.setCurrentLocation(endLocation());
				nextInstance = findManager.findNext();
				if(nextInstance == null) {
					Toolkit.getDefaultToolkit().beep();
				} else {
					setupSessionSelection(nextInstance);
				}
			} else {
				Toolkit.getDefaultToolkit().beep();
			}
		}
	}
	
	public void findPrev() {
		final FindManager findManager = getFindManager();
		setupFindManager(findManager);
		
		if(findManager.getCurrentLocation() == null) {
			findManager.setCurrentLocation(
					findManager.getDirection() == FindDirection.FORWARDS ? endLocation() : startLocation());
		}
		
		
		SessionRange nextInstance = findManager.findPrev();
		if(nextInstance != null) {
			setupSessionSelection(nextInstance);
		} else if(findManager.getDirection() == FindDirection.FORWARDS &&
				findManager.getStatus() == FindStatus.HIT_BEGINNING) {
			if(wrapBox.isSelected()) {
				findManager.setCurrentLocation(endLocation());
				nextInstance = findManager.findPrev();
				if(nextInstance == null) {
					Toolkit.getDefaultToolkit().beep();
				} else {
					setupSessionSelection(nextInstance);
				}
			} else {
				Toolkit.getDefaultToolkit().beep();
			}
		} else if(findManager.getDirection() == FindDirection.BACKWARDS &&
				findManager.getStatus() == FindStatus.HIT_END) {
			if(wrapBox.isSelected()) {
				findManager.setCurrentLocation(startLocation());
				nextInstance = findManager.findPrev();
				if(nextInstance == null) {
					Toolkit.getDefaultToolkit().beep();
				} else {
					setupSessionSelection(nextInstance);
				}
			} else {
				Toolkit.getDefaultToolkit().beep();
			}
		}
	}
	
	public void replace() {
		final FindManager findManager = getFindManager();
		setupFindManager(findManager);
		if(findManager.getMatchedExpr() != null && findManager.getMatchedRange() != null) {
			if(replaceTier != null && replaceTier.hasValue()) {
				final String replaceExpr = replaceTier.getValue();
				final Object newVal = findManager.getMatchedExpr().replace(replaceExpr);
				// re-syllabify if an IPA tier
				final SessionRange sr = findManager.getMatchedRange();
				final Record record = getEditor().getSession().getRecord(sr.getRecordIndex());
				@SuppressWarnings({ "unchecked", "rawtypes" })
				final TierEdit<?> tierEdit = new TierEdit(getEditor(), 
						getEditor().currentRecord().getTier(sr.getRecordRange().getTier()), newVal);
				if(newVal instanceof IPATranscript) {
					final IPATranscript ipa = (IPATranscript)newVal;
					final Syllabifier syllabifier = getSyllabifier(sr.getRecordRange().getTier());
					if(syllabifier != null) {
						syllabifier.syllabify(ipa.toList());
					}
					// update alignment
					final CompoundEdit edit = new CompoundEdit();
					final PhoneAlignment phoneAlignment = PhoneAlignment.fromTiers(record.getIPATargetTier(), record.getIPAActualTier());
					final TierEdit<PhoneAlignment> alignmentEdit = new TierEdit<>(getEditor(), record.getPhoneAlignmentTier(), phoneAlignment);
					tierEdit.doIt();
					edit.addEdit(tierEdit);
					alignmentEdit.doIt();
					edit.addEdit(alignmentEdit);
					edit.end();
					getEditor().getUndoSupport().postEdit(edit);
				} else {
					getEditor().getUndoSupport().postEdit(tierEdit);
				}
				getEditor().getSelectionModel().clear();
			}
		}
	}
	
	private Syllabifier getSyllabifier(String tier) {
		Syllabifier retVal = SyllabifierLibrary.getInstance().defaultSyllabifier();
		final Session session = getEditor().getSession();
		final SyllabifierInfo info = session.getExtension(SyllabifierInfo.class);
		if(info != null) {
			final Language lang = info.getSyllabifierLanguageForTier(tier);
			if(lang != null && SyllabifierLibrary.getInstance().availableSyllabifierLanguages().contains(lang)) {
				retVal = SyllabifierLibrary.getInstance().getSyllabifierForLanguage(lang);
			}
		}
		return retVal;
	}
	
	public void replaceAll() {
		final String replaceExpr = replaceTier.getValue();
		// create a new find manager
		final Session session = getEditor().getSession();
		final FindManager findManager = new FindManager(session);
		setupFindManager(findManager);
		final SessionLocation startLoc = 
				new SessionLocation(0, new RecordLocation(findManager.getSearchTiers()[0], 0));
		findManager.setCurrentLocation(startLoc);

		final CompoundEdit edit = new CompoundEdit();
		int occurrences = 0;
		
		SessionRange currentRange = null;
		while((currentRange = findManager.findNext()) != null) {
			++occurrences;
			
			final Record r = session.getRecord(currentRange.getRecordIndex());
			final Tier<?> tier = r.getTier(currentRange.getRecordRange().getTier());
			
			final Object newVal = findManager.getMatchedExpr().replace(replaceExpr);
			
			// re-syllabify if an IPA tier
			@SuppressWarnings({ "unchecked", "rawtypes" })
			final TierEdit<?> tierEdit = new TierEdit(getEditor(), tier, newVal);
			tierEdit.doIt();
			edit.addEdit(tierEdit);
			
			if(newVal instanceof IPATranscript) {
				final IPATranscript ipa = (IPATranscript)newVal;
				final Syllabifier syllabifier = getSyllabifier(currentRange.getRecordRange().getTier());
				if(syllabifier != null) {
					syllabifier.syllabify(ipa.toList());
				}
				
				// update alignment
				final PhoneAlignment phoneAlignment = PhoneAlignment.fromTiers(r.getIPATargetTier(), r.getIPAActualTier());
				final TierEdit<PhoneAlignment> alignmentEdit = new TierEdit<>(getEditor(), r.getPhoneAlignmentTier(), phoneAlignment);
				alignmentEdit.doIt();
				edit.addEdit(alignmentEdit);
				edit.end();
			}
		}
		edit.end();
		
		if(occurrences > 0) {
			final EditorEvent<Void> ee = new EditorEvent<>(new EditorEventType<>(EditorEventName.MODIFICATION_EVENT.getEventName(), Void.class), this, null);
			getEditor().getEventManager().queueEvent(ee);
			final EditorEvent<EditorEventType.RecordChangedData> refresh = new EditorEvent<>(EditorEventType.RecordRefresh, this, new EditorEventType.RecordChangedData(getEditor().getCurrentRecordIndex(), getEditor().currentRecord()));
			getEditor().getEventManager().queueEvent(refresh);
			getEditor().getUndoSupport().postEdit(edit);
		}
		
		final String message = 
				"Replaced " + occurrences + " occurrences with " + replaceExpr;
		final Toast toast = ToastFactory.makeToast(message);
		toast.start(replaceAllBtn);
	}
	
	private class TierNameCellRenderer extends DefaultListCellRenderer {

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			JLabel retVal = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			retVal.setHorizontalAlignment(SwingConstants.RIGHT);
			return retVal;
		}
		
	}
	
	private class FindOptionsPanel extends JPanel {
		
		JCheckBox caseSensitiveBox;
		
		JComboBox<SearchType> typeBox;
		
		public FindOptionsPanel() {
			super();
			init();
			
			setOpaque(false);
			caseSensitiveBox.setOpaque(false);
		}
		
		private void init() {
			setLayout(new FlowLayout(FlowLayout.LEFT));
			
			caseSensitiveBox = new JCheckBox("Case sensitive");
			caseSensitiveBox.setOpaque(false);
			add(caseSensitiveBox);
			
			typeBox = new JComboBox<>(SearchType.values());
			typeBox.setSelectedItem(SearchType.PLAIN);
			add(typeBox);
		}
		
	}
	
	private final TierEditorListener<String> tierEditorListener = (tier, newValue, oldValue, valueIsAdjusting) -> {
		if(valueIsAdjusting)
			tier.setValue(newValue);
	};

}
