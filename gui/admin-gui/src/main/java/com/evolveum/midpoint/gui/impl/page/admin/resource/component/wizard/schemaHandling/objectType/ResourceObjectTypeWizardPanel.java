/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType;

import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;

import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;

import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardChoicePanelWithSeparatedCreatePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.AttributeMappingWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.basic.ResourceObjectTypeBasicWizardPanel;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.activation.ActivationsWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.capabilities.CapabilitiesWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation.CorrelationWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.credentials.CredentialsWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.policies.PoliciesObjectTypeWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.synchronization.SynchronizationWizardPanel;

import org.apache.wicket.ajax.AjaxRequestTarget;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

/**
 * @author lskublik
 */
public class ResourceObjectTypeWizardPanel extends AbstractWizardChoicePanelWithSeparatedCreatePanel<ResourceObjectTypeDefinitionType> {

    public ResourceObjectTypeWizardPanel(
            String id,
            WizardPanelHelper<ResourceObjectTypeDefinitionType, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    @Override
    protected ResourceObjectTypeBasicWizardPanel createNewTypeWizard(String id, WizardPanelHelper<ResourceObjectTypeDefinitionType, ResourceDetailsModel> helper) {
        return new ResourceObjectTypeBasicWizardPanel(id, helper);
    }

    @Override
    protected ResourceObjectTypeWizardChoicePanel createTypePreview() {
        return new ResourceObjectTypeWizardChoicePanel(getIdOfChoicePanel(), createHelper(false)) {
            @Override
            protected void onTileClickPerformed(ResourceObjectTypePreviewTileType value, AjaxRequestTarget target) {
                switch (value) {
                    case BASIC:
                        showResourceObjectTypeBasic(target);
                        break;
                    case ATTRIBUTE_MAPPING:
                        showTableForAttributes(target);
                        break;
                    case SYNCHRONIZATION:
                        showSynchronizationConfigWizard(target);
                        break;
                    case CORRELATION:
                        showCorrelationItemsTable(target);
                        break;
                    case CREDENTIALS:
                        showCredentialsWizardPanel(target);
                        break;
                    case ACTIVATION:
                        showActivationsWizard(target);
                        break;
                    case CAPABILITIES:
                        showCapabilitiesConfigWizard(target);
                        break;
                    case POLICIES:
                        showPoliciesWizard(target);
                        break;
                }
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                super.onExitPerformed(target);
                ResourceObjectTypeWizardPanel.this.onExitPerformed(target);
            }

            @Override
            protected void showPreviewDataObjectType(AjaxRequestTarget target) {
                showTableForDataOfCurrentlyObjectType(target);
            }
        };
    }

    private void showPoliciesWizard(AjaxRequestTarget target) {
        showChoiceFragment(
                target,
                new PoliciesObjectTypeWizardPanel(getIdOfChoicePanel(), createHelper(false))
        );
    }

    private void showResourceObjectTypeBasic(AjaxRequestTarget target) {
        showChoiceFragment(
                target,
                new ResourceObjectTypeBasicWizardPanel(getIdOfChoicePanel(), createHelper(true))
        );
    }

    private void showCredentialsWizardPanel(AjaxRequestTarget target) {
        showChoiceFragment(
                target,
                new CredentialsWizardPanel(getIdOfChoicePanel(), createHelper(ResourceObjectTypeDefinitionType.F_CREDENTIALS, false))
        );
    }

    private void showCorrelationItemsTable(AjaxRequestTarget target) {
        showChoiceFragment(
                target,
                new CorrelationWizardPanel(getIdOfChoicePanel(), createHelper(ResourceObjectTypeDefinitionType.F_CORRELATION, false))
        );
    }

    private void showCapabilitiesConfigWizard(AjaxRequestTarget target) {
        showChoiceFragment(
                target,
                new CapabilitiesWizardPanel(
                        getIdOfChoicePanel(),
                        createHelper(ResourceObjectTypeDefinitionType.F_CONFIGURED_CAPABILITIES, false))
        );
    }

    private void showSynchronizationConfigWizard(AjaxRequestTarget target) {
        showWizardFragment(
                target,
                new SynchronizationWizardPanel<>(
                        getIdOfWizardPanel(),
                        createHelper(ResourceObjectTypeDefinitionType.F_SYNCHRONIZATION, false))
        );
    }

    private void showActivationsWizard(AjaxRequestTarget target) {
        showWizardFragment(
                target,
                new ActivationsWizardPanel(getIdOfWizardPanel(), createHelper(ResourceObjectTypeDefinitionType.F_ACTIVATION, false))
        );
    }

    private void showTableForAttributes(AjaxRequestTarget target) {
        showChoiceFragment(
                target,
                new AttributeMappingWizardPanel<>(getIdOfChoicePanel(), createHelper(false))
        );
    }

    private void showTableForDataOfCurrentlyObjectType(AjaxRequestTarget target) {
        showChoiceFragment(target, new PreviewResourceObjectTypeDataWizardPanel(
                getIdOfChoicePanel(),
                getAssignmentHolderModel(),
                ResourceObjectTypeWizardPanel.this.getValueModel()) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                super.onExitPerformed(target);
                showTypePreviewFragment(target);
            }
        });
    }
}
