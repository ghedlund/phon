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
package ca.phon.app.ipalookup;

import ca.phon.extensions.Extension;
import ca.phon.orthography.Word;
import ca.phon.util.Language;

import java.util.*;

/**
 * Extension for {@link Word} objects providing
 * a list of transcriptions for the given Orthography
 *
 */
@Extension(value= Word.class)
public class OrthoWordIPAOptions {
	
	private int selectedOption = 0;
	
	private final List<String> options = Collections.synchronizedList(new ArrayList<String>());
	
	private Language dictLang;
	

	public OrthoWordIPAOptions() {
		super();
	}
	
	public OrthoWordIPAOptions(String[] opts) {
		this(Arrays.asList(opts));
	}
	
	public OrthoWordIPAOptions(List<String> opts) {
		super();
		this.options.addAll(opts);
	}

	public Language getDictLang() {
		return dictLang;
	}
	
	public void setDictLang(Language dictLang) {
		this.dictLang = dictLang;
	}
	
	public List<String> getOptions() {
		return Collections.unmodifiableList(options);
	}

	public void setOptions(List<String> options) {
		this.options.clear();
		this.options.addAll(options);
	}
	
	public int getSelectedOption() {
		return this.selectedOption;
	}
	
	public void setSelectedOption(int option) {
		this.selectedOption = option;
	}
	
}
