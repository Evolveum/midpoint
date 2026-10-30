/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component;

import com.evolveum.midpoint.web.component.dialog.SuggestionOption;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.component.dialog.ConfirmationWithOptionsDto;
import com.evolveum.midpoint.web.component.dialog.ConfirmationWithOptionsPopupPanel;
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

    private final SmartSuggestConfirmationDto<T> confirmationConfig;

    public SmartSuggestButtonWithConfirmation(String id, IModel<ButtonConfig<T>> buttonConfig,
            IModel<ButtonHandlers<T>> clickHandlers, SmartSuggestConfirmationDto<T> confirmationConfig) {
        super(id, buttonConfig, clickHandlers);
        this.confirmationConfig = confirmationConfig;
    }

    @Override
    protected ConfirmationWithOptionsPopupPanel<T> createConfirmationDialog(
            String id, IModel<ConfirmationWithOptionsDto<T>> dialogConfig, ButtonHandlers<T> dialogHandlers) {
        return SmartSuggestConfirmationPopupPanel.create(id, confirmationConfig, dialogHandlers);
    }

    @Override
    public void onDetach() {
        confirmationConfig.detach();
        super.onDetach();
    }

    private static ButtonConfig<DataAccessPermission> buildButtonConfig(
            IModel<String> icon, IModel<String> title,
            SmartSuggestConfirmationDto<DataAccessPermission> confirmationConfig, PageBase pageBase) {
        return new ButtonConfig<>(icon, title, () -> confirmationConfig.confirmation(), () -> pageBase);
    }

    private static IModel<AiInfo> createAiInfoModel(PageBase pageBase) {
        return new LoadableDetachableModel<>() {
            @Override
            protected AiInfo load() {
                try {
                    var task = pageBase.createSimpleTask("getAiInfo");
                    return pageBase.getSmartIntegrationService()
                            .getAiInfo(task, task.getResult())
                            .orElse(null);
                } catch (CommonException | SystemException e) {
                    return null;
                }
            }
        };
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

        SmartSuggestConfirmationDto<DataAccessPermission> confirmationConfig =
                SmartSuggestConfirmationDto.create(pageBase, options, createAiInfoModel(pageBase));
        final ButtonConfig<DataAccessPermission> buttonConfig =
                buildButtonConfig(icon, title, confirmationConfig, pageBase);

        final SmartSuggestButtonWithConfirmation<DataAccessPermission> button =
                new SmartSuggestButtonWithConfirmation<>(id, () -> buttonConfig, clickHandlers, confirmationConfig);
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
        SmartSuggestConfirmationDto<DataAccessPermission> confirmationConfig =
                SmartSuggestConfirmationDto.create(pageBase, options, createAiInfoModel(pageBase));
        final ButtonConfig<DataAccessPermission> buttonConfig =
                buildButtonConfig(settableIcon, settableTitle, confirmationConfig, pageBase);

        final BlockingActionButtonWithConfirmationOptionsDialog<DataAccessPermission> button =
                new BlockingActionButtonWithConfirmationOptionsDialog<>(id, () -> buttonConfig, clickHandlers,
                        new ActivityIndicationInteractionsPair(activityIndicationIcon, activityIndicationTitle, true)) {
                    @Override
                    protected ConfirmationWithOptionsPopupPanel<DataAccessPermission> createConfirmationDialog(
                            String dialogId, IModel<ConfirmationWithOptionsDto<DataAccessPermission>> dialogConfig,
                            ButtonHandlers<DataAccessPermission> dialogHandlers) {
                        return SmartSuggestConfirmationPopupPanel.create(dialogId, confirmationConfig, dialogHandlers);
                    }

                    @Override
                    public void onDetach() {
                        confirmationConfig.detach();
                        super.onDetach();
                    }
                };
        button.add(AttributeModifier.append("class", "btn btn-purple"));
        return button;
    }
}
