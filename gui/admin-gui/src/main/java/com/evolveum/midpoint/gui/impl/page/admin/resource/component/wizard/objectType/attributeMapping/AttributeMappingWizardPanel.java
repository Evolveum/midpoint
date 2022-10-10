/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.attributeMapping;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.result.Toast;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardPanel;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

/**
 * @author lskublik
 */
public class AttributeMappingWizardPanel extends AbstractResourceWizardPanel<ResourceAttributeDefinitionType> {

    private final IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel;

    public AttributeMappingWizardPanel(
            String id,
            ResourceDetailsModel model,
            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        super(id, model);
        this.valueModel = valueModel;
    }

    protected void initLayout() {
        add(createChoiceFragment(createTablePanel(WrapperContext.AttributeMappingType.INBOUND)));
    }

    protected AttributeMappingsTableWizardPanel createTablePanel(WrapperContext.AttributeMappingType initialTab) {
        AttributeMappingsTableWizardPanel table
                = new AttributeMappingsTableWizardPanel(getIdOfChoicePanel(), getResourceModel(), valueModel, initialTab) {

            @Override
            protected void onSaveResourcePerformed(AjaxRequestTarget target) {
                if (!isSavedAfterWizard()) {
                    onExitPerformedAfterValidate(target);
                    return;
                }
                OperationResult result = AttributeMappingWizardPanel.this.onSaveResourcePerformed(target);
                if (result != null && !result.isError()) {
                    new Toast()
                            .success()
                            .title(getString("ResourceWizardPanel.updateResource"))
                            .icon("fas fa-circle-check")
                            .autohide(true)
                            .delay(5_000)
                            .body(getString("ResourceWizardPanel.updateResource.text")).show(target);
                    onExitPerformedAfterValidate(target);
                }
            }

            @Override
            protected void inEditOutboundValue(IModel<PrismContainerValueWrapper<MappingType>> value, AjaxRequestTarget target) {
                showOutboundAttributeMappingWizardFragment(target, value);
            }

            @Override
            protected void inEditInboundValue(IModel<PrismContainerValueWrapper<MappingType>> value, AjaxRequestTarget target) {
                showInboundAttributeMappingWizardFragment(target, value);
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                if (getTable().isValidFormComponents(target)) {
                    onExitPerformedAfterValidate(target);
                }
            }

            private void onExitPerformedAfterValidate(AjaxRequestTarget target) {
                super.onExitPerformed(target);
                AttributeMappingWizardPanel.this.onExitPerformed(target);
            }

            @Override
            protected void onShowOverrides(AjaxRequestTarget target, WrapperContext.AttributeMappingType selectedTable) {
                showAttributeOverrides(target, selectedTable);
            }

            @Override
            protected IModel<String> getSubmitLabelModel() {
                if (isSavedAfterWizard()) {
                    return super.getSubmitLabelModel();
                }
                return getPageBase().createStringResource("WizardPanel.confirm");
            }

            @Override
            protected String getSubmitIcon() {
                if (isSavedAfterWizard()) {
                    return super.getSubmitIcon();
                }
                return "fa fa-check";
            }
        };
        return table;
    }

    private void showAttributeOverrides(AjaxRequestTarget target, WrapperContext.AttributeMappingType selectedTable) {
        showChoiceFragment(
                target,
                new MappingOverridesTableWizardPanel(getIdOfChoicePanel(), getResourceModel(), getValueModel()) {
                    @Override
                    protected void onSaveResourcePerformed(AjaxRequestTarget target) {
                        if (!isSavedAfterWizard()) {
                            onExitPerformedAfterValidate(target);
                            return;
                        }
                        OperationResult result = AttributeMappingWizardPanel.this.onSaveResourcePerformed(target);
                        if (result != null && !result.isError()) {
                            new Toast()
                                    .success()
                                    .title(getString("ResourceWizardPanel.updateResource"))
                                    .icon("fas fa-circle-check")
                                    .autohide(true)
                                    .delay(5_000)
                                    .body(getString("ResourceWizardPanel.updateResource.text")).show(target);
                            onExitPerformedAfterValidate(target);
                        } else {
                            target.add(getFeedback());
                        }
                    }

                    private void onExitPerformedAfterValidate(AjaxRequestTarget target) {
                        super.onExitPerformed(target);
                        AttributeMappingWizardPanel.this.onExitPerformed(target);
                    }

                    @Override
                    protected void onExitPerformed(AjaxRequestTarget target) {
                        if (getTable().isValidFormComponents(target)) {
                            onExitPerformedAfterValidate(target);
                        }
                    }

                    @Override
                    protected void inEditNewValue(IModel<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> value, AjaxRequestTarget target) {
                        showWizardFragment(
                                target,
                                new WizardPanel(getIdOfWizardPanel(), new WizardModel(createNewAttributeOverrideSteps(value, selectedTable))));
                    }

                    @Override
                    protected IModel<String> getSubmitLabelModel() {
                        if (isSavedAfterWizard()) {
                            return super.getSubmitLabelModel();
                        }
                        return getPageBase().createStringResource("WizardPanel.confirm");
                    }

                    @Override
                    protected String getSubmitIcon() {
                        if (isSavedAfterWizard()) {
                            return super.getSubmitIcon();
                        }
                        return "fa fa-check";
                    }
                });
    }

    protected void onExitPerformed(AjaxRequestTarget target) {
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
        steps.add(new AttributeInboundStepPanel(getResourceModel(), valueModel) {
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
        steps.add(new AttributeOutboundStepPanel(getResourceModel(), valueModel) {
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
        steps.add(new MainConfigurationStepPanel(getResourceModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showAttributeOverrides(target, selectedTable);
            }
        });

        steps.add(new LimitationsStepPanel(getResourceModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showAttributeOverrides(target, selectedTable);
            }
        });
        return steps;
    }

    @Override
    protected PrismContainerWrapper<ResourceAttributeDefinitionType> findContainer(ItemPath path) throws SchemaException {
        return valueModel.getObject().findContainer(path);
    }

    protected IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> getValueModel() {
        return valueModel;
    }

    private void showTableFragment(AjaxRequestTarget target, WrapperContext.AttributeMappingType initialTab) {
        showChoiceFragment(target, createTablePanel(initialTab));
    }

    protected boolean isSavedAfterWizard() {
        return true;
    }
}
