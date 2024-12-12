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
package ca.phon.session.io.xml.v1_2;

import ca.phon.extensions.UnvalidatedValue;
import ca.phon.ipa.*;
import ca.phon.ipa.alignment.PhoneMap;
import ca.phon.orthography.*;
import ca.phon.orthography.Error;
import ca.phon.orthography.Linker;
import ca.phon.orthography.Pause;
import ca.phon.orthography.PauseLength;
import ca.phon.orthography.Word;
import ca.phon.orthography.mor.GraspTierData;
import ca.phon.orthography.mor.MorTierData;
import ca.phon.plugin.*;
import ca.phon.session.Comment;
import ca.phon.session.Record;
import ca.phon.session.*;
import ca.phon.session.io.*;
import ca.phon.session.io.xml.v12.*;
import ca.phon.session.io.xml.v12.CommentType;
import ca.phon.session.io.xml.v12.UserTierType;
import ca.phon.session.io.xml.v12.WordType;
import ca.phon.session.tierdata.TierData;
import ca.phon.syllable.*;
import ca.phon.util.Language;
import ca.phon.visitor.VisitorAdapter;
import ca.phon.visitor.annotation.Visits;
import ca.phon.worker.PhonWorker;
import ca.phon.xml.XMLObjectReader;
import ca.phon.xml.annotation.XMLSerial;
import jakarta.xml.bind.*;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.datatype.Duration;
import javax.xml.datatype.*;
import javax.xml.namespace.QName;
import javax.xml.parsers.*;
import javax.xml.stream.*;
import javax.xml.stream.events.XMLEvent;
import java.io.*;
import java.text.ParseException;
import java.time.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Session XML reader for session files with
 * version 'PB1.2'
 *
 * When reading in this older format alignment may be fixed.
 *
 * Fixing alignment
 *
 * Small pinchers for phonetic groups have been added to the IPA parser.  This was necessary for one-to-one alignment and is our solution for upgrading Phon data.  Currently, I don't do anything when reading the older version 1.3 files when alignment is not complete in groups.  We previously discussed using ',' but I think now that is a mistake. Here are some siuations.  In each example, I'm using upper case letters for words in orthography and smaller case for ipa.
 *
 * Old Phon:
 * ```
 * Orthography: [A] [B C] [D E F] [ ] [G H]
 * IPA Target:  [1] [1  ] [1 2 3] [ ] [1  ]
 * IPA Actual:  [a] [   ] [b c  ] [d] [e f]
 * ```
 *
 * New Phon fixed alignment (spaces added for clarity):
 * ```
 * Orthography: A ‹B C› ‹D E F› yyy G H .
 * IPA Actual:  a *     ‹b c›   d   e f
 * ```
 *
 * New Phon don't fix alignment:
 * ```
 * Orthography: A B C D E F G H
 * IPA Actual:  a b c d e f
 * ```
 *
 * Rules:
 *
 *  * If # of words match between Orthography and IPA - just add content (group 1/5 above)
 *  * If there is content in Orthography and not IPA, wrap orthography in phonetic group markers if more than one word and add * to IPA tier (group 2 above)
 *  * If there is content in IPA and not orthography, wrap IPA in phonetic group markers if more than one word and add yyy to Orthography (group 4 above)
 *  * If there is content in both IPA and orthography but the number of words do not match, wrap Orthography and IPA in phonetic group markers if they have more than one word (group 3 above)
 *  * If no terminator specified one will be added (e.g., '.')
 *
 */
@XMLSerial(
	namespace="http://phon.ling.mun.ca/ns/phonbank",
	elementName="session",
	bindType=Session.class
)
@SessionIO(
		group="ca.phon",
		id="phonbank",
		version="1.2",
		mimetype="application/xml",
		extension="xml",
		name="Phon <3.6 (.xml)"
)
@Rank(1)
public class XmlSessionReaderV1_2 implements SessionReader, XMLObjectReader<Session>, IPluginExtensionPoint<SessionReader> {

	private boolean fixAlignment = false;

	public boolean isFixAlignment() {
		return fixAlignment;
	}

	public void setFixAlignment(boolean fixAlignment) {
		this.fixAlignment = fixAlignment;
	}

	@Override
	public Session read(Document doc, Element ele)
			throws IOException {
		Session retVal = null;

		final ObjectFactory xmlFactory = new ObjectFactory();

		try {
			final JAXBContext context = JAXBContext.newInstance(xmlFactory.getClass());
			final Unmarshaller unmarshaller = context.createUnmarshaller();
			final JAXBElement<SessionType> sessionTypeEle =
					unmarshaller.unmarshal(doc.getDocumentElement(), SessionType.class);

			if(sessionTypeEle != null && sessionTypeEle.getValue() != null) {
				final SessionType sessionType = sessionTypeEle.getValue();
				retVal = readSessionType(sessionType);
			}
		} catch (JAXBException e) {
			throw new IOException(e);
		}

		return retVal;
	}

