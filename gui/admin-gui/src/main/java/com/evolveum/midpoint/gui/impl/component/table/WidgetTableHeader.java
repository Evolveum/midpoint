/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.table;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

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
        Label label = new Label(ID_TITLE, Model.of(WebComponentUtil.getTranslatedPolyString(GuiDisplayTypeUtil.getLabel(getModelObject()))));
        label.setRenderBodyOnly(true);
        add(label);

        WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
        icon.add(AttributeAppender.append("class", Model.of(GuiDisplayTypeUtil.getIconCssClass(getModelObject()))));
        add(icon);
    }
}
