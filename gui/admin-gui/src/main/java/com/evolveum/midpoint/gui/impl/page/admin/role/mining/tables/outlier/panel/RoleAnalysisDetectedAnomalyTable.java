/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel;

import java.io.Serial;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.RoleAnalysisExplanationTabPanelPopup;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.RoleAnalysisSinglePartitionAnomalyResultTabPopup;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static com.evolveum.midpoint.gui.api.page.PageAdminLTE.createStringResourceStatic;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.explainAnomaly;

public class RoleAnalysisDetectedAnomalyTable extends BasePanel<AnomalyObjectDto> {
    private static final String ID_DATATABLE = "datatable";
    private static final String DOT_CLASS = RoleAnalysisDetectedAnomalyTable.class.getName() + ".";
    private static final String OP_PREPARE_OBJECTS = DOT_CLASS + "prepareObjects";

    public RoleAnalysisDetectedAnomalyTable(String id,
            IModel<AnomalyObjectDto> dto) {
        super(id, dto);
        createTable(RoleAnalysisDetectedAnomalyTable.this.getModelObject());
    }

    private void createTable(AnomalyObjectDto anomalyObjectDto) {
        MainObjectListPanel<RoleType> table = new MainObjectListPanel<>(ID_DATATABLE, RoleType.class, null) {

            @Contract(pure = true)
            @Override
            public @NotNull String getAdditionalBoxCssClasses() {
                return RoleAnalysisDetectedAnomalyTable.this.getAdditionalBoxCssClasses();
            }

            @Contract("_ -> new")
            @Override
            protected @NotNull @Unmodifiable List<Component> createToolbarButtonsList(String buttonId) {
                if (anomalyObjectDto.getCategory()
                        .equals(AnomalyObjectDto.AnomalyTableCategory.OUTLIER_OVERVIEW)) {
                    return List.of();
                }
                return List.of(RoleAnalysisDetectedAnomalyTable.this.createRefreshButton(buttonId));
            }

            @Override
            protected boolean hideFooterIfSinglePage() {
                return true;
            }

            @Override
            protected void addBasicActions(List<InlineMenuItem> menuItems) {
                //TODO TBD
            }

            @Override
            protected String getInlineMenuItemCssClass() {
                return "btn btn-default btn-sm flex-nowrap text-nowrap";
            }

            @Override
            protected IColumn<SelectableBean<RoleType>, String> createCheckboxColumn() {
                return null;
            }

            @Override
            protected boolean isPagingVisible() {
                if (anomalyObjectDto.getCategory() == AnomalyObjectDto.AnomalyTableCategory.OUTLIER_OVERVIEW) {
                    return false;
                }
                return super.isPagingVisible();
            }

            @Override
            protected @NotNull List<InlineMenuItem> createInlineMenu() {
                List<InlineMenuItem> menuItems = new ArrayList<>();
                AnomalyObjectDto.AnomalyTableCategory category = anomalyObjectDto.getCategory();
                if (category == AnomalyObjectDto.AnomalyTableCategory.PARTITION_ANOMALY) {
                    menuItems.add(RoleAnalysisDetectedAnomalyTable.this.createViewDetailsMenu(anomalyObjectDto));
                    return menuItems;
                }

                if (category == AnomalyObjectDto.AnomalyTableCategory.OUTLIER_OVERVIEW) {
                    menuItems.add(RoleAnalysisDetectedAnomalyTable.this.createViewDetailsPeerGroupMenu(anomalyObjectDto));
                    menuItems.add(RoleAnalysisDetectedAnomalyTable.this.createViewDetailsAccessAnalysisMenu(anomalyObjectDto));
                    return menuItems;
                }
                return menuItems;

            }

            @Contract(pure = true)
            @Override
            protected UserProfileStorage.@Nullable TableId getTableId() {
                return null;
            }

            @Override
            protected boolean isHeaderVisible() {
                return false;
            }

            @Override
            protected boolean isCreateNewObjectVisible() {
                return false;
            }

            @Override
            protected @NotNull ISelectableDataProvider<SelectableBean<RoleType>> createProvider() {
                Task simpleTask = getPageBase().createSimpleTask(OP_PREPARE_OBJECTS);
                OperationResult result = simpleTask.getResult();
                return anomalyObjectDto.buildProvider(RoleAnalysisDetectedAnomalyTable.this, getPageBase(),
                        simpleTask, result);
            }

            @Override
            protected IColumn<SelectableBean<RoleType>, String> createNameColumn(IModel<String> displayModel,
                    GuiObjectColumnType customColumn,
                    ExpressionType expression) {
                var customization = new GuiObjectColumnType();
                customization.beginDisplay();
                customization.getDisplay().setCssClass("text-nowrap");
                var customDisplayModel = createStringResourceStatic("RoleAnalysisOutlierTable.anomaly.access");
                return super.createNameColumn(customDisplayModel, customization, expression);
            }

            @Override
            protected @NotNull List<IColumn<SelectableBean<RoleType>, String>> createDefaultColumns() {
                List<IColumn<SelectableBean<RoleType>, String>> columns = new ArrayList<>();

                IColumn<SelectableBean<RoleType>, String> column;

                if (RoleAnalysisDetectedAnomalyTable.this.getModelObject().isPartitionCountVisible) {
                    column = new AbstractExportableColumn<>(createStringResource("")) {

                        @Override
                        public IModel<?> getDataModel(IModel<SelectableBean<RoleType>> iModel) {
                            return null;
                        }

                        @Override
                        public Component getHeader(String componentId) {
                            return new Label(componentId,
                                    createStringResource("RoleAnalysisDetectedAnomalyTable.header.identification.title"));
                        }

                        @Override
                        public void populateItem(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
                                String componentId, IModel<SelectableBean<RoleType>> model) {
                            SelectableBean<RoleType> object = model.getObject();
                            RoleType role = object.getValue();
                            String oid = role.getOid();
                            int partitionCount = anomalyObjectDto.getPartitionCount(oid);
                            Label label = new Label(componentId, String.valueOf(partitionCount));
                            label.setOutputMarkupId(true);
                            cellItem.add(label);
                        }

                        @Override
                        public boolean isSortable() {
                            return false;
                        }

                    };
                    columns.add(column);
                }

                column = new AbstractExportableColumn<>(createStringResource("")) {

                    @Override
                    public IModel<?> getDataModel(IModel<SelectableBean<RoleType>> iModel) {
                        return null;
                    }

                    @Override
                    public Component getHeader(String componentId) {
                        return new Label(componentId, createStringResource(
                                "RoleAnalysisDetectedAnomalyTable.header.average.confidence.title"));
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleType>> model) {
                        SelectableBean<RoleType> object = model.getObject();
                        RoleType role = object.getValue();
                        String oid = role.getOid();

                        double anomalyScore = anomalyObjectDto.getAnomalyScore(oid);
                        BigDecimal bd = new BigDecimal(Double.toString(anomalyScore));
                        bd = bd.setScale(2, RoundingMode.HALF_UP);
                        double roundedAnomalyScore = bd.doubleValue();
                        Label anomalyScorePanel = new Label(componentId, roundedAnomalyScore + "%");
                        anomalyScorePanel.setOutputMarkupId(true);
                        cellItem.add(anomalyScorePanel);
                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                };
                columns.add(column);

                column = new AbstractExportableColumn<>(
                        createStringResource("RoleAnalysisOutlierTable.anomaly.reason")) {

                    @Override
                    public IModel<?> getDataModel(IModel<SelectableBean<RoleType>> iModel) {
                        return Model.of("");
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleType>> model) {
                        SelectableBean<RoleType> object = model.getObject();
                        RoleType role = object.getValue();
                        String oid = role.getOid();

                        DetectedAnomalyResult anomalyResult = anomalyObjectDto.getAnomalyResult(oid);
                        Model<String> explainAnomaly = explainAnomaly(anomalyResult);
                        cellItem.add(new Label(componentId, explainAnomaly));
                    }

                    @Override
                    public String getCssClass() {
                        return "";
                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                    @Override
                    public Component getHeader(String componentId) {
                        return new LabelWithHelpPanel(componentId,
                                createStringResource("RoleAnalysisOutlierTable.anomaly.reason")) {
                            @Override
                            protected IModel<String> getHelpModel() {
                                return createStringResource("RoleAnalysisOutlierTable.anomaly.reason.help");
                            }
                        };
                    }
                };
                columns.add(column);
                return columns;
            }
        };

        table.setOutputMarkupId(true);
        add(table);
    }

    private @NotNull AjaxIconButton createRefreshButton(String buttonId) {
        AjaxIconButton refreshIcon = new AjaxIconButton(buttonId, new Model<>(GuiStyleConstants.CLASS_RECONCILE),
                createStringResource("MainObjectListPanel.refresh")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                //onRefresh(target);
            }
        };
        refreshIcon.add(AttributeModifier.append("class", "btn btn-default btn-sm"));
        return refreshIcon;
    }

    @Override
    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    @Contract("_ -> new")
    private @NotNull InlineMenuItem createViewDetailsMenu(AnomalyObjectDto anomalyObjectDto) {

        return new ButtonInlineMenuItem(
                createStringResource("RoleAnalysisDetectedAnomalyTable.inline.view.details.title")) {

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_SEARCH);
            }

            @Override
            public boolean isLabelVisible() {
                return true;
            }

            public InlineMenuItemAction initAction() {
                //TODO check models think about the logic
                return new ColumnMenuAction<SelectableBean<RoleType>>() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        RoleType role = getRowModel().getObject().getValue();
                        String oid = role.getOid();

                        AnomalyObjectDto.AnomalyPartitionMap anomalyResult = anomalyObjectDto.getAnomalyPartitionMap(oid);
                        if (anomalyResult == null) {
                            return;
                        }

                        RoleAnalysisSinglePartitionAnomalyResultTabPopup detailsPanel =
                                new RoleAnalysisSinglePartitionAnomalyResultTabPopup(
                                        ((PageBase) getPage()).getMainPopupBodyId(),
                                        Model.of(anomalyResult.associatedPartition()),
                                        Model.of(anomalyResult.anomalyResult()),
                                        Model.of(anomalyObjectDto.getOutlier()));
                        ((PageBase) getPage()).showMainPopup(detailsPanel, target);

                    }
                };
            }

            @Override
            public boolean isMenuHeader() {
                return false;
            }
        };

    }

    @Contract("_ -> new")
    private @NotNull InlineMenuItem createViewDetailsPeerGroupMenu(AnomalyObjectDto anomalyObjectDto) {

        return new ButtonInlineMenuItem(
                createStringResource("RoleAnalysisDetectedAnomalyTable.createViewDetailsPeerGroupMenu.title")) {

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder("mt-1 fa fa-qrcode");
            }

            @Override
            public boolean isLabelVisible() {
                return true;
            }

            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<RoleType>>() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        SelectableBean<RoleType> object = getRowModel().getObject();

                        RoleAnalysisExplanationTabPanelPopup components = new RoleAnalysisExplanationTabPanelPopup(
                                getPageBase().getMainPopupBodyId(),
                                Model.of(anomalyObjectDto),
                                object);

                        getPageBase().showMainPopup(components, target);
                    }
                };
            }

            @Override
            public IModel<Boolean> getVisible() {
                return resolveButtonVisibilityByCategory(
                        this, anomalyObjectDto, OutlierDetectionExplanationCategoryType.UNUSUAL_ACCESS);
            }

            @Override
            public boolean isMenuHeader() {
                return false;
            }
        };
    }

    @Contract("_ -> new")
    private @NotNull InlineMenuItem createViewDetailsAccessAnalysisMenu(AnomalyObjectDto anomalyObjectDto) {

        return new ButtonInlineMenuItem(
                createStringResource("RoleAnalysisDetectedAnomalyTable.createViewDetailsAccessAnalysisMenu.title")) {

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder("fa fa-tags");
            }

            @Override
            public boolean isLabelVisible() {
                return true;
            }

            public InlineMenuItemAction initAction() {
                //TODO check models think about the logic
                return new ColumnMenuAction<SelectableBean<RoleType>>() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        SelectableBean<RoleType> object = getRowModel().getObject();

                        RoleAnalysisExplanationTabPanelPopup components = new RoleAnalysisExplanationTabPanelPopup(
                                getPageBase().getMainPopupBodyId(),
                                Model.of(anomalyObjectDto),
                                object) {
                            @Override
                            public TabType defaultTab() {
                                return TabType.VIEW_DETAILS_ACCESS_ANALYSIS;
                            }
                        };

                        getPageBase().showMainPopup(components, target);

                    }
                };
            }

            @Override
            public IModel<Boolean> getVisible() {
                return resolveButtonVisibilityByCategory(
                        this, anomalyObjectDto, OutlierDetectionExplanationCategoryType.IRREGULAR_ATTRIBUTES);
            }

            @Override
            public boolean isMenuHeader() {
                return false;
            }
        };
    }

    @SuppressWarnings("unchecked")
    public Model<Boolean> resolveButtonVisibilityByCategory(
            @NotNull ButtonInlineMenuItem buttonInlineMenuItem,
            @NotNull AnomalyObjectDto anomalyObjectDto,
            @NotNull OutlierDetectionExplanationCategoryType requiredCategory) {
        IModel<SelectableBean<RoleType>> rowModel = ((ColumnMenuAction<SelectableBean<RoleType>>) buttonInlineMenuItem.getAction()).getRowModel();
        if (rowModel != null && rowModel.getObject() != null && rowModel.getObject().getValue() != null) {
            RoleType role = rowModel.getObject().getValue();
            List<OutlierDetectionExplanationType> explanation = anomalyObjectDto.getExplanation(role.getOid());
            if (explanation == null || explanation.isEmpty()) {
                return Model.of(false);
            }

            for (OutlierDetectionExplanationType item : explanation) {
                List<OutlierDetectionExplanationCategoryType> itemCategory = item.getCategory();
                boolean equals = itemCategory.contains(requiredCategory);
                if (equals) {
                    return Model.of(true);
                }
            }

        }
        return Model.of(false);
    }

    public String getAdditionalBoxCssClasses() {
        return " m-0 elevation-0";
    }
}