	/**
	 * Read session in from given xml {@link SessionType} object.
	 *
	 * @param sessionType
	 * @return session
	 */
	private Session readSessionType(SessionType sessionType) {
		final SessionFactory factory = SessionFactory.newFactory();
		final Session retVal = factory.createSession();

		// copy header info
		retVal.setName(sessionType.getId());
		retVal.setCorpus(sessionType.getCorpus());

		final HeaderType headerData = sessionType.getHeader();
		if(headerData != null) {
			if(headerData.getMedia() != null && headerData.getMedia().length() > 0) {
				retVal.setMediaLocation(headerData.getMedia());
			}
			if(headerData.getDate() != null) {
				final XMLGregorianCalendar xmlDate = headerData.getDate();
				final LocalDate dateTime = LocalDate.of(
						xmlDate.getYear(),
						xmlDate.getMonth(),
						xmlDate.getDay());
				retVal.setDate(dateTime);
			}
			if(headerData.getLanguage().size() > 0) {
				List<Language> langs = new ArrayList<>();
				for(String lang:headerData.getLanguage()) {
					final Language language = Language.parseLanguage(lang);
					langs.add(language);
				}
				retVal.setLanguages(langs);
			}
		}

		// copy participant information
		final ParticipantsType participants = sessionType.getParticipants();
		if(participants != null) {
			for(ParticipantType pt:participants.getParticipant()) {
				final Participant p = copyParticipant(factory, pt, retVal.getDate());
				retVal.addParticipant(p);
			}
		}

		// copy transcriber information
		final TranscribersType transcribers = sessionType.getTranscribers();
		if(transcribers != null) {
			for(TranscriberType tt:transcribers.getTranscriber()) {
				final Transcriber t = copyTranscriber(factory, tt);
				retVal.addTranscriber(t);
			}
			// set default blind tiers
			final List<String> blindTiers = List.of(SystemTierType.IPATarget.getName(), SystemTierType.IPAActual.getName());
			retVal.setBlindTiers(blindTiers);
		}

		// copy tier information
		final UserTiersType userTiers = sessionType.getUserTiers();
		if(userTiers != null) {
			for(UserTierType utt:userTiers.getUserTier()) {
				if("Postcode".equals(utt.getTierName())) {
					continue;
				}
				final TierDescription td = copyTierDescription(factory, utt);
				retVal.addUserTier(td);
			}
		}

		final List<TierViewItem> tierOrder = new ArrayList<TierViewItem>();
		for(TvType tot:sessionType.getTierOrder().getTier()) {
			String tierName = tot.getTierName();
			if("Postcode".equals(tierName)) {
				continue;
			}
			// convert Notes to Comments
			if(SystemTierType.Notes.getName().equals(tierName)) {
				tierName = ca.phon.session.UserTierType.Comments.getPhonTierName();

				// add comments to tier description if necessary
				if(retVal.getUserTiers().getUserTierDescription(ca.phon.session.UserTierType.Comments.getPhonTierName()) == null) {
					final TierDescription td = factory.createTierDescription(ca.phon.session.UserTierType.Comments.getPhonTierName(), true);
					retVal.addUserTier(0, td);
				}
			}

			if("morphology".equalsIgnoreCase(tierName)) {
				tierName = "mor";
			} else if("grasp".equalsIgnoreCase(tierName)) {
				tierName = "gra";
			}
			tot.setTierName(tierName);
			final TierViewItem toi = copyTierViewItem(factory, tot);
			tierOrder.add(toi);
		}
		retVal.setTierView(tierOrder);

		// copy transcript data
		if(sessionType.getTranscript() != null) {
			for(Object uOrComment:sessionType.getTranscript().getUOrComment()) {
				if(uOrComment instanceof CommentType) {
					final CommentType ct = (CommentType)uOrComment;
					final Comment comment = copyComment(factory, ct);
					retVal.getTranscript().addComment(comment);
				} else {
					final RecordType rt = (RecordType)uOrComment;
					Record record = null;
					try {
						record = factory.createRecord(new LazyRecord(factory, retVal, rt));
					} catch (Exception e) {
						Logger.getLogger(getClass().getName()).log(Level.WARNING, e.getLocalizedMessage(), e);
					}
					retVal.addRecord(record);
				}
			}
		}

		return retVal;
	}

	// participants
	Participant copyParticipant(SessionFactory factory, ParticipantType pt, LocalDate sessionDate) {
		final Participant retVal = factory.createParticipant();

		retVal.setId(pt.getId());
		retVal.setName(pt.getName());

		final XMLGregorianCalendar bday = pt.getBirthday();
		if(bday != null) {
			final LocalDate bdt = LocalDate.of(bday.getYear(), bday.getMonth(), bday.getDay());
			retVal.setBirthDate(bdt);

			// calculate age up to the session date
			if(sessionDate != null) {
				final Period period = Period.between(bdt, sessionDate);
				retVal.setAgeTo(period);
			}
		}

		final Duration ageDuration = pt.getAge();
		if(ageDuration != null) {
			// convert to period
			final Period age = Period.of(ageDuration.getYears(), ageDuration.getMonths(), ageDuration.getDays());
			retVal.setAge(age);
		}

		retVal.setEducation(pt.getEducation());
		retVal.setGroup(pt.getGroup());

		String langs = "";
		for(String lang:pt.getLanguage())
			langs += (langs.length() > 0 ? " " : "") + lang;
		retVal.setLanguage(langs);

		if(pt.getSex() == SexType.MALE)
			retVal.setSex(Sex.MALE);
		else if(pt.getSex() == SexType.FEMALE)
			retVal.setSex(Sex.FEMALE);
		else
			retVal.setSex(Sex.UNSPECIFIED);

		ParticipantRole prole = ParticipantRole.fromString(pt.getRole());
		if(prole == null)
			prole = ParticipantRole.TARGET_CHILD;
		retVal.setRole(prole);

		retVal.setSES(pt.getSES());

		return retVal;
	}

