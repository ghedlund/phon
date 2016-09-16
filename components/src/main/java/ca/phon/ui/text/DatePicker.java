/*
 * Phon - An open source tool for research in phonology.
 * Copyright (C) 2005 - 2016, Gregory Hedlund <ghedlund@mun.ca> and Yvan Rose <yrose@mun.ca>
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
package ca.phon.ui.text;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;

import org.jdesktop.swingx.JXDatePicker;
import org.jdesktop.swingx.JXMonthView;
import org.jdesktop.swingx.event.DateSelectionEvent;
import org.jdesktop.swingx.event.DateSelectionListener;

import ca.phon.session.DateFormatter;
import ca.phon.ui.action.PhonUIAction;
import ca.phon.ui.text.PromptedTextField.FieldState;
import ca.phon.ui.toast.ToastFactory;
import ca.phon.util.icons.IconManager;
import ca.phon.util.icons.IconSize;

/**
 * Customizations for {@link JXDatePicker}
 *
 */
public class DatePicker extends JComponent {

	private static final long serialVersionUID = -1181731542683596418L;
	
	public static final String DATETIME_PROP = "dateTime";
	
	private FormatterTextField<LocalDate> textField;
	
	private JButton monthViewButton;
	
	private JXMonthView monthView;
	
	private boolean valueIsAdjusting;
	
	public DatePicker() {
		super();
		
		init();
	}

	public boolean isValueAdjusing() {
		return this.valueIsAdjusting;
	}
	
	public void setValueIsAdjusting(boolean valueIsAdjusing) {
		this.valueIsAdjusting = valueIsAdjusing;
	}
	
	private void init() {
		textField = new FormatterTextField<LocalDate>(new DateFormatter());
		textField.setPrompt("YYYY-MM-DD");
		textField.setToolTipText("Enter date in format YYYY-MM-DD");
		textField.addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent e) {
				if(textField.getState() == FieldState.INPUT && !textField.validateText()) {
					ToastFactory.makeToast("Date format: " + DateFormatter.DATETIME_FORMAT).start(textField);
					Toolkit.getDefaultToolkit().beep();
					textField.requestFocus();
				}
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				
			}
		});
		
		monthView = new JXMonthView();
		monthView.setTraversable(true);
		
		final ImageIcon calIcon = 
				IconManager.getInstance().getIcon("apps/office-calendar", IconSize.SMALL);
		
		final PhonUIAction monthViewAct = new PhonUIAction(this, "onShowMonthView");
		monthViewAct.putValue(PhonUIAction.SMALL_ICON, calIcon);
		monthViewAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Show calendar");
		monthViewButton = new JButton(monthViewAct);
		
		setLayout(new BorderLayout());
		add(textField, BorderLayout.CENTER);
		add(monthViewButton, BorderLayout.EAST);
	}
	
	public LocalDate getDateTime() {
		return textField.getValue();
	}
	
	public void setDateTime(LocalDate dateTime) {
		textField.setValue(dateTime);
	}
	
	public JXMonthView getMonthView() {
		return this.monthView;
	}
	
	public FormatterTextField<LocalDate> getTextField() {
		return this.textField;
	}
	
	public void onShowMonthView() {
		final JXMonthView monthView = getMonthView();
		monthView.setTraversable(true);
		monthView.setBorder(BorderFactory.createEtchedBorder());
		
		if(textField.getValue() != null) {
			final Date date = Date.from(textField.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant());
			monthView.setFirstDisplayedDay(date);
			monthView.setSelectionDate(date);
		}
		
		monthView.getSelectionModel().addDateSelectionListener(new DateSelectionListener() {
			
			@Override
			public void valueChanged(DateSelectionEvent ev) {
				textField.setValue(LocalDate.from(monthView.getSelectionDate().toInstant()));
			}
			
		});
		final JPopupMenu popup = new JPopupMenu();
		popup.add(monthView);
		popup.show(monthViewButton, 0, monthViewButton.getHeight());
	}
	
//	
//	private final FocusListener fl = new FocusListener() {
//		
//		String initialVal = null;
//		
//		@Override
//		public void focusLost(FocusEvent e) {
//			final String curVal = getEditor().getText();
//			if(initialVal != null && !initialVal.equals(curVal)) {
//				final DateTime newDate = dateTimeDoc.getDateTime();
//				setDate(newDate.toDate());
//				fireActionPerformed(COMMIT_KEY);
//			}
//		}
//		
//		@Override
//		public void focusGained(FocusEvent e) {
//			initialVal = getEditor().getText();
//		}
//		
//	};
//	
}
