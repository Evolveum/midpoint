package com.evolveum.midpoint.repo.sql.pure.mapping;

import java.util.function.Function;

import com.querydsl.core.types.*;

import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.query.RefFilter;

/**
 * Filter processor for a reference attribute path.
 */
public class RefItemFilterProcessor extends ItemFilterProcessor<RefFilter> {

    private final Path<?> path;

    /**
     * Returns the mapper function creating the string filter processor from context.
     */
    public static ItemSqlMapper mapper(
            Function<EntityPath<?>, Path<?>> rootToQueryItem) {
        return new ItemSqlMapper(rootToQueryItem,
                context -> new RefItemFilterProcessor(rootToQueryItem.apply(context.path())));
    }

    private RefItemFilterProcessor(Path<?> path) {
        this.path = path;
    }

    @Override
    public Predicate process(RefFilter filter) {
        PrismReferenceValue singleValue = filter.getSingleValue();
        Referencable ref = singleValue != null ? singleValue.getRealValue() : null;
        return ref != null
                ? ExpressionUtils.predicate(Ops.EQ, path, ConstantImpl.create(ref.getOid()))
                : ExpressionUtils.predicate(Ops.IS_NULL, path);
    }
}
