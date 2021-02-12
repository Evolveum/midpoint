/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.filtering;

import com.querydsl.core.types.Predicate;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;

public class PropertyValueFilterProcessor implements FilterProcessor<PropertyValueFilter<?>> {

    private final SqlQueryContext<?, ?, ?> context;

    public PropertyValueFilterProcessor(SqlQueryContext<?, ?, ?> context) {
        this.context = context;
    }

    @Override
    public Predicate process(PropertyValueFilter<?> filter) throws QueryException {
        ItemPath filterPath = filter.getPath();
        ItemName itemName = filterPath.firstName();
        if (!filterPath.isSingleName()) {
            throw new QueryException("Filter with non-single path is not supported YET: " + filterPath);
        }
        if (filter.getRightHandSidePath() != null) {
            throw new QueryException("Filter with right-hand-side path is not supported YET: " + filterPath);
        }

        // TODO: needed only for Any filter?
//        ItemDefinition definition = filter.getDefinition();

        FilterProcessor<PropertyValueFilter<?>> processor =
                context.createItemFilterProcessor(itemName);
        return processor.process(filter);
    }
}
