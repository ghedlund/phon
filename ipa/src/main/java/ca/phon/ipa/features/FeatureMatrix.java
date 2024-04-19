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

package ca.phon.ipa.features;

import javax.xml.stream.*;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Holds all defined feature set for IPA characters. This information is held in
 * the data/features.xml file.
 * 
 */
public class FeatureMatrix {

	/** The singleton instance */
	private static FeatureMatrix instance;

	/** The default data file */
	private final static String DATA_FILE = "features.xml";
	
	/** Named feature set */
	private TreeMap<String, FeatureSet> namedFeatureSets;

	/** The table of feature sets */
	private LinkedHashMap<Character, FeatureSet> featureSets;

	/** 
	 * Feature name maps for featureData index 
	 * Maps include synonyms for features
	 */
	private LinkedHashMap<String, Integer> featureNameHash;
	private Feature[] featureData;

	private int numberOfFeatures = 0;

	/** Returns the shared instance of the FeatureMatrix */
	public static synchronized FeatureMatrix getInstance() {
		if (instance == null) {
			instance = new FeatureMatrix(
					FeatureMatrix.class.getResourceAsStream(DATA_FILE));
		}
		return instance;
	}
	
	/** Create a new instance of the FeatureMatrix */
	public FeatureMatrix(String fmFile) {
		// create the matrix
		if(fmFile.endsWith(".xml")) {
			try {
				buildFromXML(fmFile);
			} catch (IOException e) {
				Logger.getLogger(getClass().getName()).severe(e.toString());
			}
		} else if(fmFile.endsWith(".csv")) {
			try {
				buildFromCSV(fmFile);
			} catch (IOException e) {
				Logger.getLogger(getClass().getName()).severe(e.toString());
			}
		} else {
			throw new IllegalArgumentException("Feature matrix must be in XML or CSV format.");
		}
	}
	
	public FeatureMatrix(InputStream stream) {
		try {
			buildFromXML(stream);
		} catch (IOException e) {
			Logger.getLogger(getClass().getName()).severe(e.toString());
		}
	}
	
	private void buildFromXML(String fmFile)
		throws IOException {
		FileInputStream stream;
		stream = new FileInputStream(fmFile);
		buildFromXML(stream);
	}
	
	public void saveAsXML(String filename) throws IOException {
		saveAsXML(new File(filename));
	}
	
	public void saveAsXML(File filename) throws IOException {
		saveAsXML(new FileOutputStream(filename));
	}
	
