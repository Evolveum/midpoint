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

/**
 * Common clockwork-related settings for synchronization purposes.
 */
public class ClockworkSettings {

    private final Boolean reconcileAll;
    private final Boolean reconcile;
    private final Boolean limitPropagation;
    private final ModelExecuteOptionsType executeOptions;
    private final ObjectReferenceType objectTemplateRef;

    private ClockworkSettings(
            Boolean reconcileAll,
            Boolean reconcile,
            Boolean limitPropagation,
            ModelExecuteOptionsType executeOptions,
            ObjectReferenceType objectTemplateRef) {
        this.reconcileAll = reconcileAll;
        this.reconcile = reconcile;
        this.limitPropagation = limitPropagation;
        this.executeOptions = executeOptions;
        this.objectTemplateRef = objectTemplateRef;
    }

    private ClockworkSettings() {
        this(null, null, null, null, null);
    }

    public static ClockworkSettings of(@NotNull ObjectSynchronizationType syncBean) {
        return new ClockworkSettings(
                null,
                syncBean.isReconcile(),
                syncBean.isLimitPropagation(),
                null,
                syncBean.getObjectTemplateRef());
    }

    public static ClockworkSettings of(@Nullable SynchronizationReactionsDefaultSettingsType bean) {
        if (bean == null) {
            return new ClockworkSettings();
        } else {
            return new ClockworkSettings(
                    bean.isReconcileAll(),
                    bean.isReconcile(),
                    bean.isLimitPropagation(),
                    bean.getExecuteOptions(),
                    bean.getObjectTemplateRef());
        }
    }

    public static ClockworkSettings empty() {
        return new ClockworkSettings();
    }

    public Boolean getReconcileAll() {
        return reconcileAll;
    }

    public Boolean getReconcile() {
        return reconcile;
    }

    public Boolean getLimitPropagation() {
        return limitPropagation;
    }

    public ModelExecuteOptionsType getExecuteOptions() {
        return executeOptions;
    }

    public ObjectReferenceType getObjectTemplateRef() {
        return objectTemplateRef;
    }

    public ClockworkSettings reconcileAll(Boolean value) {
        return new ClockworkSettings(value, reconcile, limitPropagation, executeOptions, objectTemplateRef);
    }

    public ClockworkSettings reconcile(Boolean value) {
        return new ClockworkSettings(reconcileAll, value, limitPropagation, executeOptions, objectTemplateRef);
    }

    public ClockworkSettings limitPropagation(Boolean value) {
        return new ClockworkSettings(reconcileAll, reconcile, value, executeOptions, objectTemplateRef);
    }

    public ClockworkSettings executeOptions(ModelExecuteOptionsType value) {
        return new ClockworkSettings(reconcileAll, reconcile, limitPropagation, value, objectTemplateRef);
    }

    public ClockworkSettings objectTemplateRef(ObjectReferenceType value) {
        return new ClockworkSettings(reconcileAll, reconcile, limitPropagation, executeOptions, value);
    }

    @SuppressWarnings("DuplicatedCode")
    ClockworkSettings updateFrom(AbstractClockworkBasedSynchronizationActionType bean) {
        ClockworkSettings updated = this;
        if (bean.isReconcileAll() != null) {
            updated = updated.reconcileAll(bean.isReconcileAll());
        }
        if (bean.isReconcile() != null) {
            updated = updated.reconcile(bean.isReconcile());
        }
        if (bean.isLimitPropagation() != null) {
            updated = updated.limitPropagation(bean.isLimitPropagation());
        }
        if (bean.getExecuteOptions() != null) {
            updated = updated.executeOptions(bean.getExecuteOptions()); // TODO or merge them?
        }
        if (bean.getObjectTemplateRef() != null) {
            updated = updated.objectTemplateRef(bean.getObjectTemplateRef());
        }
        return updated;
    }

    @SuppressWarnings("DuplicatedCode")
    ClockworkSettings updateFrom(LegacySynchronizationReactionType bean) {
        ClockworkSettings updated = this;
        if (bean.isReconcileAll() != null) {
            updated = updated.reconcileAll(bean.isReconcileAll());
        }
        if (bean.isReconcile() != null) {
            updated = updated.reconcile(bean.isReconcile());
        }
        if (bean.isLimitPropagation() != null) {
            updated = updated.limitPropagation(bean.isLimitPropagation());
        }
        if (bean.getExecuteOptions() != null) {
            updated = updated.executeOptions(bean.getExecuteOptions()); // TODO or merge them?
        }
        if (bean.getObjectTemplateRef() != null) {
            updated = updated.objectTemplateRef(bean.getObjectTemplateRef());
        }
        return updated;
    }
}
