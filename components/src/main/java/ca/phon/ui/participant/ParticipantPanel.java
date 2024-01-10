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
package ca.phon.ui.participant;

import ca.phon.session.*;
import ca.phon.session.format.AgeFormatter;
import ca.phon.ui.CommonModuleFrame;
import ca.phon.ui.DropDownIcon;
import ca.phon.ui.action.PhonUIAction;
import ca.phon.ui.decorations.ComponentWithMessage;
import ca.phon.ui.decorations.DialogHeader;
import ca.phon.ui.layout.ButtonBarBuilder;
import ca.phon.ui.menu.MenuBuilder;
import ca.phon.ui.text.*;
import ca.phon.ui.toast.ToastFactory;
import ca.phon.util.icons.IconManager;
import ca.phon.util.icons.IconSize;
import com.jgoodies.forms.layout.*;
import org.jdesktop.swingx.VerticalLayout;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.time.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * UI for editing participant information.
 *
 */
public class ParticipantPanel extends JPanel {

	private JComboBox<ParticipantRole> roleBox;

	private final ImageIcon warningIcn;
	private final ImageIcon infoIcn;

	private ComponentWithMessage<JTextField> idField;
	private final static String ID_FIELD_PROMPT = "Id will be used in place of name if not provided";
	private final static String ID_NO_SPACES = "Id cannot contain spaces";
	private final static String ID_NOT_EMPTY = "Id cannot be empty";

	private JComboBox<Sex> sexBox;

	private JTextField nameField;
	private JTextField groupField;
	private JTextField sesField;
	private JTextField educationField;
	private LanguageField languageField;
	private LanguageField firstLanguageField;

	private ComponentWithMessage<DatePicker> bdayField;
	private final static String BDAY_PROMPT = "Format YYYY-MM-DD";
	private final static String BDAY_BEFORE_SESSION = "Birthday is after session date";

	private ComponentWithMessage<FormatterTextField<Period>> ageField;
	private final static String AGE_NO_MATCH = "Age does not match birthday";
	private final static String AGE_PROMPT = "Format YY;MM.DD";

	private LocalDate sessionDate;

	private List<Participant> otherParticipants;

	private JTextField birthplaceField;

	private JTextField otherField;

	private JButton anonymizeBtn;

	private final Participant participant;

	public ParticipantPanel() {
		this(SessionFactory.newFactory().createParticipant());
	}

	public ParticipantPanel(Participant participant) {
		super();
		this.participant = participant;

		this.warningIcn = IconManager.getInstance().getIcon("status/dialog-warning", IconSize.XSMALL);
		this.infoIcn = null;

		init();
	}

	private JLabel createFieldLabel(String text, String propName) {
		final JLabel retVal = new JLabel(text);
		retVal.setIcon(new DropDownIcon(new ImageIcon(), 0, SwingConstants.BOTTOM));
		retVal.setHorizontalTextPosition(SwingConstants.LEFT);
		retVal.addMouseListener(new FieldMenuListener(propName));
		retVal.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		return retVal;
	}

	private void init() {
		final ImageIcon warningIcn = IconManager.getInstance().getIcon("emblems/flag-red", IconSize.XSMALL);

		// setup form
		roleBox = new JComboBox<>(ParticipantRole.values());

		idField = new ComponentWithMessage<>(new JTextField(), infoIcn, ID_FIELD_PROMPT);
		JLabel idWarningLbl = idField.getLabel();
		idWarningLbl.setIcon(warningIcn);
		idWarningLbl.setFont(idWarningLbl.getFont().deriveFont(10.0f));
		updateIdWarningLabel();

		sexBox = new JComboBox<>(Sex.values());
		sexBox.setSelectedItem(
				(participant.getSex() != null ? participant.getSex() : Sex.UNSPECIFIED));
		sexBox.setRenderer(new DefaultListCellRenderer() {

			@Override
			public Component getListCellRendererComponent(JList<?> list,
					Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				final JLabel retVal = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected,
						cellHasFocus);
				final Sex sex = (Sex)value;

				retVal.setText(sex.getText());

				return retVal;
			}

		});

