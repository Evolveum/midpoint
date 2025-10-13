/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.api;

/**
 * @author semancik
 *
 */
public enum ItemComparisonResult {

    /**
     * Value matches. Stored value is different.
     */
    MATCH,

    /**
     * Value mismatch. Stored value is the same.
     */
    MISMATCH,

    /**
     * Cannot compare. Comparison is not applicable.
     */
    NOT_APPLICABLE

}
