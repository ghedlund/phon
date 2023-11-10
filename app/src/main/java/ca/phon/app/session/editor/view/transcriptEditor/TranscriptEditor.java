package ca.phon.app.session.editor.view.transcriptEditor;

import ca.phon.app.log.LogUtil;
import ca.phon.app.session.editor.*;
import ca.phon.app.session.editor.undo.*;
import ca.phon.extensions.ExtensionSupport;
import ca.phon.extensions.IExtendable;
import ca.phon.plugin.PluginManager;
import ca.phon.session.*;
import ca.phon.session.Record;
import ca.phon.session.tierdata.TierData;
import ca.phon.ui.action.PhonActionEvent;
import ca.phon.ui.action.PhonUIAction;
import ca.phon.ui.fonts.FontPreferences;
import ca.phon.ui.menu.MenuBuilder;
import ca.phon.util.OSInfo;
import ca.phon.util.Tuple;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

public class TranscriptEditor extends JEditorPane implements IExtendable {

    /* Editor models */
    private final EditorDataModel dataModel;
    private final EditorEventManager eventManager;
    private SessionMediaModel mediaModel;
    private EditorSelectionModel selectionModel;

    /* Undo support */
    private SessionEditUndoSupport undoSupport;
    private UndoManager undoManager;

    /* Extension support */
    private final ExtensionSupport extensionSupport = new ExtensionSupport(TranscriptEditor.class, this);

