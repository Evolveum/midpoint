/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierObjectModel.generateRoleOutlierResultModel;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierObjectModel.generateUserOutlierResultModel;

import java.util.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierHeaderResultPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierItemResultPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierObjectModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierResultPanel;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

public class RoleAnalysisOutlierTable extends BasePanel<String> {

    private static final String ID_DATATABLE = "datatable";

    public RoleAnalysisOutlierTable(String id, RoleAnalysisClusterType cluster) {
        super(id);
        MainObjectListPanel<RoleAnalysisOutlierType> table = createTable(cluster);
        table.setOutputMarkupId(true);
        add(table);
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    protected DataTable<?, ?> getDataTable() {
        return ((BoxedTablePanel<?>) get(((PageBase) getPage()).createComponentPath(ID_DATATABLE))).getDataTable();
    }

    protected BoxedTablePanel<?> getTable() {
        return ((BoxedTablePanel<?>) get(((PageBase) getPage()).createComponentPath(ID_DATATABLE)));
    }

    private MainObjectListPanel<RoleAnalysisOutlierType> createTable(RoleAnalysisClusterType cluster) {
        MainObjectListPanel<RoleAnalysisOutlierType> table = new MainObjectListPanel<>(ID_DATATABLE, RoleAnalysisOutlierType.class) {
            @Override
            protected List<Component> createToolbarButtonsList(String buttonId) {
                return null;
            }

            @Override
            protected boolean isDuplicationSupported() {
                return false;
            }

            @Override
            public String getAdditionalBoxCssClasses() {
                return " m-0";
            }

            @Override
            protected ISelectableDataProvider<SelectableBean<RoleAnalysisOutlierType>> createProvider() {
                return RoleAnalysisOutlierTable.this.createProvider(cluster);
//                return createSelectableBeanObjectDataProvider(() -> getQuery(cluster), null, null);

            }

            @Override
            protected List<IColumn<SelectableBean<RoleAnalysisOutlierType>, String>> createDefaultColumns() {
                List<IColumn<SelectableBean<RoleAnalysisOutlierType>, String>> defaultColumns = super.createDefaultColumns();

                IColumn<SelectableBean<RoleAnalysisOutlierType>, String> column;
                column = new AbstractExportableColumn<>(
                        createStringResource("RoleAnalysisOutlierTable.outlier.properties")) {

                    @Override
                    public IModel<?> getDataModel(IModel<SelectableBean<RoleAnalysisOutlierType>> iModel) {
                        return Model.of(iModel.getObject().getValue().getResult().size());
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisOutlierType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisOutlierType>> model) {
                        cellItem.add(new Label(componentId, model.getObject().getValue().getResult().size()));
                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                };
                defaultColumns.add(column);

                column = new AbstractExportableColumn<>(
                        createStringResource("Confidence")) {

                    @Override
                    public IModel<?> getDataModel(IModel<SelectableBean<RoleAnalysisOutlierType>> iModel) {
                        return Model.of(iModel.getObject().getValue().getResult().size());
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisOutlierType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisOutlierType>> model) {

                        RoleAnalysisOutlierType outlier = model.getObject().getValue();

                        Double clusterConfidence = outlier.getClusterConfidence();
                        double clusterConfidenceValue = clusterConfidence != null ? clusterConfidence : 0;

                        String formattedClusterConfidence = String.format("%.2f", clusterConfidenceValue);
                        cellItem.add(new Label(componentId, formattedClusterConfidence + " %"));

                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                };
                defaultColumns.add(column);

                column = new AbstractExportableColumn<>(
                        createStringResource("RoleAnalysisOutlierTable.clusters.status.header")) {

                    @Override
                    public IModel<?> getDataModel(IModel<SelectableBean<RoleAnalysisOutlierType>> iModel) {
                        return Model.of(iModel.getObject().getValue().getResult().size());
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisOutlierType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisOutlierType>> model) {
                        List<RoleAnalysisOutlierDescriptionType> result = model.getObject().getValue().getResult();

                        double min = Double.POSITIVE_INFINITY;
                        double max = Double.NEGATIVE_INFINITY;

                        Set<String> clusters = new HashSet<>();
                        for (RoleAnalysisOutlierDescriptionType roleAnalysisOutlierDescriptionType : result) {
                            clusters.add(roleAnalysisOutlierDescriptionType.getCluster().getOid());
                        }

                        if (clusters.size() == 1) {
                            cellItem.add(new Label(componentId, "Single (" + clusters.size() + ")"));
                        } else {
                            cellItem.add(new Label(componentId, "Multiple (" + clusters.size() + ")"));
                        }

                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                };
                defaultColumns.add(column);

                defaultColumns.add(new AbstractExportableColumn<>(createStringResource("Result")) {

                    @Override
                    public IModel<?> getDataModel(IModel<SelectableBean<RoleAnalysisOutlierType>> iModel) {
                        return null;
                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisOutlierType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisOutlierType>> rowModel) {
                        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
                        Task task = getPageBase().createSimpleTask("Load object");
                        ObjectReferenceType roleAnalysisSessionRef = cluster.getRoleAnalysisSessionRef();
                        PrismObject<RoleAnalysisSessionType> sessionTypeObject = roleAnalysisService.getSessionTypeObject(roleAnalysisSessionRef.getOid(), task, task.getResult());
                        assert sessionTypeObject != null;
                        RoleAnalysisSessionType sessionType = sessionTypeObject.asObjectable();
                        RoleAnalysisProcessModeType processMode = sessionType.getAnalysisOption().getProcessMode();
                        OutlierObjectModel outlierObjectModel;
                        RoleAnalysisOutlierType outlier = rowModel.getObject().getValue();
                        if (processMode.equals(RoleAnalysisProcessModeType.USER)) {
                            outlierObjectModel = generateUserOutlierResultModel(roleAnalysisService, outlier, task, task.getResult(), cluster);
                        } else {
                            outlierObjectModel = generateRoleOutlierResultModel(roleAnalysisService, outlier, task, task.getResult(), cluster);
                        }

                        String outlierName = outlierObjectModel.getOutlierName();
                        double outlierConfidence = outlierObjectModel.getOutlierConfidence();
                        String outlierDescription = outlierObjectModel.getOutlierDescription();
                        String timeCreated = outlierObjectModel.getTimeCreated();

                        cellItem.add(new AjaxLinkPanel(componentId, Model.of("Result")) {
                            @Override
                            public void onClick(AjaxRequestTarget target) {
                                OutlierResultPanel detailsPanel = new OutlierResultPanel(
                                        ((PageBase) getPage()).getMainPopupBodyId(),
                                        Model.of("Analyzed members details panel")) {

                                    @Override
                                    public Component getCardHeaderBody(String componentId) {
                                        OutlierHeaderResultPanel components = new OutlierHeaderResultPanel(componentId, outlierName,
                                                outlierDescription, String.valueOf(outlierConfidence), timeCreated);
                                        components.setOutputMarkupId(true);
                                        return components;
                                    }

                                    @Override
                                    public Component getCardBodyComponent(String componentId) {
                                        //TODO just for testing
                                        RepeatingView cardBodyComponent = (RepeatingView) super.getCardBodyComponent(componentId);
                                        outlierObjectModel.getOutlierItemModels().forEach(outlierItemModel -> {
                                            cardBodyComponent.add(new OutlierItemResultPanel(cardBodyComponent.newChildId(), outlierItemModel));
                                        });
                                        return cardBodyComponent;
                                    }

                                    @Override
                                    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                                        super.onClose(ajaxRequestTarget);
                                    }

                                };
                                ((PageBase) getPage()).showMainPopup(detailsPanel, target);

                            }
                        });

                    }

                    @Override
                    public Component getHeader(String componentId) {
                        return new Label(
                                componentId, Model.of("Result"));
                    }

                });

                return defaultColumns;
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return null;
            }

            @Override
            protected String getNothingSelectedMessage() {
                return getString("pageUsers.message.nothingSelected");
            }

            @Override
            protected String getConfirmMessageKeyForMultiObject() {
                return "pageUsers.message.confirmationMessageForMultipleObject";
            }

            @Override
            protected String getConfirmMessageKeyForSingleObject() {
                return "pageUsers.message.confirmationMessageForSingleObject";
            }
        };
        table.setOutputMarkupId(true);
        return table;
    }

    ObjectQuery getQuery(RoleAnalysisClusterType cluster) {

        List<ObjectReferenceType> member = cluster.getMember();
        Set<String> membersOid = new HashSet<>();
        for (ObjectReferenceType objectReferenceType : member) {
            membersOid.add(objectReferenceType.getOid());
        }
        ObjectQuery query = getPrismContext().queryFor(RoleAnalysisOutlierType.class)
                .item(RoleAnalysisOutlierType.F_TARGET_OBJECT_REF).ref(membersOid.toArray(new String[0]))
                .build();

        return query;
    }

    private SelectableBeanObjectDataProvider<RoleAnalysisOutlierType> createProvider(RoleAnalysisClusterType cluster) {

        PageBase pageBase = getPageBase();
        Task task = pageBase.createSimpleTask("Search outliers");
        OperationResult result = task.getResult();
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        List<RoleAnalysisOutlierType> searchResultList = roleAnalysisService.findClusterOutliers(cluster, task, result);
        return new SelectableBeanObjectDataProvider<>(
                RoleAnalysisOutlierTable.this, Set.of()) {

            @SuppressWarnings("rawtypes")
            @Override
            protected List<RoleAnalysisOutlierType> searchObjects(Class type,
                    ObjectQuery query,
                    Collection collection,
                    Task task,
                    OperationResult result) {
                Integer offset = query.getPaging().getOffset();
                Integer maxSize = query.getPaging().getMaxSize();
                return searchResultList.subList(offset, offset + maxSize);
            }

            @Override
            protected Integer countObjects(Class<RoleAnalysisOutlierType> type,
                    ObjectQuery query,
                    Collection<SelectorOptions<GetOperationOptions>> currentOptions,
                    Task task,
                    OperationResult result) {
                return searchResultList.size();
            }
        };
    }

    @NotNull
    private List<RoleAnalysisOutlierType> findClusterOutliers(@NotNull RoleAnalysisClusterType cluster) {
        List<RoleAnalysisOutlierType> searchResultList = new ArrayList<>();
        String clusterOid = cluster.getOid();
        ResultHandler<RoleAnalysisOutlierType> resultHandler = (outlier, lResult) -> {

            RoleAnalysisOutlierType outlierObject = outlier.asObjectable();
            ObjectReferenceType targetClusterRef = outlierObject.getTargetClusterRef();
            String oid = targetClusterRef.getOid();
            if (clusterOid.equals(oid)) {
                searchResultList.add(outlier.asObjectable());
            }
            return true;
        };

        PageBase pageBase = getPageBase();
        Task task = pageBase.createSimpleTask("Search outliers");
        OperationResult result = task.getResult();

        try {
            pageBase.getModelService().searchObjectsIterative(RoleAnalysisOutlierType.class, null, resultHandler,
                    null, task, result);
        } catch (Exception ex) {
            throw new RuntimeException("Couldn't search outliers", ex);
        }
        return searchResultList;
    }

}
