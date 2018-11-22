/*
 * Copyright (c) 2018 Evolveum
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
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;

/**
 * Created by honchar
 */
public abstract class SwitchAssignmentTypePanel extends BasePanel<ContainerWrapper<AssignmentType>> {
    private static final long serialVersionUID = 1L;

    private static final String ID_ALL_ASSIGNMENTS = "allAssignments";
    private static final String ID_ROLE_TYPE_ASSIGNMENTS = "roleTypeAssignments";
    private static final String ID_ORG_TYPE_ASSIGNMENTS = "orgTypeAssignments";
    private static final String ID_SERVICE_TYPE_ASSIGNMENTS = "serviceTypeAssignments";
    private static final String ID_RESOURCE_TYPE_ASSIGNMENTS = "resourceTypeAssignments";
    private static final String ID_POLICY_RULE_TYPE_ASSIGNMENTS = "policyRuleTypeAssignments";
    private static final String ID_CONSENT_ASSIGNMENTS = "consentAssignments";
    private static final String ID_ASSIGNMENTS = "assignmentsPanel";


    public SwitchAssignmentTypePanel(String id, IModel<ContainerWrapper<AssignmentType>> assignmentContainerWrapperModel) {
        super(id, assignmentContainerWrapperModel);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initButtonsPanel();
        initAssignmentsPanel();

        setOutputMarkupId(true);
    }

    private void initButtonsPanel(){
        AjaxButton allAssignmentsButton = new AjaxButton(ID_ALL_ASSIGNMENTS, createStringResource("AssignmentPanel.allLabel")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                AssignmentPanel assignmentPanel =
                        new AssignmentPanel(ID_ASSIGNMENTS, SwitchAssignmentTypePanel.this.getModel());
                assignmentPanel.setOutputMarkupId(true);
                SwitchAssignmentTypePanel.this.addOrReplace(assignmentPanel);
                target.add(SwitchAssignmentTypePanel.this);
            }
        };
        allAssignmentsButton.setOutputMarkupId(true);
        add(allAssignmentsButton);

        AjaxButton roleTypeAssignmentsButton = new AjaxButton(ID_ROLE_TYPE_ASSIGNMENTS, createStringResource("ObjectType.RoleType")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                AbstractRoleAssignmentPanel assignmentPanel =
                        new AbstractRoleAssignmentPanel(ID_ASSIGNMENTS, SwitchAssignmentTypePanel.this.getModel()) {
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected QName getAssignmentType() {
                                return RoleType.COMPLEX_TYPE;
                            }
                        };
                assignmentPanel.setOutputMarkupId(true);
                SwitchAssignmentTypePanel.this.addOrReplace(assignmentPanel);
                target.add(SwitchAssignmentTypePanel.this);
            }
        };
        roleTypeAssignmentsButton.setOutputMarkupId(true);
        add(roleTypeAssignmentsButton);

        AjaxButton orgTypeAssignmentsButton = new AjaxButton(ID_ORG_TYPE_ASSIGNMENTS, createStringResource("ObjectType.OrgType")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                AbstractRoleAssignmentPanel assignmentPanel =
                        new AbstractRoleAssignmentPanel(ID_ASSIGNMENTS, SwitchAssignmentTypePanel.this.getModel()) {
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected QName getAssignmentType() {
                                return OrgType.COMPLEX_TYPE;
                            }
                        };
                assignmentPanel.setOutputMarkupId(true);
                SwitchAssignmentTypePanel.this.addOrReplace(assignmentPanel);
                target.add(SwitchAssignmentTypePanel.this);
            }
        };
        orgTypeAssignmentsButton.setOutputMarkupId(true);
        add(orgTypeAssignmentsButton);

        AjaxButton serviceTypeAssignmentsButton = new AjaxButton(ID_SERVICE_TYPE_ASSIGNMENTS, createStringResource("ObjectType.ServiceType")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                AbstractRoleAssignmentPanel assignmentPanel =
                        new AbstractRoleAssignmentPanel(ID_ASSIGNMENTS, SwitchAssignmentTypePanel.this.getModel()) {
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected QName getAssignmentType() {
                                return ServiceType.COMPLEX_TYPE;
                            }
                        };
                assignmentPanel.setOutputMarkupId(true);
                SwitchAssignmentTypePanel.this.addOrReplace(assignmentPanel);
                target.add(SwitchAssignmentTypePanel.this);
            }
        };
        serviceTypeAssignmentsButton.setOutputMarkupId(true);
        add(serviceTypeAssignmentsButton);

        AjaxButton resourceTypeAssignmentsButton = new AjaxButton(ID_RESOURCE_TYPE_ASSIGNMENTS, createStringResource("ObjectType.ResourceType")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                ConstructionAssignmentPanel constructionsPanel =
                        new ConstructionAssignmentPanel(ID_ASSIGNMENTS, SwitchAssignmentTypePanel.this.getModel());
                constructionsPanel.setOutputMarkupId(true);
                SwitchAssignmentTypePanel.this.addOrReplace(constructionsPanel);
                target.add(SwitchAssignmentTypePanel.this);
            }
        };
        resourceTypeAssignmentsButton.setOutputMarkupId(true);
        add(resourceTypeAssignmentsButton);

        AjaxButton policyRuleTypeAssignmentsButton = new AjaxButton(ID_POLICY_RULE_TYPE_ASSIGNMENTS, createStringResource("AssignmentType.policyRule")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                PolicyRulesPanel policyRulesPanel =
                        new PolicyRulesPanel(ID_ASSIGNMENTS, SwitchAssignmentTypePanel.this.getModel()) ;
                policyRulesPanel.setOutputMarkupId(true);
                SwitchAssignmentTypePanel.this.addOrReplace(policyRulesPanel);
                target.add(SwitchAssignmentTypePanel.this);
            }
        };
        policyRuleTypeAssignmentsButton.setOutputMarkupId(true);
        policyRuleTypeAssignmentsButton.add(new VisibleBehaviour(()  -> isInducement()));
        add(policyRuleTypeAssignmentsButton);

        //GDPR feature.. temporary disabled MID-4281
//        AjaxButton consentsButton = new AjaxButton(ID_CONSENT_ASSIGNMENTS, createStringResource("FocusType.consents")) {
//
//            @Override
//            public void onClick(AjaxRequestTarget target) {
//                GdprAssignmentPanel gdprAssignmentPanel =
//                        new GdprAssignmentPanel(ID_ASSIGNMENTS, SwitchAssignmentTypePanel.this.getModel());
//                gdprAssignmentPanel.setOutputMarkupId(true);
//                SwitchAssignmentTypePanel.this.addOrReplace(gdprAssignmentPanel);
//                target.add(SwitchAssignmentTypePanel.this);
//            }
//        };
//        consentsButton.setOutputMarkupId(true);
//        add(consentsButton);
    }

    protected abstract boolean isInducement();

    private void initAssignmentsPanel(){
        AssignmentPanel assignmentsPanel = new AssignmentPanel(ID_ASSIGNMENTS, getModel());
        assignmentsPanel.setOutputMarkupId(true);
        add(assignmentsPanel);
    }
}
