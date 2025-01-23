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
package ca.phon.app.session.editor;

import ca.phon.extensions.*;
import ca.phon.session.position.TranscriptElementLocation;
import ca.phon.session.position.TranscriptElementRange;
import ca.phon.util.Range;

import javax.swing.text.Highlighter.HighlightPainter;
import java.util.Comparator;
import java.util.Set;

/**
 * Selection information used for {@link EditorSelectionModel}.
 * Custom highlighting can be obtained by attaching an extension
 * of type {@link HighlightPainter}
 */
public class SessionEditorSelection implements IExtendable {
	
	private final TranscriptElementRange range;
	
	private final ExtensionSupport extSupport = new ExtensionSupport(SessionEditorSelection.class, this);

	public SessionEditorSelection(int elementIndex, String tierName, Range range) {
		super();
		this.range = new TranscriptElementRange(elementIndex, tierName, range);
		
		extSupport.initExtensions();
	}

	public SessionEditorSelection(TranscriptElementRange range) {
		super();
		this.range = range;

		extSupport.initExtensions();
	}

	public TranscriptElementRange getTranscriptElementRange() {
		return range;
	}

	public int getElementIndex() {
		return range.transcriptElementIndex();
	}

	public String getTierName() {
		return range.tier();
	}

	public Range getRange() {
		return range.range();
	}

	public Set<Class<?>> getExtensions() {
		return extSupport.getExtensions();
	}

	public <T> T getExtension(Class<T> cap) {
		return extSupport.getExtension(cap);
	}

	public <T> T putExtension(Class<T> cap, T impl) {
		return extSupport.putExtension(cap, impl);
	}

	public <T> T removeExtension(Class<T> cap) {
		return extSupport.removeExtension(cap);
	}
	
}
