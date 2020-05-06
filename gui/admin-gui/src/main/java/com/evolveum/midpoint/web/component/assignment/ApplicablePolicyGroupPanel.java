/*
 * Copyright (c) 2015-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.assignment;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.form.CheckBoxPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Created by honchar.
 */
public class ApplicablePolicyGroupPanel extends BasePanel<ObjectReferenceType>{
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = ApplicablePolicyGroupPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_POLICY_GROUP_MEMBERS = DOT_CLASS + "loadPolicyGroupMembers";
    private static final String OPERATION_LOAD_POLICY_GROUP_NAME = DOT_CLASS + "loadPolicyGroupName";

    private static final String ID_POLICY_GROUP_NAME = "policyGroupName";
    private static final String ID_POLICIES_CONTAINER = "policiesContainer";
    private static final String ID_POLICY_CHECK_BOX = "policyCheckBox";
    private LoadableModel<List<PrismObject<AbstractRoleType>>> policiesListModel;
    IModel<PrismContainerWrapper<AssignmentType>> assignmentsModel;

    public ApplicablePolicyGroupPanel(String id, IModel<ObjectReferenceType> model, IModel<PrismContainerWrapper<AssignmentType>> assignmentsModel){
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
                ObjectQuery membersQuery = getPageBase().getPrismContext().queryFor(AbstractRoleType.class)
                        .isChildOf(policyGroupObject.getOid())
                        .build();
                List<PrismObject<AbstractRoleType>> policiesList = WebModelServiceUtils.searchObjects(AbstractRoleType.class, membersQuery, result, getPageBase());
                policiesList.sort((o1, o2) -> {
                    String displayName1 = WebComponentUtil.getDisplayNameOrName(o1);
                    String displayName2 = WebComponentUtil.getDisplayNameOrName(o2);
                    return String.CASE_INSENSITIVE_ORDER.compare(displayName1, displayName2);
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
        for (PrismContainerValueWrapper<AssignmentType> assignment : assignmentsModel.getObject().getValues()){
            ObjectReferenceType targetRef = assignment.getRealValue().getTargetRef();
            if (targetRef != null && targetRef.getOid().equals(policyRoleOid)){
                return true;
            }
        }
        return false;
    }

    private ValueStatus getExistingAssignmentStatus(String policyRoleOid){
        for (PrismContainerValueWrapper<AssignmentType> assignment : assignmentsModel.getObject().getValues()){
            ObjectReferenceType targetRef = assignment.getRealValue().getTargetRef();
            if (targetRef != null && targetRef.getOid().equals(policyRoleOid)){
                return assignment.getStatus();
            }
        }
        return null;
    }

    private void onPolicyAddedOrRemoved(PrismObject<AbstractRoleType> assignmentTargetObject, boolean added){
        if (isAssignmentAlreadyInList(assignmentTargetObject.getOid())){
            PrismContainerValueWrapper<AssignmentType> assignmentToRemove = null;
            for (PrismContainerValueWrapper<AssignmentType> assignment : assignmentsModel.getObject().getValues()){
                ObjectReferenceType targetRef = assignment.getRealValue().getTargetRef();
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
                //TODO: not sure if this is correct way of creating new value.. this value is added directly to the origin object... what about deltas??
                PrismContainerValue<AssignmentType> newAssignment = assignmentsModel.getObject().getItem().createNewValue();
                ObjectReferenceType ref = ObjectTypeUtil.createObjectRef(assignmentTargetObject, getPageBase().getPrismContext());
                AssignmentType assignmentType = newAssignment.asContainerable();
                assignmentType.setTargetRef(ref);
                Task task = getPageBase().createSimpleTask("Creating new applicable policy");

                WrapperContext context = new WrapperContext(task, null);
                PrismContainerValueWrapper<AssignmentType> valueWrapper;
                try {
                    valueWrapper = (PrismContainerValueWrapper<AssignmentType>) getPageBase().createValueWrapper(assignmentsModel.getObject(), newAssignment, ValueStatus.ADDED, context);
                    assignmentsModel.getObject().getValues().add(valueWrapper);
                } catch (SchemaException e) {
                    //TOTO error handling
                }
//
//                valueWrapper.setShowEmpty(true, false);

            }
        }
    }
}
