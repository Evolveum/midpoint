/*
 * Copyright (c) 2016-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.policy.component;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyType;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SummaryPanelSpecificationType;

/**
 * @author semancik
 * @author katkav
 */
public class PolicySummaryPanel extends FocusSummaryPanel<PolicyType> {
    private static final long serialVersionUID = -5457446213855746564L;

    public PolicySummaryPanel(String id, IModel<PolicyType> model, SummaryPanelSpecificationType summaryPanelSpecificationType) {
        super(id, PolicyType.class, model, summaryPanelSpecificationType);
    }

    @Override
    protected QName getDisplayNamePropertyName() {
        return PolicyType.F_DISPLAY_NAME;
    }

    @Override
    protected QName getTitlePropertyName() {
        return PolicyType.F_IDENTIFIER;
    }

    @Override
    protected String getDefaultIconCssClass() {
        return GuiStyleConstants.CLASS_OBJECT_POLICY_ICON;
    }

    @Override
    protected String getIconBoxAdditionalCssClass() {
        return "summary-panel-policy";
    }

    @Override
    protected String getBoxAdditionalCssClass() {
        return "summary-panel-policy";
    }

}
