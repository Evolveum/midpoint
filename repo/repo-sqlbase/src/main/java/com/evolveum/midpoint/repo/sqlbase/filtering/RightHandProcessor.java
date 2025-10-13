/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqlbase.filtering;

import com.querydsl.core.types.Expression;

import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;

public interface RightHandProcessor {

    Expression<?> rightHand(ValueFilter<?, ?> filter) throws RepositoryException;
}
