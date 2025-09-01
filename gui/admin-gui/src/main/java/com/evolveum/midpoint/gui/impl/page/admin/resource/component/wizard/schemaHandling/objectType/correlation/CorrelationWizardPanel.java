/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemsSubCorrelatorType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

/**
 * @author lskublik
 */
public class CorrelationWizardPanel extends AbstractWizardPanel<CorrelationDefinitionType, ResourceDetailsModel> {

    public CorrelationWizardPanel(String id, WizardPanelHelper<CorrelationDefinitionType, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createChoiceFragment(createTablePanel()));
    }

    protected CorrelationItemsTableWizardPanel createTablePanel() {
        return new CorrelationItemsTableWizardPanel(getIdOfChoicePanel(), getHelper()) {

            @Override
            protected void showTableForItemRefs(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> rowModel, StatusInfo<?> statusInfo) {
                WizardPanelHelper<ItemsSubCorrelatorType, ResourceDetailsModel> helper = new WizardPanelHelper<>(getAssignmentHolderDetailsModel(), rowModel) {
                    @Override
                    public void onExitPerformed(AjaxRequestTarget target) {
                        showUnsavedChangesToast(target);
                        showChoiceFragment(target, createTablePanel());
                    }
                };
                showChoiceFragment(target, new CorrelationItemRefsTableWizardPanel(getIdOfChoicePanel(), helper, () -> statusInfo) {
                    @Override
                    protected void acceptSuggestionPerformed(
                            @NotNull AjaxRequestTarget target,
                            @NotNull IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> valueModel) {
                        PrismContainerValueWrapper<ItemsSubCorrelatorType> object = valueModel.getObject();
                        createNewValue(getPageBase(), object.getNewValue(), target);
                        performDiscard(target);
                        onExitPerformed(target);
                    }
                });
            }
        };
    }
}
