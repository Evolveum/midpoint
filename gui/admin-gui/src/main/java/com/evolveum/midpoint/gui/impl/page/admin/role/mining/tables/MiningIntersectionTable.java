/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.getRolesOidAssignment;
import static com.evolveum.midpoint.web.component.data.column.ColumnUtils.createStringResource;

import java.io.Serial;
import java.util.*;

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
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyHeaderPanel;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.util.RoleMiningProvider;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperHeaderModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class MiningIntersectionTable extends Panel {

    private static final String ID_DATATABLE = "datatable_extra";

    private final RoleAnalysisProcessModeType roleAnalysisProcessModeType;

    public MiningIntersectionTable(String id, List<DetectedPattern> detectedPatternList,
            RoleAnalysisProcessModeType roleAnalysisProcessModeType) {
        super(id);
        this.roleAnalysisProcessModeType = roleAnalysisProcessModeType;

        RoleMiningProvider<DetectedPattern> provider = new RoleMiningProvider<>(
                this, new ListModel<>(detectedPatternList) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void setObject(List<DetectedPattern> object) {
                super.setObject(object);
            }

        }, true);

        provider.setSort(DetectedPattern.F_METRIC, SortOrder.DESCENDING);

        BoxedTablePanel<DetectedPattern> table = new BoxedTablePanel<>(
                ID_DATATABLE, provider, initColumns());
        table.setOutputMarkupId(true);
        table.getDataTable().setItemsPerPage(10);
        table.enableSavePageSize();

        add(table);
    }

    public List<IColumn<DetectedPattern, String>> initColumns() {

        LoadableModel<PrismContainerDefinition<RoleAnalysisDetectionPatternType>> containerDefinitionModel
                = WebComponentUtil.getContainerDefinitionModel(RoleAnalysisDetectionPatternType.class);

        List<IColumn<DetectedPattern, String>> columns = new ArrayList<>();

        columns.add(new IconColumn<>(null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<DetectedPattern> rowModel) {
                return GuiDisplayTypeUtil.createDisplayType(IconAndStylesUtil.createDefaultBlackIcon(RoleType.COMPLEX_TYPE));
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
                if (rowModel.getObject() != null) {
                    item.add(new Label(componentId, rowModel.getObject().getClusterMetric()));
                }
            }

            @Override
            public Component getHeader(String componentId) {
                return createColumnHeader(componentId, containerDefinitionModel,
                        RoleAnalysisDetectionPatternType.F_CLUSTER_METRIC);

            }

        });

        columns.add(new AbstractColumn<>(createStringResource("")) {

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

                if (rowModel.getObject() != null && rowModel.getObject().getUsers() != null) {
                    item.add(new Label(componentId, rowModel.getObject().getUsers().size()));
                } else {
                    item.add(new EmptyPanel(componentId));
                }
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, createStringResource("RoleMining.cluster.table.column.header.user.occupation"));
            }

        });

        columns.add(new AbstractColumn<>(createStringResource("")) {

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

                if (rowModel.getObject() != null && rowModel.getObject().getRoles() != null) {
                    item.add(new Label(componentId, rowModel.getObject().getRoles().size()));
                } else {
                    item.add(new EmptyPanel(componentId));
                }
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, createStringResource("RoleMining.cluster.table.column.header.role.occupation"));
            }

        });

        columns.add(new AbstractExportableColumn<>(createStringResource("")) {

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

                if (rowModel.getObject() == null) {
                    item.add(new EmptyPanel(componentId));
                } else {
                    AjaxButton ajaxButton = new AjaxButton(componentId,
                            createStringResource("RoleMining.button.title.compute")) {
                        @Override
                        public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                            DetectedPattern rowModelObject = rowModel.getObject();

                            Set<String> resolveFullOverlap = resolveTotalOccupancy(
                                    roleAnalysisProcessModeType,
                                    rowModelObject,
                                    new OperationResult("resolveFullOverlap"),
                                    (PageBase) getPage());

                            if (!resolveFullOverlap.isEmpty()) {
                                this.setDefaultModel(Model.of(resolveFullOverlap.size() + " (recompute)"));
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

            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, createStringResource("RoleMining.button.title.compute"));
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
                if (rowModel.getObject() == null) {
                    item.add(new EmptyPanel(componentId));
                } else {
                    AjaxButton ajaxButton = new AjaxButton(componentId,
                            createStringResource("RoleMining.button.title.load")) {
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
        return ((BoxedTablePanel<?>) get(((PageBase) getPage()).createComponentPath(ID_DATATABLE))).getDataTable();
    }

    protected BoxedTablePanel<?> getTable() {
        return ((BoxedTablePanel<?>) get(((PageBase) getPage()).createComponentPath(ID_DATATABLE)));
    }

    protected StringResourceModel getHeaderTitle(String identifier) {
        return createStringResource("RoleMining.cluster.table.column.header." + identifier);
    }

    protected void onLoad(AjaxRequestTarget ajaxRequestTarget, IModel<DetectedPattern> rowModel) {

    }

    private Set<String> resolveTotalOccupancy(RoleAnalysisProcessModeType roleAnalysisProcessModeType, DetectedPattern detectedPattern,
            OperationResult result, PageBase pageBase) {

        Set<String> membersOidList = new HashSet<>();
        Set<String> detectedProperties = detectedPattern.getRoles();

        if (roleAnalysisProcessModeType.equals(RoleAnalysisProcessModeType.USER)) {
            ResultHandler<UserType> resultHandler = (object, parentResult) -> {
                try {
                    List<String> properties = getRolesOidAssignment(object.asObjectable());
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
                    List<String> members = getRolesOidAssignment(properties);
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

    private <C extends Containerable> PrismPropertyHeaderPanel<?> createColumnHeader(String componentId,
            LoadableModel<PrismContainerDefinition<C>> containerDefinitionModel,
            ItemPath itemPath) {

        return new PrismPropertyHeaderPanel<>(componentId, new PrismPropertyWrapperHeaderModel<>(
                containerDefinitionModel,
                itemPath,
                (PageBase) getPage())) {

            @Override
            protected boolean isAddButtonVisible() {
                return false;
            }

            @Override
            protected boolean isButtonEnabled() {
                return false;
            }
        };
    }
}
