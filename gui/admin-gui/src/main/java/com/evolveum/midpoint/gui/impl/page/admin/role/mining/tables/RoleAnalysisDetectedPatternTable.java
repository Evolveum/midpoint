/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.getRolesOidAssignment;
import static com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil.createDisplayType;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.RoleAnalysisObjectUtils.*;

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
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.AjaxCompositedIconButton;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRole;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.BusinessRoleApplicationDto;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.BusinessRoleDto;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.MembersDetailsPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyHeaderPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.util.RoleMiningProvider;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperHeaderModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class RoleAnalysisDetectedPatternTable extends BasePanel<String> {

    private static final String ID_DATATABLE = "datatable";
    boolean isOperationEnable;
    private static final String DOT_CLASS = RoleAnalysisDetectedPatternTable.class.getName() + ".";
    private static final String OP_PREPARE_OBJECTS = DOT_CLASS + "prepareObjects";
    OperationResult result = new OperationResult(OP_PREPARE_OBJECTS);


    public RoleAnalysisDetectedPatternTable(String id, LoadableDetachableModel<List<DetectedPattern>> detectedPatternList,
            boolean isOperationEnable, RoleAnalysisClusterType cluster) {
        super(id);

        this.isOperationEnable = isOperationEnable;
        RoleMiningProvider<DetectedPattern> provider = new RoleMiningProvider<>(
                this, new ListModel<>(detectedPatternList.getObject()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void setObject(List<DetectedPattern> object) {
                super.setObject(object);
            }

        }, true);

        provider.setSort(DetectedPattern.F_METRIC, SortOrder.DESCENDING);

        BoxedTablePanel<DetectedPattern> table = new BoxedTablePanel<>(
                ID_DATATABLE, provider, initColumns(cluster)) {
            @Override
            protected WebMarkupContainer createButtonToolbar(String id) {
                AjaxIconButton refreshIcon = new AjaxIconButton(id, new Model<>(GuiStyleConstants.CLASS_RECONCILE),
                        createStringResource("MainObjectListPanel.refresh")) {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        onRefresh(target);
                    }
                };
                refreshIcon.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
                return refreshIcon;
            }
        };
        table.setOutputMarkupId(true);
        table.getDataTable().setItemsPerPage(10);
        table.enableSavePageSize();

        add(table);
    }

    public List<IColumn<DetectedPattern, String>> initColumns(RoleAnalysisClusterType cluster) {

        LoadableModel<PrismContainerDefinition<RoleAnalysisDetectionPatternType>> containerDefinitionModel
                = WebComponentUtil.getContainerDefinitionModel(RoleAnalysisDetectionPatternType.class);

        List<IColumn<DetectedPattern, String>> columns = new ArrayList<>();

        columns.add(new IconColumn<>(null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<DetectedPattern> rowModel) {

                return createDisplayType(GuiStyleConstants.CLASS_OBJECT_ROLE_ICON, "green", "");
            }
        });

        columns.add(new AbstractColumn<>(getHeaderTitle("")) {

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
                return createColumnHeader(componentId, containerDefinitionModel
                );

            }

        });

        columns.add(new AbstractColumn<>(createStringResource("")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<DetectedPattern>> item, String componentId,
                    IModel<DetectedPattern> rowModel) {

                if (rowModel.getObject() != null && rowModel.getObject().getRoles() != null) {
                    int rolesCount = rowModel.getObject().getRoles().size();

                    CompositedIconBuilder iconBuilder = new CompositedIconBuilder()
                            .setBasicIcon(GuiStyleConstants.CLASS_OBJECT_ROLE_ICON, LayeredIconCssStyle.IN_ROW_STYLE);

                    AjaxCompositedIconButton objectButton = new AjaxCompositedIconButton(componentId, iconBuilder.build(),
                            Model.of(String.valueOf(rolesCount))) {

                        @Serial private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            List<PrismObject<FocusType>> objects = new ArrayList<>();
                            Set<String> roles = rowModel.getObject().getRoles();

                            for (String s : roles) {
                                objects.add(getFocusTypeObject(getPageBase(), s, result));
                            }
                            MembersDetailsPanel detailsPanel = new MembersDetailsPanel(((PageBase) getPage()).getMainPopupBodyId(),
                                    Model.of("Analyzed members details panel"), objects, RoleAnalysisProcessModeType.ROLE) {
                                @Override
                                public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                                    super.onClose(ajaxRequestTarget);
                                }
                            };
                            ((PageBase) getPage()).showMainPopup(detailsPanel, target);
                        }

                    };
                    objectButton.titleAsLabel(true);
                    objectButton.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
                    objectButton.add(AttributeAppender.append("style", "width:150px"));

                    if (isOperationEnable) {
                        objectButton.setEnabled(false);
                    }

                    item.add(objectButton);
                } else {
                    item.add(new EmptyPanel(componentId));
                }
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, createStringResource("RoleMining.cluster.table.column.header.role.occupation"));
            }

        });

        columns.add(new AbstractColumn<>(createStringResource("")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<DetectedPattern>> item, String componentId,
                    IModel<DetectedPattern> rowModel) {

                if (rowModel.getObject() != null && rowModel.getObject().getUsers() != null) {
                    int usersCount = rowModel.getObject().getUsers().size();

                    CompositedIconBuilder iconBuilder = new CompositedIconBuilder()
                            .setBasicIcon(GuiStyleConstants.CLASS_OBJECT_ROLE_ICON, LayeredIconCssStyle.IN_ROW_STYLE);

                    AjaxCompositedIconButton objectButton = new AjaxCompositedIconButton(componentId, iconBuilder.build(),
                            Model.of(String.valueOf(usersCount))) {

                        @Serial private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {

                            List<PrismObject<FocusType>> objects = new ArrayList<>();
                            Set<String> users = rowModel.getObject().getUsers();

                            for (String s : users) {
                                objects.add(getFocusTypeObject(getPageBase(), s, result));
                            }
                            MembersDetailsPanel detailsPanel = new MembersDetailsPanel(((PageBase) getPage()).getMainPopupBodyId(),
                                    Model.of("Analyzed members details panel"), objects, RoleAnalysisProcessModeType.USER) {
                                @Override
                                public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                                    super.onClose(ajaxRequestTarget);
                                }
                            };
                            ((PageBase) getPage()).showMainPopup(detailsPanel, target);
                        }

                    };
                    objectButton.titleAsLabel(true);
                    objectButton.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
                    objectButton.add(AttributeAppender.append("style", "width:150px"));

                    if (isOperationEnable) {
                        objectButton.setEnabled(false);
                    }

                    item.add(objectButton);
                } else {
                    item.add(new EmptyPanel(componentId));
                }

            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, createStringResource("RoleMining.cluster.table.column.header.user.occupation"));
            }

        });

        if (isOperationEnable) {
            columns.add(new AbstractExportableColumn<>(getHeaderTitle("display")) {

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

                        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(GuiStyleConstants.CLASS_ICON_SEARCH,
                                LayeredIconCssStyle.IN_ROW_STYLE);
                        AjaxCompositedIconSubmitButton exploreButton = new AjaxCompositedIconSubmitButton(componentId,
                                iconBuilder.build(),
                                createStringResource("RoleMining.button.title.load")) {
                            @Serial private static final long serialVersionUID = 1L;

                            @Override
                            protected void onSubmit(AjaxRequestTarget target) {
                                onLoad(target, rowModel);
                            }

                            @Override
                            protected void onError(AjaxRequestTarget target) {
                                target.add(((PageBase) getPage()).getFeedbackPanel());
                            }
                        };
                        exploreButton.titleAsLabel(true);
                        exploreButton.setOutputMarkupId(true);
                        exploreButton.add(AttributeAppender.append("class", "btn btn-success btn-sm"));

                        item.add(exploreButton);

                    }

                }

                @Override
                public Component getHeader(String componentId) {
                    return new Label(componentId, getHeaderTitle("display"));
                }

            });
        } else {
            columns.add(new AbstractExportableColumn<>(getHeaderTitle("display")) {

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
                        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(GuiStyleConstants.CLASS_PLUS_CIRCLE,
                                LayeredIconCssStyle.IN_ROW_STYLE);
                        AjaxCompositedIconSubmitButton migrationButton = new AjaxCompositedIconSubmitButton(componentId,
                                iconBuilder.build(),
                                createStringResource("RoleMining.button.title.process")) {
                            @Serial private static final long serialVersionUID = 1L;

                            @Override
                            protected void onSubmit(AjaxRequestTarget target) {

                                Set<String> roles = rowModel.getObject().getRoles();
                                Set<String> users = rowModel.getObject().getUsers();

                                List<AssignmentType> roleAssignments = new ArrayList<>();

                                for (String roleOid : roles) {
                                    PrismObject<RoleType> roleObject = getRoleTypeObject(getPageBase(), roleOid, result);
                                    if (roleObject != null) {
                                        roleAssignments.add(ObjectTypeUtil.createAssignmentTo(roleOid, ObjectTypes.ROLE));
                                    }
                                }

                                PrismObject<RoleType> businessRole = generateBusinessRole((PageBase) getPage(), roleAssignments, "");

                                List<BusinessRoleDto> roleApplicationDtos = new ArrayList<>();

                                for (String userOid : users) {
                                    PrismObject<UserType> userObject = getUserTypeObject(getPageBase(), userOid, result);
                                    if (userObject != null) {
                                        roleApplicationDtos.add(new BusinessRoleDto(userObject,
                                                businessRole, getPageBase()));
                                    }
                                }

                                PrismObject<RoleAnalysisClusterType> prismObjectCluster = cluster.asPrismObject();

                                BusinessRoleApplicationDto operationData = new BusinessRoleApplicationDto(prismObjectCluster, businessRole, roleApplicationDtos);

                                PageRole pageRole = new PageRole(operationData.getBusinessRole(), operationData);
                                setResponsePage(pageRole);
                            }

                            @Override
                            protected void onError(AjaxRequestTarget target) {
                                target.add(((PageBase) getPage()).getFeedbackPanel());
                            }
                        };
                        migrationButton.titleAsLabel(true);
                        migrationButton.setOutputMarkupId(true);
                        migrationButton.add(AttributeAppender.append("class", "btn btn-success btn-sm"));

                        item.add(migrationButton);
                    }

                }

                @Override
                public Component getHeader(String componentId) {
                    return new Label(componentId, getHeaderTitle("display"));
                }

            });

        }
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
            LoadableModel<PrismContainerDefinition<C>> containerDefinitionModel) {

        return new PrismPropertyHeaderPanel<>(componentId, new PrismPropertyWrapperHeaderModel<>(
                containerDefinitionModel,
                RoleAnalysisDetectionPatternType.F_CLUSTER_METRIC,
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

    protected void onRefresh(AjaxRequestTarget target) {

    }
}
