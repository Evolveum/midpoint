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
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Comparator;

import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

/**
 * Wraps both {@link SynchronizationActionType} and {@link AbstractSynchronizationActionType}.
 */
public abstract class SynchronizationActionDefinition implements Comparable<SynchronizationActionDefinition> {

    private static final Comparator<SynchronizationActionDefinition> COMPARATOR =
            Comparator.comparing(
                    SynchronizationActionDefinition::getOrder,
                    Comparator.nullsLast(Comparator.naturalOrder()));

    @NotNull private final ClockworkSettings clockworkSettings;

    SynchronizationActionDefinition(@NotNull ClockworkSettings defaultSettings) {
        this.clockworkSettings = defaultSettings;
    }

    @Override
    public int compareTo(@NotNull SynchronizationActionDefinition o) {
        return COMPARATOR.compare(this, o);
    }

    public abstract @Nullable Integer getOrder();

    public abstract @Nullable String getName();

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

    public abstract @Nullable String getLegacyActionUri();

    public abstract @Nullable Class<? extends AbstractSynchronizationActionType> getNewDefinitionBeanClass();

    public @NotNull Class<? extends AbstractSynchronizationActionType> getNewDefinitionBeanClassRequired() {
        return stateNonNull(getNewDefinitionBeanClass(),
                "Action definition bean class not present in %s", this);
    }

    @VisibleForTesting
    public abstract @Nullable AbstractSynchronizationActionType getNewDefinitionBean();

    public static class New extends SynchronizationActionDefinition {

        @NotNull private final AbstractSynchronizationActionType bean;

        public New(
                @NotNull AbstractSynchronizationActionType bean,
                @NotNull ClockworkSettings defaultSettings) {
            super(bean instanceof AbstractClockworkBasedSynchronizationActionType ?
                    defaultSettings.updateFrom((AbstractClockworkBasedSynchronizationActionType) bean) :
                    defaultSettings);
            this.bean = bean;
        }

        @Override
        public @Nullable Integer getOrder() {
            return bean.getOrder();
        }

        @Override
        public @Nullable String getName() {
            return bean.getName();
        }

        @Override
        public @Nullable String getLegacyActionUri() {
            return null;
        }

        @Override
        public @Nullable Class<? extends AbstractSynchronizationActionType> getNewDefinitionBeanClass() {
            return bean.getClass();
        }

        @Override
        public @Nullable AbstractSynchronizationActionType getNewDefinitionBean() {
            return bean;
        }

        @Override
        public String toString() {
            return "SynchronizationActionDefinition.New{" +
                    "bean:" + bean.getClass().getSimpleName() + "=" + bean +
                    "}";
        }
    }

    public static class Legacy extends SynchronizationActionDefinition {

        @NotNull private final SynchronizationActionType legacyBean;

        public Legacy(
                @NotNull SynchronizationActionType legacyBean,
                @NotNull ClockworkSettings defaultSettings) {
            super(defaultSettings);
            this.legacyBean = legacyBean;
        }

        @Override
        public @Nullable Integer getOrder() {
            return null;
        }

        @Override
        public @Nullable String getName() {
            return legacyBean.getName();
        }

        @Override
        public @Nullable String getLegacyActionUri() {
            return legacyBean.getHandlerUri();
        }

        @Override
        public @Nullable Class<? extends AbstractSynchronizationActionType> getNewDefinitionBeanClass() {
            return null;
        }

        @Override
        public @Nullable AbstractSynchronizationActionType getNewDefinitionBean() {
            return null;
        }

        @Override
        public String toString() {
            return "SynchronizationActionDefinition.Legacy{" +
                    "legacyBean:" + legacyBean.getClass().getSimpleName() + "=" + legacyBean +
                    "}";
        }
    }
}
