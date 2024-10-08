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
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOptionsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session.RoleAnalysisRoleSessionOptions;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session.RoleAnalysisUserSessionOptions;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

@PanelType(name = "roleAnalysisPanel")

@PanelInstance(
        identifier = "detectionOption",
        applicableForType = RoleAnalysisClusterType.class,
        display = @PanelDisplay(
                label = "RoleAnalysisClusterType.detectionOption",
                icon = GuiStyleConstants.CLASS_OPTIONS_COGS,
                order = 20
        ),
        childOf = RoleAnalysisClusterOptionsPanel.class,
        containerPath = "detectionOption",
        type = "RoleAnalysisDetectionOptionType",
        expanded = true
)

@PanelInstance(
        identifier = "sessionDefaultDetectionOption",
        applicableForType = RoleAnalysisSessionType.class,
        display = @PanelDisplay(
                label = "RoleAnalysisDetectionOptionType.defaultDetectionOption",
                icon = GuiStyleConstants.CLASS_OPTIONS_COG,
                order = 30
        ),
        childOf = RoleAnalysisUserSessionOptions.class,
        containerPath = "defaultDetectionOption",
        type = "RoleAnalysisDetectionOptionType",
        expanded = true
)

@PanelInstance(
        identifier = "sessionDefaultDetectionOption",
        applicableForType = RoleAnalysisSessionType.class,
        display = @PanelDisplay(
                label = "RoleAnalysisDetectionOptionType.defaultDetectionOption",
                icon = GuiStyleConstants.CLASS_OPTIONS_COG,
                order = 30
        ),
        childOf = RoleAnalysisRoleSessionOptions.class,
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

        @SuppressWarnings({ "rawtypes", "unchecked" })
        SingleContainerPanel components = new SingleContainerPanel(ID_PANEL,
                getObjectWrapperModel(),
                getPanelConfiguration()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected ItemVisibility getVisibility(@SuppressWarnings("rawtypes") ItemWrapper itemWrapper) {
                return getBasicTabVisibility(itemWrapper.getPath());
            }

            @Override
            protected ItemEditabilityHandler getEditabilityHandler() {
                return wrapper -> false;
            }
        };
        add(components);
    }

    private @NotNull ItemVisibility getBasicTabVisibility(@NotNull ItemPath path) {
        RoleAnalysisProcessModeType processMode = null;
        RoleAnalysisProcedureType analysisProcedureType = null;
        if (getObjectWrapper().getObject().getRealValue() instanceof RoleAnalysisSessionType session) {
            RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
            analysisProcedureType = analysisOption.getAnalysisProcedureType();
            processMode = analysisOption.getProcessMode();
        }

        if (processMode != null && processMode.equals(RoleAnalysisProcessModeType.ROLE)
                && path.equivalent(ItemPath.create(RoleAnalysisSessionType.F_ROLE_MODE_OPTIONS,
                AbstractAnalysisSessionOptionType.F_IS_INDIRECT))) {
            return ItemVisibility.HIDDEN;
        }

        assert analysisProcedureType != null;
        boolean isOutlierDetection = analysisProcedureType.equals(RoleAnalysisProcedureType.OUTLIER_DETECTION);

        if (!isOutlierDetection) {
            if (path.equivalent(ItemPath.create(RoleAnalysisSessionType.F_DEFAULT_DETECTION_OPTION,
                    RoleAnalysisDetectionOptionType.F_FREQUENCY_THRESHOLD))
                    || path.equivalent(ItemPath.create(RoleAnalysisSessionType.F_DEFAULT_DETECTION_OPTION,
                    RoleAnalysisDetectionOptionType.F_STANDARD_DEVIATION))
                    || path.equivalent(ItemPath.create(RoleAnalysisSessionType.F_DEFAULT_DETECTION_OPTION))) {
                return ItemVisibility.HIDDEN;
            }
        } else {
            if (path.equivalent(ItemPath.create(RoleAnalysisSessionType.F_DEFAULT_DETECTION_OPTION,
                    RoleAnalysisDetectionOptionType.F_FREQUENCY_RANGE))
                    || path.equivalent(ItemPath.create(RoleAnalysisSessionType.F_DEFAULT_DETECTION_OPTION,
                    RoleAnalysisDetectionOptionType.F_MIN_ROLES_OCCUPANCY))
                    || path.equivalent(ItemPath.create(RoleAnalysisSessionType.F_DEFAULT_DETECTION_OPTION,
                    RoleAnalysisDetectionOptionType.F_MIN_USER_OCCUPANCY))) {
                return ItemVisibility.HIDDEN;
            }
        }

        return ItemVisibility.AUTO;
    }

}
