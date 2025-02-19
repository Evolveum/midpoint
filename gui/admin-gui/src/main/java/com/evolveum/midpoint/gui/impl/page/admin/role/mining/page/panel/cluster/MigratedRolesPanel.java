/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile.component.RoleAnalysisMigrationRoleTileTable;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile.model.RoleAnalysisMigratedRolesDto;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;

import org.jetbrains.annotations.NotNull;

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
        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        RoleAnalysisMigrationRoleTileTable roleAnalysisMigrationRoleTileTable = new RoleAnalysisMigrationRoleTileTable(ID_PANEL,
                getPageBase(), () -> {
            PageBase pageBase = MigratedRolesPanel.this.getPageBase();
            Task task = pageBase.createSimpleTask(OP_PREPARE_OBJECTS);
            RoleAnalysisClusterType cluster = getObjectDetailsModels().getObjectType();
            RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
            return new RoleAnalysisMigratedRolesDto(roleAnalysisService, cluster, task, result);
        }) {
            @Override
            protected void onRefresh(@NotNull AjaxRequestTarget target) {
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

    @Override
    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

}
