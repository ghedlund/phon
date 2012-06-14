package ca.phon.phonex;

import java.util.ArrayList;
import java.util.List;

import ca.phon.ipa.IPAElement;
import ca.phon.ipa.elements.CompoundPhone;
import ca.phon.ipa.elements.Pause;
import ca.phon.ipa.elements.Phone;
import ca.phon.ipa.elements.StressMarker;
import ca.phon.ipa.elements.SyllableBoundary;
import ca.phon.ipa.elements.WordBoundary;
import ca.phon.visitor.VisitorAdapter;
import ca.phon.visitor.annotation.Visits;

/**
 * Filter a iterable list of phones based on phonex
 * flags.
 */
public class PhonexFlagVisitor extends VisitorAdapter<IPAElement> {
	
	/**
	 * Return value
	 */
	private final List<IPAElement> filteredList = 
			new ArrayList<IPAElement>();
	
	/**
	 * phonex flags
	 */
	

	@Override
	public void fallbackVisit(IPAElement obj) {
		
	}
	
	@Visits
	public void basicPhone(Phone phone) {
		
	}
	
	@Visits
	public void compoundPhone(CompoundPhone phone) {
		
	}
	
	@Visits
	public void pause(Pause phone) {
		
	}
	
	@Visits
	public void stress(StressMarker phone) {
		
	}

	@Visits
	public void syllableBoundary(SyllableBoundary phone) {
		
	}
	
	@Visits
	public void wordBoundary(WordBoundary phone) {
		
	}
}
