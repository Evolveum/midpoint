/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel;

import java.io.Serial;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemEditabilityHandler;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "roleAnalysisPanel")
@PanelInstance(
        identifier = "clusterStatistic",
        applicableForType = RoleAnalysisClusterType.class,
        display = @PanelDisplay(
                label = "AnalysisClusterStatisticType.clusterStatistic",
                icon = GuiStyleConstants.CLASS_REPORT_ICON,
                order = 40
        ),
        containerPath = "clusterStatistics",
        type = "AnalysisClusterStatisticType",
        expanded = true
)

@PanelInstance(
        identifier = "detectionOption",
        applicableForType = RoleAnalysisClusterType.class,
        display = @PanelDisplay(
                label = "RoleAnalysisClusterType.detectionOption",
                icon = GuiStyleConstants.CLASS_OPTIONS,
                order = 30
        ),
        containerPath = "detectionOption",
        type = "RoleAnalysisDetectionOptionType",
        expanded = true
)

@PanelInstance(
        identifier = "sessionOptions",
        applicableForType = RoleAnalysisSessionType.class,
        display = @PanelDisplay(
                label = "RoleAnalysisSessionType.sessionOptions",
                icon = GuiStyleConstants.CLASS_OPTIONS,
                order = 40
        ),
        containerPath = "roleModeOptions",
        type = "RoleAnalysisSessionOptionType",
        expanded = true)

@PanelInstance(
        identifier = "sessionOptions",
        applicableForType = RoleAnalysisSessionType.class,
        display = @PanelDisplay(
                label = "RoleAnalysisSessionType.sessionOptions",
                icon = GuiStyleConstants.CLASS_OPTIONS,
                order = 40
        ),
        containerPath = "userModeOptions",
        type = "UserAnalysisSessionOptionType",
        expanded = true
)

@PanelInstance(
        identifier = "sessionStatistics",
        applicableForType = RoleAnalysisSessionType.class,
        display = @PanelDisplay(
                label = "RoleAnalysisSessionType.sessionStatistic",
                icon = GuiStyleConstants.CLASS_REPORT_ICON,
                order = 50
        ),
        containerPath = "sessionStatistic",
        type = "RoleAnalysisSessionStatisticType",
        expanded = true
)

@PanelInstance(
        identifier = "sessionDefaultDetectionOption",
        applicableForType = RoleAnalysisSessionType.class,
        display = @PanelDisplay(
                label = "RoleAnalysisDetectionOptionType.defaultDetectionOption",
                icon = GuiStyleConstants.CLASS_OPTIONS,
                order = 30
        ),
        containerPath = "defaultDetectionOption",
        type = "RoleAnalysisDetectionOptionType",
        expanded = true
)

public class RoleAnalysisContainerPanel<AH extends AssignmentHolderType> extends AbstractObjectMainPanel<AH, ObjectDetailsModels<AH>> {
    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_PANEL = "panel";

    public RoleAnalysisContainerPanel(String id, AssignmentHolderDetailsModel<AH> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {

        SingleContainerPanel components = new SingleContainerPanel(ID_PANEL,
                getObjectWrapperModel(),
                getPanelConfiguration()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected ItemEditabilityHandler getEditabilityHandler() {
                ContainerPanelConfigurationType config = getPanelConfiguration();
                return setItemEditabilityHandler(config);
            }
        };
        add(components);
    }

    private static ItemEditabilityHandler setItemEditabilityHandler(ContainerPanelConfigurationType config) {
        for (VirtualContainersSpecificationType container : config.getContainer()) {
            if (container.getPath() != null
                    && (container.getPath().getItemPath().equivalent(RoleAnalysisClusterType.F_DETECTION_OPTION))) {
                return wrapper -> true;
            }
        }
        return wrapper -> false;
    }

}
