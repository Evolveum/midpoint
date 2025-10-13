/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