	public void saveAsXML(OutputStream stream) throws IOException {
//		try {
//			final ObjectFactory factory = new ObjectFactory();
//			FeatureMatrixType fmType = createFeatureMatrixType();
//			final JAXBElement<FeatureMatrixType> ele = factory.createFeatureMatrix(fmType);
//
//			final JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
//			final Marshaller marshaller = jaxbContext.createMarshaller();
//
//			final XMLOutputFactory xof = XMLOutputFactory.newFactory();
//			final XMLEventWriter xsw = xof.createXMLEventWriter(stream);
//			marshaller.setListener(new MarshalListener(xsw));
//
//			marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
//			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
//
//			marshaller.marshal(ele, xsw);
//		} catch (JAXBException | XMLStreamException e) {
//			throw new IOException(e);
//		}
		
	}
	
//	private final class MarshalListener extends Marshaller.Listener {
//
//		private XMLEventWriter xsw;
//
//		private XMLEventFactory factory;
//
//		public MarshalListener(XMLEventWriter xsw) {
//			this.xsw = xsw;
//
//			this.factory = XMLEventFactory.newFactory();
//		}
//
//		@Override
//		public void beforeMarshal(Object source) {
//			super.beforeMarshal(source);
//			try {
//				xsw.add(factory.createCharacters("\n"));
//			} catch (XMLStreamException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
//			if(source instanceof FeatureSetType) {
//				final FeatureSetType fsType = (FeatureSetType)source;
//				final String comment = fsType.getChar() + " (0x" + Integer.toHexString(fsType.getChar().charAt(0)) + ")";
//				try {
//					xsw.add(factory.createComment(comment));
//				} catch (XMLStreamException e) {}
//			}
//		}
//
//	}
	
//	private FeatureMatrixType createFeatureMatrixType() {
//		final ObjectFactory factory = new ObjectFactory();
//		final FeatureMatrixType retVal = factory.createFeatureMatrixType();
//
//		// features
//		final Map<String, FeatureType> featureTypeMap = new HashMap<>();
//		for(Feature feature:featureData) {
//			final FeatureType featureType = factory.createFeatureType();
//			featureType.setName(feature.getName());
//			if(feature.getPrimaryFamily() != null && feature.getPrimaryFamily() != FeatureFamily.UNDEFINED)
//				featureType.setPrimaryFamily(Family.fromValue(feature.getPrimaryFamily().toString().toLowerCase().replaceAll("_", " ")));
//			if(feature.getSecondaryFamily() != null && feature.getSecondaryFamily() != FeatureFamily.UNDEFINED)
//				featureType.setSecondaryFamily(Family.fromValue(feature.getSecondaryFamily().toString().toLowerCase().replaceAll("_", " ")));
//			if(feature.getSynonyms() != null) {
//				for(String syn:feature.getSynonyms())
//					featureType.getSynonym().add(syn);
//			}
//
//			featureTypeMap.put(feature.getName(), featureType);
//			retVal.getFeature().add(featureType);
//		}
//
//		// named feature sets
//		for(String key:namedFeatureSets.keySet()) {
//			final NamedFeatureSetType fsType = factory.createNamedFeatureSetType();
//			final FeatureSet fs = namedFeatureSets.get(key);
//			fsType.setName(key);
//
//			for(Feature feature:fs) {
//				fsType.getValue().add(featureTypeMap.get(feature.getName()));
//			}
//
//			retVal.getNamedFeatureSet().add(fsType);
//		}
//
//		// character feature sets
//		for(Character c:getCharacterSet()) {
//			final FeatureSetType fsType = factory.createFeatureSetType();
//			final FeatureSet fs = getFeatureSet(c);
//			fsType.setChar(c.toString());
//
//			for(Feature feature:fs) {
//				fsType.getValue().add(featureTypeMap.get(feature.getName()));
//			}
//
//			retVal.getFeatureSet().add(fsType);
//		}
//
//		return retVal;
//	}
	
