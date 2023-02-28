/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.basic;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lskublik
 */

@Experimental
public class ResourceObjectTypeBasicWizardPanel extends AbstractWizardPanel<ResourceObjectTypeDefinitionType, ResourceDetailsModel> {

    public ResourceObjectTypeBasicWizardPanel(String id, WizardPanelHelper<ResourceObjectTypeDefinitionType, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createWizardFragment(new WizardPanel(
                getIdOfWizardPanel(),
                new WizardModel(createBasicSteps(getValueModel())))));
    }

    private List<WizardStep> createBasicSteps(IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        List<WizardStep> steps = new ArrayList<>();

        steps.add(new BasicSettingResourceObjectTypeStepPanel(getAssignmentHolderModel(), getValueModel()) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                ResourceObjectTypeBasicWizardPanel.this.onExitPerformed(target);
            }
        });

        steps.add(new DelineationResourceObjectTypeStepPanel(getAssignmentHolderModel(), getValueModel()) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                ResourceObjectTypeBasicWizardPanel.this.onExitPerformed(target);
            }
        });

        steps.add(new FocusResourceObjectTypeStepPanel(getAssignmentHolderModel(), getValueModel()) {

            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
                OperationResult result = ResourceObjectTypeBasicWizardPanel.this.onSavePerformed(target);
                if (result == null || result.isError()) {
                    target.add(getFeedback());
                }
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                ResourceObjectTypeBasicWizardPanel.this.onExitPerformed(target);
            }
        });

        return steps;
    }
}
