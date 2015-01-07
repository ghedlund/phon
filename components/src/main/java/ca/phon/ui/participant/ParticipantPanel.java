package ca.phon.ui.participant;

import java.awt.Component;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JWindow;
import javax.swing.ListCellRenderer;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.jdesktop.swingx.HorizontalLayout;
import org.jdesktop.swingx.JXDatePicker;
import org.jdesktop.swingx.JXMonthView;
import org.jdesktop.swingx.VerticalLayout;
import org.jdesktop.swingx.event.DateSelectionEvent;
import org.jdesktop.swingx.event.DateSelectionListener;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.PeriodType;

import ca.phon.functor.Functor;
import ca.phon.session.AgeFormatter;
import ca.phon.session.DateFormatter;
import ca.phon.session.Participant;
import ca.phon.session.ParticipantRole;
import ca.phon.session.SessionFactory;
import ca.phon.session.Sex;
import ca.phon.ui.CommonModuleFrame;
import ca.phon.ui.action.PhonUIAction;
import ca.phon.ui.fonts.FontPreferences;
import ca.phon.ui.layout.ButtonBarBuilder;
import ca.phon.ui.nativedialogs.MessageDialogProperties;
import ca.phon.ui.nativedialogs.NativeDialogs;
import ca.phon.ui.text.DatePicker;
import ca.phon.ui.text.FormatterTextField;
import ca.phon.util.icons.IconManager;
import ca.phon.util.icons.IconSize;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * UI for editing participant information.
 *
 */
public class ParticipantPanel extends JPanel {

	private static final long serialVersionUID = 8479424482231807300L;
	
	/*
	 * UI
	 */
	private JComboBox roleBox;
	
	private JCheckBox assignIdBox;
	private JTextField idField;
	
	private JComboBox sexBox;
	
	private JTextField nameField;
	private JTextField groupField;
	private JTextField sesField;
	private JTextField educationField;
	private JTextField languageField;
	
	private FormatterTextField<DateTime> bdayField;
	private JButton calendarButton;
	
	private FormatterTextField<Period> ageField;
//	private JButton calcAgeBtn;
	
	private DateTime sessionDate;
	
	private List<Participant> otherParticipants;
	
	private JButton anonymizeBtn;
	
	private final Participant participant;

	public ParticipantPanel() {
		this(SessionFactory.newFactory().createParticipant());
	}
	
	public ParticipantPanel(Participant participant) {
		super();
		this.participant = participant;
		
		init();
	}
	
	private void init() {
		// setup form
		roleBox = new JComboBox(ParticipantRole.values());
		
		assignIdBox = new JCheckBox("Assign ID from role");
		assignIdBox.setSelected(true);
		
		idField = new JTextField();
		idField.setEnabled(false);
		
		sexBox = new JComboBox(Sex.values());
		sexBox.setSelectedItem(
				(participant.getSex() != null ? participant.getSex() : Sex.UNSPECIFIED));
		sexBox.setRenderer(new DefaultListCellRenderer() {

			@Override
			public Component getListCellRendererComponent(JList list,
					Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				final JLabel retVal = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected,
						cellHasFocus);
				final Sex sex = (Sex)value;
				
				retVal.setText(sex.getText());
				
				return retVal;
			}
			
		});
		
		final PhonUIAction anonymizeAct = new PhonUIAction(this, "onAnonymize");
		anonymizeAct.putValue(PhonUIAction.NAME, "Anonymize");
		anonymizeAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Remove all optional information");
		anonymizeBtn = new JButton(anonymizeAct);
		
		nameField = new JTextField();
		groupField = new JTextField();
		sesField = new JTextField();
		educationField = new JTextField();
		languageField = new JTextField();
		
		final ImageIcon calendarIcon = 
				IconManager.getInstance().getIcon("apps/office-calendar", IconSize.SMALL);
		
