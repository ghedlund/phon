package ca.phon.media.player;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.plaf.SliderUI;

import ca.phon.ui.PhonGuiConstants;
import ca.phon.ui.fonts.FontPreferences;
import ca.phon.util.MsFormatter;

public class TimeSliderUI extends SliderUI {
	
	private JSlider slider;

	@Override
	public void installUI(JComponent c) {
		super.installUI(c);
		
		this.slider = (JSlider)c;
		slider.addMouseMotionListener(mouseOverListener);
		slider.addMouseListener(mouseOverListener);
		this.slider.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				slider.repaint();
			}
		});
	}

	@Override
	public void uninstallUI(JComponent c) {
		super.uninstallUI(c);
	}
	
	@Override
	public Dimension getPreferredSize(JComponent c) {
		Dimension retVal = super.getPreferredSize(c);
		if(retVal == null)
			retVal = new Dimension(0, 11);
		return retVal;
	}

	@Override
	public void paint(Graphics g, JComponent c) {
		int w = c.getWidth();
		int h = c.getHeight();
		
		final Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		
		// track
		final Rectangle2D rect = getTrackRect();
		final RoundRectangle2D roundRect = new RoundRectangle2D.Double(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight(), 
				h, h);
		
		GradientPaint gp = new GradientPaint(0.0f, 0.0f, Color.gray, 0.0f, h, Color.LIGHT_GRAY);
		g2.setPaint(gp);
		g2.fill(roundRect);
		
		int val = slider.getValue();
		int min = slider.getMinimum();
		int max = slider.getMaximum();
		
		if(slider.isEnabled()) {
		// fill
			double percentComplete = 
					(double)(val - min) / (double)(max - min);
	
			final Rectangle2D fillRect = 
					new Rectangle2D.Double(rect.getX(), rect.getY(), rect.getWidth() * percentComplete, rect.getHeight());
			final RoundRectangle2D fillRoundedRect = new RoundRectangle2D.Double(
					fillRect.getX(), fillRect.getY(), fillRect.getWidth() + h/2.0, fillRect.getHeight(), 
					h, h);
			final Area fillArea = new Area(fillRoundedRect);
			fillArea.intersect(new Area(roundRect));
			
			gp = new GradientPaint(0.0f, 0.0f, PhonGuiConstants.PHON_VIEW_TITLE_COLOR, 0.0f, h,
					PhonGuiConstants.PHON_VIEW_TITLE_COLOR.darker());
			g2.setPaint(gp);
			g2.fill(fillArea);
			
			// thumb
			double thumbWidth = rect.getHeight()-1;
			final Rectangle2D thumbRect = 
					new Rectangle2D.Double(fillRect.getMaxX()-thumbWidth/2.0, 0.0,
							thumbWidth, thumbWidth);
			final Ellipse2D thumbCircle = new Ellipse2D.Double(thumbRect.getX(), thumbRect.getY(), 
					thumbRect.getWidth(), thumbRect.getHeight());
			
			gp = new GradientPaint(0.0f, 0.0f, Color.lightGray, 0.0f, h, Color.gray);
			g2.setPaint(gp);
			g2.fill(thumbCircle);
			g2.setColor(Color.DARK_GRAY);
			g2.draw(thumbCircle);
		}
	}
	
	protected Rectangle2D getTrackRect() {
		int h = slider.getHeight();
		int w = slider.getWidth();
		return new Rectangle2D.Double(h/2.0, 0.0, w-h, h);
	}
	
	protected long posToTime(int x) {
		final Rectangle2D trackRect = getTrackRect();
		double perPixel = slider.getMaximum() / trackRect.getWidth();
		x = Math.min((int)trackRect.getMaxX(), 
				Math.max((int)trackRect.getX(), x));
		int pos = (int)Math.round(x - trackRect.getX());
		long time = Math.min(Math.round(perPixel * slider.getMaximum()),
				Math.max(0, Math.round(perPixel * pos)));
		return time;
	}
	
	private JFrame timeFrame = null;
	private JLabel timeLbl = null;
	
	private final MouseInputAdapter mouseOverListener = new MouseInputAdapter() {

		private boolean dragging = false;
		
		@Override
		public void mousePressed(MouseEvent me) {
			if(!slider.isEnabled()) return;
			slider.setValueIsAdjusting(true);
			slider.setValue((int)posToTime(me.getX()));
			slider.setValueIsAdjusting(false);
			
			dragging = true;
		}
		
		@Override
		public void mouseReleased(MouseEvent me) {
			if(!slider.isEnabled()) return;
			if(dragging) {
				dragging = false;
			}
		}
		
		@Override
		public void mouseEntered(MouseEvent e) {
			if(!slider.isEnabled()) return;
			slider.repaint();
			if(timeFrame == null || !timeFrame.isVisible()) {
				timeFrame = new JFrame();
				timeFrame.setFocusable(false);
				timeFrame.setFocusableWindowState(false);
				timeFrame.setUndecorated(true);
				timeFrame.getRootPane().putClientProperty("Window.shadow", Boolean.FALSE);
				
				timeLbl = new JLabel(" 000:00.00 ");
				timeLbl.setFont(FontPreferences.getSmallFont());
				
				timeFrame.add(timeLbl);
				timeFrame.pack();
			}
			timeFrame.setVisible(true);
		}

		@Override
		public void mouseExited(MouseEvent e) {
			if(!slider.isEnabled()) return;
			if(timeFrame != null) {
				timeFrame.setVisible(false);
			}
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			if(!slider.isEnabled()) return;
			if(timeFrame != null) {
				Point p = e.getPoint();
				
				long time = posToTime(p.x);
				
				p.y = 0 - timeFrame.getHeight();
				p.x -= timeFrame.getWidth()/2;
				
				timeLbl.setText(MsFormatter.msToDisplayString(time));
				SwingUtilities.convertPointToScreen(p, slider);
				timeFrame.setLocation(p.x, p.y);
			}
		}
		
		@Override
		public void mouseDragged(MouseEvent me) {
			mouseMoved(me);
			if(dragging) {
				slider.setValueIsAdjusting(true);
				slider.setValue((int)posToTime(me.getX()));
				slider.setValueIsAdjusting(false);
			}
		}
		
	};
	
}
