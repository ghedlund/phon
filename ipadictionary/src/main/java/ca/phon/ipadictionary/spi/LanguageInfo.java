/*
 * Copyright (C) 2012-2018 Gregory Hedlund & Yvan Rose
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
package ca.phon.ipadictionary.spi;

import ca.phon.extensions.*;
import ca.phon.ipadictionary.*;
import ca.phon.util.*;

/**
 * Capability for returning langauge information
 * on a dictionary.
 *
 */
@Extension(IPADictionary.class)
public interface LanguageInfo {

	/**
	 * Returns the language handled by this dictionary.
	 * 
	 * @return the {@link Language} for this
	 *  dictionary
	 */
	public Language getLanguage();
	
}
