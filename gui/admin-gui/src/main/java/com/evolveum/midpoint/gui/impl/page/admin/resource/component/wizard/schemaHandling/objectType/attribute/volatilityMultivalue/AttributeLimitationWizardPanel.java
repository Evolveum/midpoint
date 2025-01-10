/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.volatilityMultivalue;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.LimitationsStepPanel;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;

import org.apache.wicket.ajax.AjaxRequestTarget;

import java.util.ArrayList;
import java.util.List;

/**
 * Wizard panel for attribute limitation configuration step.
 * Now not use but prepare when incoming and outgoing attributes of attribute's volatility change to multivalue containers.
 *
 * @author lskublik
 */

@Experimental
public class AttributeLimitationWizardPanel extends AbstractWizardPanel<ResourceAttributeDefinitionType, ResourceDetailsModel> {

    public AttributeLimitationWizardPanel(String id, WizardPanelHelper<ResourceAttributeDefinitionType, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createWizardFragment(new WizardPanel(
                getIdOfWizardPanel(),
                new WizardModel(createNewAttributeOverrideSteps()))));
    }

    private List<WizardStep> createNewAttributeOverrideSteps() {
        List<WizardStep> steps = new ArrayList<>();
        steps.add(new LimitationsStepPanel(getAssignmentHolderModel(), getValueModel()) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                AttributeLimitationWizardPanel.this.onExitPerformed(target);
            }

            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
                OperationResult result = AttributeLimitationWizardPanel.this.onSavePerformed(target);
                if (result == null || result.isError()) {
                    target.add(getFeedback());
                    refresh(target);
                } else {
                    onExitPerformed(target);
                }
            }
        });
        return steps;
    }
}
