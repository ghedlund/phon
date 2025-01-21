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
package ca.phon.app.session.editor.view.syllabificationAlignment;

import ca.phon.app.session.editor.*;
import ca.phon.app.session.editor.undo.TierEdit;
import ca.phon.app.session.editor.view.common.*;
import ca.phon.app.session.editor.view.syllabificationAlignment.actions.*;
import ca.phon.app.session.editor.view.transcript.TranscriptEditor;
import ca.phon.app.session.editor.view.transcript.TranscriptScrollPane;
import ca.phon.ipa.IPATranscript;
import ca.phon.ipa.alignment.PhoneMap;
import ca.phon.session.Record;
import ca.phon.session.*;
import ca.phon.syllable.SyllableConstituentType;
import ca.phon.ui.DropDownButton;
import ca.phon.ui.FlatButton;
import ca.phon.ui.IconStrip;
import ca.phon.ui.action.PhonUIAction;
import ca.phon.ui.fonts.FontPreferences;
import ca.phon.ui.ipa.*;
import ca.phon.ui.ipa.PhoneMapDisplay.AlignmentChangeData;
import ca.phon.ui.ipa.SyllabificationDisplay.SyllabificationChangeData;
import ca.phon.ui.menu.MenuBuilder;
import ca.phon.util.PrefHelper;
import ca.phon.util.icons.*;
import com.jgoodies.forms.layout.*;

import javax.swing.*;
import java.awt.*;
import java.beans.*;
import java.util.List;
import java.util.*;

public class SyllabificationAlignmentEditorView extends EditorView {

	public record ScEditData(IPATranscript ipa, int eleIdx, SyllableConstituentType oldType, SyllableConstituentType newType) { }
	public final static EditorEventType<ScEditData> ScEdit = new EditorEventType<>(EditorEventName.MODIFICATION_EVENT + "_SC_TYPE_", ScEditData.class);

	public final static String VIEW_NAME = "Syllabification & Alignment";

	public final static String VIEW_ICON = IconManager.GoogleMaterialDesignIconsFontName + ":indeterminate_question_box";

	private IconStrip toolbar;

	private final static String SHOW_TARGET_IPA = "SyllabificationAndAlignmentEditorView.showTargetIPA";
	private final static boolean DEFAULT_SHOW_TARGET_IPA = true;
	private JCheckBox targetIPABox;

	private final static String SHOW_ACTUAL_IPA = "SyllabificationAndAlignmentEditorView.showActualIPA";
	private final static boolean DEFAULT_SHOW_ACTUAL_IPA = true;
	private JCheckBox actualIPABox;

	private final static String SHOW_ALIGNMENT = "SyllabificationAndAlignmentEditorView.showAlignment";
	private final static boolean DEFAULT_SHOW_ALIGNMENT = true;
	private JCheckBox alignmentBox;

	private final static String COLOR_IN_ALIGNMENT = "SyllabificationAndAlignmentEditorView.colorInAlignment";
	private final static boolean DEFAULT_COLOR_IN_ALIGNMENT = false;
	private JCheckBox colorInAlignmentBox;

	private final static String SHOW_DIACRITICS = "SyllabificationAndAlignmentEditorView.showDiacritics";
	private final static boolean DEFAULT_SHOW_DIACRITICS = false;
	private JCheckBox showDiacriticsBox;

	private TranscriptScrollPane scrollPane;
	private TranscriptEditor editor;

	// components
	private SyllabificationDisplay targetDisplay;
	private SyllabificationDisplay actualDisplay;
	private PhoneMapDisplay alignmentDisplay;

	public SyllabificationAlignmentEditorView(SessionEditor editor) {
		super(editor);
		init();
		setupEditorActions();
	}

