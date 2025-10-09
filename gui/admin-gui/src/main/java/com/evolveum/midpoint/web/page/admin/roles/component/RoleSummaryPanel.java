/*
 * Copyright (c) 2015-2016 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.admin.roles.component;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SummaryPanelSpecificationType;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

/**
 * @author semancik
 */
public class RoleSummaryPanel extends FocusSummaryPanel<RoleType> {
    private static final long serialVersionUID = 8087858942603720878L;

    public RoleSummaryPanel(String id, IModel<RoleType> model, SummaryPanelSpecificationType summaryPanelSpecificationType) {
        super(id, RoleType.class, model, summaryPanelSpecificationType);
    }

    @Override
    protected QName getDisplayNamePropertyName() {
        return RoleType.F_DISPLAY_NAME;
    }

    @Override
    protected QName getTitlePropertyName() {
        return RoleType.F_IDENTIFIER;
    }

    @Override
    protected String getDefaultIconCssClass() {
        return GuiStyleConstants.CLASS_OBJECT_ROLE_ICON;
    }

    @Override
    protected String getIconBoxAdditionalCssClass() {
        return "summary-panel-role";
    }

    @Override
    protected String getBoxAdditionalCssClass() {
        return "summary-panel-role";
    }

}
