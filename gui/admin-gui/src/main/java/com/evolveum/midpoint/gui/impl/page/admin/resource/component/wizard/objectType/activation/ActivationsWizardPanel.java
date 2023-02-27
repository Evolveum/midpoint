/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.activation;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceActivationDefinitionType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

/**
 * @author lskublik
 */

@Experimental
public class ActivationsWizardPanel extends AbstractWizardPanel<ResourceObjectTypeDefinitionType, ResourceDetailsModel> {

    public ActivationsWizardPanel(String id, WizardPanelHelper<ResourceObjectTypeDefinitionType, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createWizardFragment(new WizardPanel(
                getIdOfWizardPanel(),
                new WizardModel(createActivationsSteps(getValueModel())))));
    }

    private List<WizardStep> createActivationsSteps(IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        List<WizardStep> steps = new ArrayList<>();
        AdministrativeStatusStepPanel adminPanel = new AdministrativeStatusStepPanel(
                getAssignmentHolderModel(),
                PrismContainerWrapperModel.fromContainerValueWrapper(
                        valueModel,
                        ItemPath.create(
                                ResourceObjectTypeDefinitionType.F_ACTIVATION,
                                ResourceActivationDefinitionType.F_ADMINISTRATIVE_STATUS))) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                ActivationsWizardPanel.this.onExitPerformed(target);
            }
        };
        adminPanel.setOutputMarkupId(true);
        steps.add(adminPanel);

        ExistenceStepPanel existencePanel = new ExistenceStepPanel(
                getAssignmentHolderModel(),
                PrismContainerWrapperModel.fromContainerValueWrapper(
                        valueModel,
                        ItemPath.create(
                                ResourceObjectTypeDefinitionType.F_ACTIVATION,
                                ResourceActivationDefinitionType.F_EXISTENCE))) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                ActivationsWizardPanel.this.onExitPerformed(target);
            }
        };
        existencePanel.setOutputMarkupId(true);
        steps.add(existencePanel);

        ValidFromStepPanel validFromPanel = new ValidFromStepPanel(
                getAssignmentHolderModel(),
                PrismContainerWrapperModel.fromContainerValueWrapper(
                        valueModel,
                        ItemPath.create(
                                ResourceObjectTypeDefinitionType.F_ACTIVATION,
                                ResourceActivationDefinitionType.F_VALID_FROM))) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                ActivationsWizardPanel.this.onExitPerformed(target);
            }
        };
        validFromPanel.setOutputMarkupId(true);
        steps.add(validFromPanel);

        ValidToStepPanel validToPanel = new ValidToStepPanel(
                getAssignmentHolderModel(),
                PrismContainerWrapperModel.fromContainerValueWrapper(
                        valueModel,
                        ItemPath.create(
                                ResourceObjectTypeDefinitionType.F_ACTIVATION,
                                ResourceActivationDefinitionType.F_VALID_TO))) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                ActivationsWizardPanel.this.onExitPerformed(target);
            }
        };
        validToPanel.setOutputMarkupId(true);
        steps.add(validToPanel);

        LockoutStatusStepPanel lockPanel = new LockoutStatusStepPanel(
                getAssignmentHolderModel(),
                PrismContainerWrapperModel.fromContainerValueWrapper(
                        valueModel,
                        ItemPath.create(
                                ResourceObjectTypeDefinitionType.F_ACTIVATION,
                                ResourceActivationDefinitionType.F_LOCKOUT_STATUS))) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                ActivationsWizardPanel.this.onExitPerformed(target);
            }

            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
//                if (!isSavedAfterWizard()) {
//                    onExitPerformed(target);
//                    return;
//                }
                OperationResult result = ActivationsWizardPanel.this.onSavePerformed(target);
                if (result != null && !result.isError()) {
//                    WebComponentUtil.createToastForUpdateObject(target, ResourceType.COMPLEX_TYPE);
                    onExitPerformed(target);
                }
            }

            @Override
            protected IModel<String> getSubmitLabelModel() {
                if (isSavedAfterWizard()) {
                    return super.getSubmitLabelModel();
                }
                return getPageBase().createStringResource("WizardPanel.confirm");
            }
        };
        lockPanel.setOutputMarkupId(true);
        steps.add(lockPanel);

        return steps;
    }
}
