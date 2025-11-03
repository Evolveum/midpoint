/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
