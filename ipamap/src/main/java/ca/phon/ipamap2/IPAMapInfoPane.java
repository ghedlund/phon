package ca.phon.ipamap2;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import org.apache.commons.lang3.StringUtils;
import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.JXStatusBar;
import org.jdesktop.swingx.JXCollapsiblePane.Direction;
import org.jdesktop.swingx.JXStatusBar.Constraint.ResizeBehavior;

import ca.phon.ipa.features.FeatureMatrix;
import ca.phon.ipa.features.FeatureSet;
import ca.phon.ipa.parser.IPATokens;
import ca.phon.ui.fonts.FontPreferences;
import ca.phon.ui.ipamap.io.Cell;

public class IPAMapInfoPane extends JPanel {
	
	private JXStatusBar statusBar;
	
	private JLabel statusLabel;
	
	private JXCollapsiblePane collapsiblePane;
	
	private JLabel previewLabel;
	
	private JEditorPane infoPane;
	
	public IPAMapInfoPane() {
		super();
		
		init();
	}
	
	private void init() {
		setLayout(new BorderLayout());
		
		statusBar = new JXStatusBar();
		statusLabel = new JLabel("[]");
		
		JXStatusBar.Constraint c1 = new JXStatusBar.Constraint(ResizeBehavior.FILL);
		statusBar.add(statusLabel, c1);
		
		add(statusBar, BorderLayout.NORTH);
		
		JPanel bottomPanel = new JPanel(new BorderLayout());
		collapsiblePane = new JXCollapsiblePane(Direction.UP);
		collapsiblePane.setContentPane(bottomPanel);
		collapsiblePane.setCollapsed(false);
		
		previewLabel = new JLabel();
		previewLabel.setFont(FontPreferences.getUIIpaFont().deriveFont(72.0f));
		previewLabel.setOpaque(true);
		previewLabel.setBackground(Color.white);
		previewLabel.setHorizontalAlignment(SwingConstants.CENTER);
		previewLabel.setPreferredSize(new Dimension(150, 150));
		
		infoPane = new JEditorPane("text/html", "<html><body></body></html>");
		infoPane.setEditable(false);
		infoPane.setOpaque(true);
		infoPane.setBackground(getBackground());
		infoPane.setPreferredSize(new Dimension(0, 150));
		
		bottomPanel.add(previewLabel, BorderLayout.WEST);
		bottomPanel.add(new JScrollPane(infoPane), BorderLayout.CENTER);
		
		add(collapsiblePane, BorderLayout.CENTER);
	}
	
	public void clear() {
		statusLabel.setText("[]");
		
		previewLabel.setText("");
		infoPane.setText("<html><body></body></html>");
	}
	
	public void update(Cell cell) {
		previewLabel.setText(cell.getText());
		
		final IPATokens tokens = IPATokens.getSharedInstance();
		
		String uniVal = "";
		String name = "";
		
		String cellText = cell.getText().replaceAll("\u25cc", "");
		for(Character c:cellText.toCharArray()) {
			String cText = "0x" + StringUtils.leftPad(Integer.toHexString((int)c), 4, '0');
			uniVal += (uniVal.length() > 0 ? " + " : "") + cText;
			
			String cName = tokens.getCharacterName(c);
			name += (name.length() > 0 ? " + " : "") + cName;
		}
		
		// create feature set
		FeatureMatrix fm = FeatureMatrix.getInstance();
		FeatureSet customFs = new FeatureSet();
		String cellData = cell.getText().replaceAll(""+(char)0x25cc, "");
		for(Character c:cellData.toCharArray()) {
			FeatureSet fs = fm.getFeatureSet(c);
			if(fs != null) {
				customFs = FeatureSet.union(customFs, fs);
			}
		}
		
		String fsString = customFs.toString();
		fsString = fsString.substring(1, fsString.length()-1);
		
		String infoTxt = "<html><table border='0'>" +
				"<tr><td>Name:</td><td>" + name + "</td></tr>" +
				"<tr><td>Unicode:</td><td>" + uniVal + "</td></tr>" +
				"<tr><td>Features:</td><td>" + fsString + "</td></tr>" +
				"</table></html>";
		infoPane.setText(infoTxt);
		
		String statusText = String.format("[%s] %s", uniVal, name);
		statusLabel.setText(statusText);
	}

}
