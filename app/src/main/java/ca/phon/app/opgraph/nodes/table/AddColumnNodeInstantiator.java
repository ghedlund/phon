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
package ca.phon.app.opgraph.nodes.table;

import ca.phon.app.log.LogUtil;
import ca.phon.opgraph.library.instantiators.Instantiator;
import ca.phon.script.*;
import ca.phon.script.params.*;

public class AddColumnNodeInstantiator implements Instantiator<AddColumnNode> {

	@Override
	public AddColumnNode newInstance(Object... params) throws InstantiationException {
		if(params.length < 1)
			throw new InstantiationException("Incorrect number of parameters");
		final Object obj = params[0];
		if(!(obj instanceof TableScriptNodeData))
			throw new InstantiationException("Incorrect node data type");
		final TableScriptNodeData tableScriptNodeData = (TableScriptNodeData)obj;

		final PhonScript templateScript = tableScriptNodeData.getPhonScript();
		final BasicScript script = new BasicScript(templateScript.getScript());

		// load parameters
		try {
			final ScriptParameters templateParams =
					templateScript.getContext().getScriptParameters(templateScript.getContext().getEvaluatedScope());
			final ScriptParameters scriptParams =
					script.getContext().getScriptParameters(script.getContext().getEvaluatedScope());

			for(ScriptParam param:scriptParams) {
				for(String paramId:param.getParamIds()) {
					scriptParams.setParamValue(paramId, templateParams.getParamValue(paramId));
				}
			}
		} catch (PhonScriptException e) {
			LogUtil.warning(e);
		}

		AddColumnNode retVal = new AddColumnNode(script);
		retVal.setName(tableScriptNodeData.name);
		return retVal;
	}

}
