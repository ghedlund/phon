package ca.phon.ui.text;

import java.awt.Color;
import java.awt.Toolkit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import ca.phon.formatter.Formatter;
import ca.phon.formatter.FormatterFactory;

public class FormatterTextField<T> extends PromptedTextField {
	
	private static final Logger LOGGER = Logger
			.getLogger(FormatterTextField.class.getName());

	private static final long serialVersionUID = 4203616792431039321L;
	
	public static <K> FormatterTextField<K> createTextField(Class<K> type) {
		return new FormatterTextField<K>(type);
	}
	
	private final Formatter<T> formatter;
	
	private FormatterTextField(Class<T> type) {
		super();
		formatter = getFormatter(type);
		getDocument().addDocumentListener(docListener);
	}
	
	public FormatterTextField(Formatter<T> formatter) {
		super();
		this.formatter = formatter;
		getDocument().addDocumentListener(docListener);
	}
	
	@Override
	public void setDocument(Document document) {
		if(getDocument() != null)
			getDocument().removeDocumentListener(docListener);
		super.setDocument(document);
		document.addDocumentListener(docListener);
	}
	
	private Formatter<T> getFormatter(Class<T> type) {
		return FormatterFactory.createFormatter(type);
	}
	
	public boolean validateText() {
		final T value = getValue();
		if(value == null && getState() == FieldState.INPUT) {
			setForeground(Color.red);
		} else {
			setForeground(getState().getColor());
		}
		return value != null;
	}
	
	public T getValue() {
		T retVal = null;
		
		try {
			retVal = formatter.parse(getText());
			setToolTipText(null);
		} catch (Exception e) {
			setToolTipText(e.getMessage());
			LOGGER.log(Level.FINE, e.getLocalizedMessage(), e);
		}
		
		return retVal;
	}
	
	public void setValue(T val) {
		if(val == null) {
			setText("");
			super.setState(FieldState.PROMPT);
		} else {
			try {
				final String s = formatter.format(val);
				setText(s);
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
		}
	}
	
	private final DocumentListener docListener = new DocumentListener() {
		
		@Override
		public void removeUpdate(DocumentEvent e) {
			validateText();
		}
		
		@Override
		public void insertUpdate(DocumentEvent e) {
			validateText();
		}
		
		@Override
		public void changedUpdate(DocumentEvent e) {
			
		}
	};

	public class FormatterDocument extends PlainDocument {

		private static final long serialVersionUID = 3345418733077374861L;

		@Override
		public void insertString(int offs, String str, AttributeSet a)
				throws BadLocationException {
			// don't allow insertions that would allow 
			// bad data
			final StringBuilder sb = new StringBuilder();
			sb.append(super.getText(0, offs));
			sb.append(str);
			sb.append(super.getText(offs, super.getLength() - offs));
			try {
				formatter.parse(sb.toString());
				super.insertString(offs, str, a);
			} catch (Exception e) {
				Toolkit.getDefaultToolkit().beep();
			}
		}
		
	}
	
}
