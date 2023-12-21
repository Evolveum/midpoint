/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.resolveDateAndTime;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisChannelMode;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRole;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.util.RoleMiningProvider;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "migratedRoles")
@PanelInstance(
        identifier = "migratedRoles",
        applicableForType = RoleAnalysisClusterType.class,
        display = @PanelDisplay(
                label = "RoleAnalysisClusterType.migratedRoles",
                icon = GuiStyleConstants.CLASS_GROUP_ICON,
                order = 30
        )
)
public class MigratedRolesPanel extends AbstractObjectMainPanel<RoleAnalysisClusterType, ObjectDetailsModels<RoleAnalysisClusterType>> {

    private static final String ID_CONTAINER = "container";
    private static final String ID_PANEL = "panelId";

    private static final String DOT_CLASS = MigratedRolesPanel.class.getName() + ".";
    private static final String OP_PREPARE_OBJECTS = DOT_CLASS + "prepareObjects";
    private final OperationResult result = new OperationResult(OP_PREPARE_OBJECTS);

    public MigratedRolesPanel(String id, ObjectDetailsModels<RoleAnalysisClusterType> model,
            ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        RoleAnalysisClusterType cluster = getObjectDetailsModels().getObjectType();
        List<ObjectReferenceType> reductionObject = cluster.getResolvedPattern();
        Task task = getPageBase().createSimpleTask("resolve role object");

        List<RoleType> roles = new ArrayList<>();
        for (ObjectReferenceType objectReferenceType : reductionObject) {
            String oid = objectReferenceType.getOid();
            if (oid != null) {
                PrismObject<RoleType> roleTypeObject = getPageBase().getRoleAnalysisService()
                        .getRoleTypeObject(oid, task, result);
                if (roleTypeObject != null) {
                    roles.add(roleTypeObject.asObjectable());
                }
            }
        }

        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        RoleMiningProvider<RoleType> provider = new RoleMiningProvider<>(
                this, new ListModel<>(roles) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void setObject(List<RoleType> object) {
                super.setObject(roles);
            }
        }, false);

