/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.RoleAnalysisClusterAction;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile.RoleAnalysisMigrationRoleTileTable;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

@PanelType(name = "migratedRoles")
@PanelInstance(
        identifier = "migratedRoles",
        applicableForType = RoleAnalysisClusterType.class,
        childOf = RoleAnalysisClusterAction.class,
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

        RoleAnalysisMigrationRoleTileTable roleAnalysisMigrationRoleTileTable = new RoleAnalysisMigrationRoleTileTable(ID_PANEL,
                getPageBase(), new LoadableDetachableModel<>() {
            @Override
            protected List<RoleType> load() {
                return roles;
            }
        }, cluster.getOid()) {
            @Override
            protected void onRefresh(AjaxRequestTarget target) {
                performOnRefresh();
            }
        };
        roleAnalysisMigrationRoleTileTable.setOutputMarkupId(true);
        container.add(roleAnalysisMigrationRoleTileTable);

    }

    private void performOnRefresh() {
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
