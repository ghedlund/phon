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
package ca.phon.phonex;

public class PhonexPluginException extends PhonexPatternException {

	private static final long serialVersionUID = -6852419990439710113L;

	public PhonexPluginException(int line, int charInLine, String message, Throwable cause) {
		super(line, charInLine, message, cause);
	}

	public PhonexPluginException(int line, int charInLine, String message) {
		super(line, charInLine, message);
	}

	public PhonexPluginException(int line, int charInLine, Throwable cause) {
		super(line, charInLine, cause);
	}

	public PhonexPluginException(int line, int charInLine) {
		super(line, charInLine);
	}
	
}
