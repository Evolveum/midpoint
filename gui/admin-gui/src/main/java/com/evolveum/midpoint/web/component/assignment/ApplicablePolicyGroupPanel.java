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
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.CheckBoxPanel;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.page.admin.users.component.AbstractRoleMemberPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

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
    private static final String ID_POLICY_LABEL = "policyLabel";
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
        policiesListModel = new LoadableModel<List<PrismObject<AbstractRoleType>>>() {
            @Override
            protected List<PrismObject<AbstractRoleType>> load() {
                OperationResult result = new OperationResult(OPERATION_LOAD_POLICY_GROUP_MEMBERS);

                ObjectReferenceType policyGroupObject = ApplicablePolicyGroupPanel.this.getModelObject();
                ObjectQuery membersQuery = QueryBuilder.queryFor(AbstractRoleType.class, getPageBase().getPrismContext())
                        .isChildOf(policyGroupObject.getOid())
                        .build();
                return WebModelServiceUtils.searchObjects(AbstractRoleType.class, membersQuery, result, getPageBase());
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
                CheckBoxPanel policyCheckBox = new CheckBoxPanel(ID_POLICY_CHECK_BOX,
                        ApplicablePolicyGroupPanel.this.isRoleAssignedModel(listItem.getModelObject().getOid()));
                policyCheckBox.setOutputMarkupId(true);
                listItem.add(policyCheckBox);

                Label policyLabel = new Label(ID_POLICY_LABEL, Model.of(WebComponentUtil.getDisplayNameOrName(listItem.getModelObject())));
                policyLabel.setOutputMarkupId(true);
                listItem.add(policyLabel);
            }
        };
        policiesPanel.setOutputMarkupId(true);
        add(policiesPanel);
    }


    private IModel<Boolean> isRoleAssignedModel(String policyRoleOid){
        for (ContainerValueWrapper<AssignmentType> assignment : assignmentsModel.getObject().getValues()){
            ObjectReferenceType targetRef = assignment.getContainerValue().getValue().getTargetRef();
            if (targetRef != null && targetRef.getOid().equals(policyRoleOid)){
                return Model.of(true);
            }
        }
        return Model.of(false);
    }
}
