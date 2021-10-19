/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.configuration.component;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SummaryPanelSpecificationType;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

public class SystemConfigurationSummaryPanel extends ObjectSummaryPanel<SystemConfigurationType> {

    private static final long serialVersionUID = 1L;

    public SystemConfigurationSummaryPanel(String id, IModel<SystemConfigurationType> model, SummaryPanelSpecificationType summaryPanelSpecificationType) {
        super(id, SystemConfigurationType.class, model, summaryPanelSpecificationType);
    }

    @Override
    protected String getDefaultIconCssClass() {
        return GuiStyleConstants.CLASS_SYSTEM_CONFIGURATION_ICON;
    }

    @Override
    protected String getIconBoxAdditionalCssClass() {
        return null;
    }

    @Override
    protected String getBoxAdditionalCssClass() {
        return null;
    }

    @Override
    protected boolean isIdentifierVisible() {
        return false;
    }
}
