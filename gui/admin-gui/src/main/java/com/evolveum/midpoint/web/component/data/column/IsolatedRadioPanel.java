/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.data.column;

import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.Serializable;

/**
 * Panel that renders a single radio bound to the enclosing RadioGroup's model.
 * Must live under a RadioGroup<T>. The group model holds the selected row (T).
 */
public class IsolatedRadioPanel<T extends Serializable> extends Panel {

    private final IModel<T> optionModel;
    private final IModel<Boolean> enabled;

    public IsolatedRadioPanel(String id, IModel<T> optionModel, IModel<Boolean> enabled) {
        super(id);
        this.optionModel = optionModel;
        this.enabled = enabled != null ? enabled : Model.of(true);
        setOutputMarkupId(true);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        Radio<T> radio = new Radio<>("radio", optionModel);
        radio.setEnabled(Boolean.TRUE.equals(enabled.getObject()));
        radio.setOutputMarkupId(true);
        add(radio);
    }

    public Radio<?> getPanelComponent() {
        return (Radio<?>) get("radio");
    }

}
