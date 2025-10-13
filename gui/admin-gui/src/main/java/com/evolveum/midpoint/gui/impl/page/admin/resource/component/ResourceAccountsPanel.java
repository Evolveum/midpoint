/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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

    @Override
    protected UserProfileStorage.TableId getRepositorySearchTableId() {
        return UserProfileStorage.TableId.PAGE_RESOURCE_ACCOUNTS_PANEL_REPOSITORY_MODE;
    }
}
