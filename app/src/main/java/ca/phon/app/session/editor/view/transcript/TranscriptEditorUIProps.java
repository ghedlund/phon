package ca.phon.app.session.editor.view.transcript;

import ca.phon.ui.theme.UIDefaults;
import ca.phon.ui.theme.UIDefaultsHandler;
import ca.phon.plugin.IPluginExtensionFactory;
import ca.phon.plugin.IPluginExtensionPoint;
import ca.phon.ui.PhonGuiConstants;

import javax.swing.*;
import java.awt.*;

public class TranscriptEditorUIProps implements UIDefaultsHandler, IPluginExtensionPoint<UIDefaultsHandler> {
    public final static String BACKGROUND = "TranscriptEditor.background";
    public final static Color DEFAULT_BACKGROUND = UIManager.getColor("EditorPane.background");

    public final static String FOREGROUND = "TranscriptEditor.foreground";
    public final static Color DEFAULT_FOREGROUND = Color.BLACK;

    public final static String LABEL_BACKGROUND = "TranscriptEditor.labelBackground";
    public final static Color DEFAULT_LABEL_BACKGROUND = PhonGuiConstants.PHON_UI_STRIP_COLOR;

    public final static String SEGMENT_DASH = "TranscriptEditor.segmentDash";
    public final static Color DEFAULT_SEGMENT_DASH = Color.GRAY;

    public final static String TIER_COMMENT = "TranscriptEditor.tierComment";
    public final static Color DEFAULT_TIER_COMMENT = Color.GRAY;

    public final static String INTERNAL_MEDIA = "TranscriptEditor.internalMedia";
    public final static Color DEFAULT_INTERNAL_MEDIA = Color.GRAY;

    public final static String IPA_WORD = "TranscriptEditor.ipa.words";
    public final static Color DEFAULT_IPA_WORD = Color.BLACK;

    public final static String IPA_PAUSE = "TranscriptEditor.ipa.pause";
    public final static Color DEFAULT_IPA_PAUSE = Color.GRAY;

    public final static String CLICKABLE_HOVER_UNDERLINE = "TranscriptEditor.clickableHoverUnderline";
    public final static Color DEFAULT_CLICKABLE_HOVER_UNDERLINE = Color.BLACK;

    public final static String SEPARATOR_LINE = "TranscriptEditor.separatorLine";
    public final static Color DEFAULT_SEPARATOR_LINE = Color.GRAY;


    public final static String ORTHOGRAPHY_LINKER = "TranscriptEditor.orthography.linker";
    public final static Color DEFAULT_ORTHOGRAPHY_LINKER = Color.GRAY;

    public final static String ORTHOGRAPHY_UTTERANCE_LANGUAGE = "TranscriptEditor.orthography.utteranceLanguage";
    public final static Color DEFAULT_ORTHOGRAPHY_UTTERANCE_LANGUAGE = Color.GRAY;

    public final static String ORTHOGRAPHY_WORD_PREFIX = "TranscriptEditor.orthography.word.prefix";
    public final static Color DEFAULT_ORTHOGRAPHY_WORD_PREFIX = Color.GRAY;

    public final static String ORTHOGRAPHY_WORD_SUFFIX = "TranscriptEditor.orthography.word.suffix";
    public final static Color DEFAULT_ORTHOGRAPHY_WORD_SUFFIX = Color.GRAY;

    public final static String ORTHOGRAPHY_WORD_REPLACEMENT = "TranscriptEditor.orthography.word.replacement";
    public final static Color DEFAULT_ORTHOGRAPHY_WORD_REPLACEMENT = Color.GRAY;

    public final static String ORTHOGRAPHY_GROUP_START = "TranscriptEditor.orthography.group.start";
    public final static Color DEFAULT_ORTHOGRAPHY_GROUP_START = Color.GRAY;

    public final static String ORTHOGRAPHY_GROUP_END = "TranscriptEditor.orthography.group.end";
    public final static Color DEFAULT_ORTHOGRAPHY_GROUP_END = Color.GRAY;

    public final static String ORTHOGRAPHY_PHONETIC_GROUP_START = "TranscriptEditor.orthography.phoneticGroup.start";
    public final static Color DEFAULT_ORTHOGRAPHY_PHONETIC_GROUP_START = Color.GRAY;

    public final static String ORTHOGRAPHY_PHONETIC_GROUP_END = "TranscriptEditor.orthography.phoneticGroup.end";
    public final static Color DEFAULT_ORTHOGRAPHY_PHONETIC_GROUP_END = Color.GRAY;

