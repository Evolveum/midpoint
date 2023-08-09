/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables;

import static com.evolveum.midpoint.web.component.data.column.ColumnUtils.createStringResource;

import java.io.Serial;
import java.util.*;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.detection.DetectedPattern;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.RoleMiningBoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.util.RoleMiningProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

public class MiningIntersectionTable extends Panel {

    private static final String ID_DATATABLE = "datatable_extra";

    RoleAnalysisProcessModeType roleAnalysisProcessModeType;
    String clusterOid;

    public MiningIntersectionTable(String id, List<DetectedPattern> miningSets,
            RoleAnalysisProcessModeType roleAnalysisProcessModeType, String clusterOid) {
        super(id);
        this.roleAnalysisProcessModeType = roleAnalysisProcessModeType;
        this.clusterOid = clusterOid;
        RoleMiningProvider<DetectedPattern> provider = new RoleMiningProvider<>(
                this, new ListModel<>(miningSets) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void setObject(List<DetectedPattern> object) {
                super.setObject(object);
            }

        }, true);

        provider.setSort(DetectedPattern.F_METRIC, SortOrder.DESCENDING);

        RoleMiningBoxedTablePanel<DetectedPattern> table = new RoleMiningBoxedTablePanel<>(
                ID_DATATABLE, provider, initColumns(),
                null, true, false);
        table.setOutputMarkupId(true);
        table.getDataTable().setItemsPerPage(10);
        table.enableSavePageSize();

