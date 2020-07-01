package com.evolveum.midpoint.repo.sql.pure;

import com.querydsl.core.types.Predicate;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.repo.sql.pure.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sql.query.QueryException;

public class PropertyValueFilterProcessor implements FilterProcessor<PropertyValueFilter<?>> {

    private final SqlPathContext context;

    public PropertyValueFilterProcessor(SqlPathContext context) {
        this.context = context;
    }

    @Override
    public Predicate process(PropertyValueFilter<?> filter) throws QueryException {
        System.out.println("filter = " + filter);
        ItemPath filterPath = filter.getPath();
        ItemName itemName = filterPath.firstName();
        if (!filterPath.isSingleName()) {
            throw new QueryException("Filter with non-single path is not supported YET: " + filterPath);
        }

        // TODO: needed only for Any filter?
//        ItemDefinition definition = filter.getDefinition();

        System.out.println("itemName = " + itemName);
        System.out.println("values = " + filter.getValues());
        // TODO: later with polystring attributes this may relate to two different SQL paths, see existing PolyStringMatcher
        // instead of mapping returning things to deal with, it should accept information and return predicate

        QueryModelMapping<?, ?> mapping = context.mapping();
        FilterProcessor<PropertyValueFilter<?>> processor =
                mapping.getFilterProcessor(itemName, context.path());
        return processor.process(filter);
    }
}
