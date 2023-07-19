/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.StringResourceModel;

@PanelType(name = "resourceEntitlements")
@PanelInstance(
        identifier = "resourceEntitlements",
        applicableForOperation = OperationTypeType.MODIFY,
        applicableForType = ResourceType.class,
        display =
        @PanelDisplay(
                label = "PageResource.tab.content.entitlement",
                icon = GuiStyleConstants.CLASS_SHADOW_ICON_ENTITLEMENT,
                order = 60
        )
)
public class ResourceEntitlementsPanel extends ResourceObjectsPanel {

    public ResourceEntitlementsPanel(String id, ResourceDetailsModel model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected UserProfileStorage.TableId getRepositorySearchTableId() {
        return UserProfileStorage.TableId.PAGE_RESOURCE_ENTITLEMENT_PANEL_REPOSITORY_MODE;
    }

    @Override
    protected StringResourceModel getLabelModel() {
        return getPageBase().createStringResource("PageResource.tab.content.entitlement");
    }

    @Override
    protected ShadowKindType getKind() {
        return ShadowKindType.ENTITLEMENT;
    }
}
