/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.panel.suggestion;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterAction;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisSessionDiscoveredOutlierPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel.PartitionObjectDtos;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel.RoleAnalysisOutlierTable;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

@PanelInstance(
        identifier = "accessNoiseOutlierPanel",
        applicableForType = RoleAnalysisClusterType.class,
        childOf = RoleAnalysisClusterAction.class,
        display = @PanelDisplay(
                label = "RoleAnalysisOutlierType.accessNoise.outlierPanel",
                icon = GuiStyleConstants.CLASS_ICON_OUTLIER,
                order = 20
        )
)

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

@PanelInstance(
        identifier = "classifiedOutlierPanel",
        applicableForType = RoleAnalysisSessionType.class,
        childOf = RoleAnalysisSessionDiscoveredOutlierPanel.class,
        display = @PanelDisplay(
                label = "RoleAnalysisOutlierType.classifiedOutlierPanel",
                icon = GuiStyleConstants.CLASS_ICON_OUTLIER,
                order = 40
        )
)

@PanelInstance(
        identifier = "unclassifiedOutlierPanel",
        applicableForType = RoleAnalysisSessionType.class,
        childOf = RoleAnalysisSessionDiscoveredOutlierPanel.class,
        display = @PanelDisplay(
                label = "RoleAnalysisOutlierType.unclassifiedOutlierPanel",
                icon = GuiStyleConstants.CLASS_ICON_OUTLIER,
                order = 50
        )
)

@PanelInstance(
        identifier = "uniqueOutlierPanel",
        applicableForType = RoleAnalysisClusterType.class,
        childOf = RoleAnalysisClusterAction.class,
        display = @PanelDisplay(
                label = "RoleAnalysisOutlierType.unique.outlierPanel",
                icon = GuiStyleConstants.CLASS_ICON_OUTLIER,
                order = 30
        )
)
public class RoleAnalysisOutlierListPanel extends AbstractObjectMainPanel<AssignmentHolderType, ObjectDetailsModels<AssignmentHolderType>> {

    private static final String DOT_CLASS = RoleAnalysisOutlierListPanel.class.getName() + ".";
    private static final String OP_INIT_PARTITION_DTO = DOT_CLASS + "initPartitionDto";

    private static final String ID_CONTAINER = "container";
    private static final String ID_PANEL = "panelId";

    public RoleAnalysisOutlierListPanel(String id, ObjectDetailsModels<AssignmentHolderType> model,
            ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {

        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        RoleAnalysisOutlierTable table = new RoleAnalysisOutlierTable(ID_PANEL, buildModel());
        table.setOutputMarkupId(true);
        container.add(table);
    }

    private @NotNull LoadableModel<PartitionObjectDtos> buildModel() {
        return new LoadableModel<>() {
            @Override
            protected PartitionObjectDtos load() {
                Task task = getPageBase().createSimpleTask(OP_INIT_PARTITION_DTO);
                OperationResult result = task.getResult();
                RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
                return new PartitionObjectDtos(getObjectDetailsModels().getObjectType(), roleAnalysisService, task, result) {
                    @Override
                    public OutlierCategoryType matchOutlierCategory() {
                        return getRequiredOutlierCategory();
                    }
                };
            }
        };
    }

    private @Nullable OutlierCategoryType getRequiredOutlierCategory() {
        if (getPanelIdentifier().equals("accessNoiseOutlierPanel")) {
            return new OutlierCategoryType().outlierSpecificCategory(OutlierSpecificCategoryType.ACCESS_NOISE);
        }

        if (getPanelIdentifier().equals("classifiedOutlierPanel")) {
            return new OutlierCategoryType().outlierClusterCategory(OutlierClusterCategoryType.INNER_OUTLIER);
        }

        if (getPanelIdentifier().equals("unclassifiedOutlierPanel")) {
            return new OutlierCategoryType().outlierClusterCategory(OutlierClusterCategoryType.OUTER_OUTLIER);
        }

        if (getPanelIdentifier().equals("uniqueOutlierPanel")) {
            return new OutlierCategoryType().outlierSpecificCategory(OutlierSpecificCategoryType.UNIQUE_OBJECT);
        }

        return null;
    }

    public String getPanelIdentifier() {
        ContainerPanelConfigurationType panelConfiguration = getPanelConfiguration();
        return panelConfiguration.getIdentifier();
    }

    @Override
    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

}
