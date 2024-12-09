package ca.phon.app.session.editor.view.transcript.extensions;

import ca.phon.app.log.LogUtil;
import ca.phon.app.session.editor.EditorEvent;
import ca.phon.app.session.editor.EditorEventManager;
import ca.phon.app.session.editor.EditorEventType;
import ca.phon.app.session.editor.undo.MediaLocationEdit;
import ca.phon.app.session.editor.undo.SessionDateEdit;
import ca.phon.app.session.editor.undo.SessionLanguageEdit;
import ca.phon.app.session.editor.view.transcript.*;
import ca.phon.extensions.UnvalidatedValue;
import ca.phon.formatter.Formatter;
import ca.phon.media.MediaLocator;
import ca.phon.session.*;
import ca.phon.session.position.TranscriptElementLocation;
import ca.phon.session.tierdata.TierData;
import ca.phon.ui.CalloutWindow;
import ca.phon.ui.CommonModuleFrame;
import ca.phon.ui.action.PhonUIAction;
import ca.phon.ui.menu.MenuBuilder;
import ca.phon.ui.nativedialogs.FileFilter;
import ca.phon.ui.nativedialogs.NativeDialogs;
import ca.phon.ui.nativedialogs.OpenDialogProperties;
import ca.phon.util.Language;
import ca.phon.util.LanguageEntry;
import ca.phon.util.LanguageParser;
import ca.phon.util.PrefHelper;
import org.jdesktop.swingx.JXMonthView;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An extension that provides header tier support to the {@link TranscriptEditor}
 * */
public class HeaderTierExtension extends DefaultInsertionHook implements TranscriptEditorExtension {
    private TranscriptEditor editor;
    private TranscriptDocument doc;
    private Session session;

    /* Document property stuff */

    public static final String HEADERS_VISIBLE = "isHeadersVisible";
    public static final boolean DEFAULT_HEADERS_VISIBLE = true;

    /* State */

    private final Map<String, Tier<?>> headerTierMap = new HashMap<>();

    private final Map<Tier<?>, Object> errorUnderlineHighlights = new HashMap<>();


    @Override
    public void install(TranscriptEditor editor) {
        this.editor = editor;
        this.doc = editor.getTranscriptDocument();
        this.session = editor.getSession();

        headerTierMap.put("date", doc.getSessionFactory().createTier("Date", LocalDate.class));
        headerTierMap.put("tiers", doc.getSessionFactory().createTier("Tiers", TierData.class));
        headerTierMap.put("participants", doc.getSessionFactory().createTier("Participants", TierData.class));
        headerTierMap.put("languages", doc.getSessionFactory().createTier("Languages", TranscriptDocument.Languages.class));
        headerTierMap.put("media", doc.getSessionFactory().createTier("Media", TierData.class));

        doc.addInsertionHook(this);
        doc.addDocumentPropertyChangeListener(HEADERS_VISIBLE, evt -> doc.reload());

        editor.getEventManager().registerActionForEvent(EditorEventType.TierViewChanged, this::updateTiersHeader, EditorEventManager.RunOn.AWTEventDispatchThread);
        editor.getEventManager().registerActionForEvent(EditorEventType.SessionDateChanged, this::updateDateHeader, EditorEventManager.RunOn.AWTEventDispatchThread);
        editor.getEventManager().registerActionForEvent(TranscriptEditor.transcriptLocationChanged, this::onSessionLocationChanged, EditorEventManager.RunOn.AWTEventDispatchThread);
        editor.getEventManager().registerActionForEvent(EditorEventType.SessionMediaChanged, this::onSessionMediaChanged, EditorEventManager.RunOn.AWTEventDispatchThread);
        editor.getEventManager().registerActionForEvent(EditorEventType.SessionLangChanged, this::updateLanguagesHeader, EditorEventManager.RunOn.AWTEventDispatchThread);
        editor.getEventManager().registerActionForEvent(EditorEventType.ParticipantAdded, this::updateParticipantsHeader, EditorEventManager.RunOn.AWTEventDispatchThread);
        editor.getEventManager().registerActionForEvent(EditorEventType.ParticipantRemoved, this::updateParticipantsHeader, EditorEventManager.RunOn.AWTEventDispatchThread);
        editor.getEventManager().registerActionForEvent(EditorEventType.ParticipantChanged, this::updateParticipantsHeader, EditorEventManager.RunOn.AWTEventDispatchThread);
    }

    @Override
    public List<DefaultStyledDocument.ElementSpec> startSession() {
        TranscriptBatchBuilder batchBuilder = new TranscriptBatchBuilder(doc.getTranscriptStyleContext(), doc.getInsertionHooks());

        if (isHeadersVisible()) {
            // Add date line if present
            appendDateHeader(batchBuilder);
            batchBuilder.appendEOL();

            // Add media line if present
            appendMediaHeader(batchBuilder);
            batchBuilder.appendEOL();

            // Add languages line if present
            appendLanguagesHeader(batchBuilder);
            batchBuilder.appendEOL();

            // Add Participants header
            appendParticipantsHeader(batchBuilder);
            batchBuilder.appendEOL();

            // Add Tiers header
            appendTiersHeader(batchBuilder);
            batchBuilder.appendEOL();
        }

        return batchBuilder.getBatch();
    }

