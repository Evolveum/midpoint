/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import java.io.Serializable;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.model.Model;

/**
 * Created by Viliam Repan (lazyman).
 */
public class DetailsTableItem implements Serializable {

    private IModel<String> label;

    private IModel<String> value;

    // for representation value panel before label panel
    private boolean valueComponentBeforeLabel = false;

    public DetailsTableItem(IModel<String> label) {
        this(label, Model.of());
    }

    public DetailsTableItem(IModel<String> label, IModel<String> value) {
        this.label = label;
        this.value = value;
    }

    public IModel<String> getLabel() {
        return label;
    }

    public IModel<String> getValue() {
        return value;
    }

    public Component createValueComponent(String id) {
        Label label = new Label(id, value);
//        label.setRenderBodyOnly(true);
        return label;
    }

    public Component createLabelComponent(String id) {
       return new Label(id, () -> getLabel().getObject());
    }

    public VisibleBehaviour isVisible() {
        return null;
    }

    public boolean isValueComponentBeforeLabel() {
        return valueComponentBeforeLabel;
    }

    public void setValueComponentBeforeLabel(boolean valueComponentBeforeLabel) {
        this.valueComponentBeforeLabel = valueComponentBeforeLabel;
    }
}
