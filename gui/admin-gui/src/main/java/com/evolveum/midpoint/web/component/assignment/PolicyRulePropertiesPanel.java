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

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * Created by honchar
 * Panel displays specific for policy rule object
 * properties. A part of the PolicyRuleDetailsPage
 * */
public class PolicyRulePropertiesPanel extends BasePanel<AssignmentEditorDto>{

    private static final String ID_CONSTRAINTS_VALUE = "constraintsValue";
    private static final String ID_SITUATION_VALUE = "situationValue";
    private static final String ID_ACTION_VALUE = "actionValue";

    private PageBase pageBase;

    public PolicyRulePropertiesPanel(String id, IModel<AssignmentEditorDto> policyRuleModel, PageBase pageBase){
        super(id, policyRuleModel);
        this.pageBase = pageBase;
        initLayout();
    }

    private void initLayout(){
        AssignmentEditorDto policyRuleDto = getModelObject();

        PrismContainer<PolicyRuleType> policyRuleContainer = policyRuleDto.getPolicyRuleContainer(null);
        add(new Label(ID_CONSTRAINTS_VALUE, Model.of(PolicyRuleUtil.convertPolicyConstraintsContainerToString(policyRuleContainer, pageBase))));

        add(new Label(ID_SITUATION_VALUE, Model.of(policyRuleContainer == null ? "" : policyRuleContainer.getValue().getValue().getPolicySituation())));

        add(new Label(ID_ACTION_VALUE, Model.of(PolicyRuleUtil.convertPolicyActionsContainerToString(policyRuleContainer))));

    }


}
