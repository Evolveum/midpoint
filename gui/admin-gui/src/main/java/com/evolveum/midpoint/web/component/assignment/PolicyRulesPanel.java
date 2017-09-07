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

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.session.AssignmentsTabStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;

/**
 * Created by honchar.
 * @author katkav
 */
public class PolicyRulesPanel extends AssignmentPanel {

    private static final long serialVersionUID = 1L;


    public PolicyRulesPanel(String id, IModel<List<AssignmentDto>> policyRulesModel){
        super(id, policyRulesModel);

    }

    protected List<IColumn<AssignmentDto, String>> initColumns() {
        List<IColumn<AssignmentDto, String>> columns = new ArrayList<>();
        columns.add(new AbstractColumn<AssignmentDto, String>(createStringResource("PolicyRulesPanel.constraintsColumn")){
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<AssignmentDto>> cellItem, String componentId,
                                     final IModel<AssignmentDto> rowModel) {
                PolicyRuleType policyRuleType = rowModel.getObject().getAssignment().getPolicyRule();
                cellItem.add(new MultiLineLabel(componentId, Model.of(PolicyRuleUtil.convertPolicyConstraintsContainerToString(policyRuleType, getParentPage()))));
            }

        });
        columns.add(new AbstractColumn<AssignmentDto, String>(createStringResource("PolicyRulesPanel.situationColumn")){
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<AssignmentDto>> cellItem, String componentId,
                                     final IModel<AssignmentDto> rowModel) {
            	PolicyRuleType policyRuleType = rowModel.getObject().getAssignment().getPolicyRule();
                String situationValue = policyRuleType == null ? "" : policyRuleType.getPolicySituation();
                cellItem.add(new Label(componentId, Model.of(situationValue)));
            }

        });
        columns.add(new AbstractColumn<AssignmentDto, String>(createStringResource("PolicyRulesPanel.actionColumn")){
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<AssignmentDto>> cellItem, String componentId,
                                     final IModel<AssignmentDto> rowModel) {
            	PolicyRuleType policyRuleType = rowModel.getObject().getAssignment().getPolicyRule();
                cellItem.add(new MultiLineLabel(componentId, Model.of(PolicyRuleUtil.convertPolicyActionsContainerToString(policyRuleType))));
            }

        });
        columns.add(new AbstractColumn<AssignmentDto, String>(createStringResource("PolicyRulesPanel.orderColumn")){
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<AssignmentDto>> cellItem, String componentId,
                                     final IModel<AssignmentDto> rowModel) {
                AssignmentType assignment = rowModel.getObject().getAssignment();

                String orderValue;
                if (assignment == null || assignment.getOrder() == null){
                    orderValue = "";
                } else {
                    orderValue = Integer.toString(assignment.getOrder());
                }
                cellItem.add(new Label(componentId, Model.of(orderValue)));
            }

        });
        return columns;
    }

	@Override
	protected void initPaging() {
		  getPolicyRulesTabStorage().setPaging(ObjectPaging.createPaging(0, getItemsPerPage()));

	}

	@Override
	protected TableId getTableId() {
		return UserProfileStorage.TableId.POLICY_RULES_TAB_TABLE;
	}

	@Override
	protected int getItemsPerPage() {
		return (int) getParentPage().getItemsPerPage(UserProfileStorage.TableId.POLICY_RULES_TAB_TABLE);
	}

    private AssignmentsTabStorage getPolicyRulesTabStorage(){
        return getParentPage().getSessionStorage().getAssignmentsTabStorage();
    }

	@Override
	protected void newAssignmentClickPerformed(AjaxRequestTarget target) {
		// TODO Auto-generated method stub

        AssignmentType assignment = new AssignmentType();

        PolicyRuleType policyRule = new PolicyRuleType(getPageBase().getPrismContext());
        policyRule.setDescription("");
        assignment.setPolicyRule(policyRule);

        getModelObject().add(new AssignmentDto(assignment, UserDtoStatus.ADD));
        target.add(getAssignmentContainer());
	}

	@Override
	protected ObjectQuery createObjectQuery() {
		return null;
	}

	@Override
	protected AbstractAssignmentDetailsPanel createDetailsPanel(String idAssignmentDetails, Form<?> form, IModel<AssignmentDto> model,
			PageBase parentPage) {
		return new PolicyRuleDetailsPanel(idAssignmentDetails, form, model, parentPage);
	}

}
