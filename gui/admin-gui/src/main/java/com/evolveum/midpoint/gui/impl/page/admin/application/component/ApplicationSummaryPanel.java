/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.application.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApplicationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SummaryPanelSpecificationType;

import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;

/**
 * @author lskublik
 */
public class ApplicationSummaryPanel extends FocusSummaryPanel<ApplicationType> {
    private static final long serialVersionUID = 7293847201983475102L;

    public ApplicationSummaryPanel(String id, IModel<ApplicationType> model, SummaryPanelSpecificationType summaryPanelSpecificationType) {
        super(id, ApplicationType.class, model, summaryPanelSpecificationType);
    }

    @Override
    protected QName getDisplayNamePropertyName() {
        return ApplicationType.F_DISPLAY_NAME;
    }

    @Override
    protected QName getTitlePropertyName() {
        return ApplicationType.F_IDENTIFIER;
    }

    @Override
    protected String getDefaultIconCssClass() {
        return GuiStyleConstants.CLASS_OBJECT_APPLICATION_ICON;
    }

    @Override
    protected String getIconBoxAdditionalCssClass() {
        return "summary-panel-application";
    }

    @Override
    protected String getBoxAdditionalCssClass() {
        return "summary-panel-application";
    }

}
