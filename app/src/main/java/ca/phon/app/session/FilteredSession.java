package ca.phon.app.session;

import ca.phon.session.*;
import ca.phon.session.alignment.TierAlignmentRules;
import ca.phon.session.filter.RecordFilter;
import ca.phon.session.impl.SessionImpl;
import ca.phon.session.spi.SessionSPI;
import ca.phon.util.Language;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class FilteredSession implements SessionSPI {

    // delegate session
    private final Session session;

    private final RecordFilter filter;

    public FilteredSession(Session session, RecordFilter filter) {
        this.session = session;
        this.filter = filter;
    }

    // add delegate methods for Session
    @Override
    public String getName() {
        return session.getName();
    }

    @Override
    public LocalDate getDate() {
        return session.getDate();
    }

    @Override
    public List<Language> getLanguages() {
        return session.getLanguages();
    }

    @Override
    public void setLanguages(List<Language> languages) {
        session.setLanguages(languages);
    }

    @Override
    public String getMediaLocation() {
        return session.getMediaLocation();
    }

    @Override
    public List<TierViewItem> getTierView() {
        return session.getTierView();
    }

    @Override
    public void setTierView(List<TierViewItem> view) {
        session.setTierView(view);
    }

    @Override
    public List<TierDescription> getTiers() {
        return session.getTiers();
    }

    @Override
    public Map<String, String> getSystemTierParameters(SystemTierType systemTier) {
        return session.getSystemTierParameters(systemTier);
    }

    @Override
    public void putSystemTierParam(SystemTierType systemTier, String key, String value) {
        session.putSystemTierParam(systemTier, key, value);
    }

    @Override
    public int getUserTierCount() {
        return session.getUserTierCount();
    }

    @Override
    public TierDescription getUserTier(int idx) {
        return session.getUserTier(idx);
    }

    @Override
    public TierDescription removeUserTier(int idx) {
        return session.removeUserTier(idx);
    }

    @Override
    public TierDescription removeUserTier(TierDescription tierDescription) {
        return session.removeUserTier(tierDescription);
    }

    @Override
    public void addUserTier(TierDescription tierDescription) {
        session.addUserTier(tierDescription);
    }

    @Override
    public void addUserTier(int idx, TierDescription tierDescription) {
        session.addUserTier(idx, tierDescription);
    }

    @Override
    public List<TierAlignmentRules> getTierAlignmentRules() {
        return session.getTierAlignmentRules();
    }

    @Override
    public TierAlignmentRules getTierAlignmentRules(String tier1, String tier2) {
        return session.getTierAlignmentRules(tier1, tier2);
    }

    @Override
    public void putTierAlignmentRules(TierAlignmentRules tierAlignmentRules) {
        session.putTierAlignmentRules(tierAlignmentRules);
    }

    @Override
    public void deleteTierAlignmentRules(String tier1, String tier2) {
        session.deleteTierAlignmentRules(tier1, tier2);
    }

    @Override
    public List<String> getBlindTiers() {
        return session.getBlindTiers();
    }

    @Override
    public void setBlindTiers(List<String> blindTiers) {
        session.setBlindTiers(blindTiers);
    }

    @Override
    public int getTranscriberCount() {
        return 0;
    }

    @Override
    public Transcriber getTranscriber(String username) {
        return null;
    }

    @Override
    public Transcriber getTranscriber(int i) {
        return null;
    }

    @Override
    public void removeTranscriber(int i) {

    }

    @Override
    public Map<String, String> getMetadata() {
        return Map.of();
    }

    @Override
    public int getParticipantCount() {
        return 0;
    }

    @Override
    public void addParticipant(Participant participant) {

    }

    @Override
    public void addParticipant(int idx, Participant participant) {

    }

    @Override
    public Participant getParticipant(int idx) {
        return null;
    }

    @Override
    public int getParticipantIndex(Participant participant) {
        return 0;
    }

    @Override
    public void setName(String name) {
        session.setName(name);
    }

    @Override
    public void setDate(LocalDate date) {

    }

    @Override
    public void setMediaLocation(String mediaLocation) {

    }

    @Override
    public void addTranscriber(Transcriber t) {

    }

    @Override
    public void removeTranscriber(Transcriber t) {

    }

    @Override
    public void removeTranscriber(String username) {

    }

    @Override
    public void removeParticipant(Participant participant) {

    }

    @Override
    public void removeParticipant(int idx) {

    }

    @Override
    public Transcript getTranscript() {
        return null;
    }

    @Override
    public String getCorpus() {
        return session.getCorpus();
    }

    @Override
    public void setCorpus(String corpus) {
        session.setCorpus(corpus);
    }

    public RecordFilter getFilter() {
        return filter;
    }

    public Session getDelegate() {
        return session;
    }

}
