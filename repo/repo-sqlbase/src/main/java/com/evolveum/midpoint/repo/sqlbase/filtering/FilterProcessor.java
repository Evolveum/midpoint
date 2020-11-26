/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.filtering;

import com.querydsl.core.types.Predicate;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.mapping.item.ItemFilterProcessor;

/**
 * Filter processor is very abstract thing that takes the filter and returns the SQL predicate.
 * What happens with it depends on the context implementing the processor.
 * There are two typical usages:
 * <ul>
 *     <li>Processors in the context of a query (or subquery).
 *     These typically determine what other processor should be used in the next step.</li>
 *     <li>{@link ItemFilterProcessor}s for a single Prism item (not necessarily one SQL column).</li>
 * </ul>
 */
public interface FilterProcessor<O extends ObjectFilter> {

    Predicate process(O filter) throws QueryException;
}
