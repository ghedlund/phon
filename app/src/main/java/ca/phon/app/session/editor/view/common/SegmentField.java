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
package ca.phon.app.session.editor.view.common;

import ca.phon.app.log.LogUtil;
import ca.phon.formatter.MediaTimeFormatStyle;
import ca.phon.session.MediaSegment;
import ca.phon.session.format.MediaSegmentFormatter;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.*;
import java.text.ParseException;

public class SegmentField extends JFormattedTextField {
	
	private final Highlighter errHighlighter = new DefaultHighlighter();

	public SegmentField() {
		super();
		
		errHighlighter.install(this);
		setOpaque(false);
		this.setFormatterFactory(new SegmentFormatterFactory());
		
		setBorder(new GroupFieldBorder());
		
		getDocument().addDocumentListener(docListener);
	}
	
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		errHighlighter.paint(g);
	}
	
	@Override
	public Dimension getPreferredSize() {
		Dimension retVal = super.getPreferredSize();
		
		retVal.width += 5;
		
		return retVal;
	}
	
	public void validateText() {
		errHighlighter.removeAllHighlights();
		final MediaSegmentFormatter formatter = new MediaSegmentFormatter(MediaTimeFormatStyle.PADDED_MINUTES_AND_SECONDS);
		try {
			final MediaSegment seg = formatter.parse(getText());
			
			if(seg.getStartValue() > seg.getEndValue()) {
				((GroupFieldBorder)getBorder()).setShowWarningIcon(true);
				setToolTipText("Start time is after end time");
				try {
					errHighlighter.addHighlight(0, getText().length(), errPainter);
				} catch (BadLocationException e) {
					LogUtil.severe(e.getLocalizedMessage(), e);
				}
			} else {
				((GroupFieldBorder)getBorder()).setShowWarningIcon(false);
				setToolTipText(null);
			}
			
		} catch (ParseException e) {
			((GroupFieldBorder)getBorder()).setShowWarningIcon(true);
			setToolTipText(e.getLocalizedMessage());
		}
	}
	
	private DocumentListener docListener = new DocumentListener() {
		
		@Override
		public void removeUpdate(DocumentEvent e) {
			
		}
		
		@Override
		public void insertUpdate(DocumentEvent e) {
			validateText();
		}
		
		@Override
		public void changedUpdate(DocumentEvent e) {
			
		}
	};
	
	/** Formatter factory */
	private class SegmentFormatterFactory extends AbstractFormatterFactory {

		@Override
		public AbstractFormatter getFormatter(JFormattedTextField arg0) {
			AbstractFormatter retVal = null;
			try {
				retVal = new MaskFormatter("###:##.###-###:##.###");
				((MaskFormatter)retVal).setPlaceholderCharacter('0');
			} catch (ParseException e) {
				LogUtil.severe(e.getLocalizedMessage(), e);
			}
			return retVal;
		}

	}
	
	private final Highlighter.HighlightPainter errPainter = new Highlighter.HighlightPainter() {
		
		@Override
		public void paint(Graphics g, int p0, int p1, Shape bounds, JTextComponent c) {
			final Graphics2D g2 = (Graphics2D)g;
			
			Rectangle b = bounds.getBounds();
			try {
				final Rectangle p0rect = c.modelToView(p0);
				final Rectangle p1rect = c.modelToView(p1);
				
				b = new Rectangle(p0rect).union(p1rect);
			} catch (BadLocationException e) {
				
			}
			
			g2.setColor(Color.red);
			final float dash1[] = {1.0f};
		    final BasicStroke dashed =
		        new BasicStroke(1.0f,
		                        BasicStroke.CAP_BUTT,
		                        BasicStroke.JOIN_MITER,
		                        1.0f, dash1, 0.0f);
			g2.setStroke(dashed);
			g2.drawLine(b.x, 
					b.y + b.height - 1, 
					b.x + b.width, 
					b.y + b.height - 1);
		}
	};
}
