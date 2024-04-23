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
package ca.phon.query.report.csv;

import ca.phon.csv.CSVQuoteType;
import ca.phon.csv.CSVWriter;
import ca.phon.project.Project;
import ca.phon.query.db.*;
import ca.phon.query.report.*;
import ca.phon.query.report.datasource.*;
import ca.phon.query.report.io.Group;
import ca.phon.query.report.io.*;
import ca.phon.session.*;
import ca.phon.session.format.*;
import ca.phon.util.OSInfo;
import jakarta.xml.bind.JAXBElement;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CSV report builder implementation.
 *
 */
public class CSVReportBuilder extends ReportBuilder {
	
	/**
	 * Property for indenting content at sections (default:false)
	 * 
	 */
	public final static String INDENT_CONTENT = "indent_content";
	
	/**
	 * Property for printing section names (default:true)
	 */
	public final static String PRINT_SECTION_NAMES = "print_section_names";

	@Override
	public String[] getPropertyNames() {
		return new String[] { PRINT_SECTION_NAMES, INDENT_CONTENT };
	}

	@Override
	public Class<?> getPropertyClass(String propName) {
		if(INDENT_CONTENT.equals(propName) || PRINT_SECTION_NAMES.equals(propName)) {
			return Boolean.class;
		} else {
			return super.getPropertyClass(propName);
		}
	}

	@Override
	public String getPropertyMessage(String propName) {
		String retVal = null;
		
		if(propName.equals(PRINT_SECTION_NAMES)) {
			retVal = "Print report element titles";
		} else if(propName.equals(INDENT_CONTENT)) {
			retVal = "Indent content";
		}
		
		return (retVal == null ? super.getPropertyMessage(propName) : retVal);
	}

	@Override
	public Object getPropertyDefault(String propName) {
		Object retVal = null;
		
		if(propName.equals(PRINT_SECTION_NAMES)) {
			retVal = Boolean.TRUE;
		} else if(propName.equals(INDENT_CONTENT)) {
			retVal = Boolean.FALSE;
		}
		
		return (retVal == null ? super.getPropertyDefault(propName) : retVal);
	}
	
	
	/*
	 * CSV report properties
	 */
	/**
	 * CSV separator char
	 */
	public static final String CSV_SEP_CHAR = "_sep_char_";
	
	/**
	 * CSV quote char
	 */
	public static final String CSV_QUOTE_CHAR = "_quote_char_";
	
	/**
	 * CSV line term
	 */
	public static final String CSV_LINE_TERM = "_line_term_";
	
	/**
	 * File Encoding
	 */
	public static final String FILE_ENCODING = "_file_encoding_";
	
	/**
	 * CSV File writer
	 */
	private CSVWriter writer;

	public CSVReportBuilder() {
		// put default props
		putProperty(CSV_SEP_CHAR, ',');
		putProperty(CSV_QUOTE_CHAR, '\"');
		putProperty(CSV_LINE_TERM,
				(OSInfo.isWindows() ? "\r\n" : "\n"));
		putProperty(FILE_ENCODING, "UTF-8");
	}
	
