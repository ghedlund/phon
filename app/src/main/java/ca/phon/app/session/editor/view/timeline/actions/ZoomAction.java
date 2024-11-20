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
package ca.phon.app.session.editor.view.timeline.actions;

import ca.phon.app.session.editor.view.timeline.TimelineView;
import ca.phon.media.TimeUIModel;
import ca.phon.session.Session;
import ca.phon.util.PrefHelper;
import ca.phon.util.icons.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class ZoomAction extends TimelineAction {

	private static final long serialVersionUID = 1L;
	
	private final static String CMD_NAME_ZOOMIN = "Zoom in";
	
	private final static String CMD_NAME_ZOOMOUT = "Zoom out";
	
	private final static ImageIcon ZOOMIN_ICON = 
			IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "zoom_in", IconSize.SMALL, UIManager.getColor("Button.foreground"));
	
	private final static ImageIcon ZOOMOUT_ICON =
			IconManager.getInstance().getFontIcon(IconManager.GoogleMaterialDesignIconsFontName, "zoom_out", IconSize.SMALL, UIManager.getColor("Button.foreground"));
	
	private final static String ZOOM_AMOUNT_PROP = 
			TimelineView.class.getName() + ".zoomAmount";
	private final static float DEFAULT_ZOOM_AMOUNT = 0.3f;
	private float zoomAmount = PrefHelper.getFloat(ZOOM_AMOUNT_PROP, DEFAULT_ZOOM_AMOUNT);
	
	private boolean zoomIn = true;
	
	private final static float MIN_PXPERS = 1.0f;
	
	private final static float MAX_PXPERS = 3200.0f;

	public ZoomAction(TimelineView view, boolean zoomIn) {
		super(view);
		
		this.zoomIn = zoomIn;
		
		putValue(NAME, (zoomIn ? CMD_NAME_ZOOMIN : CMD_NAME_ZOOMOUT));
		putValue(SMALL_ICON, (zoomIn ? ZOOMIN_ICON : ZOOMOUT_ICON));
	}

	@Override
	public void hookableActionPerformed(ActionEvent ae) {
		TimeUIModel timeModel = getView().getTimeModel();
		
		float pxPerS = timeModel.getPixelsPerSecond();
		float zoomAmount = -1.0f * pxPerS * this.zoomAmount;
		if(zoomIn) {
			zoomAmount *= -1.0f;
		}
		pxPerS += zoomAmount;
		
		pxPerS = Math.max(MIN_PXPERS, Math.min(pxPerS, MAX_PXPERS));
		
		// beep to indicate we are at a limit
		if(pxPerS == MIN_PXPERS || pxPerS == MAX_PXPERS)
			Toolkit.getDefaultToolkit().beep();
		
		timeModel.setPixelsPerSecond(pxPerS);

		final Session session = getView().getEditor().getSession();
		ListSelectionModel selectionModel = getView().getRecordTier().getSelectionModel();
		int selectedRecords[] = selectionModel.getSelectedIndices();
		if(selectedRecords.length > 0) {
		 	getView().scrollToRecord(session.getRecord(selectionModel.getLeadSelectionIndex()));
		} else {
			getView().scrollToRecord(getView().getRecordTier().getRecordGrid().getCurrentRecord());
		}
	}

}
