/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.wizard.AbstractWizardBasicPanel;
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
public abstract class ResourceObjectTypeTableWizardPanel extends AbstractWizardBasicPanel {

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
        ResourceSchemaHandlingPanel table = new ResourceSchemaHandlingPanel(ID_TABLE, getResourceModel(), getConfiguration()) {
            @Override
            protected void onEditValue(IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel, AjaxRequestTarget target) {
                ResourceObjectTypeTableWizardPanel.this.onEditValue(valueModel, target);
            }

            @Override
            protected void onBeforeRender() {
                super.onBeforeRender();

                getTable().getTable().setShowAsCard(false);
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
                getResourceModel().getObjectDetailsPageConfiguration().getObject(),
                PANEL_TYPE);
    }

    protected abstract void onEditValue(IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> value, AjaxRequestTarget target);

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
