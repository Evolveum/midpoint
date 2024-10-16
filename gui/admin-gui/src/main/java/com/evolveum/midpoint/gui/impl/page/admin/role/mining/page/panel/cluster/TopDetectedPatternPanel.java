/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile.RoleAnalysisDetectedPatternTileTable;

import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel.RoleAnalysisDetectedPatternTable;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.common.mining.utils.ExtractPatternUtils.transformDefaultPattern;
@PanelType(name = "sessionRoleSuggestions")
@PanelInstance(
        identifier = "sessionRoleSuggestions",
        applicableForType = RoleAnalysisSessionType.class,
        display = @PanelDisplay(
                label = "RoleAnalysisDetectionPatternType.action.suggestion",
                icon = GuiStyleConstants.CLASS_ICON_SEARCH,
                order = 30
        )
)
public class TopDetectedPatternPanel extends AbstractObjectMainPanel<RoleAnalysisSessionType, ObjectDetailsModels<RoleAnalysisSessionType>> {

    private static final String ID_CONTAINER = "container";
    private static final String ID_PANEL = "panelId";

    public TopDetectedPatternPanel(String id, ObjectDetailsModels<RoleAnalysisSessionType> model,
            ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {

        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        @NotNull RoleAnalysisDetectedPatternTileTable components = loadTable();
        container.add(components);
    }

    private @NotNull RoleAnalysisDetectedPatternTileTable loadTable() {
        RoleAnalysisSessionType session = getObjectDetailsModels().getObjectType();
        LoadableDetachableModel<List<DetectedPattern>> loadableDetachableModel = new LoadableDetachableModel<>() {
            @Override
            protected List<DetectedPattern> load() {
                return getTopPatterns(session);
            }
        };

        RoleAnalysisDetectedPatternTileTable components = new RoleAnalysisDetectedPatternTileTable(ID_PANEL, getPageBase(),
                loadableDetachableModel) {

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
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, getObjectDetailsModels().getObjectType().getOid());
        parameters.add(ID_PANEL, getPanelConfiguration().getIdentifier());
        Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                .getObjectDetailsPage(RoleAnalysisSessionType.class);
        getPageBase().navigateToNext(detailsPageClass, parameters);
    }

    //TODO reduction vs attribute confidence
    // not correct way how to load top patterns
    private @NotNull List<DetectedPattern> getTopPatterns(RoleAnalysisSessionType session) {
        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();

        Task task = getPageBase().createSimpleTask("getTopPatterns");
        OperationResult result = task.getResult();
        List<PrismObject<RoleAnalysisClusterType>> sessionClusters = roleAnalysisService.searchSessionClusters(
                session, task, result);

        List<DetectedPattern> topDetectedPatterns = new ArrayList<>();
        for (PrismObject<RoleAnalysisClusterType> cluster : sessionClusters) {
            List<DetectedPattern> detectedPatterns = transformDefaultPattern(cluster.asObjectable(), session);

            double maxOverallConfidence = 0;
            DetectedPattern topDetectedPattern = null;
            for (DetectedPattern detectedPattern : detectedPatterns) {
                double itemsConfidence = detectedPattern.getItemsConfidence();
                double reductionFactorConfidence = detectedPattern.getReductionFactorConfidence();
                double overallConfidence = itemsConfidence + reductionFactorConfidence;
                if (overallConfidence > maxOverallConfidence) {
                    maxOverallConfidence = overallConfidence;
                    topDetectedPattern = detectedPattern;
                }
            }
            if (topDetectedPattern != null) {
                topDetectedPatterns.add(topDetectedPattern);
            }

        }

        topDetectedPatterns.sort((o1, o2) -> Double.compare(o2.getMetric(), o1.getMetric()));
        return topDetectedPatterns;
    }

}
