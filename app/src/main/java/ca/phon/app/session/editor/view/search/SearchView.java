package ca.phon.app.session.editor.view.search;

import ca.phon.app.session.editor.*;
import ca.phon.app.session.editor.search.FindExpr;
import ca.phon.app.session.editor.search.FindManager;
import ca.phon.app.session.editor.search.SearchType;
import ca.phon.app.session.editor.view.transcript.BoxSelectHighlightPainter;
import ca.phon.app.session.editor.view.transcript.TranscriptEditor;
import ca.phon.app.session.editor.view.transcript.TranscriptView;
import ca.phon.session.Participant;
import ca.phon.session.TierViewItem;
import ca.phon.session.position.TranscriptElementLocation;
import ca.phon.session.position.TranscriptElementRange;
import ca.phon.ui.FlatButton;
import ca.phon.ui.action.PhonUIAction;
import ca.phon.ui.text.SearchField;
import ca.phon.util.icons.IconManager;
import ca.phon.util.icons.IconSize;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Search view for session editor.  Allows users to search for text in the session transcript.
 * Includes options for case-sensitive, regex, and phonex searches as well as options for filtering
 * search results based on tier and speaker.
 */
@EditorViewInfo(name=SearchView.VIEW_NAME, category= EditorViewCategory.UTILITIES, icon=SearchView.VIEW_ICON)
public class SearchView extends EditorView {

    public static final String VIEW_NAME = "Search";

    public static final String VIEW_ICON = IconManager.GoogleMaterialDesignIconsFontName + ":SEARCH";

    private final static String SEARCH_HISTORY_PROP_PREFIX = "SessionEditor.searchHistory";

    private final static int MAX_SEARCH_HISTORY = 10;

    private Stack<String> searchHistory;

    private SearchField searchField;

    private JLabel resultsLabel;

    private FlatButton filterButton;

    private FlatButton caseSensitiveButton;

    private FlatButton regexButton;

    private FlatButton phonexButton;

    private SearchViewTable table;

    private List<String> filterTiers = new ArrayList<>();

    private List<Participant> filterSpeakers = new ArrayList<>();

    private boolean includeComments = true;

    private boolean includeGems = true;

    public SearchView(SessionEditor editor) {
        super(editor);

        init();
    }

    private SessionEditorSelection currentSelection = null;

    private void init() {
        setLayout(new BorderLayout());

        final PhonUIAction<Void> caseSensitiveAct = PhonUIAction.runnable(this::toggleCaseSensitive);
        caseSensitiveAct.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
        caseSensitiveAct.putValue(FlatButton.ICON_NAME_PROP, "match_case");
        caseSensitiveAct.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);
        caseSensitiveAct.putValue(Action.SHORT_DESCRIPTION, "Match case");
        caseSensitiveAct.putValue(Action.SELECTED_KEY, false);
        caseSensitiveButton = new FlatButton(caseSensitiveAct);
        caseSensitiveButton.setIconColor(UIManager.getColor("textInactiveText"));
        caseSensitiveButton.setIconSelectedColor(UIManager.getColor("Phon.darkBlue"));

