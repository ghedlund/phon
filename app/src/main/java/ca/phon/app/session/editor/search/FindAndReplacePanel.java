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
package ca.phon.app.session.editor.search;

import ca.phon.app.session.editor.*;
import ca.phon.app.session.editor.search.FindManager.*;
import ca.phon.app.session.editor.search.actions.*;
import ca.phon.app.session.editor.view.common.*;
import ca.phon.app.session.editor.view.transcript.BoxSelectHighlightPainter;
import ca.phon.app.session.editor.view.transcript.TranscriptView;
import ca.phon.session.Record;
import ca.phon.session.*;
import ca.phon.session.position.*;
import ca.phon.ui.FlatButton;
import ca.phon.ui.IconStrip;
import ca.phon.ui.action.PhonUIAction;
import ca.phon.ui.text.PromptedTextField;
import ca.phon.ui.text.SearchField;
import ca.phon.util.icons.IconManager;
import ca.phon.util.icons.IconSize;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.jdesktop.swingx.HorizontalLayout;
import org.jdesktop.swingx.VerticalLayout;

import javax.swing.*;
import javax.swing.text.Highlighter;
import javax.swing.undo.UndoableEditSupport;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * Find and replace panel for the session editor.
 */
public class FindAndReplacePanel extends JPanel {

	// find panel
	private JPanel searchOptionsPanel;

	private JPanel searchOptionsButtonPanel;

	private SearchField searchField;

	private JLabel resultsLabel;

	private FlatButton filterButton;

	private FlatButton caseSensitiveButton;

	private FlatButton regexButton;

	private FlatButton phonexButton;

	private FlatButton findButton;

	private FlatButton findPrevButton;

	// replace panel
	private FlatButton toggleReplaceOptionsButton;

	private JPanel replaceOptionsButtonPanel;

	private SearchField replaceField;

	private JButton replaceButton;

	private JButton replaceFindButton;

	private JButton replaceAllButton;

	// region - filter settings
	private List<String> filterTiers = new ArrayList<>();

	private List<Participant> filterSpeakers = new ArrayList<>();

	private boolean includeComments = true;

	private boolean includeGems = true;
	// endregion
	
	// find manager
	private final FindManager findManager;
	private final EditorViewModel editorViewModel;
	private final EditorDataModel editorDataModel;
	private final EditorSelectionModel selectionModel;
	private final EditorEventManager editorEventManager;
	private final UndoableEditSupport undoableEditSupport;

	private List<TranscriptElementRange> searchResults = new ArrayList<>();
	private int currentResultIdx = -1;
	private SessionEditorSelection currentSelection = null;

	/**
	 * Create a new FindAndReplacePanel for the given session editor
	 *
	 * @param sessionEditor
	 */
	public FindAndReplacePanel(SessionEditor sessionEditor) {
		this(sessionEditor.getDataModel(), sessionEditor.getSelectionModel(), sessionEditor.getEventManager(), sessionEditor.getViewModel(), sessionEditor.getUndoSupport());
	}

	/**
	 * Create a new FindAndReplacePanel using the provided Session and EditorSelectionModel
	 *
	 * @param editorDataModel
	 * @param selectionModel
	 * @param eventManager
	 */
	public FindAndReplacePanel(EditorDataModel editorDataModel, EditorSelectionModel selectionModel, EditorEventManager eventManager, EditorViewModel editorViewModel, UndoableEditSupport undoSupport) {
		super();

		this.editorDataModel = editorDataModel;
		this.findManager = new FindManager(editorDataModel.getSession());
		this.selectionModel = selectionModel;
		this.editorEventManager = eventManager;
		this.editorViewModel = editorViewModel;
		this.undoableEditSupport = undoSupport;

		init();
	}

	private void init() {
		setOpaque(true);
		setBackground(UIManager.getColor("text"));

		setLayout(new BorderLayout());

		final PhonUIAction<Void> findAct = PhonUIAction.runnable(this::findNext);
		findAct.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
		findAct.putValue(FlatButton.ICON_NAME_PROP, "arrow_downward");
		findAct.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);
		findAct.putValue(Action.SHORT_DESCRIPTION, "Find next");
		findButton = new FlatButton(findAct);
		findButton.setIconColor(UIManager.getColor("textInactiveText"));

