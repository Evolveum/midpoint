/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.correlation;

import com.evolveum.midpoint.gui.api.component.result.Toast;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardPanel;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemsSubCorrelatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
public class CorrelationWizardPanel extends AbstractResourceWizardPanel<ResourceObjectTypeDefinitionType> {

    private final IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel;

    public CorrelationWizardPanel(String id, ResourceDetailsModel model, IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        super(id, model);
        this.valueModel = valueModel;
    }

    protected void initLayout() {
        add(createChoiceFragment(createTablePanel()));
    }

    protected CorrelationItemsTableWizardPanel createTablePanel() {
        CorrelationItemsTableWizardPanel table
                = new CorrelationItemsTableWizardPanel(getIdOfChoicePanel(), getResourceModel(), valueModel) {

            @Override
            protected void onSaveResourcePerformed(AjaxRequestTarget target) {
                if (!isSavedAfterWizard()) {
                    onExitPerformed(target);
                    return;
                }
                OperationResult result = CorrelationWizardPanel.this.onSaveResourcePerformed(target);
                if (result != null && !result.isError()) {
                    new Toast()
                            .success()
                            .title(getString("ResourceWizardPanel.updateResource"))
                            .icon("fas fa-circle-check")
                            .autohide(true)
                            .delay(5_000)
                            .body(getString("ResourceWizardPanel.updateResource.text")).show(target);
                    onExitPerformed(target);
                } else {
                    target.add(getFeedback());
                }
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                super.onExitPerformed(target);
                CorrelationWizardPanel.this.onExitPerformed(target);
            }

            @Override
            protected void showTableForItemRefs(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> rowModel) {
                showChoiceFragment(target, new CorrelationItemRefsTableWizardPanel(getIdOfChoicePanel(), getResourceModel(), rowModel) {
                    @Override
                    protected void onExitPerformed(AjaxRequestTarget target) {
                        if (getTable().isValidFormComponents()) {
                            super.onExitPerformed(target);
                            showChoiceFragment(target, createTablePanel());
                        }
                    }
                });
            }

            @Override
            protected IModel<String> getSubmitLabelModel() {
                if (isSavedAfterWizard()) {
                    return super.getSubmitLabelModel();
                }
                return getPageBase().createStringResource("WizardPanel.confirm");
            }

            @Override
            protected String getSubmitIcon() {
                if (isSavedAfterWizard()) {
                    return super.getSubmitIcon();
                }
                return "fa fa-check";
            }
        };
        return table;
    }

    public IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> getValueModel() {
        return valueModel;
    }

    protected boolean isSavedAfterWizard() {
        return true;
    }
}
