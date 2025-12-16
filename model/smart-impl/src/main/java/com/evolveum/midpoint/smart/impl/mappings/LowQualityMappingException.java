/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl.mappings;

/**
 * Exception thrown when a mapping suggestion has quality below the acceptable threshold.
 * This is not an error condition, but rather indicates that the suggested mapping
 * should be skipped due to insufficient quality.
 */
public class LowQualityMappingException extends Exception {

    public LowQualityMappingException(float quality, float threshold, String attributePath, String propertyPath) {
        super(String.format("Mapping quality %.2f%% is below threshold %.2f%% for %s -> %s",
                quality * 100, threshold * 100, attributePath, propertyPath));
    }

}
