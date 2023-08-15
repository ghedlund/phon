package ca.phon.app.session.editor.transcriptEditor;

import ca.phon.formatter.MediaTimeFormatter;
import ca.phon.ipa.IPATranscript;
import ca.phon.ipa.alignment.PhoneMap;
import ca.phon.session.*;
import ca.phon.session.Record;
import ca.phon.ui.fonts.FontPreferences;
import ca.phon.ui.ipa.PhoneMapDisplay;
import ca.phon.ui.ipa.SyllabificationDisplay;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.*;
import java.util.List;

public class TranscriptDocument extends DefaultStyledDocument {
    private final Session session;
    private OpenLabelPopup openLabelPopup;
    private boolean targetSyllablesVisible = false;
    private boolean actualSyllablesVisible = false;
    private boolean alignmentVisible = false;

    public int getLabelMaxWidth() {
        return labelMaxWidth;
    }

    private int labelMaxWidth = -1;

    // region Get/Set Caret Pos

    public interface OpenLabelPopup {
        public void op(JLabel tierLabel, MouseEvent e);
    }

    public void setOpenLabelPopup(OpenLabelPopup lambda) {
        openLabelPopup = lambda;
    }

    // endregion Get/Set Caret Pos

    // region Visible Getters/Setters

    public boolean getTargetSyllablesVisible() {
        return targetSyllablesVisible;
    }

    public void setTargetSyllablesVisible(boolean targetSyllablesVisible) {
        this.targetSyllablesVisible = targetSyllablesVisible;
        reload();
    }

    public boolean getActualSyllablesVisible() {
        return actualSyllablesVisible;
    }

    public void setActualSyllablesVisible(boolean actualSyllablesVisible) {
        this.actualSyllablesVisible = actualSyllablesVisible;
        reload();
    }

    public boolean getAlignmentVisible() {
        return alignmentVisible;
    }

    public void setAlignmentVisible(boolean alignmentVisible) {
        if (alignmentVisible != this.alignmentVisible) {
            this.alignmentVisible = alignmentVisible;
            reload();
        }
    }

    // endregion Visible Getters/Setters

