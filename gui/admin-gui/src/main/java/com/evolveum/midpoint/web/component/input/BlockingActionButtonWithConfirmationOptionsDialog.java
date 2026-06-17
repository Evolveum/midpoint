/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.web.component.input;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.dialog.ConfirmationOption;
import com.evolveum.midpoint.web.component.util.Describable;

/**
 * A {@link ButtonWithConfirmationOptionsDialog} variant for *blocking* confirm actions that need an activity
 * indication while they run.
 *
 * === How it works
 * Because the action is blocking the browser cannot display an intermediate UI update (e.g. a spinner) before the
 * Ajax response is returned — the request is blocked until the action finishes. This class works around that by
 * splitting the work across two Ajax round-trips via {@link AjaxEventBasedInteractionsLinker}:
 *
 *   . *Confirmation phase*: the confirmed options are saved temporarily, {@link ComponentInteractionsPair#action} is
 *     called (e.g. shows a spinner on the button) and a JavaScript event is fired. The response is returned to the
 *     browser immediately so the spinner is visible.
 *   . *Activity phase*: The {@link ButtonHandlers#confirmHandler()} is invoked with the saved options, and
 *     {@link ComponentInteractionsPair#reaction} is called to restore the button's original appearance.</li>
 */
public class BlockingActionButtonWithConfirmationOptionsDialog<T extends Describable>
        extends ButtonWithConfirmationOptionsDialog<T> {

    private final ComponentInteractionsPair<AjaxIconButton> interactionPairs;
    private IModel<List<ConfirmationOption<T>>> pendingConfirmedOptions;
    private AjaxEventBasedInteractionsLinker<AjaxIconButton> interactionsLinker;


    public BlockingActionButtonWithConfirmationOptionsDialog(String id, IModel<ButtonConfig<T>> buttonConfig,
            IModel<ButtonHandlers<T>> clickHandlers, ComponentInteractionsPair<AjaxIconButton> interactionPairs) {
        super(id, buttonConfig, clickHandlers);
        this.interactionPairs = interactionPairs.reactionAndThen((component, request) -> {
            clickHandlers.getObject().confirmHandler().accept(request, pendingConfirmedOptions);
            pendingConfirmedOptions = null;
        });
    }

    @Override
    public void onInitialize() {
        super.onInitialize();
        this.interactionsLinker = AjaxEventBasedInteractionsLinker.linkInteractions(this, interactionPairs);
    }

    @Override
    void onDialogConfirmed(AjaxRequestTarget target,
            IModel<List<ConfirmationOption<T>>> confirmedOptions) {
        this.pendingConfirmedOptions = confirmedOptions;
        this.interactionsLinker.callAction(target);
    }
}
