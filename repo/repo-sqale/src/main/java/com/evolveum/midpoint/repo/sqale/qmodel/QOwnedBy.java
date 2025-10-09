/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel;

import com.querydsl.core.types.dsl.BooleanExpression;

/**
 * Behavior common for child tables owned by other table, either directly by object
 * or another container.
 *
 * @param <OR> type of the owner row
 */
public interface QOwnedBy<OR> {

    /**
     * Returns predicate for where clause that matches only rows owned by the provided owner.
     * Owner row is immediate owner, so if the containers are nested, it is the owning container,
     * not the top-level owning object.
     */
    BooleanExpression isOwnedBy(OR ownerRow);
}
