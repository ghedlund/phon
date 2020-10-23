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
package ca.phon.audio;

public class InvalidHeaderException extends AudioIOException {

	private static final long serialVersionUID = 3349351994917897980L;

	public InvalidHeaderException() {
		super();
	}

	public InvalidHeaderException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public InvalidHeaderException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidHeaderException(String message) {
		super(message);
	}

	public InvalidHeaderException(Throwable cause) {
		super(cause);
	}

}
