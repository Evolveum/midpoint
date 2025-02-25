/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile.component.RoleAnalysisCandidateRoleTileTable;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile.model.RoleAnalysisCandidateRolesDto;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

@PanelType(name = "candidateRoles")
@PanelInstance(
        identifier = "candidateRoles",
        applicableForType = RoleAnalysisClusterType.class,
        childOf = RoleAnalysisClusterAction.class,
        display = @PanelDisplay(
                label = "RoleAnalysisClusterType.candidateRoles",
                icon = GuiStyleConstants.CLASS_GROUP_ICON,
                order = 20
        )
)
public class CandidateRolesPanel extends AbstractObjectMainPanel<RoleAnalysisClusterType, ObjectDetailsModels<RoleAnalysisClusterType>> {

    private static final String ID_CONTAINER = "container";
    private static final String ID_PANEL = "panelId";
    private static final String DOT_CLASS = CandidateRolesPanel.class.getName() + ".";
    private static final String OP_PREPARE_OBJECTS = DOT_CLASS + "prepareObjects";
    private static final String OP_UPDATE_STATUS = DOT_CLASS + "updateOperationStatus";

    OperationResult result = new OperationResult(OP_PREPARE_OBJECTS);

    public CandidateRolesPanel(String id, ObjectDetailsModels<RoleAnalysisClusterType> model,
            ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        recomputeClusterOpStatus();
        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        RoleAnalysisCandidateRoleTileTable components = new RoleAnalysisCandidateRoleTileTable(ID_PANEL,
                CandidateRolesPanel.this.getPageBase(),
                builtModel()) {

            @Override
            protected void onRefresh(@NotNull AjaxRequestTarget target) {
                performRefresh();
            }
        };

        components.setOutputMarkupId(true);
        container.add(components);

    }

    private @NotNull LoadableModel<RoleAnalysisCandidateRolesDto> builtModel() {
        return new LoadableModel<>() {
            @Override
            protected @NotNull RoleAnalysisCandidateRolesDto load() {
                PageBase pageBase = CandidateRolesPanel.this.getPageBase();
                Task task = pageBase.createSimpleTask(OP_PREPARE_OBJECTS);
                RoleAnalysisClusterType cluster = getObjectDetailsModels().getObjectType();
                RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
                return new RoleAnalysisCandidateRolesDto(roleAnalysisService, cluster, task, result);
            }
        };
    }

    private void performRefresh() {
        recomputeClusterOpStatus();

        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, getObjectDetailsModels().getObjectType().getOid());
        parameters.add(ID_PANEL, getPanelConfiguration().getIdentifier());
        Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                .getObjectDetailsPage(RoleAnalysisClusterType.class);
        getPageBase().navigateToNext(detailsPageClass, parameters);
    }

    private void recomputeClusterOpStatus() {
        Task task = getPageBase().createSimpleTask(OP_UPDATE_STATUS);
        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
        roleAnalysisService.recomputeAndResolveClusterOpStatus(
                getObjectWrapperObject().getOid(),
                result, task, false, getPageBase().getModelInteractionService());
    }

    @Override
    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

}