    public final static String ORTHOGRAPHY_QUOTATION = "TranscriptEditor.orthography.quotation";
    public final static Color DEFAULT_ORTHOGRAPHY_QUOTATION = Color.GRAY;

    public final static String ORTHOGRAPHY_PAUSE = "TranscriptEditor.orthography.pause";
    public final static Color DEFAULT_ORTHOGRAPHY_PAUSE = Color.GRAY;

    public final static String ORTHOGRAPHY_INTERNAL_MEDIA = "TranscriptEditor.orthography.internalMedia";
    public final static Color DEFAULT_ORTHOGRAPHY_INTERNAL_MEDIA = Color.GRAY;

    public final static String ORTHOGRAPHY_FREECODE = "TranscriptEditor.orthography.freecode";
    public final static Color DEFAULT_ORTHOGRAPHY_FREECODE = Color.GRAY;

    public final static String ORTHOGRAPHY_ACTION = "TranscriptEditor.orthography.action";
    public final static Color DEFAULT_ORTHOGRAPHY_ACTION = Color.GRAY;

    public final static String ORTHOGRAPHY_HAPPENING_PREFIX = "TranscriptEditor.orthography.happening.prefix";
    public final static Color DEFAULT_ORTHOGRAPHY_HAPPENING_PREFIX = Color.GRAY;

    public final static String ORTHOGRAPHY_HAPPENING = "TranscriptEditor.orthography.happening.text";
    public final static Color DEFAULT_ORTHOGRAPHY_HAPPENING = Color.GRAY;

    public final static String ORTHOGRAPHY_OTHER_SPOKEN_EVENT = "TranscriptEditor.orthography.otherSpokenEvent";
    public final static Color DEFAULT_ORTHOGRAPHY_OTHER_SPOKEN_EVENT = Color.GRAY;

    public final static String ORTHOGRAPHY_SEPARATOR = "TranscriptEditor.orthography.separator";
    public final static Color DEFAULT_ORTHOGRAPHY_SEPARATOR = Color.BLACK;

    public final static String ORTHOGRAPHY_TONE_MARKER = "TranscriptEditor.orthography.toneMarker";
    public final static Color DEFAULT_ORTHOGRAPHY_TONE_MARKER = Color.GRAY;

    public final static String ORTHOGRAPHY_TAG_MARKER = "TranscriptEditor.orthography.tagMarker";
    public final static Color DEFAULT_ORTHOGRAPHY_TAG_MARKER = Color.BLACK;

    public final static String ORTHOGRAPHY_OVERLAP_POINT = "TranscriptEditor.orthography.overlapPoint";
    public final static Color DEFAULT_ORTHOGRAPHY_OVERLAP_POINT = Color.GRAY;

    public final static String ORTHOGRAPHY_LONG_FEATURE = "TranscriptEditor.orthography.longFeature";
    public final static Color DEFAULT_ORTHOGRAPHY_LONG_FEATURE = Color.GRAY;

    public final static String ORTHOGRAPHY_NONVOCAL = "TranscriptEditor.orthography.nonvocal";
    public final static Color DEFAULT_ORTHOGRAPHY_NONVOCAL = Color.GRAY;

    public final static String ORTHOGRAPHY_TERMINATOR = "TranscriptEditor.orthography.terminator";
    public final static Color DEFAULT_ORTHOGRAPHY_TERMINATOR = Color.BLACK;

    public final static String ORTHOGRAPHY_POSTCODE = "TranscriptEditor.orthography.postcode";
    public final static Color DEFAULT_ORTHOGRAPHY_POSTCODE = Color.GRAY;

    public final static String ORTHOGRAPHY_FALLBACK = "TranscriptEditor.orthography.fallback";
    public final static Color DEFAULT_ORTHOGRAPHY_FALLBACK = Color.GRAY;

    public final static String ORTHOGRAPHY_WORD_TEXT = "TranscriptEditor.orthography.word.text";
    public final static Color DEFAULT_ORTHOGRAPHY_WORD_TEXT = Color.BLACK;

    public final static String ORTHOGRAPHY_CA_DELIMITER = "TranscriptEditor.orthography.word.ca.delimiter";
    public final static Color DEFAULT_ORTHOGRAPHY_CA_DELIMITER = Color.GRAY;

    public final static String ORTHOGRAPHY_CA_ELEMENT = "TranscriptEditor.orthography.word.ca.element";
    public final static Color DEFAULT_ORTHOGRAPHY_CA_ELEMENT = Color.GRAY;

