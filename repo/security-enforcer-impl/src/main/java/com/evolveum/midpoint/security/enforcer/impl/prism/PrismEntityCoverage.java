/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl.prism;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismValue;

/** Extent to which given prism entity ({@link Item} or {@link PrismValue}) is covered by (e.g.) set of authorizations. */
public enum PrismEntityCoverage {

    /** All of the given item/value is fully covered. */
    FULL,

    /** Nothing of the given item/value is covered. */
    NONE,

    /** Parts of given item/value may or may not be covered. */
    PARTIAL
}
