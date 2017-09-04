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
package com.evolveum.midpoint.web.component.assignment;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

/**
 * Created by honchar.
 */
public class PolicyRuleDetailsPanel extends AbstractAssignmentDetailsPanel {
    private static final long serialVersionUID = 1L;

    private static final String ID_POLICY_RULE = "policyRule";

    private static List hiddenItems = new ArrayList<>();

    static  {
			hiddenItems.add(AssignmentType.F_TENANT_REF);
			hiddenItems.add(AssignmentType.F_ORG_REF);
			hiddenItems.add(AssignmentType.F_FOCUS_TYPE);
			hiddenItems.add(ID_PROPERTIES_PANEL);
	};

    public PolicyRuleDetailsPanel(String id, Form<?> form, IModel<AssignmentDto> model, PageBase pageBase){
        super(id, form, model, pageBase);
    }

   @Override
protected List getHiddenItems() {
	return hiddenItems;
}

   @Override
protected void initPropertiesPanel(WebMarkupContainer propertiesPanel) {
	   PolicyRulePropertiesPanel policyDetails = new PolicyRulePropertiesPanel(ID_POLICY_RULE, getModel(), pageBase);
		policyDetails.setOutputMarkupId(true);
		policyDetails.add(new VisibleEnableBehaviour() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return PolicyRuleDetailsPanel.this.isVisible(AssignmentType.F_POLICY_RULE);
			}
		});
		propertiesPanel.add(policyDetails);
}

}
