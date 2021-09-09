/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.filtering;

import com.querydsl.core.types.Expression;

import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;

public interface RightHandProcessor {

    Expression<?> rightHand(ValueFilter<?, ?> filter) throws RepositoryException;
}
