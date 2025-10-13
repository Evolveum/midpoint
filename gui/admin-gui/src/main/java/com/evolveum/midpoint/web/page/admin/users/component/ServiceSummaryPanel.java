/*
 * Copyright (c) 2016-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.admin.users.component;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SummaryPanelSpecificationType;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;

/**
 * @author semancik
 * @author katkav
 */
public class ServiceSummaryPanel extends FocusSummaryPanel<ServiceType> {
    private static final long serialVersionUID = -5457446213855746564L;

    public ServiceSummaryPanel(String id, IModel<ServiceType> model, SummaryPanelSpecificationType summaryPanelSpecificationType) {
        super(id, ServiceType.class, model, summaryPanelSpecificationType);
    }

    @Override
    protected QName getDisplayNamePropertyName() {
        return ServiceType.F_DISPLAY_NAME;
    }

    @Override
    protected QName getTitlePropertyName() {
        return ServiceType.F_IDENTIFIER;
    }

    @Override
    protected String getDefaultIconCssClass() {
        return GuiStyleConstants.CLASS_OBJECT_SERVICE_ICON;
    }

    @Override
    protected String getIconBoxAdditionalCssClass() {
        return "summary-panel-service";
    }

    @Override
    protected String getBoxAdditionalCssClass() {
        return "summary-panel-service";
    }

}
