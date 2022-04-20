/*
 * Copyright (c) 2015-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.org.component;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SummaryPanelSpecificationType;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

/**
 * @author semancik
 *
 */
public class OrgSummaryPanel extends FocusSummaryPanel<OrgType> {
    private static final long serialVersionUID = -5457446213855746564L;

    public OrgSummaryPanel(String id, IModel<OrgType> model, SummaryPanelSpecificationType summaryPanelSpecificationType) {
        super(id, OrgType.class, model, summaryPanelSpecificationType);
    }

    @Override
    protected QName getDisplayNamePropertyName() {
        return OrgType.F_DISPLAY_NAME;
    }

    @Override
    protected QName getTitlePropertyName() {
        return OrgType.F_IDENTIFIER;
    }

    @Override
    protected String getDefaultIconCssClass() {
        return GuiStyleConstants.CLASS_OBJECT_ORG_ICON;
    }

    @Override
    protected String getIconBoxAdditionalCssClass() {
        return "summary-panel-org";
    }

    @Override
    protected String getBoxAdditionalCssClass() {
        return "summary-panel-org";
    }

}
