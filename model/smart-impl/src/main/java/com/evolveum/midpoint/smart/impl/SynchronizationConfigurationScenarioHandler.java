package com.evolveum.midpoint.smart.impl;

import com.evolveum.midpoint.smart.api.SynchronizationConfigurationScenario;
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
            case SOURCE_WITH_FEEDBACK:
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
            case TARGET_WITH_FEEDBACK:
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

    private static SynchronizationReactionType reaction(
            SynchronizationSituationType situation, SynchronizationActionsType actions) {
        return new SynchronizationReactionType()
                .situation(situation)
                .actions(actions);
    }
}