	private void init() {
		// toolbar
		toolbar = new IconStrip(SwingConstants.HORIZONTAL);

		ImageIcon sigmaIcn = IconManager.getInstance().getIcon("misc/small_sigma", IconSize.SMALL);

		final PhonUIAction<Void> syllabifierSettingsAct = PhonUIAction.runnable(() -> {
			final JPopupMenu settingsMenu = new JPopupMenu();
			final MenuBuilder menuBuilder = new MenuBuilder(settingsMenu);
		});
		syllabifierSettingsAct.putValue(PhonUIAction.NAME, "Settings");
		syllabifierSettingsAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Select syllabifier settings for session");
		syllabifierSettingsAct.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
		syllabifierSettingsAct.putValue(FlatButton.ICON_NAME_PROP, "settings");
		syllabifierSettingsAct.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);

//		final SyllabifierInfo syllabifierInfo = getEditor().getSession().getExtension(SyllabifierInfo.class);
//		SyllabificationSettingsPanel popupPanel = new SyllabificationSettingsPanel(syllabifierInfo);
//		popupPanel.addPropertyChangeListener(SyllabificationSettingsPanel.IPA_TARGET_SYLLABIFIER_PROP, (e) -> {
//			syllabifierInfo.saveInfo(getEditor().getSession());
//		});
//		popupPanel.addPropertyChangeListener(SyllabificationSettingsPanel.IPA_ACTUAL_SYLLABIFIER_PROP, (e) -> {
//			syllabifierInfo.saveInfo(getEditor().getSession());
//		});
//		syllabifierSettingsAct.putValue(DropDownButton.BUTTON_POPUP, popupPanel);
//		syllabifierSettingsAct.putValue(DropDownButton.ARROW_ICON_GAP, 0);
//		syllabifierSettingsAct.putValue(DropDownButton.ARROW_ICON_POSITION, SwingConstants.BOTTOM);

		FlatButton settingsBtn = new FlatButton(syllabifierSettingsAct);

		toolbar.add(settingsBtn, IconStrip.IconStripPosition.LEFT);

		setLayout(new BorderLayout());
//		scroller = new JScrollPane(contentPane);
//		scroller.setBackground(Color.white);
//		scroller.setOpaque(true);
		add(toolbar, BorderLayout.NORTH);
//		add(scroller, BorderLayout.CENTER);

