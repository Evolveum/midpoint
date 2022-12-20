/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.attributeMapping;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

/**
 * @author lskublik
 */
public class AttributeMappingWizardPanel extends AbstractWizardPanel<ResourceObjectTypeDefinitionType, ResourceDetailsModel> {

    public AttributeMappingWizardPanel(
            String id,
            WizardPanelHelper<ResourceObjectTypeDefinitionType, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createChoiceFragment(createTablePanel(WrapperContext.AttributeMappingType.INBOUND)));
    }

    protected AttributeMappingsTableWizardPanel createTablePanel(WrapperContext.AttributeMappingType initialTab) {
        return new AttributeMappingsTableWizardPanel(getIdOfChoicePanel(), getHelper(), initialTab) {

            @Override
            protected void inEditOutboundValue(IModel<PrismContainerValueWrapper<MappingType>> value, AjaxRequestTarget target) {
                showOutboundAttributeMappingWizardFragment(target, value);
            }

            @Override
            protected void inEditInboundValue(IModel<PrismContainerValueWrapper<MappingType>> value, AjaxRequestTarget target) {
                showInboundAttributeMappingWizardFragment(target, value);
            }

            @Override
            protected void onShowOverrides(AjaxRequestTarget target, WrapperContext.AttributeMappingType selectedTable) {
                showAttributeOverrides(target, selectedTable);
            }
        };
    }

    private void showAttributeOverrides(AjaxRequestTarget target, WrapperContext.AttributeMappingType selectedTable) {
        showChoiceFragment(
                target,
                new MappingOverridesTableWizardPanel(getIdOfChoicePanel(), getHelper()) {
                    protected void onExitPerformedAfterValidate(AjaxRequestTarget target) {
                        removeLastBreadcrumb();
                        showTableFragment(target, selectedTable);
                    }

                    @Override
                    protected void inEditNewValue(IModel<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> value, AjaxRequestTarget target) {
                        showWizardFragment(
                                target,
                                new WizardPanel(getIdOfWizardPanel(), new WizardModel(createNewAttributeOverrideSteps(value, selectedTable))));
                    }
                });
    }

    private void showInboundAttributeMappingWizardFragment(
            AjaxRequestTarget target,
            IModel<PrismContainerValueWrapper<MappingType>> valueModel) {
        showWizardFragment(
                target,
                new WizardPanel(getIdOfWizardPanel(), new WizardModel(createInboundAttributeMappingSteps(valueModel))));
    }

    private List<WizardStep> createInboundAttributeMappingSteps(IModel<PrismContainerValueWrapper<MappingType>> valueModel) {
        List<WizardStep> steps = new ArrayList<>();
        steps.add(new AttributeInboundStepPanel(getAssignmentHolderModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showTableFragment(target, WrapperContext.AttributeMappingType.INBOUND);
            }
        });
        return steps;
    }

    private void showOutboundAttributeMappingWizardFragment(
            AjaxRequestTarget target,
            IModel<PrismContainerValueWrapper<MappingType>> valueModel) {
        showWizardFragment(
                target,
                new WizardPanel(getIdOfWizardPanel(), new WizardModel(createOutboundAttributeMappingSteps(valueModel))));
    }

    private List<WizardStep> createOutboundAttributeMappingSteps(IModel<PrismContainerValueWrapper<MappingType>> valueModel) {
        List<WizardStep> steps = new ArrayList<>();
        steps.add(new AttributeOutboundStepPanel(getAssignmentHolderModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showTableFragment(target, WrapperContext.AttributeMappingType.OUTBOUND);
            }
        });
        return steps;
    }

    private List<WizardStep> createNewAttributeOverrideSteps(
            IModel<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> valueModel, WrapperContext.AttributeMappingType selectedTable) {
        List<WizardStep> steps = new ArrayList<>();
        steps.add(new MainConfigurationStepPanel(getAssignmentHolderModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showAttributeOverrides(target, selectedTable);
            }
        });

        steps.add(new LimitationsStepPanel(getAssignmentHolderModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showAttributeOverrides(target, selectedTable);
            }
        });
        return steps;
    }

    private void showTableFragment(AjaxRequestTarget target, WrapperContext.AttributeMappingType initialTab) {
        showChoiceFragment(target, createTablePanel(initialTab));
    }
}