		final PhonUIAction anonymizeAct = PhonUIAction.runnable(this::onAnonymize);
		anonymizeAct.putValue(PhonUIAction.NAME, "Anonymize");
		anonymizeAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Remove all optional information");
		anonymizeBtn = new JButton(anonymizeAct);

		int defCols = 20;
		nameField = new JTextField();
		nameField.setColumns(defCols);

		groupField = new JTextField();
		groupField.setColumns(defCols);

		sesField = new JTextField();
		sesField.setColumns(defCols);
		educationField = new JTextField();
		educationField.setColumns(defCols);
		languageField = new LanguageField();
		languageField.setColumns(defCols);

		firstLanguageField = new LanguageField();
		firstLanguageField.setColumns(defCols);

		bdayField = new ComponentWithMessage<>(new DatePicker(sessionDate), infoIcn, BDAY_PROMPT);
		JLabel bdayWarningLbl = bdayField.getLabel();
		bdayWarningLbl.setIcon(warningIcn);
		bdayWarningLbl.setFont(bdayWarningLbl.getFont().deriveFont(10.0f));
		updateBirthdayWarningLabel();


		ageField = new ComponentWithMessage<>(FormatterTextField.createTextField(Period.class), infoIcn, AGE_PROMPT);
		ageField.getComponent().setPrompt("YY;MM.DD");
		ageField.getComponent().setToolTipText("Enter age in format YY;MM.YY");
		ageField.getLabel().setFont(ageField.getLabel().getFont().deriveFont(10.0f));
		updateAgeWarningLabel();

		birthplaceField = new JTextField();

		otherField = new JTextField();

		// setup info
		if(participant.getRole() != null)
			roleBox.setSelectedItem(participant.getRole());
		if(participant.getId() != null) {
			idField.getComponent().setText(participant.getId());
		}
		updateIdWarningLabel();
		if(participant.getName() != null)
			nameField.setText(participant.getName());
		if(participant.getGroup() != null)
			groupField.setText(participant.getGroup());
		if(participant.getSES() != null)
			sesField.setText(participant.getSES());
		if(participant.getLanguage() != null)
			languageField.setText(participant.getLanguage());
		if(participant.getEducation() != null)
			educationField.setText(participant.getEducation());
		if(participant.getBirthDate() != null) {
			bdayField.getComponent().setDateTime(participant.getBirthDate());
		}
		if(participant.getAge(null) != null) {
			ageField.getComponent().setValue(participant.getAge(null));
		}
		if(participant.getFirstLanguage() != null) {
			firstLanguageField.setText(participant.getFirstLanguage());
		}
		if(participant.getBirthplace() != null) {
			birthplaceField.setText(participant.getBirthplace());
		}
		if(participant.getOther() != null) {
			otherField.setText(participant.getOther());
		}

		// setup listeners
		final Consumer<Participant> roleUpdater = (obj) -> {
			final ParticipantRole role = (ParticipantRole)roleBox.getSelectedItem();
			obj.setRole(role);
			idField.getComponent().setText(getRoleId());
		};
		roleBox.addItemListener(new ItemUpdater(roleUpdater));

		final Consumer<Participant> idUpdater = (obj) -> {
			if(idField.getComponent().getText().trim().length() > 0
				&& idField.getComponent().getText().split("\\s").length == 1)
				obj.setId(idField.getComponent().getText());
			updateIdWarningLabel();
		};
		idField.getComponent().getDocument().addDocumentListener(new TextFieldUpdater(idUpdater));

		final Consumer<Participant> nameUpdater = (obj) -> {
			obj.setName(nameField.getText());
		};
		nameField.getDocument().addDocumentListener(new TextFieldUpdater(nameUpdater));

		final Consumer<Participant> langUpdater = (obj) -> {
			obj.setLanguage(languageField.getText());
		};
		languageField.getDocument().addDocumentListener(new TextFieldUpdater(langUpdater));

		final Consumer<Participant> firstLangUpdater = (obj) -> {
			obj.setFirstLanguage(firstLanguageField.getText());
		};
		firstLanguageField.getDocument().addDocumentListener(new TextFieldUpdater(firstLangUpdater));

