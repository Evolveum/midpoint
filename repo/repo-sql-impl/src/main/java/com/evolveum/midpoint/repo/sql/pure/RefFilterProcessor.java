package com.evolveum.midpoint.repo.sql.pure;

import com.querydsl.core.types.Predicate;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.repo.sql.query.QueryException;

public class RefFilterProcessor implements FilterProcessor<RefFilter> {

    private final SqlPathContext context;

    public RefFilterProcessor(SqlPathContext context) {
        this.context = context;
    }

    @Override
    public Predicate process(RefFilter filter) throws QueryException {
        ItemPath filterPath = filter.getPath();
        ItemName itemName = filterPath.firstName();
        // TODO: value count and other attributes checks?
        if (!filterPath.isSingleName()) {
            throw new QueryException("Filter with non-single path is not supported YET: " + filterPath);
        }
        if (filter.getRightHandSidePath() != null) {
            throw new QueryException("Filter with right-hand-side path is not supported YET: " + filterPath);
        }

        FilterProcessor<RefFilter> filterProcessor =
                context.mapping().getFilterProcessor(itemName, context);
        return filterProcessor.process(filter);
    }
}
