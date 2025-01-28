/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard;

import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic.*;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.AssociationTypeTableWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.ResourceAssociationTypeWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.ResourceObjectTypeTableWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.ResourceObjectTypeWizardPanel;
import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationTypeDefinitionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
public class ResourceWizardPanel extends AbstractWizardPanel<ResourceType, ResourceDetailsModel> {

    public ResourceWizardPanel(String id, WizardPanelHelper<ResourceType, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createChoiceFragment(createBasicWizard()));
    }

    private BasicResourceWizardPanel createBasicWizard() {
        BasicResourceWizardPanel basicWizard = new BasicResourceWizardPanel(
                getIdOfChoicePanel(), getHelper()) {

            @Override
            protected void onFinishBasicWizardPerformed(AjaxRequestTarget target) {
                ResourceWizardPanel.this.onFinishBasicWizardPerformed(target);
            }
        };
        basicWizard.setOutputMarkupId(true);
        return basicWizard;
    }

    private ResourceObjectTypeWizardPanel createObjectTypeWizard(
            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {

        WizardPanelHelper<ResourceObjectTypeDefinitionType, ResourceDetailsModel> helper =
                new WizardPanelHelper<>(getAssignmentHolderModel()) {

                    @Override
                    public void onExitPerformed(AjaxRequestTarget target) {
                        showChoiceFragment(target, createObjectTypesTablePanel());
                    }

                    @Override
                    public OperationResult onSaveObjectPerformed(AjaxRequestTarget target) {
                        return getHelper().onSaveObjectPerformed(target);
                    }

                    @Override
                    public IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> getDefaultValueModel() {
                        return valueModel;
                    }
                };
        ResourceObjectTypeWizardPanel wizard = new ResourceObjectTypeWizardPanel(getIdOfChoicePanel(), helper);
        wizard.setOutputMarkupId(true);
        return wizard;
    }

    protected ResourceObjectTypeTableWizardPanel createObjectTypesTablePanel() {
        return new ResourceObjectTypeTableWizardPanel(getIdOfChoicePanel(), getAssignmentHolderModel()) {
            @Override
            protected void onEditValue(IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel, AjaxRequestTarget target) {
                ResourceObjectTypeWizardPanel wizard = createObjectTypeWizard(valueModel);
                wizard.setShowChoicePanel(true);
                showChoiceFragment(target, wizard);
            }

            @Override
            protected void onCreateValue(IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> value, AjaxRequestTarget target, boolean isDuplicate) {
                showChoiceFragment(target, createObjectTypeWizard(value));
            }

            @Override
            protected OperationResult onSaveObjectPerformed(AjaxRequestTarget target) {
                return getHelper().onSaveObjectPerformed(target);
            }

            @Override
            protected void onExitPerformedAfterValidate(AjaxRequestTarget target) {
                super.onExitPerformedAfterValidate(target);
                exitToPreview(target);
            }

            @Override
            protected void refreshValueModel() {
                getHelper().refreshValueModel();
            }
        };
    }

    protected AssociationTypeTableWizardPanel createAssociationTypesTablePanel() {
        return new AssociationTypeTableWizardPanel(getIdOfChoicePanel(), getAssignmentHolderModel()) {
            @Override
            protected void onEditValue(IModel<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> valueModel, AjaxRequestTarget target) {
                ResourceAssociationTypeWizardPanel wizard = createAssociationTypeWizard(valueModel, false);
                wizard.setShowChoicePanel(true);
                showChoiceFragment(target, wizard);
            }

            @Override
            protected void onCreateValue(IModel<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> value, AjaxRequestTarget target, boolean isDuplicate) {
                showChoiceFragment(target, createAssociationTypeWizard(value, isDuplicate));
            }

            @Override
            protected OperationResult onSaveObjectPerformed(AjaxRequestTarget target) {
                return getHelper().onSaveObjectPerformed(target);
            }

            @Override
            protected void onExitPerformedAfterValidate(AjaxRequestTarget target) {
                super.onExitPerformedAfterValidate(target);
                exitToPreview(target);
            }

            @Override
            protected void refreshValueModel() {
                getHelper().refreshValueModel();
            }
        };
    }

    protected ResourceAssociationTypeWizardPanel createAssociationTypeWizard(
            IModel<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> valueModel, boolean isDuplicate) {

        WizardPanelHelper<ShadowAssociationTypeDefinitionType, ResourceDetailsModel> helper =
                new WizardPanelHelper<>(getAssignmentHolderModel()) {

                    @Override
                    public void onExitPerformed(AjaxRequestTarget target) {
                        showChoiceFragment(target, createAssociationTypesTablePanel());
                    }

                    @Override
                    public OperationResult onSaveObjectPerformed(AjaxRequestTarget target) {
                        return getHelper().onSaveObjectPerformed(target);
                    }

                    @Override
                    public IModel<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> getDefaultValueModel() {
                        return valueModel;
                    }
                };
        ResourceAssociationTypeWizardPanel wizard = new ResourceAssociationTypeWizardPanel(getIdOfChoicePanel(), helper);
        wizard.setPanelForDuplicate(isDuplicate);
        wizard.setOutputMarkupId(true);
        return wizard;
    }

    private void onFinishBasicWizardPerformed(AjaxRequestTarget target) {
        OperationResult result = getHelper().onSaveObjectPerformed(target);
        if (!result.isError()) {
            exitToPreview(target);
        }
    }

    private PreviewResourceDataWizardPanel createPreviewResourceDataWizardPanel() {
        return new PreviewResourceDataWizardPanel(getIdOfChoicePanel(), getAssignmentHolderModel()) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                super.onExitPerformed(target);
                exitToPreview(target);
            }
        };
    }

    private void exitToPreview(AjaxRequestTarget target) {
        SchemaHandlingWizardChoicePanel preview = new SchemaHandlingWizardChoicePanel(getIdOfChoicePanel(), getAssignmentHolderModel()) {
            @Override
            protected void onTileClickPerformed(SchemaHandlingWizardChoicePanel.PreviewTileType value, AjaxRequestTarget target) {
                switch (value) {
                    case PREVIEW_DATA:
                        showChoiceFragment(target, createPreviewResourceDataWizardPanel());
                        break;
                    case CONFIGURE_OBJECT_TYPES:
                        showChoiceFragment(target, createObjectTypesTablePanel());
                        break;
                    case CONFIGURE_ASSOCIATION_TYPES:
                        showChoiceFragment(target, createAssociationTypesTablePanel());
                        break;

                }
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                getHelper().onExitPerformed(target);
            }
        };
        showChoiceFragment(target, preview);
    }
}
