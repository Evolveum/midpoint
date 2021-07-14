/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import java.util.Collection;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityCounterGroupsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExecutionModeType;

/**
 * Functionality that supports the activity execution that is going on within this task.
 */
public interface ExecutionSupport {

    /**
     * Returns the mode in which the activity executes (normal, dry run, simulate, ...).
     */
    @NotNull ExecutionModeType getExecutionMode();

    /**
     * Increments given counters related to the activity execution.
     *
     * @return Current values of the counters (after the update).
     */
    Map<String, Integer> incrementCounters(@NotNull CountersGroup counterGroup, @NotNull Collection<String> countersIdentifiers,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException;

    /**
     * Group of counters. The counter identifier is unique within its group.
     */
    enum CountersGroup {

        /**
         * Counters used to monitor policy rules thresholds.
         */
        POLICY_RULES(ActivityCounterGroupsType.F_POLICY_RULES);

        @NotNull private final ItemName itemName;

        CountersGroup(@NotNull ItemName itemName) {
            this.itemName = itemName;
        }

        public @NotNull ItemName getItemName() {
            return itemName;
        }
    }
}
