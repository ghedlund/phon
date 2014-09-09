package ca.phon.ipa;

import java.text.CollationKey;
import java.text.Collator;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ca.phon.ipa.features.FeatureSet;
import ca.phon.phonex.PhonexMatcher;
import ca.phon.phonex.PhonexPattern;

public class FeatureCollator extends Collator {

	private final List<FeatureSet> featureSets = new ArrayList<FeatureSet>();
	
	public static FeatureCollator createPlaceCollator() {
		return new FeatureCollator(
				FeatureSet.fromArray(new String[]{"Labial"}),
				FeatureSet.fromArray(new String[]{"Coronal"}),
				FeatureSet.fromArray(new String[]{"Dorsal"}),
				FeatureSet.fromArray(new String[]{"Guttural"}));
	}
	
	public static FeatureCollator createVoicingCollator() {
		return new FeatureCollator(
				FeatureSet.fromArray(new String[]{"Voiceless"}),
				FeatureSet.fromArray(new String[]{"Voiced"}));
	}
	
	public static FeatureCollator createMannerCollator() {
		return new FeatureCollator(
				FeatureSet.fromArray(new String[]{"Stop"}),
				FeatureSet.fromArray(new String[]{"Fricative"}),
				FeatureSet.fromArray(new String[]{"Affricate"}),
				FeatureSet.fromArray(new String[]{"Nasal"}),
				FeatureSet.fromArray(new String[]{"Lateral"}),
				FeatureSet.fromArray(new String[]{"Rhotic"}),
				FeatureSet.fromArray(new String[]{"Glide"}),
				FeatureSet.fromArray(new String[]{"Vowel"}));
	}
	
	public static FeatureCollator createContinuancyCollator() {
		return new FeatureCollator(
				FeatureSet.fromArray(new String[]{"Stop"}),
				FeatureSet.fromArray(new String[]{"Fricative"}),
				FeatureSet.fromArray(new String[]{"Approximant"}));
	}
	
	public FeatureCollator() {
		super();
	}
	
	public FeatureCollator(FeatureSet ... sets) {
		this(Arrays.asList(sets));
	}
	
	public FeatureCollator(List<FeatureSet> sets) {
		super();
		featureSets.addAll(sets);
	}
	
	/**
	 * Live list of feature sets.
	 * 
	 * @return
	 */
	public List<FeatureSet> getFeatureSets() {
		return this.featureSets;
	}
	
	private int featureSetIndex(FeatureSet fs) {
		int retVal = Integer.MAX_VALUE;
		
		for(int i = 0; i < featureSets.size(); i++) {
			final FeatureSet testFs = featureSets.get(i);
			if(fs.intersect(testFs).equals(testFs)) {
				retVal = i;
				break;
			}
		}
		
		return retVal;
	}
	
	@Override
	public int compare(String source, String target) {
		final CollationKey k1 = getCollationKey(source);
		final CollationKey k2 = getCollationKey(target);
		return k1.compareTo(k2);
	}

	@Override
	public CollationKey getCollationKey(String source) {
		return new FeatureCollationKey(source);
	}

	@Override
	public int hashCode() {
		return featureSets.hashCode();
	}

	private class FeatureCollationKey extends CollationKey {
		
		private FeatureSet[] features;
		
		private IPATranscript t;

		protected FeatureCollationKey(String source) {
			super(source);
			features = new FeatureSet[0];
			
			IPATranscript t = new IPATranscript();
			try {
				t = IPATranscript.parseIPATranscript(source);
				if(t != null) {
					final PhonexPattern pattern = PhonexPattern.compile("(\\w+)\\b\u2194\\b(\\w+)");
					final PhonexMatcher matcher = pattern.matcher(t);
					if(matcher.matches()) {
						t = new IPATranscript(matcher.group(1));
						System.out.println(t);
					}
					features = new FeatureSet[t.length()];
					this.t = t;
				}
			} catch (ParseException e) {
			}
			for(int i = 0; i < features.length; i++) {
				features[i] = t.elementAt(i).getFeatureSet();
			}
		}

		@Override
		public int compareTo(CollationKey target) {
			byte[] myBytes = toByteArray();
			byte[] targetBytes = target.toByteArray();
			
			int cmp = 0;
			int idx = 0;
			int max = Math.min(myBytes.length, targetBytes.length);
			
			while(idx < max && cmp == 0) {
				cmp = ((Byte)myBytes[idx]).compareTo(targetBytes[idx]);
				if(cmp == 0) {
					final FeatureCollationKey targetKey = (FeatureCollationKey)target;
					cmp = t.elementAt(idx).toString().compareTo(targetKey.t.toString());
				}
			}
			
			if(cmp == 0) {
				if(myBytes.length != targetBytes.length) {
					cmp = ((Integer)myBytes.length).compareTo(targetBytes.length);
				}
			}
			
			return cmp;
		}

		@Override
		public byte[] toByteArray() {
			byte[] retVal = new byte[features.length];
			
			for(int i = 0; i < features.length; i++) {
				retVal[i] = (byte)featureSetIndex(features[i]);
			}
			
			return retVal;
		}
		
	}
	
}
