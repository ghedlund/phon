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
import java.util.regex.*;


/**
 * Text formatter for milliseconds.
 *
 * @deprecated Use MediaTimeFormat instead
 */
@Deprecated
public class MsFormat extends Format {

	private static final long serialVersionUID = 4432179935812692306L;
	
	private final static String PATTERN = "(?:([0-9]{1,3})\\:)?([0-9]{1,2})\\.([0-9]{1,3})";

	@Override
	public StringBuffer format(Object obj, StringBuffer toAppendTo,
			FieldPosition pos) {
		Long toParse = null;
		
		if(obj instanceof Integer) toParse = ((Integer)obj).longValue();
		if(obj instanceof Long) toParse = (Long)obj;
		
		StringBuffer retVal = new StringBuffer();
		retVal.append(this.msToDisplayString(toParse));
		
		return retVal;
	}

	protected String msToDisplayString(long ms) 
		throws IllegalArgumentException {
		if(ms < 0)
			throw new IllegalArgumentException("Time cannot be negative.");
		
		long numSeconds = ms / 1000;
		long numMSecondsLeft = ms % 1000;
		
		long numMinutes = numSeconds / 60;
		long numSecondsLeft = numSeconds % 60;
		
		NumberFormat nf = NumberFormat.getIntegerInstance();
		nf.setMinimumIntegerDigits(2);
		
		NumberFormat msNf = NumberFormat.getIntegerInstance();
		msNf.setMinimumIntegerDigits(3);
		
		String minuteString = msNf.format(numMinutes) + ":";
		
		String secondString =
			(numMinutes == 0 
					? (nf.format(numSeconds) + ".")
					: (nf.format(numSecondsLeft) + ".")
			);
		
		String msString = 
			(msNf.format(numMSecondsLeft));
		
		String timeString = 
			minuteString + secondString + msString;
		
		return timeString;
	}
	
	@Override
	public Object parseObject(String source, ParsePosition pos) {
		Object retVal = null;
		boolean negative = source.startsWith("-");
		if(negative)
			source = source.substring(1);
		long value = 0L;
		if(source.matches("[0-9]+")) {
			value = Long.parseLong(source);
			pos.setIndex(source.length());
		} else {
			final Pattern pattern = Pattern.compile(PATTERN);
			final Matcher matcher = pattern.matcher(source);
			if (matcher.matches()) {
				final String minString = matcher.group(1);
				int mins = 0;
				if(minString != null)
					mins = Integer.parseInt(minString);

				final String secString = matcher.group(2);
				final int secs = Integer.parseInt(secString);

				final String msString = matcher.group(3);
				final int ms = Integer.parseInt(msString);

				value = new Long(
						ms + (secs * 1000) + (mins * 60 * 1000));
				pos.setIndex(source.length());
			}
		}
		return (negative ? -1 : 1) * value;
	}

}
