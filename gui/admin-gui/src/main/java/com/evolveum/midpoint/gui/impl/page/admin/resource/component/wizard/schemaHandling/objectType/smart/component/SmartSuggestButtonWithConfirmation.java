/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component;

import com.evolveum.midpoint.smart.api.info.HealthStatus;

import com.evolveum.midpoint.web.component.dialog.SuggestionOption;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.component.dialog.ConfirmationWithOptionsDto;
import com.evolveum.midpoint.web.component.dialog.privacy.DataAccessPermission;
import com.evolveum.midpoint.web.component.input.ActivityIndicationInteractionsPair;
import com.evolveum.midpoint.web.component.input.BlockingActionButtonWithConfirmationOptionsDialog;
import com.evolveum.midpoint.web.component.input.ButtonWithConfirmationOptionsDialog;
import com.evolveum.midpoint.smart.api.info.AiInfo;
import com.evolveum.midpoint.web.component.util.Describable;

/**
 * A styled variant of {@link ButtonWithConfirmationOptionsDialog} pre-configured with the Smart Suggest confirmation
 * dialog content.
 * <p>
 * Use {@link #create} for the default asynchronous path, or {@link #forBlockingActionWithIndication} when the
 * confirmation action is synchronous and an activity indication (spinner) should be shown while it runs.
 */
public class SmartSuggestButtonWithConfirmation<T extends Describable>
        extends ButtonWithConfirmationOptionsDialog<T> {

    public SmartSuggestButtonWithConfirmation(String id, IModel<ButtonConfig<T>> buttonConfig,
            IModel<ButtonHandlers<T>> clickHandlers) {
        super(id, buttonConfig, clickHandlers);
    }

    private static ButtonConfig<DataAccessPermission> buildButtonConfig(
            IModel<String> icon,
            IModel<String> title,
            SuggestionOption options,
            PageBase pageBase) {

        IModel<AiInfo> aiInfoModel = createAiInfoModel(pageBase);

        IModel<ConfirmationWithOptionsDto<DataAccessPermission>> confirmationDialogConfig =
                () -> ConfirmationWithOptionsDto.<DataAccessPermission>builder()
                        .confirmationTitle(pageBase.createStringResource("SmartSuggestConfirmationPanel.title"))
                        .confirmationSubtitle(pageBase.createStringResource("SmartSuggestConfirmationPanel.subtitle"))
                        .confirmationOptionsTitle(pageBase.createStringResource(
                                "SmartSuggestConfirmationPanel.request.component.title"))
                        .infoEntries(aiInfoModel)
                        .errorMessage(() ->
                                options.requiresAiService()
                                        ? getUnavailableMessage(
                                        pageBase,
                                        aiInfoModel,
                                        "SmartSuggestConfirmationPanel.serviceUnreachable.error")
                                        : null)
                        .warningMessage(() ->
                                options.requiresAiService()
                                        ? null
                                        : getUnavailableMessage(
                                        pageBase,
                                        aiInfoModel,
                                        "SmartSuggestConfirmationPanel.serviceUnreachable.warning"))
                        .confirmationOptions(options.confirmationOptions())
                        .requireAiService(options.requiresAiService())
                        .build();

        return new ButtonConfig<>(icon, title, confirmationDialogConfig, () -> pageBase);
    }

    private static IModel<AiInfo> createAiInfoModel(PageBase pageBase) {
        return new LoadableDetachableModel<>() {
            @Override
            protected AiInfo load() {
                try {
                    return pageBase.getSmartIntegrationService()
                            .getAiInfo()
                            .orElse(null);
                } catch (SystemException e) {
                    return null;
                }
            }
        };
    }

    private static boolean isAiServiceUnavailable(IModel<AiInfo> aiInfoModel) {
        AiInfo aiInfo = aiInfoModel.getObject();
        return aiInfo == null || !HealthStatus.OK.equals(aiInfo.status());
    }

    private static String getUnavailableMessage(
            PageBase pageBase,
            IModel<AiInfo> aiInfoModel,
            String resourceKey) {

        if (!isAiServiceUnavailable(aiInfoModel)) {
            return null;
        }

        return pageBase.createStringResource(resourceKey).getString();
    }

    /**
     * Creates a Smart Suggest button for an *non-blocking* confirm action.
     * <p>
     * The {@link ButtonHandlers#confirmHandler()} is called immediately inside the dialog's Ajax callback.
     */
    public static SmartSuggestButtonWithConfirmation<DataAccessPermission> create(String id,
            IModel<String> title, IModel<String> icon,
            SuggestionOption options,
            IModel<ButtonHandlers<DataAccessPermission>> clickHandlers, PageBase pageBase) {

        final ButtonConfig<DataAccessPermission> buttonConfig =
                buildButtonConfig(icon, title, options, pageBase);

        final SmartSuggestButtonWithConfirmation<DataAccessPermission> button =
                new SmartSuggestButtonWithConfirmation<>(id, () -> buttonConfig, clickHandlers);
        button.add(AttributeModifier.append("class", "btn btn-purple"));
        return button;
    }

    /**
     * Creates a Smart Suggest button for a *blocking* confirm action, wrapped with an activity indication (spinner)
     * while the action runs.
     * <p>
     * Internally this creates a {@link BlockingActionButtonWithConfirmationOptionsDialog}.
     */
    public static BlockingActionButtonWithConfirmationOptionsDialog<DataAccessPermission>
    forBlockingActionWithIndication(String id, IModel<String> title, IModel<String> icon,
            IModel<String> activityIndicationIcon, IModel<String> activityIndicationTitle,
            SuggestionOption options,
            IModel<ButtonHandlers<DataAccessPermission>> clickHandlers, PageBase pageBase) {

        // We need to be sure, the models support the `setObject` operation for activity indication.
        final IModel<String> settableIcon = Model.of(icon.getObject());
        final IModel<String> settableTitle = Model.of(title.getObject());
        final ButtonConfig<DataAccessPermission> buttonConfig =
                buildButtonConfig(settableIcon, settableTitle, options, pageBase);

        final BlockingActionButtonWithConfirmationOptionsDialog<DataAccessPermission> button =
                new BlockingActionButtonWithConfirmationOptionsDialog<>(id, () -> buttonConfig, clickHandlers,
                        new ActivityIndicationInteractionsPair(activityIndicationIcon, activityIndicationTitle, true));
        button.add(AttributeModifier.append("class", "btn rounded bg-purple"));
        return button;
    }
}
