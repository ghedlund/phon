package ca.phon.app.session.editor.view.transcript;

import ca.phon.app.session.editor.*;
import ca.phon.app.session.editor.actions.*;
import ca.phon.app.session.editor.autotranscribe.AutoTranscribeAction;
import ca.phon.app.session.editor.search.FindAndReplacePanel;
import ca.phon.app.session.editor.undo.*;
import ca.phon.app.session.editor.view.mediaPlayer.MediaPlayerEditorView;
import ca.phon.app.session.editor.view.participants.ParticipantsView;
import ca.phon.app.session.editor.view.speechAnalysis.SpeechAnalysisEditorView;
import ca.phon.app.session.editor.view.tierManagement.TierMenuBuilder;
import ca.phon.app.session.editor.view.timeline.TimelineView;
import ca.phon.app.session.editor.view.transcript.actions.*;
import ca.phon.app.session.editor.view.transcript.extensions.*;
import ca.phon.session.*;
import ca.phon.session.io.SessionOutputFactory;
import ca.phon.session.position.TranscriptElementLocation;
import ca.phon.ui.*;
import ca.phon.ui.action.PhonActionEvent;
import ca.phon.ui.action.PhonUIAction;
import ca.phon.ui.fonts.FontPreferences;
import ca.phon.ui.layout.ButtonBarBuilder;
import ca.phon.ui.menu.MenuBuilder;
import ca.phon.ui.nativedialogs.MessageDialogProperties;
import ca.phon.ui.nativedialogs.NativeDialogEvent;
import ca.phon.ui.nativedialogs.NativeDialogs;
import ca.phon.util.Language;
import ca.phon.util.PrefHelper;
import ca.phon.util.icons.IconManager;
import ca.phon.util.icons.IconSize;
import org.jdesktop.swingx.HorizontalLayout;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTree;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

/**
 * The {@link EditorView} that uses the {@link TranscriptEditor}
 * */
public class TranscriptView extends EditorView {

    public final static String VIEW_NAME = "Transcript";
    public final static String VIEW_ICON = IconManager.GoogleMaterialDesignIconsFontName + ":description";

    private IconStrip iconStrip;

    private DropDownButton playSegmentButton;

    private final TranscriptEditor transcriptEditor;
    private TranscriptScrollPane transcriptScrollPane;
    private FindAndReplacePanel findAndReplacePanel;
    private JPanel centerPanel;

    /* Preferences stuff */

    public final static String FONT_SIZE_DELTA_PROP = TranscriptView.class.getName() + ".fontSizeDelta";
    public final static float DEFAULT_FONT_SIZE_DELTA = 0.0f;

    /* State */

    private final static int FONT_SIZE_DELTA_MIN = -8;
    private final static int FONT_SIZE_DELTA_MAX = 8;
    public float fontSizeDelta = PrefHelper.getFloat(FONT_SIZE_DELTA_PROP, DEFAULT_FONT_SIZE_DELTA);
    private boolean findAndReplaceVisible = false;

    /** Transcript tree view (debug mode) */
    private JPanel transcriptTreePanel = null;
    private JXTree transcriptTree = null;

    /**
     * Constructor
     * */
    public TranscriptView(SessionEditor editor) {
        super(editor);
        this.transcriptEditor = new TranscriptEditor(
            editor.getDataModel(),
            editor.getSelectionModel(),
            editor.getEventManager(),
            editor.getUndoSupport(),
            editor.getUndoManager()
        );

        this.transcriptEditor.setMediaModel(editor.getMediaModel());
        this.transcriptEditor.addPropertyChangeListener(
            "currentRecordIndex", e -> {
                if(!isSingleRecordView()) {
                    editor.setCurrentRecordIndex((Integer) e.getNewValue(), transcriptEditor);
                }
            }
        );
        this.transcriptEditor.addMouseListener(contextMouseHandler);

        initUI();

        editor.getEventManager().registerActionForEvent(
            EditorEventType.EditorFinishedLoading,
            this::onEditorFinishedLoading,
            EditorEventManager.RunOn.EditorEventDispatchThread
        );

        addPropertyChangeListener("fontSizeDelta", e -> {
            PrefHelper.getUserPreferences().putFloat(FONT_SIZE_DELTA_PROP, getFontSizeDelta());
            transcriptEditor.recalculateTierLabelWidth();
        });
        setupKeyboardShortcuts();
    }

    private void setupKeyboardShortcuts() {
        InputMap inputMap = transcriptEditor.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = transcriptEditor.getActionMap();

        KeyStroke save = KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        inputMap.put(save, "save");
        PhonUIAction<Void> saveAct = PhonUIAction.runnable(() -> {
            transcriptEditor.saveCurrentLine();
            SwingUtilities.invokeLater(() -> new SaveSessionAction(getEditor()).hookableActionPerformed(null));
        });
        actionMap.put("save", saveAct);

        KeyStroke saveAs = KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | InputEvent.SHIFT_DOWN_MASK);
        inputMap.put(saveAs, "saveAs");
        PhonUIAction<Void> saveAsAct = PhonUIAction.runnable(() -> {
            transcriptEditor.saveCurrentLine();
            SwingUtilities.invokeLater(() -> new SaveAsAction(getEditor(), new SessionOutputFactory().availableSessionIOs().get(0)).hookableActionPerformed(null));
        });
        actionMap.put("saveAs", saveAsAct);

        KeyStroke increaseFontSizeKs = KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        inputMap.put(increaseFontSizeKs, "increaseFontSize");
        PhonUIAction<Void> increaseFontSizeAct = PhonUIAction.runnable(() -> {
            setFontSizeDelta(Math.min(FONT_SIZE_DELTA_MAX, getFontSizeDelta() + 1));
            transcriptEditor.getTranscriptDocument().reload();
        });
        actionMap.put("increaseFontSize", increaseFontSizeAct);

        KeyStroke decreaseFontSizeKs = KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        inputMap.put(decreaseFontSizeKs, "decreaseFontSize");
        PhonUIAction<Void> decreaseFontSizeAct = PhonUIAction.runnable(() -> {
            setFontSizeDelta(Math.max(FONT_SIZE_DELTA_MIN, getFontSizeDelta() - 1));
            transcriptEditor.getTranscriptDocument().reload();
        });
        actionMap.put("decreaseFontSize", decreaseFontSizeAct);

        KeyStroke showFindKs = KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        inputMap.put(showFindKs, "showFind");
        PhonUIAction<Void> showFindAct = PhonUIAction.runnable(this::showFind);
        actionMap.put("showFind", showFindAct);

        KeyStroke showReplaceKs = KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        inputMap.put(showReplaceKs, "showReplace");
        PhonUIAction<Void> showReplaceAct = PhonUIAction.runnable(this::showReplace);
        actionMap.put("showReplace", showReplaceAct);
    }

    /**
     * Sets up the UI
     * */
    private void initUI() {
        setLayout(new BorderLayout());

        iconStrip = new IconStrip(SwingConstants.HORIZONTAL);
        setupIconStrip();
        add(iconStrip, BorderLayout.NORTH);

        centerPanel = new JPanel(new BorderLayout());
        add(centerPanel, BorderLayout.CENTER);

        transcriptScrollPane = new TranscriptScrollPane(transcriptEditor);
        transcriptScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        centerPanel.add(transcriptScrollPane, BorderLayout.CENTER);
        centerPanel.add(new TranscriptStatusBar(transcriptEditor), BorderLayout.SOUTH);
    }

    /**
     * Setup icon strip
     */
    private void setupIconStrip() {
        // participants button
        final PhonUIAction<Void> participantsAct = PhonUIAction.eventConsumer(this::showParticipantsMenu);
        participantsAct.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
        participantsAct.putValue(FlatButton.ICON_NAME_PROP, "group");
        participantsAct.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);
        participantsAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Participants menu");
        participantsAct.putValue(PhonUIAction.NAME, "Participants");
        final FlatButton participantsBtn = new FlatButton(participantsAct);
        participantsBtn.setPadding(2);

        // tiers button
        final PhonUIAction<Void> tiersAct = PhonUIAction.eventConsumer(this::showTiersMenu);
        tiersAct.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
        tiersAct.putValue(FlatButton.ICON_NAME_PROP, "data_table");
        tiersAct.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);
        tiersAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Tiers menu");
        tiersAct.putValue(PhonUIAction.NAME, "Tiers");
        final FlatButton tiersBtn = new FlatButton(tiersAct);
        tiersBtn.setPadding(2);

        // records button
        final PhonUIAction<Void> recordsAct = PhonUIAction.eventConsumer(this::showRecordsMenu);
        recordsAct.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
        recordsAct.putValue(FlatButton.ICON_NAME_PROP, "article");
        recordsAct.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);
        recordsAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Records menu");
        recordsAct.putValue(PhonUIAction.NAME, "Records");
        final FlatButton recordsBtn = new FlatButton(recordsAct);
        recordsBtn.setPadding(2);

        // comments menu
        final PhonUIAction<Void> commentsAct = PhonUIAction.eventConsumer(this::showCommentsMenu);
        commentsAct.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
        commentsAct.putValue(FlatButton.ICON_NAME_PROP, "comment");
        commentsAct.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);
        commentsAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Comments menu");
        commentsAct.putValue(PhonUIAction.NAME, "Comments");
        final FlatButton commentsBtn = new FlatButton(commentsAct);
        commentsBtn.setPadding(2);

        // gems menu
        final PhonUIAction<Void> gemsAct = PhonUIAction.eventConsumer(this::showGemsMenu);
        gemsAct.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
        gemsAct.putValue(FlatButton.ICON_NAME_PROP, "diamond");
        gemsAct.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);
        gemsAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Gems menu");
        gemsAct.putValue(PhonUIAction.NAME, "Gems");
        final FlatButton gemsBtn = new FlatButton(gemsAct);
        gemsBtn.setPadding(2);

