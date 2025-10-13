/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel;

import static com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil.createDisplayType;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.*;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.densityBasedColorOposite;
import static com.evolveum.midpoint.web.component.data.mining.RoleAnalysisCollapsableTablePanel.*;

import java.io.Serial;
import java.util.*;

import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.bar.RoleAnalysisInlineProgressBar;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.RoleAnalysisProgressBarDto;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.RoleAnalysisPartitionOverviewPanel;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.LinkIconLabelIconPanel;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.mining.CollapsableContainerPanel;
import com.evolveum.midpoint.web.component.data.mining.RoleAnalysisCollapsableTablePanel;
import com.evolveum.midpoint.web.component.util.RoleMiningProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class RoleAnalysisOutlierTable extends BasePanel<PartitionObjectDtos> {

    private static final String ID_DATATABLE = "datatable";
    private static final String DOT_CLASS = RoleAnalysisOutlierTable.class.getName() + ".";
    private static final String OP_EXPLAIN_PARTITION = DOT_CLASS + "explainPartition";
    private static final String OP_PREPARE_ANOMALY_OBJECT = DOT_CLASS + "prepareAnomalyObject";

    public RoleAnalysisOutlierTable(
            @NotNull String id,
            @NotNull IModel<PartitionObjectDtos> partitionModel) {
        super(id, partitionModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        RoleMiningProvider<PartitionObjectDto> provider = buildProvider();
        initTable(provider);
    }

    private void initTable(RoleMiningProvider<PartitionObjectDto> provider) {
        RoleAnalysisCollapsableTablePanel<PartitionObjectDto> table = buildTableComponent(provider);
        table.getDataTable().setItemsPerPage(10);
        table.enableSavePageSize();
        add(table);
    }

    private @NotNull RoleMiningProvider<PartitionObjectDto> buildProvider() {
        RoleMiningProvider<PartitionObjectDto> provider = new RoleMiningProvider<>(
                this, () -> getModelObject().partitionObjectDtoList, true);
        provider.setSort(PartitionObjectDto.F_OUTLIER_PARTITION_SCORE, SortOrder.DESCENDING);
        return provider;
    }

    private @NotNull RoleAnalysisCollapsableTablePanel<PartitionObjectDto> buildTableComponent(
            RoleMiningProvider<PartitionObjectDto> provider) {
        RoleAnalysisCollapsableTablePanel<PartitionObjectDto> table = new RoleAnalysisCollapsableTablePanel<>(
                ID_DATATABLE, provider, initColumns()) {

            @Contract(pure = true)
            @Override
            public @NotNull String getAdditionalBoxCssClasses() {
                return " m-0";
            }

            @Override
            public boolean isShowAsCard() {
                return RoleAnalysisOutlierTable.this.isShowAsCard();
            }

            @Override
            protected boolean isPagingVisible() {
                return isPaginationVisible();
            }

            @Override
            protected boolean visibleFooter(ISortableDataProvider<PartitionObjectDto, String> provider, int pageSize) {
                if (RoleAnalysisOutlierTable.this.hideFooter()) {
                    return false;
                }

                return super.visibleFooter(provider, pageSize);
            }
        };
        table.setOutputMarkupId(true);
        return table;
    }

    public boolean hideFooter() {
        return false;
    }

    public List<IColumn<PartitionObjectDto, String>> initColumns() {

        List<IColumn<PartitionObjectDto, String>> columns = new ArrayList<>();

        initIconColumn(columns);

        initNameColumn(columns);

        if (isCategoryVisible()) {
            initCategoryColumn(columns);
        }

        initExplanationColumn(columns);

        initAnomalyAccessColumn(columns);

        initPartitionScoreColumn(columns);

        initActionColumn(columns);

        return columns;
    }

    private void initCategoryColumn(@NotNull List<IColumn<PartitionObjectDto, String>> columns) {
        columns.add(new AbstractColumn<>(createStringResource("RoleAnalysisOutlierTable.outlier.category")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<PartitionObjectDto>> item, String componentId,
                    IModel<PartitionObjectDto> rowModel) {
                RoleAnalysisOutlierPartitionType partition = rowModel.getObject().getPartition();
                RoleAnalysisPartitionAnalysisType partitionAnalysis = partition.getPartitionAnalysis();
                OutlierCategoryType outlierCategory = partitionAnalysis.getOutlierCategory();
                OutlierNoiseCategoryType outlierNoiseCategory = outlierCategory.getOutlierNoiseCategory();
                String category = outlierNoiseCategory != null
                        ? outlierNoiseCategory.value().replace("_", " ")
                        : "N/A";

                Label statusBar = new Label(componentId, Model.of(category));
                statusBar.add(AttributeModifier.append(CLASS_CSS,
                        "badge bg-transparent-blue border border-primary text-primary text-uppercase"));
                statusBar.setOutputMarkupId(true);
                item.add(statusBar);
            }

            @Override
            public Component getHeader(String componentId) {
                return new LabelWithHelpPanel(componentId,
                        createStringResource("RoleAnalysisOutlierTable.outlier.category"));
            }

        });
    }

    private void initActionColumn(@NotNull List<IColumn<PartitionObjectDto, String>> columns) {
        columns.add(new AbstractColumn<>(createStringResource("")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<PartitionObjectDto>> item, String componentId,
                    IModel<PartitionObjectDto> rowModel) {

                CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                        GuiStyleConstants.CLASS_ICON_SEARCH, IconCssStyle.IN_ROW_STYLE);
                AjaxCompositedIconSubmitButton viewDetailsButton = new AjaxCompositedIconSubmitButton(
                        componentId,
                        iconBuilder.build(),
                        createStringResource("RoleAnalysis.title.panel.explore.partition.details")) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        RoleAnalysisOutlierPartitionType partition = rowModel.getObject().getPartition();
                        RoleAnalysisOutlierType outlier = rowModel.getObject().getOutlier();
                        RoleAnalysisPartitionOverviewPanel detailsPanel = new RoleAnalysisPartitionOverviewPanel(
                                ((PageBase) getPage()).getMainPopupBodyId(),
                                Model.of(partition),
                                Model.of(outlier));
                        ((PageBase) getPage()).showMainPopup(detailsPanel, target);
                    }

                    @Override
                    protected void onError(@NotNull AjaxRequestTarget target) {
                        target.add(((PageBase) getPage()).getFeedbackPanel());
                    }
                };
                viewDetailsButton.titleAsLabel(true);
                viewDetailsButton.setOutputMarkupId(true);
                viewDetailsButton.add(AttributeModifier.append(CLASS_CSS, "btn btn-default btn-sm text-nowrap"));
                viewDetailsButton.setOutputMarkupId(true);
                item.add(viewDetailsButton);
            }

            @Override
            public Component getHeader(String componentId) {
                return new EmptyPanel(componentId);
            }

        });
    }

    private void initExplanationColumn(@NotNull List<IColumn<PartitionObjectDto, String>> columns) {
        columns.add(new AbstractColumn<>(createStringResource("RoleAnalysisOutlierTable.outlier.explanation")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<PartitionObjectDto>> item, String componentId,
                    IModel<PartitionObjectDto> rowModel) {
                RoleAnalysisOutlierPartitionType partition = rowModel.getObject().getPartition();

                Task task = getPageBase().createSimpleTask(OP_EXPLAIN_PARTITION);
                OperationResult result = task.getResult();

                Model<String> explanationTranslatedModel = explainPartition(
                        getPageBase().getRoleAnalysisService(), partition, true, task, result);
                item.add(new Label(componentId, explanationTranslatedModel));
            }

            @Override
            public Component getHeader(String componentId) {
                return new LabelWithHelpPanel(componentId,
                        createStringResource("RoleAnalysisOutlierTable.outlier.explanation")) {
                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleAnalysisOutlierTable.outlier.explanation.help");
                    }
                };

            }

        });
    }

    private void initAnomalyAccessColumn(@NotNull List<IColumn<PartitionObjectDto, String>> columns) {
        columns.add(new AbstractColumn<>(
                createStringResource("")) {

            @Override
            public String getSortProperty() {
                return PartitionObjectDto.F_ANOMALY_ACCESS_COUNT;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public Component getHeader(String componentId) {
                return new LabelWithHelpPanel(componentId,
                        createStringResource("RoleAnalysisOutlierType.anomalies"));
            }

            @Override
            public void populateItem(Item<ICellPopulator<PartitionObjectDto>> cellItem, String componentId, IModel<PartitionObjectDto> model) {
                LinkIconLabelIconPanel components = new LinkIconLabelIconPanel(componentId,
                        new PropertyModel<>(model, PartitionObjectDto.F_ANOMALY_ACCESS_COUNT)) {
                    @Contract(pure = true)
                    @Override
                    protected @NotNull String getLinkContainerCssClass() {
                        return super.getLinkContainerCssClass() + " btn btn-outline-primary p-0 justify-content-around w-100";
                    }

                    @Contract(pure = true)
                    @Override
                    public @NotNull String getIconCssClass() {
                        return "fa fa-chart-bar";
                    }

                    @Contract(pure = true)
                    @Override
                    protected @NotNull String getSubComponentCssStyle() {
                        return "margin-top:2px;";
                    }

                    @Override
                    protected void onClickPerform(AjaxRequestTarget target) {
                        CollapsableContainerPanel collapseContainerUser = (CollapsableContainerPanel) cellItem
                                .findParent(Item.class).get(ID_FIRST_COLLAPSABLE_CONTAINER);
                        CollapsableContainerPanel collapseContainerRole = (CollapsableContainerPanel) cellItem
                                .findParent(Item.class).get(ID_SECOND_COLLAPSABLE_CONTAINER);

                        if (!collapseContainerUser.isExpanded()) {
                            CollapsableContainerPanel webMarkupContainerUser = new CollapsableContainerPanel(
                                    ID_FIRST_COLLAPSABLE_CONTAINER);
                            webMarkupContainerUser.setOutputMarkupId(true);
                            webMarkupContainerUser.add(AttributeModifier.replace("class", "collapse"));
                            webMarkupContainerUser.add(AttributeModifier.replace("style", "display: none;"));
                            webMarkupContainerUser.setExpanded(true);

                            PartitionObjectDto modelObject = model.getObject();
                            RoleAnalysisDetectedAnomalyTable detectedAnomalyTable = buildDetectedAnomalyTable(modelObject, modelObject.getPartition());
                            webMarkupContainerUser.add(detectedAnomalyTable);

                            collapseContainerUser.replaceWith(webMarkupContainerUser);
                            target.add(webMarkupContainerUser);
                        }
                        target.appendJavaScript(getCollapseScript(collapseContainerUser, collapseContainerRole));
                    }
                };

                components.setOutputMarkupId(true);
                cellItem.add(components);
            }

        });
    }

    private @NotNull RoleAnalysisDetectedAnomalyTable buildDetectedAnomalyTable(
            @NotNull PartitionObjectDto modelObject,
            @NotNull RoleAnalysisOutlierPartitionType partition) {

        RoleAnalysisOutlierType outlierObject = modelObject.getOutlier();
        Task task = getPageBase().createSimpleTask(OP_PREPARE_ANOMALY_OBJECT);
        OperationResult result = task.getResult();

        AnomalyObjectDto dto = new AnomalyObjectDto(
                getPageBase().getRoleAnalysisService(), outlierObject, partition, false, task, result);
        RoleAnalysisDetectedAnomalyTable detectedAnomalyTable = new RoleAnalysisDetectedAnomalyTable(ID_COLLAPSABLE_CONTENT, Model.of(dto)) {

            @Contract(pure = true)
            @Override
            public @NotNull String getAdditionalBoxCssClasses() {
                return " m-3 border";
            }

        };
        detectedAnomalyTable.setOutputMarkupId(true);
        return detectedAnomalyTable;
    }

    private void initPartitionScoreColumn(@NotNull List<IColumn<PartitionObjectDto, String>> columns) {
        columns.add(new AbstractColumn<>(createStringResource("RoleAnalysisOutlierTable.outlier.confidence")) {

            @Override
            public String getSortProperty() {
                return PartitionObjectDto.F_OUTLIER_PARTITION_SCORE;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<PartitionObjectDto>> item, String componentId,
                    IModel<PartitionObjectDto> rowModel) {
                initDensityProgressPanel(item, componentId, rowModel);
            }

            @Override
            public Component getHeader(String componentId) {
                return new LabelWithHelpPanel(componentId,
                        createStringResource("RoleAnalysisOutlierTable.outlier.confidence")) {
                };

            }

        });
    }

    private void initNameColumn(@NotNull List<IColumn<PartitionObjectDto, String>> columns) {
        columns.add(new AbstractColumn<>(createStringResource("RoleAnalysisOutlierTable.outlier.name")) {

            @Override
            public String getSortProperty() {
                return PartitionObjectDto.F_ASSOCIATED_OUTLIER_NAME;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<PartitionObjectDto>> item, String componentId,
                    IModel<PartitionObjectDto> rowModel) {
                RoleAnalysisOutlierType outlierObject = rowModel.getObject().getOutlier();
                if (outlierObject == null) {
                    item.add(new EmptyPanel(componentId));
                } else {
                    AjaxLinkPanel component = new AjaxLinkPanel(componentId, Model.of(outlierObject.getName())) {

                        @Serial private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            DetailsPageUtil.dispatchToObjectDetailsPage(RoleAnalysisOutlierType.class, outlierObject.getOid(), this, true);
                        }
                    };
                    component.setOutputMarkupId(true);
                    item.add(component);
                }
            }

            @Override
            public Component getHeader(String componentId) {
                return new LabelWithHelpPanel(componentId,
                        createStringResource("RoleAnalysisOutlierTable.outlier.name"));

            }

        });
    }

    private static void initIconColumn(@NotNull List<IColumn<PartitionObjectDto, String>> columns) {
        columns.add(new IconColumn<>(null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<PartitionObjectDto> rowModel) {

                return createDisplayType(GuiStyleConstants.CLASS_ICON_OUTLIER, "red", "");
            }
        });
    }

    private static void initDensityProgressPanel(
            @NotNull Item<ICellPopulator<PartitionObjectDto>> cellItem,
            @NotNull String componentId,
            @NotNull IModel<PartitionObjectDto> rowModel) {

        IModel<RoleAnalysisProgressBarDto> model = () -> {
            RoleAnalysisOutlierPartitionType partition = rowModel.getObject().getPartition();
            RoleAnalysisPartitionAnalysisType partitionAnalysis = partition.getPartitionAnalysis();
            if (partitionAnalysis == null) {
                return null;
            }

            Double overallConfidence = partitionAnalysis.getOverallConfidence();
            double finalOverallConfidence = overallConfidence != null ? overallConfidence : 0;

            String colorClass = densityBasedColorOposite(finalOverallConfidence);
            return new RoleAnalysisProgressBarDto(finalOverallConfidence, colorClass);
        };

        RoleAnalysisInlineProgressBar progressBar = new RoleAnalysisInlineProgressBar(componentId, model);
        progressBar.add(new VisibleBehaviour(() -> model.getObject() != null)); //TODO visibility? or default dto values?
        progressBar.setOutputMarkupId(true);
        cellItem.add(progressBar);
    }

    public boolean isPaginationVisible() {
        return true;
    }

    public boolean isShowAsCard() {
        return true;
    }

    public boolean isCategoryVisible() {
        return false;
    }
}
