/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.api;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.AccessDecision;

import com.evolveum.midpoint.util.DebugDumpable;

import org.jetbrains.annotations.NotNull;

/** Compiled security constraints for a given operation applicable to a prism entity ({@link Item} or {@link PrismValue}). */
public interface PrismEntityOpConstraints extends DebugDumpable {

    /**
     * Returns the access decision for the current element:
     *
     * - either definite ({@link AccessDecision#ALLOW} or {@link AccessDecision#DENY})
     * - or not definite ({@link AccessDecision#DEFAULT}.
     */
    @NotNull AccessDecision getDecision();

    /** Compiled constraints related to an {@link Item}; they describe access to their values. */
    interface ForItemContent extends PrismEntityOpConstraints {

        /** Returns compiled constraints for given value of the current item. */
        @NotNull PrismEntityOpConstraints.ForValueContent getValueConstraints(@NotNull PrismValue value);
    }

    /** Compiled constraints related to a {@link PrismValue}; they describe access to contained sub-items. */
    interface ForValueContent extends PrismEntityOpConstraints {

        /** Returns compiled constraints for given sub-item of the current container value. */
        @NotNull PrismEntityOpConstraints.ForItemContent getItemConstraints(@NotNull ItemName name);

        /**
         * Returns compiled constraints for given item, ignoring differences between values.
         * Temporary/experimental: useful for cases where we do not have the knowledge about specific values.
         * See {@link CompileConstraintsOptions#skipSubObjectSelectors}.
         */
        @NotNull PrismEntityOpConstraints.ForValueContent getValueConstraints(@NotNull ItemPath nameOnlyPath);
    }
}
