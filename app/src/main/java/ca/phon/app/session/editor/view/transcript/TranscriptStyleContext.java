package ca.phon.app.session.editor.view.transcript;

import ca.phon.app.session.editor.EditorEventManager;
import ca.phon.app.session.editor.undo.SessionEditUndoSupport;
import ca.phon.ipa.IPATranscript;
import ca.phon.session.*;
import ca.phon.session.Record;
import ca.phon.session.tierdata.*;
import ca.phon.ui.fonts.FontPreferences;
import ca.phon.util.PrefHelper;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.*;
import java.awt.*;
import java.util.Enumeration;
import java.util.List;

/**
 * A collection of named styles for TranscriptDocument
 */
public class TranscriptStyleContext extends StyleContext {

    public static final String DEFAULT = "default";

    public static final String GHOST_TEXT = "ghost-text";

    public TranscriptStyleContext() {
        super();

        // add default styles
        addDefaultStyles();
    }

    private void addDefaultStyles() {
        createDefaultStyle();
        createGhostTextStyle();
    }

    private Style createDefaultStyle() {
        final Style style = addStyle(DEFAULT, null);
        StyleConstants.setFontFamily(style, FontPreferences.getTierFont().getFamily());
        StyleConstants.setFontSize(style, FontPreferences.getTierFont().getSize());
        StyleConstants.setBold(style, FontPreferences.getTierFont().isBold());
        StyleConstants.setItalic(style, FontPreferences.getTierFont().isItalic());
        return style;
    }

    private Style createGhostTextStyle() {
    	final Style style = addStyle(GHOST_TEXT, getStyle(DEFAULT));
    	StyleConstants.setForeground(style, Color.gray);
    	StyleConstants.setItalic(style, true);
    	return style;
    }

    public static MutableAttributeSet stripStyle(AttributeSet attrs) {
    	MutableAttributeSet retVal = new SimpleAttributeSet();
    	final Enumeration<?> names = attrs.getAttributeNames();
        while(names.hasMoreElements()) {
            final Object key = names.nextElement();
            if(key == StyleConstants.Bold || key == StyleConstants.Italic || key == StyleConstants.FontFamily
                || key == StyleConstants.FontSize || key == StyleConstants.Foreground || key == StyleConstants.Background) {
                continue;
            }
            retVal.addAttribute(key, attrs.getAttribute(key));
        }
    	return retVal;
    }

    // region Attribute Getters

