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

import com.evolveum.midpoint.web.component.prism.*;
import com.evolveum.midpoint.web.model.ContainerWrapperFromObjectWrapperModel;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.form.Form;

/**
 * Created by honchar.
 */
public class PolicyRuleDetailsPanel<F extends FocusType> extends AbstractAssignmentDetailsPanel<F> {
    private static final long serialVersionUID = 1L;
    
    public PolicyRuleDetailsPanel(String id, Form<?> form, IModel<ContainerValueWrapper<AssignmentType>> model){
        super(id, form, model);
    }

	@Override
    protected void initContainersPanel(Form form, PageAdminObjectDetails<F> pageBase){
		ContainerWrapperFromObjectWrapperModel<PolicyRuleType, F> policyRuleModel =
				new ContainerWrapperFromObjectWrapperModel<PolicyRuleType, F>(pageBase.getObjectModel(),
						getModelObject().getPath().append(AssignmentType.F_POLICY_RULE));

		ContainerWrapper<PolicyRuleType> policyRules = policyRuleModel.getObject();
		if (policyRules.getValues() != null){
			policyRules.getValues().forEach(policyRuleContainerValueWrapper -> {
				policyRuleContainerValueWrapper.setShowEmpty(true, false);
			});
		}
		policyRules.setShowEmpty(true, false);
		setRemoveContainerButtonVisibility(policyRules);
		setAddContainerButtonVisibility(policyRules);

		PrismContainerPanel<PolicyRuleType> constraintsContainerPanel = new PrismContainerPanel(ID_SPECIFIC_CONTAINERS, policyRuleModel,
				false, form, null, pageBase);
		constraintsContainerPanel.setOutputMarkupId(true);
		add(constraintsContainerPanel);
	}

	private void setRemoveContainerButtonVisibility(ContainerWrapper<PolicyRuleType> policyRulesContainer){
		ContainerWrapper constraintsContainer = policyRulesContainer.findContainerWrapper(new ItemPath(policyRulesContainer.getPath(), PolicyRuleType.F_POLICY_CONSTRAINTS));
		if (constraintsContainer != null){
			constraintsContainer.getValues().forEach(value ->
					((ContainerValueWrapper)value).getItems().forEach(
							constraintContainerItem -> {
								if (constraintContainerItem instanceof ContainerWrapper && ((ContainerWrapper) constraintContainerItem).getItemDefinition().isMultiValue()){
									((ContainerWrapper) constraintContainerItem).setRemoveContainerButtonVisible(true);
								}
							}
					));
		}
	}

	private void setAddContainerButtonVisibility(ContainerWrapper<PolicyRuleType> policyRulesContainer){
		ContainerWrapper constraintsContainer = policyRulesContainer.findContainerWrapper(new ItemPath(policyRulesContainer.getPath(), PolicyRuleType.F_POLICY_CONSTRAINTS));
		constraintsContainer.setShowEmpty(true, false);
		constraintsContainer.setAddContainerButtonVisible(true);
	}

	@Override
	protected List<ItemPath> collectContainersToShow() {
		List<ItemPath> containersToShow = new ArrayList<>();
		containersToShow.add(getAssignmentPath().append(AssignmentType.F_POLICY_RULE));
		
		return containersToShow;
	}

}
