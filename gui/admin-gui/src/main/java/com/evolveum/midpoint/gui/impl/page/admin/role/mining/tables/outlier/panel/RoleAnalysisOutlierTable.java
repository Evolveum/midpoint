/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel;

import static com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil.createDisplayType;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.*;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.TEXT_TRUNCATE;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel.PartitionObjectDto.buildPartitionObjectList;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.densityBasedColorOposite;
import static com.evolveum.midpoint.web.component.data.mining.RoleAnalysisCollapsableTablePanel.*;

import java.io.Serial;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.ProgressBarSecondStyle;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.PageRoleAnalysisCluster;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.RoleAnalysisPartitionOverviewPanel;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.LinkIconLabelIconPanel;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;

import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

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
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.mining.CollapsableContainerPanel;
import com.evolveum.midpoint.web.component.data.mining.RoleAnalysisCollapsableTablePanel;
import com.evolveum.midpoint.web.component.util.RoleMiningProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class RoleAnalysisOutlierTable extends BasePanel<AssignmentHolderType> {

    private static final String ID_DATATABLE = "datatable";
    private static final String DOT_CLASS = RoleAnalysisOutlierTable.class.getName() + ".";
    private static final String OP_PREPARE_OBJECTS = DOT_CLASS + "prepareObjects";

    PageBase pageBase;

    public RoleAnalysisOutlierTable(
            @NotNull String id,
            @NotNull PageBase pageBase,
            @NotNull LoadableDetachableModel<AssignmentHolderType> sessionModel) {
        super(id, sessionModel);

        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        Task task = pageBase.createSimpleTask(OP_PREPARE_OBJECTS);
        OperationResult result = task.getResult();

        List<PartitionObjectDto> partitionObjectDtoList = new ArrayList<>();
        if (getModel().getObject() instanceof RoleAnalysisSessionType sessionObject) {
            partitionObjectDtoList = buildPartitionObjectList(
                    roleAnalysisService, sessionObject, getLimit(), matchOutlierCategory(), task, result);
        } else if (getModel().getObject() instanceof RoleAnalysisClusterType clusterObject) {
            partitionObjectDtoList = buildPartitionObjectList(
                    roleAnalysisService, clusterObject, getLimit(), matchOutlierCategory(), task, result);
        }

        this.pageBase = pageBase;
        RoleMiningProvider<PartitionObjectDto> provider = new RoleMiningProvider<>(
                this, new ListModel<>(partitionObjectDtoList), true);

        provider.setSort(PartitionObjectDto.F_OUTLIER_PARTITION_SCORE, SortOrder.DESCENDING);

        RoleAnalysisCollapsableTablePanel<PartitionObjectDto> table = buildTableComponent(
                provider);
        table.getDataTable().setItemsPerPage(10);
        table.enableSavePageSize();

        add(table);
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

        initPartitionScoreColumn(columns);

        initAnomalyAccessColumn(columns);

        initExplanationColumn(columns);

        initLocationColumn(columns);

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

    private void initLocationColumn(@NotNull List<IColumn<PartitionObjectDto, String>> columns) {
        columns.add(new AbstractColumn<>(createStringResource("RoleAnalysis.title.panel.location")) {

            @Override
            public String getSortProperty() {
                return PartitionObjectDto.F_CLUSTER_LOCATION_NAME;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<PartitionObjectDto>> item, String componentId,
                    IModel<PartitionObjectDto> rowModel) {

                PartitionObjectDto modelObject = rowModel.getObject();
                String clusterLocationName = modelObject.getClusterLocationName();

                AjaxLinkPanel sessionLink = new AjaxLinkPanel(componentId, Model.of(clusterLocationName)) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        RoleAnalysisOutlierPartitionType partition = modelObject.getPartition();
                        ObjectReferenceType clusterRef = partition.getClusterRef();
                        if (clusterRef != null && clusterRef.getOid() != null) {
                            PageParameters parameters = new PageParameters();
                            parameters.add(OnePageParameterEncoder.PARAMETER, clusterRef.getOid());
                            getPageBase().navigateToNext(PageRoleAnalysisCluster.class, parameters);
                        }
                    }
                };

                sessionLink.setOutputMarkupId(true);
                sessionLink.add(AttributeModifier.append(STYLE_CSS, "max-width:130px"));
                sessionLink.add(AttributeModifier.append(CLASS_CSS, TEXT_TRUNCATE));
                item.add(sessionLink);
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(
                        componentId, createStringResource("RoleAnalysis.title.panel.location"));
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
                Model<String> explanationTranslatedModel = explainPartition(partition);
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
                PartitionObjectDto modelObject = model.getObject();
                RoleAnalysisOutlierPartitionType partition = modelObject.getPartition();

                List<DetectedAnomalyResult> detectedAnomalyResult = partition.getDetectedAnomalyResult();

                if (detectedAnomalyResult == null) {
                    cellItem.add(new Label(componentId, Model.of(0)));
                    return;
                }

                int anomalyCount = detectedAnomalyResult.size();
                LinkIconLabelIconPanel components = new LinkIconLabelIconPanel(componentId,
                        Model.of(String.valueOf(anomalyCount))) {
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

                            RoleAnalysisDetectedAnomalyTable detectedAnomalyTable = buildDetectedAnomalyTable(modelObject, partition);
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

    private static @NotNull RoleAnalysisDetectedAnomalyTable buildDetectedAnomalyTable(
            @NotNull PartitionObjectDto modelObject,
            RoleAnalysisOutlierPartitionType partition) {
        RoleAnalysisOutlierType outlierObject = modelObject.getOutlier();
        AnomalyObjectDto dto = new AnomalyObjectDto(outlierObject, partition, false);
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
                RoleAnalysisOutlierPartitionType partition = rowModel.getObject().getPartition();
                RoleAnalysisPartitionAnalysisType partitionAnalysis = partition.getPartitionAnalysis();
                if (partitionAnalysis == null) {
                    item.add(new EmptyPanel(componentId));
                    return;
                }

                Double overallConfidence = partitionAnalysis.getOverallConfidence();
                double finalOverallConfidence = overallConfidence != null ? overallConfidence : 0;
                initDensityProgressPanel(item, componentId, finalOverallConfidence);
            }

            @Override
            public Component getHeader(String componentId) {
                return new LabelWithHelpPanel(componentId,
                        createStringResource("RoleAnalysisOutlierTable.outlier.confidence")) {
//                    @Override
//                    protected IModel<String> getHelpModel() {
//                        return createStringResource("RoleAnalysisOutlierTable.outlier.confidence.help");
//                    }
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
                            DetailsPageUtil.dispatchToObjectDetailsPage(outlierObject.asPrismObject(), this);
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

    @Override
    public PageBase getPageBase() {
        return pageBase;
    }

    private static void initDensityProgressPanel(
            @NotNull Item<ICellPopulator<PartitionObjectDto>> cellItem,
            @NotNull String componentId,
            @NotNull Double density) {

        BigDecimal bd = new BigDecimal(Double.toString(density));
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        double pointsDensity = bd.doubleValue();

        String colorClass = densityBasedColorOposite(pointsDensity);

        ProgressBarSecondStyle progressBar = new ProgressBarSecondStyle(componentId) {

            @Override
            public boolean isInline() {
                return true;
            }

            @Override
            public double getActualValue() {
                return pointsDensity;
            }

            @Override
            public String getProgressBarColor() {
                return colorClass;
            }

            @Override
            public String getBarTitle() {
                return "";
            }
        };
        progressBar.setOutputMarkupId(true);
        cellItem.add(progressBar);
    }

    public Integer getLimit() {
        return null;
    }

    public OutlierCategoryType matchOutlierCategory() {
        return null;
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
