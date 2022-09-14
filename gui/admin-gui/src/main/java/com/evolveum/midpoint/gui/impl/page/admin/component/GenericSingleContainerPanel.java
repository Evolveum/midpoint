/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.component;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "formPanel")
@PanelInstance(
        identifier = "infrastructurePanel",
        applicableForType = SystemConfigurationType.class,
        display = @PanelDisplay(
                label = "InfrastructureContentPanel.label",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 30
        ),
        containerPath = "infrastructure",
        type = "InfrastructureConfigurationType",
        expanded = true
)
@PanelInstance(
        identifier = "fullTextSearchPanel",
        applicableForType = SystemConfigurationType.class,
        display = @PanelDisplay(
                label = "FullTextSearchPanel.label",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 40
        ),
        containerPath = "fullTextSearch",
        type = "FullTextSearchConfigurationType",
        expanded = true
)
@PanelInstance(
        identifier = "adminGuiPanel",
        applicableForType = AdminGuiConfigurationType.class,
        display = @PanelDisplay(
                label = "AdminGuiConfiguration.label",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 10
        ),
        containerPath = "adminGuiConfiguration",
        type = "AdminGuiConfigurationType",
        expanded = true,
        hiddenContainers = {
                "adminGuiConfiguration/additionalMenuLink",
                "adminGuiConfiguration/userDashboardLink",
                "adminGuiConfiguration/objectCollectionViews",
                "adminGuiConfiguration/objectDetails",
                "adminGuiConfiguration/configurableUserDashboard"
        }
)
@PanelInstance(
        identifier = "wfConfigurationPanel",
        applicableForType = WfConfigurationType.class,
        display = @PanelDisplay(
                label = "WfConfiguration.label",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 10
        ),
        containerPath = "workflowConfiguration",
        type = "WfConfigurationType",
        expanded = true
)
@PanelInstance(
        identifier = "projectionPolicyPanel",
        applicableForType = ProjectionPolicyType.class,
        display = @PanelDisplay(
                label = "ProjectionPolicy.label",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 30
        ),
        containerPath = "globalAccountSynchronizationSettings",
        type = "ProjectionPolicyType",
        expanded = true
)
@PanelInstance(
        identifier = "cleanupPolicyPanel",
        applicableForType = CleanupPoliciesType.class,
        display = @PanelDisplay(
                label = "CleanupPolicies.label",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 40
        ),
        containerPath = "cleanupPolicy",
        type = "CleanupPoliciesType",
        expanded = true
)
@PanelInstance(
        identifier = "accessCertificationPanel",
        applicableForType = AccessCertificationConfigurationType.class,
        display = @PanelDisplay(
                label = "AccessCertificationContentPanel.label",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 10
        ),
        containerPath = "accessCertification",
        type = "AccessCertificationConfigurationType",
        expanded = true
)
@PanelInstance(
        identifier = "deploymentPanel",
        applicableForType = SystemConfigurationType.class,
        display = @PanelDisplay(
                label = "DeploymentContentPanel.label",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 20
        ),
        containerPath = "deploymentInformation",
        type = "DeploymentInformationType",
        expanded = true
)
@PanelInstance(
        identifier = "internalsPanel",
        applicableForType = InternalsConfigurationType.class,
        display = @PanelDisplay(
                label = "InternalsConfigurationPanel.label",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 10
        ),
        containerPath = "internals",
        type = "InternalsConfigurationType",
        expanded = true
)
@PanelInstance(
        identifier = "roleManagementPanel",
        applicableForType = RoleManagementConfigurationType.class,
        display = @PanelDisplay(
                label = "RoleManagementContentPanel.label",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 10
        ),
        containerPath = "roleManagement",
        type = "RoleManagementConfigurationType",
        hiddenContainers = {
                "roleManagement/relations"
        },
        expanded = true
)
public class GenericSingleContainerPanel<C extends Containerable, O extends ObjectType> extends AbstractObjectMainPanel<O, ObjectDetailsModels<O>> {

    private static final String ID_DETAILS = "details";

    public GenericSingleContainerPanel(String id, ObjectDetailsModels<O> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        add(new SingleContainerPanel<C>(ID_DETAILS, (IModel) getObjectWrapperModel(), getPanelConfiguration()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected ItemVisibility getVisibility(ItemWrapper itemWrapper) {
                ContainerPanelConfigurationType config = getPanelConfiguration();
                if (config == null || config.getContainer() == null) {
                    return ItemVisibility.AUTO;
                }
                for (VirtualContainersSpecificationType container : config.getContainer()) {
                    if (container.getPath() != null && itemWrapper.getPath().equivalent(container.getPath().getItemPath()) &&
                            !WebComponentUtil.getElementVisibility(container.getVisibility())) {
                        return ItemVisibility.HIDDEN;
                    }
                }
                return ItemVisibility.AUTO;
            }
        });
    }
}
