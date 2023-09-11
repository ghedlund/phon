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
package ca.phon.project;

import ca.phon.session.Record;
import ca.phon.session.*;

import java.io.IOException;
import java.time.*;
import java.util.*;

/**
 * Keep track of participants and what sessions in which they take part.
 * This object is usually used as an extension on {@link Project}s.
 *
 */
public class ParticipantCache {

	private final static org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger(ParticipantCache.class.getName());

	private Project project;

	private final Set<Participant> participantSet;

	private final Map<SessionPath, ZonedDateTime> sessionMap;

	final Comparator<Participant> participantComparator = (p1, p2) -> {
		int retVal = p1.getId().compareTo(p2.getId());
		if(retVal == 0) {
			final String p1Name = (p1.getName() == null ? "" : p1.getName());
			final String p2Name = (p2.getName() == null ? "" : p2.getName());
			retVal = p1Name.compareTo(p2Name);
			if(retVal == 0) {
				retVal = p1.getRole().compareTo(p2.getRole());
			}
		}
		return retVal;
	};

	private void resetCache() {
		participantSet.clear();
	}

	public ParticipantCache(Project project) {
		super();
		this.project = project;
		this.participantSet = Collections.synchronizedSet(new TreeSet<>(participantComparator));
		this.sessionMap = Collections.synchronizedMap(new HashMap<>());

		this.project.addProjectListener(new ProjectListener() {
			@Override
			public void projectStructureChanged(ProjectEvent pe) {
				resetCache();
			}

			@Override
			public void projectDataChanged(ProjectEvent pe) {

			}

			@Override
			public void projectWriteLocksChanged(ProjectEvent pe) {
				if(pe.getEventType() == ProjectEvent.ProjectEventType.SESSION_CHANGED)
					resetCache();
			}
		});
	}

	public void setProject(Project project) {
		this.project = project;
	}

	public void loadSession(String corpus, String session) {
		loadSession(new SessionPath(corpus, session));
	}

	public void loadSession(SessionPath sessionPath) {
		final ZonedDateTime lastScanModTime = sessionMap.get(sessionPath);
		if(lastScanModTime != null) {
			final ZonedDateTime currentModTime = project.getSessionModificationTime(sessionPath.getFolder(), sessionPath.getSessionFile());
			if(currentModTime.isEqual(lastScanModTime) || currentModTime.isBefore(lastScanModTime))
				return;
		}
		try {
			Session session = project.openSession(sessionPath.getFolder(), sessionPath.getSessionFile());
			Collection<Participant> participants = new ArrayList<>();

			participants.add( SessionFactory.newFactory().cloneParticipant(Participant.UNKNOWN) );
			session.getParticipants().forEach( (p) -> participants.add(p) );

			synchronized (participantSet) {
				for(Participant participant:participants) {
					Participant speaker = null;
					if(participantSet.contains(participant)) {
						for(Participant p:participantSet) {
							if(participantComparator.compare(participant, p) == 0) {
								speaker = p;
								break;
							}
						}
					} else {
						speaker = SessionFactory.newFactory().cloneParticipant(participant);
					}

					// get record count
					int count = 0;
					for(Record r:session.getRecords()) {
						if(participantComparator.compare(r.getSpeaker(), participant) == 0) ++count;
					}

					if(speaker != null) {
						if(count == 0 && participantComparator.compare(Participant.UNKNOWN, speaker) == 0) {
							// do not add unknown speaker if there are no records
						} else {
							ParticipantHistory history = speaker.getExtension(ParticipantHistory.class);
							if(history == null) {
								history = new ParticipantHistory();
								speaker.putExtension(ParticipantHistory.class, history);
							}
							Period age =
									(participant != null ? participant.getAge(session.getDate()) : null);
							history.setAgeForSession(sessionPath, age);
							history.setNumberOfRecordsForSession(sessionPath, count);
						}
					}

					if(!participantSet.contains(speaker)) {
						if(participantComparator.compare(Participant.UNKNOWN, speaker) == 0) {
							if(count > 0)
								participantSet.add(speaker);
						} else {
							participantSet.add(speaker);
						}
					}

				}
			}

			sessionMap.put(sessionPath, project.getSessionModificationTime(session));
		} catch (IOException e) {
			LOGGER.warn( e.getLocalizedMessage(), e);
		}
	}

	public Set<Participant> getParticipants(Collection<SessionPath> sessionPaths) {
		final TreeSet<Participant> retVal = new TreeSet<>(participantComparator);

		synchronized(participantSet) {
			for(Participant speaker:participantSet) {
				final ParticipantHistory history = speaker.getExtension(ParticipantHistory.class);
				if(history != null) {
					if(!Collections.disjoint(history.getSessions(), sessionPaths))
						retVal.add(speaker);
				}
			}
		}

		return retVal;
	}

}
