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
package ca.phon.ipadictionary;

import java.util.*;

import org.apache.logging.log4j.*;

import ca.phon.extensions.*;
import ca.phon.ipadictionary.exceptions.*;
import ca.phon.ipadictionary.spi.*;
import ca.phon.util.*;

/**
 * API for the IPA dictionary.  This class is final.
 * To implement custom IPADictionaries, implement the
 * {@link IPADictionarySPI} interface and pass the constructed
 * object as an argument to the constructor for this class.
 * 
 * Dictionaries are only required to provide the method {@link #lookup(String)}.
 * All other functionality is optional, but it is recommended
 * to at least implement the actions {@link AddEntry}, {@link RemoveEntry},
 * and {@link OrthoKeyIterator}.
 * 
 */
public final class IPADictionary implements IExtendable {
	
	private final static org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(IPADictionary.class.getName());

	/** 
	 * Implementing dictionary
	 */
	private final IPADictionarySPI impl;
	
	/**
	 * Extension support
	 */
	private final ExtensionSupport extSupport = new ExtensionSupport(IPADictionary.class, this);
	
	/**
	 * Constructor
	 * 
	 * @param dictionary implementation
	 */
	public IPADictionary(IPADictionarySPI impl) {
		if(impl == null)
			throw new IllegalArgumentException("null implementation");
		this.impl = impl;
		this.impl.install(this);
		extSupport.initExtensions();
	}
	
	/**
	 * Perform a lookup for the given orthographic transcription.
	 * 
	 * @param orthography
	 * @return the ipa transcriptions found or generated by this
	 *  dictionary
	 */
	public String[] lookup(String orthography) {
		String[] retVal = new String[0];
		try {
			retVal = impl.lookup(orthography);
		} catch (IPADictionaryExecption e) {
			LOGGER.error( e.getLocalizedMessage(), e);
		}
		
		// generate suggestions and add them to the end 
		// (if implementation supports this)
		// suggestions are usually implemented to handle
		// contractions in the target language
		GenerateSuggestions genSuggestions = 
			getExtension(GenerateSuggestions.class);
		if(genSuggestions != null) {
			String[] suggestions = 
				genSuggestions.generateSuggestions(orthography);
			if(suggestions.length > 0) {
				List<String> newList = new ArrayList<String>();
				newList.addAll(Arrays.asList(retVal));
				newList.addAll(Arrays.asList(suggestions));
				retVal = newList.toArray(new String[0]);
			}
		}
		
		return retVal;
	}
	
	/**
	 * Add an entry to this dictionary 
	 * (optional)
	 * 
	 * @param orthography
	 * @param ipa
	 * @throws IPADictionaryExecption if the dictionary is
	 *  not capable of adding entries or the entry was not
	 *  added because it was a duplicate.
	 */
	public void addEntry(String orthography, String ipa)
		throws IPADictionaryExecption {
		AddEntry cap = getExtension(AddEntry.class);
		if(cap == null) 
			throw new CapabilityNotImplemented(AddEntry.class);
		cap.addEntry(orthography, ipa);
	}
	
	/**
	 * Remove an entry from this dictionary.
	 * (optional)
	 * 
	 * @param orthography
	 * @param ipa
	 * @throws IPADictionaryException if the dictionary is
	 *  not capable of removing the entry.
	 */
	public void removeEntry(String orthography, String ipa) 
		throws IPADictionaryExecption {
		RemoveEntry cap = getExtension(RemoveEntry.class);
		if(cap == null)
			throw new CapabilityNotImplemented(RemoveEntry.class);
		cap.removeEntry(orthography, ipa);
	}
	
	/**
	 * Return an iterator for the keys in this dictionary.
	 * (optional)
	 * 
	 * @return the key iterator for the dictionary
	 * @throws IPADictionaryExecption if the dictionary is
	 *  not capable of iterating it's keys
	 */
	public Iterator<String> keyIterator() 
		throws IPADictionaryExecption {
		OrthoKeyIterator cap = getExtension(OrthoKeyIterator.class);
		if(cap == null)
			throw new CapabilityNotImplemented(OrthoKeyIterator.class);
		return cap.iterator();
	}
	
	/**
	 * Return all keys which start with the given prefix.
	 * (optional)
	 * 
	 * @param prefix
	 * @return the list of keys which have the given
	 *  prefix
	 * @throws IPADictionaryExecption if the dictionary does
	 *  not support this function
	 */
	public String[] prefixSearch(String prefix) 
		throws IPADictionaryExecption {
		PrefixSearch cap = getExtension(PrefixSearch.class);
		if(cap == null)
			throw new CapabilityNotImplemented(PrefixSearch.class);
		return cap.keysWithPrefix(prefix);
	}
	
	/**
	 * Returns the language handled by this dictionary.
	 * 
	 * @return the {@link LanguageEntry} for this
	 *  dictionary
	 */
	public Language getLanguage() {
		final LanguageInfo langInfo = getExtension(LanguageInfo.class);
		
		Language retVal = 
				(langInfo == null ? new Language() : langInfo.getLanguage());
		return retVal;
	}
	
	
	/**
	 * Returns a string identifier for this dictionary.
	 * While not required, the name should be unique
	 * to help users identify dictionaries which handle
	 * the same language.
	 * 
	 * @return the dictionary name
	 */
	public String getName() {
		NameInfo nameInfo = getExtension(NameInfo.class);
		String retVal = "";
		
		if(nameInfo != null) {
			retVal = nameInfo.getName();
		}
		
		return retVal;
	}
	
	/**
	 * Get value for a given metadata key.
	 * 
	 * @param key the metadata key.  Common keys are
	 *  'provider' and 'website'
	 * @return the value for the specified key or <code>null</code>
	 *  if no data is available. See {@link #metadataKeyIterator()}
	 */
	public String getMetadataValue(String key) {
		String retVal = null;
		Metadata metadataCap = getExtension(Metadata.class);
		if(metadataCap != null) {
			retVal = metadataCap.getMetadataValue(key);
		}
		return retVal;
	}
	
	/**
	 * Get the iteator for metadata keys.
	 * 
	 * @return an iterator for the metadata keys available
	 */
	public Iterator<String> metadataKeyIterator() {
		Iterator<String> retVal = (new ArrayList<String>()).iterator();
		Metadata metadataCap = getExtension(Metadata.class);
		if(metadataCap != null) {
			retVal = metadataCap.metadataKeyIterator();
		}
		return retVal;
	}
	
	@Override
	public Set<Class<?>> getExtensions() {
		return extSupport.getExtensions();
	}

	@Override
	public <T> T getExtension(Class<T> cap) {
		return extSupport.getExtension(cap);
	}

	@Override
	public <T> T putExtension(Class<T> cap, T impl) {
		return extSupport.putExtension(cap, impl);
	}

	@Override
	public <T> T removeExtension(Class<T> cap) {
		return extSupport.removeExtension(cap);
	}
 
}
