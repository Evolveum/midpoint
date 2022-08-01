/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.attributeMapping;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardPanel;
import com.evolveum.midpoint.util.exception.SchemaException;
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
        add(createChoiceFragment(createTablePanel()));
    }

    protected AttributeMappingsTableWizardPanel createTablePanel() {
        AttributeMappingsTableWizardPanel table
                = new AttributeMappingsTableWizardPanel(getIdOfChoicePanel(), getResourceModel(), valueModel) {
            @Override
            protected void onAddNewObject(AjaxRequestTarget target) {
                showNewAttributeMappingWizardFragment(target, null);
            }

            @Override
            protected void onEditValue(IModel<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> valueModel, AjaxRequestTarget target) {
                showNewAttributeMappingWizardFragment(target, valueModel);
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                AttributeMappingWizardPanel.this.onExitPerformed(target);
            }
        };
        return table;
    }

    protected void onExitPerformed(AjaxRequestTarget target) {
    }

    private void showNewAttributeMappingWizardFragment(
            AjaxRequestTarget target,
            IModel<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> valueModel) {
        showWizardFragment(
                target,
                new WizardPanel(getIdOfWizardPanel(), new WizardModel(createNewAttributeMappingSteps(valueModel))));
    }

    private List<WizardStep> createNewAttributeMappingSteps(IModel<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> valueModel) {
        List<WizardStep> steps = new ArrayList<>();
        if (valueModel == null) {
            valueModel = createModelOfNewValue(ResourceObjectTypeDefinitionType.F_ATTRIBUTE);
        }
        steps.add(new BasicSettingStepPanel(getResourceModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showTableFragment(target);
            }
        });

        steps.add(new LimitationsStepPanel(getResourceModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showTableFragment(target);
            }
        });

        steps.add(new AttributeOutboundStepPanel(getResourceModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showTableFragment(target);
            }
        });

        steps.add(new AttributeInboundStepPanel(getResourceModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showTableFragment(target);
            }

            @Override
            protected void onFinishWizardPerformed(AjaxRequestTarget target) {
                OperationResult result = onSaveResourcePerformed(target);
                if (result != null && !result.isError()) {
                    onExitPerformed(target);
                }
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

    private void showTableFragment(AjaxRequestTarget target) {
        showChoiceFragment(target, createTablePanel());
    }
}
