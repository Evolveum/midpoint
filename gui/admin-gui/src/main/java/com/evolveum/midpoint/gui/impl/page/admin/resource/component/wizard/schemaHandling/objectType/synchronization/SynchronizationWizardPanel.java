/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.synchronization;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractSynchronizationReactionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationReactionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lskublik
 */
public class SynchronizationWizardPanel<C extends AbstractSynchronizationReactionType, P extends Containerable> extends AbstractWizardPanel<P, ResourceDetailsModel> {

    public SynchronizationWizardPanel(String id, WizardPanelHelper<P, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createChoiceFragment(createTablePanel()));
    }

    protected SynchronizationReactionTableWizardPanel<C, P> createTablePanel() {
        return new SynchronizationReactionTableWizardPanel<>(getIdOfChoicePanel(), getHelper()) {
            @Override
            protected void inEditNewValue(IModel<PrismContainerValueWrapper<C>> value, AjaxRequestTarget target) {
                showWizardFragment(target, new WizardPanel(
                        getIdOfWizardPanel(),
                        new WizardModel(createSynchronizationConfigSteps(value))));
            }
        };
    }

    private List<WizardStep> createSynchronizationConfigSteps(IModel<PrismContainerValueWrapper<C>> valueModel) {
        List<WizardStep> steps = new ArrayList<>();
        steps.add(new ReactionMainSettingStepPanel<>(getAssignmentHolderModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showChoiceFragment(target, createTablePanel());
            }
        });

        steps.add(new ActionStepPanel(
                getAssignmentHolderModel(),
                PrismContainerWrapperModel.fromContainerValueWrapper(valueModel, SynchronizationReactionType.F_ACTIONS)) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showChoiceFragment(target, createTablePanel());
            }
        });

        steps.add(new ReactionOptionalSettingStepPanel<>(getAssignmentHolderModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showChoiceFragment(target, createTablePanel());
            }
        });

        return steps;
    }
}
