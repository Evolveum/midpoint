/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.web.component.input;

import java.io.Serializable;

import com.evolveum.midpoint.web.component.dialog.ConfirmationResult;
import com.evolveum.midpoint.web.component.dialog.ConfirmationWithOptionsPopupPanel;

import com.evolveum.midpoint.web.component.util.SerializableBiConsumer;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.dialog.ConfirmationWithOptionsDto;
import com.evolveum.midpoint.web.component.util.Describable;
import com.evolveum.midpoint.web.component.util.SerializableConsumer;

/**
 * A button that opens a confirmation-with-confirmationOptions dialog when clicked and delegates the outcome to a pair of
 * handlers ({@link ButtonHandlers}).
 * <p>
 * When the user confirms the dialog the {@link ButtonHandlers#confirmHandler()} is invoked directly inside the
 * dialog's Ajax callback.
 * <p>
 * This is the right choice when the action is *non-blocking*. For synchronous actions that need an activity
 * indication (e.g. a spinner shown while the action runs), use {@link BlockingActionButtonWithConfirmationOptionsDialog}
 * instead.
 */
public class ButtonWithConfirmationOptionsDialog<T extends Describable> extends AjaxIconButton {

    private final IModel<ButtonConfig<T>> config;
    private final IModel<ButtonHandlers<T>> handlers;

    public ButtonWithConfirmationOptionsDialog(String id, IModel<ButtonConfig<T>> buttonConfig,
            IModel<ButtonHandlers<T>> clickHandlers) {
        super(id, buttonConfig.getObject().icon(), buttonConfig.getObject().title());
        this.config = buttonConfig;
        this.handlers = clickHandlers;
    }

    @Override
    public void onInitialize() {
        super.onInitialize();
    }

    @Override
    public void onClick(AjaxRequestTarget target) {
        final ButtonConfig<T> cfg = this.config.getObject();
        final PageBase pageBase = cfg.pageBase().getObject();

        ConfirmationWithOptionsPopupPanel<T> dialog = createConfirmationDialog(
                pageBase.getMainPopupBodyId(),
                Model.of(cfg.confirmationDialogConfig().getObject()),
                new ButtonHandlers<>(
                        request -> {
                            SerializableConsumer<AjaxRequestTarget> cancel = handlers.getObject().cancelHandler();
                            if (cancel != null) {
                                cancel.accept(request);
                            }
                        },
                        this::onDialogConfirmed));
        pageBase.showMainPopup(dialog, target);
    }

    protected ConfirmationWithOptionsPopupPanel<T> createConfirmationDialog(
            String id, IModel<ConfirmationWithOptionsDto<T>> dialogConfig, ButtonHandlers<T> dialogHandlers) {
        return new ConfirmationWithOptionsPopupPanel<>(id, dialogConfig) {
            @Override
            protected void confirmationPerformed(AjaxRequestTarget target,
                    IModel<ConfirmationResult<T>> confirmationResult) {
                dialogHandlers.confirmHandler().accept(target, confirmationResult);
            }

            @Override
            protected void cancelPerformed(AjaxRequestTarget target) {
                dialogHandlers.cancelHandler().accept(target);
            }
        };
    }

    /**
     * Called when the user confirms the dialog. The default implementation invokes
     * {@link ButtonHandlers#confirmHandler()}.
     * <p>
     * NOTE: This method is intentionally package private, to prevent overriding from anonymous implementations
     * outside of this package.
     */
    void onDialogConfirmed(AjaxRequestTarget target,
            IModel<ConfirmationResult<T>> confirmationResult) {
        final ButtonHandlers<T> onClickHandlers = handlers.getObject();
        if (onClickHandlers.confirmHandler() != null) {
            onClickHandlers.confirmHandler().accept(target, confirmationResult);
        }
    }

    @Override
    public void onDetach() {
        this.config.detach();
        this.handlers.detach();
        super.onDetach();
    }

    public record ButtonHandlers<T extends Describable>(
            SerializableConsumer<AjaxRequestTarget> cancelHandler,
            SerializableBiConsumer<AjaxRequestTarget, IModel<ConfirmationResult<T>>> confirmHandler)
            implements Serializable {
    }

    public record ButtonConfig<T extends Describable>(
            IModel<String> icon,
            IModel<String> title,
            IModel<ConfirmationWithOptionsDto<T>> confirmationDialogConfig,
            IModel<PageBase> pageBase)
            implements Serializable {
    }
}
