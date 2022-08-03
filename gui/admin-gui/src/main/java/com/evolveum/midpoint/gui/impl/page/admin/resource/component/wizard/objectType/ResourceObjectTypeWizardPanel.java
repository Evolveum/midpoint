/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType;

import com.evolveum.midpoint.gui.api.component.result.Toast;
import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.attributeMapping.*;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.credentials.PasswordInboundStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.credentials.PasswordOutboundStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.synchronization.DefaultSettingStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.synchronization.ReactionStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.synchronization.SynchronizationConfigWizardPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaHandlingType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lskublik
 */
public class ResourceObjectTypeWizardPanel extends AbstractResourceWizardPanel<ResourceObjectTypeDefinitionType> {

    public ResourceObjectTypeWizardPanel(String id, ResourceDetailsModel model) {
        super(id, model);
    }

    protected void initLayout() {
        add(createChoiceFragment(createTablePanel()));
    }

    protected ResourceObjectTypeTableWizardPanel createTablePanel() {
        ResourceObjectTypeTableWizardPanel table = new ResourceObjectTypeTableWizardPanel(getIdOfChoicePanel(), getResourceModel()) {
            @Override
            protected void onAddNewObject(AjaxRequestTarget target) {
                showNewResourceObjectTypeWizardFragment(target);
            }

            @Override
            protected void onEditValue(IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel, AjaxRequestTarget target) {
                showObjectTypePreviewFragment(valueModel, target);
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                ResourceObjectTypeWizardPanel.this.onExitPerformed(target);
            }
        };
        return table;
    }

    protected void onExitPerformed(AjaxRequestTarget target) {
    }

    private void showNewResourceObjectTypeWizardFragment(AjaxRequestTarget target) {
        showWizardFragment(target, new WizardPanel(getIdOfWizardPanel(), new WizardModel(createNewObjectTypeSteps())));
    }

    private List<WizardStep> createNewObjectTypeSteps() {
        List<WizardStep> steps = new ArrayList<>();
        steps.add(new BasicSettingResourceObjectTypeStepPanel(
                getResourceModel(),
                createModelOfNewValue(ItemPath.create(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE))) {
            @Override
            protected void onFinishPerformed(AjaxRequestTarget target) {
                OperationResult result = onSaveResourcePerformed(target);
                if (result != null && !result.isError()) {
                    showObjectTypePreviewFragment(getValueModel(), target);
                }
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showTableFragment(target);
            }
        });

        return steps;
    }

    private void showTableFragment(AjaxRequestTarget target) {
        showChoiceFragment(target, createTablePanel());
    }

    private void showObjectTypePreviewFragment(IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> model, AjaxRequestTarget target) {
        showChoiceFragment(target, new ResourceObjectTypeWizardPreviewPanel(getIdOfChoicePanel(), getResourceModel(), model) {
            @Override
            protected void onResourceTileClick(ResourceObjectTypePreviewTileType value, AjaxRequestTarget target) {
                switch (value) {
                    case PREVIEW_DATA:
                        showTableForDataOfCurrentlyObjectType(target, getValueModel());
                        break;
                    case ATTRIBUTE_MAPPING:
                        showTableForAttributes(target, getValueModel());
                        break;
                    case SYNCHRONIZATION_CONFIG:
                        showSynchronizationConfigWizard(target, getValueModel());
                        break;
                    case CREDENTIALS:
                        showCredentialsWizard(target, getValueModel());
                        break;
                }
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showTableFragment(target);
                new Toast()
                        .success()
                        .title(getString("ResourceObjectTypeWizardPanel.createObjectType"))
                        .icon("fas fa-circle-check")
                        .autohide(true)
                        .delay(5_000)
                        .body(getString("ResourceObjectTypeWizardPanel.createObjectType.text")).show(target);
            }
        });
    }

    private void showSynchronizationConfigWizard(
            AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        showWizardFragment(
                target,
                new SynchronizationConfigWizardPanel(getIdOfWizardPanel(), getResourceModel(), valueModel) {
                    protected void onExitPerformed(AjaxRequestTarget target) {
                        showObjectTypePreviewFragment(getValueModel(), target);
                    }

                    @Override
                    protected OperationResult onSaveResourcePerformed(AjaxRequestTarget target) {
                        return ResourceObjectTypeWizardPanel.this.onSaveResourcePerformed(target);
                    }
                });
    }

    private void showCredentialsWizard(
            AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        showWizardFragment(
                target,
                new WizardPanel(getIdOfWizardPanel(), new WizardModel(createCredentialsSteps(valueModel))));
    }

    private List<WizardStep> createCredentialsSteps(IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        List<WizardStep> steps = new ArrayList<>();
        steps.add(new PasswordInboundStepPanel(getResourceModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showObjectTypePreviewFragment(valueModel, target);
            }
        });

        steps.add(new PasswordOutboundStepPanel(getResourceModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showObjectTypePreviewFragment(valueModel, target);
            }

            @Override
            protected void onFinishPerformed(AjaxRequestTarget target) {
                OperationResult result = onSaveResourcePerformed(target);
                if (result != null && !result.isError()) {
                    onExitPerformed(target);
                }
            }
        });



        return steps;
    }

    private void showTableForAttributes(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        showChoiceFragment(target, new AttributeMappingWizardPanel(
                getIdOfChoicePanel(),
                getResourceModel(),
                valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showObjectTypePreviewFragment(getValueModel(), target);
            }
        });
    }

    private void showTableForDataOfCurrentlyObjectType(
            AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        showChoiceFragment(target, new PreviewResourceObjectTypeDataWizardPanel(
                getIdOfChoicePanel(),
                getResourceModel(),
                valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showObjectTypePreviewFragment(getResourceObjectType(), target);
            }
        });
    }
}
