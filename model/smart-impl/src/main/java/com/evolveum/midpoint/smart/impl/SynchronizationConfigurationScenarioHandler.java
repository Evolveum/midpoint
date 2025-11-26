/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl;

import com.evolveum.midpoint.smart.api.synchronization.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class SynchronizationConfigurationScenarioHandler {
    /**
     * Builds predefined synchronization reactions for the given direction scenario.
     * Intended for use under schemaHandling/objectType/synchronizationReactions.
     */
    public static SynchronizationReactionsType getPredefinedSynchronizationReactions(
            SynchronizationConfigurationScenario scenario, boolean includeCorrelationCaseAction) {
        var reactions = new SynchronizationReactionsType();

        switch (scenario) {
            case SOURCE:
                // UNMATCHED: default addFocus
                reactions.getReaction().add(
                        reaction(SynchronizationSituationType.UNMATCHED,
                                new SynchronizationActionsBuilder()
                                        .addFocus()
                                        .build()));

                // DELETED: deleteFocus, inactivateFocus, synchronize
                reactions.getReaction().add(
                        reaction(SynchronizationSituationType.DELETED,
                                new SynchronizationActionsBuilder()
                                        .deleteFocus()
                                        .inactivateFocus()
                                        .synchronize()
                                        .build()));

                // UNLINKED: link
                reactions.getReaction().add(
                        reaction(SynchronizationSituationType.UNLINKED,
                                new SynchronizationActionsBuilder()
                                        .link()
                                        .build()));

                // LINKED: synchronize
                reactions.getReaction().add(
                        reaction(SynchronizationSituationType.LINKED,
                                new SynchronizationActionsBuilder()
                                        .synchronize()
                                        .build()));
                break;

            case TARGET:
                // UNMATCHED: inactivate/delete resource object
                reactions.getReaction().add(
                        reaction(SynchronizationSituationType.UNMATCHED,
                                new SynchronizationActionsBuilder()
                                        .inactivateResourceObject()
                                        .deleteResourceObject()
                                        .build()));

                // DELETED: default synchronize
                reactions.getReaction().add(
                        reaction(SynchronizationSituationType.DELETED,
                                new SynchronizationActionsBuilder()
                                        .synchronize()
                                        .build()));

                // UNLINKED: link
                reactions.getReaction().add(
                        reaction(SynchronizationSituationType.UNLINKED,
                                new SynchronizationActionsBuilder()
                                        .link()
                                        .build()));

                // LINKED: synchronize
                reactions.getReaction().add(
                        reaction(SynchronizationSituationType.LINKED,
                                new SynchronizationActionsBuilder()
                                        .synchronize()
                                        .build()));

                // DISPUTED: default createCorrelationCase (if requested)
                if (includeCorrelationCaseAction) {
                    reactions.getReaction().add(
                            reaction(SynchronizationSituationType.DISPUTED,
                                    new SynchronizationActionsBuilder()
                                            .createCorrelationCase()
                                            .build()));
                }
                break;

            default:
                // No predefined reactions; return empty container
                break;
        }

        return reactions;
    }

    public static SynchronizationReactionsType getSynchronizationReactionsFromAnswers(
            SynchronizationConfigurationScenario scenario, SynchronizationAnswers answers) {
        switch (scenario) {
            case SOURCE:
                return getSynchronizationReactionsFromSource(answers);
            case TARGET:
                return getSynchronizationReactionsFromTarget(answers);
        }
        return null;
    }

    private static SynchronizationReactionsType getSynchronizationReactionsFromSource(SynchronizationAnswers answers) {
        var reactions = new SynchronizationReactionsType();

        if (answers != null) {
            UnmatchedChoice unmatched = answers.getUnmatched();
            if (unmatched != null) {
                SynchronizationActionsType actions;
                switch (unmatched) {
                    case IMPORT_TO_MIDPOINT:
                        actions = new SynchronizationActionsBuilder()
                                .addFocus()
                                .build();
                        break;
                    case DO_NOTHING:
                    default:
                        actions = new SynchronizationActionsType();
                        break;
                }
                reactions.getReaction().add(
                        reaction(SynchronizationSituationType.UNMATCHED, actions));
            }
            DeletedChoice deleted = answers.getDeleted();
            if (deleted != null) {
                SynchronizationActionsType actions;
                switch (deleted) {
                    case DELETE_FOCUS:
                        actions = new SynchronizationActionsBuilder()
                                .deleteFocus()
                                .build();
                        break;
                    case DISABLE_FOCUS:
                        actions = new SynchronizationActionsBuilder()
                                .inactivateFocus()
                                .build();
                        break;
                    case REMOVE_BROKEN_LINK:
                        actions = new SynchronizationActionsBuilder()
                                .synchronize()
                                .build();
                        break;
                    case DO_NOTHING:
                    default:
                        actions = new SynchronizationActionsType();
                        break;
                }
                reactions.getReaction().add(
                        reaction(SynchronizationSituationType.DELETED, actions));
            }
        }
        reactions.getReaction().add(
                reaction(SynchronizationSituationType.UNLINKED,
                        new SynchronizationActionsBuilder().link().build()));
        reactions.getReaction().add(
                reaction(SynchronizationSituationType.LINKED,
                        new SynchronizationActionsBuilder().synchronize().build()));

        return reactions;
    }

    private static SynchronizationReactionsType getSynchronizationReactionsFromTarget(SynchronizationAnswers answers) {
        var reactions = new SynchronizationReactionsType();

        if (answers != null) {
            UnmatchedChoice unmatched = answers.getUnmatched();
            if (unmatched != null) {
                SynchronizationActionsType actions;
                switch (unmatched) {
                    case DELETE_RESOURCE_OBJECT:
                        actions = new SynchronizationActionsBuilder()
                                .deleteResourceObject()
                                .build();
                        break;
                    case DISABLE_RESOURCE_OBJECT:
                        actions = new SynchronizationActionsBuilder()
                                .inactivateResourceObject()
                                .build();
                        break;
                    default:
                        actions = new SynchronizationActionsType();
                        break;
                }
                reactions.getReaction().add(
                        reaction(SynchronizationSituationType.UNMATCHED, actions));
            }
            DeletedChoice deleted = answers.getDeleted();
            if (deleted != null) {
                SynchronizationActionsType actions;
                switch (deleted) {
                    case REMOVE_BROKEN_LINK:
                        actions = new SynchronizationActionsBuilder()
                                .synchronize()
                                .build();
                        break;
                    case DO_NOTHING:
                    default:
                        actions = new SynchronizationActionsType();
                        break;
                }
                reactions.getReaction().add(
                        reaction(SynchronizationSituationType.DELETED, actions));
            }
            DisputedChoice disputed = answers.getDisputed();
            if (disputed != null) {
                SynchronizationActionsType actions;
                switch (disputed) {
                    case CREATE_CORRELATION_CASE:
                        actions = new SynchronizationActionsBuilder()
                                .createCorrelationCase()
                                .build();
                        break;
                    case DO_NOTHING:
                    default:
                        actions = new SynchronizationActionsType();
                        break;
                }
                reactions.getReaction().add(
                        reaction(SynchronizationSituationType.DISPUTED, actions));
            }
        }
        reactions.getReaction().add(
                reaction(SynchronizationSituationType.UNLINKED,
                        new SynchronizationActionsBuilder().link().build()));
        reactions.getReaction().add(
                reaction(SynchronizationSituationType.LINKED,
                        new SynchronizationActionsBuilder().synchronize().build()));

        return reactions;
    }

    private static SynchronizationReactionType reaction(
            SynchronizationSituationType situation, SynchronizationActionsType actions) {
        return new SynchronizationReactionType()
                .situation(situation)
                .actions(actions);
    }
}
