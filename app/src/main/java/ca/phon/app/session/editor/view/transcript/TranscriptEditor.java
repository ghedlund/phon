package ca.phon.app.session.editor.view.transcript;

import ca.phon.app.log.LogUtil;
import ca.phon.app.session.editor.*;
import ca.phon.app.session.editor.undo.*;
import ca.phon.extensions.ExtensionSupport;
import ca.phon.extensions.IExtendable;
import ca.phon.session.Record;
import ca.phon.session.*;
import ca.phon.session.position.TranscriptElementLocation;
import ca.phon.session.tierdata.TierData;
import ca.phon.ui.IconStrip;
import ca.phon.ui.action.PhonActionEvent;
import ca.phon.ui.action.PhonUIAction;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.List;
import java.util.*;
import java.util.function.BiConsumer;

public class TranscriptEditor extends JEditorPane implements IExtendable {

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

    /**
     * A set of attributes that the caret should not be able to move to
     */
    private final Set<String> notTraversableAttributes;

    private SessionMediaModel mediaModel;

    private final EditorSelectionModel selectionModel;

    /**
     * A reference to the current debug highlight object
     */
    private Object currentHighlight;

    /**
     * The index of the current record
     */
    private int currentRecordIndex = -1;

    /**
     * Whether the transcript editor is currently in "single record view" mode
     */
    private boolean singleRecordView = false;

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

//    /**
//     * Whether the next edit will change trigger any changes to the document
//     */
//    private boolean internalEdit = false;

    /**
     * A reference to the current box selection highlight object
     */
    private Object currentBoxSelect = null;

    /**
     * The session location of the current caret position
     */
    private TranscriptElementLocation currentTranscriptLocation = null;

    /**
     * The custom editor caret
     */
    private final TranscriptEditorCaret caret;

