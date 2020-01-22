/*
 * Copyright (c) 2015-2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.roles.component;

import javax.xml.namespace.QName;

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

    public RoleSummaryPanel(String id, IModel<RoleType> model, ModelServiceLocator serviceLocator) {
        super(id, RoleType.class, model, serviceLocator);
    }

    @Override
    protected QName getDisplayNamePropertyName() {
        return RoleType.F_NAME;            // TODO F_DISPLAY_NAME ?
    }

    @Override
    protected QName getTitlePropertyName() {
        return RoleType.F_ROLE_TYPE;        // TODO what about subtype? [MID-4818]
    }

    @Override
    protected String getIconCssClass() {
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
