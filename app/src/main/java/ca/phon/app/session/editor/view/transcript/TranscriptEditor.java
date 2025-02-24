package ca.phon.app.session.editor.view.transcript;

import ca.phon.app.log.LogUtil;
import ca.phon.app.prefs.PhonProperties;
import ca.phon.app.session.editor.*;
import ca.phon.app.session.editor.undo.*;
import ca.phon.extensions.ExtensionSupport;
import ca.phon.extensions.IExtendable;
import ca.phon.extensions.UnvalidatedValue;
import ca.phon.ipamap2.*;
import ca.phon.orthography.Orthography;
import ca.phon.session.Record;
import ca.phon.session.*;
import ca.phon.session.position.TranscriptElementLocation;
import ca.phon.session.tierdata.TierData;
import ca.phon.ui.CalloutWindow;
import ca.phon.ui.CommonModuleFrame;
import ca.phon.ui.action.PhonActionEvent;
import ca.phon.ui.action.PhonUIAction;
import ca.phon.ui.fonts.FontPreferences;
import ca.phon.ui.ipamap.io.Cell;
import ca.phon.ui.ipamap.io.CellProp;
import ca.phon.ui.menu.MenuBuilder;
import ca.phon.util.PrefHelper;

import javax.management.Attribute;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

/**
 * A custom JEditorPane implementation for displaying and modifying Phon session transcripts.
 *
 */
public class TranscriptEditor extends JEditorPane implements IExtendable, ClipboardOwner {

    public final static EditorEventType<Void> transcriptDocumentPopulated = new EditorEventType<>("transcriptDocumentPopulated", Void.class);

    /**
     * The editor event type for the event that gets fired if the current record changes when the editor is in
     * "single record view" mode
     */
    public final static EditorEventType<Void> recordChangedInSingleRecordMode = new EditorEventType<>("recordChangedInSingleRecordMode", Void.class);

    /**
     * The editor event type for the session location change event
     */
    public final static EditorEventType<TranscriptLocationChangeData> transcriptLocationChanged = new EditorEventType<>("transcriptLocationChanged", TranscriptLocationChangeData.class);

    /* Editor models */
    private final EditorDataModel dataModel;

    private final EditorEventManager eventManager;

    /* Undo support */
    private final SessionEditUndoSupport undoSupport;

    private final UndoManager undoManager;

    /* Extension support */
    private final ExtensionSupport extensionSupport = new ExtensionSupport(TranscriptEditor.class, this);

    /* State */
    /**
     * The instance of {@link DefaultHighlighter.DefaultHighlightPainter} that acts as the debug highlight painter
     */
    private final DefaultHighlighter.DefaultHighlightPainter highlightPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);

    /**
     * An instance of {@link HoverUnderlinePainter}
     */
    private final HoverUnderlinePainter underlinePainter = new HoverUnderlinePainter();

    /**
     * A map of tiers with errors to their respective error underline highlight objects
     */
    private final Map<Tier<?>, Object> errorUnderlineHighlights = new HashMap<>();

    /**
     * An instance of {@link BoxSelectHighlightPainter}
     */
    private final BoxSelectHighlightPainter boxSelectPainter = new BoxSelectHighlightPainter();

    /**
     * A list containing references to all the current selection highlight objects
     */
    private final List<Object> selectionHighlightList = new ArrayList<>();

    private final Map<SessionEditorSelection, Object> selectionMap = new HashMap<>();

    /**
     * A set of attributes that the caret should not be able to move to
     */
    private final Set<String> notTraversableAttributes;
    private final EditorSelectionModel selectionModel;
    /**
     * The custom editor caret
     */
    private final TranscriptEditorCaret caret;
    private SessionMediaModel mediaModel;
    /**
     * A reference to the current debug highlight object
     */
    private Object currentHighlight;
    /**
     * The index of the current record
     */
    private int currentRecordIndex = -1;
    /**
     * A reference to the document element currently being hovered over
     */
    private Element hoverElem = null;
    /**
     * A reference to the current underline highlight object
     */
    private Object currentUnderline;
    /**
     * The stored offset from the label column for when the caret is being moved up and down with the arrow keys
     */
    private int upDownOffset = -1;
    /**
     * Whether the caret is currently moving up or down with the arrow keys
     */
    private boolean caretMoveFromUpDown = false;
    /**
     * A reference to the current box selection highlight object
     */
    private Object currentBoxSelect = null;
    /**
     * The session location of the current caret position
     */
    private TranscriptElementLocation currentTranscriptLocation = null;

    /**
     * Reference to our custom transcript editor kit
     */
    private final TranscriptEditorKit editorKit;

    /**
     * The current callout window being displayed (if any)
     */
    private AtomicReference<CalloutWindow> currentCallout = new AtomicReference<>();

    /**
     * Constructor
     */
    public TranscriptEditor(Session session, EditorEventManager eventManager, SessionEditUndoSupport undoSupport, UndoManager undoManager) {
        this(new DefaultEditorDataModel(null, session), new DefaultEditorSelectionModel(), eventManager, undoSupport, undoManager);
    }

    /**
     * Constructor
     */
    public TranscriptEditor(EditorDataModel dataModel, EditorSelectionModel selectionModel, EditorEventManager eventManager, SessionEditUndoSupport undoSupport, UndoManager undoManager) {
        super();

        caret = new TranscriptEditorCaret();
        super.setCaret(caret);
        this.dataModel = dataModel;
        this.selectionModel = selectionModel;
        this.eventManager = eventManager;
        this.undoSupport = undoSupport;
        this.undoManager = undoManager;
        initActions();
        registerEditorActions();
        this.editorKit = new TranscriptEditorKit(dataModel.getSession());
        super.setEditorKitForContentType(TranscriptEditorKit.CONTENT_TYPE, this.editorKit);
        setContentType(TranscriptEditorKit.CONTENT_TYPE);
        setNavigationFilter(new TranscriptNavigationFilter(this));
        TranscriptMouseAdapter mouseAdapter = new TranscriptMouseAdapter();
        addMouseMotionListener(mouseAdapter);
        addMouseListener(mouseAdapter);

        addCaretListener(e -> {
            TranscriptDocument doc = getTranscriptDocument();
            String transcriptElementType = (String) doc.getCharacterElement(e.getDot()).getAttributes().getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);
            if (transcriptElementType != null && transcriptElementType.equals(TranscriptStyleConstants.ATTR_KEY_RECORD)) {
                int dot = e.getDot();
                int recordIdx = doc.getRecordIndex(dot);
                if (recordIdx >= 0) {
                    setCurrentRecordIndex(recordIdx);
                }
            }
        });
        selectionModel.addSelectionModelListener(new TranscriptSelectionListener());

        notTraversableAttributes = new HashSet<>();
        notTraversableAttributes.add(TranscriptStyleConstants.ATTR_KEY_NOT_TRAVERSABLE);

        getTranscriptDocument().addDocumentPropertyChangeListener("populate", (e) -> {
            if (e.getNewValue() instanceof Boolean b && !b) {
                // update error highlights
                updateErrorHighlights();
            }
        });
        if (dataModel.getTranscriber() != Transcriber.VALIDATOR) {
            getTranscriptDocument().setTranscriber(dataModel.getTranscriber());
        }

        // save current changes on focus lost
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                saveCurrentLine();
            }
        });

        // init extensions
        extensionSupport.initExtensions();
    }

    /**
     * Constructor
     */
    public TranscriptEditor(Session session) {
        this(session, new EditorEventManager(), new SessionEditUndoSupport(), new UndoManager());
    }

    public TranscriptEditorCaret getTranscriptEditorCaret() {
        return caret;
    }

    /**
     * Sets up all the input actions
     */
    private void initActions() {
        InputMap inputMap = super.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = super.getActionMap();

        KeyStroke tab = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0);
        inputMap.put(tab, "nextTierOrElement");
        PhonUIAction<Void> tabAct = PhonUIAction.runnable(this::nextTierOrElement);
        actionMap.put("nextTierOrElement", tabAct);

        KeyStroke shiftTab = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, KeyEvent.SHIFT_DOWN_MASK);
        inputMap.put(shiftTab, "prevTierOrElement");
        PhonUIAction<Void> shiftTabAct = PhonUIAction.runnable(this::prevTierOrElement);
        actionMap.put("prevTierOrElement", shiftTabAct);

        KeyStroke right = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0);
        inputMap.put(right, "nextValidIndex");
        PhonUIAction<Void> rightAct = PhonUIAction.runnable(() -> {
            var caretLocation = getNextValidIndex(getCaretPosition() + 1, false);
            if (caretLocation >= 0) {
                setCaretPosition(caretLocation);
            }
        });
        actionMap.put("nextValidIndex", rightAct);

        KeyStroke left = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0);
        inputMap.put(left, "prevValidIndex");
        PhonUIAction<Void> leftAct = PhonUIAction.runnable(() -> {
            var caretLocation = getPrevValidIndex(getCaretPosition() - 1, false);
            if (caretLocation >= 0) {
                setCaretPosition(caretLocation);
            }
        });
        actionMap.put("prevValidIndex", leftAct);

        KeyStroke up = KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0);
        inputMap.put(up, "sameOffsetPrevTier");
        PhonUIAction<Void> upAct = PhonUIAction.runnable(this::sameOffsetInPrevTierOrElement);
        actionMap.put("sameOffsetPrevTier", upAct);

        KeyStroke down = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0);
        inputMap.put(down, "sameOffsetNextTier");
        PhonUIAction<Void> downAct = PhonUIAction.runnable(this::sameOffsetInNextTierOrElement);
        actionMap.put("sameOffsetNextTier", downAct);

        KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        inputMap.put(enter, "pressedEnter");
        PhonUIAction<Void> enterAct = PhonUIAction.eventConsumer(this::onPressedEnter, null);
        actionMap.put("pressedEnter", enterAct);

        KeyStroke home = KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0);
        inputMap.put(home, "pressedHome");
        PhonUIAction<Void> homeAct = PhonUIAction.runnable(this::onPressedHome);
        actionMap.put("pressedHome", homeAct);

        KeyStroke end = KeyStroke.getKeyStroke(KeyEvent.VK_END, 0);
        inputMap.put(end, "pressedEnd");
        PhonUIAction<Void> endAct = PhonUIAction.runnable(this::onPressedEnd);
        actionMap.put("pressedEnd", endAct);

