/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.correlation;

import com.evolveum.midpoint.gui.api.component.wizard.AbstractWizardBasicPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemsSubCorrelatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author lskublik
 */
public abstract class CorrelationItemsTableWizardPanel extends AbstractWizardBasicPanel {

    private static final String ID_TABLE = "table";

    private final IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel;

    public CorrelationItemsTableWizardPanel(
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
        CorrelationItemsTable table = new CorrelationItemsTable(ID_TABLE, valueModel) {
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
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        onSaveResourcePerformed(target);
    }

    @Override
    protected boolean isSubmitButtonVisible() {
        return true;
    }

    protected IModel<String> getSubmitLabelModel() {
        return getPageBase().createStringResource("CorrelationWizardPanelWizardPanel.saveButton");
    }

    protected abstract void onSaveResourcePerformed(AjaxRequestTarget target);

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
}
