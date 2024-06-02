/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.RoleAnalysisClusterAction;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile.RoleAnalysisCandidateRoleTileTable;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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

        Task task = getPageBase().createSimpleTask(OP_PREPARE_OBJECTS);
        RoleAnalysisClusterType cluster = getObjectDetailsModels().getObjectType();
        List<RoleAnalysisCandidateRoleType> candidateRoles = cluster.getCandidateRoles();

        HashMap<String, RoleAnalysisCandidateRoleType> cacheCandidate = new HashMap<>();
        List<RoleType> roles = new ArrayList<>();
        for (RoleAnalysisCandidateRoleType candidateRoleType : candidateRoles) {
            ObjectReferenceType candidateRoleRef = candidateRoleType.getCandidateRoleRef();
            PrismObject<RoleType> role = getPageBase().getRoleAnalysisService()
                    .getRoleTypeObject(candidateRoleRef.getOid(), task, result);
            if (Objects.nonNull(role)) {
                cacheCandidate.put(candidateRoleRef.getOid(), candidateRoleType);
                roles.add(role.asObjectable());
            }
        }

        RoleAnalysisCandidateRoleTileTable components = new RoleAnalysisCandidateRoleTileTable(ID_PANEL, getPageBase(),
                new LoadableDetachableModel<>() {
                    @Override
                    protected List<RoleType> load() {
                        return roles;
                    }
                }, cacheCandidate, cluster.getOid()) {

            @Override
            protected void onRefresh(AjaxRequestTarget target) {
                performRefresh();
            }
        };

        components.setOutputMarkupId(true);
        container.add(components);

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

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

}
