/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.util.SimulationUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Wraps both {@link LegacySynchronizationReactionType} and {@link SynchronizationReactionType}.
 */
public abstract class SynchronizationReactionDefinition implements Comparable<SynchronizationReactionDefinition> {

    private static final Comparator<SynchronizationReactionDefinition> COMPARATOR =
            Comparator.comparing(
                    SynchronizationReactionDefinition::getOrder,
                    Comparator.nullsLast(Comparator.naturalOrder()));

    @Nullable private final Integer order;
    @Nullable private final String name;
    @Nullable private final String lifecycleState;
    @NotNull private final Collection<String> channels;
    @Nullable private final ExpressionType condition;

    private SynchronizationReactionDefinition(
            @Nullable Integer order,
            @Nullable String name,
            @Nullable String lifecycleState,
            @NotNull Collection<String> channels,
            @Nullable ExpressionType condition) {
        this.order = order;
        this.name = name;
        this.lifecycleState = lifecycleState;
        this.channels = channels;
        this.condition = condition;
    }

    private SynchronizationReactionDefinition(AbstractSynchronizationReactionType bean) {
        this.order = bean.getOrder();
        this.name = bean.getName();
        this.lifecycleState = bean.getLifecycleState();
        this.channels = List.of();
        this.condition = null;
    }

    private SynchronizationReactionDefinition(SynchronizationReactionType bean) {
        this.order = bean.getOrder();
        this.name = bean.getName();
        this.lifecycleState = bean.getLifecycleState();
        this.channels = bean.getChannel();
        this.condition = bean.getCondition();
    }

    public static @NotNull SynchronizationReactionDefinition.ObjectSynchronizationReactionDefinition legacy(
            @NotNull LegacySynchronizationReactionType legacyBean,
            boolean addCreateCasesAction,
            @NotNull ClockworkSettings defaultSettings)
            throws ConfigurationException {
        return new ObjectSynchronizationReactionDefinition(legacyBean, addCreateCasesAction, defaultSettings);
    }

    private static @NotNull SynchronizationReactionDefinition.ObjectSynchronizationReactionDefinition modern(
            @NotNull SynchronizationReactionType bean,
            @NotNull ClockworkSettings defaultSettings) {
        return new ObjectSynchronizationReactionDefinition(bean, defaultSettings);
    }

    private static @NotNull SynchronizationReactionDefinition.ItemSynchronizationReactionDefinition itemLevel(@NotNull ItemSynchronizationReactionType bean) {
        return new ItemSynchronizationReactionDefinition(bean);
    }

    private static @NotNull List<ObjectSynchronizationReactionDefinition> modern(
            @NotNull SynchronizationReactionsType reactionsBean,
            @NotNull ClockworkSettings reactionLevelSettings) {
        return reactionsBean.getReaction().stream()
                .map(bean -> modern(bean, reactionLevelSettings))
                .collect(Collectors.toList());
    }

    public static @NotNull List<ObjectSynchronizationReactionDefinition> modern(SynchronizationReactionsType reactions) {
        if (reactions == null) {
            return List.of();
        } else {
            ClockworkSettings reactionLevelSettings = ClockworkSettings.of(reactions.getDefaultSettings());
            return modern(reactions, reactionLevelSettings);
        }
    }

    public static @NotNull List<ItemSynchronizationReactionDefinition> itemLevel(ItemSynchronizationReactionsType reactions) {
        if (reactions == null) {
            return List.of();
        } else {
            return reactions.getReaction().stream()
                    .map(bean -> itemLevel(bean))
                    .toList();
        }
    }

    @Override
    public int compareTo(@NotNull SynchronizationReactionDefinition o) {
        return COMPARATOR.compare(this, o);
    }

    public @Nullable String getName() {
        return name;
    }

    public @Nullable Integer getOrder() {
        return order;
    }

    public @NotNull Collection<String> getChannels() {
        return channels;
    }

    public @Nullable ExpressionType getCondition() {
        return condition;
    }

    public boolean isVisible(TaskExecutionMode mode) {
        return SimulationUtil.isVisible(lifecycleState, mode);
    }

    public static class ObjectSynchronizationReactionDefinition extends SynchronizationReactionDefinition {

        @NotNull private final Set<SynchronizationSituationType> situations;

        private final boolean legacySynchronizeOff;

        /** These actions are appropriately ordered. */
        @NotNull private final List<SynchronizationActionDefinition> actions;

