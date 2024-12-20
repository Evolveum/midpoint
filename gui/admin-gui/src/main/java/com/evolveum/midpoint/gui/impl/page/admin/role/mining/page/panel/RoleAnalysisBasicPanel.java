/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemEditabilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOptionsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.RoleAnalysisOutlierSettings;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session.RoleAnalysisRoleSessionOptions;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session.RoleAnalysisUserSessionOptions;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@PanelType(name = "miningBasic", defaultContainerPath = "empty")

@PanelInstance(identifier = "sessionBasic",
        applicableForType = RoleAnalysisSessionType.class,
        childOf = RoleAnalysisUserSessionOptions.class,
        display = @PanelDisplay(
                label = "RoleAnalysis.basic.panel",
                icon = GuiStyleConstants.CLASS_INFO_CIRCLE,
                order = 10))

@PanelInstance(identifier = "sessionBasic",
        applicableForType = RoleAnalysisSessionType.class,
        childOf = RoleAnalysisRoleSessionOptions.class,
        display = @PanelDisplay(
                label = "RoleAnalysis.basic.panel",
                icon = GuiStyleConstants.CLASS_INFO_CIRCLE,
                order = 10))

@PanelInstance(identifier = "clusterBasic",
        applicableForType = RoleAnalysisClusterType.class,
        childOf = RoleAnalysisClusterOptionsPanel.class,
        display = @PanelDisplay(
                label = "RoleAnalysis.basic.panel",
                icon = GuiStyleConstants.CLASS_INFO_CIRCLE,
                order = 10))

@PanelInstance(identifier = "outlierBasic",
        applicableForType = RoleAnalysisOutlierType.class,
        childOf = RoleAnalysisOutlierSettings.class,
        display = @PanelDisplay(
                label = "RoleAnalysis.basic.panel",
                icon = GuiStyleConstants.CLASS_INFO_CIRCLE,
                order = 10))

public class RoleAnalysisBasicPanel<AH extends AssignmentHolderType> extends AbstractObjectMainPanel<AH, ObjectDetailsModels<AH>> {

    private static final String ID_MAIN_PANEL = "main";

    public RoleAnalysisBasicPanel(String id, AssignmentHolderDetailsModel<AH> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void initLayout() {
        SingleContainerPanel mainPanel = new SingleContainerPanel(ID_MAIN_PANEL, getObjectWrapperModel(), getPanelConfiguration()) {

            @Override
            protected ItemVisibility getVisibility(@NotNull ItemWrapper itemWrapper) {
                return getBasicTabVisibility(itemWrapper.getPath());
            }

            @Contract(pure = true)
            @Override
            protected @NotNull ItemEditabilityHandler getEditabilityHandler() {
                return wrapper -> true;
            }

        };
        add(mainPanel);
    }

    private ItemVisibility getBasicTabVisibility(ItemPath path) {
        if (RoleAnalysisSessionType.F_NAME.equivalent(path)) {
            return ItemVisibility.AUTO;
        }

        if (RoleAnalysisSessionType.F_DOCUMENTATION.equivalent(path)) {
            return ItemVisibility.AUTO;
        }

        if (RoleAnalysisSessionType.F_DESCRIPTION.equivalent(path)) {
            return ItemVisibility.AUTO;
        }

        if (RoleAnalysisOutlierType.F_OBJECT_REF.equivalent(path)) {
            return ItemVisibility.AUTO;
        }

        return ItemVisibility.HIDDEN;
    }

}