    public TranscriptDocument(Session session) {
        super(new StyleContext());
        this.session = session;

        try {
            populate();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private SimpleAttributeSet getTierAttributes(Tier tier) {
        final SimpleAttributeSet retVal = new SimpleAttributeSet();
        retVal.addAttribute("tier", tier);
        StyleConstants.setFontFamily(retVal, FontPreferences.TIER_FONT);
        return retVal;
    }

    private SimpleAttributeSet getIPATierAttributes(Tier<IPATranscript> tier) {
        final SimpleAttributeSet retVal = new SimpleAttributeSet();
        retVal.addAttribute("tier", tier);

        SyllabificationDisplay display = new SyllabificationDisplay();
        display.setTranscript(tier.getValue());
        StyleConstants.setComponent(retVal, display);

        return retVal;
    }

    private SimpleAttributeSet getSegmentTimeAttributes() {
        SimpleAttributeSet retVal = new SimpleAttributeSet();

        return retVal;
    }

    private SimpleAttributeSet getSegmentDashAttributes() {
        SimpleAttributeSet retVal = new SimpleAttributeSet();

        StyleConstants.setForeground(retVal, Color.GRAY);

        return retVal;
    }

    private SimpleAttributeSet getAlignmentAttributes(PhoneAlignment alignment) {
        SimpleAttributeSet retVal = new SimpleAttributeSet();

        PhoneMapDisplay display = new PhoneMapDisplay();
        int wordIndex = 0;
        for (Iterator<PhoneMap> i = alignment.iterator(); i.hasNext();) {
            var phoneMap = i.next();
            display.setPhoneMapForWord(wordIndex, phoneMap);
            wordIndex++;
        }
        StyleConstants.setComponent(retVal, display);

        return retVal;
    }

    private SimpleAttributeSet getTierLabelAttributes(String tierName) {
        final SimpleAttributeSet retVal = new SimpleAttributeSet();
        JLabel tierLabel = new JLabel(tierName + ":");
        var labelFont = new Font(tierLabel.getFont().getFontName(), tierLabel.getFont().getStyle(), 12);
        tierLabel.setFont(labelFont);
        tierLabel.setAlignmentY(.8f);
        tierLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        EmptyBorder tierLabelPadding = new EmptyBorder(0,8,0,8);
        tierLabel.setBorder(tierLabelPadding);
        labelMaxWidth = Math.max(
            labelMaxWidth,
            tierLabel.getPreferredSize().width +
                tierLabelPadding.getBorderInsets().left +
                tierLabelPadding.getBorderInsets().right
        );
        tierLabel.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openLabelPopup.op(tierLabel, e);
            }

            // region unused

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }

            // endregion unused
        });
        StyleConstants.setComponent(retVal, tierLabel);
        return retVal;
    }

    // region Hide/Move Tiers

    public void hideTier (int tierStartIndex, String tierName) {
        var startingTierView = session.getTierView();

        List<TierViewItem> newTierVew = new ArrayList<>();
        for (var tv : startingTierView) {
            newTierVew.add(tv);
        }
        newTierVew.remove(tierStartIndex);
        session.setTierView(newTierVew);
        // Reload the contents of the editor
        int recordCount = session.getRecordCount();
        for (int i = 0; i < recordCount; i++) {
            int startOffset = getTierStart(i, tierName) - 1;
            int endOffset = getTierEnd(i, tierName) + 1;
            try {
                remove(startOffset, endOffset - startOffset);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void moveTierUp (int tierStartIndex, String tierName, String aboveTierName) {
        var startingTierView = session.getTierView();

        // Move the tier
        List<TierViewItem> newTierVew = new ArrayList<>();
        for (var tv : startingTierView) {
            newTierVew.add(tv);
        }
        var movedTV = newTierVew.remove(tierStartIndex);
        newTierVew.add(tierStartIndex - 1, movedTV);
        session.setTierView(newTierVew);
        // Reload the contents of the editor
        int recordCount = session.getRecordCount();
        for (int i = 0; i < recordCount; i++) {
            try {

                int movingStartOffset = getTierStart(i, tierName) - 1;
                int movingEndOffset = getTierEnd(i, tierName) + 1;
                remove(movingStartOffset, movingEndOffset - movingStartOffset);

                int aboveStartOffset = getTierStart(i, aboveTierName) - 1;
                int aboveEndOffset = getTierEnd(i, aboveTierName) + 1;
                remove(aboveStartOffset, aboveEndOffset - aboveStartOffset);

                int newOffset = insertTier(i, tierName, aboveStartOffset);

                insertTier(i, aboveTierName, newOffset);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void moveTierDown (int tierStartIndex, String tierName, String belowTierName) {
        var startingTierView = session.getTierView();

        // Move the tier
        List<TierViewItem> newTierVew = new ArrayList<>();
        for (var tv : startingTierView) {
            newTierVew.add(tv);
        }
        var movedTV = newTierVew.remove(tierStartIndex);
        newTierVew.add(tierStartIndex + 1, movedTV);
        session.setTierView(newTierVew);
        // Reload the contents of the editor
        int recordCount = session.getRecordCount();
        for (int i = 0; i < recordCount; i++) {
            try {

                int belowStartOffset = getTierStart(i, belowTierName) - 1;
                int belowEndOffset = getTierEnd(i, belowTierName) + 1;
                remove(belowStartOffset, belowEndOffset - belowStartOffset);

                int movingStartOffset = getTierStart(i, tierName) - 1;
                int movingEndOffset = getTierEnd(i, tierName) + 1;
                remove(movingStartOffset, movingEndOffset - movingStartOffset);

                int newOffset = insertTier(i, belowTierName, movingStartOffset);

                insertTier(i, tierName, newOffset);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    // endregion Hide/Move Tiers

    private SimpleAttributeSet getSeparatorAttributes() {
        final SimpleAttributeSet retVal = new SimpleAttributeSet();
        StyleConstants.setComponent(retVal, new JSeparator(JSeparator.HORIZONTAL));
        return retVal;
    }

    public void reload() {
        try {
            // Remove the old stuff
            remove(0, getLength());
            // Put the new stuff back
            populate();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int getRecordIndex(int offset) {
        AttributeSet attributes = getCharacterElement(offset).getAttributes();
        var index = attributes.getAttribute("recordIndex");
        if (index == null) {
            return -1;
        }
        return (int)index;
    }

    public int getRecordElementIndex(int offset) {
        AttributeSet attributes = getCharacterElement(offset).getAttributes();
        var index = attributes.getAttribute("recordElementIndex");
        if (index == null) {
            return -1;
        }
        return (int)index;
    }

    public Tier getTier(int offset) {
        AttributeSet attributes = getCharacterElement(offset).getAttributes();
        var tier = attributes.getAttribute("tier");
        if (tier == null) {
            return null;
        }
        return ((Tier)tier);
    }

    // region Get Record/Tier Start/End

    public int getRecordStart(int recordIndex, String tierName) {
        Element root = getDefaultRootElement();

        int retVal = -1;

        for (int i = 0; i < root.getElementCount(); i++) {
            Element e = root.getElement(i);
            AttributeSet attrs = e.getAttributes();
            var currentRecordIndex = attrs.getAttribute("recordIndex");
            if (currentRecordIndex != null && recordIndex == (int)currentRecordIndex) {
                if (retVal == -1) {
                    retVal = e.getStartOffset();
                }
                else {
                    retVal = Math.min(retVal, e.getStartOffset());
                }
            }
        }

        return retVal;
    }

    public int getRecordStart(Tier tier) {
        Element root = getDefaultRootElement();

        int retVal = -1;

        for (int i = 0; i < root.getElementCount(); i++) {
            Element elem = root.getElement(i);
            var currentRecordIndex = elem.getAttributes().getAttribute("recordIndex");
            // If correct record index
            if (currentRecordIndex != null) {
                for (int j = 0; j < elem.getElementCount(); j++) {
                    Element innerElem = elem.getElement(j);
                    var currentTier = innerElem.getAttributes().getAttribute("tier");
                    // If correct tier
                    if (currentTier != null && currentTier == tier) {
                        if (retVal == -1) {
                            retVal = elem.getStartOffset();
                        }
                        else {
                            retVal = Math.min(retVal, elem.getStartOffset());
                        }
                    }
                }
            }
        }

        return retVal;
    }

    public int getRecordEnd(int recordIndex, String tierName) {
        Element root = getDefaultRootElement();

        int retVal = -1;

        for (int i = 0; i < root.getElementCount(); i++) {
            Element e = root.getElement(i);
            AttributeSet attrs = e.getAttributes();
            var currentRecordIndex = attrs.getAttribute("recordIndex");
            if (currentRecordIndex != null && recordIndex == (int)currentRecordIndex) {
                retVal = Math.max(retVal, e.getEndOffset());
            }
        }

        return retVal;
    }

    public int getRecordEnd(Tier tier) {
        Element root = getDefaultRootElement();

        for (int i = 0; i < root.getElementCount(); i++) {
            Element elem = root.getElement(i);
            var currentRecordIndex = elem.getAttributes().getAttribute("recordIndex");
            // If correct record index
            if (currentRecordIndex != null) {
                for (int j = 0; j < elem.getElementCount(); j++) {
                    Element innerElem = elem.getElement(j);
                    var currentTier = innerElem.getAttributes().getAttribute("tier");
                    // If correct tier
                    if (currentTier != null && currentTier == tier) {
                        return getRecordEnd((int)currentRecordIndex, tier.getName());
                    }
                }
            }
        }

        return -1;
    }

    public int getTierStart(int recordIndex, String tierName) {
        Element root = getDefaultRootElement();

        int retVal = -1;

        for (int i = 0; i < root.getElementCount(); i++) {
            Element elem = root.getElement(i);
            var currentRecordIndex = elem.getAttributes().getAttribute("recordIndex");
            // If correct record index
            if (currentRecordIndex != null && ((int)currentRecordIndex) == recordIndex) {
                for (int j = 0; j < elem.getElementCount(); j++) {
                    Element innerElem = elem.getElement(j);
                    var tier = innerElem.getAttributes().getAttribute("tier");
                    // If correct tier name
                    if (tier != null && ((Tier)tier).getName().equals(tierName)) {
                        if (retVal == -1) {
                            retVal = innerElem.getStartOffset();
                        }
                        else {
                            retVal = Math.min(retVal, innerElem.getStartOffset());
                        }
                    }
                }
            }
        }

        return retVal;
    }

    public int getTierStart(Tier tier) {
        Element root = getDefaultRootElement();

        int retVal = -1;

        for (int i = 0; i < root.getElementCount(); i++) {
            Element elem = root.getElement(i);
            var currentRecordIndex = elem.getAttributes().getAttribute("recordIndex");
            // If correct record index
            if (currentRecordIndex != null) {
                for (int j = 0; j < elem.getElementCount(); j++) {
                    Element innerElem = elem.getElement(j);
                    var currentTier = innerElem.getAttributes().getAttribute("tier");
                    // If correct tier
                    if (currentTier != null && currentTier == tier) {
                        if (retVal == -1) {
                            retVal = innerElem.getStartOffset();
                        }
                        else {
                            retVal = Math.min(retVal, innerElem.getStartOffset());
                        }
                    }
                }
            }
        }

        return retVal;
    }

    public int getTierEnd(int recordIndex, String tierName) {
        Element root = getDefaultRootElement();

        int retVal = -1;

        for (int i = 0; i < root.getElementCount(); i++) {
            Element elem = root.getElement(i);
            var currentRecordIndex = elem.getAttributes().getAttribute("recordIndex");
            // If correct record index
            if (currentRecordIndex != null && ((int)currentRecordIndex) == recordIndex) {
                for (int j = 0; j < elem.getElementCount(); j++) {
                    Element innerElem = elem.getElement(j);
                    var tier = innerElem.getAttributes().getAttribute("tier");
                    // If correct tier name
                    if (tier != null && ((Tier)tier).getName().equals(tierName)) {
                        retVal = Math.max(retVal, innerElem.getEndOffset());
                    }
                }
            }
        }

        return retVal;
    }

    public int getTierEnd(Tier tier) {
        Element root = getDefaultRootElement();

        for (int i = 0; i < root.getElementCount(); i++) {
            Element elem = root.getElement(i);
            var currentRecordIndex = elem.getAttributes().getAttribute("recordIndex");
            // If correct record index
            if (currentRecordIndex != null) {
                for (int j = 0; j < elem.getElementCount(); j++) {
                    Element innerElem = elem.getElement(j);
                    var currentTier = innerElem.getAttributes().getAttribute("tier");
                    // If correct tier
                    if (currentTier != null && currentTier == tier) {
                        return getTierEnd((int)currentRecordIndex, tier.getName());
                    }
                }
            }
        }

        return -1;
    }

    // endregion Get Record/Tier Start/End

    private int insertTier(int recordIndex, String tierName, int offset) throws BadLocationException {
        insertString(offset++, " ", getTierLabelAttributes(tierName));
        Record record = session.getRecord(recordIndex);
        Tier tier = record.getTier(tierName);

        if (tier == null) return offset;

        if (tierName.equals("IPA Target") && targetSyllablesVisible) {
            String ipaTarget = ((Tier<IPATranscript>)tier).getValue().toString(true);
            insertString(offset++, ipaTarget, getIPATierAttributes(tier));
        }
        else if (tierName.equals("IPA Actual") && actualSyllablesVisible) {
            String ipaActual = ((Tier<IPATranscript>)tier).getValue().toString(true);
            insertString(offset++, ipaActual, getIPATierAttributes(tier));
        }
        else if (tierName.equals("Alignment")) {
            insertString(offset++, " ", getAlignmentAttributes(record.getPhoneAlignment()));
        }
        else if (tierName.equals("Segment")) {
            MediaSegment segment = record.getMediaSegment();
            String start = MediaTimeFormatter.msToPaddedMinutesAndSeconds(segment.getStartValue());

            SimpleAttributeSet tierAttrs = getTierAttributes(tier);

            var segmentTimeAttrs = getSegmentTimeAttributes();
            segmentTimeAttrs.addAttributes(tierAttrs);

            insertString(offset, start, segmentTimeAttrs);
            offset += start.length();

            var segmentDashAttrs = getSegmentDashAttributes();
            segmentDashAttrs.addAttributes(tierAttrs);

            insertString(offset++, "-", segmentDashAttrs);

            String end = MediaTimeFormatter.msToPaddedMinutesAndSeconds(segment.getEndValue());

            insertString(offset, end, segmentTimeAttrs);
            offset += end.length();
        }
        else {
            String tierContent = tier.toString();
            if (tierContent.strip().equals("")) tierContent = "FILLER";

            SimpleAttributeSet tierAttrs = getTierAttributes(tier);
            insertString(offset, tierContent, tierAttrs);

            offset += tierContent.length();
        }

        insertString(offset++, "\n", null);

        return offset;
    }

    private void populate() throws BadLocationException {

        Transcript transcript = session.getTranscript();

        List<Record> records = session.getRecords().stream().toList();
        var tierView = session.getTierView();

        int len = 0;

        for (int i = 0; i < records.size(); i++) {
            int recordStart = len;
            Record record = records.get(i);
            int recordElementIndex = transcript.getRecordElementIndex(record);

            for (var tv : tierView) {
                if (!tv.isVisible()) continue;

                String tierName = tv.getTierName();

                len = insertTier(i, tierName, len);
            }

            if (i < records.size() - 1) {
                insertString(len, "-\n", getSeparatorAttributes());
                len += 2;
            }

            SimpleAttributeSet recordAttrs = new SimpleAttributeSet();
            recordAttrs.addAttribute("recordIndex", i);
            recordAttrs.addAttribute("recordElementIndex", recordElementIndex);
            setParagraphAttributes(recordStart, len - recordStart, recordAttrs, false);
        }
    }

    @Override
    protected void insert(int offset, ElementSpec[] data) throws BadLocationException {
        super.insert(offset, data);
    }

    @Override
    public void removeElement(Element elem) {
        super.removeElement(elem);
    }
}
