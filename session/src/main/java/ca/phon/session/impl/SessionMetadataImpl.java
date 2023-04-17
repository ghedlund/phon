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
package ca.phon.session.impl;

import ca.phon.session.Comment;
import ca.phon.session.spi.SessionMetadataSPI;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class SessionMetadataImpl implements SessionMetadataSPI {
	
	SessionMetadataImpl() {
		super();
	}
	
	// TITLE
	private final AtomicReference<String> titleRef = new AtomicReference<String>();
	
	@Override
	public String getTitle() {
		return titleRef.get();
	}

	@Override
	public void setTitle(String title) {
		titleRef.getAndSet(title);
	}

	// CREATOR
	private final AtomicReference<String> creatorRef = new AtomicReference<String>();
	
	@Override
	public String getCreator() {
		return creatorRef.get();
	}

	@Override
	public void setCreator(String creator) {
		creatorRef.getAndSet(creator);
	}

	// APPID
	private final AtomicReference<String> appIdRef = new AtomicReference<String>();
	
	@Override
	public String getAppID() {
		return appIdRef.get();
	}

	@Override
	public void setAppID(String appID) {
		appIdRef.getAndSet(appID);
	}

	// SUBJECT
	private final AtomicReference<String> subjectRef = new AtomicReference<String>();
	
	@Override
	public String getSubject() {
		return subjectRef.get();
	}

	@Override
	public void setSubject(String subject) {
		subjectRef.getAndSet(subject);
	}

	// DESCRIPTION
	private final AtomicReference<String> descriptionRef = new AtomicReference<String>();
	
	@Override
	public String getDescription() {
		return descriptionRef.get();
	}

	@Override
	public void setDescription(String description) {
		descriptionRef.getAndSet(description);
	}

	// PUBLISHER
	private final AtomicReference<String> publisherRef = new AtomicReference<String>();
	
	@Override
	public String getPublisher() {
		return publisherRef.get();
	}

	@Override
	public void setPublisher(String publisher) {
		publisherRef.getAndSet(publisher);
	}

	// CONTRIBUTER
	private final AtomicReference<String> contributerRef = new AtomicReference<String>();
	
	@Override
	public String getContributor() {
		return contributerRef.get();
	}

	@Override
	public void setContributor(String contributer) {
		contributerRef.getAndSet(contributer);
	}

	// DATE (un-checked, user-formatted string)
	private final AtomicReference<String> dateRef = new AtomicReference<String>();
	
	@Override
	public String getDate() {
		return dateRef.get();
	}

	@Override
	public void setDate(String date) {
		dateRef.getAndSet(date);
	}

	// TYPE
	private final AtomicReference<String> typeRef = new AtomicReference<String>();
	
	@Override
	public String getType() {
		return typeRef.get();
	}

	@Override
	public void setType(String type) {
		typeRef.getAndSet(type);
	}
	
	// FORMAT
	private final AtomicReference<String> formatRef = new AtomicReference<String>();

	@Override
	public String getFormat() {
		return formatRef.get();
	}

	@Override
	public void setFormat(String format) {
		formatRef.getAndSet(format);
	}
	
	// IDENTIFIER
	private final AtomicReference<String> identifierRef = new AtomicReference<String>();

	@Override
	public String getIdentifier() {
		return identifierRef.get();
	}

	@Override
	public void setIdentifier(String identifier) {
		identifierRef.getAndSet(identifier);
	}
	
	// SOURCE
	private final AtomicReference<String> sourceRef = new AtomicReference<String>();

	@Override
	public String getSource() {
		return sourceRef.get();
	}

	@Override
	public void setSource(String source) {
		sourceRef.getAndSet(source);
	}
	
	// LANGUAGE
	private final AtomicReference<String> languageRef = new AtomicReference<String>();

	@Override
	public String getLanguage() {
		return languageRef.get();
	}

	@Override
	public void setLanguage(String language) {
		languageRef.getAndSet(language);
	}
	
	// RELATION
	private final AtomicReference<String> relationRef = new AtomicReference<String>();

	@Override
	public String getRelation() {
		return relationRef.get();
	}

	@Override
	public void setRelation(String relation) {
		relationRef.getAndSet(relation);
	}
	
	// COVERAGE
	private final AtomicReference<String> coverageRef = new AtomicReference<String>();

	@Override
	public String getCoverage() {
		return coverageRef.get();
	}

	@Override
	public void setCoverage(String coverage) {
		coverageRef.getAndSet(coverage);
	}
	
	// RIGHTS
	private final AtomicReference<String> rightsRef = new AtomicReference<String>();

	@Override
	public String getRights() {
		return rightsRef.get();
	}

	@Override
	public void setRights(String rights) {
		rightsRef.getAndSet(rights);
	}

	// COMMENTS
	private final List<Comment> comments = 
			Collections.synchronizedList(new ArrayList<Comment>());
	
	@Override
	public int getNumberOfComments() {
		return comments.size();
	}

	@Override
	public Comment getComment(int idx) {
		return comments.get(idx);
	}

	@Override
	public void addComment(Comment comment) {
		comments.add(comment);
	}

	@Override
	public void removeComment(Comment comment) {
		comments.remove(comment);
	}

	@Override
	public void removeComment(int idx) {
		comments.remove(idx);
	}

	private final List<Comment> trailingComments =
			Collections.synchronizedList(new ArrayList<Comment>());

	public int getNumberOfTrailingComments() {
		return trailingComments.size();
	}

	/**
	 * Return comment at end of session at given index
	 *
	 * @param idx
	 * @return
	 */
	public Comment getTrailingComment(int idx) {
		return trailingComments.get(idx);
	}

	/**
	 * Add comment to end of session
	 * @param comment
	 */
	public void addTrailingComment(Comment comment) {
		trailingComments.add(comment);
	}

	/**
	 * Remove comment from end of session
	 *
	 * @param comment
	 */
	public void removeTrailingComment(Comment comment) {
		trailingComments.remove(comment);
	}

	/**
	 * Remove comment from beginning of end at given index
	 * @param idx
	 */
	public void removeTrailingComment(int idx) {
		trailingComments.remove(idx);
	}

}
