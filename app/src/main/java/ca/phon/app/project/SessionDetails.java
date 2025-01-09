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
package ca.phon.app.project;

import ca.phon.app.log.LogUtil;
import ca.phon.formatter.FormatterUtil;
import ca.phon.project.*;
import ca.phon.project.ProjectEvent.ProjectEventProp;
import ca.phon.session.*;
import ca.phon.util.OSInfo;
import ca.phon.util.icons.*;
import ca.phon.worker.*;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;

public class SessionDetails extends JPanel {

	private final Project project;
	
	private String corpus;
	
	private String session;
	
	private JLabel fileLabel;
	private JLabel modifiedLabel;
	private JLabel recordsLabel;
	
	private JXTable speakerTable;
	private SpeakerTableModel speakerTableModel;
	
	private UpdateTask currentUpdateTask;
		
	public SessionDetails(Project project) {
		super();
		
		this.project = project;
		project.addProjectListener(projectListener);
		
		init();
	}
	
	private void init() {
		setLayout(new BorderLayout());
		
		fileLabel = new JLabel();
		fileLabel.addMouseListener(new MouseInputAdapter() {
			
			@Override
			public void mouseClicked(MouseEvent me) {
				if(OSInfo.isMacOs()) {
					final String sessionPath = project.getSessionPath(corpus, session);
					final StringBuffer buffer = new StringBuffer();
					buffer.append("tell application \"Finder\" to (activate) & ");
					buffer.append("(reveal \"").append(sessionPath).append("\" as POSIX file) & return");
					final String script = buffer.toString();		
					
					String[] args = { "osascript", "-e", script };
					try {
						Runtime.getRuntime().exec(args);
					} catch (IOException e) {
						LogUtil.severe(e);
					}
				} else if(Desktop.isDesktopSupported()) {
					try {
						Desktop.getDesktop().open(new File(project.getSessionPath(corpus, session)));
					} catch (IOException e) {
						Toolkit.getDefaultToolkit().beep();
						LogUtil.severe(e);
					}
				}
			}
			
		});
		fileLabel.setForeground(Color.blue);
		fileLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		
		modifiedLabel = new JLabel();
		
		recordsLabel = new JLabel();
		
		final JPanel detailsPanel = new JPanel(new GridBagLayout());
		detailsPanel.setOpaque(false);
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.WEST;
		detailsPanel.add(new JLabel("File:"), gbc);
		++gbc.gridx;
		gbc.weightx = 1.0;
		gbc.insets = new Insets(0, 5, 0, 0);
		detailsPanel.add(fileLabel, gbc);
		
		++gbc.gridy;
		gbc.gridx = 0;
		gbc.insets = new Insets(0, 0, 0, 0);
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0.0;
		detailsPanel.add(new JLabel("Modified:"), gbc);
		++gbc.gridx;
		gbc.weightx = 1.0;
		gbc.insets = new Insets(0, 5, 0, 0);
		detailsPanel.add(modifiedLabel, gbc);
		
		++gbc.gridy;
		gbc.gridx = 0;
		gbc.insets = new Insets(0, 0, 0, 0);
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0.0;
		detailsPanel.add(new JLabel("Records:"), gbc);
		++gbc.gridx;
		gbc.insets = new Insets(0, 5, 0, 0);
		gbc.weightx = 1.0;
		detailsPanel.add(recordsLabel, gbc);
		
		speakerTableModel = new SpeakerTableModel();
		speakerTable = new JXTable(speakerTableModel);
		speakerTable.setVisibleRowCount(4);
		final JScrollPane speakerScroller = new JScrollPane(speakerTable);
		speakerScroller.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
		
		add(detailsPanel, BorderLayout.NORTH);
		add(speakerScroller, BorderLayout.CENTER);
	}
	
	public String getCorpus() {
		return this.corpus;
	}
	
	public void setCorpus(String corpus) {
		this.corpus = corpus;
	}
	
	public String getSession() {
		return this.session;
	}
	
	public void setSession(String session) {
		this.session = session;
		update();
	}
	
	public void setSession(String corpus, String session) {
		setCorpus(corpus);
		setSession(session);
	}
	
