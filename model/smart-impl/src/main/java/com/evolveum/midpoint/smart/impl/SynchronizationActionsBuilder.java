/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl;

import com.evolveum.midpoint.smart.api.DirectionScenario;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class SynchronizationActionsBuilder {
    private final SynchronizationActionsType actions = new SynchronizationActionsType();

    SynchronizationActionsBuilder synchronize(int order) {
        var a = new SynchronizeSynchronizationActionType();
        a.setOrder(order);
        actions.getSynchronize().add(a);
        return this;
    }

    SynchronizationActionsBuilder link(int order) {
        var a = new LinkSynchronizationActionType();
        a.setOrder(order);
        actions.getLink().add(a);
        return this;
    }

    SynchronizationActionsBuilder addFocus(int order) {
        var a = new AddFocusSynchronizationActionType();
        a.setOrder(order);
        actions.getAddFocus().add(a);
        return this;
    }

    SynchronizationActionsBuilder deleteFocus(int order) {
        var a = new DeleteFocusSynchronizationActionType();
        a.setOrder(order);
        actions.getDeleteFocus().add(a);
        return this;
    }

    SynchronizationActionsBuilder inactivateFocus(int order) {
        var a = new InactivateFocusSynchronizationActionType();
        a.setOrder(order);
        actions.getInactivateFocus().add(a);
        return this;
    }

    SynchronizationActionsBuilder deleteResourceObject(int order) {
        var a = new DeleteResourceObjectSynchronizationActionType();
        a.setOrder(order);
        actions.getDeleteResourceObject().add(a);
        return this;
    }

    SynchronizationActionsBuilder inactivateResourceObject(int order) {
        var a = new InactivateResourceObjectSynchronizationActionType();
        a.setOrder(order);
        actions.getInactivateResourceObject().add(a);
        return this;
    }

    SynchronizationActionsBuilder createCorrelationCase(int order) {
        var a = new CreateCorrelationCaseSynchronizationActionType();
        a.setOrder(order);
        actions.getCreateCorrelationCase().add(a);
        return this;
    }

    SynchronizationActionsType build() {
        return actions;
    }

    /**
     * Builds predefined synchronization reactions for the given direction scenario.
     * Intended for use under schemaHandling/objectType/synchronizationReactions.
     */
    public static SynchronizationReactionsType getPredefinedSynchronizationReactions(
            DirectionScenario scenario, boolean includeCorrelationCaseAction) {
        var reactions = new SynchronizationReactionsType();

        switch (scenario) {
            case SOURCE:
            case SOURCE_WITH_FEEDBACK:
                // UNMATCHED: default addFocus
                reactions.getReaction().add(
                        reaction(SynchronizationSituationType.UNMATCHED,
                                actionsBuilder()
                                        .addFocus(1)
                                        .build()));

                // DELETED: deleteFocus, inactivateFocus, synchronize
                reactions.getReaction().add(
                        reaction(SynchronizationSituationType.DELETED,
                                actionsBuilder()
                                        .deleteFocus(1)
                                        .inactivateFocus(2)
                                        .synchronize(3)
                                        .build()));

                // UNLINKED: link
                reactions.getReaction().add(
                        reaction(SynchronizationSituationType.UNLINKED,
                                actionsBuilder()
                                        .link(1)
                                        .build()));

                // LINKED: synchronize
                reactions.getReaction().add(
                        reaction(SynchronizationSituationType.LINKED,
                                actionsBuilder()
                                        .synchronize(1)
                                        .build()));
                break;

            case TARGET:
            case TARGET_WITH_FEEDBACK:
                // UNMATCHED: inactivate/delete resource object
                reactions.getReaction().add(
                        reaction(SynchronizationSituationType.UNMATCHED,
                                actionsBuilder()
                                        .inactivateResourceObject(1)
                                        .deleteResourceObject(2)
                                        .build()));

                // DELETED: default synchronize
                reactions.getReaction().add(
                        reaction(SynchronizationSituationType.DELETED,
                                actionsBuilder()
                                        .synchronize(1)
                                        .build()));

                // UNLINKED: link
                reactions.getReaction().add(
                        reaction(SynchronizationSituationType.UNLINKED,
                                actionsBuilder()
                                        .link(1)
                                        .build()));

                // LINKED: synchronize
                reactions.getReaction().add(
                        reaction(SynchronizationSituationType.LINKED,
                                actionsBuilder()
                                        .synchronize(1)
                                        .build()));

                // DISPUTED: default createCorrelationCase (if requested)
                if (includeCorrelationCaseAction) {
                    reactions.getReaction().add(
                            reaction(SynchronizationSituationType.DISPUTED,
                                    actionsBuilder()
                                            .createCorrelationCase(1)
                                            .build()));
                }
                break;

            default:
                // No predefined reactions; return empty container
                break;
        }

        return reactions;
    }

    private static SynchronizationReactionType reaction(
            SynchronizationSituationType situation, SynchronizationActionsType actions) {
        return new SynchronizationReactionType()
                .situation(situation)
                .actions(actions);
    }

    private static SynchronizationActionsBuilder actionsBuilder() {
        return new SynchronizationActionsBuilder();
    }
}
