/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component;

import java.io.Serial;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.focus.FocusDetailsModels;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VirtualContainersSpecificationType;

/**
 * Panel telling whether the role assigns itself and under which condition.
 *
 * @author jjarabinec
 */
@PanelType(name = "autoassign", defaultContainerPath = "autoassign")
@PanelInstance(identifier = "autoassign",
        applicableForType = AbstractRoleType.class,
        display = @PanelDisplay(label = "AutoassignPanel.label", icon = GuiStyleConstants.CLASS_CIRCLE_FULL, order = 65),
        hiddenContainers = {
                "autoassign/focus/mapping/extension",
                "autoassign/focus/mapping/metadataMapping",
                "autoassign/focus/selector/parent"
        })
public class AutoassignPanel<AR extends AbstractRoleType> extends AbstractObjectMainPanel<AR, FocusDetailsModels<AR>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_AUTOASSIGN = "autoassign";

    public AutoassignPanel(String id, FocusDetailsModels<AR> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        SingleContainerPanel panel =
                new SingleContainerPanel(ID_AUTOASSIGN, getObjectWrapperModel(), getPanelConfiguration()) {

                    @Override
                    protected ItemVisibility getVisibility(ItemWrapper itemWrapper) {
                        return isHiddenByConfiguration(itemWrapper) ? ItemVisibility.HIDDEN : ItemVisibility.AUTO;
                    }
                };
        add(panel);
    }

    private boolean isHiddenByConfiguration(ItemWrapper<?, ?> itemWrapper) {
        ContainerPanelConfigurationType config = getPanelConfiguration();
        if (config == null) {
            return false;
        }

        for (VirtualContainersSpecificationType container : config.getContainer()) {
            if (container.getPath() != null
                    && itemWrapper.getPath().equivalent(container.getPath().getItemPath())
                    && !WebComponentUtil.getElementVisibility(container.getVisibility())) {
                return true;
            }
        }
        return false;
    }
}
