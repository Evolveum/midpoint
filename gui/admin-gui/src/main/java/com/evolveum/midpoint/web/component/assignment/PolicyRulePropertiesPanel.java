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
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueExpandablePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;

/**
 * Created by honchar
 * Panel displays specific for policy rule object
 * properties. A part of the PolicyRuleDetailsPage
 * */
public class PolicyRulePropertiesPanel extends BasePanel<AssignmentDto>{

    private static final String ID_CONSTRAINTS_CONTAINER = "constraintsContainer";
    private static final String ID_EXCLUSION_CONSTRAINTS = "exclusionConstraints";
    private static final String ID_MIN_ASSIGNEES_CONSTRAINTS = "minAssigneesConstraints";
    private static final String ID_MAX_ASSIGNEES_CONSTRAINTS = "maxAssigneesConstraints";
    private static final String ID_MODIFICATION_CONSTRAINTS = "modificationConstraints";
    private static final String ID_ASSIGNMENT_CONSTRAINTS = "assignmentConstraints";
    private static final String ID_TIME_VALIDITY_CONSTRAINTS = "timeValidityConstraints";
    private static final String ID_SITUATION_CONSTRAINTS = "situationConstraints";
    private static final String ID_SITUATION_VALUE = "situationValue";
    private static final String ID_ACTION_VALUE = "actionValue";
    private static final String ID_ADD_EXCLUSION = "addExclusionConstraints";
    private static final String ID_ADD_MIN_ASSIGNEES = "addMinAssigneesConstraints";
    private static final String ID_ADD_MAX_ASSIGNEES = "addMaxAssigneesConstraints";
    private static final String ID_ADD_MODIFICATION = "addModificationConstraints";
    private static final String ID_ADD_ASSIGNMENT = "addAssignmentConstraints";
    private static final String ID_ADD_TIME_VALIDITY = "addTimeValidityConstraints";
    private static final String ID_ADD_SITUATION = "addSituationConstraints";

    private PageBase pageBase;

    public PolicyRulePropertiesPanel(String id, IModel<AssignmentDto> policyRuleModel, PageBase pageBase){
        super(id, policyRuleModel);
        this.pageBase = pageBase;
        initLayout();
    }

