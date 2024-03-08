/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOutlierDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOutlierType;

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
            protected ISelectableDataProvider<SelectableBean<RoleAnalysisOutlierType>> createProvider() {
                return createSelectableBeanObjectDataProvider(() -> getQuery(cluster), null, null);

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
                        createStringResource("RoleAnalysisOutlierTable.confidence.range")) {

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

                        for (RoleAnalysisOutlierDescriptionType roleAnalysisOutlierDescriptionType : result) {
                            Double confidence = roleAnalysisOutlierDescriptionType.getConfidence();
                            if (confidence != null) {
                                if (confidence < min) {
                                    min = confidence;
                                }
                                if (confidence > max) {
                                    max = confidence;
                                }
                            }
                        }

                        double minPercentage = min * 100.0;
                        double maxPercentage = max * 100.0;

                        minPercentage = Math.round(minPercentage * 100.0) / 100.0;
                        maxPercentage = Math.round(maxPercentage * 100.0) / 100.0;

                        String formattedMinMax = String.format("%.2f - %.2f", minPercentage, maxPercentage);

                        cellItem.add(new Label(componentId, formattedMinMax + " (%)"));

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

    private ObjectQuery getQuery(RoleAnalysisClusterType cluster) {

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
}
