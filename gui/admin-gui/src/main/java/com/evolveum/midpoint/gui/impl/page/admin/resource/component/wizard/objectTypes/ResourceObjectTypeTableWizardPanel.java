/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectTypes;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractPageObjectDetails;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.ResourceSchemaHandlingPanel;
import com.evolveum.midpoint.gui.api.component.wizard.AbstractWizardBasicPanel;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.List;

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
            protected void editItemPerformed(
                    AjaxRequestTarget target,
                    IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> rowModel,
                    List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> listItems,
                    AbstractPageObjectDetails parent) {
                if ((listItems != null && !listItems.isEmpty()) || rowModel != null) {
                    IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel;
                    if (rowModel == null) {
                        valueModel = () -> listItems.iterator().next();
                    } else {
                        valueModel = rowModel;
                    }
                    onEditValue(valueModel, target);
                } else {
                    warn(createStringResource("MultivalueContainerListPanel.message.noItemsSelected").getString());
                    target.add(getPageBase().getFeedbackPanel());
                }
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

    @Override
    protected void addCustomButtons(RepeatingView buttons) {
        AjaxIconButton newObjectTypeButton = new AjaxIconButton(
                buttons.newChildId(),
                Model.of("fa fa-circle-plus"),
                getPageBase().createStringResource("ResourceObjectTypeTableWizardPanel.addNewObjectType")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onAddNewObject(target);
            }
        };
        newObjectTypeButton.showTitleAsLabel(true);
        newObjectTypeButton.add(AttributeAppender.append("class", "btn btn-primary"));
        buttons.add(newObjectTypeButton);
    }

    protected abstract void onAddNewObject(AjaxRequestTarget target);

    protected abstract void onEditValue(IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> value, AjaxRequestTarget target);

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("ResourceObjectTypeTableWizardPanel.text");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("ResourceObjectTypeTableWizardPanel.subText");
    }
}