	/**
	 * Creates the feature matrix from an xml file.
	 * 
	 */
	private void buildFromXML(InputStream stream)
		throws IOException {
		try {
			XMLInputFactory factory = XMLInputFactory.newFactory();
			XMLStreamReader reader = factory.createXMLStreamReader(stream);

			String currentElement = "";
			String currentFeature = "";
			FeatureFamily currentFamily = FeatureFamily.UNDEFINED;
			FeatureFamily currentSecondaryFamily = FeatureFamily.UNDEFINED;
			List<String> currentSynonyms = new ArrayList<>();

			featureData = new Feature[20];
			featureNameHash = new LinkedHashMap<>();
			featureSets = new LinkedHashMap<>();

			while(reader.hasNext()) {
				int evt = reader.next();
				switch (evt) {
					case XMLStreamConstants.START_ELEMENT:
						currentElement = reader.getLocalName();
						if (currentElement.equals("feature")) {
							currentFeature = reader.getAttributeValue(null, "name");
							String primaryFamily = reader.getAttributeValue(null, "primaryFamily");
							if (primaryFamily != null) {
								currentFamily = FeatureFamily.fromValue(primaryFamily);
							}
							String secondaryFamily = reader.getAttributeValue(null, "secondaryFamily");
							if (secondaryFamily != null) {
								currentSecondaryFamily = FeatureFamily.fromValue(secondaryFamily);
							}
						} else if (currentElement.equals("namedFeatureSet")) {
							String name = reader.getAttributeValue(null, "name");
						} else if (currentElement.equals("feature_set")) {
							String currentChar = reader.getAttributeValue(null, "char");
							String featuresStr = reader.getElementText();
							String[] featureNames = featuresStr.split("\\p{Space}");

							FeatureSet fs = new FeatureSet();
							for (String name : featureNames) {
								Feature f = getFeature(name);
								if (f != null) {
									fs = FeatureSet.union(fs, getFeatureSetForFeature(name));
								}
							}
							featureSets.put(currentChar.charAt(0), fs);
						} else if (currentElement.equals("synonym")) {
							currentSynonyms.add(reader.getElementText());
						}
						break;

					case XMLStreamConstants.END_ELEMENT:
						if (reader.getLocalName().equals("feature")) {
							Feature f = new Feature(currentFeature);
							f.setPrimaryFamily(currentFamily);
							f.setSecondaryFamily(currentSecondaryFamily);
							f.setSynonyms(currentSynonyms.toArray(new String[0]));
							featureNameHash.put(currentFeature.toLowerCase(), numberOfFeatures);
							for(String syn:currentSynonyms) {
								featureNameHash.put(syn.toLowerCase(), numberOfFeatures);
							}
							featureData = Arrays.copyOf(featureData, numberOfFeatures + 1);
							featureData[numberOfFeatures] = f;
							numberOfFeatures++;

							BitSet bs = new BitSet(numberOfFeatures);
							bs.clear();
							bs.set(numberOfFeatures - 1, true);
							f.setFeatureSet(new FeatureSet(bs));

							currentFamily = FeatureFamily.UNDEFINED;
							currentSecondaryFamily = FeatureFamily.UNDEFINED;
							currentSynonyms.clear();
						}
						break;
				}
			}
			reader.close();
		} catch (XMLStreamException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Create the feature matrix from a CSV file.
	 */
	@Deprecated
	private void buildFromCSV(String fmFile) 
		throws IOException {
//		CSVReader reader = new CSVReader(new FileReader(fmFile));
//		String[] colLine = reader.readNext();
//
//		// first two cols are char and unicode value
//		int sFeatureIdx = 2;
//		featureNameHash = new LinkedHashMap<String, Integer>();
//		reverseHash = new LinkedHashMap<Integer, String>();
//		featureData = new FeatureDescription[colLine.length-2];
//		numberOfFeatures = colLine.length-2;
//
//		// create list of features
//		for(int i = sFeatureIdx; i < colLine.length; i++) {
//			int featureIndex = i-2;
//			String currentFeature = colLine[i];
//
//			FeatureDescription fd = new FeatureDescription();
//			fd.featureName = currentFeature;
//			fd.primary = "";
//			fd.secondary = "";
//			BitSet bs = new BitSet(numberOfFeatures);
//			bs.clear();
//			bs.set(featureIndex, true);
//			fd.fs = new FeatureSet(bs);
//
//			featureNameHash.put(currentFeature, featureIndex);
//			reverseHash.put(featureIndex, currentFeature);
//			featureData[featureIndex] = fd;
//		}
//
//		featureSets = new LinkedHashMap<Character, FeatureSet>();
//		// create feature sets from remainder rows
//		String[] line = null;
//		while((line = reader.readNext()) != null) {
//			String unicodeValue = line[1];
//			Integer charVal = Integer.decode(unicodeValue);
//			Character currentChar = new Character((char)charVal.byteValue());
//
//			BitSet bs = new BitSet(numberOfFeatures);
//			bs.clear();
//
//			for(int i = sFeatureIdx; i < line.length; i++) {
//				String featureIdc = line[i];
//				if(StringUtils.strip(featureIdc).equals("+")) {
//					bs.set(i-2, true);
//				}
//			}
//
//			FeatureSet fs = new FeatureSet(bs);
//			fs.setIpaChar(currentChar.charValue());
//			featureSets.put(currentChar, fs);
//		}
//
//		reader.close();
	}

	/**
	 * Returns the number of features.
	 */
	public int getNumberOfFeatures() {
		return numberOfFeatures;
	}

	/**
	 * Return the feature set described by the given ipaChar, or null if
	 * nonexistent.
	 * 
	 * @param ipaChar
	 *            IPA character describing the desired feature set
	 * @return the desired feature set
	 */
	public FeatureSet getFeatureSet(char ipaChar) {
		FeatureSet fs = featureSets.get(ipaChar);
		if (fs == null) {
			fs = new FeatureSet();
		}
		return fs;
	}

	/**
	 * Remove the feature set from the matrix if it exists, do nothing
	 * otherwise.
	 * 
	 * @param ipaChar
	 *            IPA character representing the feature set to remove
	 */
	public void removeFeatureSet(char ipaChar) {
		if (featureSets.keySet().contains(ipaChar)) {
			featureSets.remove(ipaChar);
		}
	}

	/**
	 * Set the given feature set for the given IPA character. If that character
	 * already has feature set defined, replace that set with the given feature
	 * set. Any given feature or feature set that doesn't exist already is
	 * created.
	 * 
	 * @param ipaChar
	 *            IPA character to put the feature set with
	 * @param newFeatureSet
	 *            feature set to put in
	 */
	public void putFeatureSet(char ipaChar, FeatureSet newFeatureSet) {
		featureSets.put(ipaChar, newFeatureSet);
	}

	/**
	 * Returns a hash set of the names of all the features in the matrix as
	 * strings. If there are no features, returns an empty hash set.
	 * 
	 * @return a hash set of the feature names
	 */
	public Set<String> getFeatures() {
		Set<String> retVal = new HashSet<String>();
		
		for(Feature f:featureData) {
			retVal.add(f.getName());
		}
		
		return retVal;
	}
	
	/**
	 * Returns the Feature object for the given
	 * feature name (or synonym.)
	 * 
	 * @param featureName
	 * @return the Feature or null if not found
	 */
	public Feature getFeature(String featureName) {
		Feature retVal = null;
		
		String fName = featureName.toLowerCase();
		Integer fIdx = featureNameHash.get(fName);
		if(fIdx != null && fIdx >= 0 && fIdx < featureData.length) {
			retVal = featureData[fIdx];
		}
		
		return retVal;
	}
	
	/**
	 * Return all named feature sets.
	 * 
	 * @return map of named feature sets
	 */
	public Map<String, FeatureSet> getNamedFeatureSets() {
		return this.namedFeatureSets;
	}
	
	/**
	 * Set the value of a named FeatureSet
	 * 
	 * @param name
	 * @param featureSet
	 */
	public void putNamedFeatureSet(String name, FeatureSet featureSet) {
		this.namedFeatureSets.put(name, featureSet);
	}

	/**
	 * Return the value of a named FeatureSet
	 * 
	 * @param name
	 * @return featureSet for given name or <code>null</code>
	 */
	public FeatureSet getNamedFeatureSet(String name) {
		return this.namedFeatureSets.get(name);
	}

	/**
	 * Returns a hash map with all of the feature sets in the matrix as values.
	 * Each feature set has its character as its matching key. If not feature
	 * sets exist, returns an empty hash map.
	 * 
	 * @return a hash map of characters to feature sets
	 */
	public Map<Character, FeatureSet> getFeatureSets() {
		return featureSets;
	}

	/**
	 * Returns the feature set for the name feature.
	 * 
	 * @param feature
	 *            the feature to look up
	 * @return the featureset for the give feature or an empty set if the
	 *         feature was not found
	 */
	public FeatureSet getFeatureSetForFeature(String feature) {
		BitSet fs = new BitSet(numberOfFeatures);
		fs.clear();
		FeatureSet retVal = new FeatureSet(fs);

		Integer fIdx = featureNameHash.get(feature.toLowerCase());
		if (fIdx != null && fIdx >= 0) {
			retVal = featureData[fIdx].getFeatureSet();
		}
		return retVal;
	}

	/**
	 * Returns a collection of all characters that have the given feature.
	 * 
	 * @param featureName
	 *            the name of the feature to search by
	 * @return collection of all characters that match featureName
	 */
	public Collection<Character> getCharactersWithFeature(String featureName) {
		ArrayList<Character> retVal = new ArrayList<Character>();

		for (Character ch : featureSets.keySet()) {
			FeatureSet fs = featureSets.get(ch);
			if (fs != null && fs.hasFeature(featureName))
				retVal.add(ch);
		}

		return retVal;
	}
	
	/**
	 * Returns the character set supported by this feature matrix
	 * 
	 * @return set of characters
	 */
	public Set<Character> getCharacterSet() {
		return this.featureSets.keySet();
	}

	/**
	 * Returns the name of the primary family for the given feature. null if the
	 * feature does not have a primary family.
	 * 
	 * @param featureName
	 *            name of feature
	 * @return name of feature's primary family
	 */
	public String getFeaturePrimaryFamily(String featureName) {
		String retVal = "";

		Integer fIdx = featureNameHash.get(featureName);
		if (fIdx != null && fIdx >= 0) {
			Feature fd = featureData[fIdx];
			if (fd != null)
				retVal = fd.getPrimaryFamily().value();
		}

		return retVal;
	}

	/**
	 * Returns the feature name for the given index.
	 */
	public String getFeatureForIndex(int idx) {
		return featureData[idx].getName();
	}

	/**
	 * Sets the primary family for the given feature. If family doesn't exist,
	 * it is created. Returns true if successful, false if not. Put will be
	 * unsuccessful if feature doesn't exist.
	 * 
	 * @param featureName
	 *            name of feature
	 * @param familyName
	 *            name of primary family to put with feature
	 * @return true if successful, false if not
	 */
	public boolean putFeaturePrimaryFamily(String featureName, String familyName) {
		boolean retVal = false;
		Integer fIdx = featureNameHash.get(featureName);
		if (fIdx != null && fIdx >= 0) {
			Feature fd = featureData[fIdx];
			if (fd != null) {
				fd.setPrimaryFamily(FeatureFamily.fromValue(familyName.toLowerCase()));
				retVal = true;
			}
		}
		return retVal;
	}

	/**
	 * Returns the secondary families for the given feature. Null if the feature
	 * does not have any secondary families.
	 * 
	 * @param featureName
	 *            name of the feature
	 * @return name of the feature's secondary family
	 */
	public String getFeatureSecondaryFamily(String featureName) {
		String retVal = "";

		Integer fIdx = featureNameHash.get(featureName);
		if (fIdx != null && fIdx >= 0) {
			Feature fd = featureData[fIdx];
			if (fd != null)
				retVal = fd.getSecondaryFamily().value();
		}

		return retVal;
	}

	/**
	 * Sets the secondary family for the given feature. If family doesn't exist,
	 * it is created. Returns true if successful, false if not. Put will be
	 * unsuccessful if feature doesn't exist.
	 * 
	 * @param featureName
	 *            name of feature
	 * @param familyName
	 *            name of secondary family to put with feature
	 * @return true if successful, false if not
	 */
	public boolean putFeatureSecondaryFamily(String featureName,
			String familyName) {
		boolean retVal = false;
		Integer fIdx = featureNameHash.get(featureName);
		if (fIdx != null && fIdx >= 0) {
			Feature fd = featureData[fIdx];
			if (fd != null) {
				fd.setSecondaryFamily(FeatureFamily.fromValue(familyName.toLowerCase()));
				retVal = true;
			}
		}
		return retVal;
	}

	public Collection<String> getFeaturesWithPrimaryFamily(String familyName) {
		ArrayList<String> result = new ArrayList<String>();
		for (String feature : getFeatures()) {
			Integer fIdx = featureNameHash.get(feature);
			if (fIdx != null && fIdx >= 0) {
				Feature fd = featureData[fIdx];
				FeatureFamily family = fd.getPrimaryFamily();
				if (family != null && family == FeatureFamily.fromValue(familyName.toLowerCase()))
					result.add(feature);
			}
		}
		return result;
	}
	
	public Feature[] getFeatureData() {
		return this.featureData;
	}
	
}