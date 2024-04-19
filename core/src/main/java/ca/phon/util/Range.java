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
package ca.phon.util;

import java.io.Serializable;
import java.util.*;
import java.util.regex.*;


/**
 * An iterable integer range. 
 *
 */
public class Range implements Iterable<Integer>, Serializable {
	private int start;
	private int end;
	private boolean excludesEnd = false;
	int step = 1;
	
	public Range() {
		this(0, 0);
	}
	
	public Range(int start, int end) {
		this(start, end, false);
	}
	
	public Range(int start, int end, boolean excludesEnd) {
		super();
		
		this.start = start;
		this.end = end;
		this.excludesEnd = excludesEnd;
		
		if(this.end < this.start)
			step = -1;
		else
			step = 1;
	}

	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
		
		if(this.end < this.start)
			step = -1;
		else
			step = 1;
	}

	public int getEnd() {
		return end;
	}
	
	public int getRange() {
		return end-start;
	}

	public void setEnd(int end) {
		this.end = end;
		
		if(this.end < this.start)
			step = -1;
		else
			step = 1;
	}
	
	public boolean isExcludesEnd() {
		return excludesEnd;
	}
	
	public int getFirst() {
		return getStart();
	}
	
	public int getLast() {
		return (isExcludesEnd() ? getEnd()-step : getEnd());
	}

	public void setExcludesEnd(boolean excludesEnd) {
		this.excludesEnd = excludesEnd;
	}
	
	@Override
	public int hashCode() {
		return (new Integer(start)).hashCode() +
				(new Integer(end)).hashCode();
	}

	@Override
	public Iterator<Integer> iterator() {
		return new RangeIterator();
	}
	
	public boolean overlaps(Range anotherRange) {
		for(int val:anotherRange) {
			if(contains(val))
				return true;
		}
		return false;
	}
	
	public boolean contains(Range anotherRange) {
		if(anotherRange.getFirst() >= getFirst() &&
				anotherRange.getLast() <= getLast())
			return true;
		else
			return false;
	}
	
	public boolean contains(int val) {
		if(getStart() <= val) {
			if(isExcludesEnd()) {
				return val < getEnd();
			} else {
				return val <= getEnd();
			}
		}
		return false;
	}
	
	public Range intersect(Range anotherRange) {
		boolean started = false;
		boolean excludeEnd = false;
		Integer startIndex = null;
		Integer endIndex = null;
		
		for(int i:this) {
			if(anotherRange.contains(i)) {
				if(!started) {
					started = true;
					startIndex = i;
				}
			} else {
				if(started) {
					started = false;
					excludeEnd = true;
					endIndex = i;
					break;
				}
			}
		}
		
		if(started) {
			endIndex = this.getLast();
		
		if(startIndex != null && endIndex != null)
			return new Range(startIndex, endIndex, excludeEnd);
		}
		
		return null;
	
	}
	
	private class RangeIterator implements Iterator<Integer> {
		
		private int currentVal;
		
		public RangeIterator() {
			super();
			
			this.currentVal = getStart();
		}

		@Override
		public boolean hasNext() {
			if(isExcludesEnd()) {
				return !(this.currentVal == getEnd());
			} else {
				return !(this.currentVal == getEnd()+step);
			}
		}

		@Override
		public Integer next() {
			int retVal = this.currentVal;
			this.currentVal += step;
			return retVal;
		}

		@Override
		public void remove() {
			// noop
		}
		
	}
	
	public static void main(String[] args) {
		Range myRange = Range.fromString("(0..1)");
		for(int i:myRange) System.out.println("\t" + i);
		System.out.println(myRange.toString());
		myRange = Range.fromString("(5...10)");
		for(int i:myRange) System.out.println("\t" + i);
		System.out.println(myRange);
		myRange = Range.fromString("(-1...-10)");
		for(int i:myRange) {
			System.out.println("\t" + i);
		}
		System.out.println(myRange);
	}
	
	@Override
	public String toString() {
		String seperator = 
			(isExcludesEnd() ? "..." : "..");
		return "(" + getStart() + seperator + getEnd() + ")";
	}
	
	/**
	 * Parse a string into a range object.  Supported
	 * formats are the same as return values from toString()
	 * 
	 * @param rangeStr
	 * @return Range
	 */
	public static Range fromString(String rangeStr) {
		Pattern rangePattern = 
			Pattern.compile("\\(" +
					"(-?[0-9]+)" +
					"(\\.{2,3})" +
					"(-?[0-9]+)" +
					"\\)");
		Matcher m = rangePattern.matcher(rangeStr);
		
		if(m.matches()) {
			String startStr = m.group(1);
			String excludesStr = m.group(2);
			String endStr = m.group(3);
			
			int startVal = Integer.parseInt(startStr);
			boolean excludesEnd = excludesStr.length() == 3;
			int endVal = Integer.parseInt(endStr);
			
			return new Range(startVal, endVal, excludesEnd);
		} else {
			throw new IllegalArgumentException("Invalid range string: " + rangeStr);
		}
	}
	
	/**
	 * This method takes a list of ranges and attempts
	 * to reduce them.  If one range is completely contained
	 * within another only the larger range is kept.
	 * 
	 * @param ranges
	 * @return List<Range>
	 */
	public static List<Range> reduceRanges(List<Range> ranges) {
		List<Range> retVal = new ArrayList<Range>();
		
		for(int i = 0; i < ranges.size(); i++) {
			boolean keepRange = true;
			Range rangeToKeep = ranges.get(i);
			
			for(int j = 0; j < ranges.size(); j++) {
				if(j == i) continue;
				
				Range testRange = ranges.get(j);
				if(rangeToKeep.overlaps(testRange)) {
					if(testRange.contains(rangeToKeep)) {
						keepRange = false;
					}
				}
			}
			
			if(keepRange && rangeToKeep.getRange() != 0)
				retVal.add(rangeToKeep);
		}
		
		return retVal;
	}
}
