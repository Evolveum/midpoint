/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.api.info;

import org.jetbrains.annotations.Nullable;

/**
 * Health status of the AI provider or microservice component.
 * Matches the HealthStatus enum from the Smart Integration microservice.
 */
public enum HealthStatus {

    OK,
    ERROR;

    /**
     * Converts a string value to HealthStatus enum.
     * Returns ERROR as fallback for unknown values.
     * Returns null if the input is null.
     */
    public static @Nullable HealthStatus fromString(@Nullable String value) {
        if (value == null) {
            return null;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ERROR;
        }
    }
}
