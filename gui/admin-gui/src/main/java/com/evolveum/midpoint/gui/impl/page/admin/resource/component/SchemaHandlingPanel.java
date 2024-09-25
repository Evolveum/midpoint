/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.markup.html.WebMarkupContainer;

@PanelInstance(identifier = "schemaHandling", applicableForType = ResourceType.class,
        display = @PanelDisplay(label = "PageResource.tab.schemaHandling", icon = "fa fa-gears", order = 90))
public class SchemaHandlingPanel<AH extends AssignmentHolderType> extends AbstractObjectMainPanel<AH, ObjectDetailsModels<AH>> {

    private static final String ID_SCHEMA_HANDLING = "schemaHandlingContainer";

    public SchemaHandlingPanel(String id, ObjectDetailsModels<AH> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        WebMarkupContainer assignments = new WebMarkupContainer(ID_SCHEMA_HANDLING);
        assignments.setOutputMarkupId(true);
        add(assignments);
    }

    protected boolean isReadonly(){
        return false;
    }
}
