/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.security.enforcer.api;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationDecisionType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Compiled security constraints for a given prism value (usually containerable) and operation, at the granularity
 * of individual contained items.
 *
 * Basically, it can answer questions whether the operation is allowed for given item (with children) or all item paths.
 *
 * Contained in {@link ObjectSecurityConstraints}. However, the latter contains information about multiple operations and phases.
 * This one does not.
 *
 * @author semancik
 */
public interface ItemSecurityConstraints extends DebugDumpable {

    /**
     * Returns the explicit allow-deny decision (if present) that is common to all paths in the value.
     *
     * - `DENY` means that the access to all items in the value is explicitly _denied_.
     * - `ALLOW` means that the access to all items in the value is explicitly _allowed_.
     * (But beware, some of the items inside may still be explicitly denied, which then takes precedence over being allowed.)
     * - `null` means that neither of the above is the case.
     */
    @Nullable AuthorizationDecisionType findAllItemsDecision();

    /**
     * Returns the explicit allow-deny decision (if present) for the particular item and all its sub-items.
     *
     * - `DENY` means that the access to this item and all its sub-items is explicitly _denied_.
     * - `ALLOW` means that the access to this item and all its sub-items is explicitly _allowed_.
     * (But beware, some of the sub-items may still be explicitly denied, which then takes precedence over being allowed.)
     * - `null` means that the access to this item is neither explicitly denied nor allowed. For structured items
     * this means that individual children may still be denied or allowed.
     */
    @Nullable AuthorizationDecisionType findItemDecision(@NotNull ItemPath nameOnlyItemPath);
}
