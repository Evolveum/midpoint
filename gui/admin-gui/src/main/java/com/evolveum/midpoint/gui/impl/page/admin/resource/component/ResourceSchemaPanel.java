/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.page.admin.resources.component.SchemaListPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

@PanelType(name = "resourceSchema")
@PanelInstance(identifier = "resourceSchema", applicableForType = ResourceType.class,
        display = @PanelDisplay(label = "PageResource.tab.resourceSchema", icon = GuiStyleConstants.CLASS_ICON_RESOURCE_SCHEMA, order = 110))
public class ResourceSchemaPanel extends AbstractObjectMainPanel<ResourceType, ResourceDetailsModel> {

    private static final String ID_VIEW = "viewPanel";
    public ResourceSchemaPanel(String id, ResourceDetailsModel model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    protected void initLayout() {
        setOutputMarkupId(true);

        SchemaListPanel view = new SchemaListPanel(ID_VIEW, getObjectWrapperModel(), getPageBase());
        view.setOutputMarkupId(true);
        add(view);
    }
}
