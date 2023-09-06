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

package ca.phon.session;

import ca.phon.extensions.ExtendableObject;
import ca.phon.session.spi.CommentSPI;
import ca.phon.session.tierdata.TierData;

/**
 * Entity class for comments.  See {@link CommentType} for more information
 * on types of comments specified by the CHAT transcription format.
 *
 */
public final class Comment extends ExtendableObject {
	
	private CommentSPI commentImpl;
	
	Comment(CommentSPI impl) {
		super();
		this.commentImpl = impl;
	}
	
	/** 
	 * Get the comment data.
	 * @return tier data for comment
	 */
	public TierData getValue() {
		return commentImpl.getValue();
	}
	
	/**
	 * Set the comment value.
	 * @param comment
	 */
	public void setValue(TierData comment) {
		commentImpl.setValue(comment);
	}

	/**
	 * Get comment type
	 *
	 * @return comment type
	 */
	public CommentType getType() { return commentImpl.getType(); }

	/**
	 * Set comment type
	 *
	 * @param type
	 */
	public void setType(CommentType type) { commentImpl.setType(type); }

}
