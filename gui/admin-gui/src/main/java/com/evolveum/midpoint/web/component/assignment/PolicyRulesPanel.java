/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.assignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn.ColumnType;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismContainerWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;

/**
 * Created by honchar.
 * @author katkav
 */
public class PolicyRulesPanel extends AssignmentPanel {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PolicyRulesPanel.class);

    public PolicyRulesPanel(String id, IModel<PrismContainerWrapper<AssignmentType>> assignmentContainerWrapperModel){
        super(id, assignmentContainerWrapperModel);

    }

    protected List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> initColumns() {
        List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> columns = new ArrayList<>();


        columns.add(new PrismContainerWrapperColumn<>(getModel(), ItemPath.create(AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_CONSTRAINTS), getPageBase()));

        columns.add(new PrismPropertyWrapperColumn<>(getModel(), ItemPath.create(AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_SITUATION), ColumnType.STRING, getPageBase()));

        columns.add(new PrismContainerWrapperColumn<>(getModel(), ItemPath.create(AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_ACTIONS), getPageBase()));

        columns.add(new PrismPropertyWrapperColumn<>(getModel(), AssignmentType.F_ORDER, ColumnType.STRING, getPageBase()));

        return columns;
    }

    @Override
    protected void initCustomPaging() {
        getAssignmentsTabStorage().setPaging(getPrismContext().queryFactory()
                .createPaging(0, ((int) getParentPage().getItemsPerPage(UserProfileStorage.TableId.POLICY_RULES_TAB_TABLE))));

    }

    @Override
    protected TableId getTableId() {
        return UserProfileStorage.TableId.POLICY_RULES_TAB_TABLE;
    }

    @Override
    protected void newAssignmentClickPerformed(AjaxRequestTarget target, AssignmentObjectRelation assignmentTargetRelation) {
        PrismContainerValue<AssignmentType> newAssignment = getModelObject().getItem().createNewValue();
        AssignmentType assignmentType = newAssignment.asContainerable();
        try {
            newAssignment.findOrCreateContainer(AssignmentType.F_POLICY_RULE);
            assignmentType.setPolicyRule(new PolicyRuleType());
        } catch (SchemaException e) {
            LOGGER.error("Cannot create policy rule assignment: {}", e.getMessage(), e);
            getSession().error("Cannot create policyRule assignment.");
            target.add(getPageBase().getFeedbackPanel());
            return;
        }
        PrismContainerValueWrapper<AssignmentType> newAssignmentWrapper = getMultivalueContainerListPanel().createNewItemContainerValueWrapper(newAssignment, getModelObject(), target);
        getMultivalueContainerListPanel().itemDetailsPerformed(target, Collections.singletonList(newAssignmentWrapper));
    }

    @Override
    protected ObjectQuery createObjectQuery() {
        return getParentPage().getPrismContext().queryFor(AssignmentType.class)
                .exists(AssignmentType.F_POLICY_RULE)
                .build();
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

    @Override
    protected ItemVisibility getTypedContainerVisibility(ItemWrapper<?, ?, ?, ?> wrapper) {
        if (QNameUtil.match(ConstructionType.COMPLEX_TYPE, wrapper.getTypeName())){
            return ItemVisibility.HIDDEN;
        }

        if (QNameUtil.match(PersonaConstructionType.COMPLEX_TYPE, wrapper.getTypeName())){
            return ItemVisibility.HIDDEN;
        }

        if (QNameUtil.match(AssignmentType.F_ORG_REF, wrapper.getItemName())){
            return ItemVisibility.HIDDEN;
        }

        if (QNameUtil.match(AssignmentType.F_TARGET_REF, wrapper.getItemName())){
            return ItemVisibility.HIDDEN;
        }

        if (QNameUtil.match(AssignmentType.F_TENANT_REF, wrapper.getItemName())){
            return ItemVisibility.HIDDEN;
        }
        return ItemVisibility.AUTO;
    }
}
