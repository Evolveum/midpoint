/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.run.state;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityStatePersistenceType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 * TODO better name
 *
 * Defines basic facts about activity work state, like:
 *
 * @param workStateTypeName type of the work state
 * @param persistence the persistence level
 */
public record ActivityStateDefinition(
        @NotNull QName workStateTypeName,
        @NotNull ActivityStatePersistenceType persistence) {

    public static ActivityStateDefinition normal() {
        return normal(AbstractActivityWorkStateType.COMPLEX_TYPE);
    }

    public static ActivityStateDefinition normal(@NotNull QName workStateTypeName) {
        return new ActivityStateDefinition(workStateTypeName, ActivityStatePersistenceType.SINGLE_REALIZATION);
    }

    public static ActivityStateDefinition perpetual() {
        return perpetual(AbstractActivityWorkStateType.COMPLEX_TYPE);
    }

    public static ActivityStateDefinition perpetual(@NotNull QName workStateTypeName) {
        return new ActivityStateDefinition(workStateTypeName, ActivityStatePersistenceType.PERPETUAL);
    }

    public boolean isSingleRealization() {
        return persistence() == ActivityStatePersistenceType.SINGLE_REALIZATION;
    }
}
