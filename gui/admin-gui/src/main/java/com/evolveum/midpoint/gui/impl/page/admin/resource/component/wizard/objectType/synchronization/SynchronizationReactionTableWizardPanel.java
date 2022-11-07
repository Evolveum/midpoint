/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.synchronization;

import com.evolveum.midpoint.gui.api.component.wizard.AbstractWizardBasicPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationReactionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author lskublik
 */
public abstract class SynchronizationReactionTableWizardPanel extends AbstractWizardBasicPanel {

    private static final String ID_TABLE = "table";

    private static final String ID_DEPRECATED_CONTAINER_INFO = "deprecatedContainerInfo";

    private final IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel;

    public SynchronizationReactionTableWizardPanel(
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

        SynchronizationReactionTable table = new SynchronizationReactionTable(ID_TABLE, valueModel) {
            @Override
            protected void editItemPerformed(
                    AjaxRequestTarget target,
                    IModel<PrismContainerValueWrapper<SynchronizationReactionType>> rowModel,
                    List<PrismContainerValueWrapper<SynchronizationReactionType>> listItems) {
                inEditNewValue(rowModel, target);
            }
        };

        table.setOutputMarkupId(true);
        add(table);

        Label info = new Label(
                ID_DEPRECATED_CONTAINER_INFO,
                getPageBase().createStringResource("SynchronizationReactionTableWizardPanel.deprecatedContainer"));
        info.setOutputMarkupId(true);
        info.add(new VisibleBehaviour(this::isDeprecatedContainerInfoVisible));
        add(info);
    }

    private boolean isDeprecatedContainerInfoVisible() {
        PrismContainerValue depSynch = getResourceModel().getObjectType().getSynchronization().clone().asPrismContainerValue();
        depSynch = WebPrismUtil.cleanupEmptyContainerValue(depSynch);
        return depSynch != null && !depSynch.hasNoItems();
    }

    public SynchronizationReactionTable getTablePanel() {
        return (SynchronizationReactionTable) get(ID_TABLE);
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        if (getTablePanel().isValidFormComponents(target)) {
            onSaveResourcePerformed(target);
        }
    }

    @Override
    protected boolean isSubmitButtonVisible() {
        return true;
    }

    protected IModel<String> getSubmitLabelModel() {
        return getPageBase().createStringResource("SynchronizationReactionTableWizardPanel.saveButton");
    }

    protected abstract void onSaveResourcePerformed(AjaxRequestTarget target);

    protected abstract void inEditNewValue(IModel<PrismContainerValueWrapper<SynchronizationReactionType>> value, AjaxRequestTarget target);

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return getTextModel();
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("SynchronizationReactionTableWizardPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("SynchronizationReactionTableWizardPanel.subText");
    }
}
