/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl.scoring;

/**
 * Exception thrown when filter validation fails during object type suggestion.
 * This indicates that a suggested filter cannot be executed on the resource.
 */
public class FilterValidationException extends Exception {

    public FilterValidationException(String message) {
        super(message);
    }

}
