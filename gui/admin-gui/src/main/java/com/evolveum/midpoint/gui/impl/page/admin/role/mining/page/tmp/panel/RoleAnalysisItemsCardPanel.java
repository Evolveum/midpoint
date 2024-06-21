/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel;

import java.io.Serial;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.util.TooltipBehavior;

public class RoleAnalysisItemsCardPanel extends BasePanel<String> {

    @Serial private static final long serialVersionUID = 1L;
    private static final String ID_CONTAINER = "container";
    private static final String ID_ITEMS = "items";
    private static final String ID_HEADER_ITEMS = "headerItems";

    public RoleAnalysisItemsCardPanel(String id, IModel<String> model) {
        super(id, model);
        initLayout();
    }

    private void initLayout() {

        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.add(AttributeModifier.replace("style", getContainerStyle()));
        container.add(AttributeModifier.replace("class", getContainerCssClass()));
        add(container);

        RepeatingView headerItems = new RepeatingView(ID_HEADER_ITEMS);
        headerItems.add(AttributeModifier.replace("style", getHeaderItemsStyle()));
        headerItems.add(AttributeModifier.replace("class", getHeaderItemsCssClass()));
        headerItems.add(AttributeModifier.replace("title", getModel()));
        headerItems.add(new TooltipBehavior());
        headerItems.setOutputMarkupId(true);
        container.add(headerItems);
        appendHeaderItems(headerItems);

        RepeatingView items = new RepeatingView(ID_ITEMS);
        items.add(AttributeModifier.replace("style", getItemsStyle()));
        items.add(AttributeModifier.replace("class", getItemsCssClass()));
        items.add(AttributeModifier.replace("title", getModel()));
        items.add(new TooltipBehavior());
        items.setOutputMarkupId(true);
        container.add(items);
        appendItems(items);
    }

    protected void appendHeaderItems(RepeatingView items) {

    }
    protected void appendItems(RepeatingView items) {

    }

    private String getContainerCssClass() {
        return "";
    }

    private String getContainerStyle() {
        return "max-height:900px";
    }

    private String getItemsCssClass() {
        return "info-box";
    }

    private String getItemsStyle() {
        return "margin-bottom:0.55rem";
    }

    protected String getHeaderItemsStyle() {
        return "";
    }
    protected String getHeaderItemsCssClass() {
        return "";
    }
}