        ObjectSynchronizationReactionDefinition(
                @NotNull LegacySynchronizationReactionType legacyBean,
                boolean addCreateCasesAction,
                @NotNull ClockworkSettings syncLevelSettings)
                throws ConfigurationException {
            super(null, legacyBean.getName(), null, legacyBean.getChannel(), legacyBean.getCondition());
            this.situations = Set.of(
                    MiscUtil.requireNonNull(
                            legacyBean.getSituation(),
                            () -> new ConfigurationException("Situation is not defined in " + legacyBean)));
            this.legacySynchronizeOff =
                    !addCreateCasesAction &&
                            (Boolean.FALSE.equals(legacyBean.isSynchronize())
                                    || legacyBean.isSynchronize() == null && legacyBean.getAction().isEmpty());
            ClockworkSettings reactionLevelSettings = syncLevelSettings.updateFrom(legacyBean);
            this.actions = createActions(legacyBean, reactionLevelSettings);
            // Instead of "<cases>" in the correlation definition we create a special "create correlation cases" action.
            // We are sure that no such action is there yet, as it's not possible to formulate such an action using action URIs.
            if (addCreateCasesAction) {
                actions.add(new SynchronizationActionDefinition.New(
                        new CreateCorrelationCaseSynchronizationActionType(),
                        ClockworkSettings.empty()));
            }
        }

        private List<SynchronizationActionDefinition> createActions(
                @NotNull LegacySynchronizationReactionType bean,
                @NotNull ClockworkSettings defaultSettings) {
            if (bean.getAction().isEmpty()) {
                if (Boolean.TRUE.equals(bean.isSynchronize())) {
                    return List.of(
                            new SynchronizationActionDefinition.New(
                                    new SynchronizeSynchronizationActionType(), defaultSettings));
                }
            }
            // Intentionally not sorting these (there's no order in legacy configuration).
            return bean.getAction().stream()
                    .map(a -> new SynchronizationActionDefinition.Legacy(a, defaultSettings))
                    .collect(Collectors.toList());
        }

        private ObjectSynchronizationReactionDefinition(
                @NotNull SynchronizationReactionType bean, @NotNull ClockworkSettings syncLevelSettings) {
            super(bean);
            this.situations = new HashSet<>(bean.getSituation());
            this.legacySynchronizeOff = false;
            this.actions = createActions(bean, syncLevelSettings);
        }

        private List<SynchronizationActionDefinition> createActions(
                @NotNull SynchronizationReactionType bean,
                @NotNull ClockworkSettings syncLevelSettings) {
            return getAllActionBeans(bean).stream()
                    .map(a -> new SynchronizationActionDefinition.New(a, syncLevelSettings))
                    .sorted()
                    .collect(Collectors.toList());
        }

        private List<AbstractSynchronizationActionType> getAllActionBeans(@NotNull SynchronizationReactionType reaction) {
            SynchronizationActionsType actions = reaction.getActions();
            if (actions == null) {
                return List.of();
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
            // TODO support extensions
            return all;
        }

        public @NotNull Set<SynchronizationSituationType> getSituations() {
            return situations;
        }

        public boolean matchesSituation(@NotNull SynchronizationSituationType currentSituation) {
            return situations.isEmpty() || situations.contains(currentSituation);
        }

        public @NotNull List<SynchronizationActionDefinition> getActions() {
            return actions;
        }

        public boolean isLegacySynchronizeOff() {
            return legacySynchronizeOff;
        }

        @Override
        public String toString() {
            return "ObjectSynchronizationReactionDefinition{" +
                    "name='" + getName() + '\'' +
                    ", order=" + getOrder() +
                    ", situations=" + situations +
                    ", # of actions: " + actions.size() +
                    '}';
        }
    }

    public static class ItemSynchronizationReactionDefinition extends SynchronizationReactionDefinition {

        @NotNull private final ItemSynchronizationReactionType bean;
        @NotNull private final List<AbstractSynchronizationActionType> actions;

        private ItemSynchronizationReactionDefinition(@NotNull ItemSynchronizationReactionType bean) {
            super(bean);
            this.bean = bean;
            this.actions = getAllActionBeans(bean);
        }

        private static List<AbstractSynchronizationActionType> getAllActionBeans(
                @NotNull ItemSynchronizationReactionType reaction) {
            ItemSynchronizationActionsType actions = reaction.getActions();
            if (actions == null) {
                return List.of();
            }
            List<AbstractSynchronizationActionType> all = new ArrayList<>();
            all.addAll(actions.getSynchronize());
            all.addAll(actions.getAddFocusValue());
            all.addAll(actions.getDeleteFocusValue());
            all.addAll(actions.getInactivateFocusValue());
            all.addAll(actions.getDeleteResourceObjectValue());
            all.addAll(actions.getInactivateResourceObjectValue());
            // TODO support extensions
            return all;
        }

        public @NotNull List<? extends AbstractSynchronizationActionType> getActions() {
            return actions;
        }

        private List<ItemSynchronizationSituationType> getSituations() {
            return bean.getSituation();
        }

        public boolean matchesSituation(@NotNull ItemSynchronizationSituationType currentSituation) {
            var situations = getSituations();
            return situations.isEmpty() || situations.contains(currentSituation);
        }

        @Override
        public String toString() {
            return "ObjectSynchronizationReactionDefinition{" +
                    "name='" + getName() + '\'' +
                    ", order=" + getOrder() +
                    ", situations=" + getSituations() +
                    ", # of actions: " + actions.size() +
                    '}';
        }
    }
}