//        KeyStroke delete = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
//        inputMap.put(delete, "deleteElement");
//        PhonUIAction<Void> deleteAct = PhonUIAction.runnable(() -> {
//            TranscriptDocument doc = getTranscriptDocument();
//
//            int currentPos = getCaretPosition();
//            var currentPosAttrs = getTranscriptDocument().getCharacterElement(currentPos).getAttributes();
//            String elementType = TranscriptStyleConstants.getElementType(currentPosAttrs);
//
//            boolean atEndOfTier = false;
//
//            switch (elementType) {
//                case "record" -> {
//                    currentPosAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_RECORD);
//                    Record currentRecord = TranscriptStyleConstants.getRecord(currentPosAttrs);
//                    if (currentRecord == null) return;
//                    int recordIndex = doc.getSession().getRecordPosition(currentRecord);
//                    Tier<?> tier = TranscriptStyleConstants.getTier(currentPosAttrs);
//                    if (tier == null) return;
//                    int endPos = doc.getTierEnd(recordIndex, tier.getName());
//
//                    atEndOfTier = currentPos + 1 == endPos;
//                }
//                case "comment" -> {
//                    Comment currentComment = TranscriptStyleConstants.getComment(currentPosAttrs);
//                    if (currentComment == null) return;
//                    int endPos = doc.getCommentEnd(currentComment);
//
//                    atEndOfTier = currentPos + 1 == endPos;
//                }
//                case "gem" -> {
//                    Gem currentGem = TranscriptStyleConstants.getGEM(currentPosAttrs);
//                    if (currentGem == null) return;
//                    int endPos = doc.getGemEnd(currentGem);
//
//                    atEndOfTier = currentPos + 1 == endPos;
//                }
//                case "generic" -> {
//                    Tier<?> currentGeneric = TranscriptStyleConstants.getGenericTier(currentPosAttrs);
//                    if (currentGeneric == null) return;
//                    int endPos = doc.getGenericEnd(currentGeneric);
//
//                    atEndOfTier = currentPos + 1 == endPos;
//                }
//            }
//
//            if (!atEndOfTier) {
//                try {
//                    getTranscriptDocument().remove(currentPos, 1);
//                } catch (BadLocationException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        });
//        actionMap.put("deleteElement", deleteAct);

        // show ipa character map
        KeyStroke inputCallout = KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0);
        KeyStroke inputCalloutKs2 = KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.CTRL_DOWN_MASK);
        inputMap.put(inputCallout, "showInputCallout");
        inputMap.put(inputCalloutKs2, "showInputCallout");

        PhonUIAction<Void> showInputCalloutAct = PhonUIAction.runnable(this::showInputCallout);
        actionMap.put("showInputCallout", showInputCalloutAct);
    }

    /**
     * Registers actions for all the necessary events
     */
    private void registerEditorActions() {
        this.eventManager.registerActionForEvent(EditorEventType.SessionChanged, this::onSessionChanged, EditorEventManager.RunOn.AWTEventDispatchThread);
        this.eventManager.registerActionForEvent(EditorEventType.TierViewChanged, this::onTierViewChanged, EditorEventManager.RunOn.AWTEventDispatchThread);
        this.eventManager.registerActionForEvent(EditorEventType.RecordChanged, this::onRecordChanged, EditorEventManager.RunOn.AWTEventDispatchThread);

        this.eventManager.registerActionForEvent(EditorEventType.RecordAdded, this::onRecordAdded, EditorEventManager.RunOn.AWTEventDispatchThread);
        this.eventManager.registerActionForEvent(EditorEventType.RecordDeleted, this::onRecordDeleted, EditorEventManager.RunOn.AWTEventDispatchThread);
        this.eventManager.registerActionForEvent(EditorEventType.RecordMoved, this::onRecordMoved, EditorEventManager.RunOn.AWTEventDispatchThread);

        this.eventManager.registerActionForEvent(EditorEventType.SpeakerChanged, this::onSpeakerChanged, EditorEventManager.RunOn.AWTEventDispatchThread);

        this.eventManager.registerActionForEvent(EditorEventType.TierChange, this::onTierDataChanged, EditorEventManager.RunOn.AWTEventDispatchThread);

        this.eventManager.registerActionForEvent(EditorEventType.CommentAdded, this::onCommentAdded, EditorEventManager.RunOn.AWTEventDispatchThread);
        this.eventManager.registerActionForEvent(EditorEventType.GemAdded, this::onGemAdded, EditorEventManager.RunOn.AWTEventDispatchThread);

        this.eventManager.registerActionForEvent(EditorEventType.ElementDeleted, this::onTranscriptElementDeleted, EditorEventManager.RunOn.AWTEventDispatchThread);

        this.eventManager.registerActionForEvent(EditorEventType.CommenTypeChanged, this::onCommentTypeChanged, EditorEventManager.RunOn.AWTEventDispatchThread);
        this.eventManager.registerActionForEvent(EditorEventType.GemTypeChanged, this::onGemTypeChanged, EditorEventManager.RunOn.AWTEventDispatchThread);

        this.eventManager.registerActionForEvent(transcriptLocationChanged, this::onSessionLocationChanged, EditorEventManager.RunOn.EditorEventDispatchThread);

        // TODO: Get this working
//        this.eventManager.registerActionForEvent(EditorEventType.ParticipantChanged, this::onParticipantChanged, EditorEventManager.RunOn.AWTEventDispatchThread);
    }

    public EditorDataModel getDataModel() {
        return dataModel;
    }

    public SessionMediaModel getMediaModel() {
        return this.mediaModel;
    }

    public void setMediaModel(SessionMediaModel mediaModel) {
        var oldModel = this.mediaModel;
        this.mediaModel = mediaModel;
        firePropertyChange("mediaModel", oldModel, mediaModel);
    }

    public SegmentPlayback getSegmentPlayback() {
        return (mediaModel != null ? mediaModel.getSegmentPlayback() : null);
    }

    public UndoManager getUndoManager() {
        return undoManager;
    }

    /**
     * Gets the record index of the carets current position
     *
     * @return the record index (returns -1 if caret not in a record)
     */
    public int getCurrentRecordIndex() {
        Element elem = getTranscriptDocument().getCharacterElement(getCaretPosition());
        Element firstInnerElem = elem.getElement(0);
        if (firstInnerElem != null) {
            Record record = (Record) firstInnerElem.getAttributes().getAttribute(TranscriptStyleConstants.ATTR_KEY_RECORD);
            if (record != null) {
                return getSession().getRecordPosition(record);
            }
        }

        Transcript transcript = getSession().getTranscript();
        for (int i = getCurrentElementIndex(); i < transcript.getNumberOfElements(); i++) {
            Transcript.Element transcriptElem = transcript.getElementAt(i);
            if (transcriptElem.isRecord()) {
                return transcript.getRecordPosition(transcriptElem.asRecord());
            }
        }

        return -1;
    }

    public TranscriptDocument getTranscriptDocument() {
        return (TranscriptDocument) getDocument();
    }

    public Session getSession() {
        return dataModel.getSession();
    }

    /**
     * Gets the transcript element index for the current caret pos
     *
     * @return the transcript element index
     */
    public int getCurrentElementIndex() {
        Element elem = getTranscriptDocument().getCharacterElement(getCaretPosition());

        if (elem == null) return -1;

        String elementType = (String) elem.getAttributes().getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);

        if (elementType == null) return -1;

        switch (elementType) {
            case TranscriptStyleConstants.ATTR_KEY_COMMENT -> {
                Comment comment = (Comment) elem.getAttributes().getAttribute(TranscriptStyleConstants.ATTR_KEY_COMMENT);
                return getSession().getTranscript().getElementIndex(comment);
            }
            case TranscriptStyleConstants.ATTR_KEY_GEM -> {
                Gem gem = (Gem) elem.getAttributes().getAttribute(TranscriptStyleConstants.ATTR_KEY_GEM);
                return getSession().getTranscript().getElementIndex(gem);
            }
            case TranscriptStyleConstants.ATTR_KEY_RECORD -> {
                Record record = (Record) elem.getAttributes().getAttribute(TranscriptStyleConstants.ATTR_KEY_RECORD);
                return getSession().getRecordElementIndex(record);
            }
            default -> {
                return -1;
            }
        }
    }

    /**
     * Moves the caret to the beginning of the transcript element at the index specified
     *
     * @param index the transcript element index
     */
    public void setCurrentElementIndex(int index) {

        Transcript.Element transcriptElem = getSession().getTranscript().getElementAt(index);
        String transcriptElemType;
        if (transcriptElem.isComment()) {
            transcriptElemType = TranscriptStyleConstants.ATTR_KEY_COMMENT;
        } else if (transcriptElem.isGem()) {
            transcriptElemType = TranscriptStyleConstants.ATTR_KEY_GEM;
        } else {
            transcriptElemType = TranscriptStyleConstants.ATTR_KEY_RECORD;
        }

        var root = getTranscriptDocument().getDefaultRootElement();
        for (int i = 0; i < root.getElementCount(); i++) {
            Element elem = root.getElement(i);
            for (int j = 0; j < elem.getElementCount(); j++) {
                Element innerElem = elem.getElement(j);
                String elemType = (String) innerElem.getAttributes().getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);
                if (elemType != null && elemType.equals(transcriptElemType)) {
                    if (transcriptElem.isComment()) {
                        Comment comment = (Comment) innerElem.getAttributes().getAttribute(TranscriptStyleConstants.ATTR_KEY_COMMENT);
                        if (comment.equals(transcriptElem.asComment())) {
                            setCaretPosition(innerElem.getStartOffset());
                        }
                    } else if (transcriptElem.isGem()) {
                        Gem gem = (Gem) innerElem.getAttributes().getAttribute(TranscriptStyleConstants.ATTR_KEY_GEM);
                        if (gem.equals(transcriptElem.asGem())) {
                            setCaretPosition(innerElem.getStartOffset());
                        }
                    } else {
                        Record record = (Record) innerElem.getAttributes().getAttribute(TranscriptStyleConstants.ATTR_KEY_RECORD);
                        if (record.equals(transcriptElem.asRecord())) {
                            setCaretPosition(innerElem.getStartOffset());
                        }
                    }
                }
            }
        }
    }

    /**
     * Moves the caret to the beginning of the record at the index specified
     */
    public void setCurrentRecordIndex(int index) {
        int oldIndex = this.currentRecordIndex;
        this.currentRecordIndex = index;
        super.firePropertyChange("currentRecordIndex", oldIndex, this.currentRecordIndex);
    }

    /**
     * Toggle single record view
     */
    public void toggleSingleRecordView() {
        setSingleRecordView(!isSingleRecordView());
    }

    /**
     * Is the document in single record view
     *
     * @return true if single record view, false otherwise
     */
    public boolean isSingleRecordView() {
        return getTranscriptDocument().getSingleRecordView();
    }

    /**
     * Sets whether the document is in single record view
     *
     * @param singleRecordView
     */
    public void setSingleRecordView(boolean singleRecordView) {
        var wasSingleRecordView = isSingleRecordView();
        getTranscriptDocument().setSingleRecordView(singleRecordView);
        if (wasSingleRecordView != singleRecordView)
            getTranscriptDocument().setSingleRecordView(singleRecordView);
        firePropertyChange("singleRecordView", wasSingleRecordView, singleRecordView);
    }

    /**
     * Checks if the current transcriber is the validator
     *
     * @return whether the transcriber is the validator
     */
    public boolean isTranscriberValidator() {
        return dataModel.getTranscriber() == Transcriber.VALIDATOR;
    }

