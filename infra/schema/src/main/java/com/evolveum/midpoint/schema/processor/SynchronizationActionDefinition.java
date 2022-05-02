/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

/**
 * Wraps both {@link SynchronizationActionType} and {@link AbstractSynchronizationActionType}.
 */
public class SynchronizationActionDefinition implements Comparable<SynchronizationActionDefinition> {

    private static final Comparator<SynchronizationActionDefinition> COMPARATOR =
            Comparator.comparing(
                    SynchronizationActionDefinition::getOrder,
                    Comparator.nullsLast(Comparator.naturalOrder()));

    @NotNull private final ClockworkSettings clockworkSettings;

    @Nullable private final SynchronizationActionType legacyBean;
    @Nullable private final AbstractSynchronizationActionType bean;

    SynchronizationActionDefinition(
            @NotNull SynchronizationActionType bean,
            @NotNull ClockworkSettings defaultSettings) {
        this.clockworkSettings = defaultSettings;
        this.legacyBean = bean;
        this.bean = null;
    }

    SynchronizationActionDefinition(
            @NotNull AbstractSynchronizationActionType bean,
            @NotNull ClockworkSettings defaultSettings) {
        this.clockworkSettings = bean instanceof AbstractClockworkBasedSynchronizationActionType ?
                defaultSettings.updateFrom((AbstractClockworkBasedSynchronizationActionType) bean) : defaultSettings;
        this.legacyBean = null;
        this.bean = bean;
    }

    @Override
    public int compareTo(@NotNull SynchronizationActionDefinition o) {
        return COMPARATOR.compare(this, o);
    }

    public @Nullable Integer getOrder() {
        return bean != null ? bean.getOrder() : null;
    }

    public Boolean isReconcileAll() {
        return clockworkSettings.getReconcileAll();
    }

    public ModelExecuteOptionsType getExecuteOptions() {
        return clockworkSettings.getExecuteOptions();
    }

    public Boolean isReconcile() {
        return clockworkSettings.getReconcile();
    }

    public Boolean isLimitPropagation() {
        return clockworkSettings.getLimitPropagation();
    }

    public ObjectReferenceType getObjectTemplateRef() {
        return clockworkSettings.getObjectTemplateRef();
    }

    public @Nullable String getLegacyActionUri() {
        return legacyBean != null ? legacyBean.getHandlerUri() : null;
    }

    public @Nullable Class<? extends AbstractSynchronizationActionType> getDefinitionBeanClass() {
        return bean != null ? bean.getClass() : null;
    }
}
