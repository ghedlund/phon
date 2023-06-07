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
package ca.phon.session.check;

import ca.phon.ipa.*;
import ca.phon.ipa.alignment.*;
import ca.phon.plugin.*;
import ca.phon.session.Record;
import ca.phon.session.*;
import ca.phon.util.PrefHelper;

import java.util.*;

@PhonPlugin(name="Check Phone Alignments", comments="Check phone alignments")
@Rank(2)
public class CheckAlignment implements SessionCheck, IPluginExtensionPoint<SessionCheck> {
	
	public final static String RESET_ALIGNMENT = CheckAlignment.class.getName() + ".resetAlignment";
	public final static boolean DEFAULT_RESET_ALIGNMENT = false;
	private boolean resetAlignment = PrefHelper.getBoolean(RESET_ALIGNMENT, DEFAULT_RESET_ALIGNMENT);
	
	public CheckAlignment() {
		super();
	}
	
	public boolean isResetAlignment() {
		return resetAlignment;
	}

	public void setResetAlignment(boolean resetAlignment) {
		this.resetAlignment = resetAlignment;
	}

	@Override
	public Class<?> getExtensionType() {
		return SessionCheck.class;
	}

	@Override
	public IPluginExtensionFactory<SessionCheck> getFactory() {
		return (Object ... args) -> this;
	}

	@Override
	public boolean performCheckByDefault() {
		return true;
	}

	@Override
	public boolean checkSession(SessionValidator validator, Session session) {
		boolean modified = false;
		
//		PhoneAligner aligner = new PhoneAligner();
//		for(int rIdx = 0; rIdx < session.getRecordCount(); rIdx++) {
//			final Record r = session.getRecord(rIdx);
//
//			if(isResetAlignment()) {
//				// reset all alignment tiers
//				List<Tier<PhoneMap>> alignmentTiers = r.getTiersOfType(PhoneMap.class);
//				for(var tier:alignmentTiers) {
//					for(int gIdx = 0; gIdx < tier.numberOfGroups(); gIdx++) {
//						PhoneMap oldAlignment = tier.getGroup(gIdx);
//						PhoneMap newAlignment = aligner.calculatePhoneAlignment(oldAlignment.getTargetRep(), oldAlignment.getActualRep());
//						tier.setGroup(gIdx, newAlignment);
//
//						boolean changed = !oldAlignment.toString().equals(newAlignment.toString());
//
//						if(changed) {
//							ValidationEvent evt = new ValidationEvent(session, rIdx, SystemTierType.SyllableAlignment.getName(), gIdx,
//									"Reset alignment", new ResetAlignmentQuickFix());
//							validator.fireValidationEvent(evt);
//						}
//
//						modified |= changed;
//					}
//				}
//			}
//			for(int gIdx = 0; gIdx < r.numberOfGroups(); gIdx++) {
//				Group g = r.getGroup(gIdx);
//
//				IPATranscript ipaTarget = g.getIPATarget();
//				IPATranscript ipaActual = g.getIPAActual();
//				PhoneMap alignment = g.getPhoneAlignment();
//
//				if(ipaTarget == null || ipaActual == null || alignment == null) continue;
//
//				boolean shouldHaveAlignment = ipaTarget.audiblePhones().length() > 0 && ipaActual.audiblePhones().length() > 0;
//				if(!shouldHaveAlignment) continue;
//
//				IPATranscriptBuilder targetBuilder = new IPATranscriptBuilder();
//				IPATranscriptBuilder actualBuilder = new IPATranscriptBuilder();
//				for(int i = 0; i < alignment.getAlignmentLength(); i++) {
//					List<IPAElement> alignedEles = alignment.getAlignedElements(i);
//					if(alignedEles.get(0) != null) {
//						targetBuilder.append(alignedEles.get(0));
//					}
//					if(alignedEles.get(1) != null) {
//						actualBuilder.append(alignedEles.get(1));
//					}
//				}
//
//				if(!ipaTarget.audiblePhones().toString().equals(targetBuilder.toIPATranscript().toString())
//						|| !ipaActual.audiblePhones().toString().equals(actualBuilder.toIPATranscript().toString())) {
//					ValidationEvent evt = new ValidationEvent(session, rIdx, SystemTierType.SyllableAlignment.getName(), gIdx,
//							"Alignment out of sync - requires reset", new ResetAlignmentQuickFix());
//					validator.fireValidationEvent(evt);
//					continue;
//				}
//
//				for(int wIdx = 0; wIdx < g.getAlignedWordCount(); wIdx++) {
//					Word w = g.getAlignedWord(wIdx);
//					IPATranscript ipaT = w.getIPATarget();
//					IPATranscript ipaA = w.getIPAActual();
//					if(ipaT != null && ipaA != null) {
//						PhoneMap align  = w.getPhoneAlignment();
//						if(align != null) {
//							// ensure alignment exists within proper word boundaries
//							boolean ipaTargetMatches = ipaT.audiblePhones().toString().equals(align.getTargetRep().audiblePhones().toString());
//							boolean ipaActualMatches = ipaA.audiblePhones().toString().equals(align.getActualRep().audiblePhones().toString());
//
//							if(!ipaTargetMatches || !ipaActualMatches) {
//								ValidationEvent evt = new ValidationEvent(session, rIdx, SystemTierType.SyllableAlignment.getName(), gIdx,
//									"Phone alignment crosses word boundary", new ResetAlignmentQuickFix());
//								validator.fireValidationEvent(evt);
//								break;
//							}
//						}
//					}
//				}
//			}
//		}
		
		return modified;
	}

	@Override
	public Properties getProperties() {
		Properties retVal = new Properties();
		
		retVal.put(RESET_ALIGNMENT, isResetAlignment());
		
		return retVal;
	}

	@Override
	public void loadProperties(Properties props) {
		setResetAlignment(Boolean.parseBoolean(props.getProperty(RESET_ALIGNMENT, Boolean.toString(DEFAULT_RESET_ALIGNMENT))));
	}
	
	public static class ResetAlignmentQuickFix extends SessionQuickFix {

		public ResetAlignmentQuickFix() {
			super();
		}
		
		@Override
		public String getDescription() {
			return "Reset alignment";
		}

		@Override
		public boolean fix(ValidationEvent evt) {
//			Record r = evt.getSession().getRecord(evt.getRecord());
//			Group g = r.getGroup(evt.getGroup());
//
//			IPATranscript ipaT = g.getIPATarget();
//			IPATranscript ipaA = g.getIPAActual();
//			PhoneMap pm = (new PhoneAligner()).calculatePhoneAlignment(ipaT, ipaA);
//			g.setPhoneAlignment(pm);
//
			return true;
		}
		
	}

}
