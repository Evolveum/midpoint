/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.table;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

public class WidgetTableHeader extends BasePanel<DisplayType> {

    private static final String ID_TITLE = "title";
    private static final String ID_ICON = "icon";

    public WidgetTableHeader(String id, IModel<DisplayType> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        Label label = new Label(ID_TITLE, getModelObject().getLabel());
        label.setRenderBodyOnly(true);
        add(label);

        WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
        icon.add(AttributeAppender.append("class", getModelObject().getIcon().getCssClass()));
        add(icon);
    }
}
