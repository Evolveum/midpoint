/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.mappingContainer.outbound;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.mappingContainer.inbound.BasicAssociationInboundStepPanel;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

/**
 * @author lskublik
 */

@Experimental
public class AssociationOutboundBasicWizardPanel extends AbstractWizardPanel<MappingType, ResourceDetailsModel> {

    public AssociationOutboundBasicWizardPanel(String id, WizardPanelHelper<MappingType, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createWizardFragment(new WizardPanel(
                getIdOfWizardPanel(),
                new WizardModel(createBasicSteps()))));
    }

    private List<WizardStep> createBasicSteps() {
        List<WizardStep> steps = new ArrayList<>();

        steps.add(new BasicAssociationOutboundStepPanel(getAssignmentHolderModel(), getHelper().getValueModel()) {

            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
                OperationResult result = AssociationOutboundBasicWizardPanel.this.onSavePerformed(target);
                if (result == null || result.isError()) {
                    target.add(getFeedback());
                    refresh(target);
                }
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                AssociationOutboundBasicWizardPanel.this.onExitPerformed(target);
            }
        });

        return steps;
    }
}
