/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.mappingContainer.outbound.mapping;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.OutboundMappingMainConfigurationStepPanel;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.OutboundMappingOptionalConfigurationStepPanel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssociationConstructionExpressionEvaluatorType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

/**
 * @author lskublik
 */
public class AssociationOutboundMappingWizardPanel extends AbstractWizardPanel<AssociationConstructionExpressionEvaluatorType, ResourceDetailsModel> {

    public AssociationOutboundMappingWizardPanel(
            String id,
            WizardPanelHelper<AssociationConstructionExpressionEvaluatorType, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createChoiceFragment(createTablePanel(MappingDirection.OBJECTS)));
    }

    protected OutboundMappingsTableWizardPanel createTablePanel(
            MappingDirection initialTab) {
        return new OutboundMappingsTableWizardPanel(getIdOfChoicePanel(), getHelper(), initialTab) {

            @Override
            protected void inEditAttributeValue(IModel<PrismContainerValueWrapper<MappingType>> value, AjaxRequestTarget target, MappingDirection initialTab) {
                showAttributeMappingWizardFragment(target, value, initialTab);
            }
        };
    }


    private void showAttributeMappingWizardFragment(
            AjaxRequestTarget target,
            IModel<PrismContainerValueWrapper<MappingType>> valueModel, MappingDirection initialTab) {
        showWizardFragment(
                target,
                new WizardPanel(getIdOfWizardPanel(), new WizardModel(createInboundAttributeMappingSteps(valueModel, initialTab))));
    }

    private List<WizardStep> createInboundAttributeMappingSteps(
            IModel<PrismContainerValueWrapper<MappingType>> valueModel, MappingDirection initialTab) {
        List<WizardStep> steps = new ArrayList<>();
        steps.add(new OutboundMappingMainConfigurationStepPanel(getAssignmentHolderModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showTableFragment(target, initialTab);
            }
        });
        steps.add(new OutboundMappingOptionalConfigurationStepPanel(getAssignmentHolderModel(), valueModel) {
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
