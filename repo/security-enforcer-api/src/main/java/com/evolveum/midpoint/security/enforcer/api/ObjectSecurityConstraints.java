/*
 * Copyright (c) 2014-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.security.enforcer.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.selector.eval.OwnerResolver;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Extracted relevant security constraints related to given object.
 * Unlike {@link ObjectOperationConstraints}, this one covers all operations (represented by action URLs).
 *
 * @see SecurityEnforcer#compileSecurityConstraints(PrismObject, OwnerResolver, Task, OperationResult)
 * @see ObjectOperationConstraints
 */
public interface ObjectSecurityConstraints extends DebugDumpable {

    /** Are there any constraints defined? */
    boolean isEmpty();

    /**
     * A variant of {@link #findAllItemsDecision(String, AuthorizationPhaseType)} that considers several equivalent action URLs,
     * e.g. "read" and "get" actions. If any of them is denied, operation is denied. If any of them is allowed, operation is
     * allowed.
     */
    @Nullable AuthorizationDecisionType findAllItemsDecision(@NotNull String[] actionUrls, @Nullable AuthorizationPhaseType phase);

    /**
     * Returns the explicit allow-deny decision (if present) that is common to all items in the object.
     *
     * If there is no universally-applicable decision then null is returned. In that case there may still be fine-grained
     * decisions for individual items. Use {@link #findItemDecision(ItemPath, String, AuthorizationPhaseType)} to get them.
     */
    @Nullable AuthorizationDecisionType findAllItemsDecision(@NotNull String actionUrl, @Nullable AuthorizationPhaseType phase);

    /**
     * Returns the explicit allow-deny decision (if present) for the particular item and all its sub-items,
     * relevant to the actions (considered equivalent) and phase(s).
     *
     * A variant of {@link #findItemDecision(ItemPath, String, AuthorizationPhaseType)}.
     */
    @Nullable AuthorizationDecisionType findItemDecision(
            @NotNull ItemPath nameOnlyItemPath, @NotNull String[] actionUrls, @Nullable AuthorizationPhaseType phase);

    /**
     * Returns the explicit allow-deny decision (if present) for the particular item and all its sub-items,
     * relevant to the action and phase(s).
     *
     * See also {@link ItemSecurityConstraints#findItemDecision(ItemPath)}.
     */
    @Nullable AuthorizationDecisionType findItemDecision(
            @NotNull ItemPath nameOnlyItemPath, @NotNull String actionUrl, @Nullable AuthorizationPhaseType phase);
}
