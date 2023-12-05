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

import ca.phon.app.session.ViewPosition;

import javax.swing.*;
import java.lang.annotation.*;

/**
 * Annotation used for editor view extensions.  This annotation
 * should exist on the plug-in extension point implementation.
 * 
 * 
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EditorViewInfo {
	
	/**
	 * Name of the view
	 */
	public String name();

	/**
	 * View category
	 */
	public EditorViewCategory category() default EditorViewCategory.PLUGINS;
	
	/**
	 * View icon
	 */
	public String icon() default "blank";

	/**
	 * Preferred dock position, defaults to WORK
	 *
	 */
	public ViewPosition dockPosition() default ViewPosition.WORK;
	
}
