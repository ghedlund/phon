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
package ca.phon.ipadictionary.exceptions;

/**
 * Exception thrown when a requested capability is not
 * implemented in the dictionary object.
 * 
 */
public class CapabilityNotImplemented extends IPADictionaryException {
	
	/**
	 * Requested capability
	 */
	private Class<?> capability;

	public CapabilityNotImplemented(Class<?> cap) {
		super();
		this.capability = cap;
	}

	public CapabilityNotImplemented(Class<?> cap, String arg0, Throwable arg1) {
		super(arg0, arg1);
		this.capability = cap;
	}

	public CapabilityNotImplemented(Class<?> cap, String arg0) {
		super(arg0);
		this.capability = cap;
	}

	public CapabilityNotImplemented(Class<?> cap, Throwable arg0) {
		super(arg0);
		this.capability = cap;
	}
	
	

}
