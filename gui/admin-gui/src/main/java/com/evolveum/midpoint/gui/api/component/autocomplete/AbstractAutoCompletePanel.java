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
package com.evolveum.midpoint.gui.api.component.autocomplete;

import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;

import com.evolveum.midpoint.web.component.prism.InputPanel;

/**
 * @author semancik
 *
 */
public abstract class AbstractAutoCompletePanel extends InputPanel {
	private static final long serialVersionUID = 1L;

	public AbstractAutoCompletePanel(String id) {
		super(id);
	}

	protected AutoCompleteSettings createAutoCompleteSettings() {
        AutoCompleteSettings autoCompleteSettings = new AutoCompleteSettings();
        autoCompleteSettings.setShowListOnEmptyInput(true);
        autoCompleteSettings.setShowListOnFocusGain(true);
        autoCompleteSettings.setMaxHeightInPx(200);
        autoCompleteSettings.setShowCompleteListOnFocusGain(true);
        return autoCompleteSettings;
	}
}
