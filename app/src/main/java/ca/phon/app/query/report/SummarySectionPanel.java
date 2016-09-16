/*
 * Phon - An open source tool for research in phonology.
 * Copyright (C) 2005 - 2016, Gregory Hedlund <ghedlund@mun.ca> and Yvan Rose <yrose@mun.ca>
 * Dept of Linguistics, Memorial University <https://phon.ca>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ca.phon.app.query.report;

import ca.phon.query.report.io.SummarySection;

/**
 * Search summary
 */
public class SummarySectionPanel extends SectionPanel<SummarySection> {

	/**
	 * Help text
	 */
	private final static String INFO_TEXT = 
		"<html><body>" +
		"<i>Query Summary</i>" +
		"<p>Outputs session names and number of results for each result set.</p>" +
		"</body></html>";
	
	public SummarySectionPanel(SummarySection section) {
		super(section);
		
		init();
	}
	
	private void init() {
		super.setInformationText(getClass().getName()+".info", INFO_TEXT);
	}
	
}
