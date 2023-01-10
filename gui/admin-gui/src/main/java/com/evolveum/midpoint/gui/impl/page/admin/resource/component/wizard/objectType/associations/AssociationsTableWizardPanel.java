/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.associations;

import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectAssociationType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import org.jetbrains.annotations.NotNull;

/**
 * @author lskublik
 */
@Experimental
public abstract class AssociationsTableWizardPanel extends AbstractResourceWizardBasicPanel<ResourceObjectTypeDefinitionType> {

    private static final String ID_TABLE = "table";

    public AssociationsTableWizardPanel(
            String id,
            WizardPanelHelper<ResourceObjectTypeDefinitionType, ResourceDetailsModel> superHelper) {
        super(id, superHelper);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        AssociationsTable table = new AssociationsTable(ID_TABLE, getValueModel()) {
            @Override
            protected void editItemPerformed(
                    AjaxRequestTarget target,
                    IModel<PrismContainerValueWrapper<ResourceObjectAssociationType>> rowModel,
                    List<PrismContainerValueWrapper<ResourceObjectAssociationType>> listItems) {
                inEditNewValue(rowModel, target);
            }
        };
        table.setOutputMarkupId(true);
        add(table);
    }

    protected abstract void inEditNewValue(IModel<PrismContainerValueWrapper<ResourceObjectAssociationType>> value, AjaxRequestTarget target);

    @Override
    protected boolean isValid(AjaxRequestTarget target) {
        return getTable().isValidFormComponents(target);
    }

    @Override
    protected String getSaveLabelKey() {
        return "AssociationsTableWizardPanel.saveButton";
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return getTextModel();
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("AssociationsTableWizardPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("AssociationsTableWizardPanel.subText");
    }

    protected AssociationsTable getTable() {
        return (AssociationsTable) get(ID_TABLE);
    }
}