    private boolean isHeadersVisible() {
        return (boolean) doc.getDocumentPropertyOrDefault(HEADERS_VISIBLE, DEFAULT_HEADERS_VISIBLE);
    }

    private void updateParticipantsHeader(EditorEvent<Participant> participantEditorEvent) {
        try {
            Tier<TierData> participantsTier = (Tier<TierData>) headerTierMap.get("participants");
            Participants participants = session.getParticipants();
            StringJoiner participantsJoiner = new StringJoiner(", ");
            for (Participant participant : participants) {
                if (participant.getName() != null) {
                    participantsJoiner.add(participant.getName() + " (" + participant.getId() + ")");
                }
                else {
                    participantsJoiner.add(participant.getId());
                }
            }
            participantsTier.setText(participantsJoiner.toString());

            final TranscriptDocument.StartEnd startEnd = doc.getGenericContentStartEnd(participantsTier);
            if (startEnd.valid()) {
                doc.setBypassDocumentFilter(true);
                editor.getTranscriptEditorCaret().freeze();
                doc.remove(startEnd.start(), startEnd.length());

                final SimpleAttributeSet attrs = new SimpleAttributeSet(doc.getTranscriptStyleContext().getStyle(TranscriptStyleContext.DEFAULT_STYLE));
                TranscriptStyleConstants.setElementType(attrs, TranscriptStyleConstants.ELEMENT_TYPE_GENERIC);
                TranscriptStyleConstants.setGenericTier(attrs, participantsTier);

                TranscriptBatchBuilder batchBuilder = new TranscriptBatchBuilder(doc.getTranscriptStyleContext(), doc.getInsertionHooks());
                TranscriptStyleConstants.setNotEditable(attrs, true);
                batchBuilder.appendBatchString(participantsTier.toString(), attrs);
                doc.processBatchUpdates(startEnd.start(), batchBuilder.getBatch());
            }
        }
        catch (BadLocationException e) {
            LogUtil.severe(e);
        } finally {
            editor.getTranscriptEditorCaret().unfreeze();
        }
    }

    private void appendParticipantsHeader(TranscriptBatchBuilder batchBuilder) {
        Tier<TierData> participantsTier = (Tier<TierData>) headerTierMap.get("participants");
        Participants participants = session.getParticipants();
        StringJoiner participantsJoiner = new StringJoiner(", ");
        for (Participant participant : participants) {
            if (participant.getName() != null) {
                participantsJoiner.add(participant.getName() + " (" + participant.getId() + ")");
            }
            else {
                participantsJoiner.add(participant.getId());
            }
        }
        participantsTier.setText(participantsJoiner.toString());

        final SimpleAttributeSet attrs = new SimpleAttributeSet(doc.getTranscriptStyleContext().getStyle(TranscriptStyleContext.DEFAULT_STYLE));
        TranscriptStyleConstants.setElementType(attrs, TranscriptStyleConstants.ELEMENT_TYPE_GENERIC);
        TranscriptStyleConstants.setGenericTier(attrs, participantsTier);
        StyleConstants.setFontSize(attrs, StyleConstants.getFontSize(attrs));

        // start paragraph
        batchBuilder.appendBatchEndStart(batchBuilder.getTrailingAttributes(), attrs);

        // label
        final SimpleAttributeSet labelAttrs = new SimpleAttributeSet(attrs);
        labelAttrs.addAttributes(doc.getTranscriptStyleContext().getLabelAttributes());
        TranscriptStyleConstants.setClickHandler(labelAttrs, (e, tier) -> {
            showParticipantsMenu(e);
        });
        String labelText = batchBuilder.formatLabelText("Participants");
        TranscriptStyleConstants.setUnderlineOnHover(labelAttrs, true);
        batchBuilder.appendBatchString(labelText, labelAttrs);
        TranscriptStyleConstants.setUnderlineOnHover(labelAttrs, false);
        batchBuilder.appendBatchString(": ", labelAttrs);

        // value
        TranscriptStyleConstants.setNotEditable(attrs, true);
        batchBuilder.appendBatchString(participantsTier.toString(), attrs);
    }

