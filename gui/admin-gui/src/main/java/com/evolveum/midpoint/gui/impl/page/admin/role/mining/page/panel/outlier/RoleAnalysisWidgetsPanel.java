/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.DetailsTableItem;

public class RoleAnalysisWidgetsPanel extends BasePanel<List<DetailsTableItem>> {

    private static final String ID_CONTAINER = "container";
    private static final String ID_HEADER_ITEMS = "header-items";
    private static final String ID_PANEL = "panelId";
    private static final String ID_LABEL = "label";
    private static final String ID_VALUE = "value";

    public RoleAnalysisWidgetsPanel(
            @NotNull String id,
            @NotNull IModel<List<DetailsTableItem>> model) {
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
        return new WebMarkupContainer(id);
    }

    private void initHeaderLayout(@NotNull WebMarkupContainer container) {

        ListView<DetailsTableItem> details = new ListView<>(ID_HEADER_ITEMS, getModel()) {

            @Override
            protected void populateItem(@NotNull ListItem<DetailsTableItem> item) {
                DetailsTableItem data = item.getModelObject();
                item.add(data.createLabelComponent(ID_LABEL));
                item.add(data.createValueComponent(ID_VALUE));

                if (data.isVisible() != null) {
                    item.add(data.isVisible());
                }
            }
        };
        details.setOutputMarkupId(true);
        container.add(details);
    }

}
