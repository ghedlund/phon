package ca.phon.ui.text;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;

import javax.swing.JTextField;
import javax.swing.plaf.basic.BasicTextFieldUI;
import javax.swing.text.Caret;
import javax.swing.text.Highlighter;

import ca.phon.util.icons.IconSize;

/**
 * 
 */
public class SearchFieldUI extends BasicTextFieldUI {
	
	

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
	

}
