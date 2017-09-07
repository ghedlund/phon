/*
 * Phon - An open source tool for research in phonology.
 * Copyright (C) 2005 - 2016, Gregory Hedlund <ghedlund@mun.ca> and Yvan Rose <yrose@mun.ca>
 * Dept of Linguistics, Memorial University <https://phon.ca>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ca.phon.session;

import java.util.*;

import ca.phon.extensions.*;
import ca.phon.visitor.*;

/**
 * Helper class providing iterator and visitor methods
 * for {@link Session} {@link Participant}s.
 */
public abstract class Participants implements Iterable<Participant>, IExtendable, Visitable<Participant> {

	protected Participants() {
		super();
		extSupport.initExtensions();
	}

	/**
	 * Extension support
	 */
	private final ExtensionSupport extSupport = new ExtensionSupport(Participants.class, this);

	@Override
	public Set<Class<?>> getExtensions() {
		return extSupport.getExtensions();
	}

	@Override
	public <T> T getExtension(Class<T> cap) {
		return extSupport.getExtension(cap);
	}

	@Override
	public <T> T putExtension(Class<T> cap, T impl) {
		return extSupport.putExtension(cap, impl);
	}

	@Override
	public <T> T removeExtension(Class<T> cap) {
		return extSupport.removeExtension(cap);
	}
	
	@Override
	public void accept(Visitor<Participant> visitor) {
		for(Participant p:this) {
			visitor.visit(p);
		}
	}
	
	public Map<ParticipantRole, Integer> getRoleCount() {
		final Map<ParticipantRole, Integer> retVal = new HashMap<ParticipantRole, Integer>();
		
		for(Participant p:this) {
			Integer rc = retVal.get(p.getRole());
			if(rc == null) {
				rc = 0;
			}
			rc++;
			retVal.put(p.getRole(), rc);
		}
		
		return retVal;
	}
	
	/**
	 * Returns a list of participants which does not include
	 * the given participant.
	 * 
	 * @param part
	 * @return
	 */
	public List<Participant> otherParticipants(Participant part) {
		List<Participant> retVal = new ArrayList<Participant>();
		for(Participant p:this) {
			if(p == part) continue;
			retVal.add(p);
		}
		return retVal;
	}
	
	public static void copyParticipantInfo(Participant src, Participant dest) {
		dest.setId(src.getId());
		dest.setBirthDate(src.getBirthDate());
		dest.setAge(src.getAge(null));
		dest.setEducation(src.getEducation());
		dest.setGroup(src.getGroup());
		dest.setLanguage(src.getLanguage());
		dest.setName(src.getName());
		dest.setRole(src.getRole());
		dest.setSES(src.getSES());
		dest.setSex(src.getSex());
	}
}
