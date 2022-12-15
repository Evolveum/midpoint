/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.synchronization;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.ResourceWizardPanelHelper;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
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

    public SynchronizationWizardPanel(String id, ResourceWizardPanelHelper<ResourceObjectTypeDefinitionType> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createChoiceFragment(createTablePanel()));
    }

    protected SynchronizationReactionTableWizardPanel createTablePanel() {
        return new SynchronizationReactionTableWizardPanel(getIdOfChoicePanel(), getHelper()) {
            @Override
            protected void inEditNewValue(IModel<PrismContainerValueWrapper<SynchronizationReactionType>> value, AjaxRequestTarget target) {
                showWizardFragment(target, new WizardPanel(
                        getIdOfWizardPanel(),
                        new WizardModel(createSynchronizationConfigSteps(value))));
            }
        };
    }

    private List<WizardStep> createSynchronizationConfigSteps(IModel<PrismContainerValueWrapper<SynchronizationReactionType>> valueModel) {
        List<WizardStep> steps = new ArrayList<>();
        steps.add(new ReactionMainSettingStepPanel(getResourceModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showChoiceFragment(target, createTablePanel());
            }
        });

        steps.add(new ActionStepPanel(
                getResourceModel(),
                PrismContainerWrapperModel.fromContainerValueWrapper(valueModel, SynchronizationReactionType.F_ACTIONS)) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showChoiceFragment(target, createTablePanel());
            }
        });

        steps.add(new ReactionOptionalSettingStepPanel(getResourceModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showChoiceFragment(target, createTablePanel());
            }
        });

        return steps;
    }
}
