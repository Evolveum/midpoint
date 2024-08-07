/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.outlier.panel;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierObjectModel.generateUserOutlierResultModel;

import java.io.Serial;
import java.io.Serializable;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.component.menu.listGroup.ListGroupMenuItem;
import com.evolveum.midpoint.gui.impl.component.menu.listGroup.MenuItemLinkPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierHeaderResultPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierItemResultPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierObjectModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierResultPanel;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOutlierPartitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOutlierType;

public class OutlierOverviewItemPanel<T extends Serializable>
        extends BasePanel<ListGroupMenuItem<T>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_LINK = "link";

    private final IModel<RoleAnalysisOutlierPartitionType> partitionModel;
    private final IModel<RoleAnalysisOutlierType> outlierModel;

    public OutlierOverviewItemPanel(@NotNull String id,
            @NotNull IModel<ListGroupMenuItem<T>> model,
            @NotNull IModel<RoleAnalysisOutlierPartitionType> selectionModel,
            @NotNull IModel<RoleAnalysisOutlierType> outlierModel) {
        super(id, model);

        this.partitionModel = selectionModel;
        this.outlierModel = outlierModel;
        initLayout();
        if (isActive()) {
            onClickPerformed(null, getDetailsPanelComponent());
        }
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
                OutlierOverviewItemPanel.this.onClickPerformed(target, getDetailsPanelComponent());
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
        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
        Task task = getPageBase().createSimpleTask("loadOutlierDetails");

        RoleAnalysisOutlierType outlier = getOutlierModel().getObject();
        RoleAnalysisOutlierPartitionType partition = getPartitionModel().getObject();

        //TODO!
        OutlierObjectModel outlierObjectModel = generateUserOutlierResultModel(roleAnalysisService, outlier,
                task, task.getResult(), partition, getPageBase());

        if (outlierObjectModel == null) {
            return new WebMarkupContainer(id);
        }

        OutlierResultPanel detailsPanel = loadOutlierResultPanel(outlierObjectModel, id);
        detailsPanel.setOutputMarkupId(true);
        return detailsPanel;
    }

    @NotNull
    private OutlierResultPanel loadOutlierResultPanel(@NotNull OutlierObjectModel outlierObjectModel, @NotNull String id) {
        String outlierName = outlierObjectModel.getOutlierName();
        double outlierConfidence = outlierObjectModel.getOutlierConfidence();
        String outlierDescription = outlierObjectModel.getOutlierDescription();
        String timeCreated = outlierObjectModel.getTimeCreated();

        return new OutlierResultPanel(
                id,
                Model.of("Analyzed members details panel")) {

            @Override
            protected boolean isBodyTitleVisible() {
                return false;
            }

            @Override
            protected boolean isBodySubtitleVisible() {
                return false;
            }

            @Override
            public @NotNull Component getCardHeaderBody(String componentId) {
                OutlierHeaderResultPanel components = new OutlierHeaderResultPanel(componentId, outlierName,
                        outlierDescription, String.valueOf(outlierConfidence), timeCreated);
                components.setOutputMarkupId(true);
                return components;
            }

            @Override
            public Component getCardBodyComponent(String componentId) {
                RepeatingView cardBodyComponent = (RepeatingView) super.getCardBodyComponent(componentId);
                outlierObjectModel.getOutlierItemModels().forEach(outlierItemModel -> {
                    OutlierItemResultPanel component = new OutlierItemResultPanel(cardBodyComponent.newChildId(), outlierItemModel) {
                        @Contract(pure = true)
                        @Override
                        protected @NotNull String getItemBoxCssStyle() {
                            return "height:150px;";
                        }

                        @Contract(pure = true)
                        @Override
                        protected @NotNull String getItemBocCssClass() {
                            return "small-box bg-white";
                        }

                        @Contract(pure = true)
                        @Override
                        protected @NotNull String getLinkCssClass() {
                            return "";
                        }

                        @Override
                        protected String getInitialCssClass() {
                            return "col-3";
                        }
                    };

                    cardBodyComponent.add(component);
                });
                return cardBodyComponent;
            }

        };
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
