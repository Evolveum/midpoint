/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger;

/**
 * TODO better strategy names
 */
public enum MergeStrategy {

    /**
     * Merge the values from the source item to the target item.
     *
     * If the target item already contains a value:
     * * for containers, the values are merged recursively
     * * for other items (property, reference), the value is *preserved*
     *
     * If the source item contains a value that is not present in the target item, it is added.
     *
     * If the source item doesn't contain value present in target, value is *not changed* in target.
     */
    SIMPLE,

    /**
     * Merge the values from the source item to the target item.
     *
     * If the target item already contains a value:
     * * for containers, the values are merged recursively
     * * for other items (property, reference), the value is *replaced*
     *
     * If the source item contains a value that is not present in the target item, it is added.
     *
     * If the source item doesn't contain value present in target, value is *not changed* in target.
     */
    OVERLAY,

    /**
     * Merge the values from the source item to the target item.
     *
     * If the target item already contains a value:
     * * for containers, the values are merged recursively
     * * for other items (property, reference), the value is *replaced*
     *
     * If the source item contains a value that is not present in the target item, it is added.
     *
     * If the source item doesn't contain value present in target, value is *removed* from target.
     */
    FULL
}