		final PhonUIAction<Void> findPrevAct = PhonUIAction.runnable(this::findPrev);
		findPrevAct.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
		findPrevAct.putValue(FlatButton.ICON_NAME_PROP, "arrow_upward");
		findPrevAct.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);
		findPrevAct.putValue(Action.SHORT_DESCRIPTION, "Find previous");
		findPrevButton = new FlatButton(findPrevAct);
		findPrevButton.setIconColor(UIManager.getColor("textInactiveText"));

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
		searchField.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		resultsLabel = new JLabel("0 results");
		resultsLabel.setForeground(UIManager.getColor("textInactiveText"));
		resultsLabel.setHorizontalAlignment(SwingConstants.CENTER);
		resultsLabel.setPreferredSize(new Dimension(100, resultsLabel.getPreferredSize().height));

		searchOptionsPanel = new JPanel(new FormLayout("fill:pref:grow, pref", "pref, pref"));
		final CellConstraints cc = new CellConstraints();
		add(searchOptionsPanel, BorderLayout.CENTER);

		searchOptionsPanel.add(searchField, cc.xy(1, 1));

		searchOptionsButtonPanel = new IconStrip(SwingConstants.HORIZONTAL);
		searchOptionsButtonPanel.add(findButton, IconStrip.IconStripPosition.LEFT);
		searchOptionsButtonPanel.add(findPrevButton, IconStrip.IconStripPosition.LEFT);
		searchOptionsButtonPanel.add(caseSensitiveButton, IconStrip.IconStripPosition.LEFT);
		searchOptionsButtonPanel.add(regexButton, IconStrip.IconStripPosition.LEFT);
		searchOptionsButtonPanel.add(phonexButton, IconStrip.IconStripPosition.LEFT);
		searchOptionsButtonPanel.add(resultsLabel, IconStrip.IconStripPosition.LEFT);
		searchOptionsButtonPanel.add(filterButton, IconStrip.IconStripPosition.LEFT);
		searchOptionsPanel.add(searchOptionsButtonPanel, cc.xy(2, 1));

		replaceField = new SearchField("Replace with...");
		replaceField.setPreferredSize(new Dimension(200, (int)searchField.getPreferredSize().getHeight()));
		replaceField.setBorder(
				BorderFactory.createCompoundBorder(
						BorderFactory.createMatteBorder(1,0, 0, 0, UIManager.getColor("window")),
						BorderFactory.createEmptyBorder(4, 5, 5, 5)));

		replaceOptionsButtonPanel = new IconStrip(SwingConstants.HORIZONTAL);
		searchOptionsPanel.add(replaceField, cc.xy(1, 2));
		searchOptionsPanel.add(replaceOptionsButtonPanel, cc.xy(2, 2));
		replaceField.setVisible(false);
		replaceOptionsButtonPanel.setVisible(false);

		final PhonUIAction<Void> replaceAct = PhonUIAction.runnable(this::replaceCurrent);
		replaceAct.putValue(PhonUIAction.NAME, "Replace");
//		replaceAct.putValue(FlatButton.ICON_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
//		replaceAct.putValue(FlatButton.ICON_NAME_PROP, "read_more");
//		replaceAct.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);
		replaceAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Replace and move to next");
		replaceButton = new JButton(replaceAct);
