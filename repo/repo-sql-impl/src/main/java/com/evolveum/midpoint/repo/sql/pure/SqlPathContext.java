package com.evolveum.midpoint.repo.sql.pure;

import java.util.function.BiFunction;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Predicate;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.sql.pure.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sql.query.QueryException;

/**
 * SQL path context with mapping information.
 *
 * @param <Q> type of entity path
 * @param <R> row type related to the {@link Q}
 */
public abstract class SqlPathContext<Q extends EntityPath<R>, R> {

    private final Q path;
    private final QueryModelMapping<?, Q, R> mapping;
    private final PrismContext prismContext;

    private boolean notFilterUsed = false;

    public SqlPathContext(Q path, QueryModelMapping<?, Q, R> mapping, PrismContext prismContext) {
        this.path = path;
        this.mapping = mapping;
        this.prismContext = prismContext;
    }

    public Q path() {
        return path;
    }

    public <T extends FlexibleRelationalPathBase<?>> T path(Class<T> pathType) {
        return pathType.cast(path);
    }

    public QueryModelMapping<?, Q, R> mapping() {
        return mapping;
    }

    // TODO - will be necessary for mappers requiring model insight, e.g. path->CanonicalItemPath, etc.
    public PrismContext prismContext() {
        return prismContext;
    }

    public <T extends ObjectFilter> @NotNull FilterProcessor<T> createItemFilterProcessor(
            ItemName itemName) throws QueryException {
        return mapping.createItemFilterProcessor(itemName, this);
    }

    public void markNotFilterUsage() {
        notFilterUsed = true;
    }

    public boolean isNotFilterUsed() {
        return notFilterUsed;
    }

    public abstract <DQ extends EntityPath<DR>, DR> SqlQueryContext<DQ, DR> leftJoin(
            @NotNull DQ newPath,
            @NotNull BiFunction<Q, DQ, Predicate> joinOnPredicate);

    public abstract String uniqueAliasName(String baseAliasName);
}
