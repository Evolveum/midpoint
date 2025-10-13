/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
