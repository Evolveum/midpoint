/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class MetricValuePanel extends BasePanel<String> {

    private static final String ID_CONTAINER = "container";
    private static final String ID_TITLE = "title";
    private static final String ID_VALUE = "value";
    private static final String ID_HELP_CONTAINER = "helpContainer";
    private static final String ID_HELP = "help";

    public MetricValuePanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        container.add(AttributeModifier.replace("class", getContainerCssClass()));
        add(container);

        Component title = getTitleComponent(ID_TITLE);
        title.setOutputMarkupId(true);
        container.add(title);

        Component value = getValueComponent(ID_VALUE);
        value.setOutputMarkupId(true);
        container.add(value);

        WebMarkupContainer helpContainer = new WebMarkupContainer(ID_HELP_CONTAINER);
        helpContainer.setOutputMarkupId(true);
        helpContainer.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(getHelpModel().getObject())));
        container.add(helpContainer);

        Label help = new Label(ID_HELP);
        IModel<String> helpModel = getHelpModel();
        help.add(AttributeModifier.replace("data-original-title", createStringResource(
                helpModel.getObject() != null ? helpModel.getObject() : "")));
        help.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(helpModel.getObject())));
        help.setOutputMarkupId(true);
        helpContainer.add(help);
    }

    protected Component getTitleComponent(String id) {
        return new WebMarkupContainer(id);
    }

    protected Component getValueComponent(String id) {
        return new WebMarkupContainer(id);
    }

    protected IModel<String> getHelpModel() {
        return Model.of("");
    }

    protected String getContainerCssClass() {
        return "col p-0";
    }

}
