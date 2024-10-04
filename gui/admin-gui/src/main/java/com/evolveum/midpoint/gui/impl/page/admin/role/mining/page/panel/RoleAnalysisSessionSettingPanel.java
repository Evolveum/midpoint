/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel;

import java.io.Serial;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemEditabilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session.RoleAnalysisRoleSessionOptions;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session.RoleAnalysisUserSessionOptions;

import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "sessionOptionsPanel")
@PanelInstance(
        identifier = "sessionOptions",
        applicableForType = RoleAnalysisSessionType.class,
        display = @PanelDisplay(
                label = "RoleAnalysisSessionType.sessionOptions",
                icon = GuiStyleConstants.CLASS_OPTIONS_COGS,
                order = 20
        ),
        childOf = RoleAnalysisRoleSessionOptions.class,
        containerPath = "roleModeOptions",
        type = "RoleAnalysisSessionOptionType",
        expanded = true)

@PanelInstance(
        identifier = "sessionOptions",
        applicableForType = RoleAnalysisSessionType.class,
        display = @PanelDisplay(
                label = "RoleAnalysisSessionType.sessionOptions",
                icon = GuiStyleConstants.CLASS_OPTIONS_COG,
                order = 20
        ),
        childOf = RoleAnalysisUserSessionOptions.class,
        containerPath = "userModeOptions",
        type = "UserAnalysisSessionOptionType",
        expanded = true
)
public class RoleAnalysisSessionSettingPanel extends AbstractObjectMainPanel<RoleAnalysisSessionType, ObjectDetailsModels<RoleAnalysisSessionType>> {

    private static final String ID_PANEL = "panelId";

    public RoleAnalysisSessionSettingPanel(String id, ObjectDetailsModels<RoleAnalysisSessionType> model, ContainerPanelConfigurationType config) {
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
        RoleAnalysisCategoryType analysisCategory = null;
        RoleAnalysisProcessModeType processMode = null;
        RoleAnalysisSessionType session = getObjectWrapper().getObject().getRealValue();
        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        analysisCategory = analysisOption.getAnalysisCategory();
        processMode = analysisOption.getProcessMode();


        if (path.equivalent(ItemPath.create(RoleAnalysisSessionType.F_ROLE_MODE_OPTIONS,
                AbstractAnalysisSessionOptionType.F_USER_ANALYSIS_ATTRIBUTE_SETTING))
                || path.equivalent(ItemPath.create(RoleAnalysisSessionType.F_ROLE_MODE_OPTIONS,
                AbstractAnalysisSessionOptionType.F_CLUSTERING_ATTRIBUTE_SETTING))
                || path.equivalent(ItemPath.create(RoleAnalysisSessionType.F_USER_MODE_OPTIONS,
                AbstractAnalysisSessionOptionType.F_CLUSTERING_ATTRIBUTE_SETTING))
                || path.equivalent(ItemPath.create(RoleAnalysisSessionType.F_USER_MODE_OPTIONS,
                AbstractAnalysisSessionOptionType.F_USER_ANALYSIS_ATTRIBUTE_SETTING))) {
            return ItemVisibility.HIDDEN;
        }

        if (processMode != null && processMode.equals(RoleAnalysisProcessModeType.ROLE)
                && path.equivalent(ItemPath.create(RoleAnalysisSessionType.F_ROLE_MODE_OPTIONS,
                AbstractAnalysisSessionOptionType.F_IS_INDIRECT))) {
            return ItemVisibility.HIDDEN;
        }

        return ItemVisibility.AUTO;
    }

}
