/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator;

import com.evolveum.midpoint.util.DebugDumpable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractCorrelatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CompositeCorrelatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSynchronizationType;

/**
 * Overall context in which the correlator works.
 *
 * Differs from {@link CorrelationContext} in that the latter covers only a single correlation operation.
 * The former covers the whole life of a correlator, and operations other than correlation.
 */
public class CorrelatorContext<C extends AbstractCorrelatorType> implements DebugDumpable {

    @NotNull private final C configurationBean;

    @Nullable private final CorrelatorConfiguration configuration;

    @Nullable private final ObjectSynchronizationType synchronizationBean;

    public CorrelatorContext(
            @NotNull C configurationBean,
            @Nullable CorrelatorConfiguration configuration,
            @Nullable ObjectSynchronizationType synchronizationBean) {
        this.configurationBean = configurationBean;
        this.configuration = configuration;
        this.synchronizationBean = synchronizationBean;
    }

    public CorrelatorContext(
            @NotNull CorrelatorConfiguration configuration,
            @Nullable ObjectSynchronizationType synchronizationBean) {
        //noinspection unchecked
        this.configurationBean = (C) configuration.getConfigurationBean();
        this.configuration = configuration;
        this.synchronizationBean = synchronizationBean;
    }

    public static CorrelatorContext<?> create(
            @NotNull CompositeCorrelatorType correlators,
            @Nullable ObjectSynchronizationType objectSynchronizationBean) {
        return new CorrelatorContext<>(
                CorrelatorConfiguration.getConfiguration(correlators),
                objectSynchronizationBean);
    }

    @VisibleForTesting
    public static CorrelatorContext<?> create(@NotNull AbstractCorrelatorType configBean) {
        return new CorrelatorContext<>(
                CorrelatorConfiguration.typed(configBean), null);
    }

    public @NotNull C getConfigurationBean() {
        return configurationBean;
    }

    public @Nullable CorrelatorConfiguration getConfiguration() {
        return configuration;
    }

    public @Nullable ObjectSynchronizationType getSynchronizationBean() {
        return synchronizationBean;
    }

    public CorrelatorContext<?> spawn(@NotNull CorrelatorConfiguration configuration) {
        return new CorrelatorContext<>(configuration, synchronizationBean);
    }

    public boolean shouldCreateCases() {
        return synchronizationBean != null
                && synchronizationBean.getCorrelationDefinition() != null
                && synchronizationBean.getCorrelationDefinition().getCases() != null
                && !Boolean.FALSE.equals(synchronizationBean.getCorrelationDefinition().getCases().isEnabled());
    }

    @Override
    public String debugDump(int indent) {
        // Temporary: this config bean is the core of the context; other things need not be so urgently dumped
        // (maybe they might be - in some shortened form).
        return configurationBean.debugDump(indent);
    }
}
