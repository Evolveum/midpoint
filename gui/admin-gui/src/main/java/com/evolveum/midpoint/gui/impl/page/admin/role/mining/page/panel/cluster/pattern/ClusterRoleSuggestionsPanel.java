/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.pattern;

import static com.evolveum.midpoint.common.mining.utils.ExtractPatternUtils.transformDefaultPattern;

import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterAction;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel.RoleAnalysisDetectedPatternTable;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile.RoleAnalysisDetectedPatternTileTable;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;

import org.jetbrains.annotations.Nullable;

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

    @NotNull
    private RoleAnalysisDetectedPatternTileTable loadTable() {
        RoleAnalysisDetectedPatternTileTable components = new RoleAnalysisDetectedPatternTileTable(ID_PANEL, getPageBase(),
                new LoadableDetachableModel<>() {
                    @Override
                    protected List<DetectedPattern> load() {
                        return transformDefaultPattern(getObjectDetailsModels().getObjectType(), loadPatternSession());
                    }
                }) {

            @Override
            protected void onRefresh(AjaxRequestTarget target) {
                performOnRefresh();
            }
        };
        components.setOutputMarkupId(true);
        return components;
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    protected RoleAnalysisDetectedPatternTable getTable() {
        return (RoleAnalysisDetectedPatternTable) get(((PageBase) getPage()).createComponentPath(ID_CONTAINER, ID_PANEL));
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

    private @Nullable RoleAnalysisSessionType loadPatternSession() {
        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
        RoleAnalysisClusterType cluster = getObjectDetailsModels().getObjectType();
        Task task = getPageBase().createSimpleTask("Load pattern session");
        OperationResult result = task.getResult();
        PrismObject<RoleAnalysisSessionType> sessionTypeObject = roleAnalysisService.getSessionTypeObject(
                cluster.getRoleAnalysisSessionRef().getOid(), task, result);
        if (sessionTypeObject != null) {
            return sessionTypeObject.asObjectable();
        }
        return null;
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