	// transcribers
	private Transcriber copyTranscriber(SessionFactory factory, TranscriberType tt) {
		final Transcriber retVal = factory.createTranscriber();
		retVal.setUsername(tt.getId());
		retVal.setRealName(tt.getName());
		if(tt.getPassword() != null) {
			retVal.setPassword(tt.getPassword().getContent());
			retVal.setUsePassword(tt.getPassword().isUse());
		}
		return retVal;
	}

	// tier descriptions
	private TierDescription copyTierDescription(SessionFactory factory, UserTierType utt) {
		final String name = utt.getTierName();
		Class<?> type = TierData.class;
		final var uttType = ca.phon.session.UserTierType.fromPhonTierName(name);
		if(uttType != null) {
			type = uttType.getType();
		}

		return factory.createTierDescription(
				uttType != null ? uttType.getPhonTierName() : name, type, new HashMap<>(), true);
	}

	private TierViewItem copyTierViewItem(SessionFactory factory, TvType tvt) {
		final boolean locked = tvt.isLocked();
		final boolean visible = tvt.isVisible();
		final String name = tvt.getTierName();
		final String font = tvt.getFont();
		return factory.createTierViewItem(name, visible, font, locked);
	}
	
	private MediaSegment copySegment(SessionFactory factory, SegmentType st) {
		final MediaSegment segment = factory.createMediaSegment();
		segment.setStartValue(Math.abs(st.getStartTime()));
		segment.setEndValue(Math.abs(st.getStartTime()) + Math.abs(st.getDuration()));
		segment.setUnitType(MediaUnit.Millisecond);
		return segment;
	}

