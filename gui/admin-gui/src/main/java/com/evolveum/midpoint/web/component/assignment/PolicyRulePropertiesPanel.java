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
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

/**
 * Created by honchar
 * */
public class PolicyRulePropertiesPanel extends BasePanel<PolicyRuleType>{

    private static final String ID_REPEATER = "repeater";
    private static final String ID_CONSTRAINTS_CONTAINER = "constraintsContainer";
    private static final String ID_EXCLUSION_REPEATER = "exclusionRepeater";
    private static final String ID_EXCLUSION_CONSTRAINTS = "exclusionConstraints";
    private static final String ID_MIN_ASSIGNEES_REPEATER = "minAssigneesRepeater";
    private static final String ID_MIN_ASSIGNEES_CONSTRAINTS = "minAssigneesConstraints";
    private static final String ID_MAX_ASSIGNEES_REPEATER = "maxAssigneesRepeater";
    private static final String ID_MAX_ASSIGNEES_CONSTRAINTS = "maxAssigneesConstraints";
    private static final String ID_MODIFICATION_REPEATER = "modificationRepeater";
    private static final String ID_MODIFICATION_CONSTRAINTS = "modificationConstraints";
    private static final String ID_ASSIGNMENT_REPEATER = "assignmentRepeater";
    private static final String ID_ASSIGNMENT_CONSTRAINTS = "assignmentConstraints";
    private static final String ID_TIME_VALIDITY_REPEATER = "timeValidityRepeater";
    private static final String ID_TIME_VALIDITY_CONSTRAINTS = "timeValidityConstraints";
    private static final String ID_SITUATION_REPEATER = "situationRepeater";
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

    public PolicyRulePropertiesPanel(String id, IModel<PolicyRuleType> policyRuleModel, PageBase pageBase){
        super(id, policyRuleModel);
        this.pageBase = pageBase;
        initLayout();
    }

