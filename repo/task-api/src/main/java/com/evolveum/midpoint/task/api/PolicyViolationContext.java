/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPolicyActionType;

public record PolicyViolationContext(
        @NotNull String ruleIdentifier,
        String ruleName,
        String reactionName,
        ActivityPolicyActionType policyAction,
        Integer executionAttempt) {



    public static PolicyViolationContext getPolicyViolationContext(TaskRunResult result) {
        if (result == null) {
            return null;
        }

        if (!(result.getThrowable() instanceof ActivityThresholdPolicyViolationException ex)) {
            return null;
        }

        return ex.getPolicyViolationContext();
    }

    public static <T extends ActivityPolicyActionType> T getPolicyAction(TaskRunResult result, Class<T> actionType) {
        PolicyViolationContext ctx = getPolicyViolationContext(result);
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
