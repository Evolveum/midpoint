/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.associations;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.ResourceWizardPanelHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectAssociationType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardPanel;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

/**
 * @author lskublik
 */

@Experimental
public class AssociationsWizardPanel extends AbstractResourceWizardPanel<ResourceObjectTypeDefinitionType> {

    public AssociationsWizardPanel(String id, ResourceWizardPanelHelper<ResourceObjectTypeDefinitionType> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createChoiceFragment(createTablePanel()));
    }

    protected AssociationsTableWizardPanel createTablePanel() {
        return new AssociationsTableWizardPanel(getIdOfChoicePanel(), getHelper()) {
            @Override
            protected void inEditNewValue(IModel<PrismContainerValueWrapper<ResourceObjectAssociationType>> value, AjaxRequestTarget target) {
                showWizardFragment(target, new WizardPanel(
                        getIdOfWizardPanel(),
                        new WizardModel(createAssociationsSteps(value))));
            }
        };
    }

    private List<WizardStep> createAssociationsSteps(IModel<PrismContainerValueWrapper<ResourceObjectAssociationType>> valueModel) {
        List<WizardStep> steps = new ArrayList<>();
        AssociationStepPanel panel = new AssociationStepPanel(
                getResourceModel(),
                valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showChoiceFragment(target, createTablePanel());
            }
        };
        panel.setOutputMarkupId(true);
        steps.add(panel);

        return steps;
    }
}
