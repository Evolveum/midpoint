/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.pattern;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterAction;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile.component.RoleAnalysisDetectedPatternTileTable;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile.model.RoleAnalysisDetectedPatternsDto;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;

@PanelType(name = "clusterRoleSuggestions")
@PanelInstance(
        identifier = "clusterRoleSuggestions",
        applicableForType = RoleAnalysisClusterType.class,
        childOf = RoleAnalysisClusterAction.class,
        display = @PanelDisplay(
                label = "RoleAnalysisDetectionPatternType.detectedPattern",
                icon = GuiStyleConstants.CLASS_ICON_SEARCH,
                order = 10
        )
)
public class ClusterRoleSuggestionsPanel extends AbstractObjectMainPanel<RoleAnalysisClusterType, ObjectDetailsModels<RoleAnalysisClusterType>> {

    private static final String DOT_CLASS = ClusterRoleSuggestionsPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_OBJECTS = DOT_CLASS + "loadObjects";

    private static final String ID_CONTAINER = "container";
    private static final String ID_PANEL = "panelId";

    public ClusterRoleSuggestionsPanel(String id, ObjectDetailsModels<RoleAnalysisClusterType> model,
            ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        updatePatternIfRequired();

        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        RoleAnalysisDetectedPatternTileTable components = loadTable();
        container.add(components);
    }

    private @NotNull RoleAnalysisDetectedPatternTileTable loadTable() {
        RoleAnalysisDetectedPatternTileTable components = new RoleAnalysisDetectedPatternTileTable(ID_PANEL, getPageBase(),
                builtModel()) {

            @Override
            protected void onRefresh(AjaxRequestTarget target) {
                performOnRefresh();
            }
        };
        components.setOutputMarkupId(true);
        return components;
    }

    private @NotNull LoadableModel<RoleAnalysisDetectedPatternsDto> builtModel() {
        return new LoadableModel<>() {
            @Override
            protected @NotNull RoleAnalysisDetectedPatternsDto load() {
                RoleAnalysisClusterType cluster = getObjectDetailsModels().getObjectType();
                RoleAnalysisService roleAnalysisService = ((PageBase) getPage()).getRoleAnalysisService();
                OperationResult result = new OperationResult(OPERATION_LOAD_OBJECTS);
                return new RoleAnalysisDetectedPatternsDto(roleAnalysisService, cluster, result);
            }
        };
    }

    @Override
    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    private void performOnRefresh() {
        updatePatternIfRequired();

        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, getObjectDetailsModels().getObjectType().getOid());
        parameters.add(ID_PANEL, getPanelConfiguration().getIdentifier());
        Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                .getObjectDetailsPage(RoleAnalysisClusterType.class);
        getPageBase().navigateToNext(detailsPageClass, parameters);
    }

    private void updatePatternIfRequired() {
        PageBase pageBase = getPageBase();
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        RoleAnalysisClusterType cluster = getObjectDetailsModels().getObjectType();
        Task task = pageBase.createSimpleTask("Recompute and resolve cluster operation status");
        OperationResult result = task.getResult();
        roleAnalysisService
                .recomputeAndResolveClusterOpStatus(cluster.getOid(), result, task, false,
                        pageBase.getModelInteractionService());
    }

}
