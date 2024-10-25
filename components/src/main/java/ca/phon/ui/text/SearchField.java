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
package ca.phon.ui.text;

import ca.phon.ui.action.*;
import ca.phon.ui.text.PromptedTextField.FieldState;
import ca.phon.util.PrefHelper;
import ca.phon.util.icons.IconSize;
import com.jgoodies.forms.layout.*;

import javax.swing.*;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.beans.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A search field with optional context button.
 * The field displayes a prompt when the text field
 * text is empty.
 *
 */
public class SearchField extends JPanel {
	
	private static final long serialVersionUID = 839864308294242792L;

	private final static String HISTORY_SEPARATOR = ";;";

	private final String historyProperty;

	private final List<String> history = new ArrayList<>();

	/**
	 * Search context button
	 */
	protected SearchFieldButton ctxButton;
	
	protected SearchFieldButton endButton;
	
	protected final PromptedTextField queryField;
	
	/**
	 * Search icon
	 * 
	 */
//	private ImageIcon searchIcn = null;
	public SearchField() {
		this(null);
	}

	public SearchField(String prompt) {
		this(null, prompt);
	}
	
	public SearchField(String historyProperty, String prompt) {
		super();

		setBackground(Color.white);
		setOpaque(true);
		setFocusable(true);

		queryField = new PromptedTextField(prompt);
		queryField.setBackground(Color.white);
		queryField.setOpaque(true);
		queryField.addPropertyChangeListener(PromptedTextField.STATE_PROPERTY, fieldStateListener);

		this.historyProperty = historyProperty;
		if(historyProperty != null) {
			loadHistory();
		}

		updateUI();
		init();
	}
	
