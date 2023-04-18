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
public class SynchronizationReactionDefinition implements Comparable<SynchronizationReactionDefinition> {

    private static final Comparator<SynchronizationReactionDefinition> COMPARATOR =
            Comparator.comparing(
                    SynchronizationReactionDefinition::getOrder,
                    Comparator.nullsLast(Comparator.naturalOrder()));

    private final Integer order;
    private final String name;
    @Nullable private final String lifecycleState;
    @NotNull private final Set<SynchronizationSituationType> situations;
    @NotNull private final Collection<String> channels;
    private final ExpressionType condition;
    private final boolean legacySynchronizeOff;

    /** These actions are appropriately ordered. */
    @NotNull private final List<SynchronizationActionDefinition> actions;

    private SynchronizationReactionDefinition(
            @NotNull LegacySynchronizationReactionType legacyBean,
            boolean addCreateCasesAction,
            @NotNull ClockworkSettings syncLevelSettings)
            throws ConfigurationException {
        this.order = null;
        this.name = legacyBean.getName();
        this.lifecycleState = null;
        this.situations = Set.of(
                MiscUtil.requireNonNull(
                        legacyBean.getSituation(),
                        () -> new ConfigurationException("Situation is not defined in " + legacyBean)));
        this.channels = legacyBean.getChannel();
        this.condition = legacyBean.getCondition();
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

    private SynchronizationReactionDefinition(
            @NotNull SynchronizationReactionType bean, @NotNull ClockworkSettings syncLevelSettings) {
        this.order = bean.getOrder();
        this.name = bean.getName();
        this.lifecycleState = bean.getLifecycleState();
        this.situations = new HashSet<>(bean.getSituation());
        this.channels = bean.getChannel();
        this.condition = bean.getCondition();
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

    public static @NotNull SynchronizationReactionDefinition of(
            @NotNull LegacySynchronizationReactionType legacyBean,
            boolean addCreateCasesAction,
            @NotNull ClockworkSettings defaultSettings)
            throws ConfigurationException {
        return new SynchronizationReactionDefinition(legacyBean, addCreateCasesAction, defaultSettings);
    }

    public static @NotNull SynchronizationReactionDefinition of(
            @NotNull SynchronizationReactionType bean,
            @NotNull ClockworkSettings defaultSettings) {
        return new SynchronizationReactionDefinition(bean, defaultSettings);
    }

    @Override
    public int compareTo(@NotNull SynchronizationReactionDefinition o) {
        return COMPARATOR.compare(this, o);
    }

    public boolean matchesSituation(@NotNull SynchronizationSituationType currentSituation) {
        return situations.isEmpty() || situations.contains(currentSituation);
    }

    public String getName() {
        return name;
    }

    public Integer getOrder() {
        return order;
    }

    public @NotNull Set<SynchronizationSituationType> getSituations() {
        return situations;
    }

    public @NotNull Collection<String> getChannels() {
        return channels;
    }

    public ExpressionType getCondition() {
        return condition;
    }

    public boolean isLegacySynchronizeOff() {
        return legacySynchronizeOff;
    }

    public @NotNull List<SynchronizationActionDefinition> getActions() {
        return actions;
    }

    @Override
    public String toString() {
        return "SynchronizationReactionDefinition{" +
                "name='" + name + '\'' +
                ", order=" + order +
                ", situations=" + situations +
                ", # of actions: " + actions.size() +
                '}';
    }

    public boolean isVisible(TaskExecutionMode mode) {
        return SimulationUtil.isVisible(lifecycleState, mode);
    }
}
