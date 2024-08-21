/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.outlier.panel;

import java.io.Serial;
import java.io.Serializable;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.component.menu.listGroup.ListGroupMenuItem;
import com.evolveum.midpoint.gui.impl.component.menu.listGroup.MenuItemLinkPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.RoleAnalysisPartitionOverviewPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOutlierPartitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOutlierType;

public class PartitionOverviewItemPanel<T extends Serializable>
        extends BasePanel<ListGroupMenuItem<T>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_LINK = "link";

    private final IModel<RoleAnalysisOutlierPartitionType> partitionModel;
    private final IModel<RoleAnalysisOutlierType> outlierModel;

    public PartitionOverviewItemPanel(@NotNull String id,
            @NotNull IModel<ListGroupMenuItem<T>> model,
            @NotNull IModel<RoleAnalysisOutlierPartitionType> selectionModel,
            @NotNull IModel<RoleAnalysisOutlierType> outlierModel) {
        super(id, model);

        this.partitionModel = selectionModel;
        this.outlierModel = outlierModel;
        initLayout();
    }

    public boolean isActive() {
        return false;
    }

    private void initLayout() {
        add(AttributeAppender.append("class", () -> getModelObject().isOpen() ? "open" : null));
        MenuItemLinkPanel<?> link = new MenuItemLinkPanel<>(ID_LINK, getModel(), 0) {
            @Override
            protected boolean isChevronLinkVisible() {
                return false;
            }

            @SuppressWarnings("rawtypes")
            @Override
            protected void onClickPerformed(AjaxRequestTarget target, ListGroupMenuItem item) {
                PartitionOverviewItemPanel.this.onClickPerformed(target, getDetailsPanelComponent());
            }
        };
        add(link);
    }

    protected void onClickPerformed(@NotNull AjaxRequestTarget target, @NotNull Component panelComponent) {
        dispatchComponent(target, panelComponent);
    }

    private void dispatchComponent(@NotNull AjaxRequestTarget target, @NotNull Component component) {
        component.replaceWith(buildOutlierOverviewPanel(component.getId()));
        target.add(getDetailsPanelComponent());
    }

    private @NotNull Component buildOutlierOverviewPanel(@NotNull String id) {
        Component detailsPanel = loadOutlierResultPanel(id);
        detailsPanel.setOutputMarkupId(true);
        return detailsPanel;
    }

    @NotNull
    private Component loadOutlierResultPanel(@NotNull String id) {
        return new RoleAnalysisPartitionOverviewPanel(id, getPartitionModel(), getOutlierModel());
    }

    protected @NotNull Component getDetailsPanelComponent() {
        return getPageBase().get("form").get("panel");
    }

    public IModel<RoleAnalysisOutlierPartitionType> getPartitionModel() {
        return partitionModel;
    }

    public IModel<RoleAnalysisOutlierType> getOutlierModel() {
        return outlierModel;
    }

}
