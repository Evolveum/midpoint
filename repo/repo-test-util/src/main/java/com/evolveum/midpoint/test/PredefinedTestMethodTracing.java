/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test;

/**
 *
 */
public enum PredefinedTestMethodTracing {

    /**
     * No test method tracing.
     */
    OFF,

    /**
     * Functional tracing with model logging set to TRACE.
     */
    MODEL_LOGGING,

    /**
     * Functional tracing with model and provisioning logging set to TRACE.
     */
    MODEL_PROVISIONING_LOGGING,

    /**
     * Functional tracing with model and workflow logging set to TRACE.
     */
    MODEL_WORKFLOW_LOGGING
}
