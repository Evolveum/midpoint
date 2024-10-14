/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile;

import static com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil.createDisplayType;

import java.io.Serial;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.apache.wicket.model.StringResourceModel;
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
import com.evolveum.midpoint.gui.impl.page.self.requestAccess.PageableListView;
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

    IModel<ObjectReferenceType> clusterRef;
    IModel<ObjectReferenceType> sessionRef;

    public RoleAnalysisOutlierAssociatedTileTable(
            @NotNull String id,
            @NotNull IModel<List<RoleAnalysisOutlierType>> outliers,
            @NotNull RoleAnalysisClusterType cluster) {
        super(id, outliers);
        this.clusterRef = new LoadableModel<>() {
            @Override
            protected ObjectReferenceType load() {
                return new ObjectReferenceType()
                        .oid(cluster.getOid())
                        .type(RoleAnalysisClusterType.COMPLEX_TYPE)
                        .targetName(cluster.getName());
            }
        };

        this.sessionRef = new LoadableModel<>() {
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
        this.sessionRef = new LoadableModel<>() {
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
        if (getClusterRef() != null) {
            String clusterOid = getClusterRef().getOid();
            for (RoleAnalysisOutlierPartitionType partition : outlierPartitions) {
                ObjectReferenceType targetClusterRef = partition.getClusterRef();
                if (targetClusterRef != null
                        && targetClusterRef.getOid() != null
                        && targetClusterRef.getOid().equals(clusterOid)) {
                    return partition;
                }
            }
        } else if (getSessionRef() != null) {
            String sessionOid = getSessionRef().getOid();
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
                Model.of(ViewToggle.TILE),
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
            protected void onInitialize() {
                super.onInitialize();
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

            @Override
            protected PageableListView<?, ?> createTilesPanel(String tilesId, ISortableDataProvider<RoleAnalysisOutlierType, String> provider1) {
                return super.createTilesPanel(tilesId, provider1);
            }

            @SuppressWarnings({ "rawtypes", "unchecked" })
            @Override
            protected RoleAnalysisOutlierTileModel createTileObject(RoleAnalysisOutlierType object) {
                RoleAnalysisOutlierPartitionType outlierPartition = getOutlierPartition(object);
                return new RoleAnalysisOutlierTileModel<>(getOutlierPartition(object), object, getPageBase());
            }

            @Override
            protected String getTileCssStyle() {
                return " min-height:170px ";
            }

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

        columns.add(new IconColumn<>(null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<RoleAnalysisOutlierType> rowModel) {
                return createDisplayType(GuiStyleConstants.CLASS_ICON_OUTLIER, "red", "");
            }
        });

        columns.add(new AbstractColumn<>(createStringResource("Name")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleAnalysisOutlierType>> item, String componentId,
                    IModel<RoleAnalysisOutlierType> rowModel) {

                String objectName = "unknown";
                RoleAnalysisOutlierType outlier = rowModel.getObject();
                ObjectReferenceType targetObjectRef = outlier.getObjectRef();
                if (targetObjectRef != null && targetObjectRef.getTargetName() != null) {
                    objectName = targetObjectRef.getTargetName().toString();
                }

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

        columns.add(new AbstractColumn<>(createStringResource("Confidence")) {

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
                    Double clusterConfidence = outlierPartition.getPartitionAnalysis().getOverallConfidence();
                    double clusterConfidenceValue = clusterConfidence != null ? clusterConfidence : 0;

                    String formattedClusterConfidence = String.format("%.2f", clusterConfidenceValue);
                    item.add(new Label(componentId, formattedClusterConfidence + " %"));
                } else {
                    item.add(new Label(componentId, "N/A"));
                }

            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(
                        componentId, createStringResource("Confidence"));
            }

        });

        columns.add(new AbstractColumn<>(createStringResource("Result")) {

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

                item.add(new AjaxLinkPanel(componentId, Model.of("Result")) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (partition != null) {
                            RoleAnalysisPartitionOverviewPanel panel = new RoleAnalysisPartitionOverviewPanel(
                                    ((PageBase) getPage()).getMainPopupBodyId(),
                                    Model.of(partition), Model.of(outlier)) {
                                @Override
                                public IModel<String> getTitle() {
                                    return createStringResource(
                                            "RoleAnalysisPartitionOverviewPanel.title.most.impact.partition");
                                }
                            };
                            panel.setOutputMarkupId(true);
                            ((PageBase) getPage()).showMainPopup(panel, target);
                        }
                    }
                });

            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(
                        componentId, createStringResource("Result"));

            }

        });

        return columns;
    }

    @SuppressWarnings("unchecked")
    private TileTablePanel<RoleAnalysisSessionTileModel<SelectableBean<RoleAnalysisSessionType>>, SelectableBean<RoleAnalysisSessionType>> getTable() {
        return (TileTablePanel<RoleAnalysisSessionTileModel<SelectableBean<RoleAnalysisSessionType>>, SelectableBean<RoleAnalysisSessionType>>)
                get(createComponentPath(ID_DATATABLE));
    }

    protected StringResourceModel getHeaderTitle(String identifier) {
        return createStringResource("RoleMining.cluster.table.column.header." + identifier);
    }

    public IModel<List<Toggle<ViewToggle>>> getItems() {
        return items;
    }

//    @Override
//    public PageBase getPageBase() {
//        return pageBase;
//    }

    protected void onRefresh(AjaxRequestTarget target) {

    }

    private @Nullable ObjectReferenceType getClusterRef() {
        if (clusterRef == null) {
            return null;
        }
        return clusterRef.getObject();
    }

    private @Nullable ObjectReferenceType getSessionRef() {
        if (sessionRef == null) {
            return null;
        }
        return sessionRef.getObject();
    }

    private IModel<ObjectReferenceType> getClusterRefModel() {
        return clusterRef;
    }

    private IModel<ObjectReferenceType> getSessionRefModel() {
        return sessionRef;
    }

}