//    /**
//     * Sets whether the next edit shouldn't cause any changed to the document
//     */
//    public void setInternalEdit(boolean value) {
//        internalEdit = value;
//    }

    public int getUpDownOffset() {
        return upDownOffset;
    }

    public void setUpDownOffset(int upDownOffset) {
        this.upDownOffset = upDownOffset;
    }

    public boolean isCaretMoveFromUpDown() {
        return caretMoveFromUpDown;
    }

    public void setCaretMoveFromUpDown(boolean caretMoveFromUpDown) {
        this.caretMoveFromUpDown = caretMoveFromUpDown;
    }

    /**
     * Moves the caret to the start of the next tier or transcript element
     */
    public void nextTierOrElement() {
        int caretPos = getCaretPosition();

        int newCaretPos = getStartOfNextTierOrElement(caretPos);
        if (newCaretPos == -1) return;

        setCaretPosition(newCaretPos);
    }

    /**
     * Moves the caret to the start of the previous tier or transcript element
     */
    public void prevTierOrElement() {
        int caretPos = getCaretPosition();

        int newCaretPos = getStartOfPrevTierOrElement(caretPos);
        if (newCaretPos == -1) return;

        setCaretPosition(newCaretPos);
    }

    /**
     * Commit changed tier data at caret location
     *
     */
    public void commitChanges(int charPos) {
        final TranscriptElementLocation loc = charPosToSessionLocation(charPos);
        if(!loc.valid()) return;
        if(loc.transcriptElementIndex() < 0) return;

        final Element charElem = getTranscriptDocument().getCharacterElement(charPos);
        final AttributeSet attrs = charElem.getAttributes();
        final Transcript.Element elem = getSession().getTranscript().getElementAt(loc.transcriptElementIndex());
        if(elem.isRecord()) {
            final Record record = elem.asRecord();
            final Tier<?> tier = TranscriptStyleConstants.getTier(attrs);
            final int recordIndex = getSession().getRecordPosition(record);
            final TranscriptDocument.StartEnd startEnd = getTranscriptDocument().getTierContentStartEnd(recordIndex, loc.tier());
            if(!startEnd.valid()) return;
            try {
                final String text = getTranscriptDocument().getText(startEnd.start(), startEnd.end() - startEnd.start());
                changeTierData(record, tier, text);
            } catch (BadLocationException e) {
                LogUtil.severe(e);
            }
        } else if(elem.isComment()) {
            final Comment comment = elem.asComment();
            final TranscriptDocument.StartEnd startEnd = getTranscriptDocument().getCommentContentStartEnd(comment);
            if(!startEnd.valid()) return;
            try {
                final String text = getTranscriptDocument().getText(startEnd.start(), startEnd.end() - startEnd.start());
                commentDataChanged(comment, text);
            } catch (BadLocationException e) {
                LogUtil.severe(e);
            }
        } else if(elem.isGem()) {
            final Gem gem = elem.asGem();
            final TranscriptDocument.StartEnd startEnd = getTranscriptDocument().getGemContentStartEnd(gem);
            if(!startEnd.valid()) return;
            try {
                final String text = getTranscriptDocument().getText(startEnd.start(), startEnd.end() - startEnd.start());
                gemDataChanged(gem, text);
            } catch (BadLocationException e) {
                LogUtil.severe(e);
            }
        }
    }

    /**
     * Runs when the user presses enter
     */
    public void onPressedEnter(PhonActionEvent<Void> pae) {

        TranscriptDocument doc = getTranscriptDocument();
        AttributeSet attrs = doc.getCharacterElement(getCaretPosition()).getAttributes();
        var enterAct = TranscriptStyleConstants.getEnterAction(attrs);
        if (enterAct != null) {
            enterAct.actionPerformed(pae.getActionEvent());
            return;
        }

        String elemType = TranscriptStyleConstants.getElementType(attrs);
        if (elemType != null) {
            saveCurrentLine();
        }
    }

    /**
     * Runs when the user presses home
     */
    public void onPressedHome() {
        TranscriptDocument doc = getTranscriptDocument();

        Element caretElem = doc.getCharacterElement(getCaretPosition());
        AttributeSet attrs = caretElem.getAttributes();
        String elementType = TranscriptStyleConstants.getElementType(attrs);
        if (elementType == null) return;
        int start = -1;
        switch (elementType) {
            case TranscriptStyleConstants.ATTR_KEY_RECORD -> {
                Record record = TranscriptStyleConstants.getRecord(attrs);
                int recordIndex = getSession().getRecordPosition(record);
                Tier<?> tier = TranscriptStyleConstants.getTier(attrs);
                if (tier != null && recordIndex >= 0) {
                    start = doc.getTierContentStart(recordIndex, tier.getName());
                }
            }
            case TranscriptStyleConstants.ATTR_KEY_COMMENT -> {
                Comment comment = TranscriptStyleConstants.getComment(attrs);
                if (comment != null) {
                    start = doc.getCommentContentStart(comment);
                }
            }
            case TranscriptStyleConstants.ATTR_KEY_GEM -> {
                Gem gem = TranscriptStyleConstants.getGEM(attrs);
                if (gem != null) {
                    start = doc.getGemContentStart(gem);
                }
            }
            case TranscriptStyleConstants.ATTR_KEY_GENERIC_TIER -> {
                Tier<?> genericTier = TranscriptStyleConstants.getGenericTier(attrs);
                if (genericTier != null) {
                    start = doc.getGenericContentStart(genericTier);
                }
            }
        }
        if (start != -1) {
            setCaretPosition(start);
        }
    }

    /**
     * Update error highlights
     */
    private void updateErrorHighlights() {
        TranscriptDocument doc = getTranscriptDocument();
        for (int i = 0; i < doc.getDefaultRootElement().getElementCount(); i++) {
            final Element elem = doc.getDefaultRootElement().getElement(i);
            final AttributeSet attrs = elem.getAttributes();
            final String elementType = TranscriptStyleConstants.getElementType(attrs);
            if (elementType == null) continue;
            Tier<?> tier = switch (elementType) {
                case TranscriptStyleConstants.ELEMENT_TYPE_RECORD -> TranscriptStyleConstants.getTier(attrs);
                case TranscriptStyleConstants.ELEMENT_TYPE_GENERIC -> TranscriptStyleConstants.getGenericTier(attrs);
                default -> null;
            };
            if (tier == null) continue;
            UnvalidatedValue uv = null;
            boolean isBlindMode = getDataModel().getTranscriber() != Transcriber.VALIDATOR;
            if (tier.isBlind() && isBlindMode) {
                if (tier.isBlindTranscriptionUnvalidated(getDataModel().getTranscriber().getUsername())) {
                    uv = tier.getBlindUnvalidatedValue(getDataModel().getTranscriber().getUsername());
                } else {
                    if (tier.isUnvalidated()) {
                        uv = tier.getUnvalidatedValue();
                    }
                }
            } else {
                uv = tier.isUnvalidated() ? tier.getUnvalidatedValue() : null;
            }

            final Record record = TranscriptStyleConstants.getRecord(attrs);
            final int recordIdx = record != null ? getSession().getRecordPosition(record) : -1;
            if (uv != null) {
                TranscriptDocument.StartEnd startEnd = switch (elementType) {
                    case TranscriptStyleConstants.ELEMENT_TYPE_RECORD ->
                            doc.getTierContentStartEnd(recordIdx, tier.getName());
                    case TranscriptStyleConstants.ELEMENT_TYPE_GENERIC -> doc.getGenericContentStartEnd(tier);
                    default -> new TranscriptDocument.StartEnd(-1, -1);
                };
                if (startEnd.valid()) {
                    try {
                        final int startIdx = uv.getParseError().getErrorOffset() >= 0 ? startEnd.start() + uv.getParseError().getErrorOffset() : startEnd.start();
                        var errorUnderlineHighlight = getHighlighter().addHighlight(startIdx, startEnd.end(), new ErrorUnderlinePainter());
                        errorUnderlineHighlights.put(tier, errorUnderlineHighlight);
                    } catch (BadLocationException e) {
                        LogUtil.warning(e);
                    }
                }
            }
        }
    }

    /**
     * Runs when the user presses end
     */
    public void onPressedEnd() {
        TranscriptDocument doc = getTranscriptDocument();

        Element caretElem = doc.getCharacterElement(getCaretPosition());
        AttributeSet attrs = caretElem.getAttributes();
        String elementType = TranscriptStyleConstants.getElementType(attrs);
        if (elementType == null) return;
        int end = -1;
        switch (elementType) {
            case TranscriptStyleConstants.ATTR_KEY_RECORD -> {
                Record record = TranscriptStyleConstants.getRecord(attrs);
                int recordIndex = getSession().getRecordPosition(record);
                Tier<?> tier = TranscriptStyleConstants.getTier(attrs);
                if (tier != null && recordIndex >= 0) {
                    end = doc.getTierContentEnd(recordIndex, tier.getName());
                }
            }
            case TranscriptStyleConstants.ATTR_KEY_COMMENT -> {
                Comment comment = TranscriptStyleConstants.getComment(attrs);
                if (comment != null) {
                    end = doc.getCommentContentEnd(comment);
                }
            }
            case TranscriptStyleConstants.ATTR_KEY_GEM -> {
                Gem gem = TranscriptStyleConstants.getGEM(attrs);
                if (gem != null) {
                    end = doc.getGemContentEnd(gem);
                }
            }
            case TranscriptStyleConstants.ATTR_KEY_GENERIC_TIER -> {
                Tier<?> genericTier = TranscriptStyleConstants.getGenericTier(attrs);
                if (genericTier != null) {
                    end = doc.getGenericContentEnd(genericTier);
                }
            }
        }
        if (end >= 0) {
            setCaretPosition(end);
        }
    }

    public String getFirstVisibleTierName() {
        return getSession().getTierView().stream().filter(TierViewItem::isVisible).findFirst().map(TierViewItem::getTierName).orElse(null);
    }

    /**
     * Runs when a new record gets added
     *
     * @param editorEvent the event that adds the record
     */
    private void onRecordAdded(EditorEvent<EditorEventType.RecordAddedData> editorEvent) {
        var data = editorEvent.data();
        // Get the new record and the element index
        Record addedRecord = getSession().getRecord(data.recordIndex());
        int elementIndex = data.elementIndex();
        // Add it to the doc
        if(isSingleRecordView()) {
            getTranscriptDocument().setSingleRecordIndex(-1);
            getTranscriptDocument().setSingleRecordIndex(editorEvent.data().recordIndex());
        } else {
            getTranscriptDocument().addRecord(addedRecord, elementIndex);
        }

        final TranscriptElementLocation newCaretLoc = new TranscriptElementLocation(elementIndex,
                getFirstVisibleTierName(), 0);
        if(newCaretLoc.valid()) {
            final int newDot = sessionLocationToCharPos(newCaretLoc);
            if(newDot >= 0) {
                setCaretPosition(newDot);
            }
        }
    }

    /**
     * Runs when a record gets deleted
     *
     * @param editorEvent the event that deletes the record
     */
    private void onRecordDeleted(EditorEvent<EditorEventType.RecordDeletedData> editorEvent) {
        TranscriptDocument doc = getTranscriptDocument();

        int deletedTranscriptElementIndex = editorEvent.data().elementIndex();

        var currentLocation = getTranscriptEditorCaret().getTranscriptLocation();
        if(currentLocation.transcriptElementIndex() == -2) {
            // we are inside deleted record
            currentLocation = new TranscriptElementLocation(deletedTranscriptElementIndex, currentLocation.tier(), currentLocation.charPosition());
        }

        // Delete the record from the doc
        var data = editorEvent.data();
        getTranscriptDocument().deleteRecord(data.elementIndex(), data.recordIndex(), data.record());

        // Caret in record / tier
        if(currentLocation.valid()) {
            TranscriptElementLocation nextLoc = currentLocation;
            if(deletedTranscriptElementIndex == currentLocation.transcriptElementIndex()) {
                if(currentLocation.transcriptElementIndex() < getSession().getTranscript().getNumberOfElements()) {
                    final Transcript.Element nextElem = getSession().getTranscript().getElementAt(currentLocation.transcriptElementIndex());
                    if(nextElem.isRecord()) {
                        // keep caret in same tier
                        nextLoc = new TranscriptElementLocation(currentLocation.transcriptElementIndex(), currentLocation.tier(), 0);
                    } else {
                        // move caret to beginning of comment/gem
                        nextLoc = new TranscriptElementLocation(currentLocation.transcriptElementIndex(), "", 0);
                    }
                } else {
                    nextLoc = new TranscriptElementLocation(currentLocation.transcriptElementIndex() - 1, null, 0);
                    if(nextLoc.transcriptElementIndex() < 0) {
                        nextLoc = new TranscriptElementLocation(-2, null, 0);
                    }
                }
            }
            if(nextLoc.valid()) {
                final int newDot = sessionLocationToCharPos(nextLoc);
                if(newDot >= 0) {
                    setCaretPosition(newDot);
                } else {
                    int prevValidIdx = getPrevValidIndex(getCaretPosition() - 1, false);
                    if(prevValidIdx >= 0) {
                        setCaretPosition(prevValidIdx);
                    } else {
                        setCaretPosition(0);
                    }
                }
            } else {
                int prevValidIdx = getPrevValidIndex(getCaretPosition() - 1, false);
                if(prevValidIdx >= 0) {
                    setCaretPosition(prevValidIdx);
                } else {
                    setCaretPosition(0);
                }
            }
        } else {
            int prevValidIdx = getPrevValidIndex(getCaretPosition() - 1, false);
            if(prevValidIdx >= 0) {
                setCaretPosition(prevValidIdx);
            } else {
                setCaretPosition(0);
            }
        }
    }

    /**
     * Runs when a record moves
     *
     * @param editorEvent the event that moves the record
     */
    private void onRecordMoved(EditorEvent<EditorEventType.RecordMovedData> editorEvent) {
        // Record caret pos
        int caretPos = getCaretPosition();

        // Move the records in the doc
        var data = editorEvent.data();
        getTranscriptDocument().moveRecord(data.fromRecordIndex(), data.toRecordIndex(), data.fromElementIndex(), data.toElementIndex());
    }

    /**
     * Runs when the speaker for a record changes
     *
     * @param editorEvent the event that changes the speaker
     */
    private void onSpeakerChanged(EditorEvent<EditorEventType.SpeakerChangedData> editorEvent) {
        var data = editorEvent.data();
        // Update the speaker on the separator in the doc
        final TranscriptElementLocation caretLoc = getTranscriptEditorCaret().getTranscriptLocation();
        getTranscriptEditorCaret().freeze();
        getTranscriptDocument().onChangeSpeaker(data.record());
        getTranscriptEditorCaret().unfreeze();
        // Set the caret position back to where it was
        final int newDot = sessionLocationToCharPos(caretLoc);
        if(newDot >= 0) {
            setCaretPosition(newDot);
        }
    }

    /**
     * Runs when the data for a tier changes
     *
     * @param editorEvent the event that changes the tiers data
     */
    private void onTierDataChanged(EditorEvent<EditorEventType.TierChangeData> editorEvent) {
        TranscriptDocument doc = getTranscriptDocument();
        Tier<?> changedTier = editorEvent.data().tier();
        // update media segment changes as they occur
        boolean isMediaSegmentTier = changedTier.getDeclaredType().equals(MediaSegment.class);
        if (!isMediaSegmentTier && editorEvent.getData().get().valueAdjusting()) return;

        if (errorUnderlineHighlights.containsKey(changedTier)) {
            getHighlighter().removeHighlight(errorUnderlineHighlights.get(changedTier));
            errorUnderlineHighlights.remove(changedTier);
        }

        int start = -1;
        int end = -1;

        if (changedTier.isUnvalidated()) {
            int recordIndex = doc.getSession().getRecordPosition(editorEvent.data().record());
            if (recordIndex < 0) return;
            TranscriptDocument.StartEnd se = doc.getTierContentStartEnd(recordIndex, changedTier.getName());
            start = se.start() + changedTier.getUnvalidatedValue().getParseError().getErrorOffset();
            end = se.start() + changedTier.getUnvalidatedValue().getValue().length();
        }

        final TranscriptElementLocation caretLoc = getTranscriptEditorCaret().getTranscriptLocation();
        final int currentDot = getTranscriptEditorCaret().getDot();

        if(PrefHelper.isDebugMode()) {
            LogUtil.info("Updating tier text: " + changedTier.getName());
        }
        boolean wasCaretFrozen = getTranscriptEditorCaret().isFreezeCaret();
        getTranscriptEditorCaret().freeze();
        // Update the changed tier data in the doc
        getTranscriptDocument().onTierDataChanged(editorEvent.data().record(), changedTier);
        final int newDot = sessionLocationToCharPos(caretLoc);
        if (newDot != currentDot) {
            getTranscriptDocument().setBypassDocumentFilter(true);
            getTranscriptEditorCaret().setDot(newDot, true);
            getTranscriptDocument().setBypassDocumentFilter(false);
        }
        if (!wasCaretFrozen) {
            getTranscriptEditorCaret().unfreeze();
        }

        if (changedTier.isUnvalidated()) {
            try {
                var errorUnderlineHighlight = getHighlighter().addHighlight(start, end, new ErrorUnderlinePainter());
                errorUnderlineHighlights.put(changedTier, errorUnderlineHighlight);
            } catch (BadLocationException e) {
                LogUtil.warning(e);
            }
        }
    }

    /**
     * Runs when the user clicks on the label for a blind transcription tier
     *
     * @param point       the point where the user clicks
     * @param record      the record that the tier belongs to
     * @param tier        the tier that the blind transcription belongs to
     * @param transcriber the transcriber that the blind transcription belongs to
     */
    private void onClickBlindTranscriptionLabel(Point2D point, Record record, Tier<?> tier, String transcriber) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem select = new JMenuItem();
        PhonUIAction<Void> selectAction = PhonUIAction.runnable(() -> {
            selectTranscription(record, tier, transcriber);
        });
        selectAction.putValue(PhonUIAction.NAME, "Select transcription");
        select.setAction(selectAction);
        menu.add(select);

        JMenuItem append = new JMenuItem();
        PhonUIAction<Void> appendAction = PhonUIAction.runnable(() -> {
            appendTranscription(record, tier, transcriber);
        });
        appendAction.putValue(PhonUIAction.NAME, "Append");
        append.setAction(appendAction);
        menu.add(append);

        menu.show(this, (int) point.getX(), (int) point.getY());
    }

    /**
     * Selects the transcription of a given transcriber to be the value for the given tier
     *
     * @param record      the record that the tier belongs to
     * @param tier        the tier that the transcription is being selected for
     * @param transcriber the name / id of the transcriber whose transcription is selected
     */
    private void selectTranscription(Record record, Tier<?> tier, String transcriber) {
        TranscriptDocument doc = getTranscriptDocument();

        Tier<?> dummy = SessionFactory.newFactory().createTier("dummy", tier.getDeclaredType());
        dummy.setText(doc.getTierText(tier, transcriber));

        SwingUtilities.invokeLater(() -> {
            TierEdit<?> edit = new TierEdit(dataModel.getSession(), eventManager, Transcriber.VALIDATOR, record, tier, dummy.getValue());
            edit.setValueAdjusting(false);
            undoSupport.postEdit(edit);
        });
    }

    /**
     * Appends the transcription of a given transcriber to the value of the given tier
     *
     * @param record      the record that the tier belongs to
     * @param tier        the tier that the transcription is being appended to
     * @param transcriber the name / id of the transcriber whose transcription is appended
     */
    private void appendTranscription(Record record, Tier<?> tier, String transcriber) {
        TranscriptDocument doc = getTranscriptDocument();

        Tier<?> dummy = SessionFactory.newFactory().createTier("dummy", tier.getDeclaredType());
        dummy.setText(doc.getTierText(tier, null) + " " + doc.getTierText(tier, transcriber));

        SwingUtilities.invokeLater(() -> {
            TierEdit<?> edit = new TierEdit(dataModel.getSession(), eventManager, Transcriber.VALIDATOR, record, tier, dummy.getValue());
            edit.setValueAdjusting(false);
            undoSupport.postEdit(edit);
        });
    }

    /**
     * Get the current callout window (if any)
     *
     * @return the current callout window, null if none
     */
    public CalloutWindow getCurrentCallout() {
        return this.currentCallout.get();
    }

    /**
     * Set the current callout window
     *
     * @param callout
     */
    public void setCurrentCallout(CalloutWindow callout) {
        this.currentCallout.set(callout);
    }

    /**
     * Show a callout window with the provided contents.
     * If a callout is already shown, it will be disposed.
     *
     * @param modal
     * @param content
     * @param sideOfWindow
     * @param pointAtRect
     */
    public void showCallout(boolean modal, JComponent content, int sideOfWindow, Rectangle pointAtRect) {
        final CalloutWindow currentCallout = this.currentCallout.get();
        if (currentCallout != null) {
            currentCallout.dispose();
        }

        final CalloutWindow callout = CalloutWindow.showCallout(CommonModuleFrame.getCurrentFrame(),
                modal, content, sideOfWindow, pointAtRect);
        setCurrentCallout(callout);
    }

    /**
     * Show a callout window with the provided contents.
     * If a callout is already shown, it will be disposed.
     *
     * @param modal
     * @param content
     * @param sideOfWindow
     * @param pointAtRect
     */
    public void showNonFocusableCallout(boolean modal, JComponent content, int sideOfWindow, Rectangle pointAtRect) {
        final CalloutWindow currentCallout = this.currentCallout.get();
        if (currentCallout != null) {
            currentCallout.dispose();
        }

        final CalloutWindow callout = CalloutWindow.showNonFocusableCallout(CommonModuleFrame.getCurrentFrame(),
                content, sideOfWindow, pointAtRect);
        setCurrentCallout(callout);
    }

    /**
     * Show ipa input map as a callout pointing at current cursor location
     *
     */
    private void showInputCallout() {
        final CalloutWindow currentCallout = this.currentCallout.get();
        if(currentCallout != null) {
            currentCallout.dispose();
        }

        final IPAMapGridContainer chatMap = new IPAMapGridContainer();
        for(var ipaGrid: ChatGrids.getInstance().loadGrids().getGrid()) {
            chatMap.addGrid(ipaGrid);
        }
        chatMap.setFont(FontPreferences.getTierFont().deriveFont(FontPreferences.getDefaultFontSize() +
                PrefHelper.getFloat(TranscriptView.FONT_SIZE_DELTA_PROP, 0.0f)));

        final IPAMapGridContainer ipaMap = new IPAMapGridContainer();
        ipaMap.addDefaultGrids();
        final Font ipaFont = FontPreferences.getTierFont().deriveFont(FontPreferences.getDefaultFontSize() +
                PrefHelper.getFloat(TranscriptView.FONT_SIZE_DELTA_PROP, 0.0f));
        ipaMap.setFont(ipaFont);

        final AtomicReference<URL> currentDocUrl = new AtomicReference<>();
        final IPAMapGridMouseListener gridMouseListener = new IPAMapGridMouseListener() {
            @Override
            public void mousePressed(Cell cell, MouseEvent me) {

            }

            @Override
            public void mouseReleased(Cell cell, MouseEvent me) {

            }

            @Override
            public void mouseClicked(Cell cell, MouseEvent me) {
                final String text = cell.getText().replaceAll("◌", "");
                final CellProp insertProp =
                        cell.getProperty().stream().filter(p -> p.getName().equals("insert")).findFirst().orElse(null);
                final String insertText = (insertProp != null ? insertProp.getContent() : "");

                final List<Integer> markers = new ArrayList<>();
                final StringBuilder sb = new StringBuilder();
                if(insertText != null && !insertText.isBlank()) {
                    for(int i = 0; i < insertText.length(); i++) {
                        final char c = insertText.charAt(i);
                        if(c == '$' && (i+1 < insertText.length() && insertText.charAt(i+1) == '$')) {
                            markers.add(sb.length());
                            i++;
                        } else {
                            sb.append(c);
                        }
                    }
                } else {
                    sb.append(text);
                }

                final String selectedText = TranscriptEditor.this.getSelectedText();
                if(selectedText != null && !selectedText.isBlank()) {
                    if(markers.size() == 1) {
                        // insert selected text at marker
                        sb.insert(markers.get(0), selectedText);
                    } else if(markers.size() == 2) {
                        // replace selected text between markers
                        final int start = markers.get(0);
                        final int end = markers.get(1);
                        sb.replace(start, end, selectedText);
                    }
                }

                // copy into system clipboard
                final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                final Transferable currentContents = clipboard.getContents(TranscriptEditor.this);
                clipboard.setContents(new StringSelection(sb.toString()), TranscriptEditor.this);

                final TranscriptElementLocation currentLocation = TranscriptEditor.this.getTranscriptEditorCaret().getTranscriptLocation();
                if(!currentLocation.valid()) return;

                // insert into document
                TranscriptEditor.this.paste();
                final EditorAction<EditorEventType.TierChangeData> act = new EditorAction<>() {
                    @Override
                    public void eventOccurred(EditorEvent<EditorEventType.TierChangeData> ee) {
                        if(ee.data().valueAdjusting()) return;
                        if(markers.size() == 1) {
                            // set caret position to first marker
                            final int markerPos = TranscriptEditor.this.getCaretPosition() - sb.length() + markers.get(0);
                        } else if(markers.size() == 2) {
                            // set selection to markers
                            final int start = TranscriptEditor.this.getCaretPosition() - sb.length() + markers.get(0);
                            final int end = TranscriptEditor.this.getCaretPosition() - sb.length() + markers.get(1);
                            TranscriptEditor.this.setSelectionStart(start);
                            TranscriptEditor.this.setSelectionEnd(end);
                        }
                        TranscriptEditor.this.getEventManager().removeActionForEvent(EditorEventType.TierChange, this);
                    }
                };
                TranscriptEditor.this.getEventManager().registerActionForEvent(
                        EditorEventType.TierChange, act, EditorEventManager.RunOn.AWTEventDispatchThread);
                TranscriptEditor.this.commitChanges(TranscriptEditor.this.getCaretPosition());
                clipboard.setContents(currentContents, TranscriptEditor.this);
            }

            @Override
            public void mouseEntered(Cell cell, MouseEvent me) {
                final StringBuilder sb = new StringBuilder();
                final CellProp nameProp = cell.getProperty().stream().filter(p -> p.getName().equalsIgnoreCase("name")).findFirst().orElse(null);
                if(nameProp != null) {
                    sb.append(nameProp.getContent());
                }

                final CellProp docProp = cell.getProperty().stream().filter(p -> p.getName().equalsIgnoreCase("doc")).findFirst().orElse(null);
                if(docProp != null) {
                    final String docUri = docProp.getContent();
                    if(docUri != null && !docUri.isBlank()) {
                        try {
                            final URL docUrl = new URL(docUri);
                            currentDocUrl.set(docUrl);

                            sb.append(" (F2 for more info)");
                        } catch (MalformedURLException e) {
                            LogUtil.warning(e);
                        }
                    }
                }

                ((JComponent)me.getSource()).setToolTipText(sb.toString());
            }

            @Override
            public void mouseExited(Cell cell, MouseEvent me) {

            }
        };

        chatMap.addCellMouseListener(gridMouseListener);
        ipaMap.addCellMouseListener(gridMouseListener);

        try {

            final Rectangle2D caretRect = modelToView2D(getCaretPosition());
            final Point caretPoint = new Point((int)caretRect.getMinX(), (int)caretRect.getMinY());
            SwingUtilities.convertPointToScreen(caretPoint, this);

            // get font ascent at caret position
            final FontMetrics fm = getFontMetrics(getFont());
            final int fontAscent = fm.getAscent();
            final Rectangle r = new Rectangle(caretPoint.x, caretPoint.y - fontAscent, (int)caretRect.getWidth(), (int)caretRect.getHeight() + fontAscent);

//            final JPanel p = new JPanel(new BorderLayout());
            final JTabbedPane tabbedPane = new JTabbedPane();
            final JScrollPane chatScrollPane = new JScrollPane(chatMap);
            tabbedPane.addTab("CHAT", chatScrollPane);
            final JScrollPane scrollPane = new JScrollPane(ipaMap);
            tabbedPane.addTab("IPA", scrollPane);
            tabbedPane.setPreferredSize(new Dimension(tabbedPane.getPreferredSize().width, 500));
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            tabbedPane.setSelectedIndex(1);

            final TranscriptElementLocation elementLocation = TranscriptEditor.this.getTranscriptEditorCaret().getTranscriptLocation();
            if(!elementLocation.valid()) return;
            if(elementLocation.transcriptElementIndex() >= 0) {
                final Transcript.Element transcriptElement = getSession().getTranscript().getElementAt(elementLocation.transcriptElementIndex());
                if(transcriptElement.isRecord()) {
                    final TierDescription td = getSession().getTier(elementLocation.tier());
                    if(td.getDeclaredType() == Orthography.class) {
                        tabbedPane.setSelectedIndex(0);
                    }
                }
            }

            final CalloutWindow window =
                    CalloutWindow.showNonFocusableCallout(CommonModuleFrame.getCurrentFrame(), tabbedPane, SwingConstants.TOP, r);
            window.setAlwaysOnTop(true);
            this.currentCallout.set(window);

            // escape closes window
            final PhonUIAction<Void> closeAct = PhonUIAction.runnable(() -> {
                window.setVisible(false);
                window.dispose();

            });
            final AWTEventListener escListener = new AWTEventListener() {
                @Override
                public void eventDispatched(AWTEvent event) {
                    if(event instanceof KeyEvent) {
                        final KeyEvent ke = (KeyEvent)event;
                        if(ke.getID() == KeyEvent.KEY_PRESSED && ke.getKeyCode() == KeyEvent.VK_ESCAPE) {
                            closeAct.actionPerformed(null);
                        } else if(ke.getID() == KeyEvent.KEY_PRESSED && ke.getKeyCode() == KeyEvent.VK_F2) {
                            if(currentDocUrl.get() != null) {
                                try {
                                    Desktop.getDesktop().browse(currentDocUrl.get().toURI());
                                } catch (IOException | URISyntaxException e) {
                                    LogUtil.warning(e);
                                }
                            }
                        }
                    }
                }
            };
            Toolkit.getDefaultToolkit().addAWTEventListener(escListener, AWTEvent.KEY_EVENT_MASK);

            window.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    Toolkit.getDefaultToolkit().removeAWTEventListener(escListener);
                }
            });

            // hide window when caret moves to a different tier
            TranscriptEditor.this.addPropertyChangeListener("currentSessionLocation", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent e) {
                    if(window.isVisible()) {
                        // if tier had changed
                        final TranscriptElementLocation oldLoc = (TranscriptElementLocation)e.getOldValue();
                        final TranscriptElementLocation newLoc = (TranscriptElementLocation)e.getNewValue();
                        if(oldLoc != null && newLoc != null && (oldLoc.tier() != newLoc.tier() || oldLoc.transcriptElementIndex() != newLoc.transcriptElementIndex())) {
                            window.setVisible(false);
                            window.dispose();
                            TranscriptEditor.this.removePropertyChangeListener("currentSessionLocation", this);
                        } else {
                            // move window to new caret position
                            final Rectangle2D caretRect;
                            try {
                                caretRect = modelToView2D(getCaretPosition());
                            } catch (BadLocationException ex) {
                                return;
                            }
                            final Point caretPoint = new Point((int)caretRect.getMinX(), (int)caretRect.getMinY());
                            SwingUtilities.convertPointToScreen(caretPoint, TranscriptEditor.this);
                            // get font ascent at caret position
                            final FontMetrics fm = getFontMetrics(getFont());
                            final int fontAscent = fm.getAscent();
                            final Rectangle r = new Rectangle(caretPoint.x, caretPoint.y - fontAscent, (int)caretRect.getWidth(), (int)caretRect.getHeight() + fontAscent);
                            window.pointAtRect(SwingConstants.TOP, r);
                        }
                    }
                }
            });
        } catch (BadLocationException e) {
            LogUtil.warning(e);
        }
    }

    /**
     * Setup context menu items on provided menu builder
     *
     * @param menuBuilder the menu builder to add the menu items to
     */
    void setupContextMenu(MenuBuilder menuBuilder) {
        // add show input dialog item
        final PhonUIAction<Void> showInputAct = PhonUIAction.runnable(this::showInputCallout);
        showInputAct.putValue(PhonUIAction.NAME, "Show input dialog");
        showInputAct.putValue(PhonUIAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        menuBuilder.addItem(".", showInputAct);
    }

    /**
     * Saves the changes made to the line the caret is currently on
     */
    public void saveCurrentLine() {
        commitChanges(getCaretPosition());
    }

    public TranscriptElementLocation getCurrentSessionLocation() {
        return currentTranscriptLocation;
    }

    public void setCurrentSessionLocation(TranscriptElementLocation currentTranscriptLocation) {
        TranscriptElementLocation oldLoc = this.currentTranscriptLocation;
        this.currentTranscriptLocation = currentTranscriptLocation;
        firePropertyChange("currentSessionLocation", oldLoc, currentTranscriptLocation);
    }

    /**
     * Changes the data in a given tier if the provided data is different
     *
     * @param record  the record that the tier belongs to
     * @param tier    the tier that the data is changing for
     * @param newData the possible new data for the tier
     */
    public void changeTierData(Record record, Tier<?> tier, String newData) {
        if(PrefHelper.isDebugMode()) {
            LogUtil.info("Changing tier data for " + tier.getName() + " to " + newData);
        }
        TranscriptDocument doc = getTranscriptDocument();
        String transcriber = dataModel.getTranscriber().getUsername();

        Tier<?> dummy = SessionFactory.newFactory().createTier("dummy", tier.getDeclaredType());
        dummy.setText(newData);

        if (tier.getDeclaredType() == MediaSegment.class) return;
        if (doc.getTierText(tier, transcriber).equals(doc.getTierText(dummy, transcriber))) return;

//        SwingUtilities.invokeLater(() -> {
            TierEdit<?> edit = new TierEdit(getSession(), eventManager, dataModel.getTranscriber(), record, tier, dummy.getValue());
            edit.setValueAdjusting(false);
            getUndoSupport().postEdit(edit);
//        });
    }

    /**
     * Changes the data in a given comment if the provided data is different
     *
     * @param comment the comment that the data is changing for
     * @param newData the possible new data for the tier
     */
    public void commentDataChanged(Comment comment, String newData) {
        Tier<TierData> dummy = SessionFactory.newFactory().createTier("dummy", TierData.class);
        dummy.setText(newData);

        String transcriber = dataModel.getTranscriber().getUsername();
        if (comment.getValue().toString().equals(getTranscriptDocument().getTierText(dummy, transcriber))) return;

//        SwingUtilities.invokeLater(() -> {
            ChangeCommentEdit edit = new ChangeCommentEdit(getSession(), eventManager, comment, dummy.getValue());
            getUndoSupport().postEdit(edit);
//        });
    }

    /**
     * Changes the data in a given gem if the provided data is different
     *
     * @param gem     the gem that the data is changing for
     * @param newData the possible new data for the tier
     */
    public void gemDataChanged(Gem gem, String newData) {

        if (gem.getLabel().equals(newData)) return;

//        SwingUtilities.invokeLater(() -> {
            ChangeGemEdit edit = new ChangeGemEdit(getSession(), eventManager, gem, newData);
            getUndoSupport().postEdit(edit);
//        });
    }

    public SessionEditUndoSupport getUndoSupport() {
        return undoSupport;
    }

    @Override
    protected void paintComponent(Graphics g) {
        // XXX background colors are not being drawn correctly on windows
//        g2d.setRenderingHint(
//                RenderingHints.KEY_TEXT_ANTIALIASING,
//                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
//        g2d.setRenderingHint(
//                RenderingHints.KEY_RENDERING,
//                RenderingHints.VALUE_RENDER_QUALITY);
//
//        TranscriptDocument doc = getTranscriptDocument();
//        // Get the clip bounds of the current view
//        Rectangle drawHere = g.getClipBounds();
//
//        // Fill the background with the appropriate color
//        g.setColor(UIManager.getColor(TranscriptEditorUIProps.BACKGROUND));
//        g.fillRect(0, drawHere.y, drawHere.width, drawHere.height);
//
//        // Fill the label column background with the appropriate color
//        g.setColor(UIManager.getColor(TranscriptEditorUIProps.LABEL_BACKGROUND));
//        FontMetrics fontMetrics = g.getFontMetrics(FontPreferences.getMonospaceFont().deriveFont(14.0f));
//        final int labelColumnWidth = TranscriptViewFactory.LABEL_COLUMN_WIDTH;
////        char[] template = new char[getTranscriptDocument().getLabelColumnWidth() + 1];
////        Arrays.fill(template, ' ');
////        int labelColWidth = fontMetrics.stringWidth(new String(template));
//        Rectangle labelColRect = new Rectangle(0, 0, labelColumnWidth, getHeight());
//        if (labelColRect.intersects(drawHere)) {
//            g.fillRect(0, (int) drawHere.getMinY(), labelColumnWidth, drawHere.height);
//        }
//
//        Element root = doc.getDefaultRootElement();
//        if (root.getElementCount() == 0) return;
//
//        for (int i = 0; i < root.getElementCount(); i++) {
//            Element elem = root.getElement(i);
//            if (elem.getElementCount() == 0) continue;
//            Element innerElem = elem.getElement(0);
//            AttributeSet attrs = elem.getAttributes();
//            String elementType = (String) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);
//            if (elementType != null) {
//                var startEnd = new TranscriptDocument.StartEnd(-1, -1);
//
//                switch (elementType) {
//                    case TranscriptStyleConstants.ELEMENT_TYPE_COMMENT -> {
//                        g.setColor(UIManager.getColor(TranscriptEditorUIProps.COMMENT_BACKGROUND));
//                        Comment comment = (Comment) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_COMMENT);
//                        startEnd = doc.getCommentContentStartEnd(comment);
//                    }
//                    case TranscriptStyleConstants.ELEMENT_TYPE_GEM -> {
//                        g.setColor(UIManager.getColor(TranscriptEditorUIProps.GEM_BACKGROUND));
//                        Gem gem = (Gem) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GEM);
//                        startEnd = doc.getGemContentStartEnd(gem);
//                    }
//                    case TranscriptStyleConstants.ELEMENT_TYPE_GENERIC -> {
//                        g.setColor(UIManager.getColor(TranscriptEditorUIProps.GENERIC_BACKGROUND));
//                        Tier<?> genericTier = (Tier<?>) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GENERIC_TIER);
//                        startEnd = doc.getGenericContentStartEnd(genericTier);
//                    }
//                }
//                if (!startEnd.valid()) continue;
//                try {
//                    var startRect = modelToView2D(startEnd.start());
//                    var endRect = modelToView2D(startEnd.end());
//                    if (startRect == null || endRect == null) continue;
//                    var colorRect = new Rectangle(labelColumnWidth, (int) startRect.getMinY(), (int) (TranscriptViewFactory.PAGE_WIDTH - labelColumnWidth), (int) (endRect.getMaxY() - startRect.getMinY()));
//                    if (!drawHere.intersects(colorRect)) continue;
//                    g.fillRect((int) colorRect.getMinX(), (int) colorRect.getMinY(), (int) colorRect.getWidth(), (int) colorRect.getHeight());
//                } catch (BadLocationException e) {
//                    LogUtil.severe(e);
//                }
//            }
//        }
//
//        g.setColor(UIManager.getColor(TranscriptEditorUIProps.SEPARATOR_LINE));
//        int sepLineHeight = 1;
//        int fontHeight = fontMetrics.getHeight();
//
//        float lineSpacing = StyleConstants.getLineSpacing(root.getElement(0).getAttributes());
//        int sepLineOffset = (int) (((fontHeight * lineSpacing) + sepLineHeight) / 2);
//        // For every element
//        for (int i = 0; i < root.getElementCount(); i++) {
//            Element elem = root.getElement(i);
//            if (elem.getElementCount() == 0) continue;
//            Element innerElem = elem.getElement(0);
//            AttributeSet attrs = innerElem.getAttributes();
//            // If it's a separator
//            if (attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_SEPARATOR) != null) {
//                try {
//                    var sepRect = modelToView2D(innerElem.getStartOffset());
//                    if (sepRect == null) continue;
//                    boolean topVisible = sepRect.getMinY() >= drawHere.getMinY() && sepRect.getMinY() <= drawHere.getMaxY();
//                    boolean bottomVisible = sepRect.getMaxY() >= drawHere.getMinY() && sepRect.getMaxY() <= drawHere.getMaxY();
//                    // And it's onscreen
//                    if (!topVisible && !bottomVisible) continue;
//                    // Draw the separator line
//                    g.fillRect(drawHere.x, ((int) sepRect.getMinY()) - sepLineOffset, drawHere.width, sepLineHeight);
//                } catch (BadLocationException e) {
//                    LogUtil.severe(e);
//                }
//            }
//        }

        super.paintComponent(g);

        // to fix an issue where label views would repaint their content on some lines causing a 'bold' effect to occur
        // label views clear their background before painting. We need to repaint our highlights here.
        if (getHighlighter() != null) {
            for (var highlight : getHighlighter().getHighlights()) {
                if (highlight.getPainter() instanceof BoxSelectHighlightPainter || highlight.getPainter() instanceof ErrorUnderlinePainter
                        || highlight.getPainter() instanceof HoverUnderlinePainter) {
                    // calculate highlight bounds
                    var start = highlight.getStartOffset();
                    var end = highlight.getEndOffset();
                    try {
                        var startRect = modelToView2D(start);
                        var endRect = modelToView2D(end);
                        if (startRect == null || endRect == null) continue;
                        var highlightRect = new Rectangle((int) startRect.getMinX(), (int) startRect.getMinY(), (int) (endRect.getMaxX() - startRect.getMinX()), (int) (endRect.getMaxY() - startRect.getMinY()));
                        highlight.getPainter().paint(g, highlight.getStartOffset(), highlight.getEndOffset(), highlightRect, this);
                    } catch (BadLocationException e) {
                        getHighlighter().removeHighlight(highlight);
                    }
                }
            }
        }
    }

    @Override
    public TransferHandler getTransferHandler() {
        return new CustomTransferHandler();
    }

    /**
     * Removes editor actions for specific events
     */
    public void removeEditorActions() {
        this.eventManager.removeActionForEvent(EditorEventType.SessionChanged, this::onSessionChanged);
        this.eventManager.removeActionForEvent(EditorEventType.TierViewChanged, this::onTierViewChanged);
        this.eventManager.removeActionForEvent(EditorEventType.RecordChanged, this::onRecordChanged);
    }

    /**
     * Runs when the loaded session changes
     *
     * @param editorEvent the event that changes the session
     */
    private void onSessionChanged(EditorEvent<Session> editorEvent) {

    }

    /**
     * Runs when any changes are made to the tier view
     *
     * @param editorEvent the event that made the changes to the tier view
     */
    private void onTierViewChanged(EditorEvent<EditorEventType.TierViewChangedData> editorEvent) {
        var changeType = editorEvent.data().changeType();
        final Runnable runnable = switch (changeType) {
            case RELOAD -> () -> getTranscriptDocument().reload();
            case MOVE_TIER -> () -> {
                moveTier(editorEvent.data());
            };
            case DELETE_TIER, HIDE_TIER -> () -> {
                hideTier(editorEvent.data());
                recalculateTierLabelWidth();
            };
            case ADD_TIER, SHOW_TIER -> () -> {
                showTier(editorEvent.data());
                recalculateTierLabelWidth();
            };
            case TIER_NAME_CHANGE, TIER_FONT_CHANGE -> () -> {
                tierFontOrNameChanged(editorEvent.data());
                recalculateTierLabelWidth();
            };
            default -> () -> {
                LogUtil.info("Unhandled tier view change type: " + changeType);
            };
        };
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    /**
     * Recalculates the width of the tier labels and updates all paragraph attributes
     *
     * @param additionalTierNames additional tier names to consider when recalculating the tier label width
     */
    public void recalculateTierLabelWidth(List<String> additionalTierNames) {
        editorKit.invalidateTierLabelWidth(additionalTierNames);
        getTranscriptDocument().updateGlobalParagraphAttributes();
    }

    /**
     * Recalculates the width of the tier labels and updates all paragraph attributes
     */
    public void recalculateTierLabelWidth() {
        recalculateTierLabelWidth(Collections.emptyList());
    }

    /**
     * Runs when the "current record" changes
     *
     * @param editorEvent the event that says the current record haas changed
     */
    private void onRecordChanged(EditorEvent<EditorEventType.RecordChangedData> editorEvent) {
        TranscriptDocument doc = getTranscriptDocument();

        final TranscriptElementLocation currentLocation = getTranscriptEditorCaret().getTranscriptLocation();
        // commit any changes
        getTranscriptEditorCaret().freeze();
        commitChanges(getCaretPosition());
        // Update the single record index in the doc
        doc.setSingleRecordIndex(editorEvent.data().recordIndex());
        getTranscriptEditorCaret().unfreeze();

        // set dot to start of currently selected record tier
        if(editorEvent.source() != this && currentLocation.transcriptElementIndex() >= 0) {
            final int transcriptElementIndex = editorEvent.data().elementIndex();
            final TranscriptElementLocation newLocation = new TranscriptElementLocation(transcriptElementIndex,
                    currentLocation.tier(), 0);
            final int dot = sessionLocationToCharPos(newLocation);
            if (dot >= 0) {
                setCaretPosition(dot);
            }
        }

        // If it's currently in single record view fire the appropriate event
        if (doc.getSingleRecordView()) {
            final EditorEvent<Void> e = new EditorEvent<>(recordChangedInSingleRecordMode, this, null);
            eventManager.queueEvent(e);

            // update highlights for the new record
            updateSelectionHighlights();

            return;
        }

        // If the transcript editor is currently in focus, stop here
        if (hasFocus()) return;

        try {
            // Get rects for the start and end positions of the record
            int recordStartPos = doc.getRecordStart(editorEvent.data().recordIndex());
            int recordEndPos = doc.getRecordEnd(editorEvent.data().recordIndex());
            var startRect = modelToView2D(recordStartPos);
            var endRect = modelToView2D(recordEndPos);

            // Create a rect that contains the whole record
            Rectangle scrollToRect = new Rectangle((int) startRect.getMinX(), (int) startRect.getMinY(), (int) (endRect.getMaxX() - startRect.getMinX()), (int) (endRect.getMaxY() - startRect.getMinY()));
            // Scroll to a point where that new rect is visible
            super.scrollRectToVisible(scrollToRect);
        } catch (BadLocationException e) {
            LogUtil.severe(e);
        }
    }

    private void updateSelectionHighlights() {
        if(!isSingleRecordView()) return;

        // clear old highlights
        for(var highlight:selectionHighlightList) {
            getHighlighter().removeHighlight(highlight);
        }
        selectionHighlightList.clear();
        selectionMap.clear();

        // get selections for the currently display transcript elements
        final TranscriptDocument doc = getTranscriptDocument();
        final Set<Integer> selectedTranscriptElementIndices = new HashSet<>();
        for(int i = 0; i < doc.getDefaultRootElement().getElementCount(); i++) {
            final Element elem = doc.getDefaultRootElement().getElement(i);
            final AttributeSet attrs = elem.getElementCount() > 0 ? elem.getElement(0).getAttributes() : new SimpleAttributeSet();
            final String elementType = TranscriptStyleConstants.getElementType(attrs);
            if(elementType == null) continue;
            if(TranscriptStyleConstants.ELEMENT_TYPE_RECORD.equals(elementType)) {
                final Record record = TranscriptStyleConstants.getRecord(attrs);
                if(record == null) continue;
                final int elementIndex = getSession().getTranscript().getElementIndex(record);
                if(elementIndex < 0) continue;
                selectedTranscriptElementIndices.add(elementIndex);
            } else if(TranscriptStyleConstants.ELEMENT_TYPE_GEM.equals(elementType)) {
                final Gem gem = TranscriptStyleConstants.getGEM(attrs);
                if(gem == null) continue;
                final int elementIndex = getSession().getTranscript().getElementIndex(gem);
                if(elementIndex < 0) continue;
                selectedTranscriptElementIndices.add(elementIndex);
            } else if(TranscriptStyleConstants.ELEMENT_TYPE_COMMENT.equals(elementType)) {
                final Comment comment = TranscriptStyleConstants.getComment(attrs);
                if(comment == null) continue;
                final int elementIndex = getSession().getTranscript().getElementIndex(comment);
                if(elementIndex < 0) continue;
                selectedTranscriptElementIndices.add(elementIndex);
            }
        }

        for(int elementIndex:selectedTranscriptElementIndices) {
            selectionModel.getSelectionsForElement(elementIndex).forEach(this::addHighlightForSelection);
        }
    }

    /**
     * Moves the specified tiers (and the caret if needed)
     *
     * @param data the data from the tier view changed event
     */
    public void moveTier(EditorEventType.TierViewChangedData data) {

        TranscriptDocument doc = getTranscriptDocument();

        final TranscriptElementLocation startLocation = getTranscriptEditorCaret().getTranscriptLocation();
        // Move tier in doc
        getTranscriptEditorCaret().freeze();
        for (String tierName : data.tierNames()) {
            int newIdx = data.viewIndices().get(1);
            doc.removeTier(tierName);
            doc.addTier(tierName, newIdx, record -> record.getTier(tierName));
        }
        doc.updateGlobalParagraphAttributes();
        getTranscriptEditorCaret().unfreeze();

        // Correct caret
        if (startLocation.valid()) {
            final int charPos = sessionLocationToCharPos(startLocation);
            if (charPos >= 0) {
                setCaretPosition(charPos);
            }
        }
    }

    /**
     * Hides the specified tiers (and moves the caret if needed)
     *
     * @param data the data from the tier view changed event
     */
    public void hideTier(EditorEventType.TierViewChangedData data) {
        TranscriptDocument doc = getTranscriptDocument();

        List<String> hiddenTiersNames = data.tierNames();

        final TranscriptElementLocation startLocation = getTranscriptEditorCaret().getTranscriptLocation();
        boolean caretInHiddenTier = false;
        int nextParagraphIndex = -1;
        if (startLocation.valid()) {
            final int caretParagraphIndex = doc.findParagraphElementIndexForSessionElementIndex(startLocation.transcriptElementIndex());
            nextParagraphIndex = caretParagraphIndex < doc.getDefaultRootElement().getElementCount() ? caretParagraphIndex + 1 : caretParagraphIndex;
            final String caretTier = startLocation.tier();
            caretInHiddenTier = caretTier != null && hiddenTiersNames.contains(caretTier);
        }

        getTranscriptEditorCaret().freeze();
        for (String tierName : hiddenTiersNames) {
            doc.removeTier(tierName);
        }
        getTranscriptEditorCaret().unfreeze();

        // Caret in record / tier
        if (caretInHiddenTier) {
            final Element nextParagraph = doc.getDefaultRootElement().getElement(nextParagraphIndex);
            int newPos = nextParagraph.getStartOffset();
            for (int i = 0; i < nextParagraph.getElementCount(); i++) {
                final Element elem = nextParagraph.getElement(i);
                final AttributeSet attrs = elem.getAttributes();
                if (!TranscriptStyleConstants.isNotEditable(attrs)) {
                    newPos = elem.getStartOffset();
                    break;
                }
            }
            setCaretPosition(newPos);
        } else {
            final int charPos = sessionLocationToCharPos(startLocation);
            if (charPos >= 0) {
                setCaretPosition(charPos);
            }
        }
    }

    /**
     * Shows the specified tiers (and moves the caret if needed)
     *
     * @param data the data from the tier view changed event
     */
    public void showTier(EditorEventType.TierViewChangedData data) {
        var doc = getTranscriptDocument();

        final TranscriptElementLocation startLocation = getTranscriptEditorCaret().getTranscriptLocation();

        for (int i = 0; i < data.tierNames().size(); i++) {
            var tierName = data.tierNames().get(i);
            var viewIndex = data.viewIndices().get(i);
            doc.addTier(tierName, viewIndex, record -> record.getTier(tierName));
        }

        // Correct caret
        if (startLocation.valid()) {
            final int charPos = sessionLocationToCharPos(startLocation);
            if (charPos >= 0) {
                setCaretPosition(charPos);
            }
        }
    }

    /**
     * Updates the fonts of the specified tiers
     *
     * @param data the data from the tier view changed event
     */
    public void tierFontOrNameChanged(EditorEventType.TierViewChangedData data) {
        TranscriptDocument doc = getTranscriptDocument();
        final TranscriptElementLocation startLocation = getTranscriptEditorCaret().getTranscriptLocation();

        getTranscriptEditorCaret().freeze();
        for (var tviIdx: data.viewIndices()) {
            final var tvi = data.newTierView().get(tviIdx);
            final var oldTvi = data.oldTierView().get(tviIdx);
            doc.removeTier(oldTvi.getTierName());
            doc.addTier(tvi.getTierName(), data.newTierView().indexOf(tvi), record -> record.getTier(tvi.getTierName()));
        }
        getTranscriptEditorCaret().unfreeze();

        if(startLocation.valid()) {
            final int newPos = sessionLocationToCharPos(startLocation);
            if(newPos >= 0) {
                setCaretPosition(newPos);
            }
        }
    }

    /**
     * Changes the speaker of a given record to a given participant
     *
     * @param data a record containing the record and the participant that will become the new speaker
     */
    public void changeSpeaker(RecordParticipant data) {
        ChangeSpeakerEdit edit = new ChangeSpeakerEdit(getSession(), eventManager, data.record, data.participant);
        undoSupport.postEdit(edit);
    }

    /**
     * Changes the data in a given generic tier if the provided data is different
     *
     * @param genericTier the generic tier that the data is changing for
     * @param newData     the possible new data for the tier
     */
    public void genericDataChanged(Tier<?> genericTier, String newData) {
        TranscriptDocument doc = getTranscriptDocument();
        String transcriber = dataModel.getTranscriber().getUsername();

        Tier dummy = SessionFactory.newFactory().createTier("dummy", genericTier.getDeclaredType());
        dummy.setFormatter(genericTier.getFormatter());
        dummy.setText(newData);

        if (doc.getTierText(genericTier, transcriber).equals(doc.getTierText(dummy, transcriber))) return;

        SwingUtilities.invokeLater(() -> {
            getUndoSupport().beginUpdate();

            if (genericTier.getDeclaredType() == TranscriptDocument.Languages.class) {
                Tier<TranscriptDocument.Languages> languagesTier = (Tier<TranscriptDocument.Languages>) dummy;
                if (languagesTier.hasValue()) {
                    SessionLanguageEdit edit = new SessionLanguageEdit(getSession(), eventManager, languagesTier.getValue().languageList());
                    getUndoSupport().postEdit(edit);
                }
            } else if (genericTier.getDeclaredType() == LocalDate.class) {
                Tier<LocalDate> dateTier = (Tier<LocalDate>) dummy;
                if (dateTier.hasValue()) {
                    SessionDateEdit edit = new SessionDateEdit(getSession(), getEventManager(), dateTier.getValue(), getSession().getDate());
                    undoSupport.postEdit(edit);
                }
            }

            TierEdit<?> edit = new TierEdit(getSession(), eventManager, null, genericTier, dummy.getValue());
            edit.setValueAdjusting(false);
            getUndoSupport().postEdit(edit);
            getUndoSupport().endUpdate();
        });
    }

    public EditorEventManager getEventManager() {
        return eventManager;
    }

    /**
     * Loads the session on the document
     */
    public void loadSession() {
        TranscriptDocument doc = getTranscriptDocument();
        doc.setUndoSupport(undoSupport);
        doc.setEventManager(eventManager);

        doc.addDocumentPropertyChangeListener("populate", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getNewValue() instanceof Boolean b) {
                    if (!b) {
                        // populate finished
                        getEventManager().queueEvent(new EditorEvent<>(TranscriptEditor.transcriptDocumentPopulated, TranscriptEditor.this, null));
                        doc.removeDocumentPropertyChangeListener(this);

                        if(isSingleRecordView() && getSession().getRecordCount() > 0 && doc.getSingleRecordIndex() < 0) {
                            doc.setSingleRecordIndex(0);
                        }
                    }
                }
            }
        });
        doc.setSession(getSession());
        doc.addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                TranscriptDocument doc = getTranscriptDocument();
                Element elem = doc.getCharacterElement(e.getOffset());
                AttributeSet attrs = elem.getAttributes();
                if (attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_SYLLABIFICATION) != null) {
                    Record record = TranscriptStyleConstants.getRecord(attrs);
                    int recordIndex = getSession().getRecordPosition(record);
                    if (recordIndex < 0) return;
                    Tier<?> tier = TranscriptStyleConstants.getTier(attrs);
                    final TranscriptDocument.StartEnd startEnd = doc.getTierContentStartEnd(recordIndex, tier.getName());
                    if (!startEnd.valid()) return;
                    int tierEnd = startEnd.end();
                    if (getCaretPosition() != tierEnd - 1) {
                        SwingUtilities.invokeLater(() -> {
                            setCaretPosition(getNextValidIndex(getCaret().getMark() + 1, false));
                        });
                    }
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {

            }

            @Override
            public void changedUpdate(DocumentEvent e) {

            }
        });

    }

    /**
     * Gets the next valid index for the caret from the given position
     *
     * @param currentPos the starting position of the caret
     * @param looping    whether the caret should loop back to the start of the doc at the end
     * @return the next valid caret position
     */
    public int getNextValidIndex(int currentPos, boolean looping) {
        TranscriptDocument doc = getTranscriptDocument();

        int docLen = doc.getLength();

        Element elem = doc.getCharacterElement(currentPos);
        while (containsNotTraversableAttribute(elem.getAttributes()) && currentPos < docLen) {
            currentPos++;
            elem = doc.getCharacterElement(currentPos);
        }

        if (currentPos == docLen) {
            if (looping) {
                return getNextValidIndex(0, true);
            } else {
                return -1;
            }
        }

        return currentPos;
    }

    /**
     * Gets the previous valid index for the caret from the given position
     *
     * @param currentPos the starting position of the caret
     * @param looping    whether the caret should loop back to the end of the doc at the start
     * @return the previous valid caret position
     */
    public int getPrevValidIndex(int currentPos, boolean looping) {
        TranscriptDocument doc = getTranscriptDocument();

        Element elem = doc.getCharacterElement(currentPos);
        AttributeSet attrs = elem.getAttributes();

        if (attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER) != null) {
            Tier<?> tier = (Tier<?>) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);
            Set<String> syllabificationTierNames = new HashSet<>();
            syllabificationTierNames.add(SystemTierType.TargetSyllables.getName());
            syllabificationTierNames.add(SystemTierType.ActualSyllables.getName());
            if (syllabificationTierNames.contains(tier.getName()) && getSelectionStart() != getSelectionEnd()) {
                currentPos--;
                elem = doc.getCharacterElement(currentPos);
                attrs = elem.getAttributes();
            }
        }

        while (containsNotTraversableAttribute(attrs) && currentPos >= 0) {
            currentPos--;
            elem = doc.getCharacterElement(currentPos);
            attrs = elem.getAttributes();
        }

        if (looping && currentPos < 0) {
            return getPrevValidIndex(doc.getLength() - 1, true);
        }

        return currentPos;
    }

    /**
     * Moves the caret to the position in the previous line with the same offset from the labels
     * (or the end of the line if it's not long enough)
     */
    public void sameOffsetInPrevTierOrElement() {
        TranscriptDocument doc = getTranscriptDocument();

        int caretPos = getCaretPosition();
        int offsetInContent;
        if (upDownOffset == -1) {
            offsetInContent = doc.getOffsetInContent(caretPos);
            upDownOffset = offsetInContent;
        } else {
            offsetInContent = upDownOffset;
        }

        int start = getStartOfPrevTierOrElement(caretPos);

        if (start == -1) return;

        int end;

        AttributeSet prevElementAttributes = doc.getCharacterElement(start).getAttributes();

        String elementType = (String) prevElementAttributes.getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);

        if (elementType == null) {
            return;
        } else if (elementType.equals(TranscriptStyleConstants.ATTR_KEY_RECORD)) {
            final int recordIndex = getSession().getRecordPosition(TranscriptStyleConstants.getRecord(prevElementAttributes));
            final Tier<?> tier = TranscriptStyleConstants.getTier(prevElementAttributes);
            end = doc.getTierEnd(recordIndex, tier.getName());
        } else if (elementType.equals(TranscriptStyleConstants.ATTR_KEY_COMMENT)) {
            end = doc.getCommentEnd((Comment) prevElementAttributes.getAttribute(TranscriptStyleConstants.ATTR_KEY_COMMENT));
        } else if (elementType.equals(TranscriptStyleConstants.ATTR_KEY_GEM)) {
            Gem gem = (Gem) prevElementAttributes.getAttribute(TranscriptStyleConstants.ATTR_KEY_GEM);
            end = doc.getGemEnd(gem);
        } else if (elementType.equals(TranscriptStyleConstants.ATTR_KEY_GENERIC_TIER)) {
            Tier<?> genericTier = (Tier<?>) prevElementAttributes.getAttribute(TranscriptStyleConstants.ATTR_KEY_GENERIC_TIER);
            end = doc.getGenericEnd(genericTier);
        } else {
            return;
        }

        int newCaretPos = Math.min((start == end) ? end : end - 1, start + offsetInContent);

        caretMoveFromUpDown = true;
        setCaretPosition(newCaretPos);
    }

    /**
     * Moves the caret to the position in the next line with the same offset from the labels
     * (or the end of the line if it's not long enough)
     */
    public void sameOffsetInNextTierOrElement() {
        TranscriptDocument doc = getTranscriptDocument();

        int caretPos = getCaretPosition();
        int offsetInContent;
        if (upDownOffset == -1) {
            offsetInContent = doc.getOffsetInContent(caretPos);
            upDownOffset = offsetInContent;
        } else {
            offsetInContent = upDownOffset;
        }

        int start = getStartOfNextTierOrElement(caretPos);

        if (start == -1) return;

        int end;

        AttributeSet nextElementAttributes = doc.getCharacterElement(start).getAttributes();

        String elementType = (String) nextElementAttributes.getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);

        if (elementType == null) {
            return;
        } else if (elementType.equals(TranscriptStyleConstants.ELEMENT_TYPE_RECORD)) {
            final int recordIndex = getSession().getRecordPosition(TranscriptStyleConstants.getRecord(nextElementAttributes));
            final Tier<?> tier = TranscriptStyleConstants.getTier(nextElementAttributes);
            end = doc.getTierEnd(recordIndex, tier.getName());
        } else if (elementType.equals(TranscriptStyleConstants.ELEMENT_TYPE_COMMENT)) {
            end = doc.getCommentEnd(TranscriptStyleConstants.getComment(nextElementAttributes));
        } else if (elementType.equals(TranscriptStyleConstants.ELEMENT_TYPE_GEM)) {
            end = doc.getGemEnd(TranscriptStyleConstants.getGEM(nextElementAttributes));
        } else if (elementType.equals(TranscriptStyleConstants.ELEMENT_TYPE_GENERIC)) {
            end = doc.getGenericEnd(TranscriptStyleConstants.getGenericTier(nextElementAttributes));
        } else {
            return;
        }

        int newCaretPos = Math.min((start == end) ? end : end - 1, start + offsetInContent);

        caretMoveFromUpDown = true;
        setCaretPosition(newCaretPos);
    }

    /**
     * Gets the start position of the previous line
     *
     * @param caretPos the starting caret pos
     */
    public int getStartOfPrevTierOrElement(int caretPos) {
        TranscriptDocument doc = getTranscriptDocument();
        Element elem = doc.getCharacterElement(caretPos);
        AttributeSet currentPosAttrs = elem.getAttributes();

        String elementType = (String) currentPosAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);
        Object content;
        if (elementType.equals(TranscriptStyleConstants.ATTR_KEY_RECORD)) {
            content = currentPosAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);
        } else {
            content = currentPosAttrs.getAttribute(elementType);
        }

        int currentDocElemIndex = doc.getDefaultRootElement().getElementIndex(caretPos);

        Element root = doc.getDefaultRootElement();
        for (int i = currentDocElemIndex; i >= 0; i--) {
            Element docElem = root.getElement(i);
            if (docElem.getElementCount() == 0) continue;
            for (int j = 0; j < docElem.getElementCount(); j++) {
                Element innerDocElem = docElem.getElement(j);
                AttributeSet attrs = innerDocElem.getAttributes();
                if (TranscriptStyleConstants.isNewParagraph(attrs)) {
                    continue;
                }
                Boolean isLabel = (Boolean) attrs.getAttribute("label");
                String innerDocElemType = (String) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);
                if (isLabel == null && innerDocElemType != null) {
                    if (!innerDocElemType.equals(elementType)) {
                        return innerDocElem.getStartOffset();
                    }
                    if (innerDocElemType.equals(TranscriptStyleConstants.ATTR_KEY_RECORD)) {
                        if (attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER) != content) {
                            return innerDocElem.getStartOffset();
                        }
                    } else {
                        if (attrs.getAttribute(innerDocElemType) != content) {
                            return innerDocElem.getStartOffset();
                        }
                    }
                }
            }
        }

        return -1;
    }

    /**
     * Gets the start position of the next line
     *
     * @param caretPos the starting caret pos
     */
    public int getStartOfNextTierOrElement(int caretPos) {
        TranscriptDocument doc = getTranscriptDocument();
        Element elem = doc.getCharacterElement(caretPos);
        AttributeSet currentPosAttrs = elem.getAttributes();

        String elementType = (String) currentPosAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);
        Object content;
        if (elementType.equals(TranscriptStyleConstants.ATTR_KEY_RECORD)) {
            content = currentPosAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);
        } else {
            content = currentPosAttrs.getAttribute(elementType);
        }

        int currentDocElemIndex = doc.getDefaultRootElement().getElementIndex(caretPos);

        Element root = doc.getDefaultRootElement();
        for (int i = currentDocElemIndex; i < root.getElementCount(); i++) {
            Element docElem = root.getElement(i);
            if (docElem.getElementCount() == 0) continue;
            for (int j = 0; j < docElem.getElementCount(); j++) {
                Element innerDocElem = docElem.getElement(j);
                AttributeSet attrs = innerDocElem.getAttributes();
                if (TranscriptStyleConstants.isNewParagraph(attrs)) {
                    continue;
                }
                Boolean isLabel = (Boolean) attrs.getAttribute("label");
                String innerDocElemType = (String) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);
                if (isLabel == null && innerDocElemType != null) {
                    if (!innerDocElemType.equals(elementType)) {
                        return innerDocElem.getStartOffset();
                    }
                    if (innerDocElemType.equals(TranscriptStyleConstants.ATTR_KEY_RECORD)) {
                        if (attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER) != content) {
                            return innerDocElem.getStartOffset();
                        }
                    } else {
                        if (attrs.getAttribute(innerDocElemType) != content) {
                            return innerDocElem.getStartOffset();
                        }
                    }
                }
            }
        }

        return -1;
    }

    /**
     * Converts a character position in the document into a {@link TranscriptElementLocation} object
     *
     * @param charPos the position in the document
     * @return the converted session location object
     */
    public TranscriptElementLocation charPosToSessionLocation(int charPos) {
        TranscriptDocument doc = getTranscriptDocument();
        Transcript transcript = getSession().getTranscript();

        Element charElem = doc.getCharacterElement(charPos);
        AttributeSet attrs = charElem.getAttributes();
        String elementType = (String) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);

        if (elementType == null) {
            return new TranscriptElementLocation(-2, null, -1);
        }

        int transcriptElementIndex = -1;
        String label = null;
        int posInTier = -1;

        switch (elementType) {
            case TranscriptStyleConstants.ATTR_KEY_RECORD -> {
                Record record = (Record) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_RECORD);
                if (record == null) {
                    return new TranscriptElementLocation(-1, null, -1);
                }
                int recordIndex = transcript.getRecordPosition(record);
//                if (recordIndex == -1) {
//                    return new TranscriptElementLocation(-1, null, -1);
//                }
                transcriptElementIndex = recordIndex == -1 ? -2 : transcript.getElementIndex(record);
                Tier<?> tier = (Tier<?>) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);
                if (tier != null) {
                    label = tier.getName();
                    int contentStart = doc.getTierContentStart(recordIndex, tier.getName());
                    if(contentStart > 0) {
                        posInTier = charPos - doc.getTierContentStart(recordIndex, tier.getName());
                    } else {
                        posInTier = 0;
                    }
                }
            }
            case TranscriptStyleConstants.ATTR_KEY_COMMENT -> {
                Comment comment = (Comment) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_COMMENT);
                transcriptElementIndex = transcript.getElementIndex(comment);
                label = comment.getType().getLabel();
                posInTier = charPos - doc.getCommentContentStart(comment);
            }
            case TranscriptStyleConstants.ATTR_KEY_GEM -> {
                Gem gem = (Gem) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GEM);
                transcriptElementIndex = transcript.getElementIndex(gem);
                label = gem.getType().name() + " Gem";
                posInTier = doc.getGemContentStart(gem);
            }
            case TranscriptStyleConstants.ATTR_KEY_GENERIC_TIER -> {
                Tier<?> genericTier = (Tier<?>) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GENERIC_TIER);
                if (genericTier != null) {
                    label = genericTier.getName();
                    posInTier = doc.getGenericContentStart(genericTier);
                }
            }
        }

        return new TranscriptElementLocation(transcriptElementIndex, label, posInTier);
    }

    /**
     * Converts a session location into a character position in the document
     *
     * @param transcriptLocation the session location object
     * @return the converted character position
     */
    public int sessionLocationToCharPos(TranscriptElementLocation transcriptLocation) {
        TranscriptDocument doc = getTranscriptDocument();
        Transcript transcript = getSession().getTranscript();

        if (transcriptLocation.transcriptElementIndex() > -1) {
            Transcript.Element transcriptElement = transcript.getElementAt(transcriptLocation.transcriptElementIndex());

            if (transcriptElement.isRecord()) {
                int recordIndex = transcript.getRecordPosition(transcriptElement.asRecord());
                return doc.getTierContentStart(recordIndex, transcriptLocation.tier()) + transcriptLocation.charPosition();
            } else if (transcriptElement.isComment()) {
                return doc.getCommentContentStart(transcriptElement.asComment()) + transcriptLocation.charPosition();
            } else if (transcriptElement.isGem()) {
                return doc.getGemContentStart(transcriptElement.asGem()) + transcriptLocation.charPosition();
            }
        }

        return -1;
    }


    /**
     * Underlines the given document element
     *
     * @param elem the element to underline
     */
    private void underlineElement(Element elem) {
        try {
            removeCurrentUnderline();
            // get element text
            String text = elem.getDocument().getText(elem.getStartOffset(), elem.getEndOffset() - elem.getStartOffset());
            // special case for labels
            if (text.startsWith("\t")) {
                currentUnderline = getHighlighter().addHighlight(elem.getStartOffset() + 1, elem.getEndOffset(), underlinePainter);
            } else {
                currentUnderline = getHighlighter().addHighlight(elem.getStartOffset(), elem.getEndOffset(), underlinePainter);
            }
            repaint();
        } catch (BadLocationException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Removes the current underline
     */
    private void removeCurrentUnderline() {
        if (currentUnderline != null) {
            getHighlighter().removeHighlight(currentUnderline);
            currentUnderline = null;
            repaint();
        }
    }

    /**
     * Sets the box selection to the specified bounds
     *
     * @param bounds a touple containing the upper and lower bounds positions
     */
    public void boxSelectBounds(TranscriptDocument.StartEnd bounds) {
        try {
            removeCurrentBoxSelect();
            currentBoxSelect = getHighlighter().addHighlight(bounds.start(), bounds.end(), boxSelectPainter);
        } catch (BadLocationException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Removes the current box select
     */
    public void removeCurrentBoxSelect() {
        if (currentBoxSelect != null) {
            getHighlighter().removeHighlight(currentBoxSelect);
            currentBoxSelect = null;
        }
    }

    /**
     * Adds a comment to the transcript relative to the given transcript element
     *
     * @param relativeElementIndex the index that the comment is added relative to
     * @param position             {@link SwingConstants} {@code PREVIOUS}, {@code NEXT}, or {@code BOTTOM}
     */
    private void addComment(int relativeElementIndex, int position) {
        Transcript transcript = getSession().getTranscript();
        Tier<TierData> commentTier = SessionFactory.newFactory().createTier("Comment Tier", TierData.class);
        commentTier.setText("");
        Comment newComment = SessionFactory.newFactory().createComment(CommentType.Generic, commentTier.getValue());

        int newCommentIndex = -1;
        switch (position) {
            case SwingConstants.PREVIOUS -> newCommentIndex = relativeElementIndex;
            case SwingConstants.NEXT -> newCommentIndex = relativeElementIndex + 1;
            case SwingConstants.BOTTOM -> newCommentIndex = transcript.getNumberOfElements();
        }

        AddTranscriptElementEdit edit = new AddTranscriptElementEdit(getSession(), eventManager, new Transcript.Element(newComment), newCommentIndex);
        undoSupport.postEdit(edit);
    }

    /**
     * Runs when a comment is added to the transcript.
     * Adds the comment to the document.
     *
     * @param editorEvent the event that adds the comment to the transcript
     */
    private void onCommentAdded(EditorEvent<EditorEventType.CommentAddedData> editorEvent) {
        var data = editorEvent.data();
        getTranscriptDocument().addComment(data.comment(), data.elementIndex());

        // set the caret to the start of the comment
        final TranscriptDocument.StartEnd commentRange = getTranscriptDocument().getCommentContentStartEnd(data.comment());
        if (commentRange.valid()) {
            SwingUtilities.invokeLater(() -> setCaretPosition(commentRange.start()));
        }
    }

    /**
     * Adds a gem to the transcript relative to the given transcript element
     *
     * @param relativeElementIndex the index that the gem is added relative to
     * @param position             {@link SwingConstants} {@code PREVIOUS}, {@code NEXT}, or {@code BOTTOM}
     */
    private void addGem(int relativeElementIndex, int position) {
        Transcript transcript = getSession().getTranscript();
        Gem newGem = SessionFactory.newFactory().createGem(GemType.Lazy, "");

        int newGemIndex = -1;
        switch (position) {
            case SwingConstants.PREVIOUS -> newGemIndex = relativeElementIndex;
            case SwingConstants.NEXT -> newGemIndex = relativeElementIndex + 1;
            case SwingConstants.BOTTOM -> newGemIndex = transcript.getNumberOfElements();
        }

        AddTranscriptElementEdit edit = new AddTranscriptElementEdit(getSession(), eventManager, new Transcript.Element(newGem), newGemIndex);
        undoSupport.postEdit(edit);
    }

    /**
     * Runs when a gem is added to the transcript.
     * Adds the gem to the document.
     *
     * @param editorEvent the event that adds the gem to the transcript
     */
    private void onGemAdded(EditorEvent<EditorEventType.GemAddedData> editorEvent) {
        var data = editorEvent.data();
        getTranscriptDocument().addGem(data.gem(), data.elementIndex());

        // set the caret to the start of the gem
        final TranscriptDocument.StartEnd gemRange = getTranscriptDocument().getGemContentStartEnd(data.gem());
        if (gemRange.valid()) {
            SwingUtilities.invokeLater(() -> setCaretPosition(gemRange.start()));
        }
    }

    /**
     * Runs when an element is deleted from the transcript.
     * Deletes the transcript element from the document
     *
     * @param editorEvent the event that deleted the element
     */
    private void onTranscriptElementDeleted(EditorEvent<EditorEventType.ElementDeletedData> editorEvent) {
        if (!editorEvent.data().element().isRecord()) {
            getTranscriptDocument().deleteTranscriptElement(editorEvent.data().elementIndex(), editorEvent.data().element());
        }
    }

    /**
     * Runs when a comments type is changed.
     * Updates the type in the document.
     *
     * @param editorEvent the event that changed the type
     */
    private void onCommentTypeChanged(EditorEvent<EditorEventType.CommentTypeChangedData> editorEvent) {
        getTranscriptDocument().onChangeCommentType(editorEvent.data().comment());
    }

    /**
     * Runs when a gems type is changed.
     * Updates the type in the document.
     *
     * @param editorEvent the event that changed the type
     */
    private void onGemTypeChanged(EditorEvent<EditorEventType.GemTypeChangedData> editorEvent) {
        getTranscriptDocument().onChangeGemType(editorEvent.data().gem());
    }

    /**
     * Runs when the session location changes.
     * Sets the {@code currentTranscriptLocation} to the new location
     */
    private void onSessionLocationChanged(EditorEvent<TranscriptLocationChangeData> editorEvent) {
        try {
            SwingUtilities.invokeAndWait(() -> {
                setCurrentSessionLocation(editorEvent.data().newLoc);

                final TranscriptElementLocation oldLoc = editorEvent.data().oldLoc;
                final TranscriptElementLocation newLoc = editorEvent.data().newLoc;

                if (oldLoc.tier() != null && !oldLoc.tier().equals(newLoc.tier())) {
                    // header tiers are updated in the HeaderTierExtension
                    if (oldLoc.transcriptElementIndex() >= 0) {
                        commitChanges(sessionLocationToCharPos(oldLoc));
                    }
                }
            });
        } catch (InterruptedException | InvocationTargetException e) {
            LogUtil.warning(e);
        }
    }

    private void onParticipantChanged() {

    }

    /**
     * Checks whether a given attribute set contains any "not traversable" attributes
     *
     * @param attrs the attribute set to be checked
     * @return if any "not traversable" attributes were found
     */
    private boolean containsNotTraversableAttribute(AttributeSet attrs) {
        for (String key : notTraversableAttributes) {
            if (attrs.getAttribute(key) != null) return true;
        }
        return false;
    }

    /**
     * Adds an attribute to the "not traversable" set
     *
     * @param attributeKey the attribute to be added
     */
    public void addNotTraversableAttribute(String attributeKey) {
        notTraversableAttributes.add(attributeKey);
    }

    /**
     * Removes an attribute from the "not traversable" set
     *
     * @param attributeKey the attribute to be removed
     */
    public void removeNotTraversableAttribute(String attributeKey) {
        notTraversableAttributes.remove(attributeKey);
    }

    @Override
    public Set<Class<?>> getExtensions() {
        return extensionSupport.getExtensions();
    }

    @Override
    public <T> T getExtension(Class<T> cap) {
        return extensionSupport.getExtension(cap);
    }

    @Override
    public <T> T putExtension(Class<T> cap, T impl) {
        return extensionSupport.putExtension(cap, impl);
    }

    @Override
    public <T> T removeExtension(Class<T> cap) {
        return extensionSupport.removeExtension(cap);
    }

    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {

    }

    /**
     * A record that contains the data for session location change events
     *
     * @param oldLoc the previous session location
     * @param newLoc the new session location
     */
    public record TranscriptLocationChangeData(TranscriptElementLocation oldLoc, TranscriptElementLocation newLoc) {

        @Override
        public String toString() {
            return "TranscriptLocationChangeData{" +
                    "oldLoc=" + oldLoc +
                    ", newLoc=" + newLoc +
                    '}';
        }

    }

    /**
     * The {@link Highlighter.HighlightPainter} that paints the error underlines
     */
    public static class ErrorUnderlinePainter implements Highlighter.HighlightPainter {

        @Override
        public void paint(Graphics g, int p0, int p1, Shape bounds, JTextComponent c) {
            try {
                var firstCharRect = c.modelToView2D(p0);
                var lastCharRect = c.modelToView2D(p1);
                g.setColor(Color.RED);
                int lineY = ((int) firstCharRect.getMaxY()) - 9;
                g.drawLine((int) firstCharRect.getMinX(), lineY, (int) lastCharRect.getMaxX(), lineY);
            } catch (BadLocationException e) {
                LogUtil.warning(e);
            }
        }
    }

    public record RecordParticipant(
            Record record,
            Participant participant
    ) {
    }

    /**
     * The {@link Highlighter.HighlightPainter} that paints the "clickable" underlines
     */
    private class HoverUnderlinePainter implements Highlighter.HighlightPainter {

        @Override
        public void paint(Graphics g, int p0, int p1, Shape bounds, JTextComponent c) {
            try {
                var firstCharRect = modelToView2D(p0);
                var lastCharRect = modelToView2D(p1);
                g.setColor(UIManager.getColor(TranscriptEditorUIProps.CLICKABLE_HOVER_UNDERLINE));
                int lineY = ((int) firstCharRect.getMaxY()) - 9;
                g.drawLine((int) firstCharRect.getMinX(), lineY, (int) lastCharRect.getMaxX(), lineY);
            } catch (BadLocationException e) {
                LogUtil.warning(e);
            }
        }
    }

    /**
     * The {@link MouseAdapter} that handles mouse movement and clicking for the transcript editor
     */
    private class TranscriptMouseAdapter extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            TranscriptDocument doc = getTranscriptDocument();
            int mouseButton = e.getButton();

            // Left click
            if (mouseButton == MouseEvent.BUTTON1) {
                int mousePosInDoc = viewToModel2D(e.getPoint());

                Element elem = doc.getCharacterElement(mousePosInDoc);
                AttributeSet attrs = elem.getAttributes();

                if (TranscriptStyleConstants.isNotTraversable(attrs)) {
                    String elementType = TranscriptStyleConstants.getElementType(attrs);
                    if (elementType != null) {
                        if (e.getClickCount() > 1) {
                            switch (elementType) {
                                case TranscriptStyleConstants.ELEMENT_TYPE_RECORD -> {
                                    Record record = TranscriptStyleConstants.getRecord(attrs);
                                    if (record == null) return;
                                    final int recordIndex = getSession().getRecordPosition(record);
                                    if (recordIndex == -1) return;
                                    Tier<?> tier = TranscriptStyleConstants.getTier(attrs);
                                    final TranscriptDocument.StartEnd startEnd = doc.getTierContentStartEnd(recordIndex, tier.getName());
                                    if (startEnd.valid()) {
                                        select(startEnd.start(), startEnd.end());
                                    }
                                }

                                case TranscriptStyleConstants.ELEMENT_TYPE_COMMENT -> {
                                    Comment comment = TranscriptStyleConstants.getComment(attrs);
                                    final TranscriptDocument.StartEnd startEnd = doc.getCommentContentStartEnd(comment);
                                    if (startEnd.valid()) {
                                        select(startEnd.start(), startEnd.end());
                                    }
                                }
                                case TranscriptStyleConstants.ELEMENT_TYPE_GEM -> {
                                    Gem gem = TranscriptStyleConstants.getGEM(attrs);
                                    final TranscriptDocument.StartEnd startEnd = doc.getGemContentStartEnd(gem);
                                    if (startEnd.valid()) {
                                        select(startEnd.start(), startEnd.end());
                                    }
                                }
                            }
                        } else {
                            setCaretPosition(getNextValidIndex(mousePosInDoc, false));
                        }

                        final BiConsumer<MouseEvent, AttributeSet> clickHandler = TranscriptStyleConstants.getClickHandler(attrs);
                        if (clickHandler != null) {
                            clickHandler.accept(e, attrs);
                        }
                    }
                }

                String elementType = TranscriptStyleConstants.getElementType(attrs);
                if (elementType != null) {
                    if (e.getClickCount() == 3) {
                        switch (elementType) {
                            case TranscriptStyleConstants.ELEMENT_TYPE_RECORD -> {
                                Record record = TranscriptStyleConstants.getRecord(attrs);
                                if (record == null) return;
                                final int recordIndex = getSession().getRecordPosition(record);
                                if (recordIndex == -1) return;
                                Tier<?> tier = TranscriptStyleConstants.getTier(attrs);
                                if (tier != null) {
                                    final TranscriptDocument.StartEnd startEnd = doc.getTierContentStartEnd(recordIndex, tier.getName());
                                    if (startEnd.valid()) {
                                        select(startEnd.start(), startEnd.end());
                                    }
                                }
                            }
                            case TranscriptStyleConstants.ELEMENT_TYPE_COMMENT -> {
                                Comment comment = TranscriptStyleConstants.getComment(attrs);
                                if (comment != null) {
                                    final TranscriptDocument.StartEnd startEnd = doc.getCommentContentStartEnd(comment);
                                    if (startEnd.valid()) {
                                        select(startEnd.start(), startEnd.end());
                                    }
                                }
                            }
                            case TranscriptStyleConstants.ELEMENT_TYPE_GEM -> {
                                Gem gem = TranscriptStyleConstants.getGEM(attrs);
                                if (gem != null) {
                                    final TranscriptDocument.StartEnd startEnd = doc.getGemContentStartEnd(gem);
                                    if (startEnd.valid()) {
                                        select(startEnd.start(), startEnd.end());
                                    }
                                }
                            }
                            case TranscriptStyleConstants.ELEMENT_TYPE_GENERIC -> {
                                Tier<?> generic = TranscriptStyleConstants.getGenericTier(attrs);
                                if (generic != null) {
                                    final TranscriptDocument.StartEnd startEnd = doc.getGenericContentStartEnd(generic);
                                    if (startEnd.valid()) {
                                        select(startEnd.start(), startEnd.end());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            TranscriptDocument doc = getTranscriptDocument();

            // Highlighting elements on hover with CMD/CTRL held, useful for debugging
//            if ((e.getModifiersEx() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()) == Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()) {
//                highlightElementAtPoint(e.getPoint());
//            } else if (currentHighlight != null) {
//                removeCurrentHighlight();
//            }

            int mousePosInDoc = viewToModel2D(e.getPoint());

            Element elem = doc.getCharacterElement(mousePosInDoc);
            if (elem != null) {
                if (elem.equals(hoverElem)) return;
                AttributeSet attrs = elem.getAttributes();
                boolean isClickable = TranscriptStyleConstants.isUnderlineOnHover(attrs);
                boolean isWhitespace = doc.getCharAtPos(mousePosInDoc).equals(' ');
                if (isClickable && !isWhitespace) {
                    hoverElem = elem;
                    underlineElement(elem);
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    return;
                }
            }
            if (hoverElem != null) {
                hoverElem = null;
                removeCurrentUnderline();
                setCursor(Cursor.getDefaultCursor());
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            if (hoverElem != null) {
                hoverElem = null;
                removeCurrentUnderline();
                setCursor(Cursor.getDefaultCursor());
            }
        }
    }

    private void addHighlightForSelection(SessionEditorSelection selection) {
        Highlighter.HighlightPainter painter = selection.getExtension(Highlighter.HighlightPainter.class);
        if (painter == null) {
            painter = new DefaultHighlighter.DefaultHighlightPainter(UIManager.getColor("TextArea.selectionBackground"));
        }

        final Transcript.Element transcriptElement = getSession().getTranscript().getElementAt(selection.getElementIndex());
        if (transcriptElement.isRecord()) {
            final int recordIndex = getSession().getTranscript().getRecordIndex(selection.getElementIndex());
            int tierStart = getTranscriptDocument().getTierContentStart(recordIndex, selection.getTierName());
            if (tierStart == -1) return;
            try {
                var selectionHighlight = getHighlighter().addHighlight(selection.getRange().getFirst() + tierStart, selection.getRange().getLast() + tierStart + 1, painter);
                selectionHighlightList.add(selectionHighlight);
                selectionMap.put(selection, selectionHighlight);
            } catch (BadLocationException e) {
                LogUtil.warning(e);
            }
        } else if (transcriptElement.isComment()) {
            final Comment comment = transcriptElement.asComment();
            final TranscriptDocument.StartEnd commentRange = getTranscriptDocument().getCommentContentStartEnd(comment);
            if (commentRange.valid()) {
                try {
                    var selectionHighlight = getHighlighter().addHighlight(commentRange.start() + selection.getRange().getFirst(), commentRange.start() + selection.getRange().getLast() + 1, painter);
                    selectionHighlightList.add(selectionHighlight);
                    selectionMap.put(selection, selectionHighlight);
                } catch (BadLocationException e) {
                    LogUtil.warning(e);
                }
            }
        } else if (transcriptElement.isGem()) {
            final Gem gem = transcriptElement.asGem();
            final TranscriptDocument.StartEnd gemRange = getTranscriptDocument().getGemContentStartEnd(gem);
            if (gemRange.valid()) {
                try {
                    var selectionHighlight = getHighlighter().addHighlight(gemRange.start() + selection.getRange().getFirst(), gemRange.start() + selection.getRange().getLast() + 1, painter);
                    selectionHighlightList.add(selectionHighlight);
                    selectionMap.put(selection, selectionHighlight);
                } catch (BadLocationException e) {
                    LogUtil.warning(e);
                }
            }
        }
    }

    /**
     * The {@link EditorSelectionModelListener} that listens for selections for the transcript editor
     */
    private class TranscriptSelectionListener implements EditorSelectionModelListener {

        @Override
        public void selectionAdded(EditorSelectionModel model, SessionEditorSelection selection) {
            addHighlightForSelection(selection);
        }

        @Override
        public void selectionRemoved(EditorSelectionModel model, SessionEditorSelection selection) {
            Object highlight = selectionMap.get(selection);
            if (highlight != null) {
                getHighlighter().removeHighlight(highlight);
                selectionHighlightList.remove(highlight);
                selectionMap.remove(selection);
            }
        }

        @Override
        public void selectionSet(EditorSelectionModel model, SessionEditorSelection selection) {
            selectionsCleared(model);
            selectionAdded(model, selection);
        }

        @Override
        public void selectionsCleared(EditorSelectionModel model) {
            for(Object highlight : selectionHighlightList) {
                getHighlighter().removeHighlight(highlight);
            }
            selectionHighlightList.clear();
            selectionMap.clear();
            repaint();
        }

        @Override
        public void requestSwitchToRecord(EditorSelectionModel model, int recordIndex) {
            Session session = getSession();
            Record record = session.getRecord(recordIndex);
            var data = new EditorEventType.RecordChangedData(record, session.getRecordElementIndex(record), recordIndex);
            final EditorEvent<EditorEventType.RecordChangedData> e = new EditorEvent<>(EditorEventType.RecordChanged, TranscriptEditor.this, data);
            eventManager.queueEvent(e);
        }
    }

    private class CustomTransferHandler extends TransferHandler {

        @Override
        public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {
            if (getSelectionStart() == getSelectionEnd()) {
                return;
            }

            try {
                StringSelection selection = new StringSelection(getSelectedText());
                clip.setContents(selection, selection);
            } catch (IllegalStateException e) {
                LogUtil.warning(e);
            }
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }

            try {
                String data = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);

                // if data contains a newline character, return
                if (data.isEmpty() || data.contains("\n")) {
                    Toolkit.getDefaultToolkit().beep();
                    return false;
                }

                final AttributeSet attrs = getTranscriptDocument().getCharacterElement(getCaretPosition()).getAttributes();
                final String elementType = TranscriptStyleConstants.getElementType(attrs);
                if (elementType == null) {
                    Toolkit.getDefaultToolkit().beep();
                    return false;
                }

                if (TranscriptStyleConstants.isNotTraversable(attrs) || TranscriptStyleConstants.isNotEditable(attrs)) {
                    Toolkit.getDefaultToolkit().beep();
                    return false;
                }

                if (getSelectionStart() >= 0 && getSelectionEnd() >= 0) {
                    getTranscriptDocument().remove(getSelectionStart(), getSelectionEnd() - getSelectionStart());
                }

                TranscriptBatchBuilder batchBuilder = new TranscriptBatchBuilder(getTranscriptDocument());
                batchBuilder.appendBatchString(data, attrs);
                getTranscriptDocument().processBatchUpdates(getCaretPosition(), batchBuilder.getBatch());

                return true;
            } catch (UnsupportedFlavorException | IOException | BadLocationException e) {
                Toolkit.getDefaultToolkit().beep();
                LogUtil.severe(e);
                return false;
            }
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.stringFlavor);
        }
    }

}
