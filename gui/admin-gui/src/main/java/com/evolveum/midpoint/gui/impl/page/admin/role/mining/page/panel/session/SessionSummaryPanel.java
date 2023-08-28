/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SummaryPanelSpecificationType;

import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;

public class SessionSummaryPanel extends FocusSummaryPanel<RoleAnalysisSessionType> {
    private static final long serialVersionUID = 8087858942603720878L;

    public SessionSummaryPanel(String id, IModel<RoleAnalysisSessionType> model, SummaryPanelSpecificationType summaryPanelSpecificationType) {
        super(id, RoleAnalysisSessionType.class, model, summaryPanelSpecificationType);
    }

    @Override
    protected QName getDisplayNamePropertyName() {
        return RoleAnalysisSessionType.F_NAME;
    }

    @Override
    protected QName getTitlePropertyName() {
        return RoleAnalysisSessionType.F_SUBTYPE;
    }

    @Override
    protected String getDefaultIconCssClass() {
        return GuiStyleConstants.EVO_CASE_OBJECT_ICON;
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
