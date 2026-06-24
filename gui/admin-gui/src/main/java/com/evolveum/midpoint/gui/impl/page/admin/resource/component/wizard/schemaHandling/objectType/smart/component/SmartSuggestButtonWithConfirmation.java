/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component;

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.dialog.ConfirmationOption;
import com.evolveum.midpoint.web.component.dialog.ConfirmationWithOptionsDto;
import com.evolveum.midpoint.web.component.dialog.privacy.DataAccessPermission;
import com.evolveum.midpoint.web.component.input.ActivityIndicationInteractionsPair;
import com.evolveum.midpoint.web.component.input.BlockingActionButtonWithConfirmationOptionsDialog;
import com.evolveum.midpoint.web.component.input.ButtonWithConfirmationOptionsDialog;
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

    public SmartSuggestButtonWithConfirmation(String id, IModel<ButtonConfig<T>> buttonConfig, boolean threadConfEnabled,
            IModel<ButtonHandlers<T>> clickHandlers) {
        super(id, buttonConfig, threadConfEnabled, clickHandlers);
    }

    private static ButtonConfig<DataAccessPermission> buildButtonConfig(
            IModel<String> icon, IModel<String> title,
            List<ConfirmationOption<DataAccessPermission>> options, PageBase pageBase) {

        final ConfirmationWithOptionsDto<DataAccessPermission> confirmationDialogConfig =
                ConfirmationWithOptionsDto.<DataAccessPermission>builder()
                        .confirmationTitle(pageBase.createStringResource("SmartSuggestConfirmationPanel.title"))
                        .confirmationSubtitle(pageBase.createStringResource("SmartSuggestConfirmationPanel.subtitle"))
                        .confirmationOptionsTitle(pageBase.createStringResource(
                                "SmartSuggestConfirmationPanel.request.component.title"))
                        .confirmationOptions(options)
                        .build();
        return new ButtonConfig<>(icon, title, () -> confirmationDialogConfig, () -> pageBase);
    }

    public static SmartSuggestButtonWithConfirmation<DataAccessPermission> create(
            String id,
            IModel<String> title,
            IModel<String> icon,
            List<ConfirmationOption<DataAccessPermission>> options,
            IModel<ButtonHandlers<DataAccessPermission>> clickHandlers,
            PageBase pageBase) {

        return create(id, title, icon, options, clickHandlers, pageBase, false);
    }

    public static SmartSuggestButtonWithConfirmation<DataAccessPermission> createThreaded(
            String id,
            IModel<String> title,
            IModel<String> icon,
            List<ConfirmationOption<DataAccessPermission>> options,
            IModel<ButtonHandlers<DataAccessPermission>> clickHandlers,
            PageBase pageBase) {

        return create(id, title, icon, options, clickHandlers, pageBase, true);
    }

    /**
     * Creates a Smart Suggest button for an *non-blocking* confirm action.
     * <p>
     * The {@link ButtonHandlers#confirmHandler()} is called immediately inside the dialog's Ajax callback.
     */
    private static SmartSuggestButtonWithConfirmation<DataAccessPermission> create(
            String id,
            IModel<String> title,
            IModel<String> icon,
            List<ConfirmationOption<DataAccessPermission>> options,
            IModel<ButtonHandlers<DataAccessPermission>> clickHandlers,
            PageBase pageBase,
            boolean threaded) {

        ButtonConfig<DataAccessPermission> buttonConfig =
                buildButtonConfig(icon, title, options, pageBase);

        SmartSuggestButtonWithConfirmation<DataAccessPermission> button =
                new SmartSuggestButtonWithConfirmation<>(id, () -> buttonConfig, threaded, clickHandlers);

        button.add(AttributeModifier.append("class", "btn btn-purple"));
        return button;
    }

    /**
     * Creates a Smart Suggest button for a *blocking* confirm action, wrapped with an activity indication (spinner)
     * while the action runs.
     *
     * Internally this creates a {@link BlockingActionButtonWithConfirmationOptionsDialog}.
     */
    public static BlockingActionButtonWithConfirmationOptionsDialog<DataAccessPermission>
    forBlockingActionWithIndication(String id, IModel<String> title, IModel<String> icon,
            IModel<String> activityIndicationIcon, IModel<String> activityIndicationTitle,
            List<ConfirmationOption<DataAccessPermission>> options,
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
