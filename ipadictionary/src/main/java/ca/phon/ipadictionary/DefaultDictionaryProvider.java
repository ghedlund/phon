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

import ca.phon.ipadictionary.impl.*;
import ca.phon.plugin.PluginManager;
import ca.phon.util.Language;
import ca.phon.util.resources.ClassLoaderHandler;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class DefaultDictionaryProvider extends ClassLoaderHandler<IPADictionary> 
	implements DictionaryProvider {

	private final static String DICT_LIST = "dict/dicts.list";

	private TransliterationDictionaryProvider transDictProvider;

	public DefaultDictionaryProvider() {
		super(PluginManager.getInstance());
		loadResourceFile(DICT_LIST);
		transDictProvider = new CustomTransDictProvider();
	}

	@Override
	public IPADictionary loadFromURL(URL url) throws IOException {
		final IPADictionary immutableDict = new IPADictionary(new ImmutablePlainTextDictionary(url));
		final CompoundDictionary compoundDict =
				new CompoundDictionary(new IPADictionary[]{ immutableDict });
		return new IPADictionary(compoundDict);
	}

	@Override
	public Iterator<IPADictionary> iterator() {
		return new CustomIterator(super.iterator(), transDictProvider.iterator());
	}
	
	private class CustomIterator implements Iterator<IPADictionary> {
		
		private Iterator<IPADictionary> itr;

		private Iterator<IPADictionary> transDictItr;
		
		private Iterator<Language> langItr = null;

		private Iterator<?> currentItr;

		public CustomIterator(Iterator<IPADictionary> itr, Iterator<IPADictionary> transDictItr) {
			this.itr = itr;
			this.transDictItr = transDictItr;
			currentItr = itr;
		}

		@Override
		public boolean hasNext() {
			return (itr.hasNext()
					? true
					: (transDictItr.hasNext()
						? true
						: (langItr != null
							? langItr.hasNext()
							: false
						)
					)
				);
		}

		@Override
		public IPADictionary next() {
			IPADictionary retVal = null;

			if(currentItr == itr) {
				retVal = itr.next();
				if(!itr.hasNext())
					currentItr = transDictItr;
			} else if(currentItr == transDictItr) {
				retVal = transDictItr.next();
				if(!transDictItr.hasNext()) {
					currentItr = langItr;
				}
			} else if(currentItr == langItr) {
				Language lang = langItr.next();
				if(!langItr.hasNext())
					langItr = null;
			}
			
			return retVal;
		}

		@Override
		public void remove() {
		}
		
	}

	private class CustomTransDictProvider extends TransliterationDictionaryProvider {

		@Override
		public IPADictionary loadFromURL(URL url) throws IOException {
			IPADictionary transDict = super.loadFromURL(url);
			final CompoundDictionary compoundDict =
					new CompoundDictionary(new IPADictionary[]{  transDict });
			return new IPADictionary(compoundDict);
		}

	}
	
}
