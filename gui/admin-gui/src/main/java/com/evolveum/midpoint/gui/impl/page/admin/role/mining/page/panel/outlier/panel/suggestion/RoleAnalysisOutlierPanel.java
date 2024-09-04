/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.panel.suggestion;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterAction;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile.RoleAnalysisOutlierAssociatedTileTable;

import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOutlierType;

import org.apache.wicket.markup.html.WebMarkupContainer;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;

import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@PanelType(name = "outlierPanel")
@PanelInstance(
        identifier = "outlierPanel",
        applicableForType = RoleAnalysisClusterType.class,
        childOf = RoleAnalysisClusterAction.class,
        display = @PanelDisplay(
                label = "RoleAnalysisOutlierType.basic.outlierPanel",
                icon = GuiStyleConstants.CLASS_ICON_OUTLIER,
                order = 10
        )
)
public class RoleAnalysisOutlierPanel extends AbstractObjectMainPanel<RoleAnalysisClusterType, ObjectDetailsModels<RoleAnalysisClusterType>> {

    private static final String ID_CONTAINER = "container";
    private static final String ID_PANEL = "panelId";

    public RoleAnalysisOutlierPanel(String id, ObjectDetailsModels<RoleAnalysisClusterType> model,
            ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {

        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        ObjectDetailsModels<RoleAnalysisClusterType> objectDetailsModels = getObjectDetailsModels();
        RoleAnalysisClusterType cluster = objectDetailsModels.getObjectType();
        @NotNull IModel<List<RoleAnalysisOutlierType>> outliers = getOutliers(cluster);
        RoleAnalysisOutlierAssociatedTileTable panel = new RoleAnalysisOutlierAssociatedTileTable(ID_PANEL, outliers, cluster);
        panel.setOutputMarkupId(true);
        container.add(panel);
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    private @NotNull IModel<List<RoleAnalysisOutlierType>> getOutliers(RoleAnalysisClusterType cluster) {
        PageBase pageBase = getPageBase();
        Task task = pageBase.createSimpleTask("Search outliers");
        OperationResult result = task.getResult();
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        return new LoadableModel<>() {
            @Override
            protected List<RoleAnalysisOutlierType> load() {
                return roleAnalysisService.findClusterOutliers(cluster, null, task, result);
            }
        };
    }
}
