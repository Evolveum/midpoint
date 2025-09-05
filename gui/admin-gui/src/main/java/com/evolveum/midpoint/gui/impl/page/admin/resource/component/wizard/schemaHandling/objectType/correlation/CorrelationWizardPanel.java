/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
            protected void postProcessAddSuggestion(AjaxRequestTarget target) {
                showUnsavedChangesToast(target);
                showChoiceFragment(target, createTablePanel());
            }

            @Override
            protected void showTableForItemRefs(
                    @NotNull AjaxRequestTarget target,
                    @NotNull IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> resourceObjectTypeDefinition,
                    @NotNull IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> rowModel,
                    @Nullable StatusInfo<CorrelationSuggestionsType> statusInfo) {
                WizardPanelHelper<ItemsSubCorrelatorType, ResourceDetailsModel> helper = new WizardPanelHelper<>(
                        getAssignmentHolderDetailsModel(), rowModel) {
                    @Override
                    public void onExitPerformed(AjaxRequestTarget target) {
                        showUnsavedChangesToast(target);
                        showChoiceFragment(target, createTablePanel());
                    }
                };

                showChoiceFragment(target, new CorrelationItemRuleWizardPanel(getIdOfChoicePanel(), helper, () -> statusInfo) {
                    @Override
                    protected void acceptSuggestionPerformed(
                            @NotNull AjaxRequestTarget target,
                            @NotNull IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> valueModel) {
                        acceptSuggestionItemPerformed(getPageBase(), target, valueModel, resourceObjectTypeDefinition, statusInfo);
                    }

                    @Override
                    protected void onDiscardButtonClick(
                            @NotNull PageBase pageBase,
                            @NotNull AjaxRequestTarget target,
                            @NotNull IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> valueModel,
                            @NotNull StatusInfo<?> statusInfo) {
                        performDiscard(pageBase, target, valueModel, statusInfo);
                    }

                    @Override
                    protected boolean isShowEmptyField() {
                        return true;
                    }

                });
            }
        };
    }
}
