/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.page;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils.loadObjectClassObjectTypeSuggestions;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.removeObjectTypeSuggestionNew;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.removeWholeTaskObject;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.runSuggestionAction;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationWrapperUtils.processSuggestedContainerValue;

import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component.SmartSuggestButtonWithConfirmation;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.dialog.ConfirmationOption;
import com.evolveum.midpoint.web.component.dialog.privacy.DataAccessPermission;
import com.evolveum.midpoint.web.component.input.ButtonWithConfirmationOptionsDialog;
import com.evolveum.midpoint.web.component.util.SerializableConsumer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.ComplexTypeDefinitionType;

public class SmartObjectTypeSuggestionWizardPanel extends AbstractWizardPanel<ResourceObjectTypeDefinitionType, ResourceDetailsModel> {

    private static final String CLASS_DOT = SmartObjectTypeSuggestionWizardPanel.class.getName() + ".";
    private static final String OP_DEFINE_TYPES = CLASS_DOT + "defineTypes";
    private static final String OP_DETERMINE_STATUS = CLASS_DOT + "determineStatus";

    private static final String OP_DELETE_SUGGESTIONS =
            SmartObjectTypeSuggestionWizardPanel.class.getName() + ".deleteSuggestions";

    public SmartObjectTypeSuggestionWizardPanel(String id, WizardPanelHelper<ResourceObjectTypeDefinitionType, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createChoiceFragment(createTablePanel(getIdOfChoicePanel())));
    }

    protected ResourceObjectClassTableWizardPanel<ResourceObjectTypeDefinitionType> createTablePanel(String idOfChoicePanel) {
        return new ResourceObjectClassTableWizardPanel<>(idOfChoicePanel, getHelper()) {

            @Override
            protected void onContinueWithSelected(IModel<PrismContainerValueWrapper<ComplexTypeDefinitionType>> model,
                    AjaxRequestTarget target) {
                // We have our own "submit" button bellow, so this should not be called.
                throw new SystemException("Unexpected method was called.");
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                removeLastBreadcrumb();
                SmartObjectTypeSuggestionWizardPanel.this.onExitPerformed(target);
            }

            @Override
            protected boolean isSubmitButtonVisible() {
                // Do not show the "submit" button defined in base classes.
                return false;
            }

            @Override
            protected void addCustomButtons(RepeatingView buttons) {
                AjaxIconButton generateButton = buildGenerateSuggestionButton(buttons);
                generateButton.add(new VisibleBehaviour(() -> !hasValidSuggestions(selectedModel.getObject())));
                buttons.add(generateButton);

                AjaxIconButton showSuggestionButton = buildShowSuggestionButton(buttons);
                showSuggestionButton.add(new VisibleBehaviour(() -> hasValidSuggestions(selectedModel.getObject())));
                buttons.add(showSuggestionButton);
            }

            private @NotNull AjaxIconButton buildGenerateSuggestionButton(@NotNull RepeatingView buttons) {
                AjaxIconButton generateButton = SmartSuggestButtonWithConfirmation.create(buttons.newChildId(),
                        createStringResource("ResourceObjectClassTableWizardPanel.saveButton"),
                        () -> GuiStyleConstants.CLASS_MAGIC_WAND,
                        ConfirmationOption.delineationPermissionsOptions(),
                        () -> new ButtonWithConfirmationOptionsDialog.ButtonHandlers<>(target -> {
                        },
                                (target, confirmedOptions) -> {
                                    final QName objectClassName = selectedModel.getObject().getRealValue().getName();
                                    processSuggestionActivity(target, objectClassName, false, confirmedOptions);
                                }),
                        getPageBase());

                generateButton.setOutputMarkupId(true);
                generateButton.showTitleAsLabel(true);
                return generateButton;
            }

            private @NotNull AjaxIconButton buildShowSuggestionButton(@NotNull RepeatingView buttons) {
                AjaxIconButton showSuggestionButton = new AjaxIconButton(
                        buttons.newChildId(),
                        () -> GuiStyleConstants.CLASS_ICON_PREVIEW,
                        createStringResource("SmartObjectTypeSuggestionWizardPanel.showSuggestion")) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        PrismContainerValueWrapper<ComplexTypeDefinitionType> selected = selectedModel.getObject();
                        QName objectClassName = selected.getRealValue().getName();
                        clearPageFeedback(target);
                        showChoiceFragment(target,
                                buildSelectSuggestedObjectTypeWizardPanel(getIdOfChoicePanel(), objectClassName));
                    }
                };
                showSuggestionButton.setOutputMarkupId(true);
                showSuggestionButton.showTitleAsLabel(true);
                showSuggestionButton.add(AttributeModifier.replace("class", "btn btn-primary"));
                return showSuggestionButton;
            }
        };
    }

    private boolean hasValidSuggestions(@NotNull PrismContainerValueWrapper<ComplexTypeDefinitionType> selectedObject) {
        QName objectClassName = selectedObject.getRealValue().getName();
        String resourceOid = getAssignmentHolderModel().getObjectType().getOid();
        Task task = getPageBase().createSimpleTask(OP_DETERMINE_STATUS);
        OperationResult result = task.getResult();

        StatusInfo<ObjectTypesSuggestionType> suggestions = loadObjectClassObjectTypeSuggestions(
                getPageBase(), resourceOid, objectClassName, task, result);

        return isSuccessfulSuggestion(suggestions);
    }

    /**
     * Processes the suggestion activity for the given object class name.
     */
    private void processSuggestionActivity(AjaxRequestTarget target, QName objectClassName, boolean resetSuggestion,
            IModel<List<ConfirmationOption<DataAccessPermission>>> confirmedOptions) {
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
            clearPageFeedback(target);
            showChoiceFragment(target, buildSelectSuggestedObjectTypeWizardPanel(getIdOfChoicePanel(), objectClassName));
            return;
        }

        List<DataAccessPermissionType> permissions = confirmedOptions.getObject().stream()
                .map(ConfirmationOption::option)
                .map(DataAccessPermission::toSchemaType)
                .toList();
        if (!permissions.contains(DataAccessPermissionType.SCHEMA_ACCESS) ||
                !permissions.contains(DataAccessPermissionType.STATISTICS_ACCESS)) {
            result.recordFatalError("Unable to suggest object types without permissions to access schema and "
                    + "statistics");
            getPageBase().showResult(result);
            target.add(getPageBase().getFeedbackPanel(), SmartObjectTypeSuggestionWizardPanel.this);
            return;
        }

        boolean executed = runSuggestionAction(
                getPageBase(), resourceOid, objectClassName, target, OP_DEFINE_TYPES, task, permissions);

        result.computeStatusIfUnknown();

        if (!executed) {
            if (!result.isSuccess()) {
                getPageBase().showResult(result);
                target.add(getPageBase().getFeedbackPanel(), SmartObjectTypeSuggestionWizardPanel.this);
            }
            return;
        }

        clearPageFeedback(target);
        showChoiceFragment(target, buildGeneratingWizardPanel(getIdOfChoicePanel(), objectClassName));
    }

    /** Clears page-level feedback messages to prevent stale messages on step transitions. */
    private void clearPageFeedback(@NotNull AjaxRequestTarget target) {
        getPageBase().getFeedbackPanel().getFeedbackMessages().clear();
        target.add(getPageBase().getFeedbackPanel());
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

    private @NotNull ResourceGeneratingSuggestionObjectClassWizardPanel<ResourceObjectTypeDefinitionType> buildGeneratingWizardPanel(
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
            protected void onExitPerformed(AjaxRequestTarget target) {
                SmartObjectTypeSuggestionWizardPanel.this.onExitPerformed(target);
            }

            @Override
            protected void onContinueWithSelected(AjaxRequestTarget target) {
                showChoiceFragment(target, buildSelectSuggestedObjectTypeWizardPanel(idOfChoicePanel, objectClassName));
            }
        };
    }

    private @NotNull ResourceSuggestedObjectTypeTableWizardPanel<ResourceObjectTypeDefinitionType> buildSelectSuggestedObjectTypeWizardPanel(
            @NotNull String idOfChoicePanel, QName objectClassName) {
        removeLastBreadcrumb();
        return new ResourceSuggestedObjectTypeTableWizardPanel<>(idOfChoicePanel, getHelper(), objectClassName) {

            @Override
            protected void onContinueWithSelected(
                    IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> model,
                    @NotNull PrismContainerValue<ResourceObjectTypeDefinitionType> newValue,
                    @NotNull IModel<PrismContainerWrapper<ResourceObjectTypeDefinitionType>> containerModel,
                    @NotNull AjaxRequestTarget target) {
                PageBase pageBase = getPageBase();

                ResourceObjectTypeDefinitionType suggestedObjectTypeDef = model.getObject().getRealValue();

                PrismContainerValue<ResourceObjectTypeDefinitionType> original =
                        newValue.clone();
                WebPrismUtil.cleanupEmptyContainerValue(original);

                PrismContainerValue<ResourceObjectTypeDefinitionType> suggestion =
                        processSuggestedContainerValue(original);

                onReviewSelected(suggestion, containerModel.getObject().getPath(), target, ajaxRequestTarget -> {
                    Task task = pageBase.createSimpleTask(OP_DELETE_SUGGESTIONS);
                    OperationResult result = task.getResult();
                    removeObjectTypeSuggestionNew(pageBase, statusInfo, suggestedObjectTypeDef, task, result);
                });
            }

            @Override
            public void refreshSuggestionPerform(AjaxRequestTarget target,
                    IModel<List<ConfirmationOption<DataAccessPermission>>> confirmedOptions) {
                removeLastBreadcrumb();
                processSuggestionActivity(target, objectClassName, true, confirmedOptions);
            }

            @Override
            protected void onBackPerformed(AjaxRequestTarget target) {
                removeLastBreadcrumb();
                showChoiceFragment(target, createTablePanel(idOfChoicePanel));
            }
        };
    }

    protected void onReviewSelected(
            @NotNull PrismContainerValue<ResourceObjectTypeDefinitionType> newValue,
            @NotNull ItemPath pathToContainer,
            @NotNull AjaxRequestTarget target,
            @Nullable SerializableConsumer<AjaxRequestTarget> afterSaveAction) {
        getAssignmentHolderModel().getPageResource()
                .showObjectTypeWizard(newValue, target, pathToContainer, afterSaveAction);
    }
}