    /**
     * Gets the starting paragraph attributes for a record
     *
     */
    public SimpleAttributeSet getRecordStartAttributes() {
        SimpleAttributeSet retVal = new SimpleAttributeSet(getStyle(TranscriptStyleContext.DEFAULT));
        retVal.addAttribute(TranscriptStyleConstants.ATTR_KEY_BORDER,
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor(TranscriptEditorUIProps.RECORD_BORDER_COLOR)));
        return retVal;
    }

    /**
     * Return paragraph attributes with given border
     * @param border
     * @return
     */
    public SimpleAttributeSet getBorderedParagraphAttributes(Border border) {
        SimpleAttributeSet nextParagraphAttrs = new SimpleAttributeSet();
        nextParagraphAttrs.addAttribute(TranscriptStyleConstants.ATTR_KEY_BORDER, border);
        return nextParagraphAttrs;
    }

    /**
     * Gets the attributes for a given record
     *
     * @param record the record to get the attributes of
     * @return a mutable attribute set containing all the necessary attributes for the given record
     */
    public SimpleAttributeSet getRecordAttributes(Record record) {
        SimpleAttributeSet retVal = new SimpleAttributeSet();

        retVal.addAttribute(TranscriptStyleConstants.ATTR_KEY_RECORD, record);
        retVal.addAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE, TranscriptStyleConstants.ATTR_KEY_RECORD);

        return retVal;
    }

    /**
     * Gets the attributes for a given tier
     *
     * @param tier a reference to the tier to get the attributes of
     * @return a mutable attribute set containing all the necessary attributes for the given tier
     */
    public SimpleAttributeSet getTierAttributes(Tier<?> tier) {
        return getTierAttributes(tier, null);
    }

    /**
     * Gets the attributes for a given tier
     *
     * @param tier a reference to the tier to get the attributes of
     * @param item a {@link TierViewItem} whose font data will be used if provided
     * @return a mutable attribute set containing all the necessary attributes for the given tier
     */
    public SimpleAttributeSet getTierAttributes(Tier<?> tier, TierViewItem item) {
        final SimpleAttributeSet retVal = new SimpleAttributeSet(getStyle(TranscriptStyleContext.DEFAULT));

        retVal.addAttribute(TranscriptStyleConstants.ATTR_KEY_TIER, tier);

        String fontString = "default";
        if (item != null) {
            fontString = item.getTierFont();
            if (!"default".equalsIgnoreCase(fontString)) {
                var font = Font.decode(fontString);

                StyleConstants.setFontFamily(retVal, font.getFamily());
                StyleConstants.setFontSize(retVal, font.getSize());
                StyleConstants.setBold(retVal, font.isBold());
                StyleConstants.setItalic(retVal, font.isItalic());
            }
        }
        StyleConstants.setFontSize(retVal,
                StyleConstants.getFontSize(retVal));

        return retVal;
    }

    /**
     * Gets an attribute set containing a reference to the alignment component factory.
     * Adding the contents of this attribute set to the attributes of an alignment tier will
     * cause it to appear as the {@link ca.phon.ui.ipa.PhoneMapDisplay} component instead of text
     *
     * @return an attribute set containing a reference to the alignment component factory
     */
    public SimpleAttributeSet getAlignmentAttributes() {
        final SimpleAttributeSet retVal = new SimpleAttributeSet(getStyle(TranscriptStyleContext.DEFAULT));
        retVal.addAttribute(TranscriptStyleConstants.ATTR_KEY_COMPONENT_FACTORY, new AlignmentComponentFactory());
        return retVal;
    }

    /**
     * Gets an attribute set containing a reference to the transcript selector component factory.
     * Adding the contents of this attribute set to the attributes some text will replace the text
     * with a button for selecting the specified blind transcription
     *
     * @param record            the record containing the blind tier that the transcription will be selected from
     * @param tier              the tier that the blind transcription will be selected from
     * @param transcriptionText the text of the transcription that will be selected if the button is clicked
     * @return an attribute set containing a reference to the transcript selector component factory
     */
    public SimpleAttributeSet getTranscriptionSelectorAttributes(Record record, Tier<?> tier, String transcriptionText, Session session, EditorEventManager eventManager, SessionEditUndoSupport undoSupport) {
        final SimpleAttributeSet retVal = new SimpleAttributeSet(getStyle(TranscriptStyleContext.DEFAULT));

        retVal.addAttribute(TranscriptStyleConstants.ATTR_KEY_COMPONENT_FACTORY, new TranscriptionSelectorComponentFactory(
                session,
                eventManager,
                undoSupport,
                record,
                tier,
                transcriptionText
        ));
        retVal.addAttribute(TranscriptStyleConstants.ATTR_KEY_NOT_EDITABLE, true);

        return retVal;
    }

    /**
     * Gets the attributes for an IPA word
     *
     * @param tier the tier that will be referenced in the attributes with
     *             the {@code TranscriptStyleConstants.ATTR_KEY_TIER} key
     * @return a mutable attribute set containing the attributes for an IPA word
     */
    public SimpleAttributeSet getIPAWordAttributes(Tier<IPATranscript> tier) {
        SimpleAttributeSet retVal = new SimpleAttributeSet(getStyle(TranscriptStyleContext.DEFAULT));
        retVal.addAttribute(TranscriptStyleConstants.ATTR_KEY_TIER, tier);

        StyleConstants.setForeground(retVal, UIManager.getColor(TranscriptEditorUIProps.IPA_WORD));

        return retVal;
    }

    /**
     * Gets the attributes for an IPA pause
     *
     * @param tier the tier that will be referenced in the attributes with
     *             the {@code TranscriptStyleConstants.ATTR_KEY_TIER} key
     * @return a mutable attribute set containing the attributes for an IPA pause
     */
    public SimpleAttributeSet getIPAPauseAttributes(Tier<IPATranscript> tier) {
        SimpleAttributeSet retVal = new SimpleAttributeSet(getStyle(TranscriptStyleContext.DEFAULT));
        retVal.addAttribute(TranscriptStyleConstants.ATTR_KEY_TIER, tier);

        StyleConstants.setForeground(retVal, UIManager.getColor(TranscriptEditorUIProps.IPA_PAUSE));

        return retVal;
    }

    /**
     * Gets the attributes for the label of a given tier line.
     *
     * @param tier the tier whose label these attributes will be for
     * @return a mutable attribute set containing all the necessary attributes for the label of the tier
     */
    public SimpleAttributeSet getTierLabelAttributes(Tier<?> tier) {
        final SimpleAttributeSet retVal = new SimpleAttributeSet(getStyle(TranscriptStyleContext.DEFAULT));

        retVal.addAttribute(TranscriptStyleConstants.ATTR_KEY_LABEL, true);
        retVal.addAttribute(TranscriptStyleConstants.ATTR_KEY_NOT_TRAVERSABLE, true);
        retVal.addAttribute(TranscriptStyleConstants.ATTR_KEY_NOT_EDITABLE, true);
        retVal.addAttribute(TranscriptStyleConstants.ATTR_KEY_TIER, tier);

        Style defaultStyle = getStyle(TranscriptStyleContext.DEFAULT);
        retVal.addAttributes(defaultStyle);
        StyleConstants.setBold(retVal, true);
        StyleConstants.setFontSize(retVal, StyleConstants.getFontSize(defaultStyle));

        return retVal;
    }

    /**
     * Gets the attributes for a separator / record header
     *
     * @return the attributes for a separator / record header
     */
    public SimpleAttributeSet getSeparatorAttributes() {
        final SimpleAttributeSet retVal = new SimpleAttributeSet(getStyle(TranscriptStyleContext.DEFAULT));

        retVal.addAttribute(TranscriptStyleConstants.ATTR_KEY_SEPARATOR, true);

        retVal.addAttributes(getMonospaceFontAttributes());

        return retVal;
    }

    /**
     * Gets the attributes for the blind transcription of a given transcriber in a given tier
     *
     * @param tier        the tier that contains the transcriptions
     * @param transcriber the name/ID of the transcriber who transcribed the transcription that
     *                    these attributes will be added to
     * @return a mutable attribute set containing the necessary attributes for the specified blind transcription
     */
    public SimpleAttributeSet getBlindTranscriptionAttributes(Tier<?> tier, String transcriber) {
        SimpleAttributeSet retVal = new SimpleAttributeSet(getTierAttributes(tier));

        retVal.addAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE, TranscriptStyleConstants.ATTR_KEY_BLIND_TRANSCRIPTION);
        retVal.addAttribute(TranscriptStyleConstants.ATTR_KEY_TIER, tier);
        retVal.addAttribute(TranscriptStyleConstants.ATTR_KEY_TRANSCRIBER, transcriber);
        retVal.addAttribute(TranscriptStyleConstants.ATTR_KEY_NOT_EDITABLE, true);

        return retVal;
    }

    /**
     * Gets the attributes for the tiers header
     *
     * @return the attributes for the tiers header
     */
    public SimpleAttributeSet getTiersHeaderAttributes() {
        SimpleAttributeSet retVal = new SimpleAttributeSet(getStyle(TranscriptStyleContext.DEFAULT));

        retVal.addAttribute(TranscriptStyleConstants.ATTR_KEY_NOT_EDITABLE, true);

        return retVal;
    }

    /**
     * Gets the attributes for the participants header
     *
     * @return the attributes for the participants header
     */
    public SimpleAttributeSet getParticipantsHeaderAttributes() {
        SimpleAttributeSet retVal = new SimpleAttributeSet(getStyle(TranscriptStyleContext.DEFAULT));

        retVal.addAttribute(TranscriptStyleConstants.ATTR_KEY_NOT_EDITABLE, true);

        return retVal;
    }

    /**
     * Gets the attributes for the times in a given segment
     *
     * @param segment the segment that will be referenced in the attributes with
     *                the {@code TranscriptStyleConstants.ATTR_KEY_MEDIA_SEGMENT} key
     * @return a mutable attribute set containing the attributes for the times in a given segment
     */
    public SimpleAttributeSet getSegmentTimeAttributes(MediaSegment segment) {
        SimpleAttributeSet retVal = new SimpleAttributeSet(getStyle(TranscriptStyleContext.DEFAULT));
        retVal.addAttribute(TranscriptStyleConstants.ATTR_KEY_MEDIA_SEGMENT, segment);

        return retVal;
    }

    /**
     * Gets the attributes for the dash in a given segment
     *
     * @param segment the segment that will be referenced in the attributes with
     *                the {@code TranscriptStyleConstants.ATTR_KEY_MEDIA_SEGMENT} key
     * @return a mutable attribute set containing the attributes for the dash in a given segment
     */
    public SimpleAttributeSet getSegmentDashAttributes(MediaSegment segment) {
        SimpleAttributeSet retVal = new SimpleAttributeSet(getStyle(TranscriptStyleContext.DEFAULT));
        retVal.addAttribute(TranscriptStyleConstants.ATTR_KEY_MEDIA_SEGMENT, segment);

        StyleConstants.setForeground(retVal, UIManager.getColor(TranscriptEditorUIProps.SEGMENT_DASH));

        return retVal;
    }

    /**
     * Gets the attributes for a {@link TierString} in some {@link TierData}
     *
     * @return the attributes for a {@link TierString} in some {@link TierData}
     */
    public SimpleAttributeSet getTierStringAttributes() {
        SimpleAttributeSet retVal = new SimpleAttributeSet(getStyle(TranscriptStyleContext.DEFAULT));

        // TODO: add a ui prop color  for this
        StyleConstants.setForeground(retVal, Color.black);

        return retVal;
    }

    /**
     * Gets the attributes for a {@link TierComment} in some {@link TierData}
     *
     * @return the attributes for a {@link TierComment} in some {@link TierData}
     */
    public SimpleAttributeSet getTierCommentAttributes() {
        SimpleAttributeSet retVal = new SimpleAttributeSet(getStyle(TranscriptStyleContext.DEFAULT));

        StyleConstants.setForeground(retVal, UIManager.getColor(TranscriptEditorUIProps.TIER_COMMENT));

        return retVal;
    }

    /**
     * Gets the attributes for {@link TierInternalMedia} in some {@link TierData}
     *
     * @return the attributes for {@link TierInternalMedia} in some {@link TierData}
     */
    public SimpleAttributeSet getTierInternalMediaAttributes() {
        SimpleAttributeSet retVal = new SimpleAttributeSet(getStyle(TranscriptStyleContext.DEFAULT));

        StyleConstants.setForeground(retVal, UIManager.getColor(TranscriptEditorUIProps.INTERNAL_MEDIA));

        return retVal;
    }

    /**
     * Gets the attributes for a {@link TierLink} in some {@link TierData}
     *
     * @return the attributes for a {@link TierLink} in some {@link TierData}
     */
    public SimpleAttributeSet getTierLinkAttributes() {
        SimpleAttributeSet retVal = new SimpleAttributeSet(getStyle(TranscriptStyleContext.DEFAULT));

        return retVal;
    }

    /**
     * Gets the attributes corresponding to the monospace font
     *
     * @return a mutable attribute set containing the attributes corresponding to the monospace font
     */
    public SimpleAttributeSet getMonospaceFontAttributes() {
        SimpleAttributeSet retVal = new SimpleAttributeSet(getStyle(TranscriptStyleContext.DEFAULT));

        Font font = FontPreferences.getMonospaceFont();
        StyleConstants.setFontFamily(retVal, font.getFamily());

        return retVal;
    }

    /**
     * Gets the attributes for a given gem
     *
     * @param gem the gem that the attributes will be for
     * @return the attributes for the given gem
     */
    public SimpleAttributeSet getGemAttributes(Gem gem) {
        SimpleAttributeSet retVal = new SimpleAttributeSet(getStyle(TranscriptStyleContext.DEFAULT));

        retVal.addAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE, TranscriptStyleConstants.ATTR_KEY_GEM);
        retVal.addAttribute(TranscriptStyleConstants.ATTR_KEY_GEM, gem);
        StyleConstants.setFontSize(retVal, StyleConstants.getFontSize(retVal));

        return retVal;
    }

    /**
     * Gets the attributes for a given comment
     *
     * @param comment the comment that the attributes will be for
     * @return the attributes for the given comment
     */
    public SimpleAttributeSet getCommentAttributes(Comment comment) {
        SimpleAttributeSet retVal = new SimpleAttributeSet(getStyle(TranscriptStyleContext.DEFAULT));
        TranscriptStyleConstants.setElementType(retVal, TranscriptStyleConstants.ELEMENT_TYPE_COMMENT);
        TranscriptStyleConstants.setComment(retVal, comment);
        StyleConstants.setFontSize(retVal, StyleConstants.getFontSize(retVal));
        return retVal;
    }

    /**
     * Gets the attributes for the label of a given comment line.
     *
     * @param comment the comment whose label these attributes will be for
     * @return a mutable attribute set containing all the necessary attributes for the label of the comment
     */
    public SimpleAttributeSet getCommentLabelAttributes(Comment comment) {
        SimpleAttributeSet retVal = new SimpleAttributeSet(getCommentAttributes(comment));
        TranscriptStyleConstants.setLabel(retVal, true);
        TranscriptStyleConstants.setNotTraversable(retVal, true);
        TranscriptStyleConstants.setNotEditable(retVal, true);
        StyleConstants.setBold(retVal, true);
        return retVal;
    }

    /**
     * Gets the attributes for the label of a given gem line.
     *
     * @param gem the gem whose label these attributes will be for
     * @return a mutable attribute set containing all the necessary attributes for the label of the gem
     */
    public SimpleAttributeSet getGemLabelAttributes(Gem gem) {
        SimpleAttributeSet retVal = new SimpleAttributeSet(getStyle(TranscriptStyleContext.DEFAULT));
        retVal.addAttributes(getGemAttributes(gem));
        retVal.addAttribute(TranscriptStyleConstants.ATTR_KEY_LABEL, true);
        retVal.addAttribute(TranscriptStyleConstants.ATTR_KEY_NOT_TRAVERSABLE, true);
        retVal.addAttribute(TranscriptStyleConstants.ATTR_KEY_NOT_EDITABLE, true);
        StyleConstants.setBold(retVal, true);
        return retVal;
    }

    /**
     * Gets the attributes for the label of a line that isn't a tier, record header / separator, comment or gem
     *
     * @return a mutable attribute set containing the necessary attributes for a label
     */
    public SimpleAttributeSet getLabelAttributes() {
        SimpleAttributeSet retVal = new SimpleAttributeSet(getStyle(TranscriptStyleContext.DEFAULT));

        retVal.addAttribute(TranscriptStyleConstants.ATTR_KEY_LABEL, true);
        retVal.addAttribute(TranscriptStyleConstants.ATTR_KEY_NOT_TRAVERSABLE, true);
        retVal.addAttribute(TranscriptStyleConstants.ATTR_KEY_NOT_EDITABLE, true);
        StyleConstants.setBold(retVal, true);
        StyleConstants.setFontSize(retVal, StyleConstants.getFontSize(retVal));

        return retVal;
    }

    /**
     * Gets the attributes of the last {@link javax.swing.text.DefaultStyledDocument.ElementSpec} in a provided list
     *
     * @param elementSpecs the list of elementspecs
     * @return the attributes of the last elementspec
     */
    public AttributeSet getTrailingAttributes(List<DefaultStyledDocument.ElementSpec> elementSpecs) {
        if (elementSpecs.isEmpty()) return new SimpleAttributeSet();
        AttributeSet attrs = elementSpecs.get(elementSpecs.size() - 1).getAttributes();
        return attrs == null ? new SimpleAttributeSet() : attrs;
    }
    // endregion Attribute Getters
}