		final DateFormatter bdayFormatter = new DateFormatter();
		bdayField = new FormatterTextField<DateTime>(bdayFormatter);
		bdayField.setPrompt("YYYY-MM-DD");
		bdayField.setToolTipText("Enter date in format YYYY-MM-DD");
		bdayField.addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent arg0) {
				if(bdayField.getText().length() > 0 &&
						!bdayField.validateText()) {
					Toolkit.getDefaultToolkit().beep();
					bdayField.requestFocus();
				}
			}
			
			@Override
			public void focusGained(FocusEvent arg0) {
			}
			
		});
		final PhonUIAction showCalendarAct = new PhonUIAction(this, "onShowCalendar");
		showCalendarAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Show calendar");
		showCalendarAct.putValue(PhonUIAction.SMALL_ICON, calendarIcon);
		calendarButton = new JButton(showCalendarAct);
		
		ageField = FormatterTextField.createTextField(Period.class);
		ageField.setPrompt("YY;MM.DD");
		ageField.setToolTipText("Enter age in format YY;MM.YY");
		ageField.addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent arg0) {
				if(ageField.getText().length() > 0 &&
						!ageField.validateText()) {
					Toolkit.getDefaultToolkit().beep();
					bdayField.requestFocus();
				}
			}
			
			@Override
			public void focusGained(FocusEvent arg0) {
			}
			
		});
		
		// setup info
		
		if(participant.getRole() != null)
			roleBox.setSelectedItem(participant.getRole());
		if(participant.getId() != null) {
			idField.setText(participant.getId());
		}
		idField.setText(participant.getId());
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
			bdayField.setValue(participant.getBirthDate());
		}
		
		if(participant.getAge(null) != null) {
			ageField.setValue(participant.getAge(null));
		}
		
		// setup listeners
		final Functor<Void, Participant> roleUpdater = new Functor<Void, Participant>() {
			
			@Override
			public Void op(Participant obj) {
				final ParticipantRole role = (ParticipantRole)roleBox.getSelectedItem();
				participant.setRole(role);
				if(assignIdBox.isSelected()) {
					idField.setText(getRoleId());
				}
				
				return null;
			}
			
		};
		roleBox.addItemListener(new ItemUpdater(roleUpdater));
		
		final Functor<Void, Participant> idUpdater = new Functor<Void, Participant>() {
			
			@Override
			public Void op(Participant obj) {
				participant.setId(idField.getText());
				return null;
			}
			
		};
		idField.getDocument().addDocumentListener(new TextFieldUpdater(idUpdater));
		
		final Functor<Void, Participant> nameUpdater = new Functor<Void, Participant>() {
			
			@Override
			public Void op(Participant obj) {
				participant.setName(nameField.getText());
				return null;
			}
			
		};
		nameField.getDocument().addDocumentListener(new TextFieldUpdater(nameUpdater));
		
		final Functor<Void, Participant> langUpdater = new Functor<Void, Participant>() {
			
			@Override
			public Void op(Participant obj) {
				participant.setLanguage(languageField.getText());
				return null;
			}
			
		};
		languageField.getDocument().addDocumentListener(new TextFieldUpdater(langUpdater));
		
		final Functor<Void, Participant> groupUpdater = new Functor<Void, Participant>() {
			
			@Override
			public Void op(Participant obj) {
				participant.setGroup(groupField.getText());
				return null;
			}
			
		};
		groupField.getDocument().addDocumentListener(new TextFieldUpdater(groupUpdater));
		
		final Functor<Void, Participant> eduUpdater = new Functor<Void, Participant>() {
			
			@Override
			public Void op(Participant obj) {
				participant.setEducation(educationField.getText());
				return null;
			}
			
		};
		educationField.getDocument().addDocumentListener(new TextFieldUpdater(eduUpdater));
		
		final Functor<Void, Participant> sesUpdater = new Functor<Void, Participant>() {
			
			@Override
			public Void op(Participant obj) {
				participant.setSES(sesField.getText());
				return null;
			}
			
		};
		sesField.getDocument().addDocumentListener(new TextFieldUpdater(sesUpdater));
		
		final Functor<Void, Participant> sexUpdater = new Functor<Void, Participant>() {
			
			@Override
			public Void op(Participant obj) {
				participant.setSex((Sex)sexBox.getSelectedItem());
				return null;
			}
			
		};
		sexBox.addItemListener(new ItemUpdater(sexUpdater));
		
		final Functor<Void, Participant> assignIdFunctor = new Functor<Void, Participant>() {
			
			@Override
			public Void op(Participant obj) {
				if(assignIdBox.isSelected()) {
					if(assignIdBox.isSelected()) {
						idField.setText(getRoleId());
					}
				}
				idField.setEnabled(!assignIdBox.isSelected());
				return null;
			}
			
		};
		assignIdBox.addItemListener(new ItemUpdater(assignIdFunctor));
		
		final Functor<Void, Participant> bdayUpdater = new Functor<Void, Participant>() {
			
			@Override
			public Void op(Participant obj) {
				final DateTime bday = bdayField.getValue();
				participant.setBirthDate(bday);
				if(participant.getAge(null) == null) {
					if(sessionDate != null
						&& sessionDate.isAfter(participant.getBirthDate())) {
						final Period age = participant.getAge(sessionDate);
						ageField.setPrompt(AgeFormatter.ageToString(age));
						ageField.setKeepPrompt(true);
					} else {
						ageField.setPrompt("YY:MM.DD");
						ageField.setKeepPrompt(false);
					}
				}
				return null;
			}
			
		};
		bdayField.getDocument().addDocumentListener(new TextFieldUpdater(bdayUpdater));
		bdayField.addActionListener(new ActionUpdater(bdayUpdater));
		
		final Functor<Void, Participant> ageUpdater = new Functor<Void, Participant>() {
			
			@Override
			public Void op(Participant obj) {
				if(ageField.getText().trim().length() == 0) {
					participant.setAge(null);
				} else {
					final Period p = ageField.getValue();
					participant.setAge(p);
				}
				return null;
			}
			
		};
		ageField.getDocument().addDocumentListener(new TextFieldUpdater(ageUpdater));
		
		// ensure a role is selected!
		if(participant.getRole() == null) {
			roleBox.setSelectedItem(ParticipantRole.TARGET_CHILD);
		}
		
		final CellConstraints cc = new CellConstraints();
		final FormLayout reqLayout = new FormLayout(
				"right:60px, 3dlu, fill:pref:grow",
				"pref, pref, pref");
		final JPanel required = new JPanel(reqLayout);
		required.setBorder(BorderFactory.createTitledBorder("Required Information"));
		required.add(new JLabel("Role:"), cc.xy(1,1));
		required.add(roleBox, cc.xy(3,1));
		required.add(assignIdBox, cc.xy(3,2));
		required.add(new JLabel("Id:"), cc.xy(1, 3));
		required.add(idField, cc.xy(3, 3));
		
		final FormLayout optLayout = new FormLayout(
				"right:60px, 3dlu, fill:pref:grow, 50px, pref",
				"pref, pref, pref, pref, pref, pref, pref, pref, pref, pref, pref");
		final JPanel optional = new JPanel(optLayout);
		optional.setBorder(BorderFactory.createTitledBorder("Optional Information"));
		optional.add(new JLabel("Name"), cc.xy(1, 1));
		optional.add(nameField, cc.xyw(3, 1, 3));
		optional.add(new JLabel("Sex"), cc.xy(1, 2));
		optional.add(sexBox, cc.xyw(3, 2, 3));
		optional.add(new JLabel("Birthday"), cc.xy(1, 3));
		optional.add(bdayField, cc.xyw(3, 3, 2));
		optional.add(calendarButton, cc.xy(5, 3));
		optional.add(new JLabel("Age"), cc.xy(1, 5));
		optional.add(ageField, cc.xyw(3, 5, 3));
		optional.add(new JLabel("Language"), cc.xy(1, 7));
		optional.add(languageField, cc.xyw(3, 7, 3));
		optional.add(new JLabel("Group"), cc.xy(1, 8));
		optional.add(groupField, cc.xyw(3, 8, 3));
		optional.add(new JLabel("Education"), cc.xy(1, 9));
		optional.add(educationField, cc.xyw(3, 9, 3));
		optional.add(new JLabel("SES"), cc.xy(1, 10));
		optional.add(sesField, cc.xyw(3, 10, 3));
		
		setLayout(new VerticalLayout(5));
		add(required);
		add(optional);
		add(ButtonBarBuilder.buildOkBar(anonymizeBtn));
		add(new JSeparator(SwingConstants.HORIZONTAL));
	}
	
	public void setOtherParticipants(List<Participant> parts) {
		this.otherParticipants = parts;
		
		if(participant.getRole() == null) {
			participant.setRole(ParticipantRole.TARGET_CHILD);
			participant.setId(getRoleId());
			idField.setText(participant.getId());
		}
	}
	
	public DateTime getSessionDate() {
		return this.sessionDate;
	}
	
	public void setSessionDate(DateTime sessionDate) {
		this.sessionDate = sessionDate;
		
		if(sessionDate != null && participant.getAge(null) == null
				&& participant.getBirthDate() != null 
				&& participant.getBirthDate().isBefore(sessionDate)) {
			final Period age = participant.getAge(sessionDate);
			ageField.setPrompt(AgeFormatter.ageToString(age));
			ageField.setKeepPrompt(true);
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
	
	public void onShowCalendar() {
		final JXMonthView monthView =
				(participant.getBirthDate() != null ? new JXMonthView(participant.getBirthDate().toDate()) : new JXMonthView());
		monthView.setTraversable(true);
		monthView.setBorder(BorderFactory.createEtchedBorder());
		if(participant.getBirthDate() != null) {
			monthView.setSelectionDate(participant.getBirthDate().toDate());
		}
		if(sessionDate != null) {
			monthView.setFlaggedDates(sessionDate.toDate());
		}
		
		monthView.getSelectionModel().addDateSelectionListener(new DateSelectionListener() {
			
			@Override
			public void valueChanged(DateSelectionEvent ev) {
				bdayField.setValue(new DateTime(monthView.getSelectionDate()));
				
			}
		});
		final JPopupMenu popup = new JPopupMenu();
		popup.add(monthView);
		popup.show(calendarButton, 0, calendarButton.getHeight());
	}
	
	public void onAnonymize() {
		// show confirmation
		final MessageDialogProperties props = new MessageDialogProperties();
		props.setParentWindow(CommonModuleFrame.getCurrentFrame());
		props.setRunAsync(false);
		props.setHeader("Anonymize participant information?");
		props.setMessage("Remove all optional information for this participant?");
		props.setOptions(MessageDialogProperties.okCancelOptions);
		
		final int retVal = NativeDialogs.showMessageDialog(props);
		if(retVal == 1) return;
		
		idField.setText(getRoleId());
		nameField.setText("");
		bdayField.setText("");
		sexBox.setSelectedItem(Sex.UNSPECIFIED);
		ageField.setText("");
		languageField.setText("");
		groupField.setText("");
		educationField.setText("");
		sesField.setText("");
	}
	
	public Participant getParticipant() {
		return this.participant;
	}
	
	private class ItemUpdater implements ItemListener {
		
		private final Functor<Void, Participant> updater;
		
		public ItemUpdater(Functor<Void, Participant> updater) {
			this.updater = updater;
		}

		@Override
		public void itemStateChanged(ItemEvent e) {
			updater.op(participant);
		}
		
	}
	
	private class ActionUpdater implements ActionListener {
		
		private final Functor<Void, Participant> updater;
		
		public ActionUpdater(Functor<Void, Participant> updater) {
			this.updater = updater;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			updater.op(participant);
		}
		
	}
	
	private class TextFieldUpdater implements DocumentListener {

		private final Functor<Void, Participant> updater;
		
		public TextFieldUpdater(Functor<Void, Participant> updater) {
			this.updater = updater;
		}
		
		@Override
		public void insertUpdate(DocumentEvent e) {
			updater.op(participant);
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			updater.op(participant);
		}

		@Override
		public void changedUpdate(DocumentEvent e) {
			
		}
		
	}
	
	private class PropertyUpdater implements PropertyChangeListener {
		
		private final Functor<Void, Participant> updater;
		
		public PropertyUpdater(Functor<Void, Participant> updater) {
			super();
			this.updater = updater;
		}

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			updater.op(participant);
		}
		
	}
	
}
