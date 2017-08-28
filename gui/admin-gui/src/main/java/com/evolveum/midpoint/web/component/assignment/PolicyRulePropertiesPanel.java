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
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueExpandablePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExclusionPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar
 * Panel displays specific for policy rule object
 * properties. A part of the PolicyRuleDetailsPage
 * */
public class PolicyRulePropertiesPanel extends BasePanel<AssignmentDto>{

    private static final String ID_CONSTRAINTS_VALUE = "constraintsValue";
    private static final String ID_SITUATION_VALUE = "situationValue";
    private static final String ID_ACTION_VALUE = "actionValue";

    private PageBase pageBase;

    public PolicyRulePropertiesPanel(String id, IModel<AssignmentDto> policyRuleModel, PageBase pageBase){
        super(id, policyRuleModel);
        this.pageBase = pageBase;
        initLayout();
    }

    private void initLayout(){
        AssignmentDto policyRuleDto = getModelObject();

        PolicyRuleType policyRuleContainer = policyRuleDto.getAssignment().getPolicyRule();
        add(new MultiValueExpandablePanel<AbstractPolicyConstraintType>(ID_CONSTRAINTS_VALUE,
                Model.ofList(getAllPolicyConstraintsList(policyRuleContainer.getPolicyConstraints()))));

        add(new Label(ID_SITUATION_VALUE, Model.of(policyRuleContainer == null ? "" : policyRuleContainer.getPolicySituation())));

        add(new Label(ID_ACTION_VALUE, Model.of(PolicyRuleUtil.convertPolicyActionsContainerToString(policyRuleContainer))));

    }

    private List<AbstractPolicyConstraintType> getAllPolicyConstraintsList(PolicyConstraintsType constraintsType){
        List<AbstractPolicyConstraintType> constraintsList = new ArrayList<>();
        if (constraintsType == null){
            return constraintsList;
        }
        if (constraintsType.getExclusion() != null){
            constraintsList.addAll(constraintsType.getExclusion());
        }
        if (constraintsType.getMinAssignees() != null){
            constraintsList.addAll(constraintsType.getMinAssignees());
        }
        if (constraintsType.getMaxAssignees() != null){
            constraintsList.addAll(constraintsType.getMaxAssignees());
        }
        if (constraintsType.getModification() != null){
            constraintsList.addAll(constraintsType.getModification());
        }
        if (constraintsType.getAssignment() != null){
            constraintsList.addAll(constraintsType.getAssignment());
        }
        if (constraintsType.getTimeValidity() != null){
            constraintsList.addAll(constraintsType.getTimeValidity());
        }
        if (constraintsType.getSituation() != null){
            constraintsList.addAll(constraintsType.getSituation());
        }
        return constraintsList;
    }
}
