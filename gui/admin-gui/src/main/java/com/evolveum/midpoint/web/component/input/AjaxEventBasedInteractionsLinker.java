/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.web.component.input;

import java.io.Serializable;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;

/**
 * Links a component with a `ComponentInteractionsPair` using Ajax events to enable
 * two-phase interaction patterns.
 *
 * This linker implements the mechanism for *automatic reaction invoking* described in
 * `ComponentInteractionsPair`. It solves the problem of executing an action that needs
 * to update the UI *before* performing a long-running operation. Without this linker,
 * UI updates in Wicket Ajax requests are only rendered after the entire request completes,
 * making it impossible to show intermediate states like loading indicators.
 *
 * The linker works by:
 *
 * . Registering an `AjaxEventBehavior` on the component during construction via
 *   `linkInteractions(Component, ComponentInteractionsPair)`. This behavior listens for
 *   a custom JavaScript event named using the component's markup ID.
 * . When `callAction(AjaxRequestTarget)` is called, it first executes the `action` from
 *   the interaction pair, then triggers the custom JavaScript event via
 *   {@link AjaxRequestTarget#appendJavaScript(CharSequence)}.
 * . The triggered event causes the registered `AjaxEventBehavior` to fire, which invokes
 *   the `reaction` from the interaction pair in a *separate* Ajax request.
 *
 * This separation allows the action's UI updates (such as showing a loading spinner) to be
 * rendered immediately, while the reaction (such as executing the long-running operation and
 * hiding the spinner) runs in the subsequent Ajax request.
 *
 * @param <C> The type of component this linker operates on. Must extend `Component`.
 *
 * @see ComponentInteractionsPair
 * @see ActivityIndicationInteractionsPair
 */
public class AjaxEventBasedInteractionsLinker<C extends Component> implements Serializable {

    private static final String EVENT_SUFFIX = "actionTriggered";
    private final C component;
    private final ComponentInteractionsPair<C> interactionPair;
    private final String eventName;

    private AjaxEventBasedInteractionsLinker(C component, ComponentInteractionsPair<C> interactionPair,
            String eventName) {
        this.component = component;
        this.interactionPair = interactionPair;
        this.eventName = eventName;
    }

    /**
     * Links a component with an interaction pair by registering an `AjaxEventBehavior` that
     * listens for a custom JavaScript event.
     *
     * The returned linker should be retained by the caller so that `callAction(AjaxRequestTarget)`
     * can be invoked when the interaction needs to be started.
     *
     * @param <C> The type of component, must extend `Component`.
     * @param component The component to link with the interaction pair. Must not be null.
     * @param interactionPair The interaction pair defining the action and reaction behavior. Must not be null.
     * @return A new `AjaxEventBasedInteractionsLinker` instance configured to manage the interaction.
     */
    public static <C extends Component> AjaxEventBasedInteractionsLinker<C> linkInteractions(
            C component,
            ComponentInteractionsPair<C> interactionPair) {

        final String eventName = component.getMarkupId(true) + EVENT_SUFFIX;
        component.add(new AjaxEventBehavior(eventName) {
            @Override
            protected void onEvent(AjaxRequestTarget target) {
                interactionPair.reaction(component, target);
            }
        });
        return new AjaxEventBasedInteractionsLinker<>(component, interactionPair, eventName);
    }

    /**
     * Executes the action of the interaction pair and triggers the reaction in a subsequent Ajax request.
     *
     * @param target The Ajax request target for updating components.
     */
    public void callAction(AjaxRequestTarget target) {
        interactionPair.action(component, target);
        target.appendJavaScript("""
                (function() {
                    var event = $.Event('%s');
                    $('#%s').trigger(event);
                })();
                """.formatted(eventName, component.getMarkupId(true)));
    }
}
