/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.page;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.midpoint.xml.ns._public.prism_schema_3.ComplexTypeDefinitionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils.loadObjectClassObjectTypeSuggestions;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.*;

public class SmartObjectTypeSuggestionWizardPanel<C extends ResourceObjectTypeDefinitionType, P extends Containerable> extends AbstractWizardPanel<P, ResourceDetailsModel> {

    private static final String CLASS_DOT = SmartObjectTypeSuggestionWizardPanel.class.getName() + ".";
    private static final String OP_DEFINE_TYPES = CLASS_DOT + "defineTypes";
    private static final String OP_DETERMINE_STATUS = CLASS_DOT + "determineStatus";

    private static final String OP_DELETE_SUGGESTIONS =
            SmartObjectTypeSuggestionWizardPanel.class.getName() + ".deleteSuggestions";

    public SmartObjectTypeSuggestionWizardPanel(String id, WizardPanelHelper<P, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createChoiceFragment(createTablePanel(getIdOfChoicePanel())));
    }

    protected ResourceObjectClassTableWizardPanel<ResourceObjectTypeDefinitionType, P> createTablePanel(String idOfChoicePanel) {
        return new ResourceObjectClassTableWizardPanel<>(idOfChoicePanel, getHelper()) {

            @Override
            protected void onContinueWithSelected(
                    IModel<PrismContainerValueWrapper<ComplexTypeDefinitionType>> model,
                    AjaxRequestTarget target) {
                var complexTypeDef = model.getObject().getRealValue();
                processSuggestionActivity(target, complexTypeDef.getName(), false);
            }
        };
    }

    /**
     * Processes the suggestion activity for the given object class name.
     */
    private void processSuggestionActivity(AjaxRequestTarget target, QName objectClassName, boolean resetSuggestion) {
        String resourceOid = getAssignmentHolderModel().getObjectType().getOid();
        Task task = getPageBase().createSimpleTask(OP_DETERMINE_STATUS);
        OperationResult result = task.getResult();

        StatusInfo<ObjectTypesSuggestionType> suggestions = loadObjectClassObjectTypeSuggestions(
                getPageBase(), resourceOid, objectClassName, task, result);

        boolean hasValidSuggestions = isSuccessfulSuggestion(suggestions);

        if (hasValidSuggestions && resetSuggestion) {
            removeWholeTaskObject(getPageBase(), task, result, suggestions.getToken());
            hasValidSuggestions = false;
        }

        if (hasValidSuggestions) {
            showChoiceFragment(target, buildSelectSuggestedObjectTypeWizardPanel(getIdOfChoicePanel(), objectClassName));
            return;
        }

        boolean executed = runSuggestionAction(
                getPageBase(), resourceOid, objectClassName, target, OP_DEFINE_TYPES, task);

        result.computeStatusIfUnknown();

        if (!executed) {
            if (!result.isSuccess()) {
                getPageBase().showResult(result);
                target.add(getPageBase().getFeedbackPanel(), SmartObjectTypeSuggestionWizardPanel.this);
            }
            return;
        }

        showChoiceFragment(target, buildGeneratingWizardPanel(getIdOfChoicePanel(), objectClassName));
    }

    /**
     * Checks if the given suggestion is successful and contains valid object types.
     */
    private boolean isSuccessfulSuggestion(StatusInfo<ObjectTypesSuggestionType> suggestions) {
        return suggestions != null
                && suggestions.getStatus() == OperationResultStatusType.SUCCESS
                && suggestions.getResult() != null
                && suggestions.getResult().getObjectType() != null
                && !suggestions.getResult().getObjectType().isEmpty();
    }

    private @NotNull ResourceGeneratingSuggestionObjectClassWizardPanel<ResourceObjectTypeDefinitionType, P> buildGeneratingWizardPanel(
            @NotNull String idOfChoicePanel, QName objectClassName) {
        return new ResourceGeneratingSuggestionObjectClassWizardPanel<>(idOfChoicePanel, getHelper(), objectClassName) {

            @Override
            protected IModel<String> getBackLabel() {
                return createStringResource("SmartSuggestionWizardPanel.back.to.object.class.selection");
            }

            @Override
            protected void onBackPerformed(AjaxRequestTarget target) {
                removeLastBreadcrumb();
                showChoiceFragment(target, createTablePanel(idOfChoicePanel));
            }

            @Override
            protected void onContinueWithSelected(AjaxRequestTarget target) {
                showChoiceFragment(target, buildSelectSuggestedObjectTypeWizardPanel(idOfChoicePanel, objectClassName));
            }
        };
    }

    @Contract("_, _ -> new")
    private @NotNull ResourceSuggestedObjectTypeTableWizardPanel<P> buildSelectSuggestedObjectTypeWizardPanel(
            @NotNull String idOfChoicePanel, QName objectClassName) {
        return new ResourceSuggestedObjectTypeTableWizardPanel<>(idOfChoicePanel, getHelper(), objectClassName) {

            @Override
            protected void onContinueWithSelected(
                    IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> model,
                    @NotNull PrismContainerValue<ResourceObjectTypeDefinitionType> newValue,
                    @NotNull IModel<PrismContainerWrapper<ResourceObjectTypeDefinitionType>> containerModel,
                    @NotNull AjaxRequestTarget target) {
                var suggestedValueContainer = model.getObject();
                ResourceObjectTypeDefinitionType suggestedObjectTypeDef = suggestedValueContainer.getRealValue();
                PageBase pageBase = getPageBase();

                getAssignmentHolderModel().getPageResource()
                        .showObjectTypeWizard(newValue, target, containerModel.getObject().getPath(),
                                ajaxRequestTarget -> {
                                    Task task = pageBase.createSimpleTask(OP_DELETE_SUGGESTIONS);
                                    OperationResult result = task.getResult();
                                    removeObjectTypeSuggestionNew(pageBase, statusInfo, suggestedObjectTypeDef, task, result);
                                });
            }

            @Override
            public void refreshSuggestionPerform(AjaxRequestTarget target) {
                removeLastBreadcrumb();
                processSuggestionActivity(target, objectClassName, true);
            }

            @Override
            protected void onBackPerformed(AjaxRequestTarget target) {
                removeLastBreadcrumb();
                showChoiceFragment(target, createTablePanel(idOfChoicePanel));
            }
        };
    }
}
