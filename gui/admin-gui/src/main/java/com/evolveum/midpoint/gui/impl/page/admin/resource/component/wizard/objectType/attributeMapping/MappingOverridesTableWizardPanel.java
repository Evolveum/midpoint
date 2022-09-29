/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.attributeMapping;

import com.evolveum.midpoint.gui.api.component.TableTabbedPanel;
import com.evolveum.midpoint.gui.api.component.wizard.AbstractWizardBasicPanel;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author lskublik
 */
public abstract class MappingOverridesTableWizardPanel extends AbstractWizardBasicPanel {

    private static final String ID_TABLE = "table";

    private final IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel;

    public MappingOverridesTableWizardPanel(
            String id,
            ResourceDetailsModel model,
            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        super(id, model);
        this.valueModel = valueModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        MappingOverrideTable table = new MappingOverrideTable(ID_TABLE, valueModel) {
            @Override
            protected void editItemPerformed(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> rowModel, List<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> listItems) {
                inEditNewValue(rowModel, target);
            }
        };

        table.setOutputMarkupId(true);
        add(table);
    }

    public MappingOverrideTable getTablePanel() {
        //noinspection unchecked
        return (MappingOverrideTable) get(ID_TABLE);
    }

    @Override
    protected void addCustomButtons(RepeatingView buttons) {
        AjaxIconButton newObjectTypeButton = new AjaxIconButton(
                buttons.newChildId(),
                Model.of("fa fa-circle-plus"),
                getPageBase().createStringResource("MappingOverridesTableWizardPanel.addNewAttributeOverride")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onAddNewObject(target);
            }
        };
        newObjectTypeButton.showTitleAsLabel(true);
        newObjectTypeButton.add(AttributeAppender.append("class", "btn btn-primary"));
        buttons.add(newObjectTypeButton);

        AjaxIconButton saveButton = new AjaxIconButton(
                buttons.newChildId(),
                Model.of(getSubmitIcon()),
                getSubmitLabelModel()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onSaveResourcePerformed(target);
                onExitPerformed(target);
            }
        };
        saveButton.showTitleAsLabel(true);
        saveButton.add(AttributeAppender.append("class", "btn btn-success"));
        buttons.add(saveButton);
    }

    protected String getSubmitIcon() {
        return "fa fa-floppy-disk";
    }

    protected IModel<String> getSubmitLabelModel() {
        return getPageBase().createStringResource("MappingOverridesTableWizardPanel.saveButton");
    }

    private void onAddNewObject(AjaxRequestTarget target) {
        MappingOverrideTable table = getTablePanel();
        inEditNewValue(Model.of(table.createNewOverride(target)), target);
    }

    protected abstract void onSaveResourcePerformed(AjaxRequestTarget target);

    protected abstract void inEditNewValue(IModel<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> value, AjaxRequestTarget target);

    @Override
    protected IModel<String> getBreadcrumbLabel() {
        return getPageBase().createStringResource("MappingOverridesTableWizardPanel.breadcrumb");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("MappingOverridesTableWizardPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("MappingOverridesTableWizardPanel.subText");
    }
}
