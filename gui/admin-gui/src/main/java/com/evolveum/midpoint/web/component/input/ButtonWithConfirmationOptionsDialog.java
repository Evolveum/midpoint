/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.web.component.input;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.web.component.dialog.ConfirmationWithOptionsPopupPanel;
import com.evolveum.midpoint.web.component.dialog.steper.*;
import com.evolveum.midpoint.web.component.dialog.steper.step.ConfirmationWithOptionsStepPanel;

import com.evolveum.midpoint.web.component.dialog.steper.step.ThreadSetupPopupStepPanel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.dialog.ConfirmationOption;
import com.evolveum.midpoint.web.component.dialog.ConfirmationWithOptionsDto;
import com.evolveum.midpoint.web.component.util.Describable;
import com.evolveum.midpoint.web.component.util.SerializableConsumer;

import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

/**
 * A button that opens a confirmation-with-options dialog when clicked and delegates the outcome to a pair of
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
    private final IModel<Integer> threadsModel = Model.of(4);
    boolean threadConfEnabled;

    public ButtonWithConfirmationOptionsDialog(String id, IModel<ButtonConfig<T>> buttonConfig,
            boolean threadConfEnabled,
            IModel<ButtonHandlers<T>> clickHandlers) {
        super(id, buttonConfig.getObject().icon(), buttonConfig.getObject().title());
        this.config = buttonConfig;
        this.handlers = clickHandlers;
        this.threadConfEnabled = threadConfEnabled;
    }

    @Override
    public void onInitialize() {
        super.onInitialize();
    }

    @Override
    public void onClick(AjaxRequestTarget target) {
        final ButtonConfig<T> cfg = this.config.getObject();
        final PageBase pageBase = cfg.pageBase().getObject();

        if (threadConfEnabled) {
            PopupStepperPanel popup = createThreadedConfirmationPopupStepper(cfg, pageBase);
            pageBase.showMainPopup(popup, target);
        } else {

            final ConfirmationWithOptionsPopupPanel<T> dialog = new ConfirmationWithOptionsPopupPanel<>(
                    pageBase.getMainPopupBodyId(),
                    cfg.confirmationDialogConfig()) {

                @Override
                public void confirmationPerformed(AjaxRequestTarget target,
                        IModel<List<ConfirmationOption<T>>> confirmedOptions) {
                    onDialogConfirmed(target, confirmedOptions, 1);
                }

                @Override
                public void cancelPerformed(AjaxRequestTarget target) {
                    final ButtonHandlers<T> onClickHandlers = handlers.getObject();
                    if (onClickHandlers.cancelHandler() != null) {
                        onClickHandlers.cancelHandler().accept(target);
                    }
                }
            };
            pageBase.showMainPopup(dialog, target);
        }

    }

    private @NotNull PopupStepperPanel createThreadedConfirmationPopupStepper(ButtonConfig<T> cfg, PageBase pageBase) {
        final ConfirmationWithOptionsStepPanel<T> confirmationDialogStep = new ConfirmationWithOptionsStepPanel<>(cfg.confirmationDialogConfig());

        ThreadSetupPopupStepPanel threadSetupStep = new ThreadSetupPopupStepPanel(threadsModel);

        List<PopupStep> steps = List.of(
                confirmationDialogStep,
                threadSetupStep
        );

        PopupStepperModel model = new PopupStepperModel(steps);

        return new PopupStepperPanel(
                pageBase.getMainPopupBodyId(),
                Model.of(model)) {

            @Override
            public IModel<String> getTitle() {
                return createStringResource("ButtonWithConfirmationOptionsDialog.title");
            }

            @Override
            protected IModel<String> getFinishLabel() {
                return createStringResource("ButtonWithConfirmationOptionsDialog.finishLabel");
            }

            @Override
            protected String getFinishCssClass() {
                return "btn btn-ai";
            }

            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
                Integer workerThreads = threadSetupStep.getWorkerThreads();
                IModel<List<ConfirmationOption<T>>> selectedOptionsModel = confirmationDialogStep.getSelectedOptionsModel();
                onDialogConfirmed(target, selectedOptionsModel, workerThreads);
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
    void onDialogConfirmed(
            AjaxRequestTarget target,
            IModel<List<ConfirmationOption<T>>> confirmedOptions,
            int workerThreads) {

        ButtonHandlers<T> onClickHandlers = handlers.getObject();

        if (onClickHandlers.threadedConfirmHandler() != null) {
            onClickHandlers.threadedConfirmHandler().accept(target, confirmedOptions, workerThreads);
        } else if (onClickHandlers.confirmHandler() != null) {
            onClickHandlers.confirmHandler().accept(target, confirmedOptions);
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
            ConfirmationHandler<T> confirmHandler,
            ThreadedConfirmationHandler<T> threadedConfirmHandler)
            implements Serializable {

        public ButtonHandlers(
                SerializableConsumer<AjaxRequestTarget> cancelHandler,
                ConfirmationHandler<T> confirmHandler) {
            this(cancelHandler, confirmHandler, null);
        }

        public ButtonHandlers(
                SerializableConsumer<AjaxRequestTarget> cancelHandler,
                ThreadedConfirmationHandler<T> threadedConfirmHandler) {
            this(cancelHandler, null, threadedConfirmHandler);
        }
    }

    @FunctionalInterface
    public interface ConfirmationHandler<T extends Describable> extends Serializable {

        void accept(
                AjaxRequestTarget target,
                IModel<List<ConfirmationOption<T>>> options);
    }

    @FunctionalInterface
    public interface ThreadedConfirmationHandler<T extends Describable> extends Serializable {

        void accept(
                AjaxRequestTarget target,
                IModel<List<ConfirmationOption<T>>> options,
                int workerThreads);
    }

    public record ButtonConfig<T extends Describable>(
            IModel<String> icon,
            IModel<String> title,
            IModel<ConfirmationWithOptionsDto<T>> confirmationDialogConfig,
            IModel<PageBase> pageBase)
            implements Serializable {
    }
}
