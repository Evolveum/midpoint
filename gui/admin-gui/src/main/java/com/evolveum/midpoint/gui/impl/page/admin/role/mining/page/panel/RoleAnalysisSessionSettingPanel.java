/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session.RoleAnalysisRoleSessionOptions;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session.RoleAnalysisUserSessionOptions;

import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPrismContainerValuePanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;

import com.evolveum.midpoint.web.model.PrismContainerValueWrapperModel;

import org.jetbrains.annotations.Contract;
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
        RoleAnalysisSessionType session = getObjectWrapper().getObject().getRealValue();
        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        RoleAnalysisProcessModeType processMode = analysisOption.getProcessMode();
        ItemName itemName;
        if (processMode == RoleAnalysisProcessModeType.ROLE) {
            itemName = RoleAnalysisSessionType.F_ROLE_MODE_OPTIONS;
        } else {
            itemName = RoleAnalysisSessionType.F_USER_MODE_OPTIONS;
        }

        VerticalFormPrismContainerValuePanel<Containerable, PrismContainerValueWrapper<Containerable>> panel = new VerticalFormPrismContainerValuePanel<>(
                ID_PANEL,
                PrismContainerValueWrapperModel.fromContainerWrapper(getObjectWrapperModel(), itemName),
                createItemPanelSettings()){
            @Contract(pure = true)
            @Override
            protected @NotNull ItemEditabilityHandler getEditabilityHandler() {
                return wrapper -> false;
            }

            @Override
            protected boolean isShowEmptyButtonVisible() {
                return false;
            }

            @Override
            protected boolean isVisibleSubContainer(PrismContainerWrapper<? extends Containerable> c) {
                return true;
            }
        };

        add(panel);
    }

    private @NotNull ItemPanelSettings createItemPanelSettings() {
        ItemPanelSettings settings = new ItemPanelSettingsBuilder()
                .visibilityHandler(getVisibilityHandler())
                .mandatoryHandler(getMandatoryHandler())
                .headerVisibility(false)
                .editabilityHandler(wrapper -> false).build();
        settings.setConfig(getPanelConfiguration());
        return settings;
    }

    protected ItemVisibilityHandler getVisibilityHandler() {
        return wrapper -> ItemVisibility.AUTO;
    }

    protected ItemMandatoryHandler getMandatoryHandler() {
        return null;
    }

}