//        // transcript button
//        final PhonUIAction<Void> transcriptAct = PhonUIAction.eventConsumer(this::showTranscriptMenu);
//        transcriptAct.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
//        transcriptAct.putValue(FlatButton.ICON_NAME_PROP, "description");
//        transcriptAct.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);
//        transcriptAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Transcript menu");
//        transcriptAct.putValue(PhonUIAction.NAME, "Transcript");
//        final FlatButton transcriptBtn = new FlatButton(transcriptAct);
//        transcriptBtn.setPadding(2);

//        // single record mode button
//        final PhonUIAction<Void> singleRecordModeAct = PhonUIAction.runnable(transcriptEditor::toggleSingleRecordView);
//        singleRecordModeAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Toggle single record view");
//        singleRecordModeAct.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
//        singleRecordModeAct.putValue(FlatButton.ICON_NAME_PROP, "view_stream");
//        singleRecordModeAct.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);
//        singleRecordModeAct.putValue(PhonUIAction.SELECTED_KEY, isSingleRecordView());
//        final FlatButton singleRecordModeBtn = new FlatButton(singleRecordModeAct);
//        singleRecordModeBtn.setPadding(2);
//        singleRecordModeBtn.setIconColor(UIManager.getColor("textInactiveText"));
//        singleRecordModeBtn.setIconSelectedColor(UIManager.getColor("Phon.darkBlue"));
//        transcriptEditor.addPropertyChangeListener("singleRecordView", (e) ->
//                singleRecordModeBtn.setSelected(isSingleRecordView()));

//        // find and replace button
//        final PhonUIAction<Void> findReplaceAct = PhonUIAction.runnable(this::toggleFindAndReplace);
//        findReplaceAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Find and replace");
//        findReplaceAct.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
//        findReplaceAct.putValue(FlatButton.ICON_NAME_PROP, "search");
//        findReplaceAct.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);
//        final FlatButton findReplaceBtn = new FlatButton(findReplaceAct);
//        findReplaceBtn.setPadding(2);
//        findReplaceBtn.setIconColor(UIManager.getColor("textInactiveText"));
//        findReplaceBtn.setIconSelectedColor(UIManager.getColor("Phon.darkBlue"));
//        addPropertyChangeListener("findAndReplaceVisible", (e) -> {
//            findReplaceBtn.setSelected(isFindAndReplaceVisible());
//        });

        JPopupMenu playSegmentMenu = new JPopupMenu();
        playSegmentMenu.addPopupMenuListener(new PopupMenuListener() {

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                playSegmentMenu.removeAll();
                setupPlaySegmentMenu(new MenuBuilder(playSegmentMenu));
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }

        });

        final ImageIcon playIcn = IconManager.getInstance()
                .getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "play_circle", IconSize.MEDIUM, Color.black);
        final PhonUIAction playSegmentAct = PhonUIAction.eventConsumer(this::playPause);
        playSegmentAct.putValue(PhonUIAction.NAME, "Play segment");
        playSegmentAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Play segment");
        playSegmentAct.putValue(PhonUIAction.SMALL_ICON, playIcn);
        playSegmentAct.putValue(DropDownButton.BUTTON_POPUP, playSegmentMenu);
        playSegmentButton = new DropDownButton(playSegmentAct);
        playSegmentButton.setBorderPainted(false);
        playSegmentButton.setFocusPainted(false);
        playSegmentButton.setContentAreaFilled(false);
        playSegmentButton.setRolloverEnabled(true);

        PhonUIAction<Void> fontScaleMenuAct = PhonUIAction.eventConsumer(this::showFontScaleMenu, null);
        fontScaleMenuAct.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
        fontScaleMenuAct.putValue(FlatButton.ICON_NAME_PROP, "text_increase");
        fontScaleMenuAct.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);
        fontScaleMenuAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Show font scale menu");
        fontScaleMenuAct.putValue(DropDownButton.ARROW_ICON_POSITION, SwingConstants.BOTTOM);
        fontScaleMenuAct.putValue(DropDownButton.ARROW_ICON_GAP, 2);
        final FlatButton fontScaleMenuButton = new FlatButton(fontScaleMenuAct);
        fontScaleMenuButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
//        toolbar.add(fontScaleMenuButton);


        iconStrip.add(participantsBtn, IconStrip.IconStripPosition.LEFT);
        iconStrip.add(tiersBtn, IconStrip.IconStripPosition.LEFT);
        iconStrip.add(recordsBtn, IconStrip.IconStripPosition.LEFT);
        iconStrip.add(commentsBtn, IconStrip.IconStripPosition.LEFT);
        iconStrip.add(gemsBtn, IconStrip.IconStripPosition.LEFT);
        iconStrip.add(new JSeparator(SwingConstants.VERTICAL), IconStrip.IconStripPosition.LEFT);
        iconStrip.add(fontScaleMenuButton, IconStrip.IconStripPosition.LEFT);
