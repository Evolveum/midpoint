/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier;

import java.util.List;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;

public class RoleAnalysisWidgetsPanel extends BasePanel<List<WidgetItemModel>> {

    private static final String ID_CONTAINER = "container";
    private static final String ID_HEADER_ITEMS = "header-items";
    private static final String ID_PANEL = "panelId";
    private static final String ID_TITLE = "title";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_VALUE = "value";
    private static final String ID_FOOTER = "footer";

    public RoleAnalysisWidgetsPanel(
            @NotNull String id,
            @NotNull IModel<List<WidgetItemModel>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        initHeaderLayout(container);

        Component component = getPanelComponent(ID_PANEL);
        component.setOutputMarkupId(true);
        container.add(component);
    }

    protected Component getPanelComponent(String id) {
        WebMarkupContainer components = new WebMarkupContainer(id);
        components.setOutputMarkupId(true);
        components.add(new VisibleBehaviour(() -> false));
        return components;
    }

    private void initHeaderLayout(@NotNull WebMarkupContainer container) {

        ListView<WidgetItemModel> details = new ListView<>(ID_HEADER_ITEMS, getModel()) {

            @Override
            protected void populateItem(@NotNull ListItem<WidgetItemModel> item) {
                WidgetItemModel data = item.getModelObject();
                Component titleComponent = data.createTitleComponent(ID_TITLE);
                titleComponent.add(AttributeAppender.replace("class", data.replaceTitleCssClass()));
                item.add(titleComponent);

                Component descriptionComponent = data.createDescriptionComponent(ID_DESCRIPTION);
                descriptionComponent.add(AttributeAppender.replace("class", data.replaceDescriptionCssClass()));
                item.add(descriptionComponent);
                Component valueComponent = data.createValueComponent(ID_VALUE);
                valueComponent.add(AttributeAppender.replace("class", data.replaceValueCssClass()));
                valueComponent.add(AttributeAppender.replace("style", data.replaceValueCssStyle()));
                item.add(valueComponent);

                Component footerComponent = data.createFooterComponent(ID_FOOTER);
                footerComponent.add(AttributeAppender.replace("class", data.replaceFooterCssClass()));
                item.add(footerComponent);

                if (data.isVisible() != null) {
                    item.add(data.isVisible());
                }
                item.add(AttributeAppender.replace("class", replaceWidgetCssClass()));
            }
        };
        details.setOutputMarkupId(true);
        container.add(details);
    }

    protected String replaceWidgetCssClass() {
        return "col mb-3";
    }

}
