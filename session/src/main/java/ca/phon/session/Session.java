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

import java.time.LocalDate;
import java.util.List;

import ca.phon.extensions.IExtendable;

/**
 * A session in a project.
 *
 */
public interface Session extends IExtendable {

	/** Get the corpus */
	public String getCorpus();
	
	/** Get the transcript name */
	public String getName();

	/** Get the transcript date */
	public LocalDate getDate();
	
	/** Get the language */
	public String getLanguage();
	
	
	/** Get/Set the media file location */
	public String getMediaLocation();
	
	
	/** Get/Set the tier view */
	public List<TierViewItem> getTierView();
	
	/*
	 * Custom tiers defined for the session
	 */
	/**
	 * Number of user-defined tiers for this session
	 */
	public int getUserTierCount();
	
	/**
	 * Get user tier for the specified index.
	 * 
	 * @param idx
	 * 
	 * @return tier description 
	 */
	public TierDescription getUserTier(int idx);
	
	/**
	 * Remove user tier 
	 * 
	 * @param idx
	 */
	public TierDescription removeUserTier(int idx);
	
	public TierDescription removeUserTier(TierDescription tierDescription);
	
	/**
	 * Add a user tier
	 */
	public void addUserTier(TierDescription tierDescription);
	public void addUserTier(int idx, TierDescription tierDescription);
	
	public TierDescriptions getUserTiers();
	
	/**
	 * Get the number of transcribers
	 */
	public int getTranscriberCount();
	
	/**
	 * Get transcriber for the specified username
	 * @param username
	 * @return
	 */
	public Transcriber getTranscriber(String username);
	
	/**
	 * Get the <code>i</code>th transcriber.
	 * @param i
	 * @return
	 */
	public Transcriber getTranscriber(int i);
	
	/**
	 * Remove the <code>i</code>th transcriber
	 * 
	 * @param i
	 */
	public void removeTranscriber(int i);
	
	public Transcribers getTranscribers();
	
	/**
	 * Get the metadata
	 * 
	 * @return Metadata
	 */
	public SessionMetadata getMetadata();
	
	/**
	 * Return the record at the given index.
	 * 
	 * @param pos
	 * @return the specified record
	 */
	public Record getRecord(int pos);
	
	/**
	 * Return the number of records.
	 * 
	 * @return the number of records
	 */
	public int getRecordCount();
	
	public Records getRecords();
	
	/**
	 * Get the position of the given record.
	 * 
	 * @param record
	 */
	public int getRecordPosition(Record record);
	
	/**
	 * Set the position of the given record
	 * 
	 * @param record
	 * @param position
	 */
	public void setRecordPosition(Record record, int position);
	
	/**
	 * Get the number of participants
	 * 
	 * @return the number of participants
	 */
	public int getParticipantCount();

	/**
	 * Add a new participant
	 * 
	 * @param participant
	 */
	public void addParticipant(Participant participant);
	
	/**
	 * Get the participant at the given index
	 * 
	 * @param idx
	 * @return the specified participant
	 */
	public Participant getParticipant(int idx);
	
	/**
	 * Iterable/visitable participant informamtion.
	 * 
	 * @return Participants
	 */
	public Participants getParticipants();
	
	/** Set the corpus */
	public void setCorpus(String corpus);
	
	/** Set the transcript name */
	public void setName(String name);
	
	/** Get the transcript date */
	public void setDate(LocalDate date);
	
	/** Set the language */
	public void setLanguage(String language);
	
	/** Media location */
	public void setMediaLocation(String mediaLocation);
	
	/** Tier view */
	public void setTierView(List<TierViewItem> view);
	
	/**
	 * Add a new transcriber
	 */
	public void addTranscriber(Transcriber t);
	
	/**
	 * Remove a transcriber
	 */
	public void removeTranscriber(Transcriber t);
	public void removeTranscriber(String username);
	
	/**
	 * Add a new record to the session
	 * 
	 * @param record
	 */
	public void addRecord(Record record);
	
	/**
	 * Add a new record to the list in the given position.
	 * 
	 * @param record
	 * @param idx
	 */
	public void addRecord(int pos, Record record);
	
	/**
	 * Remove a record from the session.
	 * 
	 * @param record
	 */
	public void removeRecord(Record record);
	
	/**
	 * Remove a record from the session
	 * 
	 * @param pos
	 */
	public void removeRecord(int pos);
	
	/**
	 * Remove a participant.
	 * 
	 * @param participant
	 */
	public void removeParticipant(Participant participant);
	
	/**
	 * Remove a participant
	 * 
	 * @param idx
	 */
	public void removeParticipant(int idx);
	
}