        add(table);
    }

    public List<IColumn<DetectedPattern, String>> initColumns() {

        List<IColumn<DetectedPattern, String>> columns = new ArrayList<>();

        columns.add(new IconColumn<>(null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<DetectedPattern> rowModel) {

                return GuiDisplayTypeUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(RoleType.COMPLEX_TYPE));
            }
        });

        columns.add(new AbstractColumn<>(getHeaderTitle("metric")) {

            @Override
            public String getSortProperty() {
                return DetectedPattern.F_METRIC;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<DetectedPattern>> item, String componentId,
                    IModel<DetectedPattern> rowModel) {

                item.add(new Label(componentId, rowModel.getObject().getClusterMetric()));
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, getHeaderTitle("metric"));
            }

        });

        columns.add(new AbstractColumn<>(getHeaderTitle("type")) {

            @Override
            public String getSortProperty() {
                return DetectedPattern.F_TYPE;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<DetectedPattern>> item, String componentId,
                    IModel<DetectedPattern> rowModel) {

                item.add(new Label(componentId, rowModel.getObject().getSearchMode()));
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, getHeaderTitle("type"));
            }

        });

        columns.add(new AbstractColumn<>(getIntersectionHeaderTitle()) {

            @Override
            public String getSortProperty() {
                return DetectedPattern.F_METRIC;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<DetectedPattern>> item, String componentId,
                    IModel<DetectedPattern> rowModel) {

                item.add(new Label(componentId, rowModel.getObject().getMembers().size()));
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, getIntersectionHeaderTitle());
            }

        });

        columns.add(new AbstractColumn<>(getOccupancyHeaderTitle()) {

            @Override
            public String getSortProperty() {
                return DetectedPattern.F_METRIC;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<DetectedPattern>> item, String componentId,
                    IModel<DetectedPattern> rowModel) {

                item.add(new Label(componentId, rowModel.getObject().getProperties().size()));
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, getOccupancyHeaderTitle());
            }

        });

        columns.add(new AbstractExportableColumn<>(getTotalOccupancyHeaderTitle()) {

            @Override
            public String getSortProperty() {
                return DetectedPattern.F_METRIC;
            }

            @Override
            public IModel<?> getDataModel(IModel<DetectedPattern> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<DetectedPattern>> item, String componentId,
                    IModel<DetectedPattern> rowModel) {

                AjaxButton ajaxButton = new AjaxButton(componentId,
                        createStringResource("RoleMining.button.title.compute")) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        DetectedPattern rowModelObject = rowModel.getObject();
                        Set<String> resolveFullOverlap = resolveTotalOccupancy(roleAnalysisProcessModeType, rowModelObject,
                                new OperationResult("resolveFullOverlap"), (PageBase) getPage());

                        if (resolveFullOverlap.size() > 0) {
                            this.setDefaultModel(Model.of(resolveFullOverlap.size() + " (recompute)"));
                            // TODO replaceMemberTypeObjectOccupation the current implementation is for testing and demonstration purposes

//                            replaceRoleAnalysisClusterDetectionTotal(clusterOid, (PageBase) getPage(),
//                                    new OperationResult("resolveFullOverlap"), roleAnalysisProcessModeType, rowModelObject,resolveFullOverlap);
                        }

                        item.setOutputMarkupId(true);
                        ajaxRequestTarget.add(item);
                    }
                };
                ajaxButton.setOutputMarkupId(true);
                ajaxButton.setEnabled(true);
                ajaxButton.add(AttributeAppender.replace("class", " btn btn-primary btn-sm d-flex "
                        + "justify-content-center align-items-center"));
                ajaxButton.add(new AttributeAppender("style", " width:120px; "));
                item.add(ajaxButton);

            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, getTotalOccupancyHeaderTitle());
            }

        });

        columns.add(new AbstractExportableColumn<>(getHeaderTitle("display")) {

            @Override
            public String getSortProperty() {
                return DetectedPattern.F_METRIC;
            }

            @Override
            public IModel<?> getDataModel(IModel<DetectedPattern> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<DetectedPattern>> item, String componentId,
                    IModel<DetectedPattern> rowModel) {

                AjaxButton ajaxButton = new AjaxButton(componentId, createStringResource("RoleMining.button.title.load")) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        onLoad(ajaxRequestTarget, rowModel);
                    }
                };

                ajaxButton.add(AttributeAppender.replace("class", " btn btn-primary btn-sm d-flex "
                        + "justify-content-center align-items-center"));
                ajaxButton.add(new AttributeAppender("style", " width:100px; "));
                ajaxButton.setOutputMarkupId(true);
                item.add(ajaxButton);
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, getHeaderTitle("display"));
            }

        });

        return columns;
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    protected DataTable<?, ?> getDataTable() {
        return ((RoleMiningBoxedTablePanel<?>) get(((PageBase) getPage()).createComponentPath(ID_DATATABLE))).getDataTable();
    }

    protected RoleMiningBoxedTablePanel<?> getTable() {
        return ((RoleMiningBoxedTablePanel<?>) get(((PageBase) getPage()).createComponentPath(ID_DATATABLE)));
    }

    protected StringResourceModel getHeaderTitle(String identifier) {
        return createStringResource("RoleMining.cluster.table.column.header." + identifier);
    }

    protected void onLoad(AjaxRequestTarget ajaxRequestTarget, IModel<DetectedPattern> rowModel) {

    }

    protected StringResourceModel getIntersectionHeaderTitle() {
        if (roleAnalysisProcessModeType.equals(RoleAnalysisProcessModeType.ROLE)) {
            return createStringResource("RoleMining.cluster.table.column.header.role.occupation");
        } else {
            return createStringResource("RoleMining.cluster.table.column.header.user.occupation");
        }
    }

    protected StringResourceModel getOccupancyHeaderTitle() {
        if (roleAnalysisProcessModeType.equals(RoleAnalysisProcessModeType.ROLE)) {
            return createStringResource("RoleMining.cluster.table.column.header.role.cluster.occupation");
        } else {
            return createStringResource("RoleMining.cluster.table.column.header.user.cluster.occupation");
        }
    }

    protected StringResourceModel getTotalOccupancyHeaderTitle() {
        if (roleAnalysisProcessModeType.equals(RoleAnalysisProcessModeType.ROLE)) {
            return createStringResource("RoleMining.cluster.table.column.header.user.total.occupation");
        } else {
            return createStringResource("RoleMining.cluster.table.column.header.role.total.occupation");
        }
    }

    private Set<String> resolveTotalOccupancy(RoleAnalysisProcessModeType roleAnalysisProcessModeType, DetectedPattern detectedPattern,
            OperationResult result, PageBase pageBase) {

        Set<String> membersOidList = new HashSet<>();
        Set<String> detectedProperties = detectedPattern.getProperties();

        if (roleAnalysisProcessModeType.equals(RoleAnalysisProcessModeType.USER)) {
            ResultHandler<UserType> resultHandler = (object, parentResult) -> {
                try {
                    List<String> properties = ClusterObjectUtils.getRolesOidAssignment(object.asObjectable());
                    if (new HashSet<>(properties).containsAll(detectedProperties)) {
                        membersOidList.add(object.getOid());
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return true;
            };
            GetOperationOptionsBuilder optionsBuilder = pageBase.getSchemaService().getOperationOptionsBuilder();

            RepositoryService repositoryService = pageBase.getRepositoryService();
            try {
                repositoryService.searchObjectsIterative(UserType.class, null, resultHandler, optionsBuilder.build(),
                        true, result);
            } catch (SchemaException e) {
                throw new RuntimeException(e);
            }

        } else {
            ListMultimap<String, String> roleToUserMap = ArrayListMultimap.create();

            ResultHandler<UserType> resultHandler = (object, parentResult) -> {
                try {
                    UserType properties = object.asObjectable();
                    List<String> members = ClusterObjectUtils.getRolesOidAssignment(properties);
                    for (String roleId : members) {
                        roleToUserMap.putAll(roleId, Collections.singletonList(properties.getOid()));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return true;
            };

            GetOperationOptionsBuilder optionsBuilder = pageBase.getSchemaService().getOperationOptionsBuilder();
            RepositoryService repositoryService = pageBase.getRepositoryService();

            try {
                repositoryService.searchObjectsIterative(UserType.class, null, resultHandler, optionsBuilder.build(),
                        true, result);
            } catch (SchemaException e) {
                throw new RuntimeException(e);
            }
            for (String member : roleToUserMap.keySet()) {
                List<String> properties = roleToUserMap.get(member);

                if (new HashSet<>(properties).containsAll(detectedProperties)) {
                    membersOidList.add(member);
                }
            }

        }

        return membersOidList;
    }
}
