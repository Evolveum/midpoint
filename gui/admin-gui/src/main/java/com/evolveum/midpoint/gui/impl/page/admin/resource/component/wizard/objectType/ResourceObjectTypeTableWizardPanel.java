/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.prism.PrismContainerValue;

import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardBasicPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.ResourceSchemaHandlingPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import org.jetbrains.annotations.NotNull;

/**
 * @author lskublik
 */
public abstract class ResourceObjectTypeTableWizardPanel extends AbstractWizardBasicPanel<ResourceDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectTypeTableWizardPanel.class);

    private static final String PANEL_TYPE = "schemaHandling";
    private static final String ID_TABLE = "table";

    public ResourceObjectTypeTableWizardPanel(String id, ResourceDetailsModel model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        ResourceSchemaHandlingPanel table = new ResourceSchemaHandlingPanel(ID_TABLE, getAssignmentHolderDetailsModel(), getConfiguration()) {
            @Override
            protected void onEditValue(IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel, AjaxRequestTarget target) {
                ResourceObjectTypeTableWizardPanel.this.onEditValue(valueModel, target);
            }

            @Override
            protected void onBeforeRender() {
                super.onBeforeRender();

                getTable().getTable().setShowAsCard(false);
            }

            @Override
            protected void onNewValue(IModel<PrismContainerWrapper<ResourceObjectTypeDefinitionType>> containerModel, AjaxRequestTarget target) {
                IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> model = () -> {
                    PrismContainerWrapper<ResourceObjectTypeDefinitionType> container = containerModel.getObject();
                    PrismContainerValue<ResourceObjectTypeDefinitionType> value = container.getItem().createNewValue();
                    try {
                        PrismContainerValueWrapper newWrapper = WebPrismUtil.createNewValueWrapper(
                                container, value, getPageBase(), getObjectDetailsModels().createWrapperContext());
                        container.getValues().add(newWrapper);
                        return newWrapper;
                    } catch (SchemaException e) {
                        LOGGER.error("Couldn't create new value for container " + container, e);
                    }
                    return null;
                };
                onCreateValue(model, target);
            }
        };
        table.setOutputMarkupId(true);
        add(table);
    }

    public MultivalueContainerListPanelWithDetailsPanel getTable() {
        return ((ResourceSchemaHandlingPanel) get(ID_TABLE)).getTable();
    }

    ContainerPanelConfigurationType getConfiguration(){
        return WebComponentUtil.getContainerConfiguration(
                getAssignmentHolderDetailsModel().getObjectDetailsPageConfiguration().getObject(),
                PANEL_TYPE);
    }

    protected abstract void onEditValue(IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> value, AjaxRequestTarget target);

    protected abstract void onCreateValue(IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> value, AjaxRequestTarget target);

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

    @Override
    protected String getCssForWidthOfFeedbackPanel() {
        return "col-8";
    }
}
