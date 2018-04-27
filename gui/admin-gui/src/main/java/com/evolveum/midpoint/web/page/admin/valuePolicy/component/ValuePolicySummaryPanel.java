/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    public ValuePolicySummaryPanel(String id, IModel<PrismObject<ValuePolicyType>> model, ModelServiceLocator serviceLocator) {
        super(id, ValuePolicyType.class, model, serviceLocator);
        initLayoutCommon(serviceLocator);
    }

    @Override
    protected String getIconCssClass() { return GuiStyleConstants.CLASS_VALUE_POLICY_ICON; }

    @Override
    protected String getIconBoxAdditionalCssClass() { return "summary-panel-value-policy"; }

    @Override
    protected String getBoxAdditionalCssClass() { return "summary-panel-value-policy"; }

    @Override
    protected boolean isIdentifierVisible() {
        return false;
    }
}
