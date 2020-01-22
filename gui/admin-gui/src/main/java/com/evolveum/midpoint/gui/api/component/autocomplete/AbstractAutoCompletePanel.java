/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
