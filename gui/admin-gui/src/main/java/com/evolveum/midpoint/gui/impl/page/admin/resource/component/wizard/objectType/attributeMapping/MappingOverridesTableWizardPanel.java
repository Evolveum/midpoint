/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.attributeMapping;

import com.evolveum.midpoint.gui.api.component.wizard.AbstractWizardBasicPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.util.List;

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

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        if (getTable().isValidFormComponents(target)) {
            onSaveResourcePerformed(target);
        }
    }

    @Override
    protected boolean isSubmitButtonVisible() {
        return true;
    }

    protected MappingOverrideTable getTable() {
        return (MappingOverrideTable) get(ID_TABLE);
    }

    protected IModel<String> getSubmitLabelModel() {
        return getPageBase().createStringResource("MappingOverridesTableWizardPanel.saveButton");
    }

    protected abstract void onSaveResourcePerformed(AjaxRequestTarget target);

    protected abstract void inEditNewValue(IModel<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> value, AjaxRequestTarget target);

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
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
