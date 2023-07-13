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
package ca.phon.formatter;

import java.text.*;


/**
 * Format time values in milliseconds into parseable strings.
 *
 * @deprecated Use MediaTimeFormat instead
 */
@Deprecated
public class MsFormatter implements Formatter<Long> {
	
	/**
	 * Create a new formatter object.
	 * @return formatter
	 */
	public static Format createFormatter() {
		return new MsFormat();
	}
	
	/**
	 * Convert a value in milliseconds into a string.
	 * 
	 * @param ms
	 * @return ms as a readable string
	 */
	public static String msToDisplayString(long ms) {
		final Format format = createFormatter();
		return format.format(new Long(ms));
	}
	
	/**
	 * Parse the given string into a value in milliseconds.
	 * 
	 * @param msText
	 * @return time in milliseconds
	 * 
	 * @throws ParseException
	 */
	public static long displayStringToMs(String msText)
		throws ParseException {
		final Format format = createFormatter();
		return ((Long)format.parseObject(msText)).longValue();
	}

	@Override
	public String format(Long obj) {
		return msToDisplayString(obj.longValue());
	}

	@Override
	public Long parse(String text) throws ParseException {
		return displayStringToMs(text);
	}

}
