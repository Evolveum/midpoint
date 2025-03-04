/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;

import java.io.Serial;
import java.io.Serializable;

public class RoleAnalysisValueLabelPanel<T extends Serializable> extends BasePanel<String> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CONTAINER = "container";
    private static final String ID_TITLE = "title";
    private static final String ID_VALUE = "value";


    public RoleAnalysisValueLabelPanel(String id) {
        super(id);
        initLayout();
    }

    protected void initLayout() {

        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        container.add(AttributeModifier.replace("class", getContainerCssClass()));
        container.add(AttributeModifier.replace("style", getContainerCssStyle()));
        add(container);

        Component titleComponent = getTitleComponent(ID_TITLE);
        titleComponent.setOutputMarkupId(true);
        container.add(titleComponent);

        Component valueComponent = getValueComponent(ID_VALUE);
        valueComponent.setOutputMarkupId(true);
        container.add(valueComponent);


    }

    protected Component getValueComponent(String id) {
        return new WebMarkupContainer(id);
    }

    protected Component getTitleComponent(String id) {
        return new WebMarkupContainer(id);
    }

    protected String getContainerCssClass() {
        return null;
    }

    protected String getContainerCssStyle() {
        return null;
    }
}
