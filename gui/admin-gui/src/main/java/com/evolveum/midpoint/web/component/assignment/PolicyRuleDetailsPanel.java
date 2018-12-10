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

import com.evolveum.midpoint.web.component.prism.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.component.form.Form;
import org.apache.wicket.model.Model;

/**
 * Created by honchar.
 */
public class PolicyRuleDetailsPanel<F extends FocusType> extends AbstractAssignmentDetailsPanel<F> {
    private static final long serialVersionUID = 1L;
    
    public PolicyRuleDetailsPanel(String id, Form<?> form, IModel<ContainerValueWrapper<AssignmentType>> model){
        super(id, form, model);
    }

	@Override
	protected IModel<ContainerWrapper> getSpecificContainerModel() {
		ContainerWrapper<PolicyRuleType> policyRuleWrapper = getModelObject().findContainerWrapper(getModelObject().getPath().append(AssignmentType.F_POLICY_RULE));
		policyRuleWrapper.setShowEmpty(true, true);
		return Model.of(policyRuleWrapper);
	}

}
