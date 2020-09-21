/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.valuePolicy.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;
import org.apache.wicket.model.IModel;

/**
 * Created by matus on 9/12/2017.
 */
public class ValuePolicySummaryPanel extends ObjectSummaryPanel<ValuePolicyType> {

    public ValuePolicySummaryPanel(String id, IModel<ValuePolicyType> model, ModelServiceLocator serviceLocator) {
        super(id, ValuePolicyType.class, model, serviceLocator);
    }

    @Override
    protected String getDefaultIconCssClass() { return GuiStyleConstants.CLASS_VALUE_POLICY_ICON; }

    @Override
    protected String getIconBoxAdditionalCssClass() { return "summary-panel-value-policy"; }

    @Override
    protected String getBoxAdditionalCssClass() { return "summary-panel-value-policy"; }

    @Override
    protected boolean isIdentifierVisible() {
        return false;
    }
}