		final Consumer<Participant> groupUpdater = (obj) -> {
			obj.setGroup(groupField.getText());
		};
		groupField.getDocument().addDocumentListener(new TextFieldUpdater(groupUpdater));

		final Consumer<Participant> eduUpdater = (obj) -> {
			obj.setEducation(educationField.getText());
		};
		educationField.getDocument().addDocumentListener(new TextFieldUpdater(eduUpdater));

		final Consumer<Participant> sesUpdater = (obj) -> {
			obj.setSES(sesField.getText());
		};
		sesField.getDocument().addDocumentListener(new TextFieldUpdater(sesUpdater));

		final Consumer<Participant> sexUpdater = (obj) -> {
			obj.setSex((Sex)sexBox.getSelectedItem());
		};
		sexBox.addItemListener(new ItemUpdater(sexUpdater));

		final Consumer<Participant> birthplaceUpdater = (obj) -> {
			obj.setBirthplace(birthplaceField.getText());
		};
		birthplaceField.getDocument().addDocumentListener(new TextFieldUpdater(birthplaceUpdater));

		final Consumer<Participant> otherUpdater = (obj) -> {
			obj.setOther(otherField.getText());
		};
		otherField.getDocument().addDocumentListener(new TextFieldUpdater(otherUpdater));

		final Consumer<Participant> bdayUpdater = (obj) -> {
			final LocalDate bday = bdayField.getComponent().getDateTime();
			final Period currentAge = obj.getAge(null);
			final boolean ageNotSet = (currentAge == null);
			final boolean hasValidInfo =
					(sessionDate != null && obj.getBirthDate() != null
						&& sessionDate.isAfter(obj.getBirthDate()));
			final Period calculatedAge = hasValidInfo ? obj.getBirthDate().until(sessionDate) : null;
			obj.setBirthDate(bday);
			if(bday != null) {
				if(bday.isBefore(sessionDate)) {
					if (ageNotSet || (calculatedAge != null && calculatedAge.equals(currentAge))) {
						obj.setAge(null);
						final Period age = obj.getAge(sessionDate);
						ageField.getComponent().setValue(age);
					}
				} else {
					if (ageNotSet || (calculatedAge != null && calculatedAge.equals(currentAge))) {
						ageField.getComponent().setValue(null);
					}
				}
			}

			updateBirthdayWarningLabel();
			updateAgeWarningLabel();
		};
		bdayField.getComponent().addPropertyChangeListener(DatePicker.DATETIME_PROP, new PropertyUpdater(bdayUpdater));
		bdayField.getComponent().getTextField().addActionListener(new ActionUpdater(bdayUpdater));

		final Consumer<Participant> ageUpdater = (obj) -> {
			if(ageField.getComponent().getValue() == null) {
				obj.setAge(null);
			} else {
				final Period p = ageField.getComponent().getValue();
				obj.setAge(p);
			}
			updateAgeWarningLabel();
		};
		ageField.getComponent().addPropertyChangeListener(FormatterTextField.VALIDATED_VALUE, new PropertyUpdater(ageUpdater));

		// ensure a role is selected!
		if(participant.getRole() == null) {
			roleBox.setSelectedItem(ParticipantRole.TARGET_CHILD);
		}

		final CellConstraints cc = new CellConstraints();
		final FormLayout reqLayout = new FormLayout(
				"right:pref, 3dlu, fill:pref:grow",
				"pref, pref, pref, pref");
		final JPanel required = new JPanel(reqLayout);
		required.setBorder(BorderFactory.createTitledBorder("Required Information"));
		required.add(new JLabel("Role"), cc.xy(1,1));
		required.add(roleBox, cc.xy(3,1));
		required.add(createFieldLabel("Id", "id"), cc.xy(1, 3));
		required.add(idField, cc.xy(3, 3));

