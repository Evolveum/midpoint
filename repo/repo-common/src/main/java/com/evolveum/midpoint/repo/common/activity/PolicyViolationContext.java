/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPolicyActionType;

public record PolicyViolationContext(
        @NotNull String ruleIdentifier,
        String ruleName,
        ActivityPolicyActionType action,
        Integer executionAttempt) {

    public static PolicyViolationContext getPolicyViolationContext(Throwable throwable) {
        if (throwable instanceof ActivityThresholdPolicyViolationException ex) {
            return ex.getPolicyViolationContext();
        } else {
            return null;
        }
    }

    public static <T extends ActivityPolicyActionType> T getPolicyAction(Throwable throwable, Class<T> actionType) {
        return getPolicyAction(getPolicyViolationContext(throwable), actionType);
    }

    public static <T extends ActivityPolicyActionType> @Nullable T getPolicyAction(PolicyViolationContext ctx, Class<T> actionType) {
        if (ctx == null) {
            return null;
        }

        ActivityPolicyActionType action = ctx.action();
        if (action == null) {
            return null;
        }

        if (!actionType.isAssignableFrom(action.getClass())) {
            throw new IllegalStateException(
                    "Activity policy action type " + actionType.getName() + " is not assignable from " + action.getClass().getName());
        }

        //noinspection unchecked
        return (T) action;
    }
}