    private void initLayout(){
        AssignmentDto policyRuleDto = getModelObject();

        WebMarkupContainer constraintsContainer = new WebMarkupContainer(ID_CONSTRAINTS_CONTAINER);
        constraintsContainer.setOutputMarkupId(true);
        add(constraintsContainer);

        initConstraintsContainer(constraintsContainer);

        PolicyRuleType policyRule = getPolicyRuleTypeObject();
        add(new Label(ID_SITUATION_VALUE, Model.of(policyRule == null ? "" : policyRule.getPolicySituation())));

        add(new Label(ID_ACTION_VALUE, Model.of(PolicyRuleUtil.convertPolicyActionsContainerToString(policyRule))));

        constraintsContainer.add(new AjaxButton(ID_ADD_EXCLUSION) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                addNewExclusionPerformed(ajaxRequestTarget);
            }
        });
        constraintsContainer.add(new AjaxButton(ID_ADD_MIN_ASSIGNEES) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                addNewMinAssigneesPerformed(ajaxRequestTarget);
            }
        });
        constraintsContainer.add(new AjaxButton(ID_ADD_MAX_ASSIGNEES) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                addNewMaxAssigneesPerformed(ajaxRequestTarget);
            }
        });
        constraintsContainer.add(new AjaxButton(ID_ADD_MODIFICATION) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                addNewModificationPerformed(ajaxRequestTarget);
            }
        });
        constraintsContainer.add(new AjaxButton(ID_ADD_ASSIGNMENT) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                addNewAssignmentPerformed(ajaxRequestTarget);
            }
        });
        constraintsContainer.add(new AjaxButton(ID_ADD_TIME_VALIDITY) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                addNewTimeValidityPerformed(ajaxRequestTarget);
            }
        });
        constraintsContainer.add(new AjaxButton(ID_ADD_SITUATION) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                addNewSituationPerformed(ajaxRequestTarget);
            }
        });

    }

    private void initConstraintsContainer(WebMarkupContainer constraintsContainer){
        PolicyConstraintsType policyConstraints = getPolicyConstraints();
        constraintsContainer.addOrReplace(new MultiValueExpandablePanel<ExclusionPolicyConstraintType>(ID_EXCLUSION_CONSTRAINTS,
                Model.ofList(policyConstraints == null || policyConstraints.getExclusion() == null ? new ArrayList<>() : policyConstraints.getExclusion()), pageBase));
        constraintsContainer.addOrReplace(new MultiValueExpandablePanel<MultiplicityPolicyConstraintType>(ID_MIN_ASSIGNEES_CONSTRAINTS,
                Model.ofList(policyConstraints == null || policyConstraints.getMinAssignees() == null ? new ArrayList<>() : policyConstraints.getMinAssignees()), pageBase));
        constraintsContainer.addOrReplace(new MultiValueExpandablePanel<MultiplicityPolicyConstraintType>(ID_MAX_ASSIGNEES_CONSTRAINTS,
                Model.ofList(policyConstraints == null || policyConstraints.getMaxAssignees() == null ? new ArrayList<>() : policyConstraints.getMaxAssignees()), pageBase));
        constraintsContainer.addOrReplace(new MultiValueExpandablePanel<ModificationPolicyConstraintType>(ID_MODIFICATION_CONSTRAINTS,
                Model.ofList(policyConstraints == null || policyConstraints.getModification() == null ? new ArrayList<>() : policyConstraints.getModification()), pageBase));
        constraintsContainer.addOrReplace(new MultiValueExpandablePanel<AssignmentPolicyConstraintType>(ID_ASSIGNMENT_CONSTRAINTS,
                Model.ofList(policyConstraints == null || policyConstraints.getAssignment() == null ? new ArrayList<>() : policyConstraints.getAssignment()), pageBase));
        constraintsContainer.addOrReplace(new MultiValueExpandablePanel<TimeValidityPolicyConstraintType>(ID_TIME_VALIDITY_CONSTRAINTS,
                Model.ofList(policyConstraints == null || policyConstraints.getTimeValidity() == null ? new ArrayList<>() : policyConstraints.getTimeValidity()), pageBase));
        constraintsContainer.addOrReplace(new MultiValueExpandablePanel<PolicySituationPolicyConstraintType>(ID_SITUATION_CONSTRAINTS,
                Model.ofList(policyConstraints == null || policyConstraints.getSituation() == null ? new ArrayList<>() : policyConstraints.getSituation()), pageBase));

    }

    private void addNewExclusionPerformed(AjaxRequestTarget target){
        if (getPolicyConstraints().getExclusion() == null){
            getPolicyConstraints().createExclusionList();
        }
        getPolicyConstraints().getExclusion().add(new ExclusionPolicyConstraintType());
        initConstraintsContainer(getConstraintsContainer());
        target.add(getConstraintsContainer());
    }

    private void addNewMinAssigneesPerformed(AjaxRequestTarget target){
        if (getPolicyConstraints().getMinAssignees() == null){
            getPolicyConstraints().createMinAssigneesList();
        }
        getPolicyConstraints().getMinAssignees().add(new MultiplicityPolicyConstraintType());
        initConstraintsContainer(getConstraintsContainer());
        target.add(getConstraintsContainer());
    }

    private void addNewMaxAssigneesPerformed(AjaxRequestTarget target){
        if (getPolicyConstraints().getMaxAssignees() == null){
            getPolicyConstraints().createMaxAssigneesList();
        }
        getPolicyConstraints().getMaxAssignees().add(new MultiplicityPolicyConstraintType());
        initConstraintsContainer(getConstraintsContainer());
        target.add(getConstraintsContainer());
    }

    private void addNewModificationPerformed(AjaxRequestTarget target){
        if (getPolicyConstraints().getModification() == null){
            getPolicyConstraints().createModificationList();
        }
        getPolicyConstraints().getModification().add(new ModificationPolicyConstraintType());
        initConstraintsContainer(getConstraintsContainer());
        target.add(getConstraintsContainer());
    }

    private void addNewTimeValidityPerformed(AjaxRequestTarget target){
        if (getPolicyConstraints().getTimeValidity() == null){
            getPolicyConstraints().createTimeValidityList();
        }
        getPolicyConstraints().getTimeValidity().add(new TimeValidityPolicyConstraintType());
        initConstraintsContainer(getConstraintsContainer());
        target.add(getConstraintsContainer());
    }

    private void addNewAssignmentPerformed(AjaxRequestTarget target){
        if (getPolicyConstraints().getAssignment() == null){
            getPolicyConstraints().createAssignmentList();
        }
        getPolicyConstraints().getAssignment().add(new AssignmentPolicyConstraintType());
        initConstraintsContainer(getConstraintsContainer());
        target.add(getConstraintsContainer());
    }

    private void addNewSituationPerformed(AjaxRequestTarget target){
        if (getPolicyConstraints().getSituation() == null){
            getPolicyConstraints().createSituationList();
        }
        getPolicyConstraints().getSituation().add(new PolicySituationPolicyConstraintType());
        initConstraintsContainer(getConstraintsContainer());
        target.add(getConstraintsContainer());
    }

    private PolicyConstraintsType getPolicyConstraints(){
        PolicyRuleType policyRule = getPolicyRuleTypeObject();
        PolicyConstraintsType constraints = policyRule.getPolicyConstraints();
        if (constraints == null){
            policyRule.setPolicyConstraints(new PolicyConstraintsType());
        }
        return policyRule.getPolicyConstraints();
    }

    private PolicyRuleType getPolicyRuleTypeObject(){
        PolicyRuleType policyRule = getModelObject().getAssignment().getPolicyRule();
        if (policyRule == null){
            getModelObject().getAssignment().setPolicyRule(new PolicyRuleType());
        }
        return getModelObject().getAssignment().getPolicyRule();
    }

    private WebMarkupContainer getConstraintsContainer(){
        return (WebMarkupContainer) get(ID_CONSTRAINTS_CONTAINER);
    }

}