    public final static String ORTHOGRAPHY_PROSODY = "TranscriptEditor.orthography.word.prosody";
    public final static Color DEFAULT_ORTHOGRAPHY_PROSODY = Color.GRAY;

    public final static String ORTHOGRAPHY_SHORTENING = "TranscriptEditor.orthography.word.shortening";
    public final static Color DEFAULT_ORTHOGRAPHY_SHORTENING = Color.GRAY;

    public final static String ORTHOGRAPHY_COMPOUND_WORD_MARKER = "TranscriptEditor.orthography.word.compoundWordMarker";
    public final static Color DEFAULT_ORTHOGRAPHY_COMPOUND_WORD_MARKER = Color.GRAY;

    public final static String ORTHOGRAPHY_DURATION = "TranscriptEditor.orthography.annotation.duration";
    public final static Color DEFAULT_ORTHOGRAPHY_DURATION = Color.GRAY;

    public final static String ORTHOGRAPHY_ERROR = "TranscriptEditor.orthography.annotation.error";
    public final static Color DEFAULT_ORTHOGRAPHY_ERROR = Color.GRAY;

    public final static String ORTHOGRAPHY_MARKER = "TranscriptEditor.orthography.annotation.marker";
    public final static Color DEFAULT_ORTHOGRAPHY_MARKER = Color.GRAY;

    public final static String ORTHOGRAPHY_GROUP_ANNOTATION = "TranscriptEditor.orthography.annotation.groupAnnotation";
    public final static Color DEFAULT_ORTHOGRAPHY_GROUP_ANNOTATION = Color.GRAY;

    public final static String ORTHOGRAPHY_OVERLAP = "TranscriptEditor.orthography.annotation.overlap";
    public final static Color DEFAULT_ORTHOGRAPHY_OVERLAP = Color.GRAY;

    public final static String SEGMENT_SELECTION = "TranscriptEditor.segment.selection";
    public final static Color DEFAULT_SEGMENT_SELECTION = Color.BLUE;

    public final static String COMMENT_BACKGROUND = "TranscriptEditor.commentBackground";
    public final static Color DEFAULT_COMMENT_BACKGROUND = Color.decode("#ffffbf");

    public final static String GEM_BACKGROUND = "TranscriptEditor.gemBackground";
    public final static Color DEFAULT_GEM_BACKGROUND = new Color(225, 243, 252, 255);

    public final static String GENERIC_BACKGROUND = "TranscriptEditor.genericBackground";
    public final static Color DEFAULT_GENERIC_BACKGROUND = new Color(252, 248, 241, 255);

    public final static String BLIND_TRANSCRIPTION_FOREGROUND = "TranscriptEditor.blindTranscriptionForeground";
    public final static Color DEFAULT_BLIND_TRANSCRIPTION_FOREGROUND = new Color(34, 49, 29);

    public final static String RECORD_BORDER_COLOR = "TranscriptEditor.recordBorderColor";

    public final static Color DEFAULT_RECORD_BORDER_COLOR = UIManager.getColor("Button.background");


    // region TranscriptScrollPaneGutter
    public final static String SCROLL_PANE_GUTTER_BACKGROUND = "TranscriptScrollPaneGutter.background";
    public final static Color DEFAULT_SCROLL_PANE_GUTTER_BACKGROUND = UIManager.getColor("control");

    public final static String SCROLL_PANE_GUTTER_FOREGROUND = "TranscriptScrollPaneGutter.foreground";
    public final static Color DEFAULT_SCROLL_PANE_GUTTER_FOREGROUND = UIManager.getColor("controlText");

    public final static String SCROLL_PANE_GUTTER_FONT = "TranscriptScrollPaneGutter.font";
    public final static Font DEFAULT_SCROLL_PANE_GUTTER_FONT = UIManager.getFont("Label.font");

    public final static String SCROLL_PANE_GUTTER_CURRENT_RECORD_BACKGROUND = "TranscriptScrollPaneGutter.currentRecordBackground";
    public final static Color DEFAULT_SCROLL_PANE_GUTTER_CURRENT_RECORD_BACKGROUND = Color.decode("#f8f8f8");

    public final static String SCROLL_PANE_GUTTER_CURRENT_TIER_BACKGROUND = "TranscriptScrollPaneGutter.currentTierBackground";
    public final static Color DEFAULT_SCROLL_PANE_GUTTER_CURRENT_TIER_BACKGROUND = Color.decode("#e0e0e0");

