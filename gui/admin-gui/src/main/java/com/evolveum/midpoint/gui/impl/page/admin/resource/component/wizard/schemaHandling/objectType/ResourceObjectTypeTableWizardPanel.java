/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.SchemaHandlingTypesTableWizardPanel;
import com.evolveum.midpoint.prism.PrismContainerValue;

import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.ResourceObjectTypesPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import org.jetbrains.annotations.NotNull;

/**
 * @author lskublik
 */
@PanelType(name = "rw-types")
@PanelInstance(identifier = "rw-types",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "ResourceObjectTypeTableWizardPanel.title", icon = "fa fa-object-group"))
public abstract class ResourceObjectTypeTableWizardPanel extends SchemaHandlingTypesTableWizardPanel<ResourceObjectTypeDefinitionType> {

    private static final String PANEL_TYPE = "rw-types";

    public ResourceObjectTypeTableWizardPanel(String id, ResourceDetailsModel model) {
        super(id, model);
    }

    @Override
    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    protected void initPanel(String panelId) {
        ResourceObjectTypesPanel panel = new ResourceObjectTypesPanel(panelId, getAssignmentHolderDetailsModel(), getConfiguration()) {
            @Override
            protected void onEditValue(IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel, AjaxRequestTarget target) {
                ResourceObjectTypeTableWizardPanel.this.onEditValue(valueModel, target);
            }

            @Override
            protected void onNewValue(PrismContainerValue<ResourceObjectTypeDefinitionType> value, IModel<PrismContainerWrapper<ResourceObjectTypeDefinitionType>> containerModel, AjaxRequestTarget target, boolean isDuplicate) {
                ResourceObjectTypeTableWizardPanel.this.onNewValue(value, containerModel, getObjectDetailsModels().createWrapperContext(), target, isDuplicate);
            }
        };
        panel.setOutputMarkupId(true);
        add(panel);
    }

    protected abstract void onEditValue(IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> value, AjaxRequestTarget target);

    protected abstract void onCreateValue(IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> value, AjaxRequestTarget target, boolean isDuplicate);

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("ResourceObjectTypeTableWizardPanel.subText");
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return getPageBase().createStringResource("ResourceObjectTypeTableWizardPanel.title");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("ResourceObjectTypeTableWizardPanel.text");
    }
}
