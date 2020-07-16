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
 * <p>
 * TODO if this proves to have a single subclass in, say, 2022, merge them into one, please
 * The idea is/was that not every context requires query - or represents the whole query.
 * Some sub-contexts may create inner query, e.g. in EXISTS clause.
 * On the other hand, even if sub-context does not represent the new query but only part of
 * existing one, e.g. JOIN path, we still need access to the original query, for instance to
 * add another JOIN, or test existing joins, etc.
 * But this can be done by creating new SqlQueryContext with the same query and different path.
 * So - if no special needs arise (e.g. for EXISTS support)... merge these classes. ;-)
 *
 * @param <S> schema type, used by encapsulated mapping
 * @param <Q> type of entity path
 * @param <R> row type related to the {@link Q}
 */
public abstract class SqlPathContext<S, Q extends EntityPath<R>, R> {

    private final Q path;
    private final QueryModelMapping<S, Q, R> mapping;
    private final PrismContext prismContext;

    private boolean notFilterUsed = false;

    public SqlPathContext(Q path, QueryModelMapping<S, Q, R> mapping, PrismContext prismContext) {
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

    public QueryModelMapping<S, Q, R> mapping() {
        return mapping;
    }

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

    public abstract <DQ extends EntityPath<DR>, DR> SqlQueryContext<?, DQ, DR> leftJoin(
            @NotNull DQ newPath,
            @NotNull BiFunction<Q, DQ, Predicate> joinOnPredicate);

    public abstract String uniqueAliasName(String baseAliasName);
}
