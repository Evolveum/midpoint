/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.api;

/**
 * Describes the regeneration strategy when the user wants to re-run an object type suggestion
 * that has already been generated.
 */
public enum RegenerateMode {

    /**
     * The data split (how resource objects are grouped into types) is wrong.
     */
    NEW_DATA_SPLIT,

    /**
     * The data split looks good but the filter / classification criteria are wrong.
     */
    NEW_FILTER
}