        final PhonUIAction<Void> regexAct = PhonUIAction.runnable(this::toggleRegex);
        regexAct.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
        regexAct.putValue(FlatButton.ICON_NAME_PROP, "regular_expression");
        regexAct.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);
        regexAct.putValue(Action.SHORT_DESCRIPTION, "Regular expression search");
        regexAct.putValue(Action.SELECTED_KEY, false);
        regexButton = new FlatButton(regexAct);
        regexButton.setIconColor(UIManager.getColor("textInactiveText"));
        regexButton.setIconSelectedColor(UIManager.getColor("Phon.darkBlue"));

        final PhonUIAction<Void> phonexAct = PhonUIAction.runnable(this::togglePhonex);
        phonexAct.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
        phonexAct.putValue(FlatButton.ICON_NAME_PROP, "data_object");
        phonexAct.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);
        phonexAct.putValue(Action.SHORT_DESCRIPTION, "Phonex search");
        phonexAct.putValue(Action.SELECTED_KEY, false);
        phonexButton = new FlatButton(phonexAct);
        phonexButton.setIconColor(UIManager.getColor("textInactiveText"));
        phonexButton.setIconSelectedColor(UIManager.getColor("Phon.darkBlue"));

        final PhonUIAction<Void> filterAct = PhonUIAction.runnable(this::showFilterMenu);
        filterAct.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
        filterAct.putValue(FlatButton.ICON_NAME_PROP, "filter_list");
        filterAct.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);
        filterAct.putValue(Action.SHORT_DESCRIPTION, "Filter search results");
        filterButton = new FlatButton(filterAct);
        filterButton.setIconColor(UIManager.getColor("textInactiveText"));
        filterButton.setIconSelectedColor(UIManager.getColor("Phon.darkBlue"));

        this.searchField = new SearchField("Search tiers...");
        final PhonUIAction<Void> searchAct = PhonUIAction.runnable(this::onQuery);
        this.searchField.setAction(searchAct);
        this.searchField.addPropertyChangeListener("text_cleared", (e) -> {
            clearResults();
        });

        resultsLabel = new JLabel("0 results");
        resultsLabel.setForeground(UIManager.getColor("textInactiveText"));
        resultsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        resultsLabel.setPreferredSize(new Dimension(100, resultsLabel.getPreferredSize().height));

        final JPanel searchOptionsPanel = new JPanel(new FormLayout("fill:pref:grow, pref, pref, pref, pref, pref", "pref"));
        final CellConstraints cc = new CellConstraints();
        int col = 1;
        searchOptionsPanel.add(searchField, cc.xy(col++, 1));
        searchOptionsPanel.add(caseSensitiveButton, cc.xy(col++, 1));
        searchOptionsPanel.add(regexButton, cc.xy(col++, 1));
        searchOptionsPanel.add(phonexButton, cc.xy(col++, 1));
        searchOptionsPanel.add(resultsLabel, cc.xy(col++, 1));
        searchOptionsPanel.add(filterButton, cc.xy(col++, 1));
        add(searchOptionsPanel, BorderLayout.NORTH);

        this.table = new SearchViewTable(getEditor().getSession(), new ArrayList<>());
        this.table.getSelectionModel().addListSelectionListener(tableSelectionListener);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void clearResults() {
        getEditor().getSelectionModel().clear();
        this.table.clearSearch();
        this.resultsLabel.setText("0 results");
        this.resultsLabel.setForeground(UIManager.getColor("textInactiveText"));
    }

    private void toggleCaseSensitive() {
        final boolean caseSensitive = caseSensitiveButton.isSelected();
        caseSensitiveButton.setSelected(!caseSensitive);
        onQuery();
    }

    private void toggleRegex() {
        final boolean regex = regexButton.isSelected();
        regexButton.setSelected(!regex);
        if(regexButton.isSelected() && phonexButton.isSelected()) {
            phonexButton.setSelected(false);
        }
        onQuery();
    }

    private void togglePhonex() {
        final boolean phonex = phonexButton.isSelected();
        phonexButton.setSelected(!phonex);
        if(phonexButton.isSelected() && regexButton.isSelected()) {
            regexButton.setSelected(false);
        }
        onQuery();
    }

    private void showFilterMenu() {
        final JPopupMenu filterMenu = new JPopupMenu();

        filterMenu.add(new JLabel("Filter by tier:"));
        for(TierViewItem tvi:getEditor().getSession().getTierView()) {
            if(!tvi.isVisible()) continue;
            final JCheckBoxMenuItem tierItem = new JCheckBoxMenuItem(tvi.getTierName());
            tierItem.setSelected(filterTiers.contains(tvi.getTierName()));
            tierItem.addActionListener( (e) -> {
                if(tierItem.isSelected()) {
                    filterTiers.add(tvi.getTierName());
                } else {
                    filterTiers.remove(tvi.getTierName());
                }
                onQuery();
            });
            filterMenu.add(tierItem);
        }

        filterMenu.addSeparator();

        filterMenu.add(new JLabel("Filter by speaker:"));
        final List<Participant> participants = new ArrayList<>();
        for(Participant speaker:getEditor().getSession().getParticipants()) {
            participants.add(speaker);
        }
        participants.add(Participant.UNKNOWN);
        for(Participant speaker:participants) {
            final JCheckBoxMenuItem speakerItem = new JCheckBoxMenuItem(speaker.toString());
            speakerItem.setSelected(filterSpeakers.contains(speaker));
            speakerItem.addActionListener( (e) -> {
                if(speakerItem.isSelected()) {
                    filterSpeakers.add(speaker);
                } else {
                    filterSpeakers.remove(speaker);
                }
                onQuery();
            });
            filterMenu.add(speakerItem);
        }

        filterMenu.addSeparator();
        final JCheckBoxMenuItem includeCommentsItem = new JCheckBoxMenuItem("Include comments");
        includeCommentsItem.setSelected(includeComments);
        includeCommentsItem.addActionListener( (e) -> {
            includeComments = includeCommentsItem.isSelected();
            onQuery();
        });
        filterMenu.add(includeCommentsItem);

        final JCheckBoxMenuItem includeGemsItem = new JCheckBoxMenuItem("Include gems");
        includeGemsItem.setSelected(includeGems);
        includeGemsItem.addActionListener( (e) -> {
            includeGems = includeGemsItem.isSelected();
            onQuery();
        });
        filterMenu.add(includeGemsItem);

        filterMenu.addSeparator();
        final JMenuItem resetItem = new JCheckBoxMenuItem("Reset filters");
        resetItem.addActionListener( (e) -> {
            filterTiers.clear();
            filterSpeakers.clear();
            includeComments = true;
            includeGems = true;
            onQuery();
        });
        filterMenu.add(resetItem);

        filterMenu.show(filterButton, 0, filterButton.getHeight());
    }

    private void setupSearchTiers(FindManager findManager) {
        final List<String> searchTiers = new ArrayList<>();
        for(TierViewItem tvi:getEditor().getSession().getTierView()) {
            if(!tvi.isVisible()) continue;
            if(filterTiers.size() > 0 && !filterTiers.contains(tvi.getTierName())) continue;
            searchTiers.add(tvi.getTierName());
        }
        findManager.setSearchTiers(searchTiers.toArray(new String[0]));
    }

    private void setupRecordFilter(FindManager findManager) {
        final List<Participant> speakers = new ArrayList<>();
        if(filterSpeakers.size() > 0) {
            if(filterSpeakers.contains(Participant.UNKNOWN)) {
                speakers.add(Participant.UNKNOWN);
            }
            for (Participant speaker : getEditor().getSession().getParticipants()) {
                if (!filterSpeakers.contains(speaker)) continue;
                speakers.add(speaker);
            }
            findManager.setSpeakers(speakers);
        }
        findManager.setIncludeComments(includeComments);
        findManager.setIncludeGems(includeGems);
    }

    private void updateFilterButton() {
        if(filterTiers.size() > 0 || filterSpeakers.size() > 0 || !includeComments || !includeGems) {
            filterButton.setSelected(true);
        } else {
            filterButton.setSelected(false);
        }
    }

    /**
     * Executes query will all current filters
     *
     */
    public void onQuery() {
        getEditor().getSelectionModel().clear();
        updateFilterButton();
        final String queryText = searchField.getText();
        if(queryText.trim().length() == 0) {
            clearResults();
            return;
        }
        final FindManager findManager = new FindManager(getEditor().getSession());
        findManager.setCurrentLocation(new TranscriptElementLocation(0, findManager.getSearchTiers()[0], 0));
        final FindExpr findExpr = new FindExpr(searchField.getText());
        findExpr.setCaseSensitive(caseSensitiveButton.isSelected());
        if(regexButton.isSelected()) {
            findExpr.setType(SearchType.REGEX);
        } else if(phonexButton.isSelected()) {
            findExpr.setType(SearchType.PHONEX);
        } else {
            findExpr.setType(SearchType.PLAIN);
        }
        findManager.setAnyExpr(findExpr);
        setupSearchTiers(findManager);
        setupRecordFilter(findManager);

        this.resultsLabel.setText("0 results");
        this.resultsLabel.setForeground(UIManager.getColor("textInactiveText"));
        this.table.search(findManager);
        final TableModelListener listener = new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if(e.getType() != TableModelEvent.INSERT) return;
                for(int i = e.getFirstRow(); i <= e.getLastRow(); i++) {
                    final TranscriptElementRange range = table.getSearchViewTableModel().getRangeAt(i);
                    if(range == null) continue;
                    final SessionEditorSelection selection = new SessionEditorSelection(range);
                    selection.putExtension(Highlighter.HighlightPainter.class, new BoxSelectHighlightPainter());
                    getEditor().getSelectionModel().addSelection(selection);
                }
                final int range = e.getLastRow() - e.getFirstRow() + 1;
                final int total = table.getRowCount() + range;
                resultsLabel.setText(total + (total == 1 ? " result" : " results"));
            }
        };
        this.table.getModel().addTableModelListener(listener);
        this.table.addPropertyChangeListener(SearchViewTable.SEARCHING_PROP, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if(!(Boolean)evt.getNewValue()) {
                    resultsLabel.setForeground(UIManager.getColor("textText"));
                    table.getModel().removeTableModelListener(listener);
                    table.removePropertyChangeListener(this);
                    table.packAll();
                }
            }
        });
    }

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
        return null;
    }

    /**
     * Listener for table selection and update transcript view
     */
    private final ListSelectionListener tableSelectionListener =  (e) -> {
        final int row = table.getSelectedRow();
        if(row >= 0) {
            final TranscriptElementRange range = table.getSearchViewTableModel().getRangeAt(row);
            final TranscriptElementLocation start = range.start();
            final TranscriptElementLocation end = range.end();
            // add selection to model
            if(currentSelection != null) {
                getEditor().getSelectionModel().removeSelection(currentSelection);
            }

            final SessionEditorSelection selection = new SessionEditorSelection(range);
            getEditor().getSelectionModel().addSelection(selection);
            currentSelection = selection;

            // move transcript view caret
            if(getEditor().getViewModel().isShowing(TranscriptView.VIEW_NAME)) {
                final TranscriptView transcriptView = (TranscriptView) getEditor().getViewModel().getView(TranscriptView.VIEW_NAME);
                if(transcriptView.getTranscriptEditor().isSingleRecordView()) {
                    final int recordIndex = transcriptView.getEditor().getSession().getTranscript().getRecordIndex(start.transcriptElementIndex());
                    if(recordIndex >= 0) {
                        getEditor().getEventManager().registerActionForEvent(
                                TranscriptEditor.recordChangedInSingleRecordMode,
                                new EditorAction<Void>() {
                                    @Override
                                    public void eventOccurred(EditorEvent<Void> ee) {
                                        final SessionEditorSelection selection = new SessionEditorSelection(range);
                                        getEditor().getSelectionModel().addSelection(selection);
                                        currentSelection = selection;

                                        getEditor().getEventManager().removeActionForEvent(
                                                TranscriptEditor.recordChangedInSingleRecordMode,
                                                this
                                        );
                                    }
                                },
                                EditorEventManager.RunOn.AWTEventDispatchThread
                        );

                        getEditor().setCurrentRecordIndex(recordIndex);
                    }
                } else {
                    // ??? is this necessary since we are already setting the selection?
                    final int caretLocation = transcriptView.getTranscriptEditor().sessionLocationToCharPos(start);
                    final int endLocation = transcriptView.getTranscriptEditor().sessionLocationToCharPos(end);
                    if (caretLocation >= 0 && endLocation >= caretLocation) {
                        // also select text
                        transcriptView.getTranscriptEditor().getCaret().setDot(caretLocation);
                        transcriptView.getTranscriptEditor().getCaret().moveDot(endLocation);
                    }
                }
            }
        }
    };

}
