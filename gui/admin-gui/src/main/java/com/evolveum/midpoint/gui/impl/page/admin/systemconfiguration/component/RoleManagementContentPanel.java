/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleManagementConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

import javax.xml.namespace.QName;

@PanelType(name = "roleManagementPanel")
@PanelInstance(
        identifier = "roleManagementPanel",
        applicableForType = RoleManagementConfigurationType.class,
        display = @PanelDisplay(
                label = "RoleManagementContentPanel.label",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 10
        )
)
public class RoleManagementContentPanel extends AbstractObjectMainPanel<SystemConfigurationType, AssignmentHolderDetailsModel<SystemConfigurationType>> {

    private static final String ID_MAIN_PANEL = "mainPanel";

    public RoleManagementContentPanel(String id, AssignmentHolderDetailsModel<SystemConfigurationType> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        SingleContainerPanel panel = new SingleContainerPanel(ID_MAIN_PANEL,
                PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), ItemPath.create(SystemConfigurationType.F_ROLE_MANAGEMENT)),
                RoleManagementConfigurationType.COMPLEX_TYPE) {

            @Override
            protected ItemVisibility getVisibility(ItemWrapper itemWrapper) {
                ItemPath path = itemWrapper.getPath();
                if (path == null || path.isEmpty()) {
                    return ItemVisibility.AUTO;
                }

                QName name;
                if (path.size() == 1) {
                    name = path.firstToQName();
                } else {
                    name = path.rest().firstToQName();
                }

                boolean hide = RoleManagementConfigurationType.F_RELATIONS.equals(name);

                return hide ? ItemVisibility.HIDDEN : ItemVisibility.AUTO;
            }
        };
        add(panel);
    }
}
