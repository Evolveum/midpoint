/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.credentials;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lskublik
 */

@Experimental
public class CredentialsWizardPanel extends AbstractWizardPanel<ResourceObjectTypeDefinitionType, ResourceDetailsModel> {

    public CredentialsWizardPanel(String id, WizardPanelHelper<ResourceObjectTypeDefinitionType, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createChoiceFragment(createCredentialsTablePanel(MappingDirection.INBOUND)));
    }

    private CredentialsMappingWizardPanel createCredentialsTablePanel(MappingDirection mappingDirection) {
        PrismContainerWrapperModel<ResourceObjectTypeDefinitionType, ResourceCredentialsDefinitionType> containerModel =
                PrismContainerWrapperModel.fromContainerValueWrapper(
                        getValueModel(),
                        ResourceObjectTypeDefinitionType.F_CREDENTIALS);

        return new CredentialsMappingWizardPanel(getIdOfChoicePanel(), getAssignmentHolderModel(), containerModel, mappingDirection) {
            @Override
            protected void editOutboundMapping(IModel<PrismContainerValueWrapper<MappingType>> valueModel, AjaxRequestTarget target) {
                showOutboundAttributeMappingWizardFragment(target, valueModel);
            }

            @Override
            protected void editInboundMapping(IModel<PrismContainerValueWrapper<MappingType>> valueModel, AjaxRequestTarget target) {
                showInboundAttributeMappingWizardFragment(target, valueModel);
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                CredentialsWizardPanel.this.onExitPerformed(target);
            }

            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
                getHelper().onSaveObjectPerformed(target);
                onExitPerformed(target);
            }
        };
    }

    private void showCredentialsTablePanel(AjaxRequestTarget target, MappingDirection mappingDirection) {
        showChoiceFragment(target, createCredentialsTablePanel(mappingDirection));
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
        steps.add(new InboundCredentialsMappingMainConfigurationStepPanel(getAssignmentHolderModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                WebComponentUtil.showToastForRecordedButUnsavedChanges(target, valueModel.getObject());
                showCredentialsTablePanel(target, MappingDirection.INBOUND);
            }
        });
        steps.add(new InboundCredentialsMappingOptionalConfigurationStepPanel(getAssignmentHolderModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                WebComponentUtil.showToastForRecordedButUnsavedChanges(target, valueModel.getObject());
                showCredentialsTablePanel(target, MappingDirection.INBOUND);
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
        steps.add(new OutboundCredentialsMappingMainConfigurationStepPanel(getAssignmentHolderModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                WebComponentUtil.showToastForRecordedButUnsavedChanges(target, valueModel.getObject());
                showCredentialsTablePanel(target, MappingDirection.OUTBOUND);
            }
        });
        steps.add(new OutboundCredentialsMappingOptionalConfigurationStepPanel(getAssignmentHolderModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                WebComponentUtil.showToastForRecordedButUnsavedChanges(target, valueModel.getObject());
                showCredentialsTablePanel(target, MappingDirection.OUTBOUND);
            }
        });
        return steps;
    }
}
