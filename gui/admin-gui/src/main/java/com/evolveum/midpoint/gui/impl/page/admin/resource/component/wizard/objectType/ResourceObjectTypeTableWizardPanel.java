/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;

import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.prism.PrismContainerValue;

import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

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

import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

/**
 * @author lskublik
 */
@PanelType(name = "rw-types")
@PanelInstance(identifier = "rw-types",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "ResourceObjectTypeTableWizardPanel.title", icon = "fa fa-object-group"))
public abstract class ResourceObjectTypeTableWizardPanel extends AbstractWizardBasicPanel<ResourceDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectTypeTableWizardPanel.class);

    private static final String PANEL_TYPE = "rw-types";
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
            protected void onNewValue(PrismContainerValue<ResourceObjectTypeDefinitionType> value, IModel<PrismContainerWrapper<ResourceObjectTypeDefinitionType>> containerModel, AjaxRequestTarget target) {
                PageBase pageBase = getPageBase();
                PrismContainerWrapper<ResourceObjectTypeDefinitionType> container = containerModel.getObject();
                PrismContainerValue<ResourceObjectTypeDefinitionType> newValue = value;
                if (newValue == null) {
                    newValue = container.getItem().createNewValue();
                }
                PrismContainerValueWrapper newWrapper = null;
                try {
                    newWrapper = WebPrismUtil.createNewValueWrapper(
                            container, newValue, pageBase, getObjectDetailsModels().createWrapperContext());
                    container.getValues().add(newWrapper);
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't create new value for container " + container, e);
                }
                IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> model = Model.of(newWrapper);
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
