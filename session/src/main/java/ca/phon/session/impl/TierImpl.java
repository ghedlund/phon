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
package ca.phon.session.impl;

import ca.phon.formatter.Formatter;
import ca.phon.formatter.FormatterFactory;
import ca.phon.session.alignment.TierAlignmentRules;
import ca.phon.session.spi.TierSPI;

import java.text.ParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class TierImpl<T> implements TierSPI<T> {
	
	/**
	 * Declared type
	 */
	private final Class<T> declaredType;
	
	/**
	 * name
	 */
	private final String tierName;

	private final Map<String, String> tierParameters;

	private final TierAlignmentRules tierAlignmentRules;
	
	/**
	 * Group data
	 */
	private final AtomicReference<T> valueRef = new AtomicReference<>();

	/**
	 * Constructor
	 * 
	 * @param name
	 * @param type
	 * @param grouped
	 */
	TierImpl(String name, Class<T> type, Map<String, String> tierParameters, TierAlignmentRules tierAlignmentRules) {
		super();
		this.tierName = name;
		this.declaredType = type;
		this.tierParameters = tierParameters;
		this.tierAlignmentRules = tierAlignmentRules;
	}

	@Override
	public String getName() {
		return tierName;
	}

	@Override
	public Class<T> getDeclaredType() {
		return declaredType;
	}

	@Override
	public Map<String, String> getTierParameters() {
		return Collections.unmodifiableMap(this.tierParameters);
	}

	@Override
	public TierAlignmentRules getTierAlignmentRules() {
		return this.tierAlignmentRules;
	}

	@Override
	public T parse(String text) throws ParseException {
		final Formatter<T> factory = FormatterFactory.createFormatter(getDeclaredType());
		if(factory != null) {
			return factory.parse(text);
		} else {
			throw new ParseException("No parser available for type " + getDeclaredType(), 0);
		}
	}

	@Override
	public T getValue() {
		return this.valueRef.get();
	}

	@Override
	public void setValue(T value) {
		this.valueRef.set(value);
	}
}