//        iconStrip.add(transcriptBtn, IconStrip.IconStripPosition.LEFT);
//        iconStrip.add(findReplaceBtn, IconStrip.IconStripPosition.RIGHT);
//        iconStrip.add(singleRecordModeBtn, IconStrip.IconStripPosition.RIGHT);


        NavigationPanel navPanel = new NavigationPanel(getEditor());
        iconStrip.add(playSegmentButton, IconStrip.IconStripPosition.RIGHT);
        iconStrip.add(navPanel, IconStrip.IconStripPosition.RIGHT);
    }

    public void playPause(PhonActionEvent<Void> pae) {
        final SessionMediaModel mediaModel = getEditor().getMediaModel();
        final SegmentPlayback segPlayback = mediaModel.getSegmentPlayback();
        if(segPlayback.isPlaying()) {
            segPlayback.stopPlaying();
        } else {
            (new PlaySegmentAction(getEditor())).actionPerformed(pae.getActionEvent());
        }
    }

    private void setupPlaySegmentMenu(MenuBuilder builder) {
        final SessionMediaModel mediaModel = getEditor().getMediaModel();
        final SegmentPlayback segPlayback = mediaModel.getSegmentPlayback();

        if(segPlayback.isPlaying()) {
            final PhonUIAction stopAct = PhonUIAction.runnable(segPlayback::stopPlaying);
            stopAct.putValue(PhonUIAction.NAME, "Stop playback");
            stopAct.putValue(PhonUIAction.SMALL_ICON, IconManager.getInstance().getIcon("actions/media-playback-stop", IconSize.SMALL));
            stopAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Stop segment playback");
            builder.addItem(".", stopAct);

            builder.addSeparator(".", "s1");
        }

        boolean enabled = (mediaModel.isSessionAudioAvailable() ||
                (mediaModel.isSessionMediaAvailable() && getEditor().getViewModel().isShowing(MediaPlayerEditorView.VIEW_NAME)));
        builder.addItem(".", new PlaySegmentAction(getEditor())).setEnabled(enabled);
        builder.addItem(".", new PlayCustomSegmentAction(getEditor())).setEnabled(enabled);
        builder.addItem(".", new PlaySpeechTurnAction(getEditor())).setEnabled(enabled);
        builder.addItem(".", new PlayAdjacencySequenceAction(getEditor())).setEnabled(enabled);
    }

    public TranscriptEditor getTranscriptEditor() {
        return this.transcriptEditor;
    }


    /**
     * Shows the font scale menu
     *
     * @param pae the event from the action that called the function
     **/
    private void showFontScaleMenu(PhonActionEvent<Void> pae) {
        JPanel fontScaleMenu = new JPanel(new BorderLayout());
        fontScaleMenu.setOpaque(false);
        fontScaleMenu.setBorder(new EmptyBorder(0,8,0,8));

        // Setup font scale slider
        final JLabel smallLbl = new JLabel("A");
        smallLbl.setFont(getFont().deriveFont(FontPreferences.getDefaultFontSize()));
        smallLbl.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel largeLbl = new JLabel("A");
        largeLbl.setFont(getFont().deriveFont(FontPreferences.getDefaultFontSize()*2));
        largeLbl.setHorizontalAlignment(SwingConstants.CENTER);

        final JSlider scaleSlider = new JSlider(FONT_SIZE_DELTA_MIN, FONT_SIZE_DELTA_MAX);
        scaleSlider.setValue((int)getFontSizeDelta());
        scaleSlider.setMajorTickSpacing(8);
        scaleSlider.setMinorTickSpacing(2);
        scaleSlider.setSnapToTicks(true);
        scaleSlider.setPaintTicks(true);
        scaleSlider.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (fontSizeDelta != scaleSlider.getValue()) {
                    setFontSizeDelta(scaleSlider.getValue());
//                    transcriptEditor.getTranscriptDocument().reload();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (fontSizeDelta != scaleSlider.getValue()) {
                    setFontSizeDelta(scaleSlider.getValue());
//                    transcriptEditor.getTranscriptDocument().reload();
                }
            }
        });

        JComponent fontComp = new JPanel(new HorizontalLayout());
        fontComp.setOpaque(false);
        fontComp.add(smallLbl);
        fontComp.add(scaleSlider);
        fontComp.add(largeLbl);

        fontScaleMenu.add(fontComp, BorderLayout.CENTER);

        JButton defaultSizeButton = new JButton();
        final PhonUIAction<Void> useDefaultFontSizeAct = PhonUIAction.runnable(() -> {
            scaleSlider.setValue(0);
            if (fontSizeDelta != scaleSlider.getValue()) {
                setFontSizeDelta(0);
//                transcriptEditor.getTranscriptDocument().reload();
            }
        });
        useDefaultFontSizeAct.putValue(PhonUIAction.NAME, "Use default font size");
        useDefaultFontSizeAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Reset font size");
        defaultSizeButton.setAction(useDefaultFontSizeAct);
        fontScaleMenu.add(defaultSizeButton, BorderLayout.SOUTH);

        JComponent source = (JComponent) pae.getActionEvent().getSource();
        Point point = source.getLocationOnScreen();
        point.translate(source.getWidth() / 2, source.getHeight());

        CalloutWindow.showCallout(
            CommonModuleFrame.getCurrentFrame(),
            true,
            fontScaleMenu,
            SwingConstants.TOP,
            point
        );
    }

    /**
     * Shows the session info menu
     *
     * @param pae the event from the action that called the function
     * */
    private void showSessionInfoMenu(PhonActionEvent<Void> pae) {
        JPopupMenu menu = new JPopupMenu();
        MenuBuilder menuBuilder = new MenuBuilder(menu);
        setupSessionInformationMenu(menuBuilder);

        JComponent source = (JComponent) pae.getActionEvent().getSource();
        menu.show(
            source,
            0,
            source.getHeight()
        );
    }

    /**
     * Sets up the session info menu with the given {@link MenuBuilder}
     *
     * @param menuBuilder the builder used to build the menu
     * */
    private void setupSessionInformationMenu(MenuBuilder menuBuilder) {
        JMenuItem toggleHeadersItem = new JMenuItem();
        toggleHeadersItem.setAction(new ToggleHeadersVisibleViewAction(getEditor(), TranscriptView.this));
        menuBuilder.addItem(".", toggleHeadersItem);

        JMenuItem setDateItem = new JMenuItem();
        PhonUIAction<Void> setDateAct = PhonUIAction.runnable(() -> {
            if (getEditor().getSession().getDate() == null) {
                SessionDateEdit edit = new SessionDateEdit(getEditor(), LocalDate.now(), null);
                getEditor().getUndoSupport().postEdit(edit);
            }
            SwingUtilities.invokeLater(() -> {
                int dateHeaderEnd = getTranscriptEditor().getTranscriptDocument().getGenericEnd("Date")-1;
                if (dateHeaderEnd > -1) {
                    getTranscriptEditor().setCaretPosition(dateHeaderEnd);
                    getTranscriptEditor().requestFocus();
                }
            });
        });
        setDateAct.putValue(PhonUIAction.NAME, "Set date");
        setDateItem.setAction(setDateAct);
        menuBuilder.addItem(".", setDateItem);


        JMenu languagesMenu = menuBuilder.addMenu(".", "Languages");

        List<Language> sessionLanguages = getEditor().getSession().getLanguages();

        boolean sessionAlreadyHasLanguages = !sessionLanguages.isEmpty();
        JMenuItem addLanguageItem = new JMenuItem();
        PhonUIAction<Void> addLanguageAct = PhonUIAction.runnable(() -> {
            if (!sessionAlreadyHasLanguages) {
                getTranscriptEditor().getTranscriptDocument().putDocumentProperty("forceShowLanguageHeader", true);
                getTranscriptEditor().getTranscriptDocument().reload();
            }
            SwingUtilities.invokeLater(() -> {
                int end = getTranscriptEditor().getTranscriptDocument().getGenericEnd("Languages")-1;
                if (end > -1) {
                    getTranscriptEditor().setCaretPosition(end);
                    getTranscriptEditor().requestFocus();
                }
            });
        });
        addLanguageAct.putValue(PhonUIAction.NAME, (sessionAlreadyHasLanguages ? "Add" : "Set") + " language");
        addLanguageItem.setAction(addLanguageAct);
        languagesMenu.add(addLanguageItem);

        if (sessionAlreadyHasLanguages) {
            JMenu removeLanguageSubmenu = new JMenu("Remove language");

            for (Language language : sessionLanguages) {
                JMenuItem removeLanguageItem = new JMenuItem();
                PhonUIAction<Void> removeLanguageAct = PhonUIAction.runnable(() -> {
                    var newLangList = sessionLanguages.stream().filter(item -> item != language).toList();
                    SessionLanguageEdit edit = new SessionLanguageEdit(getEditor(), newLangList);
                    getEditor().getUndoSupport().postEdit(edit);
                    SwingUtilities.invokeLater(() -> getTranscriptEditor().getTranscriptDocument().reload());
                });
                removeLanguageAct.putValue(PhonUIAction.NAME, "Remove " + language.getPrimaryLanguage().getName());
                removeLanguageItem.setAction(removeLanguageAct);
                removeLanguageSubmenu.add(removeLanguageItem);
            }

            languagesMenu.add(removeLanguageSubmenu);
        }

        JMenuItem viewMetadataItem = new JMenuItem();
        PhonUIAction<Void> viewMetadataAct = PhonUIAction.runnable(() -> {
            MetadataDialog metadataDialog = new MetadataDialog(CommonModuleFrame.getCurrentFrame());
            metadataDialog.pack();
            metadataDialog.setVisible(true);
        });
        viewMetadataAct.putValue(PhonUIAction.NAME, "View metadata");
        viewMetadataItem.setAction(viewMetadataAct);
        menuBuilder.addItem(".", viewMetadataItem);

        JMenuItem showSessionInfoViewItem = new JMenuItem();
        PhonUIAction<Void> showSessionInfoViewAct = PhonUIAction.runnable(
            () -> getEditor().getViewModel().showView(ParticipantsView.VIEW_NAME)
        );
        showSessionInfoViewAct.putValue(PhonUIAction.NAME, "Show Session Information view");
        showSessionInfoViewItem.setAction(showSessionInfoViewAct);
        menuBuilder.addItem(".", showSessionInfoViewItem);
    }

    /**
     * Shows the media menu
     *
     * @param pae the event from the action that called the function
     * */
    private void showMediaMenu(PhonActionEvent<Void> pae) {
        JPopupMenu menu = new JPopupMenu();
        MenuBuilder menuBuilder = new MenuBuilder(menu);
        setupMediaMenu(menuBuilder);

        JComponent source = (JComponent) pae.getActionEvent().getSource();
        menu.show(
            source,
            0,
            source.getHeight()
        );
    }

    /**
     * Sets up the media menu with the given {@link MenuBuilder}
     *
     * @param menuBuilder the builder used to build the menu
     * */
    private void setupMediaMenu(MenuBuilder menuBuilder) {
        JMenuItem browseMediaItem = new JMenuItem();
        browseMediaItem.setAction(new AssignMediaAction(getEditor()));
        menuBuilder.addItem(".", browseMediaItem);

        // TODO: Wait for Greg to get this part done
        JMenuItem showMediaPlayerItem = new JMenuItem();
        PhonUIAction<Void> showMediaPlayerAct = PhonUIAction.runnable(() -> {System.out.println("Show media player");});
        showMediaPlayerAct.putValue(PhonUIAction.NAME, "Show media player");
        showMediaPlayerItem.setAction(showMediaPlayerAct);
        menuBuilder.addItem(".", showMediaPlayerItem);

        JMenuItem showTimelineViewItem = new JMenuItem();
        PhonUIAction<Void> showTimelineViewAct = PhonUIAction.runnable(() -> getEditor().getViewModel().showView(TimelineView.VIEW_NAME));
        showTimelineViewAct.putValue(PhonUIAction.NAME, "Show Timeline view");
        showTimelineViewItem.setAction(showTimelineViewAct);
        menuBuilder.addItem(".", showTimelineViewItem);

        JMenuItem showMediaPlayerViewItem = new JMenuItem();
        PhonUIAction<Void> showMediaPlayerViewAct = PhonUIAction.runnable(() -> getEditor().getViewModel().showView(MediaPlayerEditorView.VIEW_NAME));
        showMediaPlayerViewAct.putValue(PhonUIAction.NAME, "Show Media Player view");
        showMediaPlayerViewItem.setAction(showMediaPlayerViewAct);
        menuBuilder.addItem(".", showMediaPlayerViewItem);

        JMenuItem showSpeechAnalysisViewItem = new JMenuItem();
        PhonUIAction<Void> showSpeechAnalysisViewAct = PhonUIAction.runnable(() -> getEditor().getViewModel().showView(SpeechAnalysisEditorView.VIEW_NAME));
        showSpeechAnalysisViewAct.putValue(PhonUIAction.NAME, "Show Speech Analysis view");
        showSpeechAnalysisViewItem.setAction(showSpeechAnalysisViewAct);
        menuBuilder.addItem(".", showSpeechAnalysisViewItem);
    }

    /**
     * Shows the participants menu
     *
     * @param pae the event from the action that called the function
     * */
    private void showParticipantsMenu(PhonActionEvent<Void> pae) {
        JPopupMenu menu = new JPopupMenu();
        MenuBuilder menuBuilder = new MenuBuilder(menu);
        setupParticipantsMenu(menuBuilder);

        JComponent source = (JComponent) pae.getActionEvent().getSource();
        menu.show(
            source,
            0,
            source.getHeight()
        );
    }

    /**
     * Sets up the participants menu with the given {@link MenuBuilder}
     *
     * @param menuBuilder the builder used to build the menu
     * */
    public void setupParticipantsMenu(MenuBuilder menuBuilder) {
        JMenuItem addParticipantItem = new JMenuItem();
        addParticipantItem.setAction(new NewParticipantAction(getEditor()));
        menuBuilder.addItem(".", addParticipantItem);

        menuBuilder.addSeparator(".", "");

        for (Participant participant : getEditor().getSession().getParticipants()) {
            JMenu participantSubmenu = menuBuilder.addMenu(".", participant.toString());

            JMenuItem editParticipantItem = new JMenuItem();
            editParticipantItem.setAction(new EditParticipantAction(getEditor(), participant));
            participantSubmenu.add(editParticipantItem);

            JMenuItem removeParticipantItem = new JMenuItem();
            removeParticipantItem.setAction(new DeleteParticipantAction(getEditor(), participant));
            participantSubmenu.add(removeParticipantItem);
        }
    }

    /**
     * Shows the transcript menu
     *
     * @param pae the event from the action that called the function
     * */
    private void showTranscriptMenu(PhonActionEvent<Void> pae) {
        JPopupMenu menu = new JPopupMenu();
        MenuBuilder menuBuilder = new MenuBuilder(menu);
        setupTranscriptMenu(menuBuilder);

        JComponent source = (JComponent) pae.getActionEvent().getSource();
        menu.show(
            source,
            0,
            source.getHeight()
        );
    }

    /**
     * Sets up the transcript menu with the given {@link MenuBuilder}
     *
     * @param menuBuilder the builder used to build the menu
     * */
    private void setupTranscriptMenu(MenuBuilder menuBuilder) {

        TranscriptElementLocation transcriptLocation = getTranscriptEditor().getCurrentSessionLocation();
        int currentTranscriptElementIndex = transcriptLocation == null ? -1 : transcriptLocation.transcriptElementIndex();
        boolean inHeaders = currentTranscriptElementIndex < 0;


        if (!inHeaders) {
            JMenuItem insertRecordAboveItem = new JMenuItem();
            PhonUIAction<Void> insertRecordAboveAct = PhonUIAction.runnable(() -> {
                final AddRecordEdit edit = new AddRecordEdit(getEditor(), SessionFactory.newFactory().createRecord(), currentTranscriptElementIndex);
                getEditor().getUndoSupport().postEdit(edit);
            });
            insertRecordAboveAct.putValue(PhonUIAction.NAME, "Insert record above");
            insertRecordAboveItem.setAction(insertRecordAboveAct);
            menuBuilder.addItem(".", insertRecordAboveItem);
        }

        JMenuItem insertRecordBelowItem = new JMenuItem();
        PhonUIAction<Void> insertRecordBelowAct = PhonUIAction.runnable(() -> {
            Transcript transcript = getEditor().getSession().getTranscript();
            final AddRecordEdit edit = new AddRecordEdit(getEditor(), SessionFactory.newFactory().createRecord(), currentTranscriptElementIndex+1);
            getEditor().getUndoSupport().postEdit(edit);
        });
        insertRecordBelowAct.putValue(PhonUIAction.NAME, "Insert record below");
        insertRecordBelowItem.setAction(insertRecordBelowAct);
        menuBuilder.addItem(".", insertRecordBelowItem);

        menuBuilder.addSeparator(".", "");

        if (!inHeaders) {
            JMenuItem insertCommentAboveItem = new JMenuItem();
            PhonUIAction<Void> insertCommentAboveAct = PhonUIAction.runnable(() -> {
                final AddTranscriptElementEdit edit = new AddTranscriptElementEdit(
                    getEditor().getSession(),
                    getEditor().getEventManager(),
                    new Transcript.Element(SessionFactory.newFactory().createComment()),
                    currentTranscriptElementIndex
                );
                getEditor().getUndoSupport().postEdit(edit);
            });
            insertCommentAboveAct.putValue(PhonUIAction.NAME, "Insert comment above");
            insertCommentAboveItem.setAction(insertCommentAboveAct);
            menuBuilder.addItem(".", insertCommentAboveItem);
        }

        JMenuItem insertCommentBelowItem = new JMenuItem();
        PhonUIAction<Void> insertCommentBelowAct = PhonUIAction.runnable(() -> {
            final AddTranscriptElementEdit edit = new AddTranscriptElementEdit(
                getEditor().getSession(),
                getEditor().getEventManager(),
                new Transcript.Element(SessionFactory.newFactory().createComment()),
                currentTranscriptElementIndex+1
            );
            getEditor().getUndoSupport().postEdit(edit);
        });
        insertCommentBelowAct.putValue(PhonUIAction.NAME, "Insert comment below");
        insertCommentBelowItem.setAction(insertCommentBelowAct);
        menuBuilder.addItem(".", insertCommentBelowItem);

        menuBuilder.addSeparator(".", "");

        if (!inHeaders) {
            JMenuItem insertGemAboveItem = new JMenuItem();
            PhonUIAction<Void> insertGemAboveAct = PhonUIAction.runnable(() -> {
                final AddTranscriptElementEdit edit = new AddTranscriptElementEdit(
                    getEditor().getSession(),
                    getEditor().getEventManager(),
                    new Transcript.Element(SessionFactory.newFactory().createGem()),
                    currentTranscriptElementIndex
                );
                getEditor().getUndoSupport().postEdit(edit);
            });
            insertGemAboveAct.putValue(PhonUIAction.NAME, "Insert gem above");
            insertGemAboveItem.setAction(insertGemAboveAct);
            menuBuilder.addItem(".", insertGemAboveItem);
        }

        JMenuItem insertGemBelowItem = new JMenuItem();
        PhonUIAction<Void> insertGemBelowAct = PhonUIAction.runnable(() -> {
            final AddTranscriptElementEdit edit = new AddTranscriptElementEdit(
                getEditor().getSession(),
                getEditor().getEventManager(),
                new Transcript.Element(SessionFactory.newFactory().createGem()),
                currentTranscriptElementIndex+1
            );
            getEditor().getUndoSupport().postEdit(edit);
        });
        insertGemBelowAct.putValue(PhonUIAction.NAME, "Insert gem below");
        insertGemBelowItem.setAction(insertGemBelowAct);
        menuBuilder.addItem(".", insertGemBelowItem);


        menuBuilder.addSeparator(".", "");


        JMenuItem autoTranscribeItem = new JMenuItem();
        autoTranscribeItem.setAction(new AutoTranscribeAction(
            getEditor().getProject(),
            getEditor().getSession(),
            getEditor().getEventManager(),
            getEditor().getUndoSupport(),
            getEditor().getDataModel().getTranscriber()
        ));
        menuBuilder.addItem(".", autoTranscribeItem);
    }

    /**
     * Shows the tiers menu
     *
     * @param pae the event from the action that called the function
     * */
    private void showTiersMenu(PhonActionEvent<Void> pae) {
        JPopupMenu menu = new JPopupMenu();
        MenuBuilder menuBuilder = new MenuBuilder(menu);
        setupTiersMenu(menuBuilder);

        JComponent source = (JComponent) pae.getActionEvent().getSource();
        menu.show(
            source,
            0,
            source.getHeight()
        );
    }

    /**
     * Sets up the tiers menu with the given {@link MenuBuilder}
     *
     * @param menuBuilder the builder used to build the menu
     * */
    public void setupTiersMenu(MenuBuilder menuBuilder) {
        JMenu addTierMenu = menuBuilder.addMenu(".", "Add tier");
        TierMenuBuilder.setupNewTierMenu(getEditor(), new MenuBuilder(addTierMenu));

        menuBuilder.addSeparator(".", "");

        TierMenuBuilder.appendExistingTiersMenu(getEditor(), menuBuilder);

        menuBuilder.addSeparator(".", "existing_tiers");

        JMenuItem toggleSyllabificationVisibleItem = new JMenuItem();
        toggleSyllabificationVisibleItem.setAction(new ToggleSyllabificationVisibleViewAction(getEditor(), TranscriptView.this));
        menuBuilder.addItem(".", toggleSyllabificationVisibleItem);

//        JMenuItem toggleSyllabificationComponentItem = new JMenuItem();
//        toggleSyllabificationComponentItem.setAction(new ToggleSyllabificationIsComponent(getEditor(), TranscriptView.this));
//        menuBuilder.addItem(".", toggleSyllabificationComponentItem);

        JMenuItem toggleAlignmentVisibleItem = new JMenuItem();
        toggleAlignmentVisibleItem.setAction(new ToggleAlignmentVisibleViewAction(getEditor(), TranscriptView.this));
        menuBuilder.addItem(".", toggleAlignmentVisibleItem);

//        JMenuItem toggleAlignmentComponentItem = new JMenuItem();
//        toggleAlignmentComponentItem.setAction(new ToggleAlignmentIsComponentViewAction(getEditor(), TranscriptView.this));
//        menuBuilder.addItem(".", toggleAlignmentComponentItem);

        menuBuilder.addSeparator(".", "");

        JMenuItem toggleBlindTiersItem = new JMenuItem();
        toggleBlindTiersItem.setAction(new ToggleValidationModeViewAction(getEditor(), TranscriptView.this));
        menuBuilder.addItem(".", toggleBlindTiersItem);

        menuBuilder.addSeparator(".", "blind_transcription");

        JMenuItem toggleHeadersItem = new JMenuItem();
        toggleHeadersItem.setAction(new ToggleHeadersVisibleViewAction(getEditor(), TranscriptView.this));
        menuBuilder.addItem(".", toggleHeadersItem);

        JMenuItem toggleChatTierNamesItem = new JMenuItem();
        PhonUIAction<Void> toggleChatTierNamesAct = PhonUIAction.runnable(this::toggleChatTierNamesShown);
        toggleChatTierNamesAct.putValue(PhonUIAction.NAME, "Toggle chat tier names");
        toggleChatTierNamesItem.setAction(toggleChatTierNamesAct);
        menuBuilder.addItem(".", toggleChatTierNamesItem);
    }

    /**
     * Show records menu
     *
     */
    private void showRecordsMenu(PhonActionEvent<Void> pae) {
        JPopupMenu menu = new JPopupMenu();
        MenuBuilder menuBuilder = new MenuBuilder(menu);
        setupRecordMenu(menuBuilder);

        JComponent source = (JComponent) pae.getActionEvent().getSource();
        menu.show(
            source,
            0,
            source.getHeight()
        );
    }

    /**
     * Setup record menu
     *
     * @param menuBuilder
     */
    private void setupRecordMenu(MenuBuilder menuBuilder) {
        TranscriptElementLocation transcriptLocation = getTranscriptEditor().getCurrentSessionLocation();
        final int transcriptElementIndex = transcriptLocation == null ? -2 : transcriptLocation.transcriptElementIndex();
        final int currentTranscriptElementIndex = transcriptElementIndex == -2 ?
                (isSingleRecordView() ? getEditor().getSession().getRecordElementIndex(getEditor().getCurrentRecordIndex()) : -2)
                : transcriptElementIndex;
        boolean inHeaders = currentTranscriptElementIndex == -1;

        JMenuItem insertRecordBelowItem = new JMenuItem();
        PhonUIAction<Void> insertRecordBelowAct = PhonUIAction.runnable(() -> {
            final AddRecordEdit edit = new AddRecordEdit(getEditor(), SessionFactory.newFactory().createRecord(), currentTranscriptElementIndex+1);
            getEditor().getUndoSupport().postEdit(edit);
        });
        insertRecordBelowAct.putValue(PhonUIAction.NAME, "Insert record");
        insertRecordBelowItem.setAction(insertRecordBelowAct);
        menuBuilder.addItem(".", insertRecordBelowItem);

        JMenuItem insertRecordAboveItem = new JMenuItem();
        PhonUIAction<Void> insertRecordAboveAct = PhonUIAction.runnable(() -> {
            final AddRecordEdit edit = new AddRecordEdit(getEditor(), SessionFactory.newFactory().createRecord(), currentTranscriptElementIndex);
            getEditor().getUndoSupport().postEdit(edit);
        });
        insertRecordAboveAct.putValue(PhonUIAction.NAME, "Insert record above");
        insertRecordAboveItem.setAction(insertRecordAboveAct);
        insertRecordAboveItem.setEnabled(!inHeaders);
        menuBuilder.addItem(".", insertRecordAboveItem);

        menuBuilder.addSeparator(".", "add_record");

        menuBuilder.addItem(".", new DuplicateRecordAction(getEditor()));
        menuBuilder.addItem(".", new DeleteRecordAction(getEditor()));
        menuBuilder.addSeparator(".", "edit_record");

        menuBuilder.addItem(".", new MoveRecordToBeginningAction(getEditor()));
        menuBuilder.addItem(".", new MoveRecordBackwardAction(getEditor()));
        menuBuilder.addItem(".", new MoveRecordForwardAction(getEditor()));
        menuBuilder.addItem(".", new MoveRecordToEndAction(getEditor()));
        menuBuilder.addItem(".", new SortRecordsAction(getEditor()));
        menuBuilder.addSeparator(".", "move_record");

        menuBuilder.addItem(".", new CutRecordAction(getEditor()));
        menuBuilder.addItem(".", new CopyRecordAction(getEditor()));
        menuBuilder.addItem(".", new PasteRecordAction(getEditor()));
        menuBuilder.addSeparator(".", "copy_paste_record");

        menuBuilder.addItem(".", new FirstRecordAction(getEditor()));
        menuBuilder.addItem(".", new PreviousRecordAction(getEditor()));
        menuBuilder.addItem(".", new NextRecordAction(getEditor()));
        menuBuilder.addItem(".", new LastRecordAction(getEditor()));

        menuBuilder.addSeparator(".", "navigation");

        final PhonUIAction<Void> toggleSingleRecordModeAct = PhonUIAction.runnable(this::toggleSingleRecordView);
        toggleSingleRecordModeAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Toggle single record view");
        toggleSingleRecordModeAct.putValue(PhonUIAction.NAME,
                isSingleRecordView() ? "Show full transcript" : "Show one record at a time");
        menuBuilder.addItem(".", toggleSingleRecordModeAct);
    }

    private void toggleSingleRecordView() {
        transcriptEditor.getTranscriptDocument().setSingleRecordIndex(getEditor().getCurrentRecordIndex());
        transcriptEditor.toggleSingleRecordView();
    }

    /**
     * Show comments menu
     */
    private void showCommentsMenu(PhonActionEvent<Void> pae) {
        JPopupMenu menu = new JPopupMenu();
        MenuBuilder menuBuilder = new MenuBuilder(menu);
        setupCommentsMenu(menuBuilder);

        JComponent source = (JComponent) pae.getActionEvent().getSource();
        menu.show(
            source,
            0,
            source.getHeight()
        );
    }

    /**
     * Setup comments menu
     *
     * @param menuBuilder
     */
    private void setupCommentsMenu(MenuBuilder menuBuilder) {
        TranscriptElementLocation transcriptLocation = getTranscriptEditor().getCurrentSessionLocation();
        final int transcriptElementIndex = transcriptLocation == null ? -2 : transcriptLocation.transcriptElementIndex();
        final int currentTranscriptElementIndex = transcriptElementIndex == -2 ?
                (isSingleRecordView() ? getEditor().getSession().getRecordElementIndex(getEditor().getCurrentRecordIndex()) : -2)
                : transcriptElementIndex;
        boolean inHeaders = currentTranscriptElementIndex == -1;

        JMenuItem insertCommentBelowItem = new JMenuItem();
        PhonUIAction<Void> insertCommentBelowAct = PhonUIAction.runnable(() -> {
            final AddTranscriptElementEdit edit = new AddTranscriptElementEdit(
                    getEditor().getSession(),
                    getEditor().getEventManager(),
                    new Transcript.Element(SessionFactory.newFactory().createComment()),
                    currentTranscriptElementIndex+1
            );
            getEditor().getUndoSupport().postEdit(edit);
        });
        insertCommentBelowAct.putValue(PhonUIAction.NAME, "Insert comment");
        insertCommentBelowItem.setAction(insertCommentBelowAct);
        menuBuilder.addItem(".", insertCommentBelowItem);

        JMenuItem insertCommentAboveItem = new JMenuItem();
        PhonUIAction<Void> insertCommentAboveAct = PhonUIAction.runnable(() -> {
            final AddTranscriptElementEdit edit = new AddTranscriptElementEdit(
                    getEditor().getSession(),
                    getEditor().getEventManager(),
                    new Transcript.Element(SessionFactory.newFactory().createComment()),
                    currentTranscriptElementIndex
            );
            getEditor().getUndoSupport().postEdit(edit);
        });
        insertCommentAboveAct.putValue(PhonUIAction.NAME, "Insert comment above");
        insertCommentAboveItem.setAction(insertCommentAboveAct);
        insertCommentAboveItem.setEnabled(!inHeaders);
        menuBuilder.addItem(".", insertCommentAboveItem);


        menuBuilder.addSeparator(".", "add_comment");

        // insert goto items for existing comments
        for(int i = 0; i < getEditor().getSession().getTranscript().getNumberOfElements(); i++) {
            final Transcript.Element element = getEditor().getSession().getTranscript().getElementAt(i);
            if(element.isComment()) {
                final Comment comment = element.asComment();
                final PhonUIAction<Void> gotoAct = PhonUIAction.runnable(() -> {
                    final TranscriptDocument.StartEnd commentRange =
                            getTranscriptEditor().getTranscriptDocument().getCommentContentStartEnd(comment);
                    if(commentRange.valid()) {
                        getTranscriptEditor().setCaretPosition(commentRange.start());
                        getTranscriptEditor().requestFocus();
                    }
                });
                gotoAct.putValue(PhonUIAction.NAME,  String.format("%s: %s", comment.getType(), comment.getValue().toString()));
                gotoAct.putValue(PhonUIAction.SMALL_ICON, IconManager.getInstance()
                        .getFontIcon(IconManager.GoogleMaterialDesignIconsFontName,
                                "comment", IconSize.SMALL, UIManager.getColor("textText")));
                final JMenuItem gotoItem = new JMenuItem(gotoAct);
                menuBuilder.addItem(".", gotoItem);
            }
        }
    }

    /**
     * Show gems menu
     *
     */
    private void showGemsMenu(PhonActionEvent<Void> pae) {
        JPopupMenu menu = new JPopupMenu();
        MenuBuilder menuBuilder = new MenuBuilder(menu);
        setupGemsMenu(menuBuilder);

        JComponent source = (JComponent) pae.getActionEvent().getSource();
        menu.show(
                source,
                0,
                source.getHeight()
        );
    }

    /**
     * Setup Gems menu
     *
     * @param menuBuilder
     */
    private void setupGemsMenu(MenuBuilder menuBuilder) {
        TranscriptElementLocation transcriptLocation = getTranscriptEditor().getCurrentSessionLocation();
        final int transcriptElementIndex = transcriptLocation == null ? -2 : transcriptLocation.transcriptElementIndex();
        final int currentTranscriptElementIndex = transcriptElementIndex == -2 ?
                (isSingleRecordView() ? getEditor().getSession().getRecordElementIndex(getEditor().getCurrentRecordIndex()) : -2)
                : transcriptElementIndex;
        boolean inHeaders = currentTranscriptElementIndex == -1;

        JMenuItem insertGemBelowItem = new JMenuItem();
        PhonUIAction<Void> insertGemBelowAct = PhonUIAction.runnable(() -> {
            final AddTranscriptElementEdit edit = new AddTranscriptElementEdit(
                    getEditor().getSession(),
                    getEditor().getEventManager(),
                    new Transcript.Element(SessionFactory.newFactory().createGem()),
                    currentTranscriptElementIndex+1
            );
            getEditor().getUndoSupport().postEdit(edit);
        });
        insertGemBelowAct.putValue(PhonUIAction.NAME, "Insert gem");
        insertGemBelowItem.setAction(insertGemBelowAct);
        menuBuilder.addItem(".", insertGemBelowItem);

        JMenuItem insertGemAboveItem = new JMenuItem();
        PhonUIAction<Void> insertGemAboveAct = PhonUIAction.runnable(() -> {
            final AddTranscriptElementEdit edit = new AddTranscriptElementEdit(
                    getEditor().getSession(),
                    getEditor().getEventManager(),
                    new Transcript.Element(SessionFactory.newFactory().createGem()),
                    currentTranscriptElementIndex
            );
            getEditor().getUndoSupport().postEdit(edit);
        });
        insertGemAboveAct.putValue(PhonUIAction.NAME, "Insert gem above");
        insertGemAboveItem.setAction(insertGemAboveAct);
        insertGemAboveItem.setEnabled(!inHeaders);
        menuBuilder.addItem(".", insertGemAboveItem);


        menuBuilder.addSeparator(".", "add_gem");

        // insert goto items for existing gems
        for(int i = 0; i < getEditor().getSession().getTranscript().getNumberOfElements(); i++) {
            final Transcript.Element element = getEditor().getSession().getTranscript().getElementAt(i);
            if(element.isGem()) {
                final Gem gem = element.asGem();
                final PhonUIAction<Void> gotoAct = PhonUIAction.runnable(() -> {
                    final TranscriptDocument.StartEnd gemRange =
                            getTranscriptEditor().getTranscriptDocument().getGemContentStartEnd(gem);
                    if(gemRange.valid()) {
                        getTranscriptEditor().setCaretPosition(gemRange.start());
                        getTranscriptEditor().requestFocus();
                    }
                });
                gotoAct.putValue(PhonUIAction.NAME,  String.format("%s: %s", gem.getType(), gem.getLabel()));
                gotoAct.putValue(PhonUIAction.SMALL_ICON, IconManager.getInstance()
                        .getFontIcon(IconManager.GoogleMaterialDesignIconsFontName,
                                "diamond", IconSize.SMALL, UIManager.getColor("textText")));
                final JMenuItem gotoItem = new JMenuItem(gotoAct);
                menuBuilder.addItem(".", gotoItem);
            }
        }
    }

    // region Getters and Setters

    @Override
    public String getName() {
        return VIEW_NAME;
    }

    @Override
    public ImageIcon getIcon() {
        final String[] iconData = VIEW_ICON.split(":");
        return IconManager.getInstance().getFontIcon(iconData[0], iconData[1], IconSize.MEDIUM, Color.darkGray);
    }

    @Override
    public JMenu getMenu() {
        final JMenu retVal = new JMenu();

        MenuBuilder menuBuilder = new MenuBuilder(retVal);

        setupParticipantsMenu(new MenuBuilder(menuBuilder.addMenu(".", "Participants")));
        setupTiersMenu(new MenuBuilder(menuBuilder.addMenu(".", "Tiers")));
        setupRecordMenu(new MenuBuilder(menuBuilder.addMenu(".", "Records")));
        setupCommentsMenu(new MenuBuilder(menuBuilder.addMenu(".", "Comments")));
        setupGemsMenu(new MenuBuilder(menuBuilder.addMenu(".", "Gems")));

        menuBuilder.addSeparator(".", "session_menus");

        JMenuItem viewMetadataItem = new JMenuItem();
        PhonUIAction<Void> viewMetadataAct = PhonUIAction.runnable(() -> {
            MetadataDialog metadataDialog = new MetadataDialog(CommonModuleFrame.getCurrentFrame());
            metadataDialog.pack();
            metadataDialog.setLocationRelativeTo(CommonModuleFrame.getCurrentFrame());
            metadataDialog.setVisible(true);
        });
        viewMetadataAct.putValue(PhonUIAction.NAME, "View metadata");
        viewMetadataItem.setAction(viewMetadataAct);
        menuBuilder.addItem(".", viewMetadataItem);

        if(PrefHelper.isDebugMode()) {
            final PhonUIAction<Void> showTranscriptTreeAct = PhonUIAction.runnable(this::onToggleTranscriptTree);
            showTranscriptTreeAct.putValue(PhonUIAction.NAME, "Toggle transcript tree");
            menuBuilder.addItem(".", showTranscriptTreeAct);
        }

        return retVal;
    }

    private void onToggleTranscriptTree() {
        if (transcriptTree == null) {
            final TranscriptDocumentTreeModel treeModel = new TranscriptDocumentTreeModel(getTranscriptEditor().getTranscriptDocument());
            transcriptTree = new JXTree(treeModel);
            transcriptTree.setRootVisible(false);
        }
        if(transcriptTreePanel == null) {
            transcriptTreePanel = new JPanel(new BorderLayout());
            IconStrip iconStrip = new IconStrip();
            transcriptTreePanel.add(iconStrip, BorderLayout.NORTH);
            final PhonUIAction rebuildTreeAct = PhonUIAction.runnable(() -> {
                transcriptTree.setModel(new TranscriptDocumentTreeModel(getTranscriptEditor().getTranscriptDocument()));
                SwingUtilities.invokeLater(() -> {
                    transcriptTree.expandRow(0);
                });
            });
            rebuildTreeAct.putValue(PhonUIAction.NAME, "Rebuild tree");
            rebuildTreeAct.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
            rebuildTreeAct.putValue(FlatButton.ICON_NAME_PROP, "refresh");
            rebuildTreeAct.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);
            iconStrip.add(rebuildTreeAct, IconStrip.IconStripPosition.LEFT);

            transcriptTreePanel.add(new JScrollPane(transcriptTree), BorderLayout.CENTER);
            transcriptTreePanel.setPreferredSize(new Dimension(500, 0));
            add(transcriptTreePanel, BorderLayout.EAST);
            revalidate();

            SwingUtilities.invokeLater(() -> {
                transcriptTree.expandRow(0);
            });
        } else {
            if(transcriptTreePanel.isVisible()) {
                transcriptTreePanel.setVisible(false);
            } else {
                transcriptTreePanel.setVisible(true);
            }
        }
    }

    private void onEditorFinishedLoading(EditorEvent<Void> event) {
        transcriptEditor.loadSession();
    }

    public boolean isSingleRecordView() {
        return transcriptEditor.isSingleRecordView();
    }

    public void toggleSingleRecordActive() {
        transcriptEditor.toggleSingleRecordView();
    }

    public boolean getShowRecordNumbers() {
        return transcriptScrollPane.getGutter().getShowRecordNumbers();
    }

    public void toggleShowRecordNumbers() {
        transcriptScrollPane.getGutter().setShowRecordNumbers(!getShowRecordNumbers());
    }

    public boolean isSyllabificationVisible() {
        return (boolean) transcriptEditor.getTranscriptDocument().getDocumentPropertyOrDefault(
            SyllabificationExtension.SYLLABIFICATION_IS_VISIBLE,
            SyllabificationExtension.SYLLABIFICATION_IS_VISIBLE_DEFAULT
        );
    }

    public void toggleSyllabificationVisible() {
        transcriptEditor.getTranscriptDocument().putDocumentProperty(
            SyllabificationExtension.SYLLABIFICATION_IS_VISIBLE,
            !isSyllabificationVisible()
        );
    }

    public boolean isSyllabificationComponent() {
        return (boolean) transcriptEditor.getTranscriptDocument().getDocumentPropertyOrDefault(
            SyllabificationExtension.SYLLABIFICATION_IS_COMPONENT,
            SyllabificationExtension.SYLLABIFICATION_IS_COMPONENT_DEFAULT
        );
    }

    public void toggleSyllabificationIsComponent() {
        transcriptEditor.getTranscriptDocument().putDocumentProperty(
            SyllabificationExtension.SYLLABIFICATION_IS_COMPONENT,
            !isSyllabificationComponent()
        );
    }

    public boolean isAlignmentVisible() {
        return (boolean) transcriptEditor.getTranscriptDocument().getDocumentPropertyOrDefault(
            AlignmentExtension.ALIGNMENT_IS_VISIBLE,
            AlignmentExtension.ALIGNMENT_IS_VISIBLE_DEFAULT
        );
    }

    public void toggleAlignmentVisible() {
        transcriptEditor.getTranscriptDocument().putDocumentProperty(
            AlignmentExtension.ALIGNMENT_IS_VISIBLE,
            !isAlignmentVisible()
        );
    }

    public boolean isAlignmentComponent() {
        return (boolean) transcriptEditor.getTranscriptDocument().getDocumentPropertyOrDefault(
            AlignmentExtension.ALIGNMENT_IS_COMPONENT,
            AlignmentExtension.ALIGNMENT_IS_COMPONENT_DEFAULT
        );
    }

    public void toggleAlignmentIsComponent() {
        transcriptEditor.getTranscriptDocument().putDocumentProperty(
            AlignmentExtension.ALIGNMENT_IS_COMPONENT,
            !isAlignmentComponent()
        );
    }

    public float getFontSizeDelta() {
        return fontSizeDelta;
    }

    public void setFontSizeDelta(float fontSizeDelta) {
        float oldVal = this.fontSizeDelta;
        this.fontSizeDelta = fontSizeDelta;
        firePropertyChange("fontSizeDelta", oldVal, fontSizeDelta);
    }

    public boolean isFindAndReplaceVisible() {
        return findAndReplaceVisible;
    }

    public FindAndReplacePanel getFindAndReplacePanel() {
        if (findAndReplacePanel == null) {
            this.findAndReplacePanel = new FindAndReplacePanel(
                getEditor().getDataModel(),
                getEditor().getSelectionModel(),
                getEditor().getEventManager(),
                getEditor().getViewModel(),
                getEditor().getUndoSupport()
            );
            final PhonUIAction<Void> closeFindAct = PhonUIAction.runnable(() -> setFindAndReplaceVisible(false, false));
            closeFindAct.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
            closeFindAct.putValue(FlatButton.ICON_NAME_PROP, "close");
            closeFindAct.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);
            closeFindAct.putValue(Action.SHORT_DESCRIPTION, "Close find and replace");
            final FlatButton closeButton = new FlatButton(closeFindAct);

            final KeyStroke escapeKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
            final InputMap inputMap = closeButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            final ActionMap actionMap = closeButton.getActionMap();
            inputMap.put(escapeKey, "close");
            actionMap.put("close", closeFindAct);

            final KeyStroke showReplaceKs = KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
            final PhonUIAction<Void> showReplaceAct = PhonUIAction.runnable(this::showReplace);
            showReplaceAct.putValue(PhonUIAction.NAME, "Show replace");
            showReplaceAct.putValue(PhonUIAction.ACCELERATOR_KEY, showReplaceKs);
            findAndReplacePanel.getActionMap().put("showReplace", showReplaceAct);
            findAndReplacePanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(showReplaceKs, "showReplace");

            final KeyStroke findNextKs = KeyStroke.getKeyStroke(KeyEvent.VK_G, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
            final PhonUIAction<Void> findNextAct = PhonUIAction.runnable(() -> getFindAndReplacePanel().findNext());
            findNextAct.putValue(PhonUIAction.NAME, "Find next");
            findNextAct.putValue(PhonUIAction.ACCELERATOR_KEY, findNextKs);
            findAndReplacePanel.getActionMap().put("findNext", findNextAct);
            findAndReplacePanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(findNextKs, "findNext");

            final KeyStroke findPrevKs = KeyStroke.getKeyStroke(KeyEvent.VK_G, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | KeyEvent.SHIFT_DOWN_MASK);
            final PhonUIAction<Void> findPrevAct = PhonUIAction.runnable(() -> getFindAndReplacePanel().findPrev());
            findPrevAct.putValue(PhonUIAction.NAME, "Find previous");
            findPrevAct.putValue(PhonUIAction.ACCELERATOR_KEY, findPrevKs);
            findAndReplacePanel.getActionMap().put("findPrevious", findPrevAct);
            findAndReplacePanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(findPrevKs, "findPrevious");

            final IconStrip rightStrip = new IconStrip(SwingConstants.VERTICAL);
            rightStrip.add(closeButton, IconStrip.IconStripPosition.LEFT);

            this.findAndReplacePanel.add(rightStrip, BorderLayout.EAST);
        }
        return findAndReplacePanel;
    }

    public void setFindAndReplaceVisible(boolean findAndReplaceVisible, boolean replaceVisible) {
        var wasFindAndReplaceVisible = this.findAndReplaceVisible;
        this.findAndReplaceVisible = findAndReplaceVisible;
        if (findAndReplaceVisible) {
            getFindAndReplacePanel().setReplaceOptionsVisible(replaceVisible);
            if(!wasFindAndReplaceVisible)
                centerPanel.add(getFindAndReplacePanel(), BorderLayout.NORTH);
            if(replaceVisible)
                getFindAndReplacePanel().getReplaceField().getTextField().requestFocusInWindow();
            else
                SwingUtilities.invokeLater(() -> getFindAndReplacePanel().getSearchField().getTextField().requestFocusInWindow());
        } else {
            centerPanel.remove(getFindAndReplacePanel());
            findAndReplacePanel.clearResults();
        }
        firePropertyChange("findAndReplaceVisible", wasFindAndReplaceVisible, findAndReplaceVisible);
        revalidate();
        repaint();
    }

    public void showFind() {
        if(!isFindAndReplaceVisible()) {
            setFindAndReplaceVisible(true, false);
        } else {
            getFindAndReplacePanel().getSearchField().getTextField().requestFocusInWindow();
        }
    }

    public void showReplace() {
        if(!isFindAndReplaceVisible()) {
            setFindAndReplaceVisible(true, true);
        } else {
            getFindAndReplacePanel().setReplaceOptionsVisible(true);
            getFindAndReplacePanel().getReplaceField().getTextField().requestFocusInWindow();
        }
    }

    public boolean isValidationMode() {
        return (boolean) transcriptEditor.getTranscriptDocument().getDocumentPropertyOrDefault(
            BlindTranscriptionExtension.VALIDATION_MODE,
            BlindTranscriptionExtension.VALIDATION_MODE_DEFAULT
        );
    }

    public void toggleValidationMode() {
        transcriptEditor.getTranscriptDocument().putDocumentProperty(
            BlindTranscriptionExtension.VALIDATION_MODE,
            !isValidationMode()
        );
    }

    public boolean isHeadersVisible() {
        return (boolean) transcriptEditor.getTranscriptDocument().getDocumentPropertyOrDefault(
            HeaderTierExtension.HEADERS_VISIBLE,
            HeaderTierExtension.DEFAULT_HEADERS_VISIBLE
        );
    }

    public void toggleHeadersVisible() {
        transcriptEditor.getTranscriptDocument().putDocumentProperty(
            HeaderTierExtension.HEADERS_VISIBLE,
            !isHeadersVisible()
        );
    }

    public boolean isChatTierNamesShown() {
        return transcriptEditor.getTranscriptDocument().isChatTierNamesShown();
    }

    public void toggleChatTierNamesShown() {
        transcriptEditor.getTranscriptDocument().setChatTierNamesShown(!isChatTierNamesShown());
    }

    //endregion Getters and Setters

    /**
     * The dialog that shows the metadata for the session
     * */
    private class MetadataDialog extends JDialog {
        private final Map<String, String> data;
        private JXTable metadataTable;
        private DefaultTableModel metadataTableModel;

        /**
         * Constructor
         *
         * @param owner the window that this dialog was opened from
         * */
        public MetadataDialog(Frame owner) {
            super(owner, "Metadata");
            this.data = new HashMap<>();
            this.data.putAll(getTranscriptEditor().getSession().getMetadata());

            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {

                    if (!isMetadataChanged()) {
                        dispose();
                        return;
                    }

                    MessageDialogProperties props = new MessageDialogProperties();
                    props.setParentWindow(CommonModuleFrame.getCurrentFrame());
                    props.setHeader("Unsaved changes");
                    props.setOptions(MessageDialogProperties.yesNoCancelOptions);
                    props.setMessage("Do you want to save your changes?");
                    props.setRunAsync(true);
                    props.setListener(nativeDialogEvent -> {
                        int result = nativeDialogEvent.getDialogResult();
                        switch (result) {
                            case NativeDialogEvent.YES_OPTION -> {
                                saveChanges();
                                dispose();
                            }
                            case NativeDialogEvent.NO_OPTION -> dispose();
                        }
                    });

                    NativeDialogs.showMessageDialog(props);
                }
            });

            init();
        }

        /**
         * Sets up the UI
         * */
        private void init() {
            setLayout(new BorderLayout());

            // region Button Bar

            JButton okButton = new JButton();
            PhonUIAction<Void> okAction = PhonUIAction.runnable(this::saveChanges);
            okAction.putValue(PhonUIAction.NAME, "Ok");
            okButton.setAction(okAction);

            JButton cancelButton = new JButton();
            PhonUIAction<Void> cancelAction = PhonUIAction.runnable(this::dispose);
            cancelAction.putValue(PhonUIAction.NAME, "Cancel");
            cancelButton.setAction(cancelAction);

            JButton addRowButton = new JButton();
            PhonUIAction<Void> addRowAction = PhonUIAction.runnable(this::addRow);
            addRowAction.putValue(PhonUIAction.NAME, "Add row");
            addRowButton.setAction(addRowAction);

            JButton removeRowButton = new JButton();
            PhonUIAction<Void> removeRowAction = PhonUIAction.runnable(this::removeRow);
            removeRowAction.putValue(PhonUIAction.NAME, "Remove row");
            removeRowButton.setAction(removeRowAction);
            removeRowButton.setEnabled(false);

            add(ButtonBarBuilder.buildOkCancelBar(okButton, cancelButton, addRowButton, removeRowButton), BorderLayout.SOUTH);

            // endregion Button Bar

            // region Table

            String[] columnNames = { "Key", "Value" };

            List<String[]> metadataList = new ArrayList<>();
            for (var pair : data.entrySet()) {
                metadataList.add(new String[]{pair.getKey(), pair.getValue()});
            }
            metadataList.sort(Comparator.comparing(array -> array[0]));

            metadataTableModel = new DefaultTableModel(metadataList.toArray(String[][]::new), columnNames);
            metadataTable = new JXTable(metadataTableModel);
            metadataTable.setGridColor(Color.BLACK);
            add(new JScrollPane(metadataTable), BorderLayout.CENTER);

            metadataTable.getSelectionModel().addListSelectionListener(e ->
                removeRowButton.setEnabled(metadataTable.getSelectedRow() != -1)
            );

            // endregion Table
        }

        /**
         * Adds an empty row to the metadata table
         */
        private void addRow() {
            metadataTableModel.addRow(new String[]{"",""});
        }

        /**
         * Removes the selected row from the metadata table
         * */
        private void removeRow() {
            int selectedRow = metadataTable.getSelectedRow();
            if (selectedRow == -1) return;
            metadataTableModel.removeRow(selectedRow);
            if (metadataTable.getRowCount() == 0) {
                metadataTable.getSelectionModel().clearSelection();
            }
            else {
                int newSelectedRow = Math.max(selectedRow - 1, 0);
                metadataTable.getSelectionModel().setSelectionInterval(newSelectedRow, newSelectedRow);
            }
        }

        /**
         * Saves the changes from the table to the sessions metadata
         * */
        private void saveChanges() {
            if (isMetadataChanged()) {
                EditSessionMetadata edit = new EditSessionMetadata(getEditor(), exportTableData());
                getEditor().getUndoSupport().postEdit(edit);
            }
            dispose();
        }

        /**
         * Checks if the metadata from the table is different from the sessions current metadata
         *
         * @return if there were any differences
         * */
        private boolean isMetadataChanged() {
            Map<String, String> existingMetadata = getTranscriptEditor().getSession().getMetadata();
            Map<String, String> dialogMetadata = exportTableData();

            if (existingMetadata.size() != dialogMetadata.size()) return true;

            for (var key : dialogMetadata.keySet()) {
                if (!existingMetadata.containsKey(key) || !existingMetadata.get(key).equals(dialogMetadata.get(key))) return true;
            }

            return false;
        }

        /**
         * Converts the data from the table into a map using the appropriate keys and values
         *
         * @return the converted map
         * */
        private Map<String, String> exportTableData() {
            Map<String, String> retVal = new HashMap<>();

            int rowCount = metadataTableModel.getRowCount();
            for (int i = 0; i < rowCount; i++) {
                String key = (String) metadataTableModel.getValueAt(i, 0);
                String value = (String) metadataTableModel.getValueAt(i, 1);

                if (key != null && !key.isBlank() && value != null && !value.isBlank()) {
                    retVal.put(key, value);
                }
            }

            return retVal;
        }
    }

    /**
     * Context mouse handler for transcript editor
     */
    private final MouseAdapter contextMouseHandler = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
            if(e.isPopupTrigger()) {
                final JPopupMenu menu = new JPopupMenu();
                final MenuBuilder menuBuilder = new MenuBuilder(menu);
                transcriptEditor.setupContextMenu(menuBuilder);

                menuBuilder.addSeparator(".", "view_items");

                final JMenu participantsMenu = menuBuilder.addMenu(".", "Participants");
                setupParticipantsMenu(new MenuBuilder(participantsMenu));

                final JMenu tiersMenu = menuBuilder.addMenu(".", "Tiers");
                setupTiersMenu(new MenuBuilder(tiersMenu));

                final JMenu recordsMenu = menuBuilder.addMenu(".", "Records");
                setupRecordMenu(new MenuBuilder(recordsMenu));

                final JMenu commentsMenu = menuBuilder.addMenu(".", "Comments");
                setupCommentsMenu(new MenuBuilder(commentsMenu));

                final JMenu gemsMenu = menuBuilder.addMenu(".", "Gems");
                setupGemsMenu(new MenuBuilder(gemsMenu));

                menu.show(transcriptEditor, e.getX(), e.getY());
            }
        }
    };
}