	// copy comment data
	private Comment copyComment(SessionFactory factory, CommentType ct) {
		final String tag = ct.getType();
		ca.phon.session.CommentType type = ca.phon.session.CommentType.fromString(tag);
		if(type == null) {
			type = ca.phon.session.CommentType.Generic;
		}
		final StringBuffer buffer = new StringBuffer();
		for(Object obj:ct.getContent()) {
			buffer.append(obj.toString());
		}
		try {
			final TierData tierData = TierData.parseTierData(buffer.toString());
			return factory.createComment(type, tierData);
		} catch (ParseException e) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, e.getLocalizedMessage(), e);
			return factory.createComment(type, new TierData());
		}
	}

	Record copyRecord(SessionFactory factory, Session session, RecordType rt) {
		final Record retVal = factory.createRecord(session);

//		try {
//			if(rt.getId() != null) {
//				UUID uuid = UUID.fromString(rt.getId());
//				retVal.setUuid(uuid);
//			}
//		} catch (IllegalArgumentException e) {
//		}

		if(rt.getSpeaker() != null) {
			final ParticipantType pt = (ParticipantType)rt.getSpeaker();
			for(int pIdx = 0; pIdx < session.getParticipantCount(); pIdx++) {
				final Participant participant = session.getParticipant(pIdx);
				if(participant.getName() != null  && participant.getName().equals(pt.getName())) {
					retVal.setSpeaker(participant);
					break;
				} else if(participant.getId() != null && participant.getId().equals(pt.getId())) {
					retVal.setSpeaker(participant);
					break;
				}
			}
		} else {
			retVal.setSpeaker(Participant.UNKNOWN);
		}

		// orthography
		final OrthographyType ot = rt.getOrthography();
		final List<Orthography> orthoGroups = copyOrthography(factory, ot);

		final Map<SystemTierType, List<IPATranscript>> ipaGroupMap = new LinkedHashMap<>();
		// ipa target/actual
		for(IpaTierType ipaTt:rt.getIpaTier()) {
			if(rt.getId().equals("c85835e4-6a11-4bc1-a72d-d3bb6a90d4e0")) {
				System.out.println("here");
			}
			final SystemTierType tierType =
					(ipaTt.getForm() == PhoTypeType.MODEL ? SystemTierType.IPATarget : SystemTierType.IPAActual);
			final List<IPATranscript> ipaGroups = copyTranscript(factory, ipaTt);
			ipaGroupMap.put(tierType, ipaGroups);
//			if(ipaTt.getForm() == PhoTypeType.ACTUAL) {
//				retVal.setIPAActual(ipaTranscript);
//			} else {
//				retVal.setIPATarget(ipaTranscript);
//			}
		}

		// build orthography and ipa data from groups
		final OrthographyBuilder orthoBuilder = new OrthographyBuilder();
		final IPATranscriptBuilder ipaTBuilder = new IPATranscriptBuilder();
		final IPATranscriptBuilder ipaABuilder = new IPATranscriptBuilder();

		if(isFixAlignment()) {
			final int numGroups = orthoGroups.size();
			final List<IPATranscript> ipaTList = ipaGroupMap.containsKey(SystemTierType.IPATarget)
					? ipaGroupMap.get(SystemTierType.IPATarget) : new ArrayList<>();
			final List<IPATranscript> ipaAList = ipaGroupMap.containsKey(SystemTierType.IPAActual)
					? ipaGroupMap.get(SystemTierType.IPAActual) : new ArrayList<>();
			for(int i = 0; i < numGroups; i++) {
				final Orthography ortho = orthoGroups.get(i);
				final IPATranscript ipaT = i < ipaTList.size() ? ipaTList.get(i) : new IPATranscript();
				final IPATranscript ipaA = i < ipaAList.size() ? ipaAList.get(i) : new IPATranscript();

				final OrthoWordExtractor wordExtractor = new OrthoWordExtractor(false);
				ortho.accept(wordExtractor);
				// basic case - add data directly to record
				if(wordExtractor.getWordList().size() == ipaT.words().size()
						&& ipaT.words().size() == ipaA.words().size()) {
					orthoBuilder.append(ortho);
					if(ipaTBuilder.size() > 0) ipaTBuilder.appendWordBoundary();
					ipaTBuilder.append(ipaT);
					if(ipaABuilder.size() > 0) ipaABuilder.appendWordBoundary();
					ipaABuilder.append(ipaA);
				} else {
					if(wordExtractor.getWordList().isEmpty()) {
						orthoBuilder.append("yyy");
					} else if(wordExtractor.getWordList().size() == 1) {
						orthoBuilder.append(ortho);
					} else {
						orthoBuilder.append(PhoneticGroup.PHONETIC_GROUP_START + ortho + PhoneticGroup.PHONETIC_GROUP_END);
					}

					if(ipaT.words().isEmpty()) {
						if(!ipaTBuilder.toString().isEmpty()) ipaTBuilder.appendWordBoundary();
						ipaTBuilder.append("*");
					} else if(ipaT.words().size() == 1) {
						if(!ipaTBuilder.toString().isEmpty()) ipaTBuilder.appendWordBoundary();
						ipaTBuilder.append(ipaT);
					} else {
						if(!ipaTBuilder.toString().isEmpty()) ipaTBuilder.appendWordBoundary();
						ipaTBuilder.append(PhoneticGroupMarkerType.BEGIN.getMakerChar() + ipaT.toString(true) + PhoneticGroupMarkerType.END.getMakerChar());
					}

					if(ipaA.words().isEmpty()) {
						if(!ipaABuilder.toString().isEmpty()) ipaABuilder.appendWordBoundary();
						ipaABuilder.append("*");
					} else if(ipaA.words().size() == 1) {
						if(!ipaABuilder.toString().isEmpty()) ipaABuilder.appendWordBoundary();
						ipaABuilder.append(ipaA);
					} else {
						if(!ipaABuilder.toString().isEmpty()) ipaABuilder.appendWordBoundary();
						ipaABuilder.append(PhoneticGroupMarkerType.BEGIN.getMakerChar() + ipaA.toString(true) + PhoneticGroupMarkerType.END.getMakerChar());
					}
				}
			}
		} else {
			boolean hasInvalidOrthoGroup = false;
			final OrthographyBuilder tempBuilder = new OrthographyBuilder();
			for(Orthography ortho:orthoGroups) {
				if (ortho.getExtension(UnvalidatedValue.class) != null) {
					hasInvalidOrthoGroup = true;
					break;
				}
				tempBuilder.append(ortho);
			}

			if(hasInvalidOrthoGroup) {
				StringBuilder sb = new StringBuilder();
				for (Orthography ortho : orthoGroups) {
					if (sb.length() > 0) sb.append(" ");
					if (ortho.getExtension(UnvalidatedValue.class) != null) {
						sb.append(ortho.getExtension(UnvalidatedValue.class).getValue());
					} else {
						sb.append(ortho.toString());
					}
				}
				try {
					orthoBuilder.append(sb.toString());
				} catch (IllegalArgumentException e) {
					final ParseException pe = (ParseException) e.getCause();
					final UnvalidatedValue uv = new UnvalidatedValue(sb.toString(), pe);
					orthoBuilder.putExtension(UnvalidatedValue.class, uv);
				}
			} else {
				// add items in reverse order, keeping track of the first terminator
				// any other terminators will be converted into freecodes
				Terminator firstTerminator = null;
				for(int i = tempBuilder.size()-1; i >= 0; i--) {
					final OrthographyElement ele = tempBuilder.elementAt(i);
					if(ele instanceof Terminator t) {
						if(firstTerminator == null) {
							firstTerminator = t;
							orthoBuilder.append(t);
						} else {
							final String text = t.text();
							final Freecode freecode = new Freecode(text);
							orthoBuilder.append(freecode);
						}
					} else {
						orthoBuilder.append(ele);
					}
				}
				orthoBuilder.reverse();
			}
			for(SystemTierType ipaTierType:ipaGroupMap.keySet()) {
				final IPATranscriptBuilder builder = ipaTierType == SystemTierType.IPATarget ? ipaTBuilder : ipaABuilder;
				final List<IPATranscript> ipas = ipaGroupMap.get(ipaTierType);
				for(IPATranscript ipa:ipas) {
					if(!builder.toString().isEmpty()) builder.appendWordBoundary();
					builder.append(ipa);
				}
			}
		}

		if(rt.isExcludeFromSearches()) {
			if(!orthoBuilder.toOrthography().hasTerminator())
				orthoBuilder.append(new Terminator(TerminatorType.PERIOD));
			orthoBuilder.append(new Postcode(Record.RECORD_XCL_POSTCODE));
		}
		final Orthography finalOrtho = orthoBuilder.toOrthography();
		if(orthoBuilder.getExtension(UnvalidatedValue.class) != null) {
			finalOrtho.putExtension(UnvalidatedValue.class, orthoBuilder.getExtension(UnvalidatedValue.class));
		}
		retVal.setOrthography(finalOrtho);
		retVal.setIPATarget(ipaTBuilder.toIPATranscript());
		retVal.setIPAActual(ipaABuilder.toIPATranscript());

		// blind transcriptions
		for(BlindTierType btt:rt.getBlindTranscription()) {
			// get the correct ipa object from our new record
			final Tier<IPATranscript> ipaTier =
					(btt.getForm() == PhoTypeType.MODEL ? retVal.getIPATargetTier() : retVal.getIPAActualTier());
			for(BgType bgt:btt.getBg()) {
				final StringBuffer buffer = new StringBuffer();
				for(WordType wt:bgt.getW()) {
					if(buffer.length() > 0)
						buffer.append(" ");
					buffer.append(wt.getContent());
				}

				try {
					final IPATranscript blindTranscript =
							IPATranscript.parseIPATranscript(buffer.toString());
					final TranscriberType tt = (TranscriberType)btt.getUser();
					final String name = tt.getId();
					ipaTier.setBlindTranscription(name, blindTranscript);
				} catch (ParseException e) {
					Logger.getLogger(getClass().getName()).log(Level.WARNING, e.getLocalizedMessage(), e);
				}
			}
		}

		// notes
		if(rt.getNotes() != null) {
			final Tier<TierData> commentsTier = factory.createTier(ca.phon.session.UserTierType.Comments.getPhonTierName(),
					TierData.class, new HashMap<>(), true, false);
			try {
				final TierData notesData = TierData.parseTierData(rt.getNotes().getContent());
				commentsTier.setValue(notesData);
			} catch (ParseException pe) {
				final TierData tierData = new TierData();
				tierData.putExtension(UnvalidatedValue.class, new UnvalidatedValue(rt.getNotes().getContent(), pe));
				commentsTier.setValue(tierData);
			}
			retVal.putTier(commentsTier);
		}

		// segment
		if(rt.getSegment() != null) {
			final MediaSegment segment = copySegment(factory, rt.getSegment());
			retVal.setMediaSegment(segment);
		} else {
			retVal.setMediaSegment(factory.createMediaSegment());
		}

		// alignment
		for(AlignmentTierType att:rt.getAlignment()) {
			final PhoneAlignment alignment = copyAlignment(factory, retVal, att);
			retVal.setPhoneAlignment(alignment);
			break; // only processing the first alignment element (which should be the only one)
		}

		// user tiers
		for(FlatTierType ftt:rt.getFlatTier()) {
			if("Postcode".equals(ftt.getTierName())) {
				final String pc = ftt.getContent();
				if(pc != null && !pc.isBlank()) {
					final Orthography ortho = retVal.getOrthography();
					final OrthographyBuilder builder = new OrthographyBuilder();
					builder.append(ortho);
					if (!ortho.hasTerminator()) {
						builder.append(new Terminator(TerminatorType.PERIOD));
					}
					builder.append(new Postcode(ftt.getContent()));
					retVal.setOrthography(builder.toOrthography());
				}
			} else {
				final TierDescription td = session.getUserTier(ftt.getTierName());
				if (td == null) {
					throw new IllegalStateException("User tier not found in session " + ftt.getTierName());
				}
				final Tier<?> userTier = factory.createTier(td);
				userTier.setText(ftt.getContent());
				retVal.putTier(userTier);
			}
		}

		for(GroupTierType gtt:rt.getGroupTier()) {
			TierDescription td = session.getUserTier(gtt.getTierName());
			if("morphology".equalsIgnoreCase(gtt.getTierName())) {
				td = session.getUserTier("mor");
			} else if("grasp".equalsIgnoreCase(gtt.getTierName())) {
				td = session.getUserTier("gra");
			}
			if(td == null) {
				//LOGGER.warning("User tier " + gtt.getTierName() + " not in session tier list");
				continue;
			}
			final Tier<?> userTier = factory.createTier(td);
			final StringBuffer buffer = new StringBuffer();
			for(TgType tgt:gtt.getTg()) {
				for(WordType wt:tgt.getW()) {
					if(buffer.length() > 0)
						buffer.append(" ");
					buffer.append(wt.getContent());
				}
			}
			userTier.setText(buffer.toString());
			retVal.putTier(userTier);
		}

		return retVal;
	}

	/**
	 * Copy orthography
	 *
	 * @param factory
	 * @param ot
	 * @return orthography
	 */
	private List<Orthography> copyOrthography(SessionFactory factory, OrthographyType ot) {
		final List<Orthography> retVal = new ArrayList<>();
		for(int i = 0 ;i < ot.getWOrGOrP().size(); i++) {
			final OrthographyBuilder builder = new OrthographyBuilder();
			final Object otEle = ot.getWOrGOrP().get(i);
			if(!(otEle instanceof GroupType gt)) continue;
			for(Object ele:gt.getWOrComOrE()) {
				if(ele instanceof WordType) {
					final WordType wt = (WordType)ele;
					String wordContent = wt.getContent();
					// fix shortenings
					wordContent = wordContent.replaceAll("\\<", "\\(");
					wordContent = wordContent.replaceAll("\\>", "\\)");

					// fix fragments
					if(wordContent.matches("^&[^&]+")) {
						wordContent = "&~" + wordContent.substring(1, wordContent.length()-1);
					}

					// fix &=action: to 0
					if(wordContent.matches("&=action:")) {
						wordContent = "0";
					}

					builder.append(new ca.phon.orthography.Word(new WordText(wordContent)));
				} else if(ele instanceof EventType) {
					final EventType et = (EventType) ele;
					final String txt = et.getContent();
					builder.append(new Happening(txt));
				} else if(ele instanceof CommentType) {
					final CommentType ct = (CommentType) ele;
					final String tag = ct.getType();
					final StringBuilder sb = new StringBuilder();
					ct.getContent().forEach(sb::append);
					final String txt = sb.toString();

					if(tag == null || tag.length() == 0) {
						final LinkerType lt = LinkerType.fromString(txt);
						if (lt != null) {
							builder.append(new Linker(lt));
							continue;
						}

						final MarkerType mt = MarkerType.fromString(txt);
						if (mt != null) {
							builder.append(new Marker(mt));
							continue;
						}

						final TagMarkerType tagMarkerType = TagMarkerType.fromString(txt);
						if (tagMarkerType != null) {
							builder.append(new TagMarker(tagMarkerType));
							continue;
						}

						if(txt.matches("\\.{1,3}")) {
							// add pause
							final PauseLength pauseLength = switch(txt) {
								case "." -> PauseLength.SIMPLE;
								case ".." -> PauseLength.LONG;
								case "..." -> PauseLength.VERY_LONG;
								default -> PauseLength.SIMPLE;
							};
							builder.append(new Pause(pauseLength));
							continue;
						}

						if ("^c".equals(txt)) {
							builder.append(new Separator(SeparatorType.CLAUSE_DELIMITER));
							continue;
						}

						final String overlapPtRegex = "([⌈⌉⌊⌋])([0-9]+)?";
						if (txt.matches(overlapPtRegex)) {
							final Pattern pattern = Pattern.compile(overlapPtRegex);
							final Matcher matcher = pattern.matcher(txt);
							if (matcher.matches()) {
								final String type = matcher.group(1);
								final OverlapPointType overlapPointType = switch (type) {
									case "⌈" -> OverlapPointType.TOP_START;
									case "⌉" -> OverlapPointType.TOP_END;
									case "⌊" -> OverlapPointType.BOTTOM_START;
									case "⌋" -> OverlapPointType.BOTTOM_END;
									// should never happen
									default -> null;
								};
								final int index = (matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : -1);
								builder.append(new OverlapPoint(overlapPointType, index));
							}
							continue;
						}

						//  overlap
						if (txt.matches("[<>][0-9]*")) {
							final OverlapType type = (txt.charAt(0) == '<' ? OverlapType.OVERLAP_PRECEDES : OverlapType.OVERLAP_FOLLOWS);
							final String remainder = (txt.length() > 1 ? txt.substring(1) : null);
							int index = (remainder == null ? -1 : Integer.parseInt(remainder));
							builder.append(new Overlap(type, index));
							continue;
						}

						// replacement
						if (txt.matches("::? .+")) {
							final String type = txt.substring(0, txt.indexOf(' '));
							final String data = txt.substring(txt.indexOf(' ') + 1);
							final boolean real = type.length() == 2;
							// wrap everything in a single WordText object, the parser will fix at the end
							builder.append(new Replacement(real, new Word(new WordText(data))));
							continue;
						}

						// duration
						final String durationRegex = "# ([0-9.]+)(m?s)?";
						if (txt.matches(durationRegex)) {
							final Pattern pattern = Pattern.compile(durationRegex);
							final Matcher matcher = pattern.matcher(txt);
							if (matcher.matches()) {
								Float length = Float.parseFloat(matcher.group(1));
								final String unit = matcher.group(2);
								if (unit != null && "ms".equals(unit)) {
									length *= 1000.0f;
								}
								builder.append(new ca.phon.orthography.Duration(length));
							}
							continue;
						}

						if (txt.startsWith("^ ")) {
							builder.append(new Freecode(txt.substring(2)));
							continue;
						} else if (txt.startsWith("=")) {
							builder.append(new GroupAnnotation(GroupAnnotationType.EXPLANATION, txt.substring(1)));
							continue;
						} else if (txt.startsWith("=!")) {
							builder.append(new GroupAnnotation(GroupAnnotationType.PARALINGUISTICS, txt.substring(2)));
							continue;
						} else if (txt.startsWith("=?")) {
							builder.append(new GroupAnnotation(GroupAnnotationType.ALTERNATIVE, txt.substring(2)));
							continue;
						}

						// finally add as comments
						builder.append(new GroupAnnotation(GroupAnnotationType.COMMENTS, txt));
					} else if("happening".equals(tag)) {
						builder.append(new Happening(txt));
					} else if("error".equals(tag)) {
						if(txt.equalsIgnoreCase("error:")) {
							builder.append(new Error(""));
						} else {
							builder.append(new Error(txt));
						}
					} else if("long-feature-begin".equals(tag)) {
						builder.append(new LongFeature(BeginEnd.BEGIN, txt));
					} else if("long-feature-end".equals(tag)) {
						builder.append(new LongFeature(BeginEnd.END, txt));
					} else if("nonvocal".equals(tag)) {
						builder.append(new Nonvocal(BeginEndSimple.SIMPLE, txt));
					} else if("nonvocal-begin".equals(tag)) {
						builder.append(new Nonvocal(BeginEndSimple.BEGIN, txt));
					} else if("nonvocal-end".equals(tag)) {
						builder.append(new Nonvocal(BeginEndSimple.END, txt));
					} else if("action".equals(tag)) {
						builder.append(new Action());
					} else if("freecode".equals(tag)) {
						builder.append(new Freecode(txt));
					} else {
						builder.append(new Freecode(txt));
					}
				} else if(ele instanceof InnerGroupMarker) {
					// not used
				} else if(ele instanceof PunctuationType) {
					final PunctuationType pt = (PunctuationType) ele;
					final TerminatorType tt = TerminatorType.fromString(pt.getType());
					if(tt != null) {
						builder.append(new Terminator(tt));
					} else if("OPEN_BRACE".equals(pt.getType())) {
						// ignore
					} else if("CLOSE_BRACE".equals(pt.getType())) {
						// ignore
					}
				}
			}
			final String orthoTxt = builder.toOrthography().toString();
			try {
				Orthography ortho = Orthography.parseOrthography(orthoTxt);
				retVal.add(ortho);
			} catch (ParseException pe) {
				final Orthography ortho = new Orthography();
				ortho.putExtension(UnvalidatedValue.class, new UnvalidatedValue(orthoTxt, pe));
				retVal.add(ortho);
			}
		}
		return retVal;
	}

	/**
	 * Copy ipa data.
	 *
	 * Attempts to copy phone objects as-is, if not possible
	 * the transcription is re-parsed.
	 *
	 * @param factory
	 * @param itt
	 */
	private List<IPATranscript> copyTranscript(SessionFactory factory, IpaTierType itt) {
		final List<IPATranscript> retVal = new ArrayList<>();
		for(PhoType pt:itt.getPg()) {
			final IPATranscriptBuilder groupBuilder = new IPATranscriptBuilder();
			if(pt != null && pt.getW() != null) {
				for(WordType wt:pt.getW()) {
					if(wt.getContent().trim().length() > 0) {
						if ((groupBuilder.size() > 0 || groupBuilder.getUnvalidatedValue() != null) && wt.getContent().trim().length() > 0)
							groupBuilder.appendWordBoundary();
						groupBuilder.append(wt.getContent().trim());
					}
				}
			}
			final IPATranscript groupTranscript = groupBuilder.toIPATranscript();
			// only assign syllabification if possible
			if(pt.getSb() != null && groupTranscript.length() == pt.getSb().getPh().size()) {
				for(int i = 0; i < pt.getSb().getPh().size(); i++) {
					var ph = pt.getSb().getPh().get(i);
					final SyllableConstituentType scType = switch (ph.getScType()) {
						case C -> SyllableConstituentType.CODA;
						case N -> SyllableConstituentType.NUCLEUS;
						case AS -> SyllableConstituentType.AMBISYLLABIC;
						case O -> SyllableConstituentType.ONSET;
						case LA -> SyllableConstituentType.LEFTAPPENDIX;
						case RA -> SyllableConstituentType.RIGHTAPPENDIX;
						case OEHS -> SyllableConstituentType.OEHS;
						case SB -> SyllableConstituentType.SYLLABLEBOUNDARYMARKER;
						case SS -> SyllableConstituentType.SYLLABLESTRESSMARKER;
						case WB -> SyllableConstituentType.WORDBOUNDARYMARKER;
						case UK -> SyllableConstituentType.UNKNOWN;
					};
					final IPAElement element = groupTranscript.elementAt(i);
					final SyllabificationInfo info = element.getExtension(SyllabificationInfo.class);
					info.setConstituentType(scType);
					if(scType == SyllableConstituentType.NUCLEUS) {
						info.setDiphthongMember(!ph.isHiatus());
					}
				}
			}
			retVal.add(groupTranscript);
		}
		return retVal;
	}

	public class CopyTranscriptVisitor extends VisitorAdapter<IPAElement> {

		int eleIdx = 0;

		final List<ConstituentType> syllabification;

		public CopyTranscriptVisitor(List<ConstituentType> syllabification) {
			super();
			this.syllabification = syllabification;
		}

		@Override
		public void fallbackVisit(IPAElement obj) {
			eleIdx++;
		}

		@Visits
		public void visitPhone(Phone phone) {
			final ConstituentType ct =
					(eleIdx < syllabification.size() ? syllabification.get(eleIdx++) : null);
			if(ct != null) {
				final ConstituentTypeType ctt = ct.getScType();
				final SyllableConstituentType scType = SyllableConstituentType.fromString(ctt.toString());
				final SyllabificationInfo info = phone.getExtension(SyllabificationInfo.class);
				if(scType != null) {
					info.setConstituentType(scType);
					if(scType == SyllableConstituentType.NUCLEUS) {
						info.setDiphthongMember(!ct.isHiatus());
					}
					phone.putExtension(SyllabificationInfo.class, info);
				}
			}
		}

		@Visits
		public void visitCompoundPhone(CompoundPhone cp) {
			final ConstituentType ct =
					(eleIdx < syllabification.size() ? syllabification.get(eleIdx++) : null);
			if(ct != null) {
				final ConstituentTypeType ctt = ct.getScType();
				final SyllabificationInfo info = cp.getExtension(SyllabificationInfo.class);
				final SyllableConstituentType scType = SyllableConstituentType.fromString(ctt.toString());
				if(scType != null) {
					info.setConstituentType(scType);
					if(scType == SyllableConstituentType.NUCLEUS) {
						info.setDiphthongMember(!ct.isHiatus());
					}
				}
			}
		}

	}

	/**
	 * Copy alignment data
	 */
	private PhoneAlignment copyAlignment(SessionFactory factory, Record record, AlignmentTierType att) {
		final IPATranscript ipaT = record.getIPATargetTier().hasValue() ? record.getIPATarget() : new IPATranscript();
		final IPATranscript ipaA = record.getIPAActualTier().hasValue() ? record.getIPAActual() : new IPATranscript();
		int alignLength = 0;
		for(AlignmentType at:att.getAg()) {
			alignLength += at.getLength();
		}
		int alignIdx = 0;
		final PhoneMap pm = new PhoneMap(ipaT, ipaA);
		final Integer[][] alignmentData = new Integer[2][];
		alignmentData[0] = new Integer[alignLength];
		alignmentData[1] = new Integer[alignLength];
		int targetOffset = 0;
		int actualOffset = 0;
		for(AlignmentType at:att.getAg()) {
			int maxTarget = targetOffset;
			int maxActual = actualOffset;
			for(int i = 0; i < at.getPhomap().size(); i++) {
				final MappingType mt = at.getPhomap().get(i);
				alignmentData[0][alignIdx] =
						(mt.getValue().size() > 0 ? mt.getValue().get(0) >= 0 ? mt.getValue().get(0) + targetOffset : -1 : null);
				if(alignmentData[0][alignIdx] != null)
					maxTarget = Math.max(maxTarget, alignmentData[0][alignIdx]);
				alignmentData[1][alignIdx] =
						(mt.getValue().size() > 1 ? mt.getValue().get(1) >= 0 ? mt.getValue().get(1) + actualOffset : -1 :  null);
				if(alignmentData[1][alignIdx] != null)
					maxActual = Math.max(maxActual, alignmentData[1][alignIdx]);
				++alignIdx;
			}
			targetOffset = maxTarget + 1;
			actualOffset = maxActual + 1;
		}
		pm.setTopAlignment(alignmentData[0]);
		pm.setBottomAlignment(alignmentData[1]);

		// split alignment by word pairing
		final List<IPATranscript> targetWords = ipaT.words();
		final List<IPATranscript> actualWords = ipaA.words();
		final int numAlignments = Math.max(targetWords.size(), actualWords.size());
		final List<PhoneMap> alignments = new ArrayList<>();
		for(int i = 0; i < numAlignments; i++) {
			final IPATranscript tw = i < targetWords.size() ? targetWords.get(i) : new IPATranscript();
			final IPATranscript aw = i < actualWords.size() ? actualWords.get(i) : new IPATranscript();
			final PhoneMap subAlignment = pm.getSubAlignment(tw, aw);
			alignments.add(subAlignment);
		}

		return new PhoneAlignment(alignments);
	}

	/**
	 * Get an dom version of the xml stream
	 *
	 * @param stream
	 * @return dom document
	 */
	private Document documentFromStream(InputStream stream)
		throws IOException {
		Document retVal = null;

		try {
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			final DocumentBuilder builder = factory.newDocumentBuilder();
			retVal = builder.parse(stream);
		} catch (ParserConfigurationException e) {
			throw new IOException(e);
		} catch (SAXException e) {
			throw new IOException(e);
		}

		return retVal;
	}

	@Override
	public Session readSession(InputStream stream) throws IOException {
		final Document doc = documentFromStream(stream);
		return read(doc, doc.getDocumentElement());
	}

	@Override
	public boolean canRead(File file) throws IOException {
		// open file and make sure the first
		// element is 'session' with the correct version
		boolean canRead = false;

		// use StAX to read only first element
		// create StAX reader
		XMLInputFactory factory = XMLInputFactory.newInstance();
		XMLEventReader reader = null;
		try(FileInputStream source = new FileInputStream(file)) {
			//BufferedReader in = new BufferedReader(new InputStreamReader(source, "UTF-8"));
			XMLEventReader xmlReader = factory.createXMLEventReader(source, "UTF-8");
			reader = factory.createFilteredReader(xmlReader, new XMLWhitespaceFilter());

			XMLEvent evt;
			while(!(evt = reader.nextEvent()).isStartElement());
			canRead =
					evt.asStartElement().getName().getLocalPart().equals("session")
					&& evt.asStartElement().getAttributeByName(new QName("version")).getValue().equals("PB1.2");
		} catch (XMLStreamException e) {
			throw new IOException(e);
		}

		return canRead;
	}

	private class XMLWhitespaceFilter implements EventFilter {

		@Override
		public boolean accept(XMLEvent arg0) {
			boolean retVal = true;


			if(arg0.isCharacters() &&
					StringUtils.strip(arg0.asCharacters().getData()).length() == 0) {

				retVal = false;
			}

			return retVal;
		}

	}

	@Override
	public Class<?> getExtensionType() {
		return SessionReader.class;
	}

	@Override
	public IPluginExtensionFactory<SessionReader> getFactory() {
		return (args) -> { return new XmlSessionReaderV1_2(); };
	}

}
