package ca.phon.app.session.editor.view.ipaDictionary;

import ca.phon.ipa.IPATranscript;
import ca.phon.ipadictionary.IPADictionary;

public interface IPALookupPostProcessor {

	/**
	 * Apply rules/transformations to given {@link IPATranscript} object
	 * using given information. This is executed during IPA Lookup/automatic
	 * transcription before the value is assigned to the tier.
	 *
	 * @param dictionary
	 * @param orthography
	 * @param transcript
	 */
	public IPATranscript postProcess(IPADictionary dictionary, String orthography, IPATranscript transcript);

}
