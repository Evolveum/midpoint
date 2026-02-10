/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.mappingContainer.help;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.OutboundMappingMainConfigurationStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.OutboundMappingOptionalConfigurationStepPanel;
import com.evolveum.midpoint.prism.Containerable;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModelBasic;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.InboundMappingMainConfigurationStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.InboundMappingOptionalConfigurationStepPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

import org.jetbrains.annotations.NotNull;

public class AssociationMappingWizardPanel<C extends Containerable> extends AbstractWizardPanel<C, ResourceDetailsModel> {

    public AssociationMappingWizardPanel(
            String id,
            WizardPanelHelper<C, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createChoiceFragment(createTablePanel(MappingDirection.OBJECTS)));
    }

    protected AssociationMappingsTableWizardPanel<C> createTablePanel(
            MappingDirection initialTab) {
        return new AssociationMappingsTableWizardPanel<>(getIdOfChoicePanel(), getHelper(), initialTab) {
            @Override
            protected void inEditInboundAttributeValue(IModel<PrismContainerValueWrapper<MappingType>> value, AjaxRequestTarget target, MappingDirection initialTab) {
                showInboundAttributeMappingWizardFragment(target, value, initialTab);
            }

            @Override
            protected void inEditOutboundAttributeValue(IModel<PrismContainerValueWrapper<MappingType>> value, AjaxRequestTarget target, MappingDirection initialTab) {
                showOutboundAttributeMappingWizardFragment(target, value, initialTab);
            }
        };
    }

    private void showInboundAttributeMappingWizardFragment(
            AjaxRequestTarget target,
            IModel<PrismContainerValueWrapper<MappingType>> valueModel, MappingDirection initialTab) {
        showWizardFragment(
                target,
                new WizardPanel(getIdOfWizardPanel(), new WizardModelBasic(createInboundAttributeMappingSteps(valueModel, initialTab))));
    }

    private List<WizardStep> createInboundAttributeMappingSteps(
            IModel<PrismContainerValueWrapper<MappingType>> valueModel, MappingDirection initialTab) {
        List<WizardStep> steps = new ArrayList<>();
        steps.add(new InboundMappingMainConfigurationStepPanel(getAssignmentHolderModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showTableFragment(target, initialTab);
            }
        });
        steps.add(new InboundMappingOptionalConfigurationStepPanel(getAssignmentHolderModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showTableFragment(target, initialTab);
            }
        });
        return steps;
    }

    private void showOutboundAttributeMappingWizardFragment(
            AjaxRequestTarget target,
            IModel<PrismContainerValueWrapper<MappingType>> valueModel, MappingDirection initialTab) {
        showWizardFragment(
                target,
                new WizardPanel(getIdOfWizardPanel(), new WizardModelBasic(createOutboundAttributeMappingSteps(valueModel, initialTab))));
    }

    private @NotNull List<WizardStep> createOutboundAttributeMappingSteps(
            IModel<PrismContainerValueWrapper<MappingType>> valueModel, MappingDirection initialTab) {
        List<WizardStep> steps = new ArrayList<>();
        steps.add(new OutboundMappingMainConfigurationStepPanel<>(getAssignmentHolderModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showTableFragment(target, initialTab);
            }
        });
        steps.add(new OutboundMappingOptionalConfigurationStepPanel<>(getAssignmentHolderModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showTableFragment(target, initialTab);
            }
        });
        return steps;
    }

    private void showTableFragment(AjaxRequestTarget target, MappingDirection initialTab) {
        showChoiceFragment(target, createTablePanel(initialTab));
    }

}