	/**
	 * TODO Datasources should be given as indirect dependencies since
	 * we can re-use them for other report builders.  Perhaps a global
	 * object can be created to keep track of created datasources so
	 * they can still be lazilly generated.
	 */
	@Override
	public void buildReport(ReportDesign design, Project project, 
			Query query, ResultSet[] resultSets, OutputStream stream) throws ReportBuilderException {
		
		char sep = (Character)getProperty(CSV_SEP_CHAR);
		CSVQuoteType quote = (Character)getProperty(CSV_QUOTE_CHAR) == '"' ? CSVQuoteType.DOUBLE_QUOTE : CSVQuoteType.SINGLE_QUOTE;
		String lineTerm = (String)getProperty(CSV_LINE_TERM);
		
		try {
			OutputStreamWriter fWriter = 
				new OutputStreamWriter(stream, getProperty(FILE_ENCODING).toString());
			
			writer = new CSVWriter(fWriter, sep, quote, true, lineTerm.length() != 2, false);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			throw new ReportBuilderException(e);
		}
		
		for(JAXBElement<? extends Section> sectionEle:design.getReportSection()) {
			// throw an exception if the build was cancelled
			if(isBuildCanceled()) {
				throw new ReportBuilderException("Canceled by user");
			}
			Section section = sectionEle.getValue();
			
			if(isPrintSectionNames()) {
				try {
					// output newline then a single line with the section name
					writer.writeNext(new String[0]);
					writer.writeNext(new String[]{section.getName()});
				} catch (IOException e) {
					throw new ReportBuilderException(e);
				}
			}
			
			CSVSectionWriter sectionWriter = null;
			
			if(section instanceof AggregrateInventory) {
				InventoryDataSource invDs = 
					new InventoryDataSource(resultSets, (AggregrateInventory)section);
				sectionWriter = new CSVTableDataSourceWriter(this, invDs);
			} else if (section instanceof ParamSection) {
				ParamSection pSec = (ParamSection)section;
				sectionWriter = new CSVTableDataSourceWriter(this, new ParamDataSource(query, pSec));
		    } else if (section instanceof SummarySection) {
		    	SummarySection summarySection = (SummarySection)section;
		    	sectionWriter = new CSVTableDataSourceWriter(this, new SummaryDataSource(resultSets, summarySection));
			} else if (section instanceof CommentSection) {
				CommentSection commentSection = (CommentSection)section;
				sectionWriter = new CSVCommentWriter(commentSection);
			} else if (section instanceof Group) {
				Group group = (Group)section;
				
				for(ResultSet resultSet:resultSets) {
					if(isBuildCanceled()) {
						throw new ReportBuilderException("Canceled by user");
					}
					// write session info header if requested
					if(group.isPrintSessionHeader()) {
						printSessionHeader(group, project, resultSet);
					}
					
					for(JAXBElement<? extends Section> groupSectionEle:group.getGroupReportSection()) {
						if(isBuildCanceled()) {
							throw new ReportBuilderException("Canceled by user");
						}
						
						Section groupSection = groupSectionEle.getValue();
						
						if(isPrintSectionNames()) {
							try {
								// output newline then a single line with the section name
								writer.writeNext(new String[0]);
								List<String> groupTitleLine = new ArrayList<String>();
								for (int i = 0; i < getIndentLevel(); i++) groupTitleLine.add("");
								groupTitleLine.add(groupSection.getName());
								writer.writeNext(groupTitleLine.toArray(new String[0]));

								if (isIndentContent()) nextIndentLevel();
							} catch (IOException e) {
								throw new ReportBuilderException(e);
							}
						}
						
						if(groupSection instanceof ResultListing) {
							ResultListing tblInv = (ResultListing)groupSection;
							ResultListingDataSource tblInvDs = 
								new ResultListingDataSource(project, resultSet, tblInv);
							
							CSVSectionWriter groupSectionWriter = new CSVTableDataSourceWriter(this, tblInvDs);
							groupSectionWriter.writeSection(writer, getIndentLevel());
							try {
								writer.writeNext(new String[0]);
							} catch (IOException e) {
								throw new ReportBuilderException(e);
							}
						} else if (groupSection instanceof CommentSection) {
							CommentSection commentSection = (CommentSection)groupSection;
							CSVCommentWriter commentWriter = new CSVCommentWriter(commentSection);
							commentWriter.writeSection(writer, getIndentLevel());
							try {
								writer.writeNext(new String[0]);
							} catch (IOException e) {
								throw new ReportBuilderException(e);
							}
						} else if(groupSection instanceof InventorySection) {
							InventorySection invSection = (InventorySection)groupSection;
							
							InventoryDataSource invDs = new InventoryDataSource(new ResultSet[]{resultSet}, invSection);
							CSVTableDataSourceWriter dsWriter = new CSVTableDataSourceWriter(this, invDs);
							dsWriter.writeSection(writer, getIndentLevel());
							try {
								writer.writeNext(new String[0]);
							} catch (IOException e) {
								throw new ReportBuilderException(e);
							}
						}
						
						if(isPrintSectionNames() && 
								isIndentContent()) prevIndentLevel();
					}
				}
				
			}
			
			if(sectionWriter != null) {
				sectionWriter.writeSection(writer, getIndentLevel());
				try {
					writer.writeNext(new String[0]);
				} catch (IOException e) {
					throw new ReportBuilderException(e);
				}

				try {
					writer.flush();
				} catch (IOException e) {
					Logger.getLogger(getClass().getName()).log(Level.WARNING, e.getLocalizedMessage(), e);
				}
			}
		}
		
		// flush and close writer
		try {   
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new ReportBuilderException(e);
		}
		
	}
	
