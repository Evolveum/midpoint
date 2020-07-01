package com.evolveum.midpoint.repo.sql.pure;

import com.querydsl.core.types.Ops;
import com.querydsl.core.types.Predicate;

import com.evolveum.midpoint.prism.query.*;
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

    default Ops operation(ValueFilter<?, ?> filter) throws QueryException {
        if (filter instanceof EqualFilter) {
            // TODO possibly EQ_IGNORE_CASE based on matching? or rather we control it?
            return Ops.EQ;
        } else if (filter instanceof GreaterFilter) {
            GreaterFilter<?> gf = (GreaterFilter<?>) filter;
            return gf.isEquals() ? Ops.GOE : Ops.GT;
        } else if (filter instanceof LessFilter) {
            LessFilter<?> lf = (LessFilter<?>) filter;
            return lf.isEquals() ? Ops.LOE : Ops.LT;
        } else if (filter instanceof SubstringFilter) {
            SubstringFilter<?> substring = (SubstringFilter<?>) filter;
            if (substring.isAnchorEnd()) {
                return Ops.ENDS_WITH;
            } else if (substring.isAnchorStart()) {
                return Ops.STARTS_WITH;
            } else {
                return Ops.STRING_CONTAINS;
            }
        }

        throw new QueryException("Can't translate filter '" + filter + "' to operation.");
    }
}
