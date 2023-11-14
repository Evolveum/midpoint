/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.autocomplete;

import java.io.Serial;
import java.time.Duration;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.ThrottlingSettings;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;

import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * @author semancik
 */
public abstract class AbstractAutoCompletePanel extends InputPanel {
    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_ICON_BUTTON = "iconButton";

    public AbstractAutoCompletePanel(String id) {
        super(id);
        AjaxLink<String> showChoices = new AjaxLink<>(ID_ICON_BUTTON) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                target.focusComponent(getBaseFormComponent());
            }

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                AutoCompleteSettings settings = createAutoCompleteSettings();
                attributes.setThrottlingSettings(new ThrottlingSettings(Duration.ofMillis(settings.getThrottleDelay()), true));
            }
        };
        showChoices.add(new VisibleBehaviour(this::isShowChoicesVisible));
        add(showChoices);
    }

    protected boolean isShowChoicesVisible() {
        return true;
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
