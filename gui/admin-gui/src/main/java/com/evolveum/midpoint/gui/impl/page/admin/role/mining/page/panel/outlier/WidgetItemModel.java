/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.Serializable;

public class WidgetItemModel implements Serializable {

    private IModel<String> label;

    private IModel<String> value;


    public WidgetItemModel(IModel<String> label) {
        this(label, Model.of());
    }

    public WidgetItemModel(IModel<String> label, IModel<String> value) {
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
        return label;
    }

    public Component createDescriptionComponent(String id) {
       return new Label(id, () -> getLabel().getObject());
    }

    public Component createTitleComponent(String id) {
        return new WebMarkupContainer(id);
    }

    public Component createFooterComponent(String id) {
        return new WebMarkupContainer(id);
    }

    public VisibleBehaviour isVisible() {
        return null;
    }

    protected String replaceValueCssClass() {
        return null;
    }

    protected String replaceValueCssStyle(){
        return null;
    }

    protected String replaceDescriptionCssClass() {
        return null;
    }

    protected String replaceTitleCssClass() {
        return null;
    }

    protected String replaceFooterCssClass() {
        return null;
    }

}