	private void printSessionHeader(Group group, Project project, ResultSet resultSet) {
		
		String sessionPath = resultSet.getSessionPath();
		
		List<String> sessionNameLine = new ArrayList<String>();
		for(int i = 0; i < getIndentLevel(); i++) sessionNameLine.add("");
		sessionNameLine.add("Session:");
		sessionNameLine.add(resultSet.getSessionPath());
		try {
			writer.writeNext(sessionNameLine.toArray(new String[0]));
		} catch (IOException e) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, e.getLocalizedMessage(), e);
		}
		
		if(group.isPrintParticipantInformation()) {
			try {
				Session t = project.openSession(resultSet.getCorpus(), resultSet.getSession());
				
				List<String> participantTitleLine = new ArrayList<String>();
				for(int i = 0; i < getIndentLevel(); i++) participantTitleLine.add("");
				participantTitleLine.add("Participants");
				writer.writeNext(participantTitleLine.toArray(new String[0]));
				
				List<String> currentLine = new ArrayList<String>();
				for(int i = 0; i < indentLevel; i++) currentLine.add(new String());
				String participantTableHeader[] = {
						"Name", "Age", "Sex", "Birthday", "Language", "Education", "Group", "Role" };
				for(int i = 0; i < participantTableHeader.length; i++) currentLine.add(participantTableHeader[i]);
				writer.writeNext(currentLine.toArray(new String[0]));
				
				for(int i = 0; i < t.getParticipantCount(); i++) {
					final Participant participant = t.getParticipant(i);
					currentLine.clear();
					for(int j = 0; j < indentLevel; j++) currentLine.add(new String());
					
					String name = 
						(participant.getName() != null ? participant.getName() : "");
					String age = 
						AgeFormatter.ageToString(participant.getAge(t.getDate()));
					String sex = 
						(participant.getSex() == Sex.MALE ? "M" : "F");
					String birthday = 
						DateFormatter.dateTimeToString(participant.getBirthDate());
					
					String participantLine[] = {
							name, age, sex, birthday,
							participant.getLanguage(), participant.getEducation(),
							participant.getGroup(), participant.getRole().getTitle()
					};
					for(int j = 0; j < participantLine.length; j++) currentLine.add(participantLine[j]);
					
					writer.writeNext(currentLine.toArray(new String[0]));
				}
				writer.writeNext(new String[0]);
				writer.flush();
			} catch (IOException e) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, e.getLocalizedMessage(), e);
			}
		}
	}
	
	/**
	 * Should we indent content
	 */
	public boolean isIndentContent() {
		Object v = super.getProperty(INDENT_CONTENT);
		if(v == null) v = getPropertyDefault(INDENT_CONTENT);
		boolean retVal = false;
		if(v != null && (v instanceof Boolean)) {
			retVal = (Boolean)v;
		}
		return retVal;
	}
	
	public boolean isPrintSectionNames() {
		Object v = super.getProperty(PRINT_SECTION_NAMES);
		if(v == null) v = getPropertyDefault(PRINT_SECTION_NAMES);
		boolean retVal = false;
		if(v != null && (v instanceof Boolean)) {
			retVal = (Boolean)v;
		}
		return retVal;
	}
	
	private int indentLevel = 0;
	private int getIndentLevel() {
		return indentLevel;
	}
	
	private int nextIndentLevel() {
		return (++indentLevel);
	}

	private int prevIndentLevel() {
		int retVal = (indentLevel > 0 ? --indentLevel : 0);
		return retVal;
	}

	@Override
	public String getMimetype() {
		return "text/csv";
	}

	@Override
	public String getFileExtension() {
		return "csv";
	}
	
	@Override
	public String getDisplayName() {
		return "CSV";
	}

}
