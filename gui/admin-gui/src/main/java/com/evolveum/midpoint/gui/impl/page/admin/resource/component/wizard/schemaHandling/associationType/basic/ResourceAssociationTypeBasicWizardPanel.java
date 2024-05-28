/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.basic;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationTypeDefinitionType;

import org.apache.wicket.ajax.AjaxRequestTarget;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;

/**
 * @author lskublik
 */

@Experimental
public class ResourceAssociationTypeBasicWizardPanel extends AbstractWizardPanel<ShadowAssociationTypeDefinitionType, ResourceDetailsModel> {

    public ResourceAssociationTypeBasicWizardPanel(String id, WizardPanelHelper<ShadowAssociationTypeDefinitionType, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createWizardFragment(new WizardPanel(
                getIdOfWizardPanel(),
                new WizardModel(createBasicSteps()))));
    }

    private List<WizardStep> createBasicSteps() {
        List<WizardStep> steps = new ArrayList<>();

        steps.add(new BasicSettingResourceAssociationTypeStepPanel(getAssignmentHolderModel(), getValueModel()) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                ResourceAssociationTypeBasicWizardPanel.this.onExitPerformed(target);
            }
        });

        steps.add(new SubjectAssociationStepPanel(getAssignmentHolderModel(), getValueModel()) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                ResourceAssociationTypeBasicWizardPanel.this.onExitPerformed(target);
            }
        });

        steps.add(new ObjectAssociationStepPanel(getAssignmentHolderModel(), getValueModel()) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                ResourceAssociationTypeBasicWizardPanel.this.onExitPerformed(target);
            }
        });

        steps.add(new AssociationDataAssociationTypeStepPanel(getAssignmentHolderModel(), getValueModel()) {

            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
                OperationResult result = ResourceAssociationTypeBasicWizardPanel.this.onSavePerformed(target);
                if (result == null || result.isError()) {
                    target.add(getFeedback());
                    refresh(target);
                } else {
                    onExitPerformed(target);
                }
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                ResourceAssociationTypeBasicWizardPanel.this.onExitPerformed(target);
            }
        });

        return steps;
    }
}
