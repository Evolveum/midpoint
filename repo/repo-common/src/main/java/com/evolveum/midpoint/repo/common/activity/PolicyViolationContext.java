/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPolicyActionType;

import org.jetbrains.annotations.Nullable;

public record PolicyViolationContext(
        @NotNull String ruleIdentifier,
        String ruleName,
        String reactionName,
        ActivityPolicyActionType policyAction,
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

        ActivityPolicyActionType action = ctx.policyAction();
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
