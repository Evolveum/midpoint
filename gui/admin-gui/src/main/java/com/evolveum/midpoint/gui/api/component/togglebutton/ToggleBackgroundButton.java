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

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;

/**
 * Simple button that toggles two states (on-off).
 * The button changes the background when pressed.
 *
 * @author semancik
 */
public abstract class ToggleBackgroundButton<T> extends AjaxLink<T> {
	private static final long serialVersionUID = 1L;

	private String cssClassOff = GuiStyleConstants.CLASS_BUTTON_TOGGLE_OFF;
	private String cssClassOn = GuiStyleConstants.CLASS_BUTTON_TOGGLE_ON;

	public ToggleBackgroundButton(String id) {
		super(id);
		initLayout();
	}

	public ToggleBackgroundButton(String id, String cssClassOff, String cssClassOn) {
		super(id);
		this.cssClassOff = cssClassOff;
		this.cssClassOn = cssClassOn;
		initLayout();
	}

	public ToggleBackgroundButton(String id, IModel<T> model) {
		super(id, model);
		initLayout();
	}

	public ToggleBackgroundButton(String id, IModel<T> model, String cssClassOff, String cssClassOn) {
		super(id, model);
		this.cssClassOff = cssClassOff;
		this.cssClassOn = cssClassOn;
		initLayout();
	}

	private void initLayout() {
		add(AttributeModifier.append("class", new Model<String>(){
			private static final long serialVersionUID = 1L;

			@Override
			public String getObject() {
				if (isOn()) {
					return cssClassOn;
				} else {
					return cssClassOff;
				}
			}
        }));
	}

	public abstract boolean isOn();

}
