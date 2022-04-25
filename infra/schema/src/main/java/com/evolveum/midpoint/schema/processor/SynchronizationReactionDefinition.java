/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Wraps both {@link SynchronizationReactionType} and {@link SynchronizationReactionNewType}.
 */
public class SynchronizationReactionDefinition implements Comparable<SynchronizationReactionDefinition> {

    private static final Comparator<SynchronizationReactionDefinition> COMPARATOR =
            Comparator.comparing(
                    SynchronizationReactionDefinition::getOrder,
                    Comparator.nullsLast(Comparator.naturalOrder()));

    private final Integer order;
    private final String name;
    @NotNull private final Set<SynchronizationSituationType> situations;
    @NotNull private final Collection<String> channels;
    private final ExpressionType condition;
    private final boolean legacySynchronizeOff;

    /** These actions are appropriately ordered. */
    @NotNull private final List<SynchronizationActionDefinition> actions;

    private SynchronizationReactionDefinition(
            @NotNull SynchronizationReactionType bean, @NotNull ClockworkSettings syncLevelSettings)
            throws ConfigurationException {
        this.order = null;
        this.name = bean.getName();
        this.situations = Set.of(
                MiscUtil.requireNonNull(
                        bean.getSituation(),
                        () -> new ConfigurationException("Situation is not defined in " + bean)));
        this.channels = bean.getChannel();
        this.condition = bean.getCondition();
        this.legacySynchronizeOff =
                Boolean.FALSE.equals(bean.isSynchronize())
                        || bean.isSynchronize() == null && bean.getAction().isEmpty();
        ClockworkSettings reactionLevelSettings = syncLevelSettings.updateFrom(bean);
        this.actions = createActions(bean, reactionLevelSettings);
    }

    private List<SynchronizationActionDefinition> createActions(
            @NotNull SynchronizationReactionType bean,
            @NotNull ClockworkSettings defaultSettings) {
        if (bean.getAction().isEmpty()) {
            if (Boolean.TRUE.equals(bean.isSynchronize())) {
                return List.of(
                        new SynchronizationActionDefinition(
                                new SynchronizeSynchronizationActionType(), defaultSettings));
            }
        }
        // Intentionally not sorting these (there's no order in legacy configuration).
        return bean.getAction().stream()
                .map(a -> new SynchronizationActionDefinition(a, defaultSettings))
                .collect(Collectors.toList());
    }

    private SynchronizationReactionDefinition(
            @NotNull SynchronizationReactionNewType bean, @NotNull ClockworkSettings syncLevelSettings) {
        this.order = bean.getOrder();
        this.name = bean.getName();
        this.situations = new HashSet<>(bean.getSituation());
        this.channels = bean.getChannel();
        this.condition = bean.getCondition();
        this.legacySynchronizeOff = false;
        this.actions = createActions(bean, syncLevelSettings);
    }

    private List<SynchronizationActionDefinition> createActions(
            @NotNull SynchronizationReactionNewType bean,
            @NotNull ClockworkSettings syncLevelSettings) {
        return getAllActionBeans(bean).stream()
                .map(a -> new SynchronizationActionDefinition(a, syncLevelSettings))
                .sorted()
                .collect(Collectors.toList());
    }

    private List<AbstractSynchronizationActionType> getAllActionBeans(@NotNull SynchronizationReactionNewType reaction) {
        List<AbstractSynchronizationActionType> all = new ArrayList<>();
        all.addAll(reaction.getSynchronize());
        all.addAll(reaction.getLink());
        all.addAll(reaction.getUnlink());
        all.addAll(reaction.getAddFocus());
        all.addAll(reaction.getDeleteFocus());
        all.addAll(reaction.getInactivateFocus());
        all.addAll(reaction.getDeleteShadow());
        all.addAll(reaction.getInactivateShadow());
        // TODO support extensions
        return all;
    }

    public static @NotNull SynchronizationReactionDefinition of(
            @NotNull SynchronizationReactionType bean,
            @NotNull ClockworkSettings defaultSettings)
            throws ConfigurationException {
        return new SynchronizationReactionDefinition(bean, defaultSettings);
    }

    public static @NotNull SynchronizationReactionDefinition of(
            @NotNull SynchronizationReactionNewType bean,
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
}
