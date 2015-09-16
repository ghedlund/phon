package ca.phon.app.session.editor;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.File;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.GrayFilter;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.event.MouseInputAdapter;

import org.jdesktop.swingx.JXStatusBar;
import org.jdesktop.swingx.JXStatusBar.Constraint.ResizeBehavior;
import org.jdesktop.swingx.plaf.metal.MetalStatusBarUI;
import org.jdesktop.swingx.plaf.windows.WindowsClassicStatusBarUI;
import org.jdesktop.swingx.plaf.windows.WindowsStatusBarUI;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import ca.phon.project.Project;
import ca.phon.session.Session;
import ca.phon.ui.fonts.FontPreferences;
import ca.phon.util.ByteSize;
import ca.phon.util.OSInfo;
import ca.phon.util.OpenFileLauncher;
import ca.phon.util.icons.IconManager;
import ca.phon.util.icons.IconSize;
import ca.phon.worker.PhonTask;
import ca.phon.worker.PhonTaskListener;
import ca.phon.worker.PhonTask.TaskStatus;

public class SessionEditorStatusBar extends JXStatusBar {
	
	private final static Logger LOGGER = Logger.getLogger(SessionEditorStatusBar.class.getName());

	private static final long serialVersionUID = 286465072395883742L;

	/**
	 * Status bar progress
	 */
	private JLabel progressLabel;
	private JProgressBar progressBar;
	
	private final PhonTaskListener taskListener = new PhonTaskListener() {
		
		@Override
		public void statusChanged(PhonTask task, TaskStatus oldStatus, TaskStatus newStatus) {
			if(newStatus == TaskStatus.RUNNING) {
				progressLabel.setText(task.getName());
				progressBar.setIndeterminate(true);
			} else if(newStatus == TaskStatus.ERROR) {
				progressLabel.setText(task.getException().getLocalizedMessage());
				progressBar.setIndeterminate(false);
				progressBar.setValue(0);
				task.removeTaskListener(this);
			} else if(newStatus == TaskStatus.FINISHED) {
				progressLabel.setText("");
				progressBar.setIndeterminate(false);
				progressBar.setValue(0);
				task.removeTaskListener(this);
			}
		}
		
		@Override
		public void propertyChanged(PhonTask task, String property, Object oldValue, Object newValue) {
			if(PhonTask.PROGRESS_PROP.equals(property)) {
				final float percentComplete = (Float)newValue;
				if(percentComplete < 0) {
					progressBar.setIndeterminate(true);
				} else {
					progressBar.setIndeterminate(false);
					progressBar.setValue(Math.round(percentComplete*progressBar.getMaximum()));
				}
			}
		}
		
	};
	
	private JLabel statusLabel;
	
	private JLabel sessionPathLabel;
	
	private ImageIcon modifiedIcon;
	
	private ImageIcon unmodifiedIcon;
	
	private final WeakReference<SessionEditor> editorRef;
	
	public SessionEditorStatusBar(SessionEditor editor) {
		super();
		this.editorRef = new WeakReference<SessionEditor>(editor);
		
		getEditor().getEventManager().registerActionForEvent(EditorEventType.MODIFIED_FLAG_CHANGED, 
				(ee) -> { 
					if(getEditor().isModified()) {
						statusLabel.setIcon(modifiedIcon);
					} else {
						statusLabel.setIcon(unmodifiedIcon);
					}
					statusLabel.setToolTipText(getStatusTooltipText());
				} );
		
		init();
	}
	
	public SessionEditor getEditor() {
		return editorRef.get();
	}
	
	private void init() {
		statusLabel = new JLabel();
		
		modifiedIcon = IconManager.getInstance().getIcon("actions/document-save", IconSize.XSMALL);
		unmodifiedIcon = IconManager.getInstance().getDisabledIcon("actions/document-save", IconSize.XSMALL);
		if(getEditor().isModified()) {
			statusLabel.setIcon(modifiedIcon);
		} else {
			statusLabel.setIcon(unmodifiedIcon);
		}
		statusLabel.setToolTipText(getStatusTooltipText());
		add(statusLabel, new JXStatusBar.Constraint(IconSize.XSMALL.getWidth()));
		
		sessionPathLabel = new JLabel(getEditor().getSession().getCorpus() + "/" + 
				getEditor().getSession().getName());
		sessionPathLabel.setFont(FontPreferences.getSmallFont());
		sessionPathLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		sessionPathLabel.setToolTipText(
				getEditor().getProject().getSessionPath(getEditor().getSession()) + " (click to show corpus folder)");
		sessionPathLabel.addMouseListener(new MouseInputAdapter() {

			@Override
			public void mouseClicked(MouseEvent arg0) {
				if(arg0.getClickCount() == 1) {
					// XXX open session folder in file explorer
					final File corpusFolder = 
							new File(getEditor().getProject().getCorpusPath(getEditor().getSession().getCorpus()));
					try {
						OpenFileLauncher.openURL(corpusFolder.toURI().toURL());
					} catch (MalformedURLException e) {
						LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
					}
				}
			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
				sessionPathLabel.setForeground(Color.blue);
			}

			@Override
			public void mouseExited(MouseEvent arg0) {
				sessionPathLabel.setForeground(Color.gray);
			}
			
		});
		sessionPathLabel.setForeground(Color.gray);
		
		add(sessionPathLabel, new JXStatusBar.Constraint(ResizeBehavior.FILL));
		
		JComponent pbar = new JPanel(new FormLayout("pref", 
				(OSInfo.isMacOs() ? "10px" : "pref")));
		progressBar = new JProgressBar(SwingConstants.HORIZONTAL);
		progressBar.setIndeterminate(false);
		progressBar.setValue(0);
		pbar.add(progressBar, (new CellConstraints()).xy(1, 1));
		
		progressLabel = new JLabel();
		progressLabel.setFont(FontPreferences.getSmallFont());
		
		add(progressLabel, new JXStatusBar.Constraint(200));
		add(pbar, new JXStatusBar.Constraint(100));
	}
	
	private String getStatusTooltipText() {
		final StringBuffer buf = new StringBuffer();
		final SessionEditor editor = getEditor();
		
		final Project project = editor.getProject();
		final Session session = editor.getSession();
		
		if(editor.isModified()) {
			buf.append("*modified* ");
		}
		
		final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd@K:ma");
		buf.append("Last save:");
		buf.append(formatter.print(project.getSessionModificationTime(session)));
		
		buf.append(" Size:");
		buf.append(ByteSize.humanReadableByteCount(project.getSessionByteSize(session), true));
		
		return buf.toString();
	}
	
	public void watchTask(PhonTask task) {
		task.addTaskListener(taskListener);
	}
	
	public JLabel getStatusLabel() {
		return this.statusLabel;
	}
	
	public JLabel getSessionPathLabel() {
		return this.sessionPathLabel;
	}
	
	public JLabel getProgressLabel() {
		return this.progressLabel;
	}
	
	public JProgressBar getProgressBar() {
		return this.progressBar;
	}
}
