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
package ca.phon.app.opgraph.wizard;

import ca.phon.app.log.LogUtil;
import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;

import java.io.*;
import java.util.*;

public class NodeWizardReportTemplate {

	private String name;

	private String template;

	public NodeWizardReportTemplate() {
		this(UUID.randomUUID().toString());
	}

	public NodeWizardReportTemplate(String name) {
		this(name, "");
	}

	public NodeWizardReportTemplate(String name, String template) {
		super();

		this.name = name;
		this.template = template;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTemplate() {
		return this.template;
	}

	public void setTemplate(String template) {
		this.template = template;
	}

	public String merge(NodeWizardReportContext ctx) {
		// TODO move velocity init somewhere else
		final Properties p = new Properties();
		try {
			p.load(getClass().getResourceAsStream("velocity.properties"));
		} catch (IOException e) {
			LogUtil.warning(e);
		}

		final VelocityEngine ve = new VelocityEngine();
		ve.init(p);

		StringResourceRepository repository = StringResourceLoader.getRepository();
		repository.putStringResource(getName(), getTemplate(), "UTF-8");

		// load template
		final Template t = ve.getTemplate(getName(), "UTF-8");
		final StringWriter sw = new StringWriter();

		t.merge(ctx.velocityContext(), sw);

		return sw.toString();
	}

}
