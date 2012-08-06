package ca.phon.ui.searchfield;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.SystemColor;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import ca.phon.ui.action.PhonActionEvent;
import ca.phon.ui.action.PhonUIAction;
import ca.phon.ui.icons.IconSize;

/**
 * A search field with optional context button.
 * The field displayes a prompt when the text field
 * text is empty.
 *
 */
public class SearchField extends JTextField {
	
	/**
	 * Text field state
	 */
	public static enum FieldState {
		UNDEFINED,
		PROMPT,
		INPUT;
		
		private Color[] stateColors = {
				Color.red,
				Color.lightGray,
				SystemColor.textText
		};
		
		public Color getColor() {
			return stateColors[ordinal()];
		}
	};
	
	private final static String STATE_PROPERTY = "_search_field_state_";
	
	/**
	 * Current state
	 */
	private FieldState fieldState = FieldState.UNDEFINED;

	/**
	 * Search field prompt
	 */
	private String prompt = "Search";
	
	/**
	 * Search context button
	 */
	private SearchFieldButton ctxButton;
	
	private SearchFieldButton endButton;
	
	/**
	 * Search icon
	 * 
	 */
//	private ImageIcon searchIcn = null;
	
	public SearchField() {
		this("Search");
	}
	
	public SearchField(String prompt) {
		init();
		this.prompt = prompt;
		setState(FieldState.PROMPT);
		addFocusListener(focusStateListener);
	}
	
	@Override
	public Dimension getPreferredSize() {
		Dimension retVal = super.getPreferredSize();
		retVal.height = Math.max(retVal.height, 25);
		return retVal;
	}
	
