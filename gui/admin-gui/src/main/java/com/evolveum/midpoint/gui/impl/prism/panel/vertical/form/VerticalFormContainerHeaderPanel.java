/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel.vertical.form;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.ResourceTilePanel;
import com.evolveum.midpoint.gui.impl.prism.panel.*;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import java.util.Collections;
import java.util.List;

/**
 * @author lskublik
 */
public abstract class VerticalFormContainerHeaderPanel<C extends Containerable> extends PrismContainerHeaderPanel<C, PrismContainerWrapper<C>> {

    private static final String ID_ICON = "icon";

    public VerticalFormContainerHeaderPanel(String id, IModel<PrismContainerWrapper<C>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
        icon.add(AttributeAppender.append("class", () -> getIcon()));
        add(icon);

        add(new AjaxEventBehavior("onClick") {

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                onExpandClick(target);
            }
        });
    }

    @Override
    protected boolean isHelpTextVisible() {
        return false;
    }

    @Override
    protected Component createTitle(IModel<String> label) {
        IModel<String> titleModel = getTitleModel();
        if (titleModel == null) {
            titleModel = label;
        }
        return super.createTitle(titleModel);
    }

    protected String getIcon() {
        return "";
    }

    protected IModel<String> getTitleModel() {
        return null;
    }
}
