package com.evolveum.midpoint.repo.sql.pure;

import com.querydsl.core.types.Predicate;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.sql.query.QueryException;

/**
 * Filter processor is very abstract thing that takes the filter and returns the SQL predicate.
 * What happens with it depends on the context implementing the processor.
 * There are two typical usages:
 * <ul>
 *     <li>processors in the context of a query (or subquery);</li>
 *     <li>processors in the context of a single Prism item (not necessarily one SQL column).</li>
 * </ul>
 */
public interface FilterProcessor<O extends ObjectFilter> {

    Predicate process(O filter) throws QueryException;
}
