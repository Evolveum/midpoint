/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session;

import java.io.Serial;
import javax.xml.namespace.QName;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SummaryPanelSpecificationType;

public class SessionSummaryPanel extends FocusSummaryPanel<RoleAnalysisSessionType> {
    @Serial private static final long serialVersionUID = 8087858942603720878L;

    public SessionSummaryPanel(String id, IModel<RoleAnalysisSessionType> model, SummaryPanelSpecificationType summaryPanelSpecificationType) {
        super(id, RoleAnalysisSessionType.class, model, summaryPanelSpecificationType);
    }

    @Override
    protected QName getTitlePropertyName() {
        return RoleAnalysisSessionType.F_SUBTYPE;
    }

    @Override
    protected String getDefaultIconCssClass() {
        return IconAndStylesUtil.createDefaultIcon(getModelObject().asPrismObject());
    }

    @Override
    protected String getIconBoxAdditionalCssClass() {
        return "summary-panel-role";
    }

    @Override
    protected String getBoxAdditionalCssClass() {
        return "summary-panel-role";
    }

    @Override
    protected boolean isActivationVisible() {
        return false;
    }
}