    /**
     * The icon strip for the toolbar
     */
    private final IconStrip iconStrip = new IconStrip(SwingConstants.HORIZONTAL);

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
//        setOpaque(false);
        initActions();
        registerEditorActions();
        super.setEditorKitForContentType(TranscriptEditorKit.CONTENT_TYPE, new TranscriptEditorKit());
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
                if(recordIdx >= 0) {
                    setCurrentRecordIndex(recordIdx);
                }
            }
        });
        selectionModel.addSelectionModelListener(new TranscriptSelectionListener());

        notTraversableAttributes = new HashSet<>();
        notTraversableAttributes.add(TranscriptStyleConstants.ATTR_KEY_NOT_TRAVERSABLE);

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
        PhonUIAction<Void> rightAct = PhonUIAction.runnable(() -> setCaretPosition(getNextValidIndex(getCaretPosition() + 1, true)));
        actionMap.put("nextValidIndex", rightAct);

        KeyStroke left = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0);
        inputMap.put(left, "prevValidIndex");
        PhonUIAction<Void> leftAct = PhonUIAction.runnable(() -> setCaretPosition(getPrevValidIndex(getCaretPosition() - 1, true)));
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

        KeyStroke delete = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
        inputMap.put(delete, "deleteElement");
        PhonUIAction<Void> deleteAct = PhonUIAction.runnable(() -> {
            TranscriptDocument doc = getTranscriptDocument();

            int currentPos = getCaretPosition();
            var currentPosAttrs = getTranscriptDocument().getCharacterElement(currentPos).getAttributes();
            String elementType = (String) currentPosAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);

            boolean atEndOfTier = false;

            switch (elementType) {
                case "record" -> {
                    currentPosAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_RECORD);
                    Tier<?> tier = (Tier<?>) currentPosAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);
                    if (tier == null) return;
                    int endPos = doc.getTierEnd(tier);

                    atEndOfTier = currentPos + 1 == endPos;
                }
                case "comment" -> {
                    Comment currentComment = (Comment) currentPosAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_COMMENT);
                    if (currentComment == null) return;
                    int endPos = doc.getCommentEnd(currentComment);

                    atEndOfTier = currentPos + 1 == endPos;
                }
                case "gem" -> {
                    Gem currentGem = (Gem) currentPosAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GEM);
                    if (currentGem == null) return;
                    int endPos = doc.getGemEnd(currentGem);

                    atEndOfTier = currentPos + 1 == endPos;
                }
                case "generic" -> {
                    Tier<?> currentGeneric = (Tier<?>) currentPosAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GENERIC_TIER);
                    if (currentGeneric == null) return;
                    int endPos = doc.getGenericEnd(currentGeneric);

                    atEndOfTier = currentPos + 1 == endPos;
                }
            }

            if (!atEndOfTier) {
                try {
                    getTranscriptDocument().remove(currentPos, 1);
                } catch (BadLocationException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        actionMap.put("deleteElement", deleteAct);
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

    public Session getSession() {
        return dataModel.getSession();
    }

    public TranscriptDocument getTranscriptDocument() {
        return (TranscriptDocument) getDocument();
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
     * Is the document in single record view
     * @return true if single record view, false otherwise
     */
    public boolean isSingleRecordView() {
        return singleRecordView;
    }

    /**
     * Sets whether the document is in single record view
     *
     * @param singleRecordView
     */
    public void setSingleRecordView(boolean singleRecordView) {
        var wasSingleRecordView = this.singleRecordView;
        this.singleRecordView = singleRecordView;
        if(wasSingleRecordView != singleRecordView)
            getTranscriptDocument().setSingleRecordView(singleRecordView);
        firePropertyChange("singleRecordView", wasSingleRecordView, singleRecordView);
    }

    /**
     * Toggle single record view
     *
     */
    public void toggleSingleRecordView() {
        setSingleRecordView(!isSingleRecordView());
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
     * Runs when the user presses enter
     */
    public void onPressedEnter(PhonActionEvent<Void> pae) {

        TranscriptDocument doc = getTranscriptDocument();
        AttributeSet attrs = doc.getCharacterElement(getCaretPosition()).getAttributes();
        var enterAct = attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_ENTER_ACTION);
        if (enterAct instanceof Action action) {
            action.actionPerformed(pae.getActionEvent());
            return;
        }

        String elemType = (String) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);
        if (elemType != null) {
            try {
                switch (elemType) {
                    case TranscriptStyleConstants.ATTR_KEY_RECORD -> {
                        Tier<?> tier = (Tier<?>) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);
                        if (tier == null) return;
                        int start = doc.getTierContentStart(tier);
                        int end = doc.getTierEnd(tier) - 1;
                        String newVal = doc.getText(start, end - start);
                        if (!tier.toString().equals(newVal)) {
                            changeTierData((Record) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_RECORD), tier, newVal);
                        }
                    }
                    case TranscriptStyleConstants.ATTR_KEY_COMMENT -> {
                        Comment comment = (Comment) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_COMMENT);
                        if (comment == null) return;
                        int start = doc.getCommentContentStart(comment);
                        int end = doc.getCommentEnd(comment) - 1;
                        commentDataChanged(comment, doc.getText(start, end - start));
                    }
                    case TranscriptStyleConstants.ATTR_KEY_GEM -> {
                        Gem gem = (Gem) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GEM);
                        if (gem == null) return;
                        int start = doc.getGemContentStart(gem);
                        int end = doc.getGemEnd(gem) - 1;
                        gemDataChanged(gem, doc.getText(start, end - start));
                    }
//                    case TranscriptStyleConstants.ATTR_KEY_GENERIC_TIER -> {
//                        Tier<?> genericTier = (Tier<?>) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GENERIC_TIER);
//                        if (genericTier == null) return;
//                        int start = doc.getGenericContentStart(genericTier);
//                        int end = doc.getGenericEnd(genericTier) - 1;
//                        genericDataChanged(genericTier, doc.getText(start, end - start));
//                    }
                }
            } catch (BadLocationException e) {
                LogUtil.severe(e);
            }

            if (elemType.equals(TranscriptStyleConstants.ATTR_KEY_RECORD) || elemType.equals(TranscriptStyleConstants.ATTR_KEY_COMMENT) || elemType.equals(TranscriptStyleConstants.ATTR_KEY_GEM)) {
                int elementIndex;
                if (elemType.equals(TranscriptStyleConstants.ATTR_KEY_RECORD)) {
                    elementIndex = getSession().getRecordElementIndex((Record) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_RECORD));
                } else if (elemType.equals(TranscriptStyleConstants.ATTR_KEY_COMMENT)) {
                    Comment comment = (Comment) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_COMMENT);
                    elementIndex = getSession().getTranscript().getElementIndex(comment);
                } else {
                    Gem gem = (Gem) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GEM);
                    elementIndex = getSession().getTranscript().getElementIndex(gem);
                }
                if (elementIndex > -1) {
                    try {
                        Rectangle2D caretRect = modelToView2D(getCaretPosition());
                        Point point = new Point((int) caretRect.getCenterX(), (int) caretRect.getMaxY());
                        showContextMenu(elementIndex, point);
                    } catch (BadLocationException e) {
                        LogUtil.severe(e);
                    }
                }
            }
        }
    }

    /**
     * Runs when the user presses home
     */
    public void onPressedHome() {
        TranscriptDocument doc = getTranscriptDocument();

        Element caretElem = doc.getCharacterElement(getCaretPosition());
        AttributeSet attrs = caretElem.getAttributes();
        String elementType = (String) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);
        if (elementType == null) return;
        int start = -1;
        switch (elementType) {
            case TranscriptStyleConstants.ATTR_KEY_RECORD -> {
                Tier<?> tier = (Tier<?>) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);
                if (tier != null) {
                    start = doc.getTierContentStart(tier);
                }
            }
            case TranscriptStyleConstants.ATTR_KEY_COMMENT -> {
                Comment comment = (Comment) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_COMMENT);
                if (comment != null) {
                    start = doc.getCommentContentStart(comment);
                }
            }
            case TranscriptStyleConstants.ATTR_KEY_GEM -> {
                Gem gem = (Gem) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GEM);
                if (gem != null) {
                    start = doc.getGemContentStart(gem);
                }
            }
            case TranscriptStyleConstants.ATTR_KEY_GENERIC_TIER -> {
                Tier<?> genericTier = (Tier<?>) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GENERIC_TIER);
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
     * Runs when the user presses end
     */
    public void onPressedEnd() {
        TranscriptDocument doc = getTranscriptDocument();

        Element caretElem = doc.getCharacterElement(getCaretPosition());
        AttributeSet attrs = caretElem.getAttributes();
        String elementType = (String) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);
        if (elementType == null) return;
        int end = -1;
        switch (elementType) {
            case TranscriptStyleConstants.ATTR_KEY_RECORD -> {
                Tier<?> tier = (Tier<?>) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);
                if (tier != null) {
                    end = doc.getTierEnd(tier);
                }
            }
            case TranscriptStyleConstants.ATTR_KEY_COMMENT -> {
                Comment comment = (Comment) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_COMMENT);
                if (comment != null) {
                    end = doc.getCommentEnd(comment);
                }
            }
            case TranscriptStyleConstants.ATTR_KEY_GEM -> {
                Gem gem = (Gem) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GEM);
                if (gem != null) {
                    end = doc.getGemEnd(gem);
                }
            }
            case TranscriptStyleConstants.ATTR_KEY_GENERIC_TIER -> {
                Tier<?> genericTier = (Tier<?>) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GENERIC_TIER);
                if (genericTier != null) {
                    end = doc.getGenericEnd(genericTier);
                }
            }
        }
        if (end != -1) {
            setCaretPosition(end - 1);
        }
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
        getTranscriptDocument().addRecord(addedRecord, elementIndex);
    }

    /**
     * Runs when a record gets deleted
     *
     * @param editorEvent the event that deletes the record
     */
    private void onRecordDeleted(EditorEvent<EditorEventType.RecordDeletedData> editorEvent) {
        TranscriptDocument doc = getTranscriptDocument();

        int deletedRecordIndex = editorEvent.data().recordIndex();

        int startCaretPos = getCaretPosition();
        var elem = doc.getCharacterElement(startCaretPos);
        var caretAttrs = elem.getAttributes();
        Tier<?> caretTier = (Tier<?>) caretAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);
        Record caretRecord = (Record) caretAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_RECORD);

        boolean caretInDeletedRecord = caretRecord != null && caretRecord == editorEvent.data().record();

        int caretOffset = doc.getOffsetInContent(startCaretPos);


        // Delete the record from the doc
        var data = editorEvent.data();
        getTranscriptDocument().deleteRecord(data.elementIndex(), data.recordIndex(), data.record());

        // Caret in record / tier
        if (caretTier != null) {
            // Caret in deleted record
            if (caretInDeletedRecord) {
                boolean deletedRecordWasLast = deletedRecordIndex == getSession().getRecordCount();

                int newCaretRecordIndex = deletedRecordWasLast ? deletedRecordIndex - 1 : deletedRecordIndex;

                int newCaretTierStart = doc.getTierContentStart(newCaretRecordIndex, caretTier.getName());
                int newCaretTierEnd = doc.getTierEnd(newCaretRecordIndex, caretTier.getName());

                int newCaretPos = Math.min(newCaretTierStart + caretOffset, newCaretTierEnd - 1);
                setCaretPosition(newCaretPos);
            }
            // Caret in record not deleted
            else {
                setCaretPosition(doc.getTierContentStart(caretTier) + caretOffset);
            }
        }
        // Caret not in record / tier
        else {
            String elementType = (String) caretAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);
            int start = -1;
            switch (elementType) {
                case TranscriptStyleConstants.ATTR_KEY_COMMENT ->
                        start = doc.getCommentContentStart((Comment) caretAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_COMMENT));
                case TranscriptStyleConstants.ATTR_KEY_GEM ->
                        start = doc.getGemContentStart((Gem) caretAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GEM));
                case TranscriptStyleConstants.ATTR_KEY_GENERIC_TIER ->
                        start = doc.getGenericContentStart((Tier<?>) caretAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GENERIC_TIER));
            }

            setCaretPosition(start + caretOffset);
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

        // Set the caret to the editable pos closest to the original pos
        setCaretPosition(getNextValidIndex(caretPos, false));
    }

    /**
     * Runs when the speaker for a record changes
     *
     * @param editorEvent the event that changes the speaker
     */
    private void onSpeakerChanged(EditorEvent<EditorEventType.SpeakerChangedData> editorEvent) {
        var data = editorEvent.data();
        // Update the speaker on the separator in the doc
        getTranscriptDocument().onChangeSpeaker(data.record());
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
        if(!isMediaSegmentTier && editorEvent.getData().get().valueAdjusting()) return;

        if (errorUnderlineHighlights.containsKey(changedTier)) {
            getHighlighter().removeHighlight(errorUnderlineHighlights.get(changedTier));
            errorUnderlineHighlights.remove(changedTier);
        }

        int start = -1;
        int end = -1;

        if (changedTier.isUnvalidated()) {
            start = doc.getTierContentStart(changedTier) + changedTier.getUnvalidatedValue().getParseError().getErrorOffset();
            end = doc.getTierContentStart(changedTier) + changedTier.getUnvalidatedValue().getValue().length();
        }

        final TranscriptElementLocation caretLoc = getTranscriptEditorCaret().getTranscriptLocation();
        final int currentDot = getTranscriptEditorCaret().getDot();

        boolean wasCaretFrozen = getTranscriptEditorCaret().isFreezeCaret();
        getTranscriptEditorCaret().freeze();
        // Update the changed tier data in the doc
        getTranscriptDocument().onTierDataChanged(editorEvent.data().record(), changedTier);
        final int newDot = sessionLocationToCharPos(caretLoc);
        if(newDot != currentDot) {
            getTranscriptDocument().setBypassDocumentFilter(true);
            getTranscriptEditorCaret().setDot(newDot, true);
            getTranscriptDocument().setBypassDocumentFilter(false);
        }
        if(!wasCaretFrozen) {
            getTranscriptEditorCaret().unfreeze();
        }

        if (changedTier.isUnvalidated()) {
            try {
                var errorUnderlineHighlight = getHighlighter().addHighlight(start, end, new ErrorUnderlinePainter());
                errorUnderlineHighlights.put(changedTier, errorUnderlineHighlight);
            } catch (BadLocationException e) {
                LogUtil.severe(e);
            }
        }
    }

    public EditorEventManager getEventManager() {
        return eventManager;
    }

    public SessionEditUndoSupport getUndoSupport() {
        return undoSupport;
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
     * Shows a context menu with options to add comments and gems at positions relative to the
     * specified transcript element
     *
     * @param transcriptElementIndex the index of the transcript element that the comments and
     *                               gems should be added relative to
     * @param pos                    the position on the screen for the context menu to appear
     */
    private void showContextMenu(int transcriptElementIndex, Point pos) {
        JPopupMenu menu = new JPopupMenu();

        JMenu addCommentMenu = new JMenu("Add comment");

        JMenuItem addCommentAbove = new JMenuItem();
        PhonUIAction<Void> addCommentAboveAct = PhonUIAction.runnable(() -> addComment(transcriptElementIndex, SwingConstants.PREVIOUS));
        addCommentAboveAct.putValue(PhonUIAction.NAME, "Add comment above");
        addCommentAbove.setAction(addCommentAboveAct);
        addCommentMenu.add(addCommentAbove);

        JMenuItem addCommentBelow = new JMenuItem();
        PhonUIAction<Void> addCommentBelowAct = PhonUIAction.runnable(() -> addComment(transcriptElementIndex, SwingConstants.NEXT));
        addCommentBelowAct.putValue(PhonUIAction.NAME, "Add comment below");
        addCommentBelow.setAction(addCommentBelowAct);
        addCommentMenu.add(addCommentBelow);

        JMenuItem addCommentBottom = new JMenuItem();
        PhonUIAction<Void> addCommentBottomAct = PhonUIAction.runnable(() -> addComment(transcriptElementIndex, SwingConstants.BOTTOM));
        addCommentBottomAct.putValue(PhonUIAction.NAME, "Add comment at bottom");
        addCommentBottom.setAction(addCommentBottomAct);
        addCommentMenu.add(addCommentBottom);
        menu.add(addCommentMenu);

        JMenu addGemMenu = new JMenu("Add gem");

        JMenuItem addGemAbove = new JMenuItem();
        PhonUIAction<Void> addGemAboveAct = PhonUIAction.runnable(() -> addGem(transcriptElementIndex, SwingConstants.PREVIOUS));
        addGemAboveAct.putValue(PhonUIAction.NAME, "Add gem above");
        addGemAbove.setAction(addGemAboveAct);
        addGemMenu.add(addGemAbove);

        JMenuItem addGemBelow = new JMenuItem();
        PhonUIAction<Void> addGemBelowAct = PhonUIAction.runnable(() -> addGem(transcriptElementIndex, SwingConstants.NEXT));
        addGemBelowAct.putValue(PhonUIAction.NAME, "Add gem below");
        addGemBelow.setAction(addGemBelowAct);
        addGemMenu.add(addGemBelow);

        JMenuItem addGemBottom = new JMenuItem();
        PhonUIAction<Void> addGemBottomAct = PhonUIAction.runnable(() -> addGem(transcriptElementIndex, SwingConstants.BOTTOM));
        addGemBottomAct.putValue(PhonUIAction.NAME, "Add gem at bottom");
        addGemBottom.setAction(addGemBottomAct);
        addGemMenu.add(addGemBottom);

        menu.add(addGemMenu);

//        JMenuItem deleteThis = new JMenuItem();
//        PhonUIAction<Void> deleteThisAct = PhonUIAction.runnable(() -> deleteTranscriptElement(getSession().getTranscript().getElementAt(transcriptElementIndex)));
//        deleteThisAct.putValue(PhonUIAction.NAME, "Delete this element");
//        deleteThis.setAction(deleteThisAct);
//        menu.add(deleteThis);

        menu.show(this, (int) pos.getX(), (int) pos.getY());
    }

    /**
     * Saves the changes made to the line the caret is currently on
     */
    public void saveCurrentLine() {
        TranscriptElementLocation transcriptLocation = getCurrentSessionLocation();
        if (transcriptLocation.transcriptElementIndex() < 0) return;

        TranscriptDocument doc = getTranscriptDocument();

        var elem = getSession().getTranscript().getElementAt(transcriptLocation.transcriptElementIndex());
        try {
            if (elem.isRecord()) {
                Record record = elem.asRecord();
                String tierName = transcriptLocation.tier();
                Tier<?> tier = record.getTier(tierName);
                int startPos = doc.getTierContentStart(tier);
                int endPos = doc.getTierEnd(tier) - 1;
                if (startPos >= 0 && endPos >= 0) {
                    changeTierData(record, tier, doc.getText(startPos, endPos - startPos));
                }
            } else if (elem.isComment()) {
                Comment comment = elem.asComment();
                int startPos = doc.getCommentContentStart(comment);
                int endPos = doc.getCommentEnd(comment) - 1;
                if (startPos >= 0 && endPos >= 0) {
                    commentDataChanged(comment, doc.getText(startPos, endPos - startPos));
                }
            } else if (elem.isGem()) {
                Gem gem = elem.asGem();
                int startPos = doc.getGemContentStart(gem);
                int endPos = doc.getGemEnd(gem) - 1;
                if (startPos >= 0 && endPos >= 0) {
                    gemDataChanged(gem, doc.getText(startPos, endPos - startPos));
                }
            }
        } catch (BadLocationException e) {
            LogUtil.severe(e);
        }
    }

    public TranscriptElementLocation getCurrentSessionLocation() {
        return currentTranscriptLocation;
    }

    public void setCurrentSessionLocation(TranscriptElementLocation currentTranscriptLocation) {
        this.currentTranscriptLocation = currentTranscriptLocation;
    }

    /**
     * Changes the data in a given tier if the provided data is different
     *
     * @param record  the record that the tier belongs to
     * @param tier    the tier that the data is changing for
     * @param newData the possible new data for the tier
     */
    public void changeTierData(Record record, Tier<?> tier, String newData) {
        TranscriptDocument doc = getTranscriptDocument();
        String transcriber = dataModel.getTranscriber().getUsername();

        Tier<?> dummy = SessionFactory.newFactory().createTier("dummy", tier.getDeclaredType());
        dummy.setText(newData);

        if (tier.getDeclaredType() == MediaSegment.class) return;
        if (doc.getTierText(tier, transcriber).equals(doc.getTierText(dummy, transcriber))) return;

        SwingUtilities.invokeLater(() -> {
            TierEdit<?> edit = new TierEdit(getSession(), eventManager, dataModel.getTranscriber(), record, tier, dummy.getValue());
            edit.setValueAdjusting(false);
            getUndoSupport().postEdit(edit);
        });
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

        SwingUtilities.invokeLater(() -> {
            ChangeCommentEdit edit = new ChangeCommentEdit(getSession(), eventManager, comment, dummy.getValue());
            getUndoSupport().postEdit(edit);
        });
    }

    /**
     * Changes the data in a given gem if the provided data is different
     *
     * @param gem     the gem that the data is changing for
     * @param newData the possible new data for the tier
     */
    public void gemDataChanged(Gem gem, String newData) {

        if (gem.getLabel().equals(newData)) return;

        SwingUtilities.invokeLater(() -> {
            ChangeGemEdit edit = new ChangeGemEdit(getSession(), eventManager, gem, newData);
            getUndoSupport().postEdit(edit);
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        // XXX background colors are not being drawn correctly on windows
//        Graphics2D g2d = (Graphics2D) g;
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
        if(getHighlighter() != null) {
            for(var highlight : getHighlighter().getHighlights()) {
                if(highlight.getPainter() instanceof BoxSelectHighlightPainter || highlight.getPainter() instanceof ErrorUnderlinePainter
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
        switch (changeType) {
            case MOVE_TIER -> moveTier(editorEvent.data());
            case RELOAD -> getTranscriptDocument().reload();
            case DELETE_TIER -> deleteTier(editorEvent.data());
            case ADD_TIER -> addTier(editorEvent.data());
            case HIDE_TIER -> hideTier(editorEvent.data());
            case SHOW_TIER -> showTier(editorEvent.data());
            case TIER_NAME_CHANGE -> tierNameChanged(editorEvent.data());
            case TIER_FONT_CHANGE -> tierFontChanged(editorEvent.data());
            default -> {
            }
        }
    }

    /**
     * Runs when the "current record" changes
     *
     * @param editorEvent the event that says the current record haas changed
     */
    private void onRecordChanged(EditorEvent<EditorEventType.RecordChangedData> editorEvent) {
        TranscriptDocument doc = getTranscriptDocument();

        // Update the single record index in the doc
        doc.setSingleRecordIndex(editorEvent.data().recordIndex());

        // If it's currently in single record view fire the appropriate event
        if (doc.getSingleRecordView()) {
            final EditorEvent<Void> e = new EditorEvent<>(recordChangedInSingleRecordMode, this, null);
            eventManager.queueEvent(e);
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

    /**
     * Moves the specified tiers (and the caret if needed)
     *
     * @param data the data from the tier view changed event
     */
    public void moveTier(EditorEventType.TierViewChangedData data) {
        var startTierView = data.oldTierView();
        var endTierView = data.newTierView();

        List<TierViewItem> movedTiers = new ArrayList<>();
        for (int i = 0; i < startTierView.size(); i++) {
            if (!startTierView.get(i).equals(endTierView.get(i))) {
                movedTiers.add(startTierView.get(i));
            }
        }

        TranscriptDocument doc = getTranscriptDocument();

        // Check if caret affected by move
        int startCaretPos = getCaretPosition();
        var elem = doc.getCharacterElement(startCaretPos);
        Tier<?> caretTier = (Tier<?>) elem.getAttributes().getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);
        int caretTierOffset = -1;

        if (caretTier != null) {
            String caretTierName = caretTier.getName();
            boolean caretTierHasMoved = movedTiers.stream().anyMatch(item -> item.getTierName().equals(caretTierName));
            if (caretTierHasMoved) {
                caretTierOffset = startCaretPos - elem.getStartOffset();
            }
        }

        Document blank = getEditorKit().createDefaultDocument();
        setDocument(blank);
        // Move tier in doc
        doc.reload();
        setDocument(doc);

        // Correct caret
        if (caretTierOffset > -1) {
            // Move the caret so that it has the same offset from the tiers new pos
            setCaretPosition(doc.getTierContentStart(caretTier) + caretTierOffset);
        }
    }

    /**
     * Deletes the specified tiers (and moves the caret if needed)
     *
     * @param data the data from the tier view changed event
     */
    public void deleteTier(EditorEventType.TierViewChangedData data) {
        TranscriptDocument doc = getTranscriptDocument();

        List<String> deletedTiersNames = data.tierNames();

        int startCaretPos = getCaretPosition();
        var elem = doc.getCharacterElement(startCaretPos);
        var caretAttrs = elem.getAttributes();
        Tier<?> caretTier = (Tier<?>) caretAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);

        boolean caretInDeletedTier = caretTier != null && deletedTiersNames.contains(caretTier.getName());

        int caretOffset = doc.getOffsetInContent(startCaretPos);

        Document blank = getEditorKit().createDefaultDocument();
        setDocument(blank);
        // Delete tier in doc
        doc.reload();
        setDocument(doc);


        // Caret in record / tier
        if (caretTier != null) {

            int caretRecordIndex = getSession().getRecordPosition((Record) caretAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_RECORD));

            // Caret in deleted tier
            if (caretInDeletedTier) {
                var oldTierView = data.oldTierView();

                boolean passedCaretTier = false;
                TierViewItem newCaretTierViewItem = null;
                for (TierViewItem item : oldTierView) {
                    if (item.getTierName().equals(caretTier.getName())) {
                        passedCaretTier = true;
                    } else if (!deletedTiersNames.contains(item.getTierName()) && item.isVisible()) {
                        newCaretTierViewItem = item;
                        if (passedCaretTier) break;
                    }
                }

                int newCaretTierStart = doc.getTierContentStart(caretRecordIndex, newCaretTierViewItem.getTierName());
                int newCaretTierEnd = doc.getTierEnd(caretRecordIndex, newCaretTierViewItem.getTierName());

                int newCaretPos = Math.min(newCaretTierStart + caretOffset, newCaretTierEnd - 1);
                setCaretPosition(newCaretPos);
            }
            // Caret in tier not deleted
            else {
                setCaretPosition(doc.getTierContentStart(caretTier) + caretOffset);
            }
        }
        // Caret not in record / tier
        else {
            String elementType = (String) caretAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);
            int start = -1;
            switch (elementType) {
                case TranscriptStyleConstants.ATTR_KEY_COMMENT ->
                        start = doc.getCommentContentStart((Comment) caretAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_COMMENT));
                case TranscriptStyleConstants.ATTR_KEY_GEM ->
                        start = doc.getGemContentStart((Gem) caretAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GEM));
                case TranscriptStyleConstants.ATTR_KEY_GENERIC_TIER ->
                        start = doc.getGenericContentStart((Tier<?>) caretAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GENERIC_TIER));
            }

            setCaretPosition(start + caretOffset);
        }
    }

    /**
     * Adds the specified tiers (and moves the caret if needed)
     *
     * @param data the data from the tier view changed event
     */
    public void addTier(EditorEventType.TierViewChangedData data) {
        var doc = getTranscriptDocument();

        int startCaretPos = getCaretPosition();
        var elem = doc.getCharacterElement(startCaretPos);
        Tier<?> caretTier = (Tier<?>) elem.getAttributes().getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);
        int caretTierOffset = -1;

        if (caretTier != null) {
            caretTierOffset = startCaretPos - elem.getStartOffset();
        }

        Document blank = getEditorKit().createDefaultDocument();
        setDocument(blank);
        // Add tier in doc
        doc.reload();
        setDocument(doc);

        // Correct caret
        if (caretTierOffset > -1) {
            // Move the caret so that it has the same offset from the tiers new pos
            setCaretPosition(doc.getTierContentStart(caretTier) + caretTierOffset);
        } else {
            // Put the caret back where it was before the move
            setCaretPosition(startCaretPos);
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

        int startCaretPos = getCaretPosition();
        var elem = doc.getCharacterElement(startCaretPos);
        var caretAttrs = elem.getAttributes();
        Tier<?> caretTier = (Tier<?>) caretAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);

        boolean caretInHiddenTier = caretTier != null && hiddenTiersNames.contains(caretTier.getName());

        int caretOffset = doc.getOffsetInContent(startCaretPos);


        Document blank = getEditorKit().createDefaultDocument();
        setDocument(blank);
        // Hide tier in doc
        doc.reload();
        setDocument(doc);


        // Caret in record / tier
        if (caretTier != null) {

            int caretRecordIndex = getSession().getRecordPosition((Record) caretAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_RECORD));

            // Caret in hidden tier
            if (caretInHiddenTier) {
                var oldTierView = data.oldTierView();

                boolean passedCaretTier = false;
                TierViewItem newCaretTierViewItem = null;
                for (TierViewItem item : oldTierView) {
                    if (item.getTierName().equals(caretTier.getName())) {
                        passedCaretTier = true;
                    } else if (!hiddenTiersNames.contains(item.getTierName()) && item.isVisible()) {
                        newCaretTierViewItem = item;
                        if (passedCaretTier) break;
                    }
                }

                int newCaretTierStart = doc.getTierContentStart(caretRecordIndex, newCaretTierViewItem.getTierName());
                int newCaretTierEnd = doc.getTierEnd(caretRecordIndex, newCaretTierViewItem.getTierName());

                int newCaretPos = Math.min(newCaretTierStart + caretOffset, newCaretTierEnd - 1);
                setCaretPosition(newCaretPos);
            }
            // Caret in tier not deleted
            else {
                setCaretPosition(doc.getTierContentStart(caretTier) + caretOffset);
            }
        }
        // Caret not in record / tier
        else {
            String elementType = (String) caretAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);
            int start = -1;
            switch (elementType) {
                case TranscriptStyleConstants.ATTR_KEY_COMMENT ->
                        start = doc.getCommentContentStart((Comment) caretAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_COMMENT));
                case TranscriptStyleConstants.ATTR_KEY_GEM ->
                        start = doc.getGemContentStart((Gem) caretAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GEM));
                case TranscriptStyleConstants.ATTR_KEY_GENERIC_TIER ->
                        start = doc.getGenericContentStart((Tier<?>) caretAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GENERIC_TIER));
            }

            setCaretPosition(start + caretOffset);
        }
    }

    /**
     * Shows the specified tiers (and moves the caret if needed)
     *
     * @param data the data from the tier view changed event
     */
    public void showTier(EditorEventType.TierViewChangedData data) {
        var doc = getTranscriptDocument();

        int startCaretPos = getCaretPosition();
        var elem = doc.getCharacterElement(startCaretPos);
        Tier<?> caretTier = (Tier<?>) elem.getAttributes().getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);
        int caretTierOffset = -1;

        if (caretTier != null) {
            caretTierOffset = startCaretPos - elem.getStartOffset();
        }

        Document blank = getEditorKit().createDefaultDocument();
        setDocument(blank);
        // Show tier in doc
        doc.reload();
        setDocument(doc);

        // Correct caret
        if (caretTierOffset > -1) {
            // Move the caret so that it has the same offset from the tiers new pos
            setCaretPosition(doc.getTierContentStart(caretTier) + caretTierOffset);
        } else {
            // Put the caret back where it was before the move
            setCaretPosition(startCaretPos);
        }
    }

    /**
     * Updates the names of the specified tiers
     *
     * @param data the data from the tier view changed event
     */
    public void tierNameChanged(EditorEventType.TierViewChangedData data) {

        boolean nothingChanged = true;
        for (int i = 0; i < data.newTierView().size(); i++) {
            if (!data.newTierView().get(i).getTierName().equals(data.oldTierView().get(i).getTierName())) {
                nothingChanged = false;
                break;
            }
        }
        if (nothingChanged) return;

        int caretPos = getCaretPosition();

        TranscriptDocument doc = getTranscriptDocument();
        Document blank = getEditorKit().createDefaultDocument();
        setDocument(blank);
        doc.reload();
        setDocument(doc);

        setCaretPosition(caretPos);
    }

    /**
     * Updates the fonts of the specified tiers
     *
     * @param data the data from the tier view changed event
     */
    public void tierFontChanged(EditorEventType.TierViewChangedData data) {
        TranscriptDocument doc = getTranscriptDocument();
        int caretPos = getCaretPosition();

        List<TierViewItem> changedTiers = data.newTierView().stream().filter(item -> data.tierNames().contains(item.getTierName())).toList();

        if (changedTiers.isEmpty()) return;

        Document blank = getEditorKit().createDefaultDocument();
        setDocument(blank);
        // Change tier font in doc
        doc.reload();
        setDocument(doc);

        setCaretPosition(caretPos);
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

    /**
     * Highlights the document element at the given point
     *
     * @param point the point on the editor
     */
    private void highlightElementAtPoint(Point2D point) {
        int mousePosInDoc = viewToModel2D(point);
        var elem = getTranscriptDocument().getCharacterElement(mousePosInDoc);
        try {
            removeCurrentHighlight();
            currentHighlight = getHighlighter().addHighlight(elem.getStartOffset(), elem.getEndOffset(), highlightPainter);
            repaint();
        } catch (BadLocationException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Removes the current highlight
     */
    private void removeCurrentHighlight() {
        if (currentHighlight != null) {
            getHighlighter().removeHighlight(currentHighlight);
            currentHighlight = null;
            SwingUtilities.invokeLater(this::repaint);
        }
    }

    /**
     * Loads the session on the document
     */
    public void loadSession() {
        TranscriptDocument doc = getTranscriptDocument();
        doc.setUndoSupport(undoSupport);
        doc.setEventManager(eventManager);

        doc.setSession(getSession());
        doc.addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                TranscriptDocument doc = getTranscriptDocument();
                Element elem = doc.getCharacterElement(e.getOffset());
                AttributeSet attrs = elem.getAttributes();
                if (attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_SYLLABIFICATION) != null) {
                    int tierEnd = doc.getTierEnd((Tier<?>) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER)) - 1;
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
            end = doc.getTierEnd((Tier<?>) prevElementAttributes.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER));
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

        int newCaretPos = Math.min(end - 1, start + offsetInContent);

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
            end = doc.getTierEnd(TranscriptStyleConstants.getTier(nextElementAttributes));
        } else if (elementType.equals(TranscriptStyleConstants.ELEMENT_TYPE_COMMENT)) {
            end = doc.getCommentEnd(TranscriptStyleConstants.getComment(nextElementAttributes));
        } else if (elementType.equals(TranscriptStyleConstants.ELEMENT_TYPE_GEM)) {
            end = doc.getGemEnd(TranscriptStyleConstants.getGEM(nextElementAttributes));
        } else if (elementType.equals(TranscriptStyleConstants.ELEMENT_TYPE_GENERIC)) {
            end = doc.getGenericEnd(TranscriptStyleConstants.getGenericTier(nextElementAttributes));
        } else {
            return;
        }

        int newCaretPos = Math.min(end - 1, start + offsetInContent);

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
                if(TranscriptStyleConstants.isNewParagraph(attrs)) {
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
                if(TranscriptStyleConstants.isNewParagraph(attrs)) {
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

        if(elementType == null) {
            return new TranscriptElementLocation(-1, null, -1);
        }

        int transcriptElementIndex = -1;
        String label = null;
        int posInTier = -1;

        switch (elementType) {
            case TranscriptStyleConstants.ATTR_KEY_RECORD -> {
                Record record = (Record) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_RECORD);
                transcriptElementIndex = transcript.getElementIndex(record);
                Tier<?> tier = (Tier<?>) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);
                if (tier != null) {
                    label = tier.getName();
                    posInTier = charPos - doc.getTierContentStart(tier);
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
            if(text.startsWith("\t")) {
                currentUnderline = getHighlighter().addHighlight(elem.getStartOffset()+1, elem.getEndOffset(), underlinePainter);
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
                        final Transcript.Element transcriptElement = getSession().getTranscript().getElementAt(oldLoc.transcriptElementIndex());
                        if (transcriptElement.isRecord()) {
                            final Record record = transcriptElement.asRecord();
                            final Tier<?> tier = record.getTier(oldLoc.tier());
                            if (tier != null) {
                                final TranscriptDocument.StartEnd tierContentRange = getTranscriptDocument().getTierContentStartEnd(
                                        getSession().getTranscript().getRecordIndex(oldLoc.transcriptElementIndex()), tier.getName());
                                try {
                                    final String currentText = getTranscriptDocument().getText(tierContentRange.start(), tierContentRange.length());
                                    if (!tier.toString().equals(currentText)) {
                                        changeTierData(record, tier, currentText);
                                    }
                                } catch (BadLocationException e) {
                                    LogUtil.warning(e);
                                }
                            }
                        } else if (transcriptElement.isGem()) {
                            final Gem gem = transcriptElement.asGem();
                            final TranscriptDocument.StartEnd gemRange = getTranscriptDocument().getGemContentStartEnd(gem);
                            try {
                                final String currentText = getTranscriptDocument().getText(gemRange.start(), gemRange.length());
                                if (!gem.toString().equals(currentText)) {
                                    gemDataChanged(gem, currentText);
                                }
                            } catch (BadLocationException e) {
                                LogUtil.warning(e);
                            }
                        } else if (transcriptElement.isComment()) {
                            final Comment comment = transcriptElement.asComment();
                            final TranscriptDocument.StartEnd commentRange = getTranscriptDocument().getCommentContentStartEnd(comment);
                            try {
                                final String currentText = getTranscriptDocument().getText(commentRange.start(), commentRange.length());
                                if (!comment.toString().equals(currentText)) {
                                    commentDataChanged(comment, currentText);
                                }
                            } catch (BadLocationException e) {
                                LogUtil.warning(e);
                            }
                        }
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

    /**
     * The {@link Highlighter.HighlightPainter} that paints "box selection"
     */
    private class BoxSelectHighlightPainter implements Highlighter.HighlightPainter {

        @Override
        public void paint(Graphics g, int p0, int p1, Shape bounds, JTextComponent c) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(UIManager.getColor(TranscriptEditorUIProps.SEGMENT_SELECTION));

            try {
                var p0Rect = modelToView2D(p0);
                var p1Rect = modelToView2D(p1);

                Element ele = getTranscriptDocument().getCharacterElement(p0);
                int actualLineHeight = g.getFontMetrics().getHeight();
                if (ele != null) {
                    final AttributeSet attrs = ele.getAttributes();
                    if (StyleConstants.getFontFamily(attrs) != null && StyleConstants.getFontSize(attrs) > 0) {
                        int style = (StyleConstants.isBold(attrs) ? Font.BOLD : 0) | (StyleConstants.isItalic(attrs) ? Font.ITALIC : 0);
                        final Font f = new Font(StyleConstants.getFontFamily(attrs), style, StyleConstants.getFontSize(attrs));
                        actualLineHeight = g.getFontMetrics(f).getHeight();
                    }
                }

                g2d.drawRect((int) p0Rect.getMinX(), (int) p0Rect.getMinY(), (int) (p1Rect.getMaxX() - p0Rect.getMinX()) - 1, actualLineHeight - 1);
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
                                    Tier<?> tier = TranscriptStyleConstants.getTier(attrs);
                                    select(doc.getTierContentStart(tier), doc.getTierEnd(tier));
                                }
                                case TranscriptStyleConstants.ELEMENT_TYPE_COMMENT -> {
                                    Comment comment = TranscriptStyleConstants.getComment(attrs);
                                    select(doc.getCommentContentStart(comment), doc.getCommentEnd(comment));
                                }
                                case TranscriptStyleConstants.ELEMENT_TYPE_GEM -> {
                                    Gem gem = TranscriptStyleConstants.getGEM(attrs);
                                    select(doc.getGemContentStart(gem), doc.getGemEnd(gem));
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
                                Tier<?> tier = TranscriptStyleConstants.getTier(attrs);
                                if (tier != null) {
                                    setSelectionStart(doc.getTierContentStart(tier));
                                    setSelectionEnd(doc.getTierEnd(tier) - 1);
                                }
                            }
                            case TranscriptStyleConstants.ELEMENT_TYPE_COMMENT -> {
                                Comment comment = TranscriptStyleConstants.getComment(attrs);
                                if (comment != null) {
                                    setSelectionStart(doc.getCommentContentStart(comment));
                                    setSelectionEnd(doc.getCommentEnd(comment) - 1);
                                }
                            }
                            case TranscriptStyleConstants.ELEMENT_TYPE_GEM -> {
                                Gem gem = TranscriptStyleConstants.getGEM(attrs);
                                if (gem != null) {
                                    setSelectionStart(doc.getGemContentStart(gem));
                                    setSelectionEnd(doc.getGemEnd(gem) - 1);
                                }
                            }
                            case TranscriptStyleConstants.ELEMENT_TYPE_GENERIC -> {
                                Tier<?> generic = TranscriptStyleConstants.getGenericTier(attrs);
                                if (generic != null) {
                                    setSelectionStart(doc.getGenericContentStart(generic));
                                    setSelectionEnd(doc.getGenericEnd(generic) - 1);
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

            if ((e.getModifiersEx() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()) == Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()) {
                highlightElementAtPoint(e.getPoint());
            } else if (currentHighlight != null) {
                removeCurrentHighlight();
            }

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
    }

    /**
     * The {@link EditorSelectionModelListener} that listens for selections for the transcript editor
     */
    private class TranscriptSelectionListener implements EditorSelectionModelListener {

        @Override
        public void selectionAdded(EditorSelectionModel model, SessionEditorSelection selection) {
            Highlighter.HighlightPainter painter = selection.getExtension(Highlighter.HighlightPainter.class);
            if (painter == null) {
                painter = new DefaultHighlighter.DefaultHighlightPainter(UIManager.getColor("TextArea.selectionBackground"));
            }

            try {
                int tierStart = getTranscriptDocument().getTierContentStart(selection.getElementIndex(), selection.getTierName());
                if (tierStart == -1) return;

                var selectionHighlight = getHighlighter().addHighlight(selection.getRange().getFirst() + tierStart, selection.getRange().getLast() + tierStart, painter);
                selectionHighlightList.add(selectionHighlight);
            } catch (BadLocationException e) {
                LogUtil.severe(e);
            }
        }

        @Override
        public void selectionSet(EditorSelectionModel model, SessionEditorSelection selection) {

            for (Object selectionHighLight : selectionHighlightList) {
                getHighlighter().removeHighlight(selectionHighLight);
            }
            selectionHighlightList.clear();

            Highlighter.HighlightPainter painter = selection.getExtension(Highlighter.HighlightPainter.class);
            if (painter == null) {
                painter = new DefaultHighlighter.DefaultHighlightPainter(UIManager.getColor("TextArea.selectionBackground"));
            }

            try {
                int tierStart = getTranscriptDocument().getTierContentStart(selection.getElementIndex(), selection.getTierName());
                if (tierStart == -1) return;

                var selectionHighlight = getHighlighter().addHighlight(selection.getRange().getFirst() + tierStart, selection.getRange().getLast() + tierStart, painter);
                selectionHighlightList.add(selectionHighlight);
            } catch (BadLocationException e) {
                LogUtil.severe(e);
            }
        }

        @Override
        public void selectionsCleared(EditorSelectionModel model) {
            for (Object selectionHighLight : selectionHighlightList) {
                getHighlighter().removeHighlight(selectionHighLight);
            }
            selectionHighlightList.clear();
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

    @Override
    public TransferHandler getTransferHandler() {
        return new CustomTransferHandler();
    }

    public record RecordParticipant(
            Record record,
            Participant participant
    ) {}

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
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.stringFlavor);
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }

            try {
                String data = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);

                // if data contains a newline character, return
                if(data.isEmpty() || data.contains("\n")) {
                    Toolkit.getDefaultToolkit().beep();
                    return false;
                }

                final AttributeSet attrs = getTranscriptDocument().getCharacterElement(getCaretPosition()).getAttributes();
                final String elementType = TranscriptStyleConstants.getElementType(attrs);
                if(elementType == null) {
                    Toolkit.getDefaultToolkit().beep();
                    return false;
                }

                if(TranscriptStyleConstants.isNotTraversable(attrs) || TranscriptStyleConstants.isNotEditable(attrs)) {
                    Toolkit.getDefaultToolkit().beep();
                    return false;
                }

                if(getSelectionStart() >= 0 && getSelectionEnd() >= 0) {
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
    }

}
