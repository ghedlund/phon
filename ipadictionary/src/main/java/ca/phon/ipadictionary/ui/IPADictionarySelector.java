package ca.phon.ipadictionary.ui;

import ca.phon.ipadictionary.*;
import ca.phon.util.Language;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A list selection component for {@link IPADictionary}
 *
 */
public class IPADictionarySelector extends JList<IPADictionary> {

	private List<IPADictionary> availableDicts = null;

	public IPADictionarySelector() {
		super();

		init();
	}

	private void init() {
		Iterator<IPADictionary> dictItr = IPADictionaryLibrary.getInstance().availableDictionaries();
		List<IPADictionary> dictList = new ArrayList<>();
		while(dictItr.hasNext()) {
			dictList.add(dictItr.next());
		}
		Collections.sort(dictList, Comparator.comparing(IPADictionary::getName));
		availableDicts = dictList;

		setModel(new IPADictionaryListModel());
		setCellRenderer(new IPADictionaryListCellRenderer());
	}

	public int getLanguageIndex(Language lang) {
		List<Language> languages = availableDicts.stream().map(IPADictionary::getLanguage).collect(Collectors.toList());
		return languages.indexOf(lang);
	}

	public int getIPADictionaryIndex(IPADictionary dict) {
		return getLanguageIndex(dict.getLanguage());
	}

	public void setSelectedLang(Language lang, boolean shouldScroll) {
		for(int i = 0; i < super.getModel().getSize(); i++) {
			IPADictionary dict = getModel().getElementAt(i);
			if(dict.getLanguage().equals(lang)) {
				setSelectedIndex(i);
				if(shouldScroll)
					ensureIndexIsVisible(i);
				return;
			}
		}
		setSelectedIndex(-1);
	}

	public IPADictionary getSelectedIPADictionary() {
		return super.getSelectedValue();
	}

	public void setSelectedIPADictionary(IPADictionary dict, boolean shouldScroll) {
		super.setSelectedValue(dict, shouldScroll);
	}

	private class IPADictionaryListModel extends AbstractListModel<IPADictionary> {

		@Override
		public int getSize() {
			return availableDicts.size();
		}

		@Override
		public IPADictionary getElementAt(int index) {
			return availableDicts.get(index);
		}

	}

	private class IPADictionaryListCellRenderer extends DefaultListCellRenderer {

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			JLabel retVal = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

			if(value instanceof IPADictionary) {
				IPADictionary dict = (IPADictionary) value;
				retVal.setText(dict.getName() + " (" + dict.getLanguage().toString() + ")");
			}

			return retVal;
		}

	}

}