    private void appendLanguagesHeader(TranscriptBatchBuilder batchBuilder) {
        Tier<TranscriptDocument.Languages> languagesTier = (Tier<TranscriptDocument.Languages>) headerTierMap.get("languages");
        languagesTier.setFormatter(new Formatter<>() {
            @Override
            public String format(TranscriptDocument.Languages obj) {
                return obj
                        .languageList()
                        .stream()
                        .map(Language::toString)
                        .collect(Collectors.joining(" "));
            }

            @Override
            public TranscriptDocument.Languages parse(String text) throws ParseException {
                List<Language> languageList = new ArrayList<>();

                String[] languageStrings = text.split(" ");
                int idx = 0;
                for (String languageString : languageStrings) {
                    try {
                        Language lang = Language.parseLanguage(languageString);

                        final LanguageEntry staticEntry = LanguageParser.getInstance().getEntryById(lang.getPrimaryLanguage().getId());
                        if(staticEntry == null) {
                            // unknown language
                            throw new IllegalArgumentException("Unknown language: " + lang.getPrimaryLanguage().getId());
                        }

                        languageList.add(lang);
                        idx += languageString.length() + 1;
                    } catch (IllegalArgumentException e) {
                       throw new ParseException(e.getMessage(), idx);
                    }
                }

                return new TranscriptDocument.Languages(languageList);
            }
        });
        final TranscriptDocument.Languages sessionLanguages = new TranscriptDocument.Languages(session.getLanguages());
        languagesTier.setValue(sessionLanguages);

        final SimpleAttributeSet attrs = new SimpleAttributeSet(doc.getTranscriptStyleContext().getStyle(TranscriptStyleContext.DEFAULT_STYLE));
        TranscriptStyleConstants.setElementType(attrs, TranscriptStyleConstants.ELEMENT_TYPE_GENERIC);
        TranscriptStyleConstants.setGenericTier(attrs, languagesTier);
        StyleConstants.setFontSize(attrs, StyleConstants.getFontSize(attrs));

        // start paragraph
        batchBuilder.appendBatchEndStart(batchBuilder.getTrailingAttributes(), attrs);

        // label
        final SimpleAttributeSet labelAttrs = new SimpleAttributeSet(attrs);
        labelAttrs.addAttributes(doc.getTranscriptStyleContext().getLabelAttributes());
        TranscriptStyleConstants.setClickHandler(labelAttrs, (e, tier) -> {
        });
        String labelText = batchBuilder.formatLabelText("Languages");
        TranscriptStyleConstants.setUnderlineOnHover(labelAttrs, true);
        batchBuilder.appendBatchString(labelText, labelAttrs);
        TranscriptStyleConstants.setUnderlineOnHover(labelAttrs, false);
        batchBuilder.appendBatchString(": ", labelAttrs);

        // value
        TranscriptStyleConstants.setEnterAction(attrs, PhonUIAction.runnable(this::updateLanguagesFromText));
        batchBuilder.appendBatchString(languagesTier.toString(), attrs);
    }

    private void updateLanguagesHeader(EditorEvent<EditorEventType.SessionLangChangedData> event) {
        if(event.getData().isEmpty()) return;
        try {
            Tier<TranscriptDocument.Languages> languagesHeaderTier = (Tier<TranscriptDocument.Languages>) headerTierMap.get("languages");
            if(errorUnderlineHighlights.containsKey(languagesHeaderTier)) {
                editor.getHighlighter().removeHighlight(errorUnderlineHighlights.get(languagesHeaderTier));
                errorUnderlineHighlights.remove(languagesHeaderTier);
            }

            final UnvalidatedValue uv = languagesHeaderTier.getUnvalidatedValue();
            languagesHeaderTier.setValue(new TranscriptDocument.Languages(event.getData().get().newLang()));
            if((!languagesHeaderTier.hasValue() || languagesHeaderTier.getValue().languageList().isEmpty()) && uv != null) {
                languagesHeaderTier.putExtension(UnvalidatedValue.class, uv);
            }
            TranscriptDocument.StartEnd startEnd = doc.getGenericContentStartEnd(languagesHeaderTier);

            if (startEnd.valid()) {
                doc.setBypassDocumentFilter(true);
                editor.getTranscriptEditorCaret().freeze();
                doc.remove(startEnd.start(), startEnd.length());

                final SimpleAttributeSet attrs = new SimpleAttributeSet(doc.getTranscriptStyleContext().getStyle(TranscriptStyleContext.DEFAULT_STYLE));
                TranscriptStyleConstants.setElementType(attrs, TranscriptStyleConstants.ELEMENT_TYPE_GENERIC);
                TranscriptStyleConstants.setGenericTier(attrs, languagesHeaderTier);

                TranscriptBatchBuilder batchBuilder = new TranscriptBatchBuilder(doc.getTranscriptStyleContext(), doc.getInsertionHooks());
                final String langStr = languagesHeaderTier.isUnvalidated() ? languagesHeaderTier.getUnvalidatedValue().getValue() : languagesHeaderTier.toString();
                batchBuilder.appendBatchString(langStr, attrs);
                doc.processBatchUpdates(startEnd.start(), batchBuilder.getBatch());

                if(languagesHeaderTier.isUnvalidated()) {
                    errorUnderlineHighlights.put(languagesHeaderTier,
                            editor.getHighlighter().addHighlight(startEnd.start() + languagesHeaderTier.getUnvalidatedValue().getParseError().getErrorOffset(), startEnd.end(), new TranscriptEditor.ErrorUnderlinePainter()));
                }
            }
        }
        catch (BadLocationException e) {
            LogUtil.severe(e);
        } finally {
            editor.getTranscriptEditorCaret().unfreeze();
        }
    }

