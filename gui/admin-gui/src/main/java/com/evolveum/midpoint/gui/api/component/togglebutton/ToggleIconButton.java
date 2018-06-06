/**
 * Copyright (c) 2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.gui.api.component.togglebutton;

import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * Simple button that toggles two states (on-off, alphasort-numericsort, etc).
 * The button changes the icon when pressed.
 *
 * @author semancik
 */
public abstract class ToggleIconButton<T> extends AjaxButton{
	private static final long serialVersionUID = 1L;

	private String cssClassOff;
	private String cssClassOn;

	public ToggleIconButton(String id) {
		super(id);
		initLayout();
	}

	public ToggleIconButton(String id, String cssClassOff, String cssClassOn) {
		super(id);
		this.cssClassOff = cssClassOff;
		this.cssClassOn = cssClassOn;
		initLayout();
	}

	public ToggleIconButton(String id, IModel model) {
		super(id, model);
		initLayout();
	}

	public ToggleIconButton(String id, IModel model, String cssClassOff, String cssClassOn) {
		super(id, model);
		this.cssClassOff = cssClassOff;
		this.cssClassOn = cssClassOn;
		initLayout();
	}

	private void initLayout() {
		setEscapeModelStrings(false);
//		setBody(new Model<String>(){
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			public String getObject() {
//				if (isOn()) {
//					return "<i class=\""+cssClassOn+"\"></i>";
//				} else {
//					return "<i class=\""+cssClassOff+"\"></i>";
//				}
//			}
//        });
	}

	public abstract boolean isOn();

}
