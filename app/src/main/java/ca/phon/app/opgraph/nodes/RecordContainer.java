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
package ca.phon.app.opgraph.nodes;

import ca.phon.app.log.LogUtil;
import ca.phon.app.opgraph.nodes.query.ResultSetRecordContainer;
import ca.phon.project.Project;
import ca.phon.query.db.ResultSet;
import ca.phon.session.*;

import java.io.IOException;
import java.util.*;

public interface RecordContainer {
	
	public static List<RecordContainer> toRecordContainers(Project project, Collection<Participant> selectedParticipants, Object obj) {
		List<RecordContainer> retVal = new ArrayList<>();
		if(obj instanceof SessionPath) {
			SessionPath sessionLoc = (SessionPath)obj;
			try {
				Session session = project.openSession(sessionLoc.getFolder(), sessionLoc.getSessionFile());
				retVal.add(new SessionRecordContainer(session, selectedParticipants));
			} catch (IOException e) {
				LogUtil.warning(e);
			}
		} else if(obj instanceof SessionPath[]) {
			SessionPath[] paths = (SessionPath[])obj;
			for(SessionPath path:paths) retVal.addAll(toRecordContainers(project, selectedParticipants, path));
		} else if(obj instanceof Session) {
			retVal.add(new SessionRecordContainer((Session)obj, selectedParticipants));
		} else if(obj instanceof Session[]) {
			Session[] sessions = (Session[])obj;
			for(Session session:sessions) retVal.add(new SessionRecordContainer(session, selectedParticipants));
		} else if(obj instanceof ResultSet) {
			ResultSet rs = (ResultSet)obj;
			try {
				Session session = project.openSession(rs.getCorpus(), rs.getSession());
				retVal.add(new ResultSetRecordContainer(session, rs));
			} catch (IOException e) {
				LogUtil.warning(e);
			}
		} else if(obj instanceof ResultSet[]) {
			ResultSet[] resultSets = (ResultSet[])obj;
			for(ResultSet resultSet:resultSets) retVal.addAll(toRecordContainers(project, selectedParticipants, resultSet));
		} else if(obj instanceof Collection) {
			Collection<?> collection = (Collection<?>)obj;
			for(Object o:collection) retVal.addAll(toRecordContainers(project, selectedParticipants, o));
		}
		return retVal;
	}

	public Session getSession();
	
	public Iterator<Integer> idxIterator();
	
}
