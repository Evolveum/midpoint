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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import org.apache.wicket.model.StringResourceModel;

@PanelType(name = "resourceWorks")
@PanelInstance(
        identifier = "resourceWorks",
        applicableForOperation = OperationTypeType.MODIFY,
        applicableForType = ResourceType.class,
        display =
        @PanelDisplay(
                label = "PageResource.tab.content.work",
                icon = GuiStyleConstants.CLASS_SHADOW_ICON_WORK,
                order = 80)
)
public class ResourceWorksPanel extends ResourceObjectsPanel {

    public static final String ID = "resourceWorks";

    public ResourceWorksPanel(String id, ResourceDetailsModel model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected UserProfileStorage.TableId getRepositorySearchTableId() {
        return UserProfileStorage.TableId.PAGE_RESOURCE_WORKS_PANEL_REPOSITORY_MODE;
    }

    @Override
    protected StringResourceModel getLabelModel() {
        return createStringResource("PageResource.tab.content.work");
    }

    @Override
    protected ShadowKindType getKind() {
        return ShadowKindType.WORK;
    }
}
