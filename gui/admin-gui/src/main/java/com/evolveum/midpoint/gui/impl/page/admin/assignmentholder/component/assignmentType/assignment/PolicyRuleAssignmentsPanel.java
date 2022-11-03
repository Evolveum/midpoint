/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.assignment;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismContainerWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.AssignmentHolderAssignmentPanel;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@PanelType(name = "policyRuleAssignments")
@PanelInstance(identifier = "policyRuleAssignments",
        applicableForType = AbstractRoleType.class,
        childOf = AssignmentHolderAssignmentPanel.class,
        display = @PanelDisplay(label = "AssignmentType.policyRule", icon = GuiStyleConstants.CLASS_POLICY_RULES_ICON, order = 60))
public class PolicyRuleAssignmentsPanel<AH extends AssignmentHolderType> extends AbstractAssignmentPanel<AH> {

    private static final Trace LOGGER = TraceManager.getTrace(PolicyRuleAssignmentsPanel.class);

    public PolicyRuleAssignmentsPanel(String id, IModel<PrismObjectWrapper<AH>> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> initColumns() {
        List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> columns = new ArrayList<>();


        columns.add(new PrismContainerWrapperColumn<>(getContainerModel(), ItemPath.create(AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_CONSTRAINTS), getPageBase()));

        columns.add(new PrismPropertyWrapperColumn<>(getContainerModel(), ItemPath.create(AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_SITUATION), AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()));

        columns.add(new PrismContainerWrapperColumn<>(getContainerModel(), ItemPath.create(AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_ACTIONS), getPageBase()));

        columns.add(new PrismPropertyWrapperColumn<>(getContainerModel(), AssignmentType.F_ORDER, AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()));

        return columns;
    }

    @Override
    protected QName getAssignmentType() {
        return PolicyRuleType.COMPLEX_TYPE;
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.POLICY_RULES_TAB_TABLE;
    }

    @Override
    protected void newAssignmentClickPerformed(AjaxRequestTarget target) {
        PrismContainerValue<AssignmentType> newAssignment = getContainerModel().getObject().getItem().createNewValue();
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
        PrismContainerValueWrapper<AssignmentType> newAssignmentWrapper = createNewItemContainerValueWrapper(newAssignment, getContainerModel().getObject(), target);
        itemDetailsPerformed(target, Collections.singletonList(newAssignmentWrapper));
    }


    protected ObjectQuery createCustomizeQuery() {
        return getPageBase().getPrismContext().queryFor(AssignmentType.class)
                .exists(AssignmentType.F_POLICY_RULE).build();
    }

//    @Override
//    protected List<SearchItemDefinition> createSearchableItems(PrismContainerDefinition<AssignmentType> containerDef) {
//        List<SearchItemDefinition> defs = new ArrayList<>();
//
//        SearchFactory.addSearchPropertyDef(containerDef, ItemPath.create(AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS), defs);
//        SearchFactory.addSearchPropertyDef(containerDef, ItemPath.create(AssignmentType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS), defs);
//        SearchFactory.addSearchPropertyDef(containerDef, ItemPath.create(AssignmentType.F_POLICY_RULE, PolicyRuleType.F_NAME), defs, "AssignmentPanel.search.policyRule.name");
//        SearchFactory.addSearchRefDef(containerDef,
//                ItemPath.create(AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_CONSTRAINTS,
//                        PolicyConstraintsType.F_EXCLUSION, ExclusionPolicyConstraintType.F_TARGET_REF), defs, AreaCategoryType.POLICY, getPageBase());
//
//        defs.addAll(SearchFactory.createExtensionDefinitionList(containerDef));
//
//        return defs;
//    }

    @Override
    protected ObjectQuery getCustomizeQuery() {
        // CustomizeQuery is not repo indexed
        if (isRepositorySearchEnabled()) {
            return null;
        }
        return createCustomizeQuery();
    }

    @Override
    protected List<PrismContainerValueWrapper<AssignmentType>> customPostSearch(
            List<PrismContainerValueWrapper<AssignmentType>> list) {
        // customizeQuery is not repository supported, so we need to prefilter list using in-memory search
        if (isRepositorySearchEnabled()) {
            return prefilterUsingQuery(list, createCustomizeQuery());
        }
        return super.customPostSearch(list);
    }
}
