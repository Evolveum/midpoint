/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.ext;

/**
 * Specifies cardinality of the {@link MExtItem}.
 */
public enum MExtItemCardinality {
    SCALAR, // if definition.maxOccurs == 1
    ARRAY
}