	private BufferedImage clearIcn = null;
	private BufferedImage createClearIcon() {
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
	private BufferedImage createSearchIcon() {
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
		// load search icon
		searchIcn = 
			createSearchIcon();
		PhonUIAction ctxAction = new PhonUIAction(this, "onShowContextMenu");
//		ctxAction.putValue(PhonUIAction.SMALL_ICON, searchIcn);
		ctxAction.putValue(PhonUIAction.SHORT_DESCRIPTION, "Click for options");
	
		final int borderInset = 10;
		
		ctxButton = new SearchFieldButton(SwingConstants.LEFT, createSearchIcon());
		ctxButton.setAction(ctxAction);
		ctxButton.setCursor(Cursor.getDefaultCursor());
		super.addComponentListener(new ComponentListener() {
			
			@Override
			public void componentShown(ComponentEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void componentResized(ComponentEvent arg0) {
				ctxButton.setBounds(0, 0, searchIcn.getWidth()+borderInset, getHeight());
				endButton.setBounds(getWidth()-(IconSize.SMALL.getWidth()+borderInset), 0,
						IconSize.SMALL.getWidth()+borderInset, getHeight());
			}
			
			@Override
			public void componentMoved(ComponentEvent arg0) {
				
			}
			
			@Override
			public void componentHidden(ComponentEvent arg0) {
			}
		});
		add(ctxButton);
		
		PhonUIAction clearTextAct = new PhonUIAction(this, "onClearText");
		clearTextAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Clear field");
		endButton = new SearchFieldButton(SwingConstants.RIGHT, null);
		endButton.setAction(clearTextAct);
		endButton.setCursor(Cursor.getDefaultCursor());
		add(endButton);
		
		// setup an empty border allowing for the
		// extra space needed for drawing
		int leftSpace = searchIcn.getWidth()+borderInset;
		int rightSpace = IconSize.SMALL.getWidth()+borderInset;
		int topSpace = 0;
		int btmSpace = 0;
		Border emptyBorder = BorderFactory.createEmptyBorder(topSpace, leftSpace, btmSpace, rightSpace);
		setBorder(BorderFactory.createCompoundBorder(emptyBorder, 
				BorderFactory.createMatteBorder(1, 0, 1, 0, Color.gray)));
	}
	
	/**
	 * Displays the context menu for the component.  By default,
	 * this displays a single option for clearing the text field.
	 * Subclasses should override to define custom options.
	 * 
	 * @param pae
	 */
	public void onShowContextMenu(PhonActionEvent pae) {
		JPopupMenu menu = new JPopupMenu();
		
		PhonUIAction clearFieldAct = new PhonUIAction(this, "onClearText");
		clearFieldAct.putValue(PhonUIAction.NAME, "Clear text");
		JMenuItem clearTextItem = new JMenuItem(clearFieldAct);
		
		menu.add(clearTextItem);
		
		menu.show(ctxButton, 0, ctxButton.getHeight());
	}
	
	public void onClearText(PhonActionEvent pae) {
		setText("");
	}
	
	@Override
	public String getText() {
		String retVal = super.getText();
		if(this.fieldState == FieldState.PROMPT) {
			retVal = "";
		}
		return retVal;
	}
	
	@Override
	public void setText(String s) {
		if(s == null) s = "";


		super.setText(s);
		if(hasFocus() && s.length() > 0) {
			setState(FieldState.INPUT);
		} else if(s.length() == 0) {
			setState(FieldState.PROMPT);
		}
	}
	
	public String getPrompt() {
		return this.prompt;
	}
	
	public void setPrompt(String prompt) {
		this.prompt = prompt;
		if(getState() == FieldState.PROMPT)
			super.setText(prompt);
	}
	
	/**
	 * Set state of field
	 * 
	 * @param state
	 */
	public void setState(FieldState state) {
		if(this.fieldState == state) return;
		FieldState oldState = this.fieldState;
		this.fieldState = state;
		
		if(this.fieldState == FieldState.PROMPT) {
			if(oldState == FieldState.INPUT && super.getText().length() > 0)
				throw new IllegalStateException("Cannot set state to PROMPT when field has input.");
			super.setForeground(this.fieldState.getColor());
			super.setText(prompt);
			
			endButton.setIcn(null);
			endButton.setEnabled(false);
		} else if(this.fieldState == FieldState.INPUT) {
			super.setForeground(this.fieldState.getColor());
			super.setText("");
			
			endButton.setIcn(createClearIcon());
			endButton.setEnabled(true);
		}
		
		super.firePropertyChange(STATE_PROPERTY, oldState, this.fieldState);
	}
	
	public FieldState getState() {
		return this.fieldState;
	}
	
	/**
	 * State change on focus
	 * 
	 */
	private static FocusListener focusStateListener = new FocusListener() {

		@Override
		public void focusGained(FocusEvent arg0) {
			SearchField sf = (SearchField)arg0.getSource();
			if(sf.fieldState == FieldState.PROMPT) {
				sf.setState(FieldState.INPUT);
			}
		}

		@Override
		public void focusLost(FocusEvent arg0) {
			SearchField sf = (SearchField)arg0.getSource();
			if(sf.getText().length()==0) {
				sf.setState(FieldState.PROMPT);
			}
		}
	};
	
	/**
	 * Shaped label to give button a rounded look on both sides
	 */
	private class EndLabel extends JLabel {
		
		@Override
		public void paintComponent(Graphics g) {
			// setup graphics context
			Graphics2D g2d = (Graphics2D)g;
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			
			// create button shape
			int w = super.getWidth();
			int h = super.getHeight();
			
			Shape circle = new Ellipse2D.Float(-w/2-2, 0, h-1, h-1);
			Area lblShape = new Area(circle);
			
			g2d.setColor(super.getBackground());
			g2d.fillRect(0, 0, w, h);
			
			GradientPaint gp = new GradientPaint(new Point(0,0), new Color(215, 215, 215), 
					new Point(0, h), new Color(200, 200, 200));
//			g2d.setColor(gp);
//			g2d.setPaint(gp);
			g2d.setColor(SearchField.this.getBackground());
			g2d.fill(lblShape);
			
			g2d.setColor(Color.gray);
			g2d.draw(lblShape);
		}
		
	}
	
	/**
	 * Custom shaped button for the search field
	 */
	private class SearchFieldButton extends JButton {
		
		private int side = SwingConstants.LEFT;
		
		private Image icn = null;
		
		public SearchFieldButton(int side, Image icn) {
			this.side = side;
			this.icn = icn;
			super.setOpaque(false);
		}

		public void setIcn(Image icn) {
			this.icn = icn;
		}
		
		public Image getIcn() {
			return this.icn;
		}
		
		@Override
		protected void paintComponent(Graphics arg0) {
			// setup graphics context
			Graphics2D g2d = (Graphics2D)arg0;
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			
			// create button shape
			int w = super.getWidth();
			int h = super.getHeight();
			
			Area btnShape = new Area();
			if(side == SwingConstants.LEFT) {
//				Shape circle = new Ellipse2D.Float(1, 0, h-1.0f, h-1.0f);
				Shape roundRect = new RoundRectangle2D.Float(1.0f, 0.0f, w*2, h-1, h, h);
//				Shape square = new Rectangle2D.Float(h/2.0f+1.0f, 0.0f, w-(h/2.0f)+1, h-1.0f);
				btnShape.add(new Area(roundRect));
//				btnShape.add(new Area(square));
			} else if(side == SwingConstants.RIGHT) {
				Shape roundRect = new RoundRectangle2D.Float(-w, 0.0f, w*2-1, h-1, h, h);
//				Shape square = new Rectangle2D.Float(0.0f, 0.0f, w/2, h-1.0f);
				btnShape.add(new Area(roundRect));
//				btnShape.add(new Area(square));
			}
			
			g2d.setColor(super.getBackground());
			g2d.fillRect(0, 0, w, h);

			GradientPaint gp = new GradientPaint(new Point(0,0), new Color(215, 215, 215), 
					new Point(0, h), new Color(200, 200, 200));
//			g2d.setColor(gp);
//			g2d.setPaint(gp);
			g2d.setColor(SearchField.this.getBackground());
			g2d.fill(btnShape);
			
			// there is sometimes a single pixel artifact left
			// over from the shape intersection.  fix this
			if(side == SwingConstants.LEFT) {
				g2d.fillRect(h/2, 1, 2, h-1);
			} else if(side == SwingConstants.RIGHT) {
				g2d.fillRect(getWidth()-(h/2)-1, 1, 2, h-1);
			}
			
			g2d.setColor(Color.gray);
			g2d.draw(btnShape);
			
			if(icn != null ) {
				int btnY = h/2 - icn.getHeight(this)/2;
				int btnX = w/2 - icn.getWidth(this)/2;
				g2d.drawImage(icn, btnX, btnY, null);
			}
			
//			Rectangle2D rectToRemove = new Rectangle2D.Float(0, h/2, w, h/2);
//			Area areaToRemove = new Area(rectToRemove);
//			Area topArea = (Area)btnShape.clone();
//			topArea.subtract(areaToRemove);
//			 gp = new GradientPaint(new Point(0,0), new Color(255, 255, 255, 75), 
//					new Point(0, h/2), new Color(255, 255, 255, 25));
//			g2d.setPaint(gp);
//			g2d.fill(topArea);
		}
		
	}
}