//		replaceButton.setIconColor(UIManager.getColor("textInactiveText"));
		replaceOptionsButtonPanel.add(replaceButton, IconStrip.IconStripPosition.LEFT);

		final PhonUIAction<Void> replaceAllAct = PhonUIAction.runnable(this::replaceAll);
		replaceAllAct.putValue(PhonUIAction.NAME, "Replace all");
		replaceAllAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Replace all occurrences");
		replaceAllButton = new JButton(replaceAllAct);
		replaceOptionsButtonPanel.add(replaceAllButton, IconStrip.IconStripPosition.LEFT);

		final PhonUIAction<Void> toggleReplaceAct = PhonUIAction.runnable(this::toggleReplace);
		toggleReplaceAct.putValue(FlatButton.ICON_FONT_NAME_PROP, IconManager.GoogleMaterialDesignIconsFontName);
		toggleReplaceAct.putValue(FlatButton.ICON_NAME_PROP, "unfold_more");
		toggleReplaceAct.putValue(FlatButton.ICON_SIZE_PROP, IconSize.MEDIUM);
		toggleReplaceAct.putValue(Action.SHORT_DESCRIPTION, "Show replace options");
		toggleReplaceOptionsButton = new FlatButton(toggleReplaceAct);

		final IconStrip toggleReplaceStrip = new IconStrip(SwingConstants.VERTICAL);
		toggleReplaceStrip.add(toggleReplaceOptionsButton, IconStrip.IconStripPosition.LEFT);
		add(toggleReplaceStrip, BorderLayout.WEST);
	}

	public void clearResults() {
		getSelectionModel().clear();
		this.resultsLabel.setText("0 results");
		this.resultsLabel.setForeground(UIManager.getColor("textInactiveText"));

		findButton.setIconColor(UIManager.getColor("textInactiveText"));
		findPrevButton.setIconColor(UIManager.getColor("textInactiveText"));

		searchResults.clear();
		currentResultIdx = -1;
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
		for(TierViewItem tvi:getEditorDataModel().getSession().getTierView()) {
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
		for(Participant speaker:getEditorDataModel().getSession().getParticipants()) {
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
		for(TierViewItem tvi:getEditorDataModel().getSession().getTierView()) {
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
			for (Participant speaker : getEditorDataModel().getSession().getParticipants()) {
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

	public void toggleReplace() {
		if(replaceField.isVisible()) {
			replaceField.setVisible(false);
			replaceOptionsButtonPanel.setVisible(false);
			toggleReplaceOptionsButton.setIconName("unfold_more");
		} else {
			replaceField.setVisible(true);
			replaceOptionsButtonPanel.setVisible(true);
			toggleReplaceOptionsButton.setIconName("unfold_less");
		}
	}

	public void onQuery() {
		final String query = searchField.getText();
		if(query.length() == 0) {
			clearResults();
			return;
		}

		getSelectionModel().clear();
		updateFilterButton();
		final String queryText = searchField.getText();
		if(queryText.trim().length() == 0) {
			clearResults();
			return;
		}

		final TranscriptView transcriptView = (TranscriptView) editorViewModel.getView(TranscriptView.VIEW_NAME);
		if(transcriptView == null) return;
		final TranscriptElementLocation startLocation = new TranscriptElementLocation(0, findManager.getSearchTiers()[0], 0);
		final TranscriptElementLocation currentLocation = transcriptView.getTranscriptEditor().getCurrentSessionLocation();
//		if(currentLocation.valid()) {
//			findManager.setCurrentLocation(currentLocation);
//		} else {
			findManager.setCurrentLocation(startLocation);
//		}

		final FindExpr findExpr = new FindExpr(queryText);
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

		searchResults.clear();
		currentResultIdx = -1;
		TranscriptElementRange result = null;
		while((result = findManager.findNext()) != null) {
			searchResults.add(result);
		}
		updateSearchButtons();

		// setup search result highlights in editor
		for(TranscriptElementRange range:searchResults) {
			final SessionEditorSelection selection = new SessionEditorSelection(range);
			selection.putExtension(Highlighter.HighlightPainter.class, new BoxSelectHighlightPainter());
			getSelectionModel().addSelection(selection);
		}

		findNext();
	}

	private void updateSearchButtons() {
		if(searchResults.size() > 0) {
			resultsLabel.setText(searchResults.size() + " results");
			resultsLabel.setForeground(UIManager.getColor("textText"));

			findButton.setIconColor(UIManager.getColor("textText"));
			findPrevButton.setIconColor(UIManager.getColor("textText"));
		} else {
			resultsLabel.setText("0 results");
			resultsLabel.setForeground(UIManager.getColor("textInactiveText"));

			findButton.setIconColor(UIManager.getColor("textInactiveText"));
			findPrevButton.setIconColor(UIManager.getColor("textInactiveText"));
		}
	}

	// region Getters/Setters
	public SearchField getSearchField() {
		return searchField;
	}

	public SearchField getReplaceField() {
		return replaceField;
	}

	public FindManager getFindManager() {
		return this.findManager;
	}

	public EditorDataModel getEditorDataModel() {
		return editorDataModel;
	}

	public EditorEventManager getEditorEventManager() {
		return editorEventManager;
	}

	public Session getSession() {
		return getEditorDataModel().getSession();
	}

	public EditorSelectionModel getSelectionModel() {
		return selectionModel;
	}

	public UndoableEditSupport getUndoSupport() {
		return undoableEditSupport;
	}
	// end region

	public void replaceCurrent() {

	}

	public void replaceAll() {

	}

	private void removeCurrentSelection() {
		if(currentSelection != null) {
			getSelectionModel().removeSelection(currentSelection);
		}
		currentSelection = null;
	}

	public void findNext() {
		removeCurrentSelection();
		final int resultIdx = currentResultIdx + 1;
		if(resultIdx < searchResults.size()) {
			currentResultIdx = resultIdx;
			final TranscriptElementRange range = searchResults.get(resultIdx);
			final SessionEditorSelection selection = new SessionEditorSelection(range);
			getSelectionModel().addSelection(selection);
			currentSelection = selection;

			final TranscriptView transcriptView = (TranscriptView) editorViewModel.getView(TranscriptView.VIEW_NAME);
			final int charPos = transcriptView.getTranscriptEditor().sessionLocationToCharPos(range.start());
			if(charPos >= 0)
				transcriptView.getTranscriptEditor().setCaretPosition(charPos);
		} else {
			Toolkit.getDefaultToolkit().beep();
			currentResultIdx = -1;
		}
	}

	public void findPrev() {
		removeCurrentSelection();
		final int resultIdx = currentResultIdx - 1;
		if(resultIdx >= 0) {
			currentResultIdx = resultIdx;
			final TranscriptElementRange range = searchResults.get(resultIdx);
			final SessionEditorSelection selection = new SessionEditorSelection(range);
			getSelectionModel().addSelection(selection);
			currentSelection = selection;

			final TranscriptView transcriptView = (TranscriptView) editorViewModel.getView(TranscriptView.VIEW_NAME);
			final int charPos = transcriptView.getTranscriptEditor().sessionLocationToCharPos(range.start());
			if (charPos >= 0)
				transcriptView.getTranscriptEditor().setCaretPosition(charPos);
		} else {
			Toolkit.getDefaultToolkit().beep();
			currentResultIdx = searchResults.size();
		}
	}

}
