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
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session.RoleAnalysisRoleSessionOptions;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session.RoleAnalysisUserSessionOptions;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.PageRoleAnalysisSession.reviseProcedureType;

@PanelType(name = "roleAnalysisSessionPanel")

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

public class RoleAnalysisSessionContainerPanel<AH extends AssignmentHolderType> extends AbstractObjectMainPanel<AH, ObjectDetailsModels<AH>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(RoleAnalysisSessionContainerPanel.class);

    private static final String ID_PANEL = "panel";

    public RoleAnalysisSessionContainerPanel(String id, AssignmentHolderDetailsModel<AH> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        RoleAnalysisOptionType sessionAnalysisOptions = extractSessionAnalysisOptions();
        @SuppressWarnings({ "rawtypes", "unchecked" })
        SingleContainerPanel components = new SingleContainerPanel(ID_PANEL,
                getObjectWrapperModel(),
                getPanelConfiguration()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected @NotNull ItemVisibility getVisibility(@SuppressWarnings("rawtypes") @NotNull ItemWrapper itemWrapper) {
                return getBasicTabVisibility(itemWrapper.getPath(), sessionAnalysisOptions);
            }

            @Override
            protected ItemEditabilityHandler getEditabilityHandler() {
                return wrapper -> false;
            }
        };
        add(components);
    }

    private RoleAnalysisOptionType extractSessionAnalysisOptions() {
        RoleAnalysisProcessModeType processMode = null;
        RoleAnalysisProcedureType analysisProcedureType = null;

        PrismObject<AH> object = getObjectWrapper().getObject();
        if (object.getRealValue() instanceof RoleAnalysisSessionType session) {
            RoleAnalysisOptionType analysisOption = session.getAnalysisOption();

            analysisProcedureType = analysisOption.getAnalysisProcedureType();
            if (analysisProcedureType == null) {
                analysisProcedureType = reviseProcedureType(session);
            }

            processMode = analysisOption.getProcessMode();
        }

        return new RoleAnalysisOptionType()
                .analysisProcedureType(analysisProcedureType)
                .processMode(processMode);
    }

    private @NotNull ItemVisibility getBasicTabVisibility(
            @NotNull ItemPath path,
            @NotNull RoleAnalysisOptionType sessionAnalysisOptions) {

        RoleAnalysisProcessModeType processMode = sessionAnalysisOptions.getProcessMode();
        RoleAnalysisProcedureType analysisProcedureType = sessionAnalysisOptions.getAnalysisProcedureType();

        if (processMode == null || analysisProcedureType == null) {
            LOGGER.debug("Process mode or analysis procedure type is null. Cannot determine visibility for {}", path);
            return ItemVisibility.AUTO;
        }

        if (processMode == RoleAnalysisProcessModeType.ROLE
                && path.equivalent(ItemPath.create(
                RoleAnalysisSessionType.F_ROLE_MODE_OPTIONS, AbstractAnalysisSessionOptionType.F_IS_INDIRECT))) {
            return ItemVisibility.HIDDEN;
        }

        boolean isOutlierDetection = analysisProcedureType.equals(RoleAnalysisProcedureType.OUTLIER_DETECTION);

        if (isOutlierDetection) {
            if (matchesAny(path,
                    RoleAnalysisDetectionOptionType.F_FREQUENCY_RANGE,
                    RoleAnalysisDetectionOptionType.F_MIN_ROLES_OCCUPANCY,
                    RoleAnalysisDetectionOptionType.F_MIN_USER_OCCUPANCY)) {
                return ItemVisibility.HIDDEN;
            }
        } else {
            if (matchesAny(path,
                    RoleAnalysisDetectionOptionType.F_FREQUENCY_THRESHOLD,
                    RoleAnalysisDetectionOptionType.F_STANDARD_DEVIATION,
                    RoleAnalysisDetectionOptionType.F_SENSITIVITY)) {
                return ItemVisibility.HIDDEN;
            }
        }

        return ItemVisibility.AUTO;
    }

    private boolean matchesAny(@NotNull ItemPath path, Object @NotNull ... segments) {
        for (Object segment : segments) {
            if (path.equivalent(ItemPath.create(RoleAnalysisSessionType.F_DEFAULT_DETECTION_OPTION, segment))) {
                return true;
            }
        }
        return false;
    }
}
