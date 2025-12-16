/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl.mappings;

/**
 * Exception thrown when source data is missing for a mapping suggestion.
 * This is not an error condition, but rather indicates that the suggested mapping
 * should be skipped due to insufficient source data.
 */
public class MissingSourceDataException extends Exception {

    public MissingSourceDataException(String attributePath, String propertyPath) {
        super(String.format("Source data missing for mapping %s -> %s",
                attributePath, propertyPath));
    }

}
