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
package ca.phon.session.spi;

import ca.phon.session.CommentType;
import ca.phon.session.tierdata.TierData;

public interface CommentSPI {
	
	/** 
	 * Get the comment string.
	 * @return String
	 */
	public TierData getValue();
	
	/**
	 * Set the comment string.
	 * @param comment
	 */
	public void setValue(TierData comment);
	
	public CommentType getType();

	public void setType(CommentType type);

}
