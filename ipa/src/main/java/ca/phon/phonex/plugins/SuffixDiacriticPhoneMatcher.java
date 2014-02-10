package ca.phon.phonex.plugins;

import ca.phon.ipa.CompoundPhone;
import ca.phon.ipa.Diacritic;
import ca.phon.ipa.IPAElement;
import ca.phon.ipa.IPAElementFactory;
import ca.phon.ipa.Phone;
import ca.phon.phonex.PhoneMatcher;
import ca.phon.visitor.VisitorAdapter;
import ca.phon.visitor.annotation.Visits;

/**
 * 
 */
public class SuffixDiacriticPhoneMatcher extends DiacriticPhoneMatcher {

	public SuffixDiacriticPhoneMatcher(String phonex) {
		super(phonex);
	}
	
	public SuffixDiacriticPhoneMatcher(PhoneMatcher matcher) {
		super(matcher);
	}

	@Override
	public boolean matches(IPAElement p) {
		final SuffixDiacriticVisitor visitor = new SuffixDiacriticVisitor();
		p.accept(visitor);
		return visitor.matches;
	}

	@Override
	public boolean matchesAnything() {
		return getMatcher().matchesAnything();
	}

	/**
	 * Visitor for match
	 */
	private class SuffixDiacriticVisitor extends VisitorAdapter<IPAElement> {
		
		public boolean matches = false;

		@Override
		public void fallbackVisit(IPAElement obj) {
			
		}
		
		@Visits
		public void visitBasicPhone(Phone phone) {
			final IPAElementFactory factory = new IPAElementFactory();
			
			// prefix
			if(phone.getPrefixDiacritic() != null) {
				final Diacritic suffixDiacritic = factory.createDiacritic(phone.getSuffixDiacritic());
				matches |= getMatcher().matches(suffixDiacritic);
			}
		}
		
		@Visits
		public void visitCompoundPhone(CompoundPhone cp) {
			visit(cp.getFirstPhone());
			visit(cp.getSecondPhone()); 
		}
		
	}

}
