/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.inducement;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component.AbstractRoleInducementPanel;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

@PanelType(name = "policyRuleInducements")
@PanelInstance(identifier = "policyRuleInducements",
        applicableForType = AbstractRoleType.class,
        childOf = AbstractRoleInducementPanel.class,
        display = @PanelDisplay(label = "AssignmentType.policyRule", icon = GuiStyleConstants.CLASS_POLICY_RULES_ICON, order = 60))
public class PolicyRuleInducementsPanel<AR extends AbstractRoleType> extends AbstractInducementPanel<AR> {

    private static final Trace LOGGER = TraceManager.getTrace(PolicyRuleInducementsPanel.class);

    public PolicyRuleInducementsPanel(String id, IModel<PrismObjectWrapper<AR>> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    protected ObjectQuery createCustomizeQuery() {
        return getPageBase().getPrismContext().queryFor(AssignmentType.class)
                .exists(AssignmentType.F_POLICY_RULE).build();
    }

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

    @Override
    protected boolean hasTargetObject() {
        return false;
    }

    @Override
    protected void initializeNewAssignmentData(PrismContainerValue<AssignmentType> newAssignmentValue,
            AssignmentType assignmentObject, AjaxRequestTarget target) {
        try {
            newAssignmentValue.findOrCreateContainer(AssignmentType.F_POLICY_RULE);
            assignmentObject.setPolicyRule(new PolicyRuleType());
        } catch (SchemaException e) {
            LOGGER.error("Cannot create policy rule inducement: {}", e.getMessage(), e);
            getSession().error("Cannot create policyRule inducement.");
            target.add(getPageBase().getFeedbackPanel());
        }
    }

}
