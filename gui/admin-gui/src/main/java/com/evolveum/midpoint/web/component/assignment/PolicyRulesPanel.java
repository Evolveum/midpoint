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

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar.
 */
public abstract class PolicyRulesPanel extends BasePanel<List<AssignmentEditorDto>> {
    private static final long serialVersionUID = 1L;
    private static final String ID_POLICY_RULES = "policyRules";
    private static final String ID_MAIN_POLICY_RULE_PANEL = "mainPolicyRulesPanel";

    private PageBase pageBase;

    public PolicyRulesPanel(String id, IModel<List<AssignmentEditorDto>> policyRulesModel, PageBase pageBase){
        super(id, policyRulesModel);
        this.pageBase = pageBase;
        initLayout();
    }

    private void initLayout(){
        WebMarkupContainer policyRulesContainer = new WebMarkupContainer(ID_POLICY_RULES);
        policyRulesContainer.setOutputMarkupId(true);
        add(policyRulesContainer);

        ListDataProvider<AssignmentEditorDto> provider = new ListDataProvider<AssignmentEditorDto>(this, getModel(), false);
        BoxedTablePanel<AssignmentEditorDto> policyRulesTable = new BoxedTablePanel<AssignmentEditorDto>(ID_MAIN_POLICY_RULE_PANEL,
                provider, initColumns(), UserProfileStorage.TableId.POLICY_RULES_TAB_TABLE,
                (int) pageBase.getItemsPerPage(UserProfileStorage.TableId.POLICY_RULES_TAB_TABLE)){
            private static final long serialVersionUID = 1L;

            @Override
            public int getItemsPerPage() {
                return pageBase.getSessionStorage().getUserProfile().getTables().get(UserProfileStorage.TableId.ASSIGNMENTS_TAB_TABLE);
            }

        };
        policyRulesTable.setOutputMarkupId(true);
//        policyRulesTable.setCurrentPage(getPaging());
        policyRulesContainer.addOrReplace(policyRulesTable);

    }

    private List<IColumn<AssignmentEditorDto, String>> initColumns() {
        List<IColumn<AssignmentEditorDto, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxHeaderColumn<AssignmentEditorDto>());

        columns.add(new IconColumn<AssignmentEditorDto>(Model.of("")){
            private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> createIconModel(IModel<AssignmentEditorDto> rowModel) {
                if (rowModel.getObject().getType() == null){
                    return Model.of("");
                }
                return Model.of(GuiStyleConstants.CLASS_POLICY_RULES);
            }

            @Override
            protected IModel<String> createTitleModel(IModel<AssignmentEditorDto> rowModel) {
                return createStringResource("PolicyRulesPanel.imageTitle");
            }

        });

        columns.add(new LinkColumn<AssignmentEditorDto>(createStringResource("PolicyRulesPanel.nameColumn")){
            private static final long serialVersionUID = 1L;

            @Override
            protected IModel createLinkModel(IModel<AssignmentEditorDto> rowModel) {
                String policyRuleName = rowModel.getObject().getName();
                if (policyRuleName != null && policyRuleName.trim().endsWith("-")){
                    policyRuleName = policyRuleName.substring(0, policyRuleName.lastIndexOf("-"));
                }
                return Model.of(policyRuleName != null ? policyRuleName : "");
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<AssignmentEditorDto> rowModel) {
                assignmentDetailsPerformed(rowModel, pageBase, target);
            }
        });
        columns.add(new AbstractColumn<AssignmentEditorDto, String>(createStringResource("PolicyRulesPanel.constraintsColumn")){
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<AssignmentEditorDto>> cellItem, String componentId,
                                     final IModel<AssignmentEditorDto> rowModel) {
                PrismContainer<PolicyRuleType> policyRuleContainer = rowModel.getObject().getPolicyRuleContainer(null);
                cellItem.add(new Label(componentId, Model.of(PolicyRuleUtil.convertPolicyConstraintsContainerToString(policyRuleContainer, pageBase))));
            }

        });
        columns.add(new AbstractColumn<AssignmentEditorDto, String>(createStringResource("PolicyRulesPanel.situationColumn")){
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<AssignmentEditorDto>> cellItem, String componentId,
                                     final IModel<AssignmentEditorDto> rowModel) {
                PrismContainer<PolicyRuleType> policyRuleContainer = rowModel.getObject().getPolicyRuleContainer(null);
                String situationValue = policyRuleContainer == null ? "" : policyRuleContainer.getValue().getValue().getPolicySituation();
                cellItem.add(new Label(componentId, Model.of(situationValue)));
            }

        });
        columns.add(new AbstractColumn<AssignmentEditorDto, String>(createStringResource("PolicyRulesPanel.actionColumn")){
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<AssignmentEditorDto>> cellItem, String componentId,
                                     final IModel<AssignmentEditorDto> rowModel) {
                PrismContainer<PolicyRuleType> policyRuleContainer = rowModel.getObject().getPolicyRuleContainer(null);
                cellItem.add(new Label(componentId, Model.of(PolicyRuleUtil.convertPolicyActionsContainerToString(policyRuleContainer))));
            }

        });
        columns.add(new AbstractColumn<AssignmentEditorDto, String>(createStringResource("PolicyRulesPanel.orderColumn")){
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<AssignmentEditorDto>> cellItem, String componentId,
                                     final IModel<AssignmentEditorDto> rowModel) {
                PrismContainerValue<AssignmentType> assignment = rowModel.getObject().getOldValue();

                String orderValue;
                if (assignment == null || assignment.getValue() == null || assignment.getValue().getOrder() == null){
                    orderValue = "";
                } else {
                    orderValue = Integer.toString(assignment.getValue().getOrder());
                }
                cellItem.add(new Label(componentId, Model.of(orderValue)));
            }

        });
        return columns;
    }

    protected abstract void assignmentDetailsPerformed(IModel<AssignmentEditorDto> policyRuleModel, PageBase pageBase, AjaxRequestTarget target);

//    private AssignmentsTabStorage getPolicyRulesTabStorage(){
//        return pageBase.getSessionStorage().getAssignmentsTabStorage();
//    }
//
//    private void initPaging(){
//        getPolicyRulesTabStorage().setPaging(ObjectPaging.createPaging(0, (int) pageBase.getItemsPerPage(UserProfileStorage.TableId.ASSIGNMENTS_TAB_TABLE)));
//    }

}