		update();
	}

	private void setupSettingsMenu(MenuBuilder menuBuilder) {
		final PhonUIAction<Void> toggleTargetAct = PhonUIAction.runnable(this::toggleCheckbox);
		toggleTargetAct.putValue(PhonUIAction.NAME, SystemTierType.TargetSyllables.getName());
		toggleTargetAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Toggle target syllables");
		toggleTargetAct.putValue(PhonUIAction.SELECTED_KEY, PrefHelper.getBoolean(SHOW_TARGET_IPA, DEFAULT_SHOW_TARGET_IPA));
		menuBuilder.addItem(".", new JCheckBoxMenuItem(toggleTargetAct));

		final PhonUIAction<Void> toggleActualAct = PhonUIAction.runnable(this::toggleCheckbox);
		toggleActualAct.putValue(PhonUIAction.NAME, SystemTierType.ActualSyllables.getName());
		toggleActualAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Toggle actual syllables");
		toggleActualAct.putValue(PhonUIAction.SELECTED_KEY, PrefHelper.getBoolean(SHOW_ACTUAL_IPA, DEFAULT_SHOW_ACTUAL_IPA));
		menuBuilder.addItem(".", new JCheckBoxMenuItem(toggleActualAct));

		final PhonUIAction<Void> toggleAlignmentAct = PhonUIAction.runnable(this::toggleCheckbox);
		toggleAlignmentAct.putValue(PhonUIAction.NAME, SystemTierType.PhoneAlignment.getName());
		toggleAlignmentAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Toggle alignment");
		toggleAlignmentAct.putValue(PhonUIAction.SELECTED_KEY, PrefHelper.getBoolean(SHOW_ALIGNMENT, DEFAULT_SHOW_ALIGNMENT));
		menuBuilder.addItem(".", new JCheckBoxMenuItem(toggleAlignmentAct));

		final PhonUIAction<Void> toggleAlignmentColorAct = PhonUIAction.runnable(this::toggleCheckbox);
		toggleAlignmentColorAct.putValue(PhonUIAction.NAME, "Color in alignment");
		toggleAlignmentColorAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Toggle color in alignment");
		toggleAlignmentColorAct.putValue(PhonUIAction.SELECTED_KEY, PrefHelper.getBoolean(COLOR_IN_ALIGNMENT, DEFAULT_COLOR_IN_ALIGNMENT));
		menuBuilder.addItem(".", new JCheckBoxMenuItem(toggleAlignmentColorAct));

		final PhonUIAction<Void> toggleDiacriticsAct = PhonUIAction.runnable(this::toggleCheckbox);
		toggleDiacriticsAct.putValue(PhonUIAction.NAME, "Show diacritics");
		toggleDiacriticsAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Toggle display of diacritics");
		toggleDiacriticsAct.putValue(PhonUIAction.SELECTED_KEY, PrefHelper.getBoolean(SHOW_DIACRITICS, DEFAULT_SHOW_DIACRITICS));
		menuBuilder.addItem(".", new JCheckBoxMenuItem(toggleDiacriticsAct));
	}

	private void setupEditorActions() {
		final SessionEditor editor = getEditor();
		final EditorEventManager eventManager = editor.getEventManager();

		eventManager.registerActionForEvent(EditorEventType.SessionChanged, this::onSessionChanged, EditorEventManager.RunOn.AWTEventDispatchThread);

		eventManager.registerActionForEvent(EditorEventType.RecordChanged, this::onRecordChanged, EditorEventManager.RunOn.AWTEventDispatchThread);
		eventManager.registerActionForEvent(EditorEventType.RecordRefresh, this::onRecordChanged, EditorEventManager.RunOn.AWTEventDispatchThread);

		eventManager.registerActionForEvent(EditorEventType.TierChange, this::onTierChanged, EditorEventManager.RunOn.AWTEventDispatchThread);

		eventManager.registerActionForEvent(ScEdit, this::onScChange, EditorEventManager.RunOn.AWTEventDispatchThread);
	}

	private SyllabificationDisplay getIPATargetDisplay() {
		if(this.targetDisplay == null) {
			this.targetDisplay = new SyllabificationDisplay();
			this.targetDisplay.addPropertyChangeListener(SyllabificationDisplay.SYLLABIFICATION_PROP_ID, syllabificationDisplayListener);
			this.targetDisplay.addPropertyChangeListener(SyllabificationDisplay.HIATUS_CHANGE_PROP_ID, hiatusChangeListener);
		}
		return this.targetDisplay;
	}

	private SyllabificationDisplay getIPAActualDisplay() {
		if(this.actualDisplay == null) {
			this.actualDisplay = new SyllabificationDisplay();
			this.actualDisplay.addPropertyChangeListener(SyllabificationDisplay.SYLLABIFICATION_PROP_ID, syllabificationDisplayListener);
			this.actualDisplay.addPropertyChangeListener(SyllabificationDisplay.HIATUS_CHANGE_PROP_ID, hiatusChangeListener);
		}
		return this.actualDisplay;
	}

	private final PropertyChangeListener syllabificationDisplayListener = new PropertyChangeListener() {

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			final SyllabificationChangeData newVal = (SyllabificationChangeData)evt.getNewValue();
			final SyllabificationDisplay display = (SyllabificationDisplay)evt.getSource();
			final ScTypeEdit edit = new ScTypeEdit(getEditor(), display.getTranscript(), newVal.getPosition(), newVal.getScType());
			getEditor().getUndoSupport().postEdit(edit);
		}

	};

	private final PropertyChangeListener hiatusChangeListener = new PropertyChangeListener() {

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			final SyllabificationDisplay display = (SyllabificationDisplay)evt.getSource();
			final ToggleDiphthongEdit edit = new ToggleDiphthongEdit(getEditor(), display.getTranscript(), (Integer)evt.getNewValue());
			getEditor().getUndoSupport().postEdit(edit);
		}

	};

	private PhoneMapDisplay getAlignmentDisplay() {
		if(this.alignmentDisplay == null) {
			this.alignmentDisplay = new PhoneMapDisplay();
			this.alignmentDisplay.addPropertyChangeListener(PhoneMapDisplay.ALIGNMENT_CHANGE_PROP, alignmentDisplayListener);
		}
		return this.alignmentDisplay;
	}

	private final PropertyChangeListener alignmentDisplayListener = new PropertyChangeListener() {

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			final AlignmentChangeData newVal = (AlignmentChangeData)evt.getNewValue();
			final Record r = getEditor().currentRecord();
			final int wIdx = newVal.getWordIndex();
			final List<IPATranscript> targetWords = targetDisplay.getTranscript().words();
			final List<IPATranscript> actualWords = actualDisplay.getTranscript().words();
			final IPATranscript ipaTarget = wIdx < targetWords.size() ? targetWords.get(wIdx) : new IPATranscript();
			final IPATranscript ipaActual = wIdx < actualWords.size() ? actualWords.get(wIdx) : new IPATranscript();
			final PhoneMap pm = new PhoneMap(ipaTarget, ipaActual);
			pm.setTopAlignment(newVal.getAlignment()[0]);
			pm.setBottomAlignment(newVal.getAlignment()[1]);

			final PhoneAlignment phoneAlignment = PhoneAlignment.fromTiers(r.getIPATargetTier(), r.getIPAActualTier());
			final List<PhoneMap> modifiedAlignments = new ArrayList<>();
			for(int i = 0; i < phoneAlignment.getAlignments().size(); i++) {
				if(i == wIdx)
					modifiedAlignments.add(pm);
				else
					modifiedAlignments.add(phoneAlignment.getAlignments().get(i));
			}

			final TierEdit<PhoneAlignment> edit = new TierEdit<>(getEditor(), r.getPhoneAlignmentTier(),
					new PhoneAlignment(modifiedAlignments));
			getEditor().getUndoSupport().postEdit(edit);
		}

	};

	public void update() {
//		final boolean showTarget = targetIPABox.isSelected();
//		PrefHelper.getUserPreferences().putBoolean(SHOW_TARGET_IPA, showTarget);
//		final boolean showActual = actualIPABox.isSelected();
//		PrefHelper.getUserPreferences().putBoolean(SHOW_ACTUAL_IPA, showActual);
//		final boolean showAlignment = alignmentBox.isSelected();
//		PrefHelper.getUserPreferences().putBoolean(SHOW_ALIGNMENT, showAlignment);
//		final boolean colorInAlignment = colorInAlignmentBox.isSelected();
//		PrefHelper.getUserPreferences().putBoolean(COLOR_IN_ALIGNMENT, colorInAlignment);
//		final boolean showDiacritics = showDiacriticsBox.isSelected();
//		PrefHelper.getUserPreferences().putBoolean(SHOW_DIACRITICS, showDiacritics);
//
//		final SessionEditor editor = getEditor();
//		final Record record = editor.currentRecord();
//		if(record == null) return;

//		contentPane.removeAll();
//		final ImageIcon reloadIcn = IconManager.getInstance().getIcon("actions/reload", IconSize.SMALL);
//
//		if(showTarget) {
//			final ResetSyllabificationCommand resetTargetAct = new ResetSyllabificationCommand(getEditor(), this, SystemTierType.IPATarget.getName());
//			resetTargetAct.putValue(Action.NAME, null);
//			resetTargetAct.putValue(Action.SMALL_ICON, reloadIcn);
//			final JButton btn = new JButton(resetTargetAct);
//
//			final JLabel targetSyllLbl = new JLabel(SystemTierType.TargetSyllables.getName());
//			targetSyllLbl.setHorizontalAlignment(SwingConstants.RIGHT);
//			targetSyllLbl.setHorizontalTextPosition(SwingConstants.RIGHT);
//
//			final JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
//			panel.setOpaque(false);
//			panel.add(btn);
//			panel.add(targetSyllLbl);
//
//			final TierDataConstraint targetConstraint = new TierDataConstraint(TierDataConstraint.TIER_LABEL_COLUMN, 0);
//			contentPane.add(panel, targetConstraint);
//		}
//
//		if(showActual) {
//			final ResetSyllabificationCommand resetActualAct = new ResetSyllabificationCommand(getEditor(), this, SystemTierType.IPAActual.getName());
//			resetActualAct.putValue(Action.NAME, null);
//			resetActualAct.putValue(Action.SMALL_ICON, reloadIcn);
//			final JButton btn = new JButton(resetActualAct);
//
//			final JLabel actualSyllLbl = new JLabel(SystemTierType.ActualSyllables.getName());
//			actualSyllLbl.setHorizontalAlignment(SwingConstants.RIGHT);
//			actualSyllLbl.setHorizontalTextPosition(SwingConstants.RIGHT);
//
//			final JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
//			panel.setOpaque(false);
//			panel.add(btn);
//			panel.add(actualSyllLbl);
//
//			final TierDataConstraint actualConstraint = new TierDataConstraint(TierDataConstraint.TIER_LABEL_COLUMN, 1);
//			contentPane.add(panel, actualConstraint);
//		}
//
//		if(showAlignment) {
//			final ResetAlignmentCommand resetAct = new ResetAlignmentCommand(getEditor(), this);
//			resetAct.putValue(Action.NAME, null);
//			resetAct.putValue(Action.SMALL_ICON, reloadIcn);
//			final JButton btn = new JButton(resetAct);
//
//			final JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
//			separator.setForeground(Color.lightGray);
//			final TierDataConstraint sepConstraint = new TierDataConstraint(TierDataConstraint.FULL_TIER_COLUMN, 2);
//			contentPane.add(separator, sepConstraint);
//
//			final JLabel alignSyllLbl = new JLabel(SystemTierType.PhoneAlignment.getName());
//			alignSyllLbl.setHorizontalAlignment(SwingConstants.RIGHT);
//			alignSyllLbl.setHorizontalTextPosition(SwingConstants.RIGHT);
//
//			final JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
//			panel.setOpaque(false);
//			panel.add(btn);
//			panel.add(alignSyllLbl);
//
//			final TierDataConstraint alignConstraint = new TierDataConstraint(TierDataConstraint.TIER_LABEL_COLUMN, 3);
//			contentPane.add(panel, alignConstraint);
//		}
//
//		final TierDataLayout layout = TierDataLayout.class.cast(contentPane.getLayout());
//		if(showTarget) {
//			// target
//			final IPATranscript ipaTarget = record.getIPATarget();
//			final SyllabificationDisplay ipaTargetDisplay = getIPATargetDisplay();
//			ipaTargetDisplay.setFont(FontPreferences.getTierFont());
//			ipaTargetDisplay.setTranscript(ipaTarget);
//			ipaTargetDisplay.setShowDiacritics(showDiacritics);
//
//			if(!layout.hasLayoutComponent(ipaTargetDisplay)) {
//				final TierDataConstraint ipaTargetConstraint =
//						new TierDataConstraint(TierDataConstraint.FLAT_TIER_COLUMN, 0);
//				contentPane.add(ipaTargetDisplay, ipaTargetConstraint);
//			}
//		}
//
//		if(showActual) {
//			// actual
//			final IPATranscript ipaActual = record.getIPAActual();
//			final SyllabificationDisplay ipaActualDisplay = getIPAActualDisplay();
//			ipaActualDisplay.setFont(FontPreferences.getTierFont());
//			ipaActualDisplay.setTranscript(ipaActual);
//			ipaActualDisplay.setShowDiacritics(showDiacritics);
//
//			if(!layout.hasLayoutComponent(ipaActualDisplay)) {
//				final TierDataConstraint ipaActualConstraint =
//						new TierDataConstraint(TierDataConstraint.FLAT_TIER_COLUMN, 1);
//				contentPane.add(ipaActualDisplay, ipaActualConstraint);
//			}
//		}
//
//		if(showAlignment) {
//			// alignment
//			PhoneAlignment phoneAlignment = record.getPhoneAlignment();
//			if(phoneAlignment == null) {
//				phoneAlignment = PhoneAlignment.fromTiers(record.getIPATargetTier(), record.getIPAActualTier());
//			}
//			final PhoneMapDisplay pmDisplay = getAlignmentDisplay();
//			pmDisplay.setFont(FontPreferences.getTierFont());
//			for(int i = 0; i < phoneAlignment.getAlignments().size(); i++) {
//				pmDisplay.setPhoneMapForWord(0, phoneAlignment.getAlignments().get(i));
//				pmDisplay.setFocusedPosition(0);
//				pmDisplay.setPaintPhoneBackground(colorInAlignment);
//				pmDisplay.setShowDiacritics(showDiacritics);
//
//				if (!layout.hasLayoutComponent(pmDisplay)) {
//					final TierDataConstraint pmConstraint =
//							new TierDataConstraint(TierDataConstraint.GROUP_START_COLUMN + i, 3);
//					contentPane.add(pmDisplay, pmConstraint);
//				}
//			}
//		}
//		contentPane.revalidate();
	}

	public void toggleCheckbox() {
		update();
		repaint();
	}

	/*---- Editor Actions -------------------*/
	private void onSessionChanged(EditorEvent<Session> ee) {
		onDataChanged(ee);
	}

	private void onRecordChanged(EditorEvent<EditorEventType.RecordChangedData> ee) {
		onDataChanged(ee);
	}

	private void onGroupListChange(EditorEvent<Void> ee) {
		onDataChanged(ee);
	}

	private void onDataChanged(EditorEvent<?> ee) {
		update();
		repaint();
	}

	private void onTierChanged(EditorEvent<EditorEventType.TierChangeData> ee) {
		final String tierName = ee.data().tier().getName();
		if(SystemTierType.IPATarget.getName().equals(tierName) ||
				SystemTierType.IPAActual.getName().equals(tierName) ||
				SystemTierType.PhoneAlignment.getName().equals(tierName)) {
			update();
			repaint();
		}
	}

	private void onScChange(EditorEvent<ScEditData> ee) {
		final IPATranscript ipa = ee.data().ipa();
		final Record r = getEditor().currentRecord();
		final SyllabificationDisplay targetDisplay = getIPATargetDisplay();
		final SyllabificationDisplay actualDisplay = getIPAActualDisplay();
		boolean found = false;
		if(targetDisplay.getTranscript() == ipa) {
			targetDisplay.repaint();
			found = true;
		} else if(actualDisplay.getTranscript() == ipa) {
			actualDisplay.repaint();
			found = true;
		}
		if(found) {
			getAlignmentDisplay().repaint();
		}
	}

	@Override
	public String getName() {
		return VIEW_NAME;
	}

	@Override
	public ImageIcon getIcon() {
		return IconManager.getInstance().getIcon("misc/syllabification", IconSize.SMALL);
	}

	@Override
	public JMenu getMenu() {
		final JMenu retVal = new JMenu();

		retVal.add(new SyllabificationSettingsCommand(getEditor(), this));

		retVal.addSeparator();

		final ResetSyllabificationCommand resetIPATargetAct = new ResetSyllabificationCommand(getEditor(), this, SystemTierType.IPATarget.getName());
		retVal.add(resetIPATargetAct);

		final ResetSyllabificationCommand resetIPAActualAct = new ResetSyllabificationCommand(getEditor(), this, SystemTierType.IPAActual.getName());
		retVal.add(resetIPAActualAct);

		final ResetAlignmentCommand resetAlignmentAct = new ResetAlignmentCommand(getEditor(), this);
		retVal.add(resetAlignmentAct);

		return retVal;
	}

}
