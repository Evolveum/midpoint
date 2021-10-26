/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.state;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityStatePersistenceType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 * TODO better name
 *
 * Defines basic facts about activity work state, like type of the work state, the persistence level, and so on.
 */
public class ActivityStateDefinition<WS extends AbstractActivityWorkStateType> {

    @NotNull private final QName workStateTypeName;

    @NotNull private final ActivityStatePersistenceType persistence;

    public ActivityStateDefinition(@NotNull QName workStateTypeName, @NotNull ActivityStatePersistenceType persistence) {
        this.workStateTypeName = workStateTypeName;
        this.persistence = persistence;
    }

    public static <WS extends AbstractActivityWorkStateType> ActivityStateDefinition<WS> normal() {
        return normal(AbstractActivityWorkStateType.COMPLEX_TYPE);
    }

    public static <WS extends AbstractActivityWorkStateType> ActivityStateDefinition<WS> normal(@NotNull QName workStateTypeName) {
        return new ActivityStateDefinition<>(workStateTypeName, ActivityStatePersistenceType.SINGLE_REALIZATION);
    }

    public static <WS extends AbstractActivityWorkStateType> ActivityStateDefinition<WS> perpetual() {
        return perpetual(AbstractActivityWorkStateType.COMPLEX_TYPE);
    }
    public static <WS extends AbstractActivityWorkStateType> ActivityStateDefinition<WS> perpetual(
            @NotNull QName workStateTypeName) {
        return new ActivityStateDefinition<>(workStateTypeName, ActivityStatePersistenceType.PERPETUAL);
    }

    public @NotNull QName getWorkStateTypeName() {
        return workStateTypeName;
    }

    public @NotNull ActivityStatePersistenceType getPersistence() {
        return persistence;
    }

    public boolean isSingleRealization() {
        return getPersistence() == ActivityStatePersistenceType.SINGLE_REALIZATION;
    }
}