        BoxedTablePanel<RoleType> panel = generateTable(provider);
        container.add(panel);

    }

    private BoxedTablePanel<RoleType> generateTable(RoleMiningProvider<RoleType> provider) {

        BoxedTablePanel<RoleType> table = new BoxedTablePanel<>(
                ID_PANEL, provider, initColumns()) {
            @Override
            protected WebMarkupContainer createButtonToolbar(String id) {
                AjaxIconButton refreshIcon = new AjaxIconButton(id, new Model<>(GuiStyleConstants.CLASS_RECONCILE),
                        createStringResource("MainObjectListPanel.refresh")) {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        onRefresh();
                    }
                };
                refreshIcon.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
                return refreshIcon;
            }
        };
        table.setOutputMarkupId(true);
        return table;
    }

    private List<IColumn<RoleType, String>> initColumns() {

        List<IColumn<RoleType, String>> columns = new ArrayList<>();

        columns.add(new IconColumn<>(null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<RoleType>> cellItem, String componentId, IModel<RoleType> rowModel) {
                super.populateItem(cellItem, componentId, rowModel);
            }

            @Override
            protected DisplayType getIconDisplayType(IModel<RoleType> rowModel) {
                return GuiDisplayTypeUtil
                        .createDisplayType(IconAndStylesUtil.createDefaultBlackIcon(RoleType.COMPLEX_TYPE));
            }
        });

        columns.add(new AbstractColumn<>(createStringResource("ObjectType.name")) {

            @Override
            public String getSortProperty() {
                return RoleType.F_NAME.getLocalPart();
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleType>> item, String componentId,
                    IModel<RoleType> rowModel) {

                String name = rowModel.getObject().getName().toString();
                String oid = rowModel.getObject().getOid();

                AjaxLinkPanel ajaxLinkPanel = new AjaxLinkPanel(componentId, Model.of(name)) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        PageParameters parameters = new PageParameters();
                        parameters.add(OnePageParameterEncoder.PARAMETER, oid);

                        ((PageBase) getPage()).navigateToNext(PageRole.class, parameters);
                    }
                };
                ajaxLinkPanel.setOutputMarkupId(true);
                item.add(ajaxLinkPanel);
            }

        });

        columns.add(new AbstractColumn<>(createStringResource("Status")) {

            @Override
            public String getSortProperty() {
                return RoleType.F_ACTIVATION.getLocalPart();
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleType>> item, String componentId,
                    IModel<RoleType> rowModel) {
                item.add(new Label(componentId, rowModel.getObject().getActivation().getEffectiveStatus()));
            }

        });

        columns.add(new AbstractColumn<>(createStringResource("Members count")) {

            @Override
            public String getSortProperty() {
                return RoleType.F_ASSIGNMENT.getLocalPart();
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleType>> item, String componentId,
                    IModel<RoleType> rowModel) {
                Task task = getPageBase().createSimpleTask("countRoleMembers");

                Integer membersCount = getPageBase().getRoleAnalysisService()
                        .countUserTypeMembers(null, rowModel.getObject().getOid(),
                                task, result);

                item.add(new Label(componentId, membersCount));
            }

        });

        columns.add(new AbstractColumn<>(createStringResource("Inducement count")) {

            @Override
            public String getSortProperty() {
                return RoleType.F_INDUCEMENT.getLocalPart();
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleType>> item, String componentId,
                    IModel<RoleType> rowModel) {
                item.add(new Label(componentId, rowModel.getObject().getInducement().size()));
            }

        });

        columns.add(new AbstractColumn<>(createStringResource("Created Timestamp")) {

            @Override
            public String getSortProperty() {
                return RoleType.F_INDUCEMENT.getLocalPart();
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleType>> item, String componentId,
                    IModel<RoleType> rowModel) {
                item.add(new Label(componentId, resolveDateAndTime(rowModel.getObject().getMetadata().getCreateTimestamp())));
            }

        });

        columns.add(new AbstractColumn<>(createStringResource("State")) {

            @Override
            public String getSortProperty() {
                return RoleType.F_INDUCEMENT.getLocalPart();
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleType>> item, String componentId,
                    IModel<RoleType> rowModel) {

                Task task = getPageBase().createSimpleTask("Recompute operation result");
                OperationResult result = task.getResult();
                RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();

                RoleAnalysisChannelMode channelMode = RoleAnalysisChannelMode.MIGRATION;
                RoleType role = rowModel.getObject();
                channelMode.setObjectIdentifier(role.getOid());

                RoleAnalysisClusterType cluster = getObjectDetailsModels().getObjectType();
                String stateString = roleAnalysisService.recomputeAndResolveClusterOpStatus(
                        cluster.asPrismObject(), channelMode
                        , result, task);

                List<OperationExecutionType> operationExecution = cluster.getOperationExecution();
                PrismObject<TaskType> taskObject = roleAnalysisService.resolveTaskObject(operationExecution, channelMode, task, result);
                String taskOid = null;
                if (taskObject != null && taskObject.getOid() != null) {
                    taskOid = taskObject.getOid();
                }

                AjaxLinkPanel ajaxLinkPanel = getTaskLinkComponents(componentId, taskOid, stateString);
                item.add(ajaxLinkPanel);
            }

            @NotNull
            private AjaxLinkPanel getTaskLinkComponents(String componentId, String taskOid, String stateString) {
                AjaxLinkPanel ajaxLinkPanel = new AjaxLinkPanel(componentId, Model.of(stateString)) {
                    @Override
                    public boolean isEnabled() {
                        return taskOid != null;
                    }

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        super.onClick(target);
                        if (taskOid != null) {
                            DetailsPageUtil.dispatchToObjectDetailsPage(TaskType.class, taskOid,
                                    this, true);
                        }
                    }
                };
                ajaxLinkPanel.setOutputMarkupId(true);
                return ajaxLinkPanel;
            }

        });

        return columns;
    }

    private void onRefresh() {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, getObjectDetailsModels().getObjectType().getOid());
        parameters.add(ID_PANEL, getPanelConfiguration().getIdentifier());
        Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                .getObjectDetailsPage(RoleAnalysisClusterType.class);
        getPageBase().navigateToNext(detailsPageClass, parameters);
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

}
