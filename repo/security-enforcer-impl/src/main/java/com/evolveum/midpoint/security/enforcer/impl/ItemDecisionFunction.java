/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.security.enforcer.impl;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.AccessDecision;

import org.jetbrains.annotations.Nullable;

/**
 * @author semancik
 */
@FunctionalInterface
public interface ItemDecisionFunction {

    /**
     * Provides an authorization decision (based on specific information) about a given item.
     *
     * - {@link AccessDecision#ALLOW} means that the operation can be applied on the item in full.
     * - {@link AccessDecision#DENY} means that the operation cannot be applied on this item.
     * - {@link AccessDecision#DEFAULT} means the deeper investigation should be done (for structured items)
     * - `null` means that this item can be ignored as far as the operation is concerned
     *
     * @param removingContainer true if the container value corresponding to given path is being deleted
     */
    @Nullable AccessDecision decide(ItemPath nameOnlyItemPath, boolean removingContainer);

}