    private void updateLanguagesFromText() {
        final Tier<TranscriptDocument.Languages> languagesTier = (Tier<TranscriptDocument.Languages>) headerTierMap.get("languages");
        final TranscriptDocument.StartEnd startEnd = doc.getGenericContentStartEnd(languagesTier);
        if(startEnd.valid()) {
            try {
                final String currentLanguagesString = languagesTier.toString();
                final String languagesStr = doc.getText(startEnd.start(), startEnd.length());
                if(!languagesStr.equals(currentLanguagesString)) {
                    languagesTier.setText(languagesStr);

                    final List<Language> languages = languagesTier.hasValue() ? languagesTier.getValue().languageList() : Collections.emptyList();
                    final SessionLanguageEdit edit = new SessionLanguageEdit(session, editor.getEventManager(), languages);
                    editor.getUndoSupport().postEdit(edit);
                }
            } catch (BadLocationException e) {
                LogUtil.warning(e);
            }
        }
    }

    /**
     * Gets a list of {@link javax.swing.text.DefaultStyledDocument.ElementSpec} that contains the data for
     * the tiers header
     *
     * @return the list of {@link javax.swing.text.DefaultStyledDocument.ElementSpec} data
     * */
    public void appendTiersHeader(TranscriptBatchBuilder batchBuilder) {
        Tier<TierData> tiersHeaderTier = (Tier<TierData>) headerTierMap.get("tiers");
        tiersHeaderTier.setText(session.getTierView().stream()
                .filter(TierViewItem::isVisible).map(TierViewItem::getTierName).collect(Collectors.joining(", ")));

        final SimpleAttributeSet attrs = new SimpleAttributeSet(doc.getTranscriptStyleContext().getStyle(TranscriptStyleContext.DEFAULT_STYLE));
        TranscriptStyleConstants.setElementType(attrs, TranscriptStyleConstants.ELEMENT_TYPE_GENERIC);
        TranscriptStyleConstants.setGenericTier(attrs, tiersHeaderTier);
        StyleConstants.setFontSize(attrs, StyleConstants.getFontSize(attrs));

        // start paragraph
        batchBuilder.appendBatchEndStart(batchBuilder.getTrailingAttributes(), attrs);

        // label
        final SimpleAttributeSet labelAttrs = new SimpleAttributeSet(attrs);
        labelAttrs.addAttributes(doc.getTranscriptStyleContext().getLabelAttributes());
        TranscriptStyleConstants.setClickHandler(labelAttrs, (e, tier) -> {
            showTierMenu(e);
        });
        String labelText = batchBuilder.formatLabelText("Tiers");
        TranscriptStyleConstants.setUnderlineOnHover(labelAttrs, true);
        batchBuilder.appendBatchString(labelText, labelAttrs);
        TranscriptStyleConstants.setUnderlineOnHover(labelAttrs, false);
        batchBuilder.appendBatchString(": ", labelAttrs);

        // value
        TranscriptStyleConstants.setNotEditable(attrs, true);
        batchBuilder.appendBatchString(tiersHeaderTier.toString(), attrs);
    }

    /**
     * Updates the tiers header when there is a change to the tier view
     *
     * @param event the event that caused the change to the tier view
     * */
    public void updateTiersHeader(EditorEvent<EditorEventType.TierViewChangedData> event) {
        final TranscriptElementLocation startLocation = editor.charPosToSessionLocation(editor.getCaretPosition());
        try {
            Tier<TierData> tiersHeaderTier = (Tier<TierData>) headerTierMap.get("tiers");
            if(errorUnderlineHighlights.containsKey(tiersHeaderTier)) {
                editor.getHighlighter().removeHighlight(errorUnderlineHighlights.get(tiersHeaderTier));
                errorUnderlineHighlights.remove(tiersHeaderTier);
            }

            tiersHeaderTier.setText(session.getTierView().stream()
                    .filter(TierViewItem::isVisible).map(TierViewItem::getTierName).collect(Collectors.joining(", ")));
            TranscriptDocument.StartEnd startEnd = doc.getGenericContentStartEnd(tiersHeaderTier);
            if (startEnd.valid()) {
                doc.setBypassDocumentFilter(true);
                editor.getTranscriptEditorCaret().freeze();
                doc.remove(startEnd.start(), startEnd.length());

                final SimpleAttributeSet attrs = new SimpleAttributeSet(doc.getTranscriptStyleContext().getStyle(TranscriptStyleContext.DEFAULT_STYLE));
                TranscriptStyleConstants.setElementType(attrs, TranscriptStyleConstants.ELEMENT_TYPE_GENERIC);
                TranscriptStyleConstants.setGenericTier(attrs, tiersHeaderTier);

                TranscriptBatchBuilder batchBuilder = new TranscriptBatchBuilder(doc.getTranscriptStyleContext(), doc.getInsertionHooks());
                batchBuilder.appendBatchString(tiersHeaderTier.toString(), attrs);
                doc.processBatchUpdates(startEnd.start(), batchBuilder.getBatch());
            }
        }
        catch (BadLocationException e) {
            LogUtil.severe(e);
        } finally {
            doc.setBypassDocumentFilter(false);
            editor.getTranscriptEditorCaret().unfreeze();
            if(startLocation.valid()) {
                final int newPos = editor.sessionLocationToCharPos(startLocation);
                if(newPos >= 0)
                    editor.setCaretPosition(newPos);
            }
        }
    }

