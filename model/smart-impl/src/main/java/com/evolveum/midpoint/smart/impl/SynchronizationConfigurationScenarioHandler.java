/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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

    /** Builds reactions from SOURCE scenario answers. */
    public static SynchronizationReactionsType getSynchronizationReactionsFromSource(
            SourceSynchronizationAnswers answers) {
        var reactions = new SynchronizationReactionsType();

        if (answers != null) {
            UnmatchedSourceChoice unmatched = answers.getUnmatched();
            if (unmatched != null) {
                SynchronizationActionsType actions;
                switch (unmatched) {
                    case ADD_FOCUS:
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
            DeletedSourceChoice deleted = answers.getDeleted();
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

    /** Builds reactions from TARGET scenario answers. */
    public static SynchronizationReactionsType getSynchronizationReactionsFromTarget(
            TargetSynchronizationAnswers answers) {
        var reactions = new SynchronizationReactionsType();

        if (answers != null) {
            UnmatchedTargetChoice unmatched = answers.getUnmatched();
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
                    case DO_NOTHING:
                    default:
                        actions = new SynchronizationActionsType();
                        break;
                }
                reactions.getReaction().add(
                        reaction(SynchronizationSituationType.UNMATCHED, actions));
            }
            DeletedTargetChoice deleted = answers.getDeleted();
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
            DisputedTargetChoice disputed = answers.getDisputed();
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
                .name(deriveReactionName(situation, actions))
                .actions(actions);
    }

    private static String deriveReactionName(SynchronizationSituationType situation, SynchronizationActionsType actions) {
        return capitalizeEnumName(situation.name()) + " - " + buildActionsName(actions);
    }

    private static String buildActionsName(SynchronizationActionsType actions) {
        if (actions == null) {
            return "Do nothing";
        }
        List<AbstractSynchronizationActionType> all = new ArrayList<>();
        all.addAll(actions.getSynchronize());
        all.addAll(actions.getLink());
        all.addAll(actions.getUnlink());
        all.addAll(actions.getAddFocus());
        all.addAll(actions.getDeleteFocus());
        all.addAll(actions.getInactivateFocus());
        all.addAll(actions.getDeleteResourceObject());
        all.addAll(actions.getInactivateResourceObject());
        all.addAll(actions.getCreateCorrelationCase());
        if (all.isEmpty()) {
            return "Do nothing";
        }
        all.sort(Comparator.comparing(a -> Optional.ofNullable(a.getOrder()).orElse(Integer.MAX_VALUE)));
        List<String> names = new ArrayList<>(all.size());
        for (AbstractSynchronizationActionType a : all) {
            names.add(a.getName());
        }
        return String.join(", ", names);
    }

    private static String capitalizeEnumName(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        String[] parts = s.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty())
                continue;
            if (!sb.isEmpty())
                sb.append('-');
            sb.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1));
        }
        return sb.toString();
    }

}