    /* State */
    private Object currentHighlight;
    private DefaultHighlighter.DefaultHighlightPainter highlightPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);
    private int currentRecordIndex = -1;
    private boolean singleRecordView = false;
    public final static EditorEventType<Void> recordChangedInSingleRecordMode = new EditorEventType<>("recordChangedInSingleRecordMode", Void.class);
    private Element hoverElem = null;
    private Object currentUnderline;
    private HoverUnderlinePainter underlinePainter = new HoverUnderlinePainter();
    private int upDownOffset = -1;
    private boolean caretMoveFromUpDown = false;
    private boolean internalEdit = false;
    private Map<Tier<?>, Object> errorUnderlineHighlights = new HashMap<>();
    private BoxSelectHighlightPainter boxSelectPainter = new BoxSelectHighlightPainter();
    private Object currentBoxSelect = null;
    private List<Object> selectionHighlightList = new ArrayList<>();
    public final static EditorEventType<SessionLocationChangeData> sessionLocationChange = new EditorEventType<>("recordChangedInSingleRecordMode", SessionLocationChangeData.class);
    private SessionLocation currentSessionLocation = null;

    public TranscriptEditor(
        Session session,
        EditorEventManager eventManager,
        SessionEditUndoSupport undoSupport,
        UndoManager undoManager
    ) {
        this(new DefaultEditorDataModel(null, session), new DefaultEditorSelectionModel(), eventManager, undoSupport, undoManager);
    }
    public TranscriptEditor(
        EditorDataModel dataModel,
        EditorSelectionModel selectionModel,
        EditorEventManager eventManager,
        SessionEditUndoSupport undoSupport,
        UndoManager undoManager
    ) {
        super();
        final TranscriptEditorCaret caret = new TranscriptEditorCaret();
        setCaret(caret);
        getCaret().deinstall(this);
        caret.install(this);
        this.dataModel = dataModel;
        this.selectionModel = selectionModel;
        this.eventManager = eventManager;
        this.undoSupport = undoSupport;
        this.undoManager = undoManager;
        initActions();
        registerEditorActions();
        super.setEditorKitForContentType(TranscriptEditorKit.CONTENT_TYPE, new TranscriptEditorKit());
        setContentType(TranscriptEditorKit.CONTENT_TYPE);
        setOpaque(false);
        setNavigationFilter(new TranscriptNavigationFilter());
        TranscriptMouseAdapter mouseAdapter = new TranscriptMouseAdapter();
        addMouseMotionListener(mouseAdapter);
        addMouseListener(mouseAdapter);

        addCaretListener(e -> {
            TranscriptDocument doc = getTranscriptDocument();
            String transcriptElementType = (String) doc.getCharacterElement(e.getDot()).getAttributes().getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);
            if (transcriptElementType != null && transcriptElementType.equals(TranscriptStyleConstants.ATTR_KEY_RECORD)) {
                setCurrentRecordIndex(doc.getRecordIndex(e.getDot()));
            }

            // FOR DEBUG PURPOSES ONLY
//            SimpleAttributeSet attrs = new SimpleAttributeSet(doc.getCharacterElement(e.getDot()).getAttributes().copyAttributes());
//            System.out.println("Attrs: " + attrs);
//            System.out.println("Dot: " + e.getDot());
//            Tier<?> tier = ((Tier<?>) attrs.getAttribute(TranscriptDocument.ATTR_KEY_TIER));
//            System.out.println(tier == null ? null : tier.getName());
//            System.out.println(attrs);
        });
        selectionModel.addSelectionModelListener(new TranscriptSelectionListener());

        // init extensions
        extensionSupport.initExtensions();
    }

    public TranscriptEditor(Session session) {
        this(session, new EditorEventManager(), new SessionEditUndoSupport(), new UndoManager());
    }


    // region Init

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
        PhonUIAction<Void> enterAct = PhonUIAction.eventConsumer(this::pressedEnter, null);
        actionMap.put("pressedEnter", enterAct);


        KeyStroke home = KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0);
        inputMap.put(home, "pressedHome");
        PhonUIAction<Void> homeAct = PhonUIAction.runnable(this::pressedHome);
        actionMap.put("pressedHome", homeAct);

        KeyStroke end = KeyStroke.getKeyStroke(KeyEvent.VK_END, 0);
        inputMap.put(end, "pressedEnd");
        PhonUIAction<Void> endAct = PhonUIAction.runnable(this::pressedEnd);
        actionMap.put("pressedEnd", endAct);
    }

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

        this.eventManager.registerActionForEvent(sessionLocationChange, this::onSessionLocationChanged, EditorEventManager.RunOn.AWTEventDispatchThread);
    }

    // endregion Init


    // region Getters and Setters

    public TranscriptDocument getTranscriptDocument() {
        return (TranscriptDocument) getDocument();
    }

    public Session getSession() {
        return dataModel.getSession();
    }

    public void setMediaModel(SessionMediaModel mediaModel) {
        var oldModel = this.mediaModel;
        this.mediaModel = mediaModel;
        firePropertyChange("mediaModel", oldModel, mediaModel);
    }

    public SessionMediaModel getMediaModel() {
        return this.mediaModel;
    }

    public SegmentPlayback getSegmentPlayback() {
        return (mediaModel != null ? mediaModel.getSegmentPlayback() : null);
    }

    public SessionEditUndoSupport getUndoSupport() {
        return undoSupport;
    }

    public UndoManager getUndoManager() {
        return undoManager;
    }

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

    public void setCurrentElementIndex(int index) {

        Transcript.Element transcriptElem = getSession().getTranscript().getElementAt(index);
        String transcriptElemType;
        if (transcriptElem.isComment()) {transcriptElemType = TranscriptStyleConstants.ATTR_KEY_COMMENT;}
        else if (transcriptElem.isGem()) {transcriptElemType = TranscriptStyleConstants.ATTR_KEY_GEM;}
        else {transcriptElemType = TranscriptStyleConstants.ATTR_KEY_RECORD;}

        var root = getTranscriptDocument().getDefaultRootElement();
        for (int i = 0; i < root.getElementCount(); i++) {
            Element elem = root.getElement(i);
            for (int j = 0; j < elem.getElementCount(); j++) {
                Element innerElem = elem.getElement(j);
                String elemType = (String) innerElem.getAttributes().getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);
                if (elemType != null && elemType.equals(transcriptElemType)) {
                    if (transcriptElem.isComment()) {
                        Comment comment = (Comment) innerElem.getAttributes().getAttribute(TranscriptStyleConstants.ATTR_KEY_COMMENT);
                        if (comment.equals(transcriptElem.asComment())) setCaretPosition(innerElem.getStartOffset());
                    }
                    else if (transcriptElem.isGem()) {
                        Gem gem = (Gem) innerElem.getAttributes().getAttribute(TranscriptStyleConstants.ATTR_KEY_GEM);
                        if (gem.equals(transcriptElem.asGem())) setCaretPosition(innerElem.getStartOffset());
                    }
                    else {
                        Record record = (Record) innerElem.getAttributes().getAttribute(TranscriptStyleConstants.ATTR_KEY_RECORD);
                        if (record.equals(transcriptElem.asRecord())) setCaretPosition(innerElem.getStartOffset());
                    }
                }
            }
        }
    }

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

    public void setCurrentRecordIndex(int index) {
        int oldIndex = this.currentRecordIndex;
        this.currentRecordIndex = index;
        super.firePropertyChange("currentRecordIndex", oldIndex, this.currentRecordIndex);
    }

    public boolean isSingleRecordView() {
        return singleRecordView;
    }

    public void setSingleRecordView(boolean singleRecordView) {
        this.singleRecordView = singleRecordView;
    }

    public EditorEventManager getEventManager() {
        return eventManager;
    }

    public boolean isTranscriberValidator() {
        return dataModel.getTranscriber() == Transcriber.VALIDATOR;
    }

    public SessionLocation getCurrentSessionLocation() {
        return currentSessionLocation;
    }

    public void setCurrentSessionLocation(SessionLocation currentSessionLocation) {
        this.currentSessionLocation = currentSessionLocation;
    }

    // endregion Getters and Setters


    // region Input Actions

    public void nextTierOrElement() {
        int caretPos = getCaretPosition();

        int newCaretPos = getStartOfNextTierOrElement(caretPos);
        if (newCaretPos == -1) return;

        setCaretPosition(newCaretPos);
    }

    public void prevTierOrElement() {
        int caretPos = getCaretPosition();

        int newCaretPos = getStartOfPrevTierOrElement(caretPos);
        if (newCaretPos == -1) return;

        setCaretPosition(newCaretPos);
    }

    public void pressedEnter(PhonActionEvent<Void> pae) {
        TranscriptDocument doc = getTranscriptDocument();
        AttributeSet attrs = doc.getCharacterElement(getCaretPosition()).getAttributes();
        var enterAct = attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_ENTER_ACTION);
        if (enterAct instanceof Action action) {
            action.actionPerformed(pae.getActionEvent());
            return;
        }

        String elemType = (String) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);
        System.out.println("Element type: " + elemType);

        if (elemType != null) {
            try {
                switch (elemType) {
                    case TranscriptStyleConstants.ATTR_KEY_RECORD -> {
                        Tier<?> tier = (Tier<?>) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);
                        if (tier == null) return;
                        int start = doc.getTierStart(tier);
                        int end = doc.getTierEnd(tier) - 1;
                        String newVal = doc.getText(start, end - start);
                        System.out.println("Old Val: " + tier.toString());
                        System.out.println("New Val: " + newVal);
                        System.out.println("Equal: " + tier.toString().equals(newVal));
                        if (!tier.toString().equals(newVal)) {
                            tierDataChanged((Record)attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_RECORD), tier, newVal);
                        }
                    }
                    case TranscriptStyleConstants.ATTR_KEY_COMMENT -> {
                        Comment comment = (Comment) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_COMMENT);
                        if (comment == null) return;
                        int start = doc.getCommentStart(comment);
                        int end = doc.getCommentEnd(comment) - 1;
                        commentDataChanged(comment, doc.getText(start, end - start));
                    }
                    case TranscriptStyleConstants.ATTR_KEY_GEM -> {
                        Gem gem = (Gem) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GEM);
                        if (gem == null) return;
                        int start = doc.getGemStart(gem);
                        int end = doc.getGemEnd(gem) - 1;
                        gemDataChanged(gem, doc.getText(start, end - start));
                    }
                    case TranscriptStyleConstants.ATTR_KEY_GENERIC -> {
                        Tier<?> genericTier = (Tier<?>) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GENERIC);
                        if (genericTier == null) return;
                        int start = doc.getGenericStart(genericTier);
                        int end = doc.getGenericEnd(genericTier) - 1;
                        genericDataChanged(genericTier, doc.getText(start, end - start));
                    }
                }
            }
            catch (BadLocationException e) {
                LogUtil.severe(e);
            }

            if (elemType.equals(TranscriptStyleConstants.ATTR_KEY_RECORD) || elemType.equals(TranscriptStyleConstants.ATTR_KEY_COMMENT) || elemType.equals(TranscriptStyleConstants.ATTR_KEY_GEM)) {
                int elementIndex;
                if (elemType.equals(TranscriptStyleConstants.ATTR_KEY_RECORD)) {
                    elementIndex = getSession().getRecordElementIndex((Record) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_RECORD));
                }
                else if (elemType.equals(TranscriptStyleConstants.ATTR_KEY_COMMENT)) {
                    Comment comment = (Comment) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_COMMENT);
                    elementIndex = getSession().getTranscript().getElementIndex(comment);
                }
                else {
                    Gem gem = (Gem) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GEM);
                    elementIndex = getSession().getTranscript().getElementIndex(gem);
                }
                if (elementIndex > -1) {
                    try {
                        Rectangle2D caretRect = modelToView2D(getCaretPosition());
                        Point point = new Point((int) caretRect.getCenterX(), (int) caretRect.getMaxY());
                        showContextMenu(elementIndex, point);
                    }
                    catch (BadLocationException e) {
                        LogUtil.severe(e);
                    }
                }
            }
        }
    }

    public void pressedHome() {
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
                    start = doc.getTierStart(tier);
                }
            }
            case TranscriptStyleConstants.ATTR_KEY_COMMENT -> {
                Comment comment = (Comment) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_COMMENT);
                if (comment != null) {
                    start = doc.getCommentStart(comment);
                }
            }
            case TranscriptStyleConstants.ATTR_KEY_GEM -> {
                Gem gem = (Gem) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GEM);
                if (gem != null) {
                    start = doc.getGemStart(gem);
                }
            }
            case TranscriptStyleConstants.ATTR_KEY_GENERIC -> {
                Tier<?> genericTier = (Tier<?>) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GENERIC);
                if (genericTier != null) {
                    start = doc.getGenericStart(genericTier);
                }
            }
        }
        if (start != -1) {
            setCaretPosition(start);
        }
    }

    public void pressedEnd() {
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
            case TranscriptStyleConstants.ATTR_KEY_GENERIC -> {
                Tier<?> genericTier = (Tier<?>) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GENERIC);
                if (genericTier != null) {
                    end = doc.getGenericEnd(genericTier);
                }
            }
        }
        if (end != -1) {
            setCaretPosition(end-1);
        }
    }

    // endregion Input Actions


    // region Tier View Changes

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
            default -> {}
        }
    }

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
        Tier caretTier = (Tier)elem.getAttributes().getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);
        int caretTierOffset = -1;

        if (caretTier != null) {
            String caretTierName = caretTier.getName();
            boolean caretTierHasMoved = movedTiers
                .stream()
                .anyMatch(item -> item.getTierName().equals(caretTierName));
            if (caretTierHasMoved) {
                System.out.println("Move the caret");
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
            setCaretPosition(doc.getTierStart(caretTier) + caretTierOffset);
        }
    }

    public void deleteTier(EditorEventType.TierViewChangedData data) {
        TranscriptDocument doc = getTranscriptDocument();

        List<String> deletedTiersNames = data.tierNames();

        int startCaretPos = getCaretPosition();
        var elem = doc.getCharacterElement(startCaretPos);
        var caretAttrs = elem.getAttributes();
        Tier caretTier = (Tier) caretAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);

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
                    }
                    else if (!deletedTiersNames.contains(item.getTierName()) && item.isVisible()) {
                        newCaretTierViewItem = item;
                        if (passedCaretTier) break;
                    }
                }

                int newCaretTierStart = doc.getTierStart(caretRecordIndex, newCaretTierViewItem.getTierName());
                int newCaretTierEnd = doc.getTierEnd(caretRecordIndex, newCaretTierViewItem.getTierName());

                int newCaretPos = Math.min(newCaretTierStart + caretOffset, newCaretTierEnd - 1);
                setCaretPosition(newCaretPos);
            }
            // Caret in tier not deleted
            else {
                setCaretPosition(doc.getTierStart(caretTier) + caretOffset);
            }
        }
        // Caret not in record / tier
        else {
            String elementType = (String) caretAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);
            int start = -1;
            switch (elementType) {
                case TranscriptStyleConstants.ATTR_KEY_COMMENT -> start = doc.getCommentStart((Comment) caretAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_COMMENT));
                case TranscriptStyleConstants.ATTR_KEY_GEM -> start = doc.getGemStart((Gem) caretAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GEM));
                case TranscriptStyleConstants.ATTR_KEY_GENERIC -> start = doc.getGenericStart((Tier<?>) caretAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GENERIC));
            }

            setCaretPosition(start + caretOffset);
        }
    }

    public void addTier(EditorEventType.TierViewChangedData data) {
        var doc = getTranscriptDocument();

        int startCaretPos = getCaretPosition();
        var elem = doc.getCharacterElement(startCaretPos);
        Tier caretTier = (Tier)elem.getAttributes().getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);
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
            setCaretPosition(doc.getTierStart(caretTier) + caretTierOffset);
        }
        else {
            // Put the caret back where it was before the move
            setCaretPosition(startCaretPos);
        }
    }

    public void hideTier(EditorEventType.TierViewChangedData data) {
        TranscriptDocument doc = getTranscriptDocument();

        List<String> hiddenTiersNames = data.tierNames();

        int startCaretPos = getCaretPosition();
        var elem = doc.getCharacterElement(startCaretPos);
        var caretAttrs = elem.getAttributes();
        Tier caretTier = (Tier) caretAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);

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
                    }
                    else if (!hiddenTiersNames.contains(item.getTierName()) && item.isVisible()) {
                        newCaretTierViewItem = item;
                        if (passedCaretTier) break;
                    }
                }

                int newCaretTierStart = doc.getTierStart(caretRecordIndex, newCaretTierViewItem.getTierName());
                int newCaretTierEnd = doc.getTierEnd(caretRecordIndex, newCaretTierViewItem.getTierName());

                int newCaretPos = Math.min(newCaretTierStart + caretOffset, newCaretTierEnd - 1);
                setCaretPosition(newCaretPos);
            }
            // Caret in tier not deleted
            else {
                setCaretPosition(doc.getTierStart(caretTier) + caretOffset);
            }
        }
        // Caret not in record / tier
        else {
            String elementType = (String) caretAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);
            int start = -1;
            switch (elementType) {
                case TranscriptStyleConstants.ATTR_KEY_COMMENT -> start = doc.getCommentStart((Comment) caretAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_COMMENT));
                case TranscriptStyleConstants.ATTR_KEY_GEM -> start = doc.getGemStart((Gem) caretAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GEM));
                case TranscriptStyleConstants.ATTR_KEY_GENERIC -> start = doc.getGenericStart((Tier<?>) caretAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GENERIC));
            }

            setCaretPosition(start + caretOffset);
        }
    }

    public void showTier(EditorEventType.TierViewChangedData data) {
        var doc = getTranscriptDocument();

        int startCaretPos = getCaretPosition();
        var elem = doc.getCharacterElement(startCaretPos);
        Tier caretTier = (Tier)elem.getAttributes().getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);
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
            setCaretPosition(doc.getTierStart(caretTier) + caretTierOffset);
        }
        else {
            // Put the caret back where it was before the move
            setCaretPosition(startCaretPos);
        }
    }

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

    public void tierFontChanged(EditorEventType.TierViewChangedData data) {
        TranscriptDocument doc = getTranscriptDocument();
        int caretPos = getCaretPosition();

        List<TierViewItem> changedTiers = data
            .newTierView()
            .stream()
            .filter(item -> data.tierNames().contains(item.getTierName()))
            .toList();

        if (changedTiers.isEmpty()) return;

        Document blank = getEditorKit().createDefaultDocument();
        setDocument(blank);
        // Change tier font in doc
        doc.reload();
        setDocument(doc);

        setCaretPosition(caretPos);
    }

    // endregion Tier View Changes


    // region Record Changes

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
            int recordStartPos = doc.getRecordStart(editorEvent.data().recordIndex(), true);
            int recordEndPos = doc.getRecordEnd(editorEvent.data().recordIndex());
            var startRect = modelToView2D(recordStartPos);
            var endRect = modelToView2D(recordEndPos);

            // Create a rect that contains the whole record
            Rectangle scrollToRect = new Rectangle(
                (int) startRect.getMinX(),
                (int) startRect.getMinY(),
                (int) (endRect.getMaxX() - startRect.getMinX()),
                (int) (endRect.getMaxY() - startRect.getMinY())
            );
            // Scroll to a point where that new rect is visible
            super.scrollRectToVisible(scrollToRect);
        }
        catch (BadLocationException e) {
            LogUtil.severe(e);
        }
    }

    private void onRecordAdded(EditorEvent<EditorEventType.RecordAddedData> editorEvent) {
        var data = editorEvent.data();
        // Get the new record
        Record addedRecord = getSession().getRecord(data.recordIndex());
        // Add it to the doc
        getTranscriptDocument().addRecord(addedRecord);
    }

    private void onRecordDeleted(EditorEvent<EditorEventType.RecordDeletedData> editorEvent) {
        TranscriptDocument doc = getTranscriptDocument();

        int deletedRecordIndex = editorEvent.data().recordIndex();

        int startCaretPos = getCaretPosition();
        var elem = doc.getCharacterElement(startCaretPos);
        var caretAttrs = elem.getAttributes();
        Tier caretTier = (Tier) caretAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);
        Record caretRecord = (Record) caretAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_RECORD);

        boolean caretInDeletedRecord = caretRecord != null && caretRecord == editorEvent.data().record();

        int caretOffset = doc.getOffsetInContent(startCaretPos);


        // Delete the record from the doc
        var data = editorEvent.data();
        getTranscriptDocument().deleteRecord(data.record());


        // Caret in record / tier
        if (caretTier != null) {
            // Caret in deleted record
            if (caretInDeletedRecord) {
                boolean deletedRecordWasLast = deletedRecordIndex == getSession().getRecordCount();

                int newCaretRecordIndex = deletedRecordWasLast ? deletedRecordIndex - 1 : deletedRecordIndex;

                int newCaretTierStart = doc.getTierStart(newCaretRecordIndex, caretTier.getName());
                int newCaretTierEnd = doc.getTierEnd(newCaretRecordIndex, caretTier.getName());

                int newCaretPos = Math.min(newCaretTierStart + caretOffset, newCaretTierEnd - 1);
                setCaretPosition(newCaretPos);
            }
            // Caret in record not deleted
            else {
                setCaretPosition(doc.getTierStart(caretTier) + caretOffset);
            }
        }
        // Caret not in record / tier
        else {
            String elementType = (String) caretAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);
            int start = -1;
            switch (elementType) {
                case TranscriptStyleConstants.ATTR_KEY_COMMENT -> start = doc.getCommentStart((Comment) caretAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_COMMENT));
                case TranscriptStyleConstants.ATTR_KEY_GEM -> start = doc.getGemStart((Gem) caretAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GEM));
                case TranscriptStyleConstants.ATTR_KEY_GENERIC -> start = doc.getGenericStart((Tier<?>) caretAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GENERIC));
            }

            setCaretPosition(start + caretOffset);
        }
    }

    private void onRecordMoved(EditorEvent<EditorEventType.RecordMovedData> editorEvent) {
        // Record caret pos
        int caretPos = getCaretPosition();

        // Move the records in the doc
        var data = editorEvent.data();
        getTranscriptDocument().moveRecord(
            data.fromRecordIndex(),
            data.toRecordIndex(),
            data.fromElementIndex(),
            data.toElementIndex()
        );

        // Set the caret to the editable pos closest to the original pos
        setCaretPosition(getNextValidIndex(caretPos, false));
    }

    private void onSpeakerChanged(EditorEvent<EditorEventType.SpeakerChangedData> editorEvent) {
        var data = editorEvent.data();
        // Update the speaker on the separator in the doc
        getTranscriptDocument().changeSpeaker(data.record());
    }

    private void onTierDataChanged(EditorEvent<EditorEventType.TierChangeData> editorEvent) {
        if(editorEvent.data().valueAdjusting()) return;

        TranscriptDocument doc = getTranscriptDocument();
        Tier<?> changedTier = editorEvent.data().tier();

        System.out.println("Tier data changed: " + changedTier.getName());

        if (errorUnderlineHighlights.containsKey(changedTier)) {
            getHighlighter().removeHighlight(errorUnderlineHighlights.get(changedTier));
            errorUnderlineHighlights.remove(changedTier);
        }

        if (changedTier.isUnvalidated()) {
            try {
                int start = doc.getTierStart(changedTier) + changedTier.getUnvalidatedValue().getParseError().getErrorOffset();
                int end = doc.getTierEnd(changedTier)-1;

                var errorUnderlineHighlight = getHighlighter().addHighlight(
                    start,
                    end,
                    new ErrorUnderlinePainter()
                );
                errorUnderlineHighlights.put(changedTier, errorUnderlineHighlight);
            }
            catch (BadLocationException e) {
                LogUtil.severe(e);
            }
        }


        if (internalEdit) {
            internalEdit = false;
            return;
        }

        System.out.println("Tier data changed");


        var caretStartAttrs = doc.getCharacterElement(getCaretPosition()).getAttributes();
        Tier<?> caretStartTier = (Tier<?>) caretStartAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);
        int offset = doc.getOffsetInContent(getCaretPosition());

        // Update the changed tier data in the doc
        getTranscriptDocument().onTierDataChanged(changedTier);

        if (caretStartTier == null) return;

        setCaretPosition(doc.getTierStart(caretStartTier) + offset);
    }

    // endregion Record Changes


    // region On Click

    private void onClickTierLabel(Point2D point, Tier<?> tier, Record record) {
        // Build a new popup menu
        JPopupMenu menu = new JPopupMenu();
        MenuBuilder builder = new MenuBuilder(menu);

        var extPts = PluginManager.getInstance().getExtensionPoints(TierLabelMenuHandler.class);

        for (var extPt : extPts) {
            var menuHandler = extPt.getFactory().createObject();
            menuHandler.addMenuItems(builder, getSession(), eventManager, undoSupport, tier, record);
        }

        // Show it where the user clicked
        menu.show(this, (int) point.getX(), (int) point.getY());
    }

    private void onClickCommentLabel(Point2D point, Comment comment) {
        // Build a new popup menu
        JPopupMenu menu = new JPopupMenu();


        JMenu changeTypeMenu = new JMenu("Change type");

        ButtonGroup changeTypeButtonGroup = new ButtonGroup();
        for (CommentType type : CommentType.values()) {
            JRadioButtonMenuItem changeTypeItem = new JRadioButtonMenuItem();
            changeTypeButtonGroup.add(changeTypeItem);
            if (comment.getType().equals(type)) {
                changeTypeButtonGroup.setSelected(changeTypeItem.getModel(), true);
            }
            PhonUIAction<Void> changeTypeAct = PhonUIAction.runnable(() -> {
                ChangeCommentTypeEdit edit = new ChangeCommentTypeEdit(getSession(), eventManager, comment, type);
                undoSupport.postEdit(edit);
            });
            changeTypeAct.putValue(PhonUIAction.NAME, type.getLabel());
            changeTypeItem.setAction(changeTypeAct);
            changeTypeMenu.add(changeTypeItem);
        }

        menu.add(changeTypeMenu);


        JMenuItem deleteThis = new JMenuItem();
        PhonUIAction<Void> deleteThisAct = PhonUIAction.runnable(() -> deleteTranscriptElement(new Transcript.Element(comment)));
        deleteThisAct.putValue(PhonUIAction.NAME, "Delete this comment");
        deleteThis.setAction(deleteThisAct);
        menu.add(deleteThis);

        // Show it where the user clicked
        menu.show(this, (int) point.getX(), (int) point.getY());
    }

    private void onClickGemLabel(Point2D point, Gem gem) {
        // Build a new popup menu
        JPopupMenu menu = new JPopupMenu();


        JMenu changeTypeMenu = new JMenu("Change type");

        ButtonGroup changeTypeButtonGroup = new ButtonGroup();
        for (GemType type : GemType.values()) {
            JRadioButtonMenuItem changeTypeItem = new JRadioButtonMenuItem();
            changeTypeButtonGroup.add(changeTypeItem);
            if (gem.getType().equals(type)) {
                changeTypeButtonGroup.setSelected(changeTypeItem.getModel(), true);
            }
            PhonUIAction<Void> changeTypeAct = PhonUIAction.runnable(() -> {
                ChangeGemTypeEdit edit = new ChangeGemTypeEdit(getSession(), eventManager, gem, type);
                undoSupport.postEdit(edit);
            });
            changeTypeAct.putValue(PhonUIAction.NAME, type.name());
            changeTypeItem.setAction(changeTypeAct);
            changeTypeMenu.add(changeTypeItem);
        }

        menu.add(changeTypeMenu);


        JMenuItem deleteThis = new JMenuItem();
        PhonUIAction<Void> deleteThisAct = PhonUIAction.runnable(() -> deleteTranscriptElement(new Transcript.Element(gem)));
        deleteThisAct.putValue(PhonUIAction.NAME, "Delete this gem");
        deleteThis.setAction(deleteThisAct);
        menu.add(deleteThis);


        // Show it where the user clicked
        menu.show(this, (int) point.getX(), (int) point.getY());
    }

    private void onClickBlindTranscriptionLabel(Point2D point, Record record, Tier<?> tier, String transcriber) {
        System.out.println("Show blind transcription label menu");

        JPopupMenu menu = new JPopupMenu();

        JMenuItem select = new JMenuItem();
        PhonUIAction selectAction = PhonUIAction.runnable(() -> {
            selectTranscription(record, tier, transcriber);
        });
        selectAction.putValue(PhonUIAction.NAME, "Select transcription");
        select.setAction(selectAction);
        menu.add(select);

        JMenuItem append = new JMenuItem();
        PhonUIAction appendAction = PhonUIAction.runnable(() -> {
           System.out.println("Append");
        });
        appendAction.putValue(PhonUIAction.NAME, "Append");
        append.setAction(appendAction);
        menu.add(append);

        menu.show(this, (int) point.getX(), (int) point.getY());
    }

    // endregion On Click

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

        JMenuItem deleteThis = new JMenuItem();
        PhonUIAction<Void> deleteThisAct = PhonUIAction.runnable(() -> deleteTranscriptElement(getSession().getTranscript().getElementAt(transcriptElementIndex)));
        deleteThisAct.putValue(PhonUIAction.NAME, "Delete this element");
        deleteThis.setAction(deleteThisAct);
        menu.add(deleteThis);

        menu.show(this, (int) pos.getX(), (int) pos.getY());
    }

    @Override
    protected void paintComponent(Graphics g) {
        TranscriptDocument doc = getTranscriptDocument();
        // Get the clip bounds of the current view
        Rectangle drawHere = g.getClipBounds();

        // Fill the background with the appropriate color
        g.setColor(UIManager.getColor(TranscriptEditorUIProps.BACKGROUND));
        g.fillRect(0, drawHere.y, drawHere.width, drawHere.height);

        // Fill the label column background with the appropriate color
        g.setColor(UIManager.getColor(TranscriptEditorUIProps.LABEL_BACKGROUND));
        FontMetrics fontMetrics = g.getFontMetrics(FontPreferences.getMonospaceFont().deriveFont(14.0f));
        char[] template = new char[getTranscriptDocument().getLabelColumnWidth() + 1];
        Arrays.fill(template, ' ');
        int labelColWidth = fontMetrics.stringWidth(new String(template));
        Rectangle labelColRect = new Rectangle(0, 0, labelColWidth, getHeight());
        if (labelColRect.intersects(drawHere)) {
            g.fillRect(0, (int) drawHere.getMinY(), labelColWidth, drawHere.height);
        }

        Element root = doc.getDefaultRootElement();
        if (root.getElementCount() == 0) return;

        for (int i = 0; i < root.getElementCount(); i++) {
            Element elem = root.getElement(i);
            if (elem.getElementCount() == 0) continue;
            Element innerElem = elem.getElement(0);
            AttributeSet attrs = innerElem.getAttributes();
            String elementType = (String) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);
            if (elementType != null) {
                int start = -1;
                int end = -1;

                switch (elementType) {
                    case TranscriptStyleConstants.ATTR_KEY_COMMENT -> {
                        g.setColor(UIManager.getColor(TranscriptEditorUIProps.COMMENT_BACKGROUND));
                        Comment comment = (Comment) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_COMMENT);
                        start = doc.getCommentStart(comment);
                        end = doc.getCommentEnd(comment);
                    }
                    case TranscriptStyleConstants.ATTR_KEY_GEM -> {
                        g.setColor(UIManager.getColor(TranscriptEditorUIProps.GEM_BACKGROUND));
                        Gem gem = (Gem) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GEM);
                        start = doc.getGemStart(gem);
                        end = doc.getGemEnd(gem);
                    }
                    case TranscriptStyleConstants.ATTR_KEY_GENERIC -> {
                        g.setColor(UIManager.getColor(TranscriptEditorUIProps.GENERIC_BACKGROUND));
                        Tier<?> genericTier = (Tier<?>) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GENERIC);
                        start = doc.getGenericStart(genericTier);
                        end = doc.getGenericEnd(genericTier);
                    }
                }
                if (start == -1) continue;
                try {
                    var startRect = modelToView2D(start - 1);
                    var endRect = modelToView2D(end-1);
                    if (startRect == null || endRect == null) continue;
                    var colorRect = new Rectangle(
                        (int) startRect.getMinX(),
                        (int) startRect.getMinY(),
                        (int) (getWidth() - startRect.getMinX()),
                        (int) (endRect.getMaxY() - startRect.getMinY())
                    );
                    if (!drawHere.intersects(colorRect)) continue;
                    g.fillRect(
                        (int) colorRect.getMinX(),
                        (int) colorRect.getMinY(),
                        (int) colorRect.getWidth(),
                        (int) colorRect.getHeight()
                    );
                }
                catch (BadLocationException e) {
                    LogUtil.severe(e);
                }
            }
        }


        g.setColor(UIManager.getColor(TranscriptEditorUIProps.SEPARATOR_LINE));
        int sepLineHeight = 1;
        int fontHeight = fontMetrics.getHeight();

        float lineSpacing = StyleConstants.getLineSpacing(root.getElement(0).getAttributes());
        int sepLineOffset = (int) (((fontHeight * lineSpacing) + sepLineHeight) / 2);
        // For every element
        for (int i = 0; i < root.getElementCount(); i++) {
            Element elem = root.getElement(i);
            if (elem.getElementCount() == 0) continue;
            Element innerElem = elem.getElement(0);
            AttributeSet attrs = innerElem.getAttributes();
            // If it's a separator
            if (attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_SEPARATOR) != null) {
                try {
                    var sepRect = modelToView2D(innerElem.getStartOffset());
                    if (sepRect == null) continue;
                    boolean topVisible = sepRect.getMinY() >= drawHere.getMinY() && sepRect.getMinY() <= drawHere.getMaxY();
                    boolean bottomVisible = sepRect.getMaxY() >= drawHere.getMinY() && sepRect.getMaxY() <= drawHere.getMaxY();
                    // And it's onscreen
                    if (!topVisible && !bottomVisible) continue;
                    // Draw the separator line
                    g.fillRect(
                        drawHere.x,
                        ((int) sepRect.getMinY()) - sepLineOffset,
                        drawHere.width,
                        sepLineHeight
                    );
                } catch (BadLocationException e) {
                    LogUtil.severe(e);
                }
            }
        }

        super.paintComponent(g);
    }

    public void removeEditorActions() {
        this.eventManager.removeActionForEvent(EditorEventType.SessionChanged, this::onSessionChanged);
        this.eventManager.removeActionForEvent(EditorEventType.TierViewChanged, this::onTierViewChanged);
        this.eventManager.removeActionForEvent(EditorEventType.RecordChanged, this::onRecordChanged);
    }

    private void onSessionChanged(EditorEvent<Session> editorEvent) {

    }

    public void changeSpeaker(Tuple<Record, Participant> data) {
        ChangeSpeakerEdit edit = new ChangeSpeakerEdit(getSession(), eventManager, data.getObj1(), data.getObj2());
        undoSupport.postEdit(edit);
    }

    public void tierDataChanged(Record record, Tier<?> tier, String newData) {
        TranscriptDocument doc = getTranscriptDocument();
        String transcriber = dataModel.getTranscriber().getUsername();

        System.out.println("Changed tier: " + tier.getName());

        Tier<?> dummy = SessionFactory.newFactory().createTier("dummy", tier.getDeclaredType());
        dummy.setText(newData);

        if (tier.getDeclaredType() == MediaSegment.class) return;
        if (doc.getTierText(tier, transcriber).equals(doc.getTierText(dummy, transcriber))) return;

        SwingUtilities.invokeLater(() -> {
            TierEdit<?> edit = new TierEdit(getSession(), eventManager, dataModel.getTranscriber(), record, tier, dummy.getValue());
            edit.setValueAdjusting(false);
            getUndoSupport().postEdit(edit);
            System.out.println("Tier data changed event posted");
        });
    }

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
                System.out.println("Language changed -----------------------");
                System.out.println("Validated: " + !languagesTier.isUnvalidated());
                if (languagesTier.hasValue()) {
                    SessionLanguageEdit edit = new SessionLanguageEdit(getSession(), eventManager, languagesTier.getValue().languageList());
                    getUndoSupport().postEdit(edit);
                }
            }

            TierEdit<?> edit = new TierEdit(getSession(), eventManager, null, genericTier, dummy.getValue());
            edit.setValueAdjusting(false);
            getUndoSupport().postEdit(edit);
            getUndoSupport().endUpdate();
        });
    }

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

    public void gemDataChanged(Gem gem, String newData) {

        if (gem.getLabel().equals(newData)) return;

        SwingUtilities.invokeLater(() -> {
            ChangeGemEdit edit = new ChangeGemEdit(getSession(), eventManager, gem, newData);
            getUndoSupport().postEdit(edit);
        });
    }

    private void selectTranscription(Record record, Tier<?> tier, String transcriber) {
        System.out.println("Transcription selected");

        TranscriptDocument doc = getTranscriptDocument();

        Tier<?> dummy = SessionFactory.newFactory().createTier("dummy", tier.getDeclaredType());
        dummy.setText(doc.getTierText(tier, transcriber));

        SwingUtilities.invokeLater(() -> {
            TierEdit<?> edit = new TierEdit(dataModel.getSession(), eventManager, Transcriber.VALIDATOR, record, tier, dummy.getValue());
            edit.setValueAdjusting(false);
            undoSupport.postEdit(edit);
            System.out.println("Tier data changed event posted");
        });
    }

    private void highlightElementAtPoint(Point2D point) {
        int mousePosInDoc = viewToModel2D(point);
        var elem = getTranscriptDocument().getCharacterElement(mousePosInDoc);
        try {
            removeCurrentHighlight();
            currentHighlight = getHighlighter().addHighlight(
                elem.getStartOffset(),
                elem.getEndOffset(),
                highlightPainter
            );
        } catch (BadLocationException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void removeCurrentHighlight() {
        if (currentHighlight != null) {
            getHighlighter().removeHighlight(currentHighlight);
            SwingUtilities.invokeLater(this::repaint);
        }
    }

    public void loadSession() {
        TranscriptDocument doc = getTranscriptDocument();
        doc.setUndoSupport(undoSupport);
        doc.setEventManager(eventManager);



        doc.setSession(getSession());
//        setDocument(doc);
        doc.addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                TranscriptDocument doc = getTranscriptDocument();
                Element elem = doc.getCharacterElement(e.getOffset());
                AttributeSet attrs = elem.getAttributes();
                if (attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_SYLLABIFICATION) != null) {
                    int tierEnd = doc.getTierEnd((Tier<?>) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER))-1;
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

    public int getNextValidIndex(int currentPos, boolean looping) {
        TranscriptDocument doc = getTranscriptDocument();

        int docLen = doc.getLength();

        Element elem = doc.getCharacterElement(currentPos);
        while (elem.getAttributes().getAttribute(TranscriptStyleConstants.ATTR_KEY_NOT_TRAVERSABLE) != null && currentPos < docLen) {
            currentPos++;
            elem = doc.getCharacterElement(currentPos);
        }

        if (currentPos == docLen) {
            if (looping) {
                return getNextValidIndex(0, true);
            }
            else {
                return -1;
            }
        }

        return currentPos;
    }

    public int getPrevValidIndex(int currentPos, boolean looping) {
        TranscriptDocument doc = getTranscriptDocument();

        Element elem = doc.getCharacterElement(currentPos);
        AttributeSet attrs = elem.getAttributes();

        if (attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER) != null) {
            System.out.println("test 1");
            Tier<?> tier = (Tier<?>) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);
            Set<String> syllabificationTierNames = new HashSet<>();
            syllabificationTierNames.add(SystemTierType.TargetSyllables.getName());
            syllabificationTierNames.add(SystemTierType.ActualSyllables.getName());
            if (syllabificationTierNames.contains(tier.getName()) && getSelectionStart() != getSelectionEnd()) {
                System.out.println("Test 2");
                currentPos--;
                elem = doc.getCharacterElement(currentPos);
                attrs = elem.getAttributes();
            }
        }

        while ((attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_NOT_TRAVERSABLE) != null) && currentPos >= 0) {
            currentPos--;
            elem = doc.getCharacterElement(currentPos);
            attrs = elem.getAttributes();
        }

        if (looping && currentPos < 0) {
            return getPrevValidIndex(doc.getLength()-1, true);
        }

        System.out.println("\n");

        return currentPos;
    }

    public void sameOffsetInPrevTierOrElement() {
        TranscriptDocument doc = getTranscriptDocument();

        int caretPos = getCaretPosition();
        int offsetInContent;
        if (upDownOffset == -1) {
            offsetInContent = doc.getOffsetInContent(caretPos);
            upDownOffset = offsetInContent;
        }
        else {
            offsetInContent = upDownOffset;
        }

        int start = getStartOfPrevTierOrElement(caretPos);

        System.out.println("Start of prev: " + start);

        if (start == -1) return;

        int end;

        AttributeSet prevElementAttributes = doc.getCharacterElement(start).getAttributes();

        String elementType = (String) prevElementAttributes.getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);

        System.out.println("Prev element type: " + elementType);

        if (elementType == null) {
            return;
        }
        else if (elementType.equals(TranscriptStyleConstants.ATTR_KEY_RECORD)) {
            end = doc.getTierEnd((Tier<?>) prevElementAttributes.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER));
        }
        else if (elementType.equals(TranscriptStyleConstants.ATTR_KEY_COMMENT)) {
            end = doc.getCommentEnd((Comment) prevElementAttributes.getAttribute(TranscriptStyleConstants.ATTR_KEY_COMMENT));
        }
        else if (elementType.equals(TranscriptStyleConstants.ATTR_KEY_GEM)) {
            Gem gem = (Gem) prevElementAttributes.getAttribute(TranscriptStyleConstants.ATTR_KEY_GEM);
            System.out.println("Gem: " + gem.getType() + " " + gem.getLabel());
            end = doc.getGemEnd(gem);
            System.out.println("Gem end: " + end);
        }
        else if (elementType.equals(TranscriptStyleConstants.ATTR_KEY_GENERIC)) {
            Tier<?> genericTier = (Tier<?>) prevElementAttributes.getAttribute(TranscriptStyleConstants.ATTR_KEY_GENERIC);
            System.out.println("tier data: " + genericTier.toString());
            end = doc.getGenericEnd(genericTier);
            System.out.println("End: " + end);
        }
        else {
            return;
        }

        int newCaretPos = Math.min(end - 1, start + offsetInContent);

        System.out.println("New caret pos: " + newCaretPos);

        caretMoveFromUpDown = true;
        setCaretPosition(newCaretPos);
    }

    public void sameOffsetInNextTierOrElement() {
        TranscriptDocument doc = getTranscriptDocument();

        int caretPos = getCaretPosition();
        int offsetInContent;
        if (upDownOffset == -1) {
            offsetInContent = doc.getOffsetInContent(caretPos);
            upDownOffset = offsetInContent;
        }
        else {
            offsetInContent = upDownOffset;
        }

        int start = getStartOfNextTierOrElement(caretPos);

        System.out.println(start);

        if (start == -1) return;

        int end;

        AttributeSet nextElementAttributes = doc.getCharacterElement(start).getAttributes();

        String elementType = (String) nextElementAttributes.getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);
        System.out.println("Next element type: " + elementType);

        if (elementType == null) {
            return;
        }
        else if (elementType.equals(TranscriptStyleConstants.ATTR_KEY_RECORD)) {
            end = doc.getTierEnd((Tier<?>) nextElementAttributes.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER));
        }
        else if (elementType.equals(TranscriptStyleConstants.ATTR_KEY_COMMENT)) {
            end = doc.getCommentEnd((Comment) nextElementAttributes.getAttribute(TranscriptStyleConstants.ATTR_KEY_COMMENT));
        }
        else if (elementType.equals(TranscriptStyleConstants.ATTR_KEY_GEM)) {
            end = doc.getGemEnd((Gem) nextElementAttributes.getAttribute(TranscriptStyleConstants.ATTR_KEY_GEM));
        }
        else if (elementType.equals(TranscriptStyleConstants.ATTR_KEY_GENERIC)) {
            end = doc.getGenericEnd((Tier<?>) nextElementAttributes.getAttribute(TranscriptStyleConstants.ATTR_KEY_GENERIC));
        }
        else {
            return;
        }

        System.out.println("End: " + end);

        int newCaretPos = Math.min(end - 1, start + offsetInContent);

        System.out.println("New caret pos: " + newCaretPos);

        caretMoveFromUpDown = true;
        setCaretPosition(newCaretPos);
    }

    public int getStartOfPrevTierOrElement(int caretPos) {
        TranscriptDocument doc = getTranscriptDocument();
        Element elem = doc.getCharacterElement(caretPos);
        AttributeSet currentPosAttrs = elem.getAttributes();

        String elementType = (String) currentPosAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);
        Object content;
        if (elementType.equals(TranscriptStyleConstants.ATTR_KEY_RECORD)) {
            content = currentPosAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);
        }
        else {
            content = currentPosAttrs.getAttribute(elementType);
        }

        int currentDocElemIndex = doc.getDefaultRootElement().getElementIndex(caretPos);

        System.out.println(elementType);
        System.out.println(currentDocElemIndex);

        Element root = doc.getDefaultRootElement();
        for (int i = currentDocElemIndex; i >= 0; i--) {
            Element docElem = root.getElement(i);
            if (docElem.getElementCount() == 0) continue;
            for (int j = 0; j < docElem.getElementCount(); j++) {
                Element innerDocElem = docElem.getElement(j);
                AttributeSet attrs = innerDocElem.getAttributes();
                Boolean isLabel = (Boolean) attrs.getAttribute("label");
                String innerDocElemType = (String) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);
                System.out.println(innerDocElemType);
                if (isLabel == null && innerDocElemType != null) {
                    if (!innerDocElemType.equals(elementType)) {
                        System.out.println(innerDocElem.getStartOffset());
                        return innerDocElem.getStartOffset();
                    }
                    if (innerDocElemType.equals(TranscriptStyleConstants.ATTR_KEY_RECORD)) {
                        if (attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER) != content) {
                            return innerDocElem.getStartOffset();
                        }
                    }
                    else {
                        if (attrs.getAttribute(innerDocElemType) != content) {
                            System.out.println(attrs.getAttribute(innerDocElemType));
                            return innerDocElem.getStartOffset();
                        }
                    }
                }
            }
        }

        return -1;
    }

    public int getStartOfNextTierOrElement(int caretPos) {
        TranscriptDocument doc = getTranscriptDocument();
        Element elem = doc.getCharacterElement(caretPos);
        AttributeSet currentPosAttrs = elem.getAttributes();

        String elementType = (String) currentPosAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);
        Object content;
        if (elementType.equals(TranscriptStyleConstants.ATTR_KEY_RECORD)) {
            content = currentPosAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);
        }
        else {
            content = currentPosAttrs.getAttribute(elementType);
        }

        int currentDocElemIndex = doc.getDefaultRootElement().getElementIndex(caretPos);

        System.out.println(elementType);
        System.out.println(currentDocElemIndex);

        Element root = doc.getDefaultRootElement();
        for (int i = currentDocElemIndex; i < root.getElementCount(); i++) {
            Element docElem = root.getElement(i);
            if (docElem.getElementCount() == 0) continue;
            for (int j = 0; j < docElem.getElementCount(); j++) {
                Element innerDocElem = docElem.getElement(j);
                AttributeSet attrs = innerDocElem.getAttributes();
                Boolean isLabel = (Boolean) attrs.getAttribute("label");
                String innerDocElemType = (String) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);
                System.out.println(innerDocElemType);
                if (isLabel == null && innerDocElemType != null) {
                    if (!innerDocElemType.equals(elementType)) {
                        System.out.println(innerDocElem.getStartOffset());
                        return innerDocElem.getStartOffset();
                    }
                    if (innerDocElemType.equals(TranscriptStyleConstants.ATTR_KEY_RECORD)) {
                        if (attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER) != content) {
                            return innerDocElem.getStartOffset();
                        }
                    }
                    else {
                        if (attrs.getAttribute(innerDocElemType) != content) {
                            System.out.println(attrs.getAttribute(innerDocElemType));
                            return innerDocElem.getStartOffset();
                        }
                    }
                }
            }
        }

        return -1;
    }

    public SessionLocation charPosToSessionLocation(int charPos) {
        TranscriptDocument doc = getTranscriptDocument();
        Transcript transcript = getSession().getTranscript();

        Element charElem = doc.getCharacterElement(charPos);
        AttributeSet attrs = charElem.getAttributes();
        String elementType = (String) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);

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
                    posInTier = charPos - doc.getTierStart(tier);
                }
            }
            case TranscriptStyleConstants.ATTR_KEY_COMMENT -> {
                Comment comment = (Comment) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_COMMENT);
                transcriptElementIndex = transcript.getElementIndex(comment);
                label = comment.getType().getLabel();
                posInTier = charPos - doc.getCommentStart(comment);
            }
            case TranscriptStyleConstants.ATTR_KEY_GEM -> {
                Gem gem = (Gem) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GEM);
                transcriptElementIndex = transcript.getElementIndex(gem);
                label = gem.getType().name() + " Gem";
                posInTier = doc.getGemStart(gem);
            }
            case TranscriptStyleConstants.ATTR_KEY_GENERIC -> {
                Tier<?> genericTier = (Tier<?>) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GENERIC);
                if (genericTier != null) {
                    label = genericTier.getName();
                    posInTier = doc.getGenericStart(genericTier);
                }
            }
        }

        return new SessionLocation(transcriptElementIndex, label, posInTier);
    }

    public int sessionLocationToCharPos(SessionLocation sessionLocation) {
        TranscriptDocument doc = getTranscriptDocument();
        Transcript transcript = getSession().getTranscript();

        if (sessionLocation.getElementIndex() > -1) {
            Transcript.Element transcriptElement = transcript.getElementAt(sessionLocation.getElementIndex());

            if (transcriptElement.isRecord()) {
                int recordIndex = transcript.getRecordPosition(transcriptElement.asRecord());
                return doc.getTierStart(recordIndex, sessionLocation.getLabel()) + sessionLocation.getPosInTier();
            }
            else if (transcriptElement.isComment()) {
                return doc.getCommentStart(transcriptElement.asComment()) + sessionLocation.getPosInTier();
            }
            else if (transcriptElement.isGem()) {
                return doc.getGemStart(transcriptElement.asGem()) + sessionLocation.getPosInTier();
            }
        }

        return -1;
    }

    private void underlineElement(Element elem) {
        try {
//            removeCurrentUnderline();
            currentUnderline = getHighlighter().addHighlight(
                elem.getStartOffset(),
                elem.getEndOffset(),
                underlinePainter
            );
        } catch (BadLocationException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void removeCurrentUnderline() {
        if (currentUnderline != null) {
            getHighlighter().removeHighlight(currentUnderline);
            currentUnderline = null;
        }
    }

    public void boxSelectBounds(Tuple<Integer, Integer> bounds) {
        try {
            removeCurrentBoxSelect();
            currentBoxSelect = getHighlighter().addHighlight(
                bounds.getObj1(),
                bounds.getObj2()+1,
                boxSelectPainter
            );
        } catch (BadLocationException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void removeCurrentBoxSelect() {
        if (currentBoxSelect != null) {
            getHighlighter().removeHighlight(currentBoxSelect);
            currentBoxSelect = null;
        }
    }

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

        AddTranscriptElementEdit edit = new AddTranscriptElementEdit(
                getSession(),
            eventManager,
            new Transcript.Element(newComment),
            newCommentIndex
        );
        undoSupport.postEdit(edit);
    }

    private void onCommentAdded(EditorEvent<EditorEventType.CommentAddedData> editorEvent) {
        var data = editorEvent.data();
        getTranscriptDocument().addComment(data.comment(), data.elementIndex());
    }

    private void addGem(int relativeElementIndex, int position) {
        Transcript transcript = getSession().getTranscript();
        Gem newGem = SessionFactory.newFactory().createGem(GemType.Lazy, "");

        int newGemIndex = -1;
        switch (position) {
            case SwingConstants.PREVIOUS -> newGemIndex = relativeElementIndex;
            case SwingConstants.NEXT -> newGemIndex = relativeElementIndex + 1;
            case SwingConstants.BOTTOM -> newGemIndex = transcript.getNumberOfElements();
        }

        AddTranscriptElementEdit edit = new AddTranscriptElementEdit(
                getSession(),
            eventManager,
            new Transcript.Element(newGem),
            newGemIndex
        );
        undoSupport.postEdit(edit);
    }

    private void onGemAdded(EditorEvent<EditorEventType.GemAddedData> editorEvent) {
        var data = editorEvent.data();
        getTranscriptDocument().addGem(data.gem(), data.elementIndex());
    }

    private void deleteTranscriptElement(Transcript.Element elem) {
        DeleteTranscriptElementEdit edit = new DeleteTranscriptElementEdit(
            getSession(),
            eventManager,
            elem,
            getSession().getTranscript().getElementIndex(elem)
        );
        undoSupport.postEdit(edit);
    }

    private void onTranscriptElementDeleted(EditorEvent<EditorEventType.ElementDeletedData> editorEvent) {
        if (!editorEvent.data().element().isRecord()) {
            getTranscriptDocument().deleteTranscriptElement(editorEvent.data().element());
        }
    }

    private void onCommentTypeChanged(EditorEvent<EditorEventType.CommentTypeChangedData> editorEvent) {
        getTranscriptDocument().changeCommentType(editorEvent.data().comment());
    }

    private void onGemTypeChanged(EditorEvent<EditorEventType.GemTypeChangedData> editorEvent) {
        getTranscriptDocument().changeGemType(editorEvent.data().gem());
    }

    private void onSessionLocationChanged(EditorEvent<SessionLocationChangeData> editorEvent) {
        setCurrentSessionLocation(editorEvent.data().newLoc);
    }

    private class TranscriptNavigationFilter extends NavigationFilter {
        @Override
        public void setDot(NavigationFilter.FilterBypass fb, int dot, Position.Bias bias) {

            TranscriptDocument doc = getTranscriptDocument();
            if (doc.getLength() == 0) {
                System.out.println("Empty doc caret movement");
                fb.setDot(dot, bias);
            }

            Element elem = doc.getCharacterElement(dot);
            AttributeSet attrs = elem.getAttributes();
            if (attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_NOT_TRAVERSABLE) != null) return;



            AttributeSet prevAttrs = doc.getCharacterElement(fb.getCaret().getDot()).getAttributes();
            AttributeSet nextAttrs = doc.getCharacterElement(dot).getAttributes();

            String prevElemType = (String) prevAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);
            String nextElemType = (String) nextAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);
            Tier<?> nextTier = (Tier<?>) nextAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);

            if (prevElemType != null) {
                try {
                    switch (prevElemType) {
                        case TranscriptStyleConstants.ATTR_KEY_RECORD -> {
                            Tier<?> prevTier = (Tier<?>) prevAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);
                            if (prevTier == null || prevTier.getDeclaredType().equals(PhoneAlignment.class)) break;
                            if (nextElemType != null && nextElemType.equals(TranscriptStyleConstants.ATTR_KEY_RECORD)) {
                                if (nextTier != null && nextTier == prevTier) break;
                            }
                            int start = doc.getTierStart(prevTier);
                            int end = doc.getTierEnd(prevTier) - 1;
                            String newValue = doc.getText(start, end - start);
                            internalEdit = true;
                            tierDataChanged((Record)prevAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_RECORD), prevTier, newValue);
                        }
                        case TranscriptStyleConstants.ATTR_KEY_COMMENT -> {
                            Comment prevComment = (Comment) prevAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_COMMENT);
                            if (prevComment == null) break;
                            if (nextElemType != null && nextElemType.equals(TranscriptStyleConstants.ATTR_KEY_COMMENT)) {
                                Comment nextComment = (Comment) nextAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_COMMENT);
                                if (nextComment != null && nextComment == prevComment) break;
                            }
                            int start = doc.getCommentStart(prevComment);
                            int end = doc.getCommentEnd(prevComment) - 1;
                            String newValue = doc.getText(start, end - start);
                            commentDataChanged(prevComment, newValue);
                        }
                        case TranscriptStyleConstants.ATTR_KEY_GEM -> {
                            Gem prevGem = (Gem) prevAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GEM);
                            if (prevGem == null) break;
                            if (nextElemType != null && nextElemType.equals(TranscriptStyleConstants.ATTR_KEY_GEM)) {
                                Gem nextGem = (Gem) nextAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GEM);
                                if (nextGem != null && nextGem == prevGem) break;
                            }
                            int start = doc.getGemStart(prevGem);
                            int end = doc.getGemEnd(prevGem) - 1;
                            String newValue = doc.getText(start, end - start);
                            gemDataChanged(prevGem, newValue);
                        }
                        case TranscriptStyleConstants.ATTR_KEY_GENERIC -> {
                            Tier<?> prevGenericTier = (Tier<?>) prevAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GENERIC);
                            if (prevGenericTier == null) break;
                            if (nextElemType != null && nextElemType.equals(TranscriptStyleConstants.ATTR_KEY_GENERIC)) {
                                Tier<?> nextGenericTier = (Tier<?>) nextAttrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GENERIC);
                                if (nextGenericTier != null && nextGenericTier == prevGenericTier) break;
                            }
                            int start = doc.getGenericStart(prevGenericTier);
                            int end = doc.getGenericEnd(prevGenericTier) - 1;
                            String newValue = doc.getText(start, end - start);
                            genericDataChanged(prevGenericTier, newValue);
                        }
                    }
                }
                catch (BadLocationException e) {
                    LogUtil.severe(e);
                }
            }

            if (doc.getLength() == dot) return;

            if (!caretMoveFromUpDown) upDownOffset = -1;
            caretMoveFromUpDown = false;



            if (nextTier != null && nextAttrs.getAttribute("syllabification") != null) {

                int tierEnd = doc.getTierEnd(nextTier)-1;
                fb.setDot(dot, Position.Bias.Forward);
                if (dot != tierEnd) {
                    fb.moveDot(getCaretPosition() + 1, Position.Bias.Forward);
                }
                return;
            }

            int prevCaretPos = getCaretPosition();

            fb.setDot(dot, bias);

            SessionLocationChangeData sessionLocationChangeData = new SessionLocationChangeData(
                charPosToSessionLocation(prevCaretPos),
                charPosToSessionLocation(dot)
            );

            System.out.println(sessionLocationChangeData.old());
            System.out.println(sessionLocationChangeData.newLoc());

            SwingUtilities.invokeLater(() -> {
                final EditorEvent<SessionLocationChangeData> e = new EditorEvent<>(
                        sessionLocationChange,
                        TranscriptEditor.this,
                        sessionLocationChangeData
                );
                eventManager.queueEvent(e);
            });
        }
        @Override
        public void moveDot(NavigationFilter.FilterBypass fb, int dot, Position.Bias bias) {
            TranscriptDocument doc = getTranscriptDocument();

            if (getTranscriptDocument().getLength() == dot) return;

            AttributeSet attrs = doc.getCharacterElement(getCaretPosition()).getAttributes();
            String elementType = (String) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);

            if (elementType != null) {
                int start = -1;
                int end = -1;

                switch (elementType) {
                    case TranscriptStyleConstants.ATTR_KEY_RECORD -> {
                        Tier<?> tier = (Tier<?>) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);
                        if (tier != null) {
                            start = doc.getTierStart(tier);
                            end = doc.getTierEnd(tier);
                        }
                    }
                    case TranscriptStyleConstants.ATTR_KEY_COMMENT -> {
                        Comment comment = (Comment) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_COMMENT);
                        if (comment != null) {
                            start = doc.getCommentStart(comment);
                            end = doc.getCommentEnd(comment);
                        }
                    }
                    case TranscriptStyleConstants.ATTR_KEY_GEM -> {
                        Gem gem = (Gem) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GEM);
                        if (gem != null) {
                            start = doc.getGemStart(gem);
                            end = doc.getGemEnd(gem);
                        }
                    }
                    case TranscriptStyleConstants.ATTR_KEY_GENERIC -> {
                        Tier<?> generic = (Tier<?>) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GENERIC);
                        if (generic != null) {
                            start = doc.getGenericStart(generic);
                            end = doc.getGenericEnd(generic);
                        }
                    }
                }

                if (start != -1 && end != -1) {
                    dot = Math.min(Math.max(dot, start), end-1);
                }
            }



            fb.moveDot(dot, bias);
        }
    }

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
                throw new RuntimeException(e);
            }
        }
    }

    private class ErrorUnderlinePainter implements Highlighter.HighlightPainter {

        @Override
        public void paint(Graphics g, int p0, int p1, Shape bounds, JTextComponent c) {
            try {
                var firstCharRect = modelToView2D(p0);
                var lastCharRect = modelToView2D(p1);
                g.setColor(Color.RED);
                int lineY = ((int) firstCharRect.getMaxY()) - 9;
                g.drawLine((int) firstCharRect.getMinX(), lineY, (int) lastCharRect.getMaxX(), lineY);
            } catch (BadLocationException e) {
                throw new RuntimeException(e);
            }
        }
    }

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
                if(ele != null) {
                    final AttributeSet attrs = ele.getAttributes();
                    if(StyleConstants.getFontFamily(attrs) != null && StyleConstants.getFontSize(attrs) > 0) {
                        int style = (StyleConstants.isBold(attrs) ? Font.BOLD : 0) |
                                (StyleConstants.isItalic(attrs) ? Font.ITALIC : 0);
                        final Font f = new Font(StyleConstants.getFontFamily(attrs), style, StyleConstants.getFontSize(attrs));
                        actualLineHeight = g.getFontMetrics(f).getHeight();
                    }
                }

                g2d.drawRect((int) p0Rect.getMinX(), (int) p0Rect.getMinY(), (int) (p1Rect.getMaxX() - p0Rect.getMinX())-1, actualLineHeight-1);
            } catch (BadLocationException e) {
                LogUtil.severe(e);
            }

        }
    }

    private class TranscriptMouseAdapter extends MouseAdapter {
        @Override
        public void mouseMoved(MouseEvent e) {
            TranscriptDocument doc = getTranscriptDocument();

            if ((e.getModifiersEx() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()) == Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()) {
                highlightElementAtPoint(e.getPoint());
            }
            else if (currentHighlight != null) {
                removeCurrentHighlight();
            }

            int mousePosInDoc = viewToModel2D(e.getPoint());

            Element elem = doc.getCharacterElement(mousePosInDoc);
            if (elem != null) {
//                if (elem.equals(hoverElem)) return;
                AttributeSet attrs = elem.getAttributes();
                boolean isClickable = attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_CLICKABLE) != null;
                boolean isWhitespace = doc.getCharAtPos(mousePosInDoc).equals(' ');
                if (isClickable && !isWhitespace) {
//                    hoverElem = elem;
//                    underlineElement(elem);
                    return;
                }
            }
//            if (hoverElem != null) {
//                hoverElem = null;
//                removeCurrentUnderline();
//            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            TranscriptDocument doc = getTranscriptDocument();
            int mouseButton = e.getButton();

            // Left click
            if (mouseButton == MouseEvent.BUTTON1) {
                int mousePosInDoc = viewToModel2D(e.getPoint());

                Element elem = doc.getCharacterElement(mousePosInDoc);
                AttributeSet attrs = elem.getAttributes();



                if (attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_NOT_TRAVERSABLE) != null) {
                    String elementType = (String) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);
                    if (elementType != null) {
                        if (e.getClickCount() > 1) {
                            switch (elementType) {
                                case TranscriptStyleConstants.ATTR_KEY_RECORD -> {
                                    Tier<?> tier = (Tier<?>) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);
                                    select(doc.getTierStart(tier), doc.getTierEnd(tier));
                                }
                                case TranscriptStyleConstants.ATTR_KEY_COMMENT -> {
                                    Comment comment = (Comment) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_COMMENT);
                                    select(doc.getCommentStart(comment), doc.getCommentEnd(comment));
                                }
                                case TranscriptStyleConstants.ATTR_KEY_GEM -> {
                                    Gem gem = (Gem) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GEM);
                                    select(doc.getGemStart(gem), doc.getGemEnd(gem));
                                }
                            }
                        }
                        else {
                                setCaretPosition(getNextValidIndex(mousePosInDoc, false));
                        }

                        if (attrs.getAttribute("clickable") != null) {
                            switch (elementType) {
                                case TranscriptStyleConstants.ATTR_KEY_RECORD -> {
                                    Tier<?> tier = (Tier<?>) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);
                                    Record record = (Record) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_RECORD);
                                    if (tier != null && record != null) {
                                        onClickTierLabel(e.getPoint(), tier, record);
                                    }
                                }
                                case TranscriptStyleConstants.ATTR_KEY_COMMENT -> onClickCommentLabel(e.getPoint(), (Comment) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_COMMENT));
                                case TranscriptStyleConstants.ATTR_KEY_GEM -> onClickGemLabel(e.getPoint(), (Gem) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GEM));
                                case TranscriptStyleConstants.ATTR_KEY_BLIND_TRANSCRIPTION -> {
                                    Record record = (Record) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_RECORD);
                                    Tier<?> tier = (Tier<?>) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);
                                    String transcriber = (String) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TRANSCRIBER);
                                    onClickBlindTranscriptionLabel(e.getPoint(), record, tier, transcriber);
                                }
                            }
                        }
                    }
                }

                String elementType = (String) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_ELEMENT_TYPE);
                if (elementType != null) {
                    if (e.getClickCount() == 3) {
                        switch (elementType) {
                            case TranscriptStyleConstants.ATTR_KEY_RECORD -> {
                                Tier<?> tier = (Tier<?>) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_TIER);
                                if (tier != null) {
                                    System.out.println(tier.getName());
                                    setSelectionStart(doc.getTierStart(tier));
                                    setSelectionEnd(doc.getTierEnd(tier)-1);
                                }
                            }
                            case TranscriptStyleConstants.ATTR_KEY_COMMENT -> {
                                Comment comment = (Comment) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_COMMENT);
                                if (comment != null) {
                                    System.out.println(comment.getType() + " " + comment.getValue());
                                    setSelectionStart(doc.getCommentStart(comment));
                                    setSelectionEnd(doc.getCommentEnd(comment)-1);
                                }
                            }
                            case TranscriptStyleConstants.ATTR_KEY_GEM -> {
                                Gem gem = (Gem) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GEM);
                                if (gem != null) {
                                    System.out.println(gem.getType() + " " + gem.getLabel());
                                    setSelectionStart(doc.getGemStart(gem));
                                    setSelectionEnd(doc.getGemEnd(gem)-1);
                                }
                            }
                            case TranscriptStyleConstants.ATTR_KEY_GENERIC -> {
                                Tier<?> generic = (Tier<?>) attrs.getAttribute(TranscriptStyleConstants.ATTR_KEY_GENERIC);
                                if (generic != null) {
                                    System.out.println(generic.getName());
                                    setSelectionStart(doc.getGenericStart(generic));
                                    setSelectionEnd(doc.getGenericEnd(generic)-1);
                                }
                            }
                        }
                    }
                }
            }
            // Right click
            else if (mouseButton == MouseEvent.BUTTON3) {
                System.out.println("Right click");
//                Point mousePos = e.getLocationOnScreen();
//                showContextMenu(, mousePos);
            }
        }
    }

    private class TranscriptSelectionListener implements EditorSelectionModelListener {

        @Override
        public void selectionAdded(EditorSelectionModel model, SessionEditorSelection selection) {
            System.out.println("Added selection");

            Highlighter.HighlightPainter painter = selection.getExtension(Highlighter.HighlightPainter.class);
            if (painter == null) {
                painter = new DefaultHighlighter.DefaultHighlightPainter(UIManager.getColor("TextArea.selectionBackground"));
            }

            try {
                int tierStart = getTranscriptDocument().getTierStart(selection.getRecordIndex(), selection.getTierName());
                if (tierStart == -1) return;

                var selectionHighlight = getHighlighter().addHighlight(
                    selection.getRange().getFirst() + tierStart,
                    selection.getRange().getLast() + tierStart,
                    painter
                );
                selectionHighlightList.add(selectionHighlight);
            }
            catch (BadLocationException e) {
                LogUtil.severe(e);
            }
        }

        @Override
        public void selectionSet(EditorSelectionModel model, SessionEditorSelection selection) {
            System.out.println("Set selection");

            for (Object selectionHighLight : selectionHighlightList) {
                getHighlighter().removeHighlight(selectionHighLight);
            }
            selectionHighlightList.clear();

            Highlighter.HighlightPainter painter = selection.getExtension(Highlighter.HighlightPainter.class);
            if (painter == null) {
                painter = new DefaultHighlighter.DefaultHighlightPainter(UIManager.getColor("TextArea.selectionBackground"));
            }

            try {
                int tierStart = getTranscriptDocument().getTierStart(selection.getRecordIndex(), selection.getTierName());
                if (tierStart == -1) return;

                var selectionHighlight = getHighlighter().addHighlight(
                    selection.getRange().getFirst() + tierStart,
                    selection.getRange().getLast() + tierStart,
                    painter
                );
                selectionHighlightList.add(selectionHighlight);
            }
            catch (BadLocationException e) {
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

    public record SessionLocationChangeData(SessionLocation old, SessionLocation newLoc) {}

    // region IExtendable

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

    // endregion IExtendable
}