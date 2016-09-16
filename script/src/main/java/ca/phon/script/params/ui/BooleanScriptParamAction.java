/*
 * Phon - An open source tool for research in phonology.
 * Copyright (C) 2005 - 2016, Gregory Hedlund <ghedlund@mun.ca> and Yvan Rose <yrose@mun.ca>
 * Dept of Linguistics, Memorial University <https://phon.ca>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ca.phon.script.params.ui;

import java.awt.event.ActionEvent;

import javax.swing.JCheckBox;

import ca.phon.script.params.ScriptParam;

/**
 * Boolean script parameters
 */
public class BooleanScriptParamAction extends ScriptParamAction {

	public BooleanScriptParamAction(ScriptParam param, String id) {
		super(param, id);
	}

	private static final long serialVersionUID = -5924195124548378433L;

	@Override
	public void actionPerformed(ActionEvent e) {
		final JCheckBox checkBox = JCheckBox.class.cast(e.getSource());
		final ScriptParam param = getScriptParam();
		param.setValue(getParamId(), checkBox.isSelected());
	}
	
}
