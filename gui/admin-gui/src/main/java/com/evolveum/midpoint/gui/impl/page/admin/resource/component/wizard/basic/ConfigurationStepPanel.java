/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic;

import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * @author lskublik
 */
@PanelType(name = "rw-connectorConfiguration")
@PanelInstance(identifier = "rw-connectorConfiguration",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(
                label = "PageResource.wizard.step.configuration",
                icon = "fa fa-cog"),
        containerPath = "connectorConfiguration/configurationProperties",
        expanded = true)
public class ConfigurationStepPanel extends AbstractConfigurationStepPanel {

    private static final Trace LOGGER = TraceManager.getTrace(ConfigurationStepPanel.class);

    private static final String PANEL_TYPE = "rw-connectorConfiguration";

    public ConfigurationStepPanel(ResourceDetailsModel model) {
        super(model);
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }
}