    private void initLayout(){
        WebMarkupContainer constraintsContainer = new WebMarkupContainer(ID_CONSTRAINTS_CONTAINER);
        constraintsContainer.setOutputMarkupId(true);
        add(constraintsContainer);

        initConstraintsContainer(constraintsContainer);

        add(new Label(ID_SITUATION_VALUE, new PropertyModel<>(getModel(), PolicyRuleType.F_POLICY_SITUATION.getLocalPart())));

        add(new Label(ID_ACTION_VALUE, Model.of(PolicyRuleUtil.convertPolicyActionsContainerToString(getModelObject()))));

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
        ListView exclusionRepeater = new ListView<ExclusionPolicyConstraintType>(ID_EXCLUSION_REPEATER, new PropertyModel<>(getModel(),
                pageBase.createPropertyModelExpression(PolicyRuleType.F_POLICY_CONSTRAINTS.getLocalPart(), PolicyConstraintsType.F_EXCLUSION.getLocalPart()))) {
            @Override
            protected void populateItem(final ListItem<ExclusionPolicyConstraintType> listItem) {
                PolicyRuleConstraintsExpandablePanel exclusionPropertiesPanel =
                        new PolicyRuleConstraintsExpandablePanel(ID_EXCLUSION_CONSTRAINTS, listItem.getModel());
                listItem.add(exclusionPropertiesPanel);
            }
        };
        constraintsContainer.addOrReplace(exclusionRepeater);

        ListView minAssigneesRepeater = new ListView<MultiplicityPolicyConstraintType>(ID_MIN_ASSIGNEES_REPEATER, new PropertyModel<>(getModel(),
                pageBase.createPropertyModelExpression(PolicyRuleType.F_POLICY_CONSTRAINTS.getLocalPart(), PolicyConstraintsType.F_MIN_ASSIGNEES.getLocalPart()))) {
            @Override
            protected void populateItem(final ListItem<MultiplicityPolicyConstraintType> listItem) {
                PolicyRuleConstraintsExpandablePanel minAssigneesPropertiesPanel =
                        new PolicyRuleConstraintsExpandablePanel(ID_MIN_ASSIGNEES_CONSTRAINTS, listItem.getModel());
                listItem.add(minAssigneesPropertiesPanel);
            }
        };
        constraintsContainer.addOrReplace(minAssigneesRepeater);

        ListView maxAssigneesRepeater = new ListView<MultiplicityPolicyConstraintType>(ID_MAX_ASSIGNEES_REPEATER, new PropertyModel<>(getModel(),
                pageBase.createPropertyModelExpression(PolicyRuleType.F_POLICY_CONSTRAINTS.getLocalPart(), PolicyConstraintsType.F_MAX_ASSIGNEES.getLocalPart()))) {
            @Override
            protected void populateItem(final ListItem<MultiplicityPolicyConstraintType> listItem) {
                PolicyRuleConstraintsExpandablePanel maxAssigneesPropertiesPanel =
                        new PolicyRuleConstraintsExpandablePanel(ID_MAX_ASSIGNEES_CONSTRAINTS, listItem.getModel());
                listItem.add(maxAssigneesPropertiesPanel);
            }
        };
        constraintsContainer.addOrReplace(maxAssigneesRepeater);

        ListView modificationRepeater = new ListView<ModificationPolicyConstraintType>(ID_MODIFICATION_REPEATER, new PropertyModel<>(getModel(),
                pageBase.createPropertyModelExpression(PolicyRuleType.F_POLICY_CONSTRAINTS.getLocalPart(), PolicyConstraintsType.F_MODIFICATION.getLocalPart()))) {
            @Override
            protected void populateItem(final ListItem<ModificationPolicyConstraintType> listItem) {
                PolicyRuleConstraintsExpandablePanel modificationPropertiesPanel =
                        new PolicyRuleConstraintsExpandablePanel(ID_MODIFICATION_CONSTRAINTS, listItem.getModel());
                listItem.add(modificationPropertiesPanel);
            }
        };
        constraintsContainer.addOrReplace(modificationRepeater);

        ListView assignmentRepeater = new ListView<AssignmentModificationPolicyConstraintType>(ID_ASSIGNMENT_REPEATER, new PropertyModel<>(getModel(),
                pageBase.createPropertyModelExpression(PolicyRuleType.F_POLICY_CONSTRAINTS.getLocalPart(), PolicyConstraintsType.F_ASSIGNMENT.getLocalPart()))) {
            @Override
            protected void populateItem(final ListItem<AssignmentModificationPolicyConstraintType> listItem) {
                PolicyRuleConstraintsExpandablePanel assignmentPropertiesPanel =
                        new PolicyRuleConstraintsExpandablePanel(ID_ASSIGNMENT_CONSTRAINTS, listItem.getModel());
                listItem.add(assignmentPropertiesPanel);
            }
        };
        constraintsContainer.addOrReplace(assignmentRepeater);

        ListView timeValidityRepeater = new ListView<TimeValidityPolicyConstraintType>(ID_TIME_VALIDITY_REPEATER, new PropertyModel<>(getModel(),
                pageBase.createPropertyModelExpression(PolicyRuleType.F_POLICY_CONSTRAINTS.getLocalPart(), PolicyConstraintsType.F_OBJECT_TIME_VALIDITY.getLocalPart()))) {
            @Override
            protected void populateItem(final ListItem<TimeValidityPolicyConstraintType> listItem) {
                PolicyRuleConstraintsExpandablePanel timeValidityPropertiesPanel =
                        new PolicyRuleConstraintsExpandablePanel(ID_TIME_VALIDITY_CONSTRAINTS, listItem.getModel());
                listItem.add(timeValidityPropertiesPanel);
            }
        };
        constraintsContainer.addOrReplace(timeValidityRepeater);

        ListView situationRepeater = new ListView<PolicySituationPolicyConstraintType>(ID_SITUATION_REPEATER, new PropertyModel<>(getModel(),
                pageBase.createPropertyModelExpression(PolicyRuleType.F_POLICY_CONSTRAINTS.getLocalPart(), PolicyConstraintsType.F_SITUATION.getLocalPart()))) {
            @Override
            protected void populateItem(final ListItem<PolicySituationPolicyConstraintType> listItem) {
                PolicyRuleConstraintsExpandablePanel exclusionPropertiesPanel =
                        new PolicyRuleConstraintsExpandablePanel(ID_SITUATION_CONSTRAINTS, listItem.getModel());
                listItem.add(exclusionPropertiesPanel);
            }
        };
        constraintsContainer.addOrReplace(situationRepeater);
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
        if (getPolicyConstraints().getObjectTimeValidity() == null){
            getPolicyConstraints().createObjectTimeValidityList();
        }
        getPolicyConstraints().getObjectTimeValidity().add(new TimeValidityPolicyConstraintType());
        initConstraintsContainer(getConstraintsContainer());
        target.add(getConstraintsContainer());
    }

    private void addNewAssignmentPerformed(AjaxRequestTarget target){
        if (getPolicyConstraints().getAssignment() == null){
            getPolicyConstraints().createAssignmentList();
        }
        getPolicyConstraints().getAssignment().add(new AssignmentModificationPolicyConstraintType());
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
        PolicyRuleType policyRule = getModelObject();
        PolicyConstraintsType constraints = policyRule.getPolicyConstraints();
        if (constraints == null){
            policyRule.setPolicyConstraints(new PolicyConstraintsType());
        }
        return policyRule.getPolicyConstraints();
    }

     private WebMarkupContainer getConstraintsContainer(){
        return (WebMarkupContainer) get(ID_CONSTRAINTS_CONTAINER);
    }

}