		final FormLayout optLayout = new FormLayout(
				"right:pref, 3dlu, fill:pref:grow, 5dlu, right:pref, 3dlu, fill:pref:grow",
				"pref, pref, pref, pref, pref, pref");
		final JPanel optional = new JPanel(optLayout);
		optional.setBorder(BorderFactory.createTitledBorder("Optional Information"));
		// left column: Name, Birthday, Language, Group, Education
		optional.add(createFieldLabel("Name", "name"), cc.xy(1, 1));
		optional.add(nameField, cc.xy(3, 1));
		optional.add(createFieldLabel("Birthday (YYYY-MM-DD)", "birthday"), cc.xy(1, 2));
		optional.add(bdayField, cc.xy(3, 2));
		optional.add(createFieldLabel("Language", "language"), cc.xy(1, 3));
		optional.add(languageField, cc.xy(3, 3));
		optional.add(createFieldLabel("Group", "group"), cc.xy(1, 4));
		optional.add(groupField, cc.xy(3, 4));
		optional.add(createFieldLabel("Education", "education"), cc.xy(1, 5));
		optional.add(educationField, cc.xy(3, 5));

		// right column: Sex, Age, First language, Birthplace, SES
		optional.add(createFieldLabel("Sex", "sex"), cc.xy(5, 1));
		optional.add(sexBox, cc.xy(7, 1));
		optional.add(createFieldLabel("Age (" + AgeFormatter.AGE_FORMAT + ")", "age"), cc.xy(5, 2));
		optional.add(ageField, cc.xy(7, 2));
		optional.add(createFieldLabel("First Language", "firstLanguage"), cc.xy(5, 3));
		optional.add(firstLanguageField, cc.xy(7, 3));
		optional.add(createFieldLabel("Birthplace", "birthplace"), cc.xy(5, 4));
		optional.add(birthplaceField, cc.xy(7, 4));
		optional.add(createFieldLabel("SES", "ses"), cc.xy(5, 5));
		optional.add(sesField, cc.xy(7, 5));

		// add other at bottom spanning across both columns
		optional.add(createFieldLabel("Other", "other"), cc.xy(1, 6));
		optional.add(otherField, cc.xyw(3, 6, 5));

