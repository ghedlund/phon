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
import ca.phon.session.spi.TranscriberSPI;

/**
 * A transcriber.
 *
 */
public final class Transcriber extends ExtendableObject {

	/**
	 * ID of validator (default transcriber)
	 */
	public final static String VALIDATOR_ID = "__validator__";
	/**
	 * Default transcriber object
	 */
	public final static Transcriber VALIDATOR;
	static {
		VALIDATOR = SessionFactory.newFactory().createTranscriber();
		VALIDATOR.setUsername(VALIDATOR_ID);
	}
	
	private final TranscriberSPI transcriberImpl;
	
	Transcriber(TranscriberSPI impl) {
		super();
		this.transcriberImpl = impl;
	}

	public String getUsername() {
		return transcriberImpl.getUsername();
	}

	public void setUsername(String username) {
		transcriberImpl.setUsername(username);
	}

	public String getRealName() {
		return transcriberImpl.getRealName();
	}

	public void setRealName(String name) {
		transcriberImpl.setRealName(name);
	}

	public boolean usePassword() {
		return transcriberImpl.usePassword();
	}

	public void setUsePassword(boolean v) {
		transcriberImpl.setUsePassword(v);
	}

	public String getPassword() {
		return transcriberImpl.getPassword();
	}

	public void setPassword(String password) {
		transcriberImpl.setPassword(password);
	}

	@Override
	public String toString() {
		String realName = getRealName();
		String username = getUsername();

		if(realName != null && realName.length() > 0) {
			return realName;
		} else {
			return username;
		}
	}

}
