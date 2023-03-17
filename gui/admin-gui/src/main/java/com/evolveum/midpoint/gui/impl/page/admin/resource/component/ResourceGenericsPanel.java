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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

@PanelType(name = "resourceGenerics")
@PanelInstance(
        identifier = "resourceGenerics",
        applicableForOperation = OperationTypeType.MODIFY,
        applicableForType = ResourceType.class,
        display =
        @PanelDisplay(
                label = "PageResource.tab.content.generic",
                icon = GuiStyleConstants.CLASS_SHADOW_ICON_GENERIC,
                order = 70))
public class ResourceGenericsPanel extends ResourceContentPanel {

    public ResourceGenericsPanel(String id, ResourceDetailsModel model, ContainerPanelConfigurationType config) {
        super(id, ShadowKindType.GENERIC, model, config);
    }
}
