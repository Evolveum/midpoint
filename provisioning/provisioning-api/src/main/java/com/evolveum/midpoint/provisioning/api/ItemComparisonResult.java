/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
