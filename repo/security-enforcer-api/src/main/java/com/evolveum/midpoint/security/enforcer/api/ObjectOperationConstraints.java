/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.OwnerResolver;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;

import org.jetbrains.annotations.Nullable;

/**
 * Extracted relevant security constraints related to a single operation (usually, reading) of a given object.
 *
 * This is a simplified and performance-optimized version of {@link ObjectSecurityConstraints}.
 *
 * @see SecurityEnforcer#compileOperationConstraints(PrismObject, OwnerResolver, java.util.Collection, Task, OperationResult)
 */
public interface ObjectOperationConstraints extends DebugDumpable {

    /**
     * Returns `true` if the operation is completely allowed over the whole object.
     * Used to quickly check for a common case of a fully readable object.
     */
    boolean isCompletelyAllowed(@Nullable AuthorizationPhaseType phase);

    /**
     * Returns the explicit allow-deny decision (if present) that is common to all items in the object.
     * See also {@link ItemSecurityConstraints#findAllItemsDecision()}.
     */
    AuthorizationDecisionType findAllItemsDecision(@Nullable AuthorizationPhaseType phase);

    /**
     * Returns the explicit allow-deny decision (if present) for the particular item and all its sub-items.
     * See also {@link ItemSecurityConstraints#findItemDecision(ItemPath)}.
     */
    AuthorizationDecisionType findItemDecision(ItemPath nameOnlyItemPath, @Nullable AuthorizationPhaseType phase);
}
