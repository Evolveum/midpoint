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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.evolveum.midpoint.gui.impl.model.PropertyWrapperFromContainerValueWrapperModel;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;

/**
 * Created by honchar.
 * @author katkav
 */
public class PolicyRulesPanel extends AssignmentPanel {

    private static final long serialVersionUID = 1L;
    
	private static final Trace LOGGER = TraceManager.getTrace(PolicyRulesPanel.class);

    public PolicyRulesPanel(String id, IModel<ContainerWrapper<AssignmentType>> assignmentContainerWrapperModel){
        super(id, assignmentContainerWrapperModel);

    }

    protected List<IColumn<ContainerValueWrapper<AssignmentType>, String>> initColumns() {
        List<IColumn<ContainerValueWrapper<AssignmentType>, String>> columns = new ArrayList<>();
        columns.add(new AbstractColumn<ContainerValueWrapper<AssignmentType>, String>(createStringResource("PolicyRulesPanel.constraintsColumn")){
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<ContainerValueWrapper<AssignmentType>>> cellItem, String componentId,
                                     final IModel<ContainerValueWrapper<AssignmentType>> rowModel) {
            	ContainerWrapper<PolicyRuleType> policyRuleWrapper = rowModel.getObject().findContainerWrapper(
			            ItemPath.create(rowModel.getObject().getPath(), AssignmentType.F_POLICY_RULE));
            	ContainerWrapper<PolicyConstraintsType> wrapper = policyRuleWrapper.getValues().get(0).findContainerWrapper(ItemPath.create(policyRuleWrapper.getPath(), PolicyRuleType.F_POLICY_CONSTRAINTS));
            	String constraints = PolicyRuleTypeUtil.toShortString(wrapper.getValues().get(0).getContainerValue().getValue());
                cellItem.add(new MultiLineLabel(componentId, Model.of(constraints != null && !constraints.equals("null") ? constraints : "")));
            }

        });
        columns.add(new AbstractColumn<ContainerValueWrapper<AssignmentType>, String>(createStringResource("PolicyRulesPanel.situationColumn")){
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<ContainerValueWrapper<AssignmentType>>> cellItem, String componentId,
                                     final IModel<ContainerValueWrapper<AssignmentType>> rowModel) {
            	ContainerWrapper<PolicyRuleType> policyRuleWrapper = rowModel.getObject().findContainerWrapper(ItemPath.create(rowModel.getObject().getPath(), AssignmentType.F_POLICY_RULE));
            	PropertyWrapperFromContainerValueWrapperModel<String, PolicyRuleType> propertyModel = new PropertyWrapperFromContainerValueWrapperModel(policyRuleWrapper.getValues().get(0), PolicyRuleType.F_POLICY_SITUATION);
            	String situationValue = propertyModel.getObject().getValues().get(0).getValue().getRealValue();
                cellItem.add(new Label(componentId, Model.of(situationValue)));
            }

        });
        columns.add(new AbstractColumn<ContainerValueWrapper<AssignmentType>, String>(createStringResource("PolicyRulesPanel.actionColumn")){
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<ContainerValueWrapper<AssignmentType>>> cellItem, String componentId,
                                     final IModel<ContainerValueWrapper<AssignmentType>> rowModel) {
            	ContainerWrapper<PolicyRuleType> policyRuleWrapper = rowModel.getObject().findContainerWrapper(ItemPath.create(rowModel.getObject().getPath(), AssignmentType.F_POLICY_RULE));
            	ContainerWrapper<PolicyActionsType> wrapper = policyRuleWrapper.getValues().get(0).findContainerWrapper(ItemPath.create(policyRuleWrapper.getValues().get(0).getPath(), PolicyRuleType.F_POLICY_ACTIONS));
            	String action = wrapper != null ?
						PolicyRuleTypeUtil.toShortString(wrapper.getValues().get(0).getContainerValue().getValue(), new ArrayList<>()) : null;
                cellItem.add(new MultiLineLabel(componentId, Model.of(action != null && !action.equals("null") ? action : "")));
            }

        });
        columns.add(new AbstractColumn<ContainerValueWrapper<AssignmentType>, String>(createStringResource("PolicyRulesPanel.orderColumn")){
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<ContainerValueWrapper<AssignmentType>>> cellItem, String componentId,
                                     final IModel<ContainerValueWrapper<AssignmentType>> rowModel) {
            	PropertyWrapperFromContainerValueWrapperModel<Integer, AssignmentType> propertyModel = new PropertyWrapperFromContainerValueWrapperModel(rowModel.getObject(), AssignmentType.F_ORDER);
                
                String orderValue;
                if (propertyModel == null || propertyModel.getObject().getValues().get(0).getValue().getRealValue() == null){
                    orderValue = "";
                } else {
                    orderValue = Integer.toString(propertyModel.getObject().getValues().get(0).getValue().getRealValue());
                }
                cellItem.add(new Label(componentId, Model.of(orderValue)));
            }

        });
        return columns;
    }

	@Override
	protected void initCustomPaging() {
		getAssignmentsTabStorage().setPaging(ObjectPaging.createPaging(0, ((int) getParentPage().getItemsPerPage(UserProfileStorage.TableId.POLICY_RULES_TAB_TABLE))));

	}

	@Override
	protected TableId getTableId() {
		return UserProfileStorage.TableId.POLICY_RULES_TAB_TABLE;
	}

	@Override
	protected void newAssignmentClickPerformed(AjaxRequestTarget target) {
        PrismContainerValue<AssignmentType> newAssignment = getModelObject().getItem().createNewValue();
        newAssignment.asContainerable().setPolicyRule(new PolicyRuleType());
        ContainerValueWrapper<AssignmentType> newAssignmentWrapper = getMultivalueContainerListPanel().createNewItemContainerValueWrapper(newAssignment, getModel());
        newAssignmentWrapper.setShowEmpty(true, false);
        newAssignmentWrapper.computeStripes();
        getMultivalueContainerListPanel().itemDetailsPerformed(target, Arrays.asList(newAssignmentWrapper));
	}

	@Override
	protected ObjectQuery createObjectQuery() {
        return QueryBuilder.queryFor(AssignmentType.class, getParentPage().getPrismContext())
                .exists(AssignmentType.F_POLICY_RULE)
                .build();
    }

	@Override
	protected Fragment getCustomSpecificContainers(String contentAreaId, ContainerValueWrapper<AssignmentType> modelObject) {
		Fragment specificContainers = new Fragment(contentAreaId, AssignmentPanel.ID_SPECIFIC_CONTAINERS_FRAGMENT, this);
		specificContainers.add(getSpecificContainerPanel(modelObject));
		return specificContainers;
	}
	
	@Override
	protected IModel<ContainerWrapper> getSpecificContainerModel(ContainerValueWrapper<AssignmentType> modelObject) {
		ContainerWrapper<PolicyRuleType> policyRuleWrapper = modelObject.findContainerWrapper(ItemPath.create(modelObject.getPath(), AssignmentType.F_POLICY_RULE));
		return Model.of(policyRuleWrapper);
	}

	@Override
	protected List<SearchItemDefinition> createSearchableItems(PrismContainerDefinition<AssignmentType> containerDef) {
		List<SearchItemDefinition> defs = new ArrayList<>();
		
		SearchFactory.addSearchPropertyDef(containerDef, ItemPath.create(AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS), defs);
		SearchFactory.addSearchPropertyDef(containerDef, ItemPath.create(AssignmentType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS), defs);
		SearchFactory.addSearchPropertyDef(containerDef, ItemPath.create(AssignmentType.F_POLICY_RULE, PolicyRuleType.F_NAME), defs);
		SearchFactory.addSearchRefDef(containerDef,
				ItemPath.create(AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_CONSTRAINTS,
						PolicyConstraintsType.F_EXCLUSION, ExclusionPolicyConstraintType.F_TARGET_REF), defs, AreaCategoryType.POLICY, getPageBase());
		
		defs.addAll(SearchFactory.createExtensionDefinitionList(containerDef));
		
		return defs;
	}
}
