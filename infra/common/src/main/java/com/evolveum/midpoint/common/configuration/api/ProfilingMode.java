/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.common.configuration.api;

import com.evolveum.midpoint.util.annotation.Experimental;

/**
 *  EXPERIMENTAL
 */
@Experimental
public enum ProfilingMode {

    /**
     * Profiling is enabled and is driven by setting logging levels via system configuration object.
     * This is the same as profilingEnabled = true in pre-4.0 versions.
     */
    ON("on"),

    /**
     * Profiling is disabled. MidpointInterceptor is not loaded.
     * This is the same as profilingEnabled = false in pre-4.0 versions.
     */
    OFF("off"),

    /**
     * Profiling is enabled/disabled on demand.
     *
     * MidpointInterceptor is loaded and ready to use. Profiling is started either on request of midPoint code
     * (see e.g. profilingObjectIntervalStart/profilingObjectIntervalLength extension properties) or traditionally by setting
     * logging levels via system configuration object.
     */
    DYNAMIC("dynamic");

    private final String value;

    ProfilingMode(String value) {
        this.value = value;
    }

    public static ProfilingMode fromValue(String value) {
        for (ProfilingMode mode : values()) {
            if (mode.value.equals(value)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown profiling mode: " + value);
    }

    public String getValue() {
        return value;
    }
}