    public final static String SCROLL_PANE_GUTTER_BORDER_COLOR = "TranscriptScrollPaneGutter.borderColor";
    public final static Color DEFAULT_SCROLL_PANE_GUTTER_BORDER_COLOR = UIManager.getColor("controlShadow");
    // endregion

    @Override
    public void setupDefaults(UIDefaults uiDefaults) {
        uiDefaults.put(BACKGROUND, DEFAULT_BACKGROUND);
        uiDefaults.put(FOREGROUND, DEFAULT_FOREGROUND);
        uiDefaults.put(LABEL_BACKGROUND, DEFAULT_LABEL_BACKGROUND);
        uiDefaults.put(SEGMENT_DASH, DEFAULT_SEGMENT_DASH);
        uiDefaults.put(TIER_COMMENT, DEFAULT_TIER_COMMENT);
        uiDefaults.put(INTERNAL_MEDIA, DEFAULT_INTERNAL_MEDIA);
        uiDefaults.put(IPA_WORD, DEFAULT_IPA_WORD);
        uiDefaults.put(IPA_PAUSE, DEFAULT_IPA_PAUSE);
        uiDefaults.put(CLICKABLE_HOVER_UNDERLINE, DEFAULT_CLICKABLE_HOVER_UNDERLINE);
        uiDefaults.put(SEPARATOR_LINE, DEFAULT_SEPARATOR_LINE);

        uiDefaults.put(ORTHOGRAPHY_LINKER, DEFAULT_ORTHOGRAPHY_LINKER);
        uiDefaults.put(ORTHOGRAPHY_UTTERANCE_LANGUAGE, DEFAULT_ORTHOGRAPHY_UTTERANCE_LANGUAGE);
        uiDefaults.put(ORTHOGRAPHY_WORD_PREFIX, DEFAULT_ORTHOGRAPHY_WORD_PREFIX);
        uiDefaults.put(ORTHOGRAPHY_WORD_SUFFIX, DEFAULT_ORTHOGRAPHY_WORD_SUFFIX);
        uiDefaults.put(ORTHOGRAPHY_WORD_REPLACEMENT, DEFAULT_ORTHOGRAPHY_WORD_REPLACEMENT);
        uiDefaults.put(ORTHOGRAPHY_GROUP_START, DEFAULT_ORTHOGRAPHY_GROUP_START);
        uiDefaults.put(ORTHOGRAPHY_GROUP_END, DEFAULT_ORTHOGRAPHY_GROUP_END);
        uiDefaults.put(ORTHOGRAPHY_PHONETIC_GROUP_START, DEFAULT_ORTHOGRAPHY_PHONETIC_GROUP_START);
        uiDefaults.put(ORTHOGRAPHY_PHONETIC_GROUP_END, DEFAULT_ORTHOGRAPHY_PHONETIC_GROUP_END);
        uiDefaults.put(ORTHOGRAPHY_QUOTATION, DEFAULT_ORTHOGRAPHY_QUOTATION);
        uiDefaults.put(ORTHOGRAPHY_PAUSE, DEFAULT_ORTHOGRAPHY_PAUSE);
        uiDefaults.put(ORTHOGRAPHY_INTERNAL_MEDIA, DEFAULT_ORTHOGRAPHY_INTERNAL_MEDIA);
        uiDefaults.put(ORTHOGRAPHY_FREECODE, DEFAULT_ORTHOGRAPHY_FREECODE);
        uiDefaults.put(ORTHOGRAPHY_ACTION, DEFAULT_ORTHOGRAPHY_ACTION);
        uiDefaults.put(ORTHOGRAPHY_HAPPENING_PREFIX, DEFAULT_ORTHOGRAPHY_HAPPENING_PREFIX);
        uiDefaults.put(ORTHOGRAPHY_HAPPENING, DEFAULT_ORTHOGRAPHY_HAPPENING);
        uiDefaults.put(ORTHOGRAPHY_OTHER_SPOKEN_EVENT, DEFAULT_ORTHOGRAPHY_OTHER_SPOKEN_EVENT);
        uiDefaults.put(ORTHOGRAPHY_SEPARATOR, DEFAULT_ORTHOGRAPHY_SEPARATOR);
        uiDefaults.put(ORTHOGRAPHY_TONE_MARKER, DEFAULT_ORTHOGRAPHY_TONE_MARKER);
        uiDefaults.put(ORTHOGRAPHY_TAG_MARKER, DEFAULT_ORTHOGRAPHY_TAG_MARKER);
        uiDefaults.put(ORTHOGRAPHY_OVERLAP_POINT, DEFAULT_ORTHOGRAPHY_OVERLAP_POINT);
        uiDefaults.put(ORTHOGRAPHY_LONG_FEATURE, DEFAULT_ORTHOGRAPHY_LONG_FEATURE);
        uiDefaults.put(ORTHOGRAPHY_NONVOCAL, DEFAULT_ORTHOGRAPHY_NONVOCAL);
        uiDefaults.put(ORTHOGRAPHY_TERMINATOR, DEFAULT_ORTHOGRAPHY_TERMINATOR);
        uiDefaults.put(ORTHOGRAPHY_POSTCODE, DEFAULT_ORTHOGRAPHY_POSTCODE);
        uiDefaults.put(ORTHOGRAPHY_FALLBACK, DEFAULT_ORTHOGRAPHY_FALLBACK);
        uiDefaults.put(ORTHOGRAPHY_WORD_TEXT, DEFAULT_ORTHOGRAPHY_WORD_TEXT);
        uiDefaults.put(ORTHOGRAPHY_CA_DELIMITER, DEFAULT_ORTHOGRAPHY_CA_DELIMITER);
        uiDefaults.put(ORTHOGRAPHY_CA_ELEMENT, DEFAULT_ORTHOGRAPHY_CA_ELEMENT);
        uiDefaults.put(ORTHOGRAPHY_PROSODY, DEFAULT_ORTHOGRAPHY_PROSODY);
        uiDefaults.put(ORTHOGRAPHY_SHORTENING, DEFAULT_ORTHOGRAPHY_SHORTENING);
        uiDefaults.put(ORTHOGRAPHY_COMPOUND_WORD_MARKER, DEFAULT_ORTHOGRAPHY_COMPOUND_WORD_MARKER);
        uiDefaults.put(ORTHOGRAPHY_DURATION, DEFAULT_ORTHOGRAPHY_DURATION);
        uiDefaults.put(ORTHOGRAPHY_ERROR, DEFAULT_ORTHOGRAPHY_ERROR);
        uiDefaults.put(ORTHOGRAPHY_MARKER, DEFAULT_ORTHOGRAPHY_MARKER);
        uiDefaults.put(ORTHOGRAPHY_GROUP_ANNOTATION, DEFAULT_ORTHOGRAPHY_GROUP_ANNOTATION);
        uiDefaults.put(ORTHOGRAPHY_OVERLAP, DEFAULT_ORTHOGRAPHY_OVERLAP);

        uiDefaults.put(SEGMENT_SELECTION, DEFAULT_SEGMENT_SELECTION);

        uiDefaults.put(COMMENT_BACKGROUND, DEFAULT_COMMENT_BACKGROUND);
        uiDefaults.put(GEM_BACKGROUND, DEFAULT_GEM_BACKGROUND);
        uiDefaults.put(GENERIC_BACKGROUND, DEFAULT_GENERIC_BACKGROUND);

        uiDefaults.put(BLIND_TRANSCRIPTION_FOREGROUND, DEFAULT_BLIND_TRANSCRIPTION_FOREGROUND);
        uiDefaults.put(RECORD_BORDER_COLOR, DEFAULT_RECORD_BORDER_COLOR);

        uiDefaults.put(SCROLL_PANE_GUTTER_BACKGROUND, DEFAULT_SCROLL_PANE_GUTTER_BACKGROUND);
        uiDefaults.put(SCROLL_PANE_GUTTER_FOREGROUND, DEFAULT_SCROLL_PANE_GUTTER_FOREGROUND);
        uiDefaults.put(SCROLL_PANE_GUTTER_FONT, DEFAULT_SCROLL_PANE_GUTTER_FONT);
        uiDefaults.put(SCROLL_PANE_GUTTER_CURRENT_RECORD_BACKGROUND, DEFAULT_SCROLL_PANE_GUTTER_CURRENT_RECORD_BACKGROUND);
        uiDefaults.put(SCROLL_PANE_GUTTER_CURRENT_TIER_BACKGROUND, DEFAULT_SCROLL_PANE_GUTTER_CURRENT_TIER_BACKGROUND);
        uiDefaults.put(SCROLL_PANE_GUTTER_BORDER_COLOR, DEFAULT_SCROLL_PANE_GUTTER_BORDER_COLOR);
    }

    @Override
    public Class<?> getExtensionType() {
        return UIDefaultsHandler.class;
    }

    @Override
    public IPluginExtensionFactory<UIDefaultsHandler> getFactory() {
        return args -> this;
    }

}
