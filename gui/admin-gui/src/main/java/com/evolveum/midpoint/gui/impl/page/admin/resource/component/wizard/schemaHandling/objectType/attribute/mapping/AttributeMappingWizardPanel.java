/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.volatilityMultivalue.AttributeTypeWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.LimitationsStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.MainConfigurationStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.MappingOverridesTableWizardPanel;
import com.evolveum.midpoint.prism.Containerable;

import com.evolveum.midpoint.prism.path.ItemName;

import com.evolveum.midpoint.schema.result.OperationResult;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

/**
 * @author lskublik
 */
public class AttributeMappingWizardPanel<C extends Containerable> extends AbstractWizardPanel<C, ResourceDetailsModel> {

    public AttributeMappingWizardPanel(
            String id,
            WizardPanelHelper<C, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createChoiceFragment(createTablePanel(MappingDirection.INBOUND)));
    }

    protected AttributeMappingsTableWizardPanel<C> createTablePanel(
            MappingDirection initialTab) {
        return new AttributeMappingsTableWizardPanel<>(getIdOfChoicePanel(), getHelper(), initialTab) {

            @Override
            protected void inEditOutboundValue(IModel<PrismContainerValueWrapper<MappingType>> value, AjaxRequestTarget target) {
                showOutboundAttributeMappingWizardFragment(target, value);
            }

            @Override
            protected void inEditInboundValue(IModel<PrismContainerValueWrapper<MappingType>> value, AjaxRequestTarget target) {
                showInboundAttributeMappingWizardFragment(target, value);
            }

            @Override
            protected ItemName getItemNameOfContainerWithMappings() {
                return AttributeMappingWizardPanel.this.getItemNameOfContainerWithMappings();
            }

            @Override
            protected void onShowOverrides(AjaxRequestTarget target, MappingDirection selectedTable) {
                showAttributeOverrides(target, selectedTable);
            }
        };
    }

    private void showAttributeOverrides(AjaxRequestTarget target, MappingDirection selectedTable) {
        showChoiceFragment(
                target,
                new MappingOverridesTableWizardPanel<>(getIdOfChoicePanel(), getHelper()) {
                    protected void onExitPerformedAfterValidate(AjaxRequestTarget target) {
                        removeLastBreadcrumb();
                        showTableFragment(target, selectedTable);
                    }

                    @Override
                    protected ItemName getItemNameOfContainerWithMappings() {
                        return AttributeMappingWizardPanel.this.getItemNameOfContainerWithMappings();
                    }

                    @Override
                    protected void inEditNewValue(IModel<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> value, AjaxRequestTarget target) {
//                        TODO uncomment if volatility incoming/outgoing container will be changed to multivalue containers
//                        WizardPanelHelper<ResourceAttributeDefinitionType, ResourceDetailsModel> helper =
//                                new WizardPanelHelper<>(getAssignmentHolderModel()) {
//
//                                    @Override
//                                    public void onExitPerformed(AjaxRequestTarget target) {
//                                        showAttributeOverrides(target, selectedTable);
//                                    }
//
//                                    @Override
//                                    public OperationResult onSaveObjectPerformed(AjaxRequestTarget target) {
//                                        return getHelper().onSaveObjectPerformed(target);
//                                    }
//
//                                    @Override
//                                    public IModel<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> getDefaultValueModel() {
//                                        return value;
//                                    }
//                                };
//                        showWizardFragment(
//                                target,
//                                new AttributeTypeWizardPanel(getIdOfWizardPanel(), helper));

                        showWizardFragment(
                                target,
                                new WizardPanel(getIdOfWizardPanel(), new WizardModel(createNewAttributeOverrideSteps(value, selectedTable))));
                    }
                });
    }

    protected ItemName getItemNameOfContainerWithMappings() {
        return ResourceObjectTypeDefinitionType.F_ATTRIBUTE;
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
        steps.add(new InboundMappingMainConfigurationStepPanel(getAssignmentHolderModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showTableFragment(target, MappingDirection.INBOUND);
            }
        });
        steps.add(new InboundMappingOptionalConfigurationStepPanel(getAssignmentHolderModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showTableFragment(target, MappingDirection.INBOUND);
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
        steps.add(new OutboundMappingMainConfigurationStepPanel<>(getAssignmentHolderModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showTableFragment(target, MappingDirection.OUTBOUND);
            }
        });
        steps.add(new OutboundMappingOptionalConfigurationStepPanel<>(getAssignmentHolderModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showTableFragment(target, MappingDirection.OUTBOUND);
            }
        });
        return steps;
    }

    private List<WizardStep> createNewAttributeOverrideSteps(
            IModel<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> valueModel, MappingDirection selectedTable) {
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

    private void showTableFragment(AjaxRequestTarget target, MappingDirection initialTab) {
        showChoiceFragment(target, createTablePanel(initialTab));
    }
}
