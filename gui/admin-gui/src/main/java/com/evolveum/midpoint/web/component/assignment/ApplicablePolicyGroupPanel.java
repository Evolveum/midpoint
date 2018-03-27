/**
 * Copyright (c) 2015-2018 Evolveum
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
import com.evolveum.midpoint.gui.api.component.form.CheckBoxPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.IsolatedCheckBoxPanel;
import com.evolveum.midpoint.web.component.prism.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by honchar.
 */
public class ApplicablePolicyGroupPanel extends BasePanel<ObjectReferenceType>{
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ApplicablePolicyGroupPanel.class);
    private static final String DOT_CLASS = ApplicablePolicyGroupPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_POLICY_GROUP_MEMBERS = DOT_CLASS + "loadPolicyGroupMembers";
    private static final String OPERATION_LOAD_POLICY_GROUP_NAME = DOT_CLASS + "loadPolicyGroupName";

    private static final String ID_POLICY_GROUP_NAME = "policyGroupName";
    private static final String ID_POLICIES_CONTAINER = "policiesContainer";
    private static final String ID_POLICY_CHECK_BOX = "policyCheckBox";
    private LoadableModel<List<PrismObject<AbstractRoleType>>> policiesListModel;
    IModel<ContainerWrapper<AssignmentType>> assignmentsModel;

    public ApplicablePolicyGroupPanel(String id, IModel<ObjectReferenceType> model, IModel<ContainerWrapper<AssignmentType>> assignmentsModel){
        super(id, model);
        this.assignmentsModel = assignmentsModel;
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initModels();
        initLayout();
    }

    private void initModels(){
        policiesListModel = new LoadableModel<List<PrismObject<AbstractRoleType>>>(false) {
            @Override
            protected List<PrismObject<AbstractRoleType>> load() {
                OperationResult result = new OperationResult(OPERATION_LOAD_POLICY_GROUP_MEMBERS);

                ObjectReferenceType policyGroupObject = ApplicablePolicyGroupPanel.this.getModelObject();
                ObjectQuery membersQuery = QueryBuilder.queryFor(AbstractRoleType.class, getPageBase().getPrismContext())
                        .isChildOf(policyGroupObject.getOid())
                        .build();
                List<PrismObject<AbstractRoleType>> policiesList = WebModelServiceUtils.searchObjects(AbstractRoleType.class, membersQuery, result, getPageBase());
                Collections.sort(policiesList, new Comparator<PrismObject<AbstractRoleType>>() {
                    @Override
                    public int compare(PrismObject<AbstractRoleType> o1, PrismObject<AbstractRoleType> o2) {
                        String displayName1 = WebComponentUtil.getDisplayNameOrName(o1);
                        String displayName2 = WebComponentUtil.getDisplayNameOrName(o2);
                        return String.CASE_INSENSITIVE_ORDER.compare(displayName1, displayName2);
                    }
                });
                return policiesList;
            }
        };
    }

    private void initLayout(){
        Label policyGroupName = new Label(ID_POLICY_GROUP_NAME, Model.of(WebComponentUtil.getDisplayNameOrName(getModelObject(), getPageBase(), OPERATION_LOAD_POLICY_GROUP_NAME)));
        policyGroupName.setOutputMarkupId(true);
        add(policyGroupName);

        ListView<PrismObject<AbstractRoleType>> policiesPanel = new ListView<PrismObject<AbstractRoleType>>(ID_POLICIES_CONTAINER, policiesListModel){
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<PrismObject<AbstractRoleType>> listItem) {
            	PrismObject<AbstractRoleType> abstractRole = listItem.getModelObject();
            	CheckBoxPanel policyCheckBox = new CheckBoxPanel(ID_POLICY_CHECK_BOX,
            			getCheckboxModel(abstractRole),
            			null, // visibility
            			Model.of(WebComponentUtil.getDisplayNameOrName(abstractRole)), // label
            			null // tooltip
            			) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onUpdate(AjaxRequestTarget target) {
                        onPolicyAddedOrRemoved(listItem.getModelObject(), getValue());
                    }
                };
                policyCheckBox.setOutputMarkupId(true);
                listItem.add(policyCheckBox);
            }
        };
        policiesPanel.setOutputMarkupId(true);
        add(policiesPanel);
    }

    private IModel<Boolean> getCheckboxModel(PrismObject<AbstractRoleType> abstractRole) {
    	return Model.of(isAssignmentAlreadyInList(abstractRole.getOid()) &&
                !ValueStatus.DELETED.equals(getExistingAssignmentStatus(abstractRole.getOid())));
    }

    private boolean isAssignmentAlreadyInList(String policyRoleOid){
        for (ContainerValueWrapper<AssignmentType> assignment : assignmentsModel.getObject().getValues()){
            ObjectReferenceType targetRef = assignment.getContainerValue().getValue().getTargetRef();
            if (targetRef != null && targetRef.getOid().equals(policyRoleOid)){
                return true;
            }
        }
        return false;
    }

    private ValueStatus getExistingAssignmentStatus(String policyRoleOid){
        for (ContainerValueWrapper<AssignmentType> assignment : assignmentsModel.getObject().getValues()){
            ObjectReferenceType targetRef = assignment.getContainerValue().getValue().getTargetRef();
            if (targetRef != null && targetRef.getOid().equals(policyRoleOid)){
                return assignment.getStatus();
            }
        }
        return null;
    }

    private void onPolicyAddedOrRemoved(PrismObject<AbstractRoleType> assignmentTargetObject, boolean added){
        if (isAssignmentAlreadyInList(assignmentTargetObject.getOid())){
            ContainerValueWrapper<AssignmentType> assignmentToRemove = null;
            for (ContainerValueWrapper<AssignmentType> assignment : assignmentsModel.getObject().getValues()){
                ObjectReferenceType targetRef = assignment.getContainerValue().getValue().getTargetRef();
                if (targetRef != null && targetRef.getOid().equals(assignmentTargetObject.getOid())){
                    if (added && assignment.getStatus() == ValueStatus.DELETED){
                        assignment.setStatus(ValueStatus.NOT_CHANGED);
                    } else if (!added && assignment.getStatus() == ValueStatus.ADDED){
                        assignmentToRemove = assignment;
                    } else if (!added){
                        assignment.setStatus(ValueStatus.DELETED);
                    }
                }
            }
            assignmentsModel.getObject().getValues().remove(assignmentToRemove);
        } else {
            if (added){
                PrismContainerValue<AssignmentType> newAssignment = assignmentsModel.getObject().getItem().createNewValue();
                ObjectReferenceType ref = ObjectTypeUtil.createObjectRef(assignmentTargetObject);
                AssignmentType assignmentType = newAssignment.asContainerable();
                assignmentType.setTargetRef(ref);
                ContainerWrapperFactory factory = new ContainerWrapperFactory(getPageBase());
                ContainerValueWrapper<AssignmentType> valueWrapper = factory.createContainerValueWrapper(assignmentsModel.getObject(), newAssignment,
                        assignmentsModel.getObject().getObjectStatus(), ValueStatus.ADDED, assignmentsModel.getObject().getPath());
                valueWrapper.setShowEmpty(true, false);
                assignmentsModel.getObject().getValues().add(valueWrapper);
            }
        }
    }
}
