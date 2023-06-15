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

package ca.phon.app.session.editor.view.common;

import ca.phon.app.session.editor.SessionEditor;
import ca.phon.formatter.Formatter;
import ca.phon.formatter.*;
import ca.phon.session.Record;
import ca.phon.session.*;
import ca.phon.session.check.SegmentOverlapCheck;
import ca.phon.ui.action.PhonUIAction;
import ca.phon.util.PrefHelper;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Editor for media segments.
 */
public class MediaSegmentTierComponent extends JComponent implements TierEditor<MediaSegment> {

    private final static String DEFAULT_SEGMENT_TEXT = "000:00.000-000:00.000";

    private final WeakReference<SessionEditor> editorRef;

    private final Record record;

    private Tier<MediaSegment> segmentTier;

    private final SegmentField segmentField;

    public MediaSegmentTierComponent(SessionEditor editor, Record record, Tier<MediaSegment> tier) {
        super();
        setOpaque(false);
        setFocusable(false);

        this.editorRef = new WeakReference<SessionEditor>(editor);
        this.record = record;
        this.segmentTier = tier;
        segmentTier.addTierListener(tierListener);

        segmentField = new SegmentField() {

            public void validateText() {
                super.validateText();
                MediaSegmentTierComponent.this.validateText();
            }

        };

        updateText();
        //validateText();
        segmentField.getDocument().addDocumentListener(docListener);

        // validate text when 'enter' is pressed
        final ActionMap actionMap = segmentField.getActionMap();
        final InputMap inputMap = segmentField.getInputMap(JComponent.WHEN_FOCUSED);

        final KeyStroke validateKs = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        final String validateId = "validate";
        final PhonUIAction<Void> validateAct = PhonUIAction.runnable(this::onEnter);
        actionMap.put(validateId, validateAct);
        inputMap.put(validateKs, validateId);

        segmentField.setActionMap(actionMap);
        segmentField.setInputMap(JComponent.WHEN_FOCUSED, inputMap);

        segmentField.addFocusListener(focusListener);

        setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        add(segmentField);
    }

    public GroupFieldBorder getGroupFieldBorder() {
        return (GroupFieldBorder)segmentField.getBorder();
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        segmentField.setFont(font);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        segmentField.setEnabled(enabled);
    }

    private SessionEditor getEditor() {
        return editorRef.get();
    }

    /**
     * Validate text contents
     *
     * @return <code>true</code> if the contents of the field
     *  are valid, <code>false</code> otherwise.
     */
    private final AtomicReference<MediaSegment> validatedObjRef = new AtomicReference<MediaSegment>();
    protected boolean validateText() {
        boolean retVal = true;

        final String text = segmentField.getText();

        // look for a formatter
        final Formatter<MediaSegment> formatter = FormatterFactory.createFormatter(MediaSegment.class);

        try {
            final MediaSegment validatedObj = formatter.parse(text);
            if(validatedObj.getEndValue() >= validatedObj.getStartValue()) {
                setValidatedObject(validatedObj);
            } else {
                retVal = false;
            }
            getGroupFieldBorder().setShowWarningIcon(false);
            segmentField.setToolTipText(null);
        } catch (ParseException e) {
            getGroupFieldBorder().setShowWarningIcon(true);
            segmentField.setToolTipText(e.getLocalizedMessage());
            retVal = false;
        }

        // check if segment overlaps with previous record
        final int tolerance = PrefHelper.getInt(SegmentOverlapCheck.OVERLAP_TOLERANCE_PROPERTY, SegmentOverlapCheck.DEFAULT_OVERLAP_TOLERANCE);
        MediaSegment validated = getValidatedObject();
        if(getEditor().getCurrentRecordIndex() > 0) {
            Record currentRecord = getEditor().currentRecord();
            int idx = getEditor().getCurrentRecordIndex()-1;
            Record prevRecord = null;
            while(idx >= 0 && prevRecord == null) {
                Record r = getEditor().getSession().getRecord(idx--);
                if(r.getSpeaker() == currentRecord.getSpeaker())
                    prevRecord = r;
            }
            if(prevRecord != null) {
                MediaSegment prevSegment = prevRecord.getMediaSegment();
                if(prevSegment != null) {
                    final float diffMs = validated.getStartValue() - prevSegment.getEndValue();

                    if( (diffMs < 0) && (Math.abs(diffMs) > tolerance) ) {
                        // XXX Border does not update properly while typing
                        getGroupFieldBorder().setShowWarningIcon(true);
                        segmentField.setToolTipText("Segment overlaps with previous record for " + prevRecord.getSpeaker() + " (#" + (idx+2) + ")");
                        repaint();
                    }
                }
            }
        }

        return retVal;
    }

