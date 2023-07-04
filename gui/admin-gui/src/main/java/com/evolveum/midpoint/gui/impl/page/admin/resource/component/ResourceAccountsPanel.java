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
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.StringResourceModel;

@PanelType(name = "resourceAccounts")
@PanelInstance(
        identifier = "resourceAccounts",
        applicableForOperation = OperationTypeType.MODIFY,
        applicableForType = ResourceType.class,
        display =
        @PanelDisplay(
                label = "PageResource.tab.content.account",
                icon = GuiStyleConstants.CLASS_SHADOW_ICON_ACCOUNT,
                order = 50
        )
)
public class ResourceAccountsPanel extends ResourceObjectsPanel {


    public ResourceAccountsPanel(String id, ResourceDetailsModel model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected ShadowKindType getKind() {
        return ShadowKindType.ACCOUNT;
    }

    @Override
    protected StringResourceModel getLabelModel() {
        return getPageBase().createStringResource("PageResource.tab.content.account");
    }
}
