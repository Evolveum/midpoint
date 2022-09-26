/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.synchronization;

import com.evolveum.midpoint.gui.api.component.result.Toast;
import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardPanel;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationReactionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lskublik
 */
public class SynchronizationWizardPanel extends AbstractResourceWizardPanel<ResourceObjectTypeDefinitionType> {

    private final IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel;

    public SynchronizationWizardPanel(String id, ResourceDetailsModel model, IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        super(id, model);
        this.valueModel = valueModel;
    }

    protected void initLayout() {
        add(createChoiceFragment(createTablePanel()));
    }

    protected SynchronizationReactionTableWizardPanel createTablePanel() {
        SynchronizationReactionTableWizardPanel table
                = new SynchronizationReactionTableWizardPanel(getIdOfChoicePanel(), getResourceModel(), valueModel) {

            @Override
            protected void onSaveResourcePerformed(AjaxRequestTarget target) {
                if (!isSavedAfterWizard()) {
                    onExitPerformed(target);
                    return;
                }
                OperationResult result = SynchronizationWizardPanel.this.onSaveResourcePerformed(target);
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
            protected void inEditNewValue(IModel<PrismContainerValueWrapper<SynchronizationReactionType>> value, AjaxRequestTarget target) {
                showWizardFragment(target, new WizardPanel(
                        getIdOfWizardPanel(),
                        new WizardModel(createSynchronizationConfigSteps(value))));
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                SynchronizationWizardPanel.this.onExitPerformed(target);
            }

            @Override
            protected IModel<String> getSubmitLabelModel() {
                if (isSavedAfterWizard()) {
                    return super.getSubmitLabelModel();
                }
                return getPageBase().createStringResource("WizardPanel.confirm");
            }
        };
        return table;
    }

    public IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> getValueModel() {
        return valueModel;
    }

    private List<WizardStep> createSynchronizationConfigSteps(IModel<PrismContainerValueWrapper<SynchronizationReactionType>> valueModel) {
        List<WizardStep> steps = new ArrayList<>();
        ReactionMainSettingStepPanel settingPanel = new ReactionMainSettingStepPanel(getResourceModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showChoiceFragment(target, createTablePanel());
            }
        };
        settingPanel.setOutputMarkupId(true);
        steps.add(settingPanel);

        steps.add(new ReactionOptionalSettingStepPanel(getResourceModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showChoiceFragment(target, createTablePanel());
            }
        });

        return steps;
    }

    protected boolean isSavedAfterWizard() {
        return true;
    }
}