	private BufferedImage clearIcn = null;
	public BufferedImage createClearIcon() {
		if(clearIcn == null) {
			clearIcn = new BufferedImage(IconSize.SMALL.getWidth(), IconSize.SMALL.getHeight(),
					BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2d = (Graphics2D)clearIcn.getGraphics();
			
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			
			g2d.setColor(new Color(210, 210, 210));
			
			Ellipse2D circle =
				new Ellipse2D.Float(2, 2, IconSize.SMALL.getWidth()-2, IconSize.SMALL.getHeight()-2);
			g2d.fill(circle);
			
			Stroke s = new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
			g2d.setStroke(s);
			
			g2d.setColor(Color.white);
			g2d.drawLine(6, 6, IconSize.SMALL.getWidth()-5, IconSize.SMALL.getHeight()-5);
			g2d.drawLine(IconSize.SMALL.getWidth()-5, 6, 6, IconSize.SMALL.getHeight()-5);
		}
		return clearIcn;
	}
	
	private BufferedImage searchIcn = null;
	public BufferedImage createSearchIcon() {
		if(searchIcn == null) {
		BufferedImage retVal = new BufferedImage(IconSize.SMALL.getWidth()+8, IconSize.SMALL.getHeight(),
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = (Graphics2D)retVal.getGraphics();
		
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		Ellipse2D circle = new Ellipse2D.Float(2, 2, 
				10, 10);
		Line2D stem = new Line2D.Float(11, 11,
				IconSize.SMALL.getWidth()-2, IconSize.SMALL.getHeight()-2);
		
		Polygon tri = new Polygon();
		tri.addPoint(16, 8);
		tri.addPoint(24, 8);
		tri.addPoint(20, 12);
		
//		Line2D triA = new Line2D.Float(14.0f, 9.0f, 17.0f, 9.0f);
//		Line2D triB = new Line2D.Float(17.0f, 9.0f, 15.5f, 11.0f);
//		Line2D triC = new Line2D.Float(15.5f, 11.0f, 14.0f, 9.0f);
		
		Stroke s = new BasicStroke(2.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		g2d.setStroke(s);
		g2d.setColor(Color.gray);
		
		g2d.draw(circle);
		g2d.draw(stem);
		
		g2d.fillPolygon(tri);

//		s = new BasicStroke(0.5f);
//		g2d.setStroke(s);
//		
//		g2d.draw(triA);
//		g2d.draw(triB);
//		g2d.draw(triC);
		searchIcn = retVal;
		}
		return searchIcn;
	}
	
	private void init() {
		final FormLayout layout = new FormLayout(
				"pref, 3dlu, fill:pref:grow, 3dlu, pref", "pref");
		setLayout(layout);
		final CellConstraints cc = new CellConstraints();
		
		// load search icon
		final ImageIcon searchIcon = new ImageIcon(createSearchIcon());
		final PhonUIAction ctxAction = PhonUIAction.eventConsumer(this::onShowContextMenu);
		ctxAction.putValue(PhonUIAction.SMALL_ICON, searchIcon);
		ctxAction.putValue(PhonUIAction.SHORT_DESCRIPTION, "Click for options");
		ctxButton = new SearchFieldButton(ctxAction);
		ctxButton.setCursor(Cursor.getDefaultCursor());
		ctxButton.setFocusable(false);
		add(ctxButton, cc.xy(1,1));
		
		add(queryField, cc.xy(3,1));
		
		final ImageIcon clearIcon = new ImageIcon(createClearIcon());
		final PhonUIAction clearTextAct = PhonUIAction.eventConsumer(this::onClearText);
		clearTextAct.putValue(PhonUIAction.SMALL_ICON, clearIcon);
		clearTextAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Clear field");
		endButton = new SearchFieldButton(clearTextAct);
		endButton.setCursor(Cursor.getDefaultCursor());
		endButton.setDrawIcon(false);
		add(endButton, cc.xy(5,1));
		
		setBorder(queryField.getBorder());
		final Insets insets = queryField.getBorder().getBorderInsets(queryField);
		queryField.setBorder(BorderFactory.createEmptyBorder(insets.top, insets.left, insets.bottom, insets.right));
	}
	
	@Override
	public void setFocusable(boolean b) {
		super.setFocusable(true);
	}
	
	/**
	 * Get the query text
	 * 
	 * @return query text
	 */
	public String getQuery() {
		return queryField.getText();
	}
	
	public Action getAction() {
		return queryField.getAction();
	}
	
	public void setAction(Action action) {
		queryField.setAction(action);
	}
	
	/**
	 * Displays the context menu for the component.  By default,
	 * this displays a single option for clearing the text field.
	 * Subclasses should override to define custom options.
	 * 
	 * @param pae
	 */
	public void onShowContextMenu(PhonActionEvent<Void> pae) {
		JPopupMenu menu = new JPopupMenu();
		
		setupPopupMenu(menu);
		
		menu.show(ctxButton, 0, ctxButton.getHeight());
	}
	
	public JButton getContextButton() {
		return this.ctxButton;
	}
	
	public JButton getEndButton() {
		return this.endButton;
	}
	
	public String getPrompt() {
		return queryField.getPrompt();
	}

	public void setPrompt(String prompt) {
		queryField.setPrompt(prompt);
	}

	public int getColumns() {
		return queryField.getColumns();
	}

	public void setColumns(int columns) {
		queryField.setColumns(columns);
	}
	
	public Document getDocument() {
		return queryField.getDocument();
	}
	
	public PromptedTextField getTextField() {
		return queryField;
	}

	/**
	 * Setup popup menu.
	 * 
	 * @param menu
	 */
	protected void setupPopupMenu(JPopupMenu menu) {
		PhonUIAction clearFieldAct = PhonUIAction.eventConsumer(this::onClearText);
		clearFieldAct.putValue(PhonUIAction.NAME, "Clear text");
		JMenuItem clearTextItem = new JMenuItem(clearFieldAct);
		
		menu.add(clearTextItem);
	}
	
	public void setState(String state) {
		queryField.setState(state);
	}

	public FieldState getState() {
		return queryField.getState();
	}

	public String getText() {
		return queryField.getText();
	}

	public void setText(String s) {
		queryField.setText(s);
	}

	public void onClearText(PhonActionEvent<Void> pae) {
		queryField.setText("");
		firePropertyChange("text_cleared", false, true);
	}
	
	/**
	 * Listener for field state changes
	 */
	private final PropertyChangeListener fieldStateListener = new PropertyChangeListener() {
		
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if(evt.getPropertyName().equals(PromptedTextField.STATE_PROPERTY)) {
				PromptedTextField.FieldState state = queryField.getState();
				if(state == FieldState.PROMPT) {
					endButton.setDrawIcon(false);
				} else if(state == FieldState.INPUT) {
					endButton.setDrawIcon(true);
				}
				endButton.repaint();
			}
		}
		
	};
	
	private final class SearchFieldButton extends JButton {
		
		private boolean drawIcon = true;
		
		public SearchFieldButton(Action a) {
			super(a);	
		}
		
		public boolean isDrawIcon() {
			return drawIcon;
		}
		
		public void setDrawIcon(boolean v) {
			this.drawIcon = v;
		}
		
		@Override
		public Dimension getPreferredSize() {
			final Dimension retVal = super.getPreferredSize();
			
			final Icon icon = getIcon();
			if(icon != null) {
				retVal.width = icon.getIconWidth();
				retVal.height = icon.getIconHeight();
			}
			
			return retVal;
		}
		
		@Override
		public void paintComponent(Graphics g) {
			g.setColor(Color.white);
			
			int width = getWidth();
			int height = getHeight();
			
			g.fillRect(0, 0, width, height);
			
			if(isDrawIcon()) {
				final Icon icon = getIcon();
				int x = (width/2) - (icon.getIconWidth()/2);
				int y = (height/2) - (icon.getIconHeight()/2);
				icon.paintIcon(this, g, x, y);
			}
		}
		
	}

	/**
	 * Load history from preferences
	 */
	public void loadHistory() {
		history.clear();
		final String historyStr = PrefHelper.get(historyProperty, "");
		final String[] historyArr = historyStr.split(HISTORY_SEPARATOR);
		for (String h : historyArr) {
			if (!h.isEmpty()) {
				history.add(h);
			}
		}
	}

	/**
	 * Append given text to history
	 *
	 * @param text
	 * @return <code>true</code> if text was added to history, <code>false</code> otherwise
	 */
	public boolean appendToHistory(String text) {
		if (text == null || text.isEmpty()) {
			return false;
		}
		if (history.contains(text)) {
			return false;
		}
		history.add(text);
		return true;
	}

	/**
	 * Save history to preferences
	 *
	 * @return <code>true</code> if history was saved, <code>false</code> otherwise
	 */
	public boolean saveHistory() {
		final StringBuilder historyStr = new StringBuilder();
		for (String h : history) {
			historyStr.append(h).append(HISTORY_SEPARATOR);
		}
		PrefHelper.getUserPreferences().put(historyProperty, historyStr.toString());
		return true;
	}

}
