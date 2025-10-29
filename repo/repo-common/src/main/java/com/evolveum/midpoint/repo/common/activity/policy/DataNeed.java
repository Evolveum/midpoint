/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

/**
 * What data does a specific policy rule or policy rule constraint need in order to be evaluated.
 * We need this to determine what data to collect from the activity tree in order to evaluate the policies.
 */
public enum DataNeed {

    /** Total execution time of the activity (and its sub-activities). */
    EXECUTION_TIME,

    /** Number of execution attempts of the activity. */
    EXECUTION_ATTEMPTS,

    /** Policy rule counters. */
    COUNTERS
}
