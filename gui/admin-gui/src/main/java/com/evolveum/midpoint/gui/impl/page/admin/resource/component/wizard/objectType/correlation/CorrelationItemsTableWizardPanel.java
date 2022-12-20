/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.correlation;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemsSubCorrelatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author lskublik
 */
public abstract class CorrelationItemsTableWizardPanel extends AbstractResourceWizardBasicPanel<ResourceObjectTypeDefinitionType> {

    private static final String ID_TABLE = "table";

    public CorrelationItemsTableWizardPanel(
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
        CorrelationItemsTable table = new CorrelationItemsTable(ID_TABLE, getValueModel()) {
            @Override
            protected void editItemPerformed(
                    AjaxRequestTarget target,
                    IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> rowModel,
                    List<PrismContainerValueWrapper<ItemsSubCorrelatorType>> listItems) {
                showTableForItemRefs(target, rowModel);
            }
        };
        table.setOutputMarkupId(true);
        add(table);
    }

    protected abstract void showTableForItemRefs(
            AjaxRequestTarget target,
            IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> rowModel);

    @Override
    protected String getSaveLabelKey() {
        return "CorrelationWizardPanelWizardPanel.saveButton";
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return getPageBase().createStringResource("CorrelationWizardPanelWizardPanel.breadcrumb");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("CorrelationWizardPanelWizardPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("CorrelationWizardPanelWizardPanel.subText");
    }

    protected CorrelationItemsTable getTable() {
        return (CorrelationItemsTable) get(ID_TABLE);
    }

    @Override
    protected boolean isValid(AjaxRequestTarget target) {
        return getTable().isValidFormComponents(target);
    }
}
