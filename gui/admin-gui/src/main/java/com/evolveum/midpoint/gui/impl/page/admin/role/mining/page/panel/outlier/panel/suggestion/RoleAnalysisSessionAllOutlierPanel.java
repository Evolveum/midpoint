/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.panel.suggestion;

import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisSessionDiscoveredOutlierPanel;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile.RoleAnalysisOutlierAssociatedTileTable;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOutlierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

@PanelType(name = "allOutlierPanel")
@PanelInstance(
        identifier = "allOutlierPanel",
        applicableForType = RoleAnalysisSessionType.class,
        childOf = RoleAnalysisSessionDiscoveredOutlierPanel.class,
        defaultPanel = true,
        display = @PanelDisplay(
                label = "RoleAnalysisOutlierType.allOutlierPanel",
                icon = GuiStyleConstants.CLASS_ICON_OUTLIER,
                order = 30
        )
)
@PanelInstance(
        identifier = "outliers",
        applicableForType = RoleAnalysisSessionType.class,
        defaultPanel = true,
        display = @PanelDisplay(
                label = "RoleAnalysisOutlierType.outliers",
                icon = GuiStyleConstants.CLASS_ICON_OUTLIER,
                order = 30
        )
)
public class RoleAnalysisSessionAllOutlierPanel extends AbstractObjectMainPanel<RoleAnalysisSessionType, ObjectDetailsModels<RoleAnalysisSessionType>> {

    private static final String ID_CONTAINER = "container";
    private static final String ID_PANEL = "panelId";

    public RoleAnalysisSessionAllOutlierPanel(String id, ObjectDetailsModels<RoleAnalysisSessionType> model,
            ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {

        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        ObjectDetailsModels<RoleAnalysisSessionType> objectDetailsModels = getObjectDetailsModels();
        RoleAnalysisSessionType session = objectDetailsModels.getObjectType();
        @NotNull IModel<List<RoleAnalysisOutlierType>> outliers = getOutliers(session);
        RoleAnalysisOutlierAssociatedTileTable panel = new RoleAnalysisOutlierAssociatedTileTable(ID_PANEL, outliers, session);
        panel.setOutputMarkupId(true);
        container.add(panel);
    }

    @Override
    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    private @NotNull IModel<List<RoleAnalysisOutlierType>> getOutliers(RoleAnalysisSessionType session) {
        PageBase pageBase = getPageBase();
        Task task = pageBase.createSimpleTask("Search outliers");
        OperationResult result = task.getResult();
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        List<RoleAnalysisOutlierType> sessionOutliers = roleAnalysisService.getSessionOutliers(session.getOid(), null, task, result);

        return new LoadableModel<>() {
            @Override
            protected List<RoleAnalysisOutlierType> load() {
                return sessionOutliers;
            }
        };
    }
}
