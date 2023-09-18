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
package ca.phon.orthography;

/**
 * Word which have not been transcribed.
 *
 */
public enum UntranscribedType {
	@CHATReference("https://talkbank.org/manuals/CHAT.html#UnintelligibleSpeech_Code")
	UNINTELLIGIBLE("xxx", "unintelligible"),
	@CHATReference("https://talkbank.org/manuals/CHAT.html#PhonologicalCoding_Code")
	UNINTELLIGIBLE_WORD_WITH_PHO("yyy", "unintelligible-with-pho"),
	@CHATReference("https://talkbank.org/manuals/CHAT.html#UntranscribedMaterial_Code")
	UNTRANSCRIBED("www", "untranscribed");
	
	private String code;
	
	private String displayName;
	
	private UntranscribedType(String code, String displayName) {
		this.code = code;
		this.displayName = displayName;
	}

	public String getCode() {
		return this.code;
	}
	
	public String getDisplayName() {
		return this.displayName;
	}

	public static UntranscribedType fromString(String code) {
		UntranscribedType retVal = null;
		
		for(UntranscribedType v:values()) {
			if(v.getCode().equals(code) || v.getDisplayName().equals(code)) {
				retVal = v;
				break;
			}
		}
		
		return retVal;
	}

}