    public void validateAndUpdate() {
        if(validateText())
            updateTier();
    }

    protected MediaSegment getValidatedObject() {
        return this.validatedObjRef.get();
    }

    protected void setValidatedObject(MediaSegment object) {
        this.validatedObjRef.getAndSet(object);
    }

    public void updateText() {
        final MediaSegment segment = getSegment();
        final Formatter<MediaSegment> segmentFormatter = FormatterFactory.createFormatter(MediaSegment.class);

        String tierTxt =
                (segmentFormatter != null ? segmentFormatter.format(segment) : DEFAULT_SEGMENT_TEXT);
        segmentField.setText(tierTxt);
    }

    public void onEnter() {
        if(!getSegment().equals(initialGroupVal)) {
            for(TierEditorListener<MediaSegment> listener:getTierEditorListeners()) {
                listener.tierValueChanged(segmentTier, getValidatedObject(), initialGroupVal, false);
            }
            initialGroupVal.setSegment(getSegment());
        }
    }

    private void updateTier() {
        final MediaSegment oldVal = getSegment();
        final MediaSegment newVal = getValidatedObject();
        if(newVal != null) {
            for(TierEditorListener listener:listeners) {
                listener.tierValueChanged(segmentTier, newVal, oldVal, true);
            }
        }
    }

    private MediaSegment getSegment() {
        return segmentTier.getValue();
    }

    private MediaSegment initialGroupVal = SessionFactory.newFactory().createMediaSegment();
    private final FocusListener focusListener = new FocusListener() {

        @Override
        public void focusLost(FocusEvent e) {
            if(!getSegment().equals(initialGroupVal)) {
                for(TierEditorListener listener:getTierEditorListeners()) {
                    listener.tierValueChanged(segmentTier, getValidatedObject(), initialGroupVal, false);
                }
            }
        }

        @Override
        public void focusGained(FocusEvent e) {
            initialGroupVal.setSegment(getSegment());
        }

    };

    private final DocumentListener docListener = new DocumentListener() {

        @Override
        public void insertUpdate(DocumentEvent de) {
            if(validateText() && segmentField.hasFocus())
                updateTier();
        }

        @Override
        public void removeUpdate(DocumentEvent de) {
        }

        @Override
        public void changedUpdate(DocumentEvent de) {
        }

    };

    private final TierListener<MediaSegment> tierListener = new TierListener<>() {
        @Override
        public void tierValueChanged(Tier<MediaSegment> tier, MediaSegment oldValue, MediaSegment newValue) {
            if(!segmentField.hasFocus()) {
                updateText();
            }
            validateText();
        }
    };

    @Override
    public JComponent getEditorComponent() {
        return this;
    }

    private final List<TierEditorListener<MediaSegment>> listeners =
            Collections.synchronizedList(new ArrayList<>());

    @Override
    public void addTierEditorListener(TierEditorListener<MediaSegment> listener) {
        if(!listeners.contains(listener))
            listeners.add(listener);
    }

    @Override
    public void removeTierEditorListener(TierEditorListener<MediaSegment> listener) {
        listeners.remove(listener);
    }

    @Override
    public List<TierEditorListener<MediaSegment>> getTierEditorListeners() {
        return listeners;
    }

}