	private void update() {
		if(this.corpus != null && this.session != null && project.getCorpusSessions(corpus).contains(session)) {
			final String sessionPath = project.getSessionPath(corpus, session);
			final File f = new File(sessionPath);
			final String name = f.getName();
			
			fileLabel.setText(name);
			fileLabel.setToolTipText(sessionPath);
			
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss (zzz)");
			final ZonedDateTime time = project.getSessionModificationTime(corpus, session);
			modifiedLabel.setText(formatter.format(time));
			
			final UpdateTask updateTask = new UpdateTask(corpus, session);
			if(currentUpdateTask != null)
				currentUpdateTask.shutdown();
			currentUpdateTask = updateTask;
			PhonWorker.getInstance().invokeLater(updateTask);
		} else {
			fileLabel.setText("");
			fileLabel.setToolTipText("");
			fileLabel.setIcon(null);
			
			modifiedLabel.setText("");
			recordsLabel.setText("");
			
			speakerTableModel.clear();
		}
	}
	
	private class UpdateTask extends PhonTask {

		private final String corpus;
		
		private final String session;
		
		public UpdateTask(String corpus, String session) {
			super();
			this.corpus = corpus;
			this.session = session;
		}
		
		@Override
		public void performTask() {
			super.setStatus(TaskStatus.RUNNING);
			
			SwingUtilities.invokeLater( () -> speakerTableModel.clear() );
			
			try {
				final int numRecords = project.numberOfRecordsInSession(corpus, session);
				if(!isShutdown())
					SwingUtilities.invokeLater( () -> recordsLabel.setText("" + numRecords) );
				else {
					super.setStatus(TaskStatus.TERMINATED);
					return;
				}
			} catch (IOException e) {
				super.err = e;
				super.setStatus(TaskStatus.ERROR);
				return;
			}
			
			final SessionPath sp = new SessionPath(corpus, session);
			final List<SessionPath> spList = Collections.singletonList(sp);
			final Set<Participant> speakerList = project.getParticipants(spList);
			if(!isShutdown())
				SwingUtilities.invokeLater( () -> speakerTableModel.setParticipants(speakerList) );
			else {
				super.setStatus(TaskStatus.TERMINATED);
				return;
			}
			
			super.setStatus(TaskStatus.FINISHED);
		}
		
	}
	
	private final ProjectListener projectListener = new ProjectListener() {
		
		@Override
		public void projectWriteLocksChanged(ProjectEvent pe) {
			final String corpus = pe.getProperty(ProjectEventProp.CORPUS);
			final String session = pe.getProperty(ProjectEventProp.SESSION);
			
			// update when unlocked
			if(!project.isSessionLocked(corpus, session)
					&& corpus.equals(getCorpus()) && session.equals(getSession()) ) {
				update();
			}
		}
		
		@Override
		public void projectStructureChanged(ProjectEvent pe) {
		}
		
		@Override
		public void projectDataChanged(ProjectEvent pe) {
		}
		
	};
	
	private class SpeakerTableModel extends AbstractTableModel {
		
		List<Participant> participants = new ArrayList<>();
		
		public void clear() {
			this.participants.clear();
			super.fireTableDataChanged();
		}
		
		public void setParticipants(Collection<Participant> participants) {
			this.participants.clear();
			this.participants.addAll(participants);
			
			super.fireTableDataChanged();
		}
		
		@Override
		public String getColumnName(int col) {
			if(col == 0) 
				return "Participant";
			else if(col == 1)
				return "Role";
			else if(col == 2)
				return "Age";
			else
				return super.getColumnName(col);
		}
		
		@Override
		public int getRowCount() {
			return participants.size();
		}

		@Override
		public int getColumnCount() {
			return 3;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			final Participant p = this.participants.get(rowIndex);
			
			if(columnIndex == 0) {
				return p.toString();
			} else if(columnIndex == 1) {
				return p.getRole().getTitle();
			} else if(columnIndex == 2) {
				final ParticipantHistory history = p.getExtension(ParticipantHistory.class);
				if(history != null) {
					return 
							FormatterUtil.format(history.getAgeForSession(new SessionPath(corpus, session)));
				} else return "";
			} else return "";
		}
		
	}
	
}