		setLayout(new VerticalLayout(5));
		add(required);
		add(optional);
		add(ButtonBarBuilder.buildOkBar(anonymizeBtn));
		add(new JSeparator(SwingConstants.HORIZONTAL));
	}

	private void updateIdWarningLabel() {
		if(idField.getComponent().getText() == null || idField.getComponent().getText().trim().length() == 0) {
			idField.updateLabel(warningIcn, ID_NOT_EMPTY, true);
		} else if(idField.getComponent().getText().split("\\s").length > 1) {
			idField.updateLabel(warningIcn, ID_NO_SPACES, true);
		} else {
			idField.updateLabel(infoIcn, ID_FIELD_PROMPT, true);
		}
	}

	private void updateAgeWarningLabel() {
		if(sessionDate != null) {
			final Period specifiedAge = ageField.getComponent().getValue();
			if (specifiedAge != null && participant.getBirthDate() != null) {
				if(sessionDate.isAfter(participant.getBirthDate())) {
					final Period calculatedAge = participant.getBirthDate().until(sessionDate);
					if(!calculatedAge.equals(specifiedAge)) {
						ageField.updateLabel(warningIcn, AGE_NO_MATCH, true);
						return;
					}
				}
			}
		}
		ageField.updateLabel(infoIcn, AGE_PROMPT, true);
	}

	private void updateBirthdayWarningLabel() {
		if(sessionDate != null &&  participant.getBirthDate() != null
			&& participant.getBirthDate().isAfter(sessionDate))
			bdayField.updateLabel(warningIcn, BDAY_BEFORE_SESSION, true);
		else
			bdayField.updateLabel(infoIcn, BDAY_PROMPT, true);
	}

	public void setOtherParticipants(List<Participant> parts) {
		this.otherParticipants = parts;

		if(participant.getRole() == null) {
			participant.setRole(ParticipantRole.TARGET_CHILD);
			participant.setId(getRoleId());
			idField.getComponent().setText(participant.getId());
		}
	}

	public LocalDate getSessionDate() {
		return this.sessionDate;
	}

	public void setSessionDate(LocalDate sessionDate) {
		this.sessionDate = sessionDate;
		bdayField.getComponent().setPromptDate(sessionDate);
		updateBirthdayWarningLabel();
		updateAgeWarningLabel();

		if(sessionDate != null && participant.getAge(null) == null
				&& participant.getBirthDate() != null
				&& participant.getBirthDate().isBefore(sessionDate)) {
			final Period age = participant.getAge(sessionDate);
			ageField.getComponent().setValue(age);
		}
	}

	public String getRoleId() {
		final ParticipantRole role = (ParticipantRole)roleBox.getSelectedItem();
		String id = role.getId();

		if(otherParticipants != null) {
			boolean checked = false;
			int idx = 0;
			while(!checked) {
				checked = true;
				for(Participant otherP:otherParticipants) {
					if(otherP.getId().equals(id)) {
						id = role.getId().substring(0, 2) + (++idx);
						checked = false;
					}
				}
			}
		}
		return id;
	}

	public void updateRoleId() {
		idField.getComponent().setText(getRoleId());
	}

	private void onShowPropertyMenu(JComponent lbl, String propName) {
		final JPopupMenu menu = new JPopupMenu();
		final MenuBuilder builder = new MenuBuilder(menu);

		switch (propName) {
			case "id" -> {
				final PhonUIAction<Void> assignIdFromRole = PhonUIAction.runnable(this::updateRoleId);
				assignIdFromRole.putValue(PhonUIAction.NAME, "Assign id from role");
				assignIdFromRole.putValue(PhonUIAction.SHORT_DESCRIPTION, "Assign id from selected role");
				builder.addItem(".", assignIdFromRole);
			}

			case "birthday" -> {
				if(participant.getAge(null) != null) {
					final PhonUIAction<Void> onCalcBirthday = PhonUIAction.runnable(this::onCalcBirthday);
					onCalcBirthday.putValue(PhonUIAction.NAME, "Calculate from age");
					onCalcBirthday.putValue(PhonUIAction.SHORT_DESCRIPTION, "Calculate birthday from provided age");
					builder.addItem(".", onCalcBirthday);
					builder.addSeparator(".", "custom_items");
				}
			}

			case "age" -> {
				if(participant.getBirthDate() != null) {
					final PhonUIAction<Void> onCalcAge = PhonUIAction.runnable(this::onCalcAge);
					onCalcAge.putValue(PhonUIAction.NAME, "Calculate from birthday");
					onCalcAge.putValue(PhonUIAction.SHORT_DESCRIPTION, "Calculate age from provided birthday");
					builder.addItem(".", onCalcAge);
					builder.addSeparator(".", "custom_items");
				}
			}
		}

		if(!"id".equals(propName)) {
			final PhonUIAction<String> onClearField = PhonUIAction.consumer(this::clearField, propName);
			onClearField.putValue(PhonUIAction.NAME, "Clear " + propName);
			onClearField.putValue(PhonUIAction.SHORT_DESCRIPTION, "Clear data for property " + propName);
			builder.addItem(".", onClearField);
		}

		menu.show(lbl, 0, lbl.getHeight());
	}

	private void clearField(String propName) {
		switch (propName) {
			case "name" -> {
				nameField.setText("");
			}

			case "language" -> {
				languageField.setText("");
			}

			case "firstLanguage" -> {
				firstLanguageField.setText("");
			}

			case "birthday" -> {
				bdayField.getComponent().setDateTime(null);
			}

			case "age" -> {
				ageField.getComponent().setValue(null);
			}

			case "group" -> {
				groupField.setText("");
			}

			case "birthplace" -> {
				birthplaceField.setText("");
			}

			case "sex" -> {
				sexBox.setSelectedItem(Sex.UNSPECIFIED);
			}

			case "ses" -> {
				sesField.setText("");
			}

			case "education" -> {
				educationField.setText("");
			}

			case "other" -> {
				otherField.setText("");
			}
		}
	}

	private void onCalcBirthday() {
		final Period age = participant.getAge(null);
		if(age != null) {
			final LocalDate sessionDate = getSessionDate();
			final LocalDate bday = sessionDate.minus(age);
			bdayField.getComponent().setDateTime(bday);
		}
	}

	private void onCalcAge() {
		final LocalDate sessionDate = getSessionDate();
		final LocalDate bday = participant.getBirthDate();
		if(sessionDate.isAfter(bday)) {
			final Period age = bday.until(sessionDate);
			ageField.getComponent().setValue(age);
		}
	}

	private void onAnonymize() {
		final JDialog anonymizeDialog = new JDialog(CommonModuleFrame.getCurrentFrame());
		anonymizeDialog.setModal(true);

		anonymizeDialog.setLayout(new BorderLayout());
		final DialogHeader header = new DialogHeader("Anonymize Participant",
				"Anonymize selected information for " + participant.toString());
		anonymizeDialog.add(header, BorderLayout.NORTH);

		final AnonymizeParticipantOptionsPanel optionsPanel = new AnonymizeParticipantOptionsPanel();
		optionsPanel.setBorder(BorderFactory.createTitledBorder("Select information to strip"));
		anonymizeDialog.add(optionsPanel, BorderLayout.CENTER);

		final ActionListener closeListener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				anonymizeDialog.setVisible(false);
			}
		};

		final PhonUIAction okAct = PhonUIAction.consumer(this::doAnonymizeParticipant, optionsPanel);
		okAct.putValue(PhonUIAction.NAME, "Ok");
		final JButton okBtn = new JButton(okAct);
		okBtn.addActionListener(closeListener);

		final JButton closeBtn = new JButton("Cancel");
		closeBtn.addActionListener(closeListener);

		final JComponent btnPanel = ButtonBarBuilder.buildOkCancelBar(okBtn, closeBtn);
		anonymizeDialog.add(btnPanel, BorderLayout.SOUTH);

		anonymizeDialog.pack();
		anonymizeDialog.setLocationRelativeTo(this);
		anonymizeDialog.setVisible(true);
	}

	public void doAnonymizeParticipant(AnonymizeParticipantOptionsPanel optionsPanel) {
		if(optionsPanel.isAssignId())
			idField.getComponent().setText(getRoleId());
		if(optionsPanel.isAnonName())
			clearField("name");
		if(optionsPanel.isAnonBday())
			clearField("birthday");
		if(optionsPanel.isAnonAge())
			clearField("age");
		if(optionsPanel.isAnonSex())
			clearField("sex");
		if(optionsPanel.isAnonLang())
			clearField("language");
		if(optionsPanel.isAnonGroup())
			clearField("group");
		if(optionsPanel.isAnonEdu())
			clearField("education");
		if(optionsPanel.isAnonSes())
			clearField("ses");
		if(optionsPanel.isAnonOther())
			clearField("other");
		if(optionsPanel.isAnonBirthplace())
			clearField("birthplace");
		if(optionsPanel.isAnonFirstLang())
			clearField("firstLanguage");
	}

	public Participant getParticipant() {
		return this.participant;
	}

	private class FieldMenuListener extends MouseInputAdapter {

		final String propName;

		public FieldMenuListener(String propName) {
			this.propName = propName;
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			onShowPropertyMenu((JLabel)e.getSource(), propName);
		}

	}

	private class ItemUpdater implements ItemListener {

		private final Consumer<Participant> updater;

		public ItemUpdater(Consumer<Participant> updater) {
			this.updater = updater;
		}

		@Override
		public void itemStateChanged(ItemEvent e) {
			updater.accept(participant);
		}

	}

	private class ActionUpdater implements ActionListener {

		private final Consumer<Participant> updater;

		public ActionUpdater(Consumer<Participant> updater) {
			this.updater = updater;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			updater.accept(participant);
		}

	}

	private class TextFieldUpdater implements DocumentListener {

		private final Consumer<Participant> updater;

		public TextFieldUpdater(Consumer<Participant> updater) {
			this.updater = updater;
		}

		@Override
		public void insertUpdate(DocumentEvent e) {
			updater.accept(participant);
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			updater.accept(participant);
		}

		@Override
		public void changedUpdate(DocumentEvent e) {

		}

	}

	private class PropertyUpdater implements PropertyChangeListener {

		private final Consumer<Participant> updater;

		public PropertyUpdater(Consumer<Participant> updater) {
			super();
			this.updater = updater;
		}

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			updater.accept(participant);
		}

	}

}
