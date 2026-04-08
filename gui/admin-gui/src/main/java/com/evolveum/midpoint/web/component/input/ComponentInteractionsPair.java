/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.web.component.input;

import java.io.Serializable;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;

import com.evolveum.midpoint.web.component.util.SerializableBiConsumer;

/**
 * Represents a pair of interactions which allows to run an "action" on an arbitrary Ajax request, while triggering
 * a reaction in the "automatic" Ajax request of different component.
 *
 * Both action and reaction of the interaction pair can operate on a component (which should be the same for both),
 * which may be unrelated to the Ajax request which triggers the action.
 *
 * To illustrate this, imagine a button and a separate label.When you click on the button, you want to trigger some
 * long-running action and modify the label icon or text to indicate that the action is running. You can't do that
 * easily, because the label would not be updated before the action is done, that is of course too late.
 *
 * You can however "switch" the order. On button click, you will only update the icon/text of the label - the
 * `action` of the interactions pair. Then in the `reaction` of the interactions pair, you will trigger the action, and
 * after it is finished, you will reset the label icon/text. In both cases, the component passed to both `action` and
 * `reaction` methods is the label. But the `action` method is run as part of the button Ajax.
 *
 * Above would of course not work on its own. This interface is supposed to be used in combination with a "linker",
 * which implements the mechanism of the automatic `reaction` invoking.
 *
 * @see ActivityIndicationInteractionsPair
 * @see AjaxEventBasedInteractionsLinker
 * @param <C> The type of component this interaction pair works with
 */
public interface ComponentInteractionsPair<C extends Component> extends Serializable {

    /**
     * Executes the action on the specified component.
     *
     * This method is called to perform the primary operation associated with this interaction pair.
     * The action typically modifies the component state or performs some business logic in response to a user
     * interaction.
     *
     * @param component The Wicket component on which the action is performed
     * @param request The Ajax request target used for updating the component or adding other components to the Ajax
     * response
     */
    void action(C component, AjaxRequestTarget request);

    /**
     * Executes the reaction on the specified component.
     *
     * This method is called to perform a follow-up operation after the main action. The reaction typically handles
     * cleanup, state restoration, or secondary updates that should occur after the primary action has been executed.
     *
     * @param component The Wicket component on which the reaction is performed
     * @param request The Ajax request target used for updating the component or adding other components to the Ajax
     * response
     */
    void reaction(C component, AjaxRequestTarget request);

    /**
     * Combines this interaction pair with another one, executing both actions and both
     * reactions in sequence.
     *
     * The returned interaction pair will execute this pair's action followed by the
     * other pair's action, and similarly for reactions. This allows for composing
     * multiple interaction behaviors into a single unified pair.
     *
     * @param other The other interaction pair to chain after this one
     * @return A new interaction pair that executes both pairs in sequence
     */
    default ComponentInteractionsPair<C> andThen(ComponentInteractionsPair<C> other) {
        return new ComponentInteractionsPair<>() {
            @Override
            public void action(C component, AjaxRequestTarget request) {
                ComponentInteractionsPair.this.action(component, request);
                other.action(component, request);
            }

            @Override
            public void reaction(C component, AjaxRequestTarget request) {
                ComponentInteractionsPair.this.reaction(component, request);
                other.reaction(component, request);
            }
        };
    }

    /**
     * Creates a new interaction pair with an additional action to be executed after this pair's action, while
     * keeping the original reaction.
     *
     * This method is useful when you want to extend the action behavior without modifying the reaction. The provided
     * consumer will be called after this pair's action completes.
     *
     * @param nextOnBefore The additional action to execute after this pair's action
     *
     * @return A new interaction pair with the combined action and original reaction
     */
    default ComponentInteractionsPair<C> actionAndThen(SerializableBiConsumer<C, AjaxRequestTarget> nextOnBefore) {
        return new ComponentInteractionsPair<>() {
            @Override
            public void action(C component, AjaxRequestTarget request) {
                ComponentInteractionsPair.this.action(component, request);
                nextOnBefore.accept(component, request);
            }

            @Override
            public void reaction(C component, AjaxRequestTarget request) {
                ComponentInteractionsPair.this.reaction(component, request);
            }
        };
    }

    /**
     * Creates a new interaction pair with an additional reaction to be executed after this pair's reaction, while
     * keeping the original action.
     *
     * This method is useful when you want to extend the reaction behavior without modifying the action. The provided
     * consumer will be called after this pair's reaction completes.
     *
     * @param nextOnAfter The additional reaction to execute after this pair's reaction
     *
     * @return A new interaction pair with the original action and combined reaction
     */
    default ComponentInteractionsPair<C> reactionAndThen(SerializableBiConsumer<C, AjaxRequestTarget> nextOnAfter) {
        return new ComponentInteractionsPair<>() {
            @Override
            public void action(C component, AjaxRequestTarget request) {
                ComponentInteractionsPair.this.action(component, request);
            }

            @Override
            public void reaction(C component, AjaxRequestTarget request) {
                ComponentInteractionsPair.this.reaction(component, request);
                nextOnAfter.accept(component, request);
            }
        };
    }
}
