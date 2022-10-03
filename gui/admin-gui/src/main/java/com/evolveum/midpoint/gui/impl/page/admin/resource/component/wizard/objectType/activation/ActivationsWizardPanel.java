/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.activation;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceActivationDefinitionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.result.Toast;
import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

/**
 * @author lskublik
 */

@Experimental
public class ActivationsWizardPanel extends AbstractResourceWizardPanel<ResourceObjectTypeDefinitionType> {

    private final IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel;

    public ActivationsWizardPanel(String id, ResourceDetailsModel model, IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        super(id, model);
        this.valueModel = valueModel;
    }

    protected void initLayout() {
        add(createWizardFragment(new WizardPanel(
                getIdOfWizardPanel(),
                new WizardModel(createActivationsSteps(getValueModel())))));
    }

    public IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> getValueModel() {
        return valueModel;
    }

    private List<WizardStep> createActivationsSteps(IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        List<WizardStep> steps = new ArrayList<>();
        AdministrativeStatusStepPanel adminPanel = new AdministrativeStatusStepPanel(
                getResourceModel(),
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
                getResourceModel(),
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
                getResourceModel(),
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
                getResourceModel(),
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
                getResourceModel(),
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
                if (!isSavedAfterWizard()) {
                    onExitPerformed(target);
                    return;
                }
                OperationResult result = ActivationsWizardPanel.this.onSaveResourcePerformed(target);
                if (result != null && !result.isError()) {
                    new Toast()
                            .success()
                            .title(getString("ResourceWizardPanel.updateResource"))
                            .icon("fas fa-circle-check")
                            .autohide(true)
                            .delay(5_000)
                            .body(getString("ResourceWizardPanel.updateResource.text")).show(target);
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

    protected boolean isSavedAfterWizard() {
        return true;
    }
}
