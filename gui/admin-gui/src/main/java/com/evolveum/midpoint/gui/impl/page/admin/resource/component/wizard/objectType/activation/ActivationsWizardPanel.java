/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.activation;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.attributeMapping.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractPredefinedActivationMappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceActivationDefinitionType;

import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lskublik
 */

public class ActivationsWizardPanel extends AbstractWizardPanel<ResourceObjectTypeDefinitionType, ResourceDetailsModel> {

    public ActivationsWizardPanel(String id, WizardPanelHelper<ResourceObjectTypeDefinitionType, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createChoiceFragment(createActivationTablePanel(MappingDirection.INBOUND)));
    }

    private ActivationMappingWizardPanel createActivationTablePanel(MappingDirection mappingDirection) {
        PrismContainerWrapperModel<ResourceObjectTypeDefinitionType, ResourceActivationDefinitionType> containerModel =
                PrismContainerWrapperModel.fromContainerValueWrapper(
                        getValueModel(),
                        ResourceObjectTypeDefinitionType.F_ACTIVATION);

        return new ActivationMappingWizardPanel(getIdOfChoicePanel(), getAssignmentHolderModel(), containerModel, mappingDirection) {
            @Override
            protected void editOutboundMapping(IModel<PrismContainerValueWrapper<MappingType>> valueModel, AjaxRequestTarget target) {
                showOutboundAttributeMappingWizardFragment(target, valueModel);
            }

            @Override
            protected void editInboundMapping(IModel<PrismContainerValueWrapper<MappingType>> valueModel, AjaxRequestTarget target) {
                showInboundAttributeMappingWizardFragment(target, valueModel);
            }

            @Override
            protected void editPredefinedMapping(
                    IModel<PrismContainerValueWrapper<AbstractPredefinedActivationMappingType>> valueModel,
                    AjaxRequestTarget target,
                    MappingDirection direction) {
                showPredefinedMappingFragment(target, valueModel, direction);
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                ActivationsWizardPanel.this.onExitPerformed(target);
            }

            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
                getHelper().onSaveObjectPerformed(target);
                onExitPerformed(target);
            }
        };
    }

    private void showPredefinedMappingFragment(
            AjaxRequestTarget target,
            IModel<PrismContainerValueWrapper<AbstractPredefinedActivationMappingType>> valueModel,
            MappingDirection direction) {
        showWizardFragment(
                target,
                new WizardPanel(getIdOfWizardPanel(), new WizardModel(createPredefinedMappingSteps(valueModel, direction))));
    }

    private List<WizardStep> createPredefinedMappingSteps(
            IModel<PrismContainerValueWrapper<AbstractPredefinedActivationMappingType>> valueModel,
            MappingDirection direction) {
        List<WizardStep> steps = new ArrayList<>();
        steps.add(new PredefinedMappingStepPanel(getAssignmentHolderModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showActivationTablePanel(target, direction);
            }
        });
        return steps;
    }

    private void showActivationTablePanel(AjaxRequestTarget target, MappingDirection mappingDirection) {
        showChoiceFragment(target, createActivationTablePanel(mappingDirection));
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
        steps.add(new InboundActivationMappingMainConfigurationStepPanel(getAssignmentHolderModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showActivationTablePanel(target, MappingDirection.INBOUND);
            }
        });
        steps.add(new InboundActivationMappingOptionalConfigurationStepPanel(getAssignmentHolderModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showActivationTablePanel(target, MappingDirection.INBOUND);
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
        steps.add(new OutboundActivationMappingMainConfigurationStepPanel(getAssignmentHolderModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showActivationTablePanel(target, MappingDirection.OUTBOUND);
            }
        });
        steps.add(new OutboundActivationMappingOptionalConfigurationStepPanel(getAssignmentHolderModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showActivationTablePanel(target, MappingDirection.OUTBOUND);
            }
        });
        return steps;
    }
}
