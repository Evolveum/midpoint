/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.component;

import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemEditabilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemMandatoryHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.ButtonBar;
import com.evolveum.midpoint.gui.impl.component.table.WidgetTableHeader;
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
                "adminGuiConfiguration/configurableUserDashboard",
                "adminGuiConfiguration/accessRequest",
                "adminGuiConfiguration/homePage"
        }
)
@PanelInstance(
        identifier = "accessRequestPanel",
        applicableForType = AdminGuiConfigurationType.class,
        display = @PanelDisplay(
                label = "AdminGuiConfigurationType.accessRequest",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 70
        ),
        containerPath = "adminGuiConfiguration/accessRequest",
        type = "AccessRequestType",
        expanded = true
)
@PanelInstance(
        identifier = "homePagePanel",
        applicableForType = AdminGuiConfigurationType.class,
        display = @PanelDisplay(
                label = "AdminGuiConfigurationType.homePage",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 70
        ),
        containerPath = "adminGuiConfiguration/homePage",
        type = "HomePageType",
        expanded = true
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
@PanelInstance(
        identifier = "eventMarkInformationPanel",
        applicableForType = MarkType.class,
        display = @PanelDisplay(
                label = "MarkType.eventMark",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 30
        ),
        containerPath = "eventMark",
        type = "EventMarkInformationType",
        expanded = true
)
@PanelInstance(
        identifier = "objectOperationPolicyPanel",
        applicableForType = MarkType.class,
        display = @PanelDisplay(
                label = "MarkType.objectOperationPolicy",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 30
        ),
        containerPath = "objectOperationPolicy",
        type = "ObjectOperationPolicyType",
        expanded = true
)
@PanelInstance(
        identifier = "shadowBehaviorPanel",
        applicableForType = ShadowType.class,
        display = @PanelDisplay(
                label = "ShadowType.shadowBehavior",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 30
        ),
        containerPath = "behavior",
        type = "ShadowBehaviorType",
        expanded = true
)
public class GenericSingleContainerPanel<C extends Containerable, O extends ObjectType> extends AbstractObjectMainPanel<O, ObjectDetailsModels<O>> {

    private static final String ID_DETAILS = "details";
    private static final String ID_CONTAINER_DETAILS = "containerDetails";
    private static final String ID_FOOTER = "footer";
    private static final String ID_PREVIEW_DETAILS = "previewDetails";
    private static final String ID_HEADER = "header";
    private static final String ID_BUTTON_BAR = "buttonBar";
    private static final String ID_SINGLE_CONTAINER_DETAILS = "singleContainerDetails";

    public GenericSingleContainerPanel(String id, ObjectDetailsModels<O> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        ContainerPanelConfigurationType panelConfig = getPanelConfiguration();

        if (panelConfig instanceof PreviewContainerPanelConfigurationType) {
            add(createPreviewDetailsFragment());
            return;
        }

        Fragment previewFragment = new Fragment(ID_DETAILS, ID_SINGLE_CONTAINER_DETAILS, this);
        previewFragment.add(createSingleContainerPanel());
        add(previewFragment);
    }

    private Fragment createPreviewDetailsFragment() {
        Fragment previewFragment = new Fragment(ID_DETAILS, ID_PREVIEW_DETAILS, this);
        previewFragment.add(new WidgetTableHeader(ID_HEADER, new PropertyModel<>(getPanelConfiguration(), PreviewContainerPanelConfigurationType.F_DISPLAY.getLocalPart())));
        previewFragment.add(createSingleContainerPanel());
        previewFragment.add(new ButtonBar(ID_FOOTER, ID_BUTTON_BAR, GenericSingleContainerPanel.this,
                (PreviewContainerPanelConfigurationType) getPanelConfiguration(), null));
        return previewFragment;
    }

    private SingleContainerPanel createSingleContainerPanel() {
        return new SingleContainerPanel<C>(ID_CONTAINER_DETAILS, (IModel) getObjectWrapperModel(), getPanelConfiguration()) {
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

            @Override
            protected ItemEditabilityHandler getEditabilityHandler() {
                ContainerPanelConfigurationType config = getPanelConfiguration();

                if (!(config instanceof PreviewContainerPanelConfigurationType)) {
                    return super.getEditabilityHandler();
                }

                return wrapper -> false;
            }

            @Override
            protected ItemMandatoryHandler getMandatoryHandler() {
                return createMandatoryHandler();
            }
        };
    }

    protected ItemMandatoryHandler createMandatoryHandler() {
        return null;
    }
}
