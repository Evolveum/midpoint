/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType;

import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;

import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.SchemaHandlingTypeWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.basic.ResourceObjectTypeBasicWizardPanel;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.activation.ActivationsWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.associations.AssociationsWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attributeMapping.AttributeMappingWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.capabilities.CapabilitiesWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation.CorrelationWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.credentials.CredentialsWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.synchronization.SynchronizationWizardPanel;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationDefinitionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

/**
 * @author lskublik
 */
public class ResourceObjectTypeWizardPanel extends SchemaHandlingTypeWizardPanel<ResourceObjectTypeDefinitionType> {

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
    protected ResourceObjectTypeWizardPreviewPanel createTypePreview() {
        return new ResourceObjectTypeWizardPreviewPanel(getIdOfChoicePanel(), createHelper(false)) {
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
                    case ASSOCIATIONS:
                        showAssociationsWizard(target);
                        break;
                    case ACTIVATION:
                        showActivationsWizard(target);
                        break;
                    case CAPABILITIES:
                        showCapabilitiesConfigWizard(target);
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

    private void showAssociationsWizard(AjaxRequestTarget target) {
        showChoiceFragment(
                target,
                new AssociationsWizardPanel(getIdOfChoicePanel(), createHelper(false))
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