    private void appendMediaHeader(TranscriptBatchBuilder batchBuilder) {
        final Tier<TierData> mediaTier = (Tier<TierData>) headerTierMap.get("media");

        final String mediaLocation = session.getMediaLocation();
        if(mediaLocation != null && !mediaLocation.isBlank() && !editor.getMediaModel().isSessionMediaAvailable()) {
            final TierData unvalidatedValue = new TierData();
            unvalidatedValue.putExtension(UnvalidatedValue.class, new UnvalidatedValue(mediaLocation, new ParseException("Media file not found", 0)));
            mediaTier.setValue(unvalidatedValue);
        } else {
            mediaTier.setText(session.getMediaLocation() != null ? session.getMediaLocation() : "");
        }
        final SimpleAttributeSet attrs = new SimpleAttributeSet(doc.getTranscriptStyleContext().getStyle(TranscriptStyleContext.DEFAULT_STYLE));
        TranscriptStyleConstants.setElementType(attrs, TranscriptStyleConstants.ELEMENT_TYPE_GENERIC);
        TranscriptStyleConstants.setGenericTier(attrs, mediaTier);
        StyleConstants.setFontSize(attrs, StyleConstants.getFontSize(attrs));

        // start paragraph
        batchBuilder.appendBatchEndStart(batchBuilder.getTrailingAttributes(), attrs);

        // label
        final SimpleAttributeSet labelAttrs = new SimpleAttributeSet(attrs);
        labelAttrs.addAttributes(doc.getTranscriptStyleContext().getLabelAttributes());
        TranscriptStyleConstants.setClickHandler(labelAttrs, (e, tier) -> {
            showMediaTierMenu(e, mediaTier);
        });
        String labelText = batchBuilder.formatLabelText("Media");
        TranscriptStyleConstants.setUnderlineOnHover(labelAttrs, true);
        batchBuilder.appendBatchString(labelText, labelAttrs);
        TranscriptStyleConstants.setUnderlineOnHover(labelAttrs, false);
        batchBuilder.appendBatchString(": ", labelAttrs);

        // value
        TranscriptStyleConstants.setEnterAction(attrs, PhonUIAction.runnable(this::updateMediaFromText));
        batchBuilder.appendBatchString(mediaTier.toString(), attrs);
    }

    /**
     * Gets a list of {@link javax.swing.text.DefaultStyledDocument.ElementSpec} that contains the data for
     * the date header
     *
     * @return the list of {@link javax.swing.text.DefaultStyledDocument.ElementSpec} data
     * */
    private void appendDateHeader(TranscriptBatchBuilder batchBuilder) {
        Tier<LocalDate> dateTier = (Tier<LocalDate>) headerTierMap.get("date");
        dateTier.setValue(editor.getSession().getDate());

        final SimpleAttributeSet attrs = new SimpleAttributeSet(doc.getTranscriptStyleContext().getStyle(TranscriptStyleContext.DEFAULT_STYLE));
        TranscriptStyleConstants.setElementType(attrs, TranscriptStyleConstants.ELEMENT_TYPE_GENERIC);
        TranscriptStyleConstants.setGenericTier(attrs, dateTier);
        StyleConstants.setFontSize(attrs, StyleConstants.getFontSize(attrs));

        // start paragraph
        batchBuilder.appendBatchEndStart(batchBuilder.getTrailingAttributes(), attrs);

        // label
        final SimpleAttributeSet labelAttrs = new SimpleAttributeSet(attrs);
        labelAttrs.addAttributes(doc.getTranscriptStyleContext().getLabelAttributes());
        TranscriptStyleConstants.setClickHandler(labelAttrs, (e, tier) -> {
            showDateTierMenu(e, dateTier);
        });
        String labelText = batchBuilder.formatLabelText("Date");
        TranscriptStyleConstants.setUnderlineOnHover(labelAttrs, true);
        batchBuilder.appendBatchString(labelText, labelAttrs);
        TranscriptStyleConstants.setUnderlineOnHover(labelAttrs, false);
        batchBuilder.appendBatchString(": ", labelAttrs);

        // value
        final PhonUIAction showDatePickerAct = PhonUIAction.runnable(this::showDatePicker);
        TranscriptStyleConstants.setEnterAction(attrs, showDatePickerAct);
        batchBuilder.appendBatchString(dateTier.toString(), attrs);
    }

