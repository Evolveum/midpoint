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
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationWrapperUtils.createMappingsValueIfRequired;

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
            protected void showTableForItemRefs(
                    @NotNull AjaxRequestTarget target,
                    @NotNull IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> resourceObjectTypeDefinition,
                    @NotNull IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> rowModel,
                    @Nullable StatusInfo<CorrelationSuggestionsType> statusInfo) {
                WizardPanelHelper<ItemsSubCorrelatorType, ResourceDetailsModel> helper = new WizardPanelHelper<>(getAssignmentHolderDetailsModel(), rowModel) {
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
                        if (statusInfo == null) {
                            getPageBase().warn("Correlation suggestion is not available.");
                            target.add(getPageBase().getFeedbackPanel().getParent());
                            return;
                        }

//TODO change to parent container

//                        CorrelationSuggestionsType result = statusInfo.getResult();
//                        List<ResourceAttributeDefinitionType> attributes = result.getAttributes();
//
//                        if (attributes.isEmpty()) {
//                            performAddOperation(target, resourceObjectTypeDefinition, attributes, valueModel);
//                            return;
//                        }
//
//                        CorrelationAddMappingConfirmationPanel confirmationPanel = new CorrelationAddMappingConfirmationPanel(
//                                getPageBase().getMainPopupBodyId(), Model.of(), () -> attributes) {
//                            @Override
//                            public void yesPerformed(AjaxRequestTarget target) {
//                                performAddOperation(target, resourceObjectTypeDefinition, attributes, valueModel);
//                            }
//                        };
//                        getPageBase().showMainPopup(confirmationPanel, target);
                    }

                    private void performAddOperation(
                            @NotNull AjaxRequestTarget target,
                            @NotNull IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> resourceObjectTypeDef,
                            @Nullable List<ResourceAttributeDefinitionType> attributes,
                            @NotNull IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> valueModel) {
                        createMappingsValueIfRequired(getPageBase(), target, resourceObjectTypeDef, attributes);
                        PrismContainerValueWrapper<ItemsSubCorrelatorType> object = valueModel.getObject();
                        createNewItemsSubCorrelatorValue(getPageBase(), object.getNewValue(), target);
//                        performDiscard(target);
                        onExitPerformed(target);
                    }

                });
            }
        };
    }
}
