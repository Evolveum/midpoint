/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.filtering;

import com.querydsl.core.types.Predicate;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.mapping.item.ItemFilterProcessor;

/**
 * Filter processor is very abstract thing that takes the filter and returns the SQL predicate.
 * What happens with it depends on the context implementing the processor.
 *
 * There are two typical usages:
 *
 * * Processors in the context of a query (or subquery).
 * These typically determine what other processor should be used in the next step.
 * The logic starts in {@link ObjectFilterProcessor#process(ObjectFilter)} and determines
 * whether to resolve logical operations or delegate to other specialized filter.
 * *Complex path resolution* is typically here (which may add JOINs), but value evaluation is not.
 *
 * * {@link ItemFilterProcessor}s for a single Prism item (not necessarily one SQL column).
 * These *process only single/final path component and use the value of the filter*.
 * While JOINs are typically only used here it is possible that multi-value attributes stored
 * in detail tables can generate another JOIN in this step too.
 */
public interface FilterProcessor<O extends ObjectFilter> {

    Predicate process(O filter) throws RepositoryException;
}