    /**
     * Show date picker Callout for date header
     *
     */
    private void showDatePicker() {
        final Tier<LocalDate> dateTier = (Tier<LocalDate>) headerTierMap.get("date");
        final JXMonthView monthView = new JXMonthView();
        monthView.setTraversable(true);
        monthView.setFocusable(false);

        JComboBox<Integer> yearSelectionBox = new JComboBox<>(new YearComboBoxModel());
        if(dateTier.hasValue()) {
            final Date date = Date.from(dateTier.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant());
            monthView.setFirstDisplayedDay(date);
            monthView.setSelectionDate(date);
            yearSelectionBox.setSelectedItem(dateTier.getValue().getYear());
        } else {
            final Date date = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
            monthView.setFirstDisplayedDay(date);
            yearSelectionBox.setSelectedItem(LocalDate.now().getYear());
        }

        yearSelectionBox.addItemListener((evt) -> {
            if(evt.getStateChange() == ItemEvent.SELECTED) {
                Date javaDate = monthView.getSelectionDate();
                if(javaDate == null) javaDate = monthView.getFirstDisplayedDay();
                if(javaDate == null) return;
                final LocalDate localDate = javaDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                final LocalDate newDate = LocalDate.of((int)yearSelectionBox.getSelectedItem(), localDate.getMonth(), localDate.getDayOfMonth());
                final Date newJavaDate = Date.from(newDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
                monthView.setFirstDisplayedDay(newJavaDate);
                if(monthView.getSelectionDate() != null) {
                    monthView.setSelectionDate(newJavaDate);
                }
            }
        });

        monthView.getSelectionModel().addDateSelectionListener((ev) -> {
            final Date javaDate = monthView.getSelectionDate();
            if (javaDate == null) return;
            final LocalDate localDate = javaDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            final SessionDateEdit edit = new SessionDateEdit(session, editor.getEventManager(), localDate, dateTier.getValue());
            editor.getUndoSupport().postEdit(edit);
        });

        final JPanel calloutPanel = new JPanel(new BorderLayout());
        calloutPanel.add(yearSelectionBox, BorderLayout.NORTH);
        calloutPanel.add(monthView, BorderLayout.CENTER);

        final TranscriptDocument.StartEnd startEnd = doc.getGenericContentStartEnd(dateTier);
        if(startEnd.valid()) {
            try {
                final var pos = editor.modelToView2D(startEnd.start());
                final var pt = new Point((int)pos.getX(), (int)pos.getMaxY());
                SwingUtilities.convertPointToScreen(pt, editor);
                CalloutWindow.showCallout(CommonModuleFrame.getCurrentFrame(), true, calloutPanel,
                        SwingUtilities.NORTH, pt);
            } catch (BadLocationException e) {
                LogUtil.warning(e);
            }
        }
    }

    /**
     * Updates the date header when the session date changes
     *
     * @param event the event that caused the change to the session date
     * */
    public void updateDateHeader(EditorEvent<EditorEventType.SessionDateChangedData> event) {
        try {
            Tier<LocalDate> dateHeaderTier = (Tier<LocalDate>) headerTierMap.get("date");
            if(errorUnderlineHighlights.containsKey(dateHeaderTier)) {
                editor.getHighlighter().removeHighlight(errorUnderlineHighlights.get(dateHeaderTier));
                errorUnderlineHighlights.remove(dateHeaderTier);
            }

            final UnvalidatedValue uv = dateHeaderTier.getUnvalidatedValue();
            dateHeaderTier.setValue(event.getData().get().newDate());
            if(!dateHeaderTier.hasValue() && uv != null) {
                dateHeaderTier.putExtension(UnvalidatedValue.class, uv);
            }
            TranscriptDocument.StartEnd startEnd = doc.getGenericContentStartEnd(dateHeaderTier);

            if (startEnd.valid()) {
                doc.setBypassDocumentFilter(true);
                editor.getTranscriptEditorCaret().freeze();
                doc.remove(startEnd.start(), startEnd.length());

                final SimpleAttributeSet attrs = new SimpleAttributeSet(doc.getTranscriptStyleContext().getStyle(TranscriptStyleContext.DEFAULT_STYLE));
                TranscriptStyleConstants.setElementType(attrs, TranscriptStyleConstants.ELEMENT_TYPE_GENERIC);
                TranscriptStyleConstants.setGenericTier(attrs, dateHeaderTier);
                StyleConstants.setFontSize(attrs, StyleConstants.getFontSize(attrs));

                TranscriptBatchBuilder batchBuilder = new TranscriptBatchBuilder(doc.getTranscriptStyleContext(), doc.getInsertionHooks());
                final PhonUIAction showDatePickerAct = PhonUIAction.runnable(this::showDatePicker);
                TranscriptStyleConstants.setEnterAction(attrs, showDatePickerAct);
                batchBuilder.appendBatchString(dateHeaderTier.toString(), attrs);
                doc.processBatchUpdates(startEnd.start(), batchBuilder.getBatch());

                if(dateHeaderTier.isUnvalidated()) {
                    errorUnderlineHighlights.put(dateHeaderTier,
                            editor.getHighlighter().addHighlight(startEnd.start(), startEnd.end(), new TranscriptEditor.ErrorUnderlinePainter()));
                }
            }
        }
        catch (BadLocationException e) {
            LogUtil.severe(e);
        } finally {
            editor.getTranscriptEditorCaret().unfreeze();
        }
    }

    private void updateDateFromText() {
        final Tier<LocalDate> dateTier = (Tier<LocalDate>) headerTierMap.get("date");
        final String currentDateString = dateTier.toString();
        final TranscriptDocument.StartEnd startEnd = doc.getGenericContentStartEnd(dateTier);
        if(startEnd.valid()) {
            try {
                final LocalDate date = dateTier.getValue();
                final String dateStr = doc.getText(startEnd.start(), startEnd.length());
                if(!dateStr.equals(currentDateString)) {
                    dateTier.setText(dateStr);

                    if (dateTier.isUnvalidated()) {
                        final SessionDateEdit edit = new SessionDateEdit(session, editor.getEventManager(), null, date);
                        editor.getUndoSupport().postEdit(edit);
                    } else {
                        final SessionDateEdit edit = new SessionDateEdit(session, editor.getEventManager(), dateTier.getValue(), date);
                        editor.getUndoSupport().postEdit(edit);
                    }
                }
            } catch (BadLocationException e) {
                LogUtil.warning(e);
            }
        }
    }

    private void onSessionMediaChanged(EditorEvent<EditorEventType.SessionMediaChangedData> evt) {
        if(!evt.getData().isPresent()) return;
        try {
            Tier<TierData> mediaTier = (Tier<TierData>) headerTierMap.get("media");
            if(errorUnderlineHighlights.containsKey(mediaTier)) {
                editor.getHighlighter().removeHighlight(errorUnderlineHighlights.get(mediaTier));
                errorUnderlineHighlights.remove(mediaTier);
            }

            final TierData newVal = evt.getData().get().newMedia() != null ? TierData.parseTierData(evt.getData().get().newMedia()) : null;
            mediaTier.setValue(newVal);
            if(newVal != null) {
                final File file = MediaLocator.findMediaFile(mediaTier.getValue().toString(), editor.getDataModel().getProject(), editor.getSession().getCorpus());
                if (file == null || !file.exists()) {
                    final UnvalidatedValue uv = new UnvalidatedValue(mediaTier.getValue().toString(), new ParseException("Media file does not exist", 0));
                    mediaTier.getValue().putExtension(UnvalidatedValue.class, uv);
                }
            }

            TranscriptDocument.StartEnd startEnd = doc.getGenericContentStartEnd(mediaTier);
            if (startEnd.valid()) {
                doc.setBypassDocumentFilter(true);
                editor.getTranscriptEditorCaret().freeze();
                doc.remove(startEnd.start(), startEnd.length());

                final SimpleAttributeSet attrs = new SimpleAttributeSet(doc.getTranscriptStyleContext().getStyle(TranscriptStyleContext.DEFAULT_STYLE));
                TranscriptStyleConstants.setElementType(attrs, TranscriptStyleConstants.ELEMENT_TYPE_GENERIC);
                TranscriptStyleConstants.setGenericTier(attrs, mediaTier);
                StyleConstants.setFontSize(attrs, StyleConstants.getFontSize(attrs));

                TranscriptBatchBuilder batchBuilder = new TranscriptBatchBuilder(doc.getTranscriptStyleContext(), doc.getInsertionHooks());
                batchBuilder.appendBatchString(mediaTier.toString(), attrs);
                doc.processBatchUpdates(startEnd.start(), batchBuilder.getBatch());

                if(mediaTier.isUnvalidated()) {
                    errorUnderlineHighlights.put(mediaTier,
                            editor.getHighlighter().addHighlight(startEnd.start(), startEnd.end(), new TranscriptEditor.ErrorUnderlinePainter()));
                }
            }
        } catch (BadLocationException | ParseException e) {
            LogUtil.severe(e);
        } finally {
            editor.getTranscriptEditorCaret().unfreeze();
        }
    }

    private void updateMediaFromText() {
        final Tier<TierData> mediaTier = (Tier<TierData>) headerTierMap.get("media");
        final TranscriptDocument.StartEnd startEnd = doc.getGenericContentStartEnd(mediaTier);
        if(startEnd.valid()) {
            try {
                final String currentMediaString = mediaTier.toString();
                final String mediaStr = doc.getText(startEnd.start(), startEnd.length());
                if(!mediaStr.equals(currentMediaString)) {
                    mediaTier.setText(mediaStr);

                    final MediaLocationEdit edit = new MediaLocationEdit(session, editor.getEventManager(), mediaTier.getValue().toString());
                    editor.getUndoSupport().postEdit(edit);
                }
            } catch (BadLocationException e) {
                LogUtil.warning(e);
            }
        }
    }

    private void onSessionLocationChanged(EditorEvent<TranscriptEditor.TranscriptLocationChangeData> evt) {
        if(!evt.getData().isPresent()) return;
        if(evt.getData().get().oldLoc().tier() == null) return;
        if(evt.getData().get().oldLoc().transcriptElementIndex() == -1) {
            if(evt.getData().get().oldLoc().tier().equals("Date")) {
                updateDateFromText();
            } else if(evt.getData().get().oldLoc().tier().equals("Media")) {
                updateMediaFromText();
            } else if(evt.getData().get().oldLoc().tier().equals("Languages")) {
                updateLanguagesFromText();
            }
        }
    }

    private void clearDate() {
        final Tier<LocalDate> dateTier = (Tier<LocalDate>) headerTierMap.get("date");
        var oldDate = dateTier.getValue();
        dateTier.setValue(null);

        final SessionDateEdit edit = new SessionDateEdit(session, editor.getEventManager(), null, oldDate);
        editor.getUndoSupport().postEdit(edit);
    }

    private void showDateTierMenu(MouseEvent e, Tier<LocalDate> dateTier) {
        final JMenu menu = new JMenu("Date");
        final MenuBuilder builder = new MenuBuilder(menu);

        final PhonUIAction<Void> showDatePickerAct = PhonUIAction.runnable(this::showDatePicker);
        showDatePickerAct.putValue(PhonUIAction.NAME, "Edit");
        showDatePickerAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Edit date using date picker");
        builder.addItem(".", showDatePickerAct);

        final PhonUIAction<Void> clearDateAct = PhonUIAction.runnable(this::clearDate);
        clearDateAct.putValue(PhonUIAction.NAME, "Remove date");
        clearDateAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Remove date from session");
        builder.addItem(".", clearDateAct);

        menu.getPopupMenu().show(e.getComponent(), e.getX(), e.getY());
    }

    private void showMediaTierMenu(MouseEvent e, Tier<TierData> mediaTier) {
        final JMenu menu = new JMenu("Media");
        final MenuBuilder builder = new MenuBuilder(menu);

        final PhonUIAction<Void> browseForMediaAct = PhonUIAction.runnable(this::browseForMedia);
        browseForMediaAct.putValue(PhonUIAction.NAME, "Browse...");
        browseForMediaAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Browse for media file");
        builder.addItem(".", browseForMediaAct);

        final PhonUIAction<Void> clearMediaAct = PhonUIAction.runnable(() -> {
            final MediaLocationEdit edit = new MediaLocationEdit(session, editor.getEventManager(), null);
            editor.getUndoSupport().postEdit(edit);
        });
        clearMediaAct.putValue(PhonUIAction.NAME, "Remove media");
        clearMediaAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Remove media from session");
        builder.addItem(".", clearMediaAct);

        menu.getPopupMenu().show(editor, e.getX(), e.getY());
    }

    private void showTierMenu(MouseEvent e) {
        final JMenu menu = new JMenu("Tiers");
        final MenuBuilder builder = new MenuBuilder(menu);

        final TranscriptView transcriptView = (TranscriptView) SwingUtilities.getAncestorOfClass(TranscriptView.class, editor);
        if(transcriptView != null) {
            transcriptView.setupTiersMenu(builder);
        }

        menu.getPopupMenu().show(editor, e.getX(), e.getY());
    }

    private void showParticipantsMenu(MouseEvent e) {
        final JMenu menu = new JMenu("Participants");
        final MenuBuilder builder = new MenuBuilder(menu);

        final TranscriptView transcriptView = (TranscriptView) SwingUtilities.getAncestorOfClass(TranscriptView.class, editor);
        if(transcriptView != null) {
            transcriptView.setupParticipantsMenu(builder);
        }

        menu.getPopupMenu().show(editor, e.getX(), e.getY());
    }

    private void browseForMedia() {
        final OpenDialogProperties props = new OpenDialogProperties();
        props.setRunAsync(false);
        props.setAllowMultipleSelection(false);
        props.setCanChooseDirectories(false);
        props.setCanChooseFiles(true);
        props.setFileFilter(FileFilter.mediaFilter);
        props.setTitle("Select media for session");

        final List<String> selectedFiles =
                NativeDialogs.showOpenDialog(props);
        if(selectedFiles != null && selectedFiles.size() > 0) {
            final String path = selectedFiles.get(0);

            final MediaLocationEdit edit = new MediaLocationEdit(session, editor.getEventManager(), path);
            editor.getUndoSupport().postEdit(edit);
        }
    }

    /**
     * Get whether the language header is currently being forced to be shown without any content
     * */
    private boolean isForceShowLanguageHeader() {
        return (boolean) doc.getDocumentPropertyOrDefault("forceShowLanguageHeader", false);
    }

    private class YearComboBoxModel extends DefaultComboBoxModel<Integer> {

        private int numYears;

        private int selectedYear = 0;

        public YearComboBoxModel() {
            this(150);
        }

        public YearComboBoxModel(int numYears) {
            this.numYears = numYears;
        }

        @Override
        public int getSize() {
            return this.numYears;
        }

        @Override
        public Integer getElementAt(int index) {
            return LocalDate.now().getYear() - ((this.numYears-1)-index);
        }

        @Override
        public void setSelectedItem(Object anItem) {
            this.selectedYear = (Integer)anItem;
        }

        @Override
        public Object getSelectedItem() {
            return this.selectedYear;
        }
    }

}
