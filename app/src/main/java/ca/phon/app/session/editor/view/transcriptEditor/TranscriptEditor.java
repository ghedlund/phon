package ca.phon.app.session.editor.view.transcriptEditor;

import ca.phon.app.log.LogUtil;
import ca.phon.app.session.editor.*;
import ca.phon.app.session.editor.undo.*;
import ca.phon.plugin.PluginManager;
import ca.phon.session.*;
import ca.phon.session.Record;
import ca.phon.session.tierdata.TierData;
import ca.phon.ui.CalloutWindow;
import ca.phon.ui.CommonModuleFrame;
import ca.phon.ui.action.PhonUIAction;
import ca.phon.ui.fonts.FontPreferences;
import ca.phon.ui.menu.MenuBuilder;
import ca.phon.util.OSInfo;
import ca.phon.util.Tuple;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class TranscriptEditor extends JEditorPane {
    private final Session session;
    private final EditorEventManager eventManager;
    private SessionMediaModel mediaModel;
    private SessionEditUndoSupport undoSupport;
    private UndoManager undoManager;
    private boolean controlPressed = false;
    private Object currentHighlight;
    DefaultHighlighter.DefaultHighlightPainter highlightPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);
    private int currentRecordIndex = -1;
    private boolean singleRecordView = false;
    public final static EditorEventType<Void> recordChangedInSingleRecordMode = new EditorEventType<>("recordChangedInSingleRecordMode", Void.class);
    private Element hoverElem = null;
    private Object currentUnderline;
    TranscriptUnderlinePainter underlinePainter = new TranscriptUnderlinePainter();
    private MediaSegment selectedSegment = null;
    private int upDownOffset = -1;
    private boolean caretMoveFromUpDown = false;


    public TranscriptEditor(
        Session session,
        EditorEventManager eventManager,
        SessionEditUndoSupport undoSupport,
        UndoManager undoManager
    ) {
        super();
        final TranscriptEditorCaret caret = new TranscriptEditorCaret();
        setCaret(caret);
        getCaret().deinstall(this);
        caret.install(this);
        this.session = session;
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
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (selectedSegment != null && e.getKeyCode() == KeyEvent.VK_SPACE) {
                    if (getSegmentPlayback() != null) {
                        getSegmentPlayback().playSegment(selectedSegment);
                    }
                }
            }
        });
        addCaretListener(e -> {
            TranscriptDocument doc = getTranscriptDocument();
            String transcriptElementType = (String) doc.getCharacterElement(e.getDot()).getAttributes().getAttribute("elementType");
            if (transcriptElementType != null && transcriptElementType.equals("record")) {
                setCurrentRecordIndex(doc.getRecordIndex(e.getDot()));
            }

            // FOR DEBUG PURPOSES ONLY
            SimpleAttributeSet attrs = new SimpleAttributeSet(doc.getCharacterElement(e.getDot()).getAttributes().copyAttributes());
            System.out.println("Attrs: " + attrs);
            System.out.println("Dot: " + e.getDot());
            Tier<?> tier = ((Tier<?>) attrs.getAttribute("tier"));
            System.out.println(tier == null ? null : tier.getName());
//            System.out.println(attrs);
        });
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


        var controlKeyEvent = OSInfo.isMacOs() ? KeyEvent.VK_META : KeyEvent.VK_CONTROL;
        var modifier = OSInfo.isMacOs() ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK;

        KeyStroke pressedControl = KeyStroke.getKeyStroke(controlKeyEvent, modifier, false);
        inputMap.put(pressedControl, "pressedControl");
        PhonUIAction<Void> pressedControlAct = PhonUIAction.runnable(this::pressedControl);
        actionMap.put("pressedControl", pressedControlAct);

        KeyStroke releasedControl = KeyStroke.getKeyStroke(controlKeyEvent, 0, true);
        inputMap.put(releasedControl, "releasedControl");
        PhonUIAction<Void> releasedControlAct = PhonUIAction.runnable(this::releasedControl);
        actionMap.put("releasedControl", releasedControlAct);


        KeyStroke right = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0);
        inputMap.put(right, "nextEditableIndex");
        PhonUIAction<Void> rightAct = PhonUIAction.runnable(() -> setCaretPosition(getNextEditableIndex(getCaretPosition() + 1, true)));
        actionMap.put("nextEditableIndex", rightAct);

        KeyStroke left = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0);
        inputMap.put(left, "prevEditableIndex");
        PhonUIAction<Void> leftAct = PhonUIAction.runnable(() -> setCaretPosition(getPrevEditableIndex(getCaretPosition() - 1, true)));
        actionMap.put("prevEditableIndex", leftAct);


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
        PhonUIAction<Void> enterAct = PhonUIAction.runnable(this::pressedEnter);
        actionMap.put("pressedEnter", enterAct);
    }

    private void registerEditorActions() {
        this.eventManager.registerActionForEvent(EditorEventType.SessionChanged, this::onSessionChanged, EditorEventManager.RunOn.AWTEventDispatchThread);
        this.eventManager.registerActionForEvent(EditorEventType.TierViewChanged, this::onTierViewChanged, EditorEventManager.RunOn.AWTEventDispatchThread);
        this.eventManager.registerActionForEvent(EditorEventType.RecordChanged, this::onRecordChanged, EditorEventManager.RunOn.AWTEventDispatchThread);

        this.eventManager.registerActionForEvent(EditorEventType.RecordAdded, this::onRecordAdded, EditorEventManager.RunOn.AWTEventDispatchThread);
        this.eventManager.registerActionForEvent(EditorEventType.RecordDeleted, this::onRecordDeleted, EditorEventManager.RunOn.AWTEventDispatchThread);
        this.eventManager.registerActionForEvent(EditorEventType.RecordMoved, this::onRecordMoved, EditorEventManager.RunOn.AWTEventDispatchThread);

        this.eventManager.registerActionForEvent(EditorEventType.SpeakerChanged, this::onSpeakerChanged, EditorEventManager.RunOn.AWTEventDispatchThread);

        this.eventManager.registerActionForEvent(EditorEventType.TierChanged, this::onTierDataChanged, EditorEventManager.RunOn.AWTEventDispatchThread);

        this.eventManager.registerActionForEvent(EditorEventType.CommentAdded, this::onCommentAdded, EditorEventManager.RunOn.AWTEventDispatchThread);
        this.eventManager.registerActionForEvent(EditorEventType.GemAdded, this::onGemAdded, EditorEventManager.RunOn.AWTEventDispatchThread);

        this.eventManager.registerActionForEvent(EditorEventType.ElementDeleted, this::onTranscriptElementDeleted, EditorEventManager.RunOn.AWTEventDispatchThread);

        this.eventManager.registerActionForEvent(EditorEventType.CommenTypeChanged, this::onCommentTypeChanged, EditorEventManager.RunOn.AWTEventDispatchThread);
        this.eventManager.registerActionForEvent(EditorEventType.GemTypeChanged, this::onGemTypeChanged, EditorEventManager.RunOn.AWTEventDispatchThread);
    }

    // endregion Init


    // region Getters and Setters

    public TranscriptDocument getTranscriptDocument() {
        return (TranscriptDocument) getDocument();
    }

    public void setAlignmentVisible(boolean visible) {
        getTranscriptDocument().setAlignmentVisible(visible);
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

        String elementType = (String) elem.getAttributes().getAttribute("elementType");

        if (elementType == null) return -1;

        switch (elementType) {
            case "comment" -> {
                Comment comment = (Comment) elem.getAttributes().getAttribute("comment");
                return session.getTranscript().getElementIndex(comment);
            }
            case "gem" -> {
                Gem gem = (Gem) elem.getAttributes().getAttribute("gem");
                return session.getTranscript().getElementIndex(gem);
            }
            case "record" -> {
                Integer recordIndex = (Integer) elem.getAttributes().getAttribute("recordIndex");
                return session.getTranscript().getRecordElementIndex(recordIndex);
            }
            default -> {
                return -1;
            }
        }
    }

    public void setCurrentElementIndex(int index) {

        Transcript.Element transcriptElem = session.getTranscript().getElementAt(index);
        String transcriptElemType;
        if (transcriptElem.isComment()) {transcriptElemType = "comment";}
        else if (transcriptElem.isGem()) {transcriptElemType = "gem";}
        else {transcriptElemType = "record";}

        var root = getTranscriptDocument().getDefaultRootElement();
        for (int i = 0; i < root.getElementCount(); i++) {
            Element elem = root.getElement(i);
            for (int j = 0; j < elem.getElementCount(); j++) {
                Element innerElem = elem.getElement(j);
                String elemType = (String) innerElem.getAttributes().getAttribute("elementType");
                if (elemType != null && elemType.equals(transcriptElemType)) {
                    if (transcriptElem.isComment()) {
                        Comment comment = (Comment) innerElem.getAttributes().getAttribute("comment");
                        if (comment.equals(transcriptElem.asComment())) setCaretPosition(innerElem.getStartOffset());
                    }
                    else if (transcriptElem.isGem()) {
                        Gem gem = (Gem) innerElem.getAttributes().getAttribute("gem");
                        if (gem.equals(transcriptElem.asGem())) setCaretPosition(innerElem.getStartOffset());
                    }
                    else {
                        Record record = (Record) innerElem.getAttributes().getAttribute("record");
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
            Integer recordIndex = (Integer) firstInnerElem.getAttributes().getAttribute("recordIndex");
            if (recordIndex != null) {
                return recordIndex;
            }
        }

        Transcript transcript = session.getTranscript();
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

    public boolean isSyllabificationVisible() {
        return getTranscriptDocument().isSyllabificationVisible();
    }

    public void setSyllabificationVisible(boolean visible) {
        TranscriptDocument doc = getTranscriptDocument();

        var oldVal = doc.isSyllabificationVisible();
        doc.setSyllabificationVisible(visible);

        super.firePropertyChange("syllabificationVisible", oldVal, visible);
    }

    public boolean isSyllabificationComponent() {
        return getTranscriptDocument().isSyllabificationComponent();
    }

    public void setSyllabificationIsComponent(boolean isComponent) {
        TranscriptDocument doc = getTranscriptDocument();

        var oldVal = doc.isSyllabificationComponent();
        doc.setSyllabificationIsComponent(isComponent);

        super.firePropertyChange("syllabificationIsComponent", oldVal, isComponent);
    }

    public boolean isAlignmentVisible() {
        return getTranscriptDocument().isAlignmentVisible();
    }

    public void setAlignmentIsVisible(boolean visible) {
        TranscriptDocument doc = getTranscriptDocument();

        var oldVal = doc.isAlignmentVisible();
        doc.setAlignmentVisible(visible);

        super.firePropertyChange("alignmentVisible", oldVal, visible);
    }

    public boolean isAlignmentComponent() {
        return getTranscriptDocument().isSyllabificationComponent();
    }

    public void setAlignmentIsComponent(boolean isComponent) {
        TranscriptDocument doc = getTranscriptDocument();

        var oldVal = doc.isSyllabificationComponent();
        doc.setAlignmentIsComponent(isComponent);

        super.firePropertyChange("alignmentIsComponent", oldVal, isComponent);
    }

    public EditorEventManager getEventManager() {
        return eventManager;
    }

    public MediaSegment getSelectedSegment() {
        return selectedSegment;
    }

    public void setSelectedSegment(MediaSegment selectedSegment) {
        this.selectedSegment = selectedSegment;
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

    public void pressedControl() {
        if (!controlPressed) {
            System.out.println("Press");
            controlPressed = true;
            Point2D mousePoint = MouseInfo.getPointerInfo().getLocation();
            System.out.println(viewToModel2D(mousePoint));
            highlightElementAtPoint(mousePoint);
            repaint();
        }
    }

    public void releasedControl() {
        if (controlPressed) {
            System.out.println("Release");
            controlPressed = false;
            removeCurrentHighlight();
        }
    }

    public void pressedEnter() {
        if (selectedSegment != null) {
            try {
                var segmentEditor = new SegmentEditorPopup(getMediaModel(), selectedSegment);
                segmentEditor.setPreferredSize(new Dimension(segmentEditor.getPreferredPopupWidth(), (int) segmentEditor.getPreferredSize().getHeight()));

                var start = modelToView2D(getSelectionStart());
                var end = modelToView2D(getSelectionEnd());

                Point point = new Point(
                    (int) (((end.getBounds().getMaxX() - start.getBounds().getMinX()) / 2) + start.getBounds().getMinX()),
                    (int) start.getCenterY()
                );

                point.x += getLocationOnScreen().x;
                point.y += getLocationOnScreen().y;

                System.out.println(point);
                System.out.println(getLocationOnScreen());

                CalloutWindow.showCallout(
                    CommonModuleFrame.getCurrentFrame(),
                    segmentEditor,
                    SwingConstants.NORTH,
                    SwingConstants.CENTER,
                    point
                );
                return;
            }
            catch (BadLocationException e) {
                LogUtil.severe(e);
            }
        }

        TranscriptDocument doc = getTranscriptDocument();

        var attrs = doc.getCharacterElement(getCaretPosition()).getAttributes();
        Tier<?> tier = (Tier<?>) attrs.getAttribute("tier");
        if (tier != null) {
            try {
                int start = doc.getTierStart(tier);
                int end = doc.getTierEnd(tier);
                String newValue = doc.getText(start, end - start);

                tierDataChanged(tier, newValue);
            }
            catch (BadLocationException e) {
                LogUtil.severe(e);
            }
        }

        String elemType = (String) attrs.getAttribute("elementType");
        System.out.println("Element type: " + elemType);
        if (elemType != null && (elemType.equals("record") || elemType.equals("comment") || elemType.equals("gem"))) {
            int elementIndex = -1;
            if (elemType.equals("record")) {
                elementIndex = (int) attrs.getAttribute("recordElementIndex");
            }
            else if (elemType.equals("comment")) {
                Comment comment = (Comment) attrs.getAttribute("comment");
                elementIndex = session.getTranscript().getElementIndex(comment);
            }
            else {
                Gem gem = (Gem) attrs.getAttribute("gem");
                elementIndex = session.getTranscript().getElementIndex(gem);
            }
            if (elementIndex > -1) {
                showContextMenu(elementIndex);
            }
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
        long startTimeMS = new Date().getTime();

        var startTierView = data.oldTierView();
        var endTierView = data.newTierView();

        System.out.println(startTierView.stream().map(item -> item.getTierName()).toList());
        System.out.println(endTierView.stream().map(item -> item.getTierName()).toList());

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
        Tier caretTier = (Tier)elem.getAttributes().getAttribute("tier");
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
        doc.moveTier(movedTiers);
        setDocument(doc);

        // Correct caret
        if (caretTierOffset > -1) {
            // Move the caret so that it has the same offset from the tiers new pos
            setCaretPosition(doc.getTierStart(caretTier) + caretTierOffset);
        }

        System.out.println("Time to move tiers: " + (new Date().getTime() - startTimeMS)/1000f + " seconds");
    }

    public void deleteTier(EditorEventType.TierViewChangedData data) {
        TranscriptDocument doc = getTranscriptDocument();

        List<String> deletedTiersNames = data.tierNames();

        int startCaretPos = getCaretPosition();
        var elem = doc.getCharacterElement(startCaretPos);
        var caretAttrs = elem.getAttributes();
        Tier caretTier = (Tier) caretAttrs.getAttribute("tier");
        Integer caretRecordIndex = (Integer) caretAttrs.getAttribute("recordIndex");

        boolean caretInDeletedTier = caretTier != null && deletedTiersNames.contains(caretTier.getName());

        int caretOffset = doc.getOffsetInContent(startCaretPos);


        Document blank = getEditorKit().createDefaultDocument();
        setDocument(blank);
        // Delete tier in doc
        doc.deleteTier(data.tierNames());
        setDocument(doc);


        // Caret in record / tier
        if (caretTier != null) {
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
            String elementType = (String) caretAttrs.getAttribute("elementType");
            int start = -1;
            switch (elementType) {
                case "comment" -> start = doc.getCommentStart((Comment) caretAttrs.getAttribute("comment"));
                case "gem" -> start = doc.getGemStart((Gem) caretAttrs.getAttribute("gem"));
                case "generic" -> start = doc.getGenericStart((Tier<?>) caretAttrs.getAttribute("generic"));
            }

            setCaretPosition(start + caretOffset);
        }
    }

    public void addTier(EditorEventType.TierViewChangedData data) {
        var doc = getTranscriptDocument();

        int startCaretPos = getCaretPosition();
        var elem = doc.getCharacterElement(startCaretPos);
        Tier caretTier = (Tier)elem.getAttributes().getAttribute("tier");
        int caretTierOffset = -1;

        if (caretTier != null) {
            caretTierOffset = startCaretPos - elem.getStartOffset();
        }

        List<TierViewItem> addedTiers = new ArrayList<>();
        for (TierViewItem item : data.newTierView()) {
            if (!data.oldTierView().contains(item)) {
                addedTiers.add(item);
            }
        }

        Document blank = getEditorKit().createDefaultDocument();
        setDocument(blank);
        // Add tier in doc
        doc.addTier(addedTiers);
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
        Tier caretTier = (Tier) caretAttrs.getAttribute("tier");
        Integer caretRecordIndex = (Integer) caretAttrs.getAttribute("recordIndex");

        boolean caretInHiddenTier = caretTier != null && hiddenTiersNames.contains(caretTier.getName());

        int caretOffset = doc.getOffsetInContent(startCaretPos);


        Document blank = getEditorKit().createDefaultDocument();
        setDocument(blank);
        // Hide tier in doc
        doc.hideTier(data.tierNames());
        setDocument(doc);


        // Caret in record / tier
        if (caretTier != null) {
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
            String elementType = (String) caretAttrs.getAttribute("elementType");
            int start = -1;
            switch (elementType) {
                case "comment" -> start = doc.getCommentStart((Comment) caretAttrs.getAttribute("comment"));
                case "gem" -> start = doc.getGemStart((Gem) caretAttrs.getAttribute("gem"));
                case "generic" -> start = doc.getGenericStart((Tier<?>) caretAttrs.getAttribute("generic"));
            }

            setCaretPosition(start + caretOffset);
        }
    }

    public void showTier(EditorEventType.TierViewChangedData data) {
        var doc = getTranscriptDocument();

        int startCaretPos = getCaretPosition();
        var elem = doc.getCharacterElement(startCaretPos);
        Tier caretTier = (Tier)elem.getAttributes().getAttribute("tier");
        int caretTierOffset = -1;

        if (caretTier != null) {
            caretTierOffset = startCaretPos - elem.getStartOffset();
        }

        List<TierViewItem> shownTiers = new ArrayList<>();
        for (TierViewItem item : data.newTierView()) {
            if (!data.oldTierView().contains(item)) {
                shownTiers.add(item);
            }
        }

        Document blank = getEditorKit().createDefaultDocument();
        setDocument(blank);
        // Show tier in doc
        doc.showTier(shownTiers, data.newTierView());
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

        List<TierViewItem> oldTiers = new ArrayList<>();
        List<TierViewItem> newTiers = new ArrayList<>();
        for (Integer index : data.viewIndices()) {
            TierViewItem item = data.newTierView().get(index);
            if (item.isVisible()) {
                oldTiers.add(data.oldTierView().get(index));
                newTiers.add(item);
            }
        }


        if (newTiers.isEmpty()) return;

        int caretPos = getCaretPosition();

        TranscriptDocument doc = getTranscriptDocument();
        Document blank = getEditorKit().createDefaultDocument();
        setDocument(blank);
        doc.tierNameChanged(oldTiers, newTiers);
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
        doc.tierFontChanged(changedTiers);
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
        Record addedRecord = session.getRecord(data.recordIndex());
        // Add it to the doc
        getTranscriptDocument().addRecord(addedRecord);
    }

    private void onRecordDeleted(EditorEvent<EditorEventType.RecordDeletedData> editorEvent) {
        TranscriptDocument doc = getTranscriptDocument();

        int deletedRecordIndex = editorEvent.data().recordIndex();

        int startCaretPos = getCaretPosition();
        var elem = doc.getCharacterElement(startCaretPos);
        var caretAttrs = elem.getAttributes();
        Tier caretTier = (Tier) caretAttrs.getAttribute("tier");
        Integer caretRecordIndex = (Integer) caretAttrs.getAttribute("recordIndex");

        boolean caretInDeletedRecord = caretRecordIndex != null && caretRecordIndex == deletedRecordIndex;

        int caretOffset = doc.getOffsetInContent(startCaretPos);


        // Delete the record from the doc
        var data = editorEvent.data();
        getTranscriptDocument().deleteRecord(data.recordIndex(), data.elementIndex());


        // Caret in record / tier
        if (caretTier != null) {
            // Caret in deleted record
            if (caretInDeletedRecord) {
                boolean deletedRecordWasLast = deletedRecordIndex == session.getRecordCount();

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
            String elementType = (String) caretAttrs.getAttribute("elementType");
            int start = -1;
            switch (elementType) {
                case "comment" -> start = doc.getCommentStart((Comment) caretAttrs.getAttribute("comment"));
                case "gem" -> start = doc.getGemStart((Gem) caretAttrs.getAttribute("gem"));
                case "generic" -> start = doc.getGenericStart((Tier<?>) caretAttrs.getAttribute("generic"));
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
        setCaretPosition(getNextEditableIndex(caretPos, false));
    }

    private void onSpeakerChanged(EditorEvent<EditorEventType.SpeakerChangedData> editorEvent) {
        var data = editorEvent.data();
        // Update the speaker on the separator in the doc
        getTranscriptDocument().changeSpeaker(data.record());
    }

    private void onTierDataChanged(EditorEvent<EditorEventType.TierChangeData> editorEvent) {
        var data = editorEvent.data();
        // Update the changed tier data in the doc
        getTranscriptDocument().onTierDataChanged(data.tier());
    }

    // endregion Record Changes


    // region On Click

    private void onClickTierLabel(Point2D point) {
        // Build a new popup menu
        JPopupMenu menu = new JPopupMenu();
        MenuBuilder builder = new MenuBuilder(menu);

        var extPts = PluginManager.getInstance().getExtensionPoints(TierLabelMenuHandler.class);

        for (var extPt : extPts) {
            var menuHandler = extPt.getFactory().createObject();
            menuHandler.addMenuItems(builder);
        }

        // Show it where the user clicked
        menu.show(this, (int) point.getX(), (int) point.getY());
    }

    private void onClickCommentLabel(Point2D point, Comment comment) {
        int clickedElementIndex = session.getTranscript().getElementIndex(comment);

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
                ChangeCommentTypeEdit edit = new ChangeCommentTypeEdit(session, eventManager, comment, type);
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
        int clickedElementIndex = session.getTranscript().getElementIndex(gem);
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
                ChangeGemTypeEdit edit = new ChangeGemTypeEdit(session, eventManager, gem, type);
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

    // endregion On Click

    private void showContextMenu(int transcriptElementIndex) {
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
        PhonUIAction<Void> deleteThisAct = PhonUIAction.runnable(() -> deleteTranscriptElement(session.getTranscript().getElementAt(transcriptElementIndex)));
        deleteThisAct.putValue(PhonUIAction.NAME, "Delete this element");
        deleteThis.setAction(deleteThisAct);
        menu.add(deleteThis);

        var mousePos = MouseInfo.getPointerInfo().getLocation();
        menu.show(this, (int) (mousePos.getX() - getLocationOnScreen().getX()), (int) (mousePos.getY() - getLocationOnScreen().getY()));
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

        g.setColor(UIManager.getColor(TranscriptEditorUIProps.SEPARATOR_LINE));
        int sepLineHeight = 1;
        int fontHeight = fontMetrics.getHeight();
        Element root = doc.getDefaultRootElement();
        if (root.getElementCount() == 0) return;
        float lineSpacing = StyleConstants.getLineSpacing(root.getElement(0).getAttributes());
        int sepLineOffset = (int) (((fontHeight * lineSpacing) + sepLineHeight) / 2);
        // For every element
        for (int i = 0; i < root.getElementCount(); i++) {
            Element elem = root.getElement(i);
            if (elem.getElementCount() == 0) continue;
            Element innerElem = elem.getElement(0);
            AttributeSet attrs = innerElem.getAttributes();
            // If it's a separator
            if (attrs.getAttribute("sep") != null) {
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
        ChangeSpeakerEdit edit = new ChangeSpeakerEdit(session, eventManager, data.getObj1(), data.getObj2());
        undoSupport.postEdit(edit);
    }

    public void tierDataChanged(Tier<?> tier, String newData) {
        Tier<?> dummy = SessionFactory.newFactory().createTier("dummy", tier.getDeclaredType());
        dummy.setText(newData);



        TierEdit edit = new TierEdit(session, eventManager, null, tier, dummy.getValue());
        getUndoSupport().postEdit(edit);
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
        }
    }

    public void loadSession() {
        TranscriptDocument doc = (TranscriptDocument) getEditorKit().createDefaultDocument();
        doc.setSession(session);
        setDocument(doc);
    }

    public int getNextEditableIndex(int currentPos, boolean looping) {
        TranscriptDocument doc = getTranscriptDocument();

        int docLen = doc.getLength();

        Element elem = doc.getCharacterElement(currentPos);
        while (elem.getAttributes().getAttribute("notEditable") != null && currentPos < docLen) {
            currentPos++;
            elem = doc.getCharacterElement(currentPos);
        }

        if (currentPos == docLen) {
            if (looping) {
                return getNextEditableIndex(0, true);
            }
            else {
                return -1;
            }
        }

        return currentPos;
    }

    public int getPrevEditableIndex(int currentPos, boolean looping) {
        TranscriptDocument doc = getTranscriptDocument();

        Element elem = doc.getCharacterElement(currentPos);
        while (elem.getAttributes().getAttribute("notEditable") != null && currentPos >= 0) {
            currentPos--;
            elem = doc.getCharacterElement(currentPos);
        }

        if (looping && currentPos < 0) {
            return getPrevEditableIndex(doc.getLength()-1, true);
        }

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

        String elementType = (String) prevElementAttributes.getAttribute("elementType");

        System.out.println("Prev element type: " + elementType);

        if (elementType == null) {
            return;
        }
        else if (elementType.equals("record")) {
            end = doc.getTierEnd((Tier<?>) prevElementAttributes.getAttribute("tier"));
        }
        else if (elementType.equals("comment")) {
            end = doc.getCommentEnd((Comment) prevElementAttributes.getAttribute("comment"));
        }
        else if (elementType.equals("gem")) {
            Gem gem = (Gem) prevElementAttributes.getAttribute("gem");
            System.out.println("Gem: " + gem.getType() + " " + gem.getLabel());
            end = doc.getGemEnd(gem);
            System.out.println("Gem end: " + end);
        }
        else if (elementType.equals("generic")) {
            Tier<?> genericTier = (Tier<?>) prevElementAttributes.getAttribute("generic");
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

        String elementType = (String) nextElementAttributes.getAttribute("elementType");
        System.out.println("Next element type: " + elementType);

        if (elementType == null) {
            return;
        }
        else if (elementType.equals("record")) {
            end = doc.getTierEnd((Tier<?>) nextElementAttributes.getAttribute("tier"));
        }
        else if (elementType.equals("comment")) {
            end = doc.getCommentEnd((Comment) nextElementAttributes.getAttribute("comment"));
        }
        else if (elementType.equals("gem")) {
            end = doc.getGemEnd((Gem) nextElementAttributes.getAttribute("gem"));
        }
        else if (elementType.equals("generic")) {
            end = doc.getGenericEnd((Tier<?>) nextElementAttributes.getAttribute("generic"));
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

        Integer recordIndex = (Integer) currentPosAttrs.getAttribute("recordIndex");
        String elementType = (String) currentPosAttrs.getAttribute("elementType");
        Object content;
        if (elementType.equals("record")) {
            content = currentPosAttrs.getAttribute("tier");
        }
        else {
            content = currentPosAttrs.getAttribute(elementType);
        }

        int currentDocElemIndex = doc.getDefaultRootElement().getElementIndex(caretPos);

        System.out.println(recordIndex);
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
                String innerDocElemType = (String) attrs.getAttribute("elementType");
                System.out.println(innerDocElemType);
                if (isLabel == null && innerDocElemType != null) {
                    if (!innerDocElemType.equals(elementType)) {
                        System.out.println(innerDocElem.getStartOffset());
                        return innerDocElem.getStartOffset();
                    }
                    if (innerDocElemType.equals("record")) {
                        if (attrs.getAttribute("tier") != content) {
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

        Integer recordIndex = (Integer) currentPosAttrs.getAttribute("recordIndex");
        String elementType = (String) currentPosAttrs.getAttribute("elementType");
        Object content;
        if (elementType.equals("record")) {
            content = currentPosAttrs.getAttribute("tier");
        }
        else {
            content = currentPosAttrs.getAttribute(elementType);
        }

        int currentDocElemIndex = doc.getDefaultRootElement().getElementIndex(caretPos);

        System.out.println(recordIndex);
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
                String innerDocElemType = (String) attrs.getAttribute("elementType");
                System.out.println(innerDocElemType);
                if (isLabel == null && innerDocElemType != null) {
                    if (!innerDocElemType.equals(elementType)) {
                        System.out.println(innerDocElem.getStartOffset());
                        return innerDocElem.getStartOffset();
                    }
                    if (innerDocElemType.equals("record")) {
                        if (attrs.getAttribute("tier") != content) {
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

    private void underlineElement(Element elem) {
        try {
            removeCurrentUnderline();
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

    private void addComment(int relativeElementIndex, int position) {
        Transcript transcript = session.getTranscript();
        Tier<TierData> commentTier = SessionFactory.newFactory().createTier("Comment Tier", TierData.class);
        commentTier.setText("New comment");
        Comment newComment = SessionFactory.newFactory().createComment(CommentType.Blank, commentTier.getValue());

        int newCommentIndex = -1;
        switch (position) {
            case SwingConstants.PREVIOUS -> newCommentIndex = relativeElementIndex;
            case SwingConstants.NEXT -> newCommentIndex = relativeElementIndex + 1;
            case SwingConstants.BOTTOM -> newCommentIndex = transcript.getNumberOfElements();
        }

        AddTranscriptElementEdit edit = new AddTranscriptElementEdit(
            session,
            eventManager,
            new Transcript.Element(newComment),
            newCommentIndex
        );
        undoSupport.postEdit(edit);
    }

    private void onCommentAdded(EditorEvent<EditorEventType.CommentAddedData> editorEvent) {
        var data = editorEvent.data();
        getTranscriptDocument().addComment(data.record(), data.elementIndex());
    }

    private void addGem(int relativeElementIndex, int position) {
        Transcript transcript = session.getTranscript();
        Gem newGem = SessionFactory.newFactory().createGem(GemType.Lazy, "New gem");

        int newGemIndex = -1;
        switch (position) {
            case SwingConstants.PREVIOUS -> newGemIndex = relativeElementIndex;
            case SwingConstants.NEXT -> newGemIndex = relativeElementIndex + 1;
            case SwingConstants.BOTTOM -> newGemIndex = transcript.getNumberOfElements();
        }

        AddTranscriptElementEdit edit = new AddTranscriptElementEdit(
            session,
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
            session,
            eventManager,
            elem,
            session.getTranscript().getElementIndex(elem)
        );
        undoSupport.postEdit(edit);
    }

    private void onTranscriptElementDeleted(EditorEvent<EditorEventType.ElementDeletedData> editorEvent) {
        getTranscriptDocument().deleteTranscriptElement(editorEvent.data().element());
    }

    private void onCommentTypeChanged(EditorEvent<EditorEventType.CommentTypeChangedData> editorEvent) {
        getTranscriptDocument().changeCommentType(editorEvent.data().comment());
    }

    private void onGemTypeChanged(EditorEvent<EditorEventType.GemTypeChangedData> editorEvent) {
        getTranscriptDocument().changeGemType(editorEvent.data().gem());
    }

    private class TranscriptNavigationFilter extends NavigationFilter {
        @Override
        public void setDot(NavigationFilter.FilterBypass fb, int dot, Position.Bias bias) {

            setSelectedSegment(null);
            TranscriptDocument doc = getTranscriptDocument();
            Element elem = doc.getCharacterElement(dot);
            AttributeSet attrs = elem.getAttributes();
            boolean isLabel = attrs.getAttribute("label") != null;
            boolean isSegment = attrs.getAttribute("mediaSegment") != null;

            if (isLabel && !isSegment) return;

            Tier<?> prevTier = (Tier<?>) doc.getCharacterElement(fb.getCaret().getDot()).getAttributes().getAttribute("tier");
            Tier<?> nextTier = (Tier<?>) doc.getCharacterElement(dot).getAttributes().getAttribute("tier");

            if (prevTier != null && (nextTier == null || !prevTier.getName().equals(nextTier.getName()))) {
                try {
                    int start = doc.getTierStart(prevTier);
                    int end = doc.getTierEnd(prevTier);
                    String newValue = doc.getText(start, end - start);

                    // TODO figure out a better way of doing this
//                    if (!newValue.equals(prevTier.getValue().toString())) {
//                        tierDataChanged(prevTier, newValue);
//                    }
                }
                catch (BadLocationException e) {
                    LogUtil.severe(e);
                }
            }

            if (doc.getLength() == dot) return;

            if (!caretMoveFromUpDown) upDownOffset = -1;
            caretMoveFromUpDown = false;

            fb.setDot(dot, bias);
        }
        @Override
        public void moveDot(NavigationFilter.FilterBypass fb, int dot, Position.Bias bias) {
            TranscriptDocument doc = getTranscriptDocument();

            if (getTranscriptDocument().getLength() == dot) return;

            Tier<?> selectionStartTier = doc.getTier(getCaret().getDot());
            if (selectionStartTier != null) {
                AttributeSet attrs = doc.getCharacterElement(dot).getAttributes();
                Tier<?> selectionEndTier = (Tier<?>) attrs.getAttribute("tier");
                Boolean isLabel = (Boolean) attrs.getAttribute("label");
                if (selectionStartTier != selectionEndTier || isLabel != null) {
                    return;
                }
            }

            fb.moveDot(dot, bias);
        }
    }

    private class TranscriptUnderlinePainter implements Highlighter.HighlightPainter {
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

    private class TranscriptMouseAdapter extends MouseAdapter {
        @Override
        public void mouseMoved(MouseEvent e) {
            TranscriptDocument doc = getTranscriptDocument();

            if (controlPressed) {
                highlightElementAtPoint(e.getPoint());
            }

            int mousePosInDoc = viewToModel2D(e.getPoint());

            Element elem = doc.getCharacterElement(mousePosInDoc);
            if (elem != null) {
                if (elem.equals(hoverElem)) return;
                AttributeSet attrs = elem.getAttributes();
                boolean isLabel = attrs.getAttribute("clickable") != null;
                boolean isWhitespace = doc.getCharAtPos(mousePosInDoc).equals(' ');
                if (isLabel && !isWhitespace) {
                    hoverElem = elem;
                    underlineElement(elem);
                    return;
                }
            }
            if (hoverElem != null) {
                hoverElem = null;
                removeCurrentUnderline();
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            TranscriptDocument doc = getTranscriptDocument();
            int mousePosInDoc = viewToModel2D(e.getPoint());

            Element elem = doc.getCharacterElement(mousePosInDoc);
            AttributeSet attrs = elem.getAttributes();

            MediaSegment mediaSegment = (MediaSegment) attrs.getAttribute("mediaSegment");
            if (mediaSegment != null) {
                var segmentBounds = doc.getSegmentBounds(mediaSegment, elem);
                System.out.println("Segment bounds: " + segmentBounds);
                setCaretPosition(segmentBounds.getObj1());
                setSelectedSegment(mediaSegment);
                moveCaretPosition(segmentBounds.getObj2()+1);
                return;
            }

            if (attrs.getAttribute("label") != null) {
                String elementType = (String) attrs.getAttribute("elementType");
                if (elementType != null) {
                    if (e.getClickCount() > 1) {
                        switch (elementType) {
                            case "record" -> {
                                Tier<?> tier = (Tier<?>) attrs.getAttribute("tier");
                                select(doc.getTierStart(tier), doc.getTierEnd(tier));
                            }
                            case "comment" -> {
                                Comment comment = (Comment) attrs.getAttribute("comment");
                                select(doc.getCommentStart(comment), doc.getCommentEnd(comment));
                            }
                            case "gem" -> {
                                Gem gem = (Gem) attrs.getAttribute("gem");
                                select(doc.getGemStart(gem), doc.getGemEnd(gem));
                            }
                        }
                    }
                    else {
                        switch (elementType) {
                            case "record" -> setCaretPosition(doc.getTierStart((Tier<?>) attrs.getAttribute("tier")));
                            case "comment" -> setCaretPosition(doc.getCommentStart((Comment) attrs.getAttribute("comment")));
                            case "gem" -> setCaretPosition(doc.getGemStart((Gem) attrs.getAttribute("gem")));
                        }
                    }

                    if (attrs.getAttribute("clickable") != null) {
                        switch (elementType) {
                            case "record" -> onClickTierLabel(e.getPoint());
                            case "comment" -> onClickCommentLabel(e.getPoint(), (Comment) attrs.getAttribute("comment"));
                            case "gem" -> onClickGemLabel(e.getPoint(), (Gem) attrs.getAttribute("gem"));
                        }
                    }
                }
            }
        }
    }
}