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
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

public class RoleAnalysisHeaderWithWidgetsPanel extends BasePanel<List<WidgetItemModel>> {

    private static final String ID_HEADER = "header";
    private static final String ID_WIDGETS = "widgets";

    public RoleAnalysisHeaderWithWidgetsPanel(
            @NotNull String id,
            @NotNull IModel<List<WidgetItemModel>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initHeader();
        initWidgets();
    }

    private void initHeader() {
        Component headerComponent = getHeaderComponent(ID_HEADER);
        headerComponent.setOutputMarkupId(true);
        add(headerComponent);
    }

    protected Component getHeaderComponent(String id) {
        WebMarkupContainer components = new WebMarkupContainer(id);
        components.add(new VisibleBehaviour(() -> false));
        return components;
    }

    private void initWidgets() {
        RoleAnalysisWidgetsPanel components = new RoleAnalysisWidgetsPanel(ID_WIDGETS, getModel()) {
            @Contract(pure = true)
            @Override
            protected @NotNull String replaceWidgetCssClass() {
                return RoleAnalysisHeaderWithWidgetsPanel.this.replaceWidgetCssClass();
            }
        };
        components.setOutputMarkupId(true);
        add(components);
    }

    protected @NotNull String replaceWidgetCssClass() {
        return "col-4 mb-3";
    }

}
