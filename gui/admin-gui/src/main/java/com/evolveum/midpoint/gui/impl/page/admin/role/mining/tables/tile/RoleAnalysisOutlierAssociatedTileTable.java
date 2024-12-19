/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile;

import static com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil.createDisplayType;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.*;
import static com.evolveum.midpoint.gui.api.util.LocalizationUtil.translateMessage;

import java.io.Serial;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.PageRoleAnalysisCluster;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.Toggle;
import com.evolveum.midpoint.gui.api.component.TogglePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.tile.TileTablePanel;
import com.evolveum.midpoint.gui.impl.component.tile.ViewToggle;
import com.evolveum.midpoint.gui.impl.component.tile.mining.outlier.RoleAnalysisOutlierTileModel;
import com.evolveum.midpoint.gui.impl.component.tile.mining.outlier.RoleAnalysisOutlierTilePanel;
import com.evolveum.midpoint.gui.impl.component.tile.mining.session.RoleAnalysisSessionTileModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.PageRoleAnalysisOutlier;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.RoleAnalysisPartitionOverviewPanel;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.util.RoleMiningProvider;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class RoleAnalysisOutlierAssociatedTileTable extends BasePanel<List<RoleAnalysisOutlierType>> {

    private static final String ID_DATATABLE = "datatable";
    IModel<List<Toggle<ViewToggle>>> items;

    IModel<ObjectReferenceType> clusterRefModel;
    IModel<ObjectReferenceType> sessionRefModel;

    public RoleAnalysisOutlierAssociatedTileTable(
            @NotNull String id,
            @NotNull IModel<List<RoleAnalysisOutlierType>> outliers,
            @NotNull RoleAnalysisClusterType cluster) {
        super(id, outliers);
        this.clusterRefModel = new LoadableModel<>() {
            @Override
            protected ObjectReferenceType load() {
                return new ObjectReferenceType()
                        .oid(cluster.getOid())
                        .type(RoleAnalysisClusterType.COMPLEX_TYPE)
                        .targetName(cluster.getName());
            }
        };

        this.sessionRefModel = new LoadableModel<>() {
            @Override
            protected ObjectReferenceType load() {
                return cluster.getRoleAnalysisSessionRef();
            }
        };

        initItems();
    }

    public RoleAnalysisOutlierAssociatedTileTable(
            @NotNull String id,
            @NotNull IModel<List<RoleAnalysisOutlierType>> outliers,
            @NotNull RoleAnalysisSessionType session) {
        super(id, outliers);
        this.sessionRefModel = new LoadableModel<>() {
            @Override
            protected ObjectReferenceType load() {
                return new ObjectReferenceType()
                        .oid(session.getOid())
                        .type(RoleAnalysisSessionType.COMPLEX_TYPE)
                        .targetName(session.getName());
            }
        };
        initItems();
    }

    private void initItems() {
        this.items = new LoadableModel<>(false) {

            @Override
            protected @NotNull List<Toggle<ViewToggle>> load() {
                List<Toggle<ViewToggle>> list = new ArrayList<>();

                Toggle<ViewToggle> asList = new Toggle<>("fa-solid fa-table-list", null);

                ViewToggle object = getTable().getViewToggleModel().getObject();

                asList.setValue(ViewToggle.TABLE);
                asList.setActive(object == ViewToggle.TABLE);
                list.add(asList);

                Toggle<ViewToggle> asTile = new Toggle<>("fa-solid fa-table-cells", null);
                asTile.setValue(ViewToggle.TILE);
                asTile.setActive(object == ViewToggle.TILE);
                list.add(asTile);

                return list;
            }
        };
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(initTable(getModel()));
    }

    private @Nullable RoleAnalysisOutlierPartitionType getOutlierPartition(@NotNull RoleAnalysisOutlierType outlier) {
        List<RoleAnalysisOutlierPartitionType> outlierPartitions = outlier.getPartition();
        ObjectReferenceType clusterRef = getClusterRef();
        ObjectReferenceType sessionRef = getSessionRef();
        if (clusterRef != null) {
            String clusterOid = clusterRef.getOid();
            for (RoleAnalysisOutlierPartitionType partition : outlierPartitions) {
                ObjectReferenceType targetClusterRef = partition.getClusterRef();
                if (targetClusterRef != null
                        && targetClusterRef.getOid() != null
                        && targetClusterRef.getOid().equals(clusterOid)) {
                    return partition;
                }
            }
        } else if (sessionRef != null) {
            String sessionOid = sessionRef.getOid();
            for (RoleAnalysisOutlierPartitionType partition : outlierPartitions) {
                ObjectReferenceType targetSessionRef = partition.getTargetSessionRef();
                if (targetSessionRef != null
                        && targetSessionRef.getOid() != null
                        && targetSessionRef.getOid().equals(sessionOid)) {
                    return partition;
                }
            }

        }
        return null;
    }

    public TileTablePanel<RoleAnalysisOutlierTileModel<RoleAnalysisOutlierType>, RoleAnalysisOutlierType> initTable(
            @NotNull IModel<List<RoleAnalysisOutlierType>> outliers) {

        RoleMiningProvider<RoleAnalysisOutlierType> provider = new RoleMiningProvider<>(
                this, outliers, false);

        return new TileTablePanel<>(
                ID_DATATABLE,
                Model.of(ViewToggle.TABLE),
                UserProfileStorage.TableId.PANEL_OUTLIER_PROPERTIES) {

            @Override
            protected String getAdditionalBoxCssClasses() {
                return " m-0";
            }

            @Override
            protected List<IColumn<RoleAnalysisOutlierType, String>> createColumns() {
                return RoleAnalysisOutlierAssociatedTileTable.this.initColumns();
            }

            @Override
            protected WebMarkupContainer createTableButtonToolbar(String id) {
                Fragment fragment = new Fragment(id, "tableFooterFragment",
                        RoleAnalysisOutlierAssociatedTileTable.this);

                AjaxIconButton refreshTable = new AjaxIconButton("refreshTable",
                        Model.of("fa fa-refresh"),
                        Model.of()) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        onRefresh(ajaxRequestTarget);
                    }
                };

                refreshTable.setOutputMarkupId(true);
                refreshTable.add(AttributeModifier.replace("title",
                        createStringResource("Refresh table")));
                refreshTable.add(new TooltipBehavior());
                fragment.add(refreshTable);
                TogglePanel<ViewToggle> viewToggle = new TogglePanel<>("viewToggle", items) {

                    @Override
                    protected void itemSelected(AjaxRequestTarget target, IModel<Toggle<ViewToggle>> item) {
                        getViewToggleModel().setObject(item.getObject().getValue());
//                        RoleAnalysisSessionTileTable.this.getTable().refresh();
                        target.add(RoleAnalysisOutlierAssociatedTileTable.this);
                    }
                };

                viewToggle.add(AttributeModifier.replace("title", createStringResource("Change view")));
                viewToggle.add(new TooltipBehavior());
                fragment.add(viewToggle);

                return fragment;
            }

            @Override
            protected WebMarkupContainer createTilesButtonToolbar(String id) {
                Fragment fragment = new Fragment(id, "tableFooterFragment",
                        RoleAnalysisOutlierAssociatedTileTable.this);

                AjaxIconButton refreshTable = new AjaxIconButton("refreshTable",
                        Model.of("fa fa-refresh"), Model.of()) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        onRefresh(ajaxRequestTarget);
                    }
                };

                refreshTable.setOutputMarkupId(true);
                fragment.add(refreshTable);

                TogglePanel<ViewToggle> viewToggle = new TogglePanel<>("viewToggle", items) {

                    @Override
                    protected void itemSelected(@NotNull AjaxRequestTarget target, @NotNull IModel<Toggle<ViewToggle>> item) {
                        getViewToggleModel().setObject(item.getObject().getValue());
                        getTable().refreshSearch();
                        target.add(RoleAnalysisOutlierAssociatedTileTable.this);
                    }
                };

                fragment.add(viewToggle);

                return fragment;
            }

            @Override
            protected String getTilesFooterCssClasses() {
                return "card-footer";
            }

            @Override
            protected String getTilesContainerAdditionalClass() {
                return " m-0";
            }

            @Override
            protected ISortableDataProvider<?, ?> createProvider() {
                return provider;
            }

            @SuppressWarnings({ "rawtypes", "unchecked" })
            @Override
            protected RoleAnalysisOutlierTileModel createTileObject(@NotNull RoleAnalysisOutlierType object) {
                RoleAnalysisOutlierPartitionType outlierPartition = getOutlierPartition(object);
                return new RoleAnalysisOutlierTileModel<>(outlierPartition, object, getPageBase());
            }

            @Override
            protected String getTileCssStyle() {
                return " min-height:170px ";
            }

            @Override
            protected String getTileCssClasses() {
                return "col-4 pb-3 pl-2 pr-2";
            }

            @Override
            protected String getTileContainerCssClass() {
                return "row justify-content-left ";
            }

            @Override
            protected Component createTile(String id, IModel<RoleAnalysisOutlierTileModel<RoleAnalysisOutlierType>> model) {
                return new RoleAnalysisOutlierTilePanel<>(id, model);
            }
        };
    }

    protected CompiledObjectCollectionView getObjectCollectionView() {
        return null;
    }

    public List<IColumn<RoleAnalysisOutlierType, String>> initColumns() {
        List<IColumn<RoleAnalysisOutlierType, String>> columns = new ArrayList<>();

        initIconColumn(columns);

        initNameColumn(columns);

        initExplanationColumn(columns);

        initAnomaliesColumn(columns);

        initPartitionConfidenceColumn(columns);

        initLocationColumn(columns);

        initViewDetailsColumn(columns);

        return columns;
    }

    private void initViewDetailsColumn(@NotNull List<IColumn<RoleAnalysisOutlierType, String>> columns) {
        columns.add(new AbstractColumn<>(createStringResource("")) {

            @Override
            public String getSortProperty() {
                return DetectedAnomalyStatistics.F_CONFIDENCE_DEVIATION.getLocalPart();
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleAnalysisOutlierType>> item, String componentId,
                    IModel<RoleAnalysisOutlierType> rowModel) {

                RoleAnalysisOutlierType outlier = rowModel.getObject();
                RoleAnalysisOutlierPartitionType partition = getOutlierPartition(outlier);

                CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                        GuiStyleConstants.CLASS_ICON_SEARCH, IconCssStyle.IN_ROW_STYLE);
                AjaxCompositedIconSubmitButton migrationButton = new AjaxCompositedIconSubmitButton(
                        componentId,
                        iconBuilder.build(),
                        createStringResource("RoleAnalysis.title.panel.explore.partition.details")) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
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
                migrationButton.titleAsLabel(true);
                migrationButton.setOutputMarkupId(true);
                migrationButton.add(AttributeModifier.append(CLASS_CSS, "btn btn-default btn-sm text-nowrap"));
                migrationButton.setOutputMarkupId(true);
                item.add(migrationButton);

            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(
                        componentId, createStringResource(""));

            }

        });
    }

    private void initLocationColumn(@NotNull List<IColumn<RoleAnalysisOutlierType, String>> columns) {
        columns.add(new AbstractColumn<>(createStringResource("RoleAnalysis.title.panel.location")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleAnalysisOutlierType>> item, String componentId,
                    IModel<RoleAnalysisOutlierType> rowModel) {
                String clusterName;

                RoleAnalysisOutlierType outlierObject = rowModel.getObject();
                RoleAnalysisOutlierPartitionType outlierPartition = getOutlierPartition(outlierObject);

                if (outlierPartition == null) {
                    item.add(new Label(componentId, "N/A"));
                    return;
                }

                ObjectReferenceType clusterRef = outlierPartition.getClusterRef();
                if (clusterRef != null && clusterRef.getTargetName() != null) {
                    clusterName = clusterRef.getTargetName().getOrig();
                } else {
                    item.add(new Label(componentId, "N/A"));
                    return;
                }

                AjaxLinkPanel sessionLink = new AjaxLinkPanel(componentId, Model.of(clusterName)) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        PageParameters parameters = new PageParameters();
                        parameters.add(OnePageParameterEncoder.PARAMETER, clusterRef.getOid());
                        getPageBase().navigateToNext(PageRoleAnalysisCluster.class, parameters);
                    }
                };

                sessionLink.setOutputMarkupId(true);
                sessionLink.add(AttributeModifier.append(STYLE_CSS, "max-width:100px"));
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

    private void initPartitionConfidenceColumn(@NotNull List<IColumn<RoleAnalysisOutlierType, String>> columns) {
        columns.add(new AbstractColumn<>(createStringResource("RoleAnalysis.tile.panel.partition.confidence")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleAnalysisOutlierType>> item, String componentId,
                    IModel<RoleAnalysisOutlierType> rowModel) {
                RoleAnalysisOutlierType outlierObject = rowModel.getObject();
                RoleAnalysisOutlierPartitionType outlierPartition = getOutlierPartition(outlierObject);

                if (outlierPartition != null) {
                    Double partitionConfidence = outlierPartition.getPartitionAnalysis().getOverallConfidence();
                    double clusterConfidenceValue = partitionConfidence != null ? partitionConfidence : 0;

                    String formattedPartitionConfidence = String.format("%.2f", clusterConfidenceValue);
                    item.add(new Label(componentId, formattedPartitionConfidence + " %"));
                } else {
                    item.add(new Label(componentId, "N/A"));
                }

            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(
                        componentId, createStringResource("RoleAnalysis.tile.panel.partition.confidence"));
            }

        });
    }

    private void initAnomaliesColumn(@NotNull List<IColumn<RoleAnalysisOutlierType, String>> columns) {
        columns.add(new AbstractColumn<>(createStringResource("RoleAnalysisOutlierTable.outlier.access")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleAnalysisOutlierType>> item, String componentId,
                    IModel<RoleAnalysisOutlierType> rowModel) {
                RoleAnalysisOutlierType outlierObject = rowModel.getObject();
                Set<String> anomalies = new HashSet<>();
                RoleAnalysisOutlierPartitionType outlierPartition = getOutlierPartition(outlierObject);

                if (outlierPartition != null) {
                    List<DetectedAnomalyResult> detectedAnomalyResult = outlierPartition.getDetectedAnomalyResult();
                    for (DetectedAnomalyResult detectedAnomaly : detectedAnomalyResult) {
                        anomalies.add(detectedAnomaly.getTargetObjectRef().getOid());
                    }
                    item.add(new Label(componentId, anomalies.size()));
                } else {
                    item.add(new Label(componentId, "N/A"));
                }
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(
                        componentId, createStringResource("RoleAnalysisOutlierTable.outlier.access"));
            }

        });
    }

    private void initExplanationColumn(@NotNull List<IColumn<RoleAnalysisOutlierType, String>> columns) {
        columns.add(new AbstractColumn<>(createStringResource("RoleAnalysisOutlierTable.outlier.explanation")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleAnalysisOutlierType>> item, String componentId,
                    IModel<RoleAnalysisOutlierType> model) {
                RoleAnalysisOutlierType outlierObject = model.getObject();

                Model<String> explanationTranslatedModel = explainOutlier(outlierObject);
                item.add(new Label(componentId, explanationTranslatedModel));
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, createStringResource("RoleAnalysisOutlierTable.outlier.explanation"));
            }

        });
    }

    private void initNameColumn(@NotNull List<IColumn<RoleAnalysisOutlierType, String>> columns) {
        columns.add(new AbstractColumn<>(createStringResource("Name")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleAnalysisOutlierType>> item, String componentId,
                    IModel<RoleAnalysisOutlierType> rowModel) {

                RoleAnalysisOutlierType outlier = rowModel.getObject();
                String objectName = outlier.getName().getOrig();

                item.add(new AjaxLinkPanel(componentId, Model.of(objectName)) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        PageParameters parameters = new PageParameters();
                        parameters.add(OnePageParameterEncoder.PARAMETER, outlier.getOid());
                        getPageBase().navigateToNext(PageRoleAnalysisOutlier.class, parameters);
                    }
                });
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(
                        componentId, createStringResource("RoleAnalysisOutlierPropertyTable.name.header"));
            }

        });
    }

    private static void initIconColumn(@NotNull List<IColumn<RoleAnalysisOutlierType, String>> columns) {
        columns.add(new IconColumn<>(null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<RoleAnalysisOutlierType> rowModel) {
                return createDisplayType(GuiStyleConstants.CLASS_ICON_OUTLIER, "red", "");
            }
        });
    }

    @SuppressWarnings("unchecked")
    private TileTablePanel<RoleAnalysisSessionTileModel<SelectableBean<RoleAnalysisSessionType>>, SelectableBean<RoleAnalysisSessionType>> getTable() {
        return (TileTablePanel<RoleAnalysisSessionTileModel<SelectableBean<RoleAnalysisSessionType>>, SelectableBean<RoleAnalysisSessionType>>)
                get(createComponentPath(ID_DATATABLE));
    }

    public IModel<List<Toggle<ViewToggle>>> getItems() {
        return items;
    }

    protected void onRefresh(AjaxRequestTarget target) {
        // TODO override
    }

    private @Nullable ObjectReferenceType getClusterRef() {
        if (clusterRefModel == null) {
            return null;
        }
        return clusterRefModel.getObject();
    }

    private @Nullable ObjectReferenceType getSessionRef() {
        if (sessionRefModel == null) {
            return null;
        }
        return sessionRefModel.getObject();
    }

}
