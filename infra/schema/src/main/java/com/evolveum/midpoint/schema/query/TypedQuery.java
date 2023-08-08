/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.query;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.exception.SchemaException;

import java.util.Collection;

/**
 * Typed Query represents query with filter, which can be used for searching references, containers and objects (depending on item type).
 *
 * Typed Query can be converted to existing {@link ObjectQuery} interface using {@link #toObjectQuery()} method. This allows
 * to use TypedQuery as a builder for {@link ObjectQuery} where resulting object queries will only differ in paging information,
 * but use same filter.
 *
 * @param <T> Resulting item type
 */
public class TypedQuery<T> extends AbstractTypedQuery<T,TypedQuery<T>> {

    private ObjectFilter filter;

    private Collection<SelectorOptions<GetOperationOptions>> options;


    TypedQuery(Class<T> type, ObjectFilter filter) {
        super(type);
        this.filter = filter;
    }

    public Class<T> getType() {
        return type;
    }

    /**
     * Creates TypedFilter for supplied type and query string.
     *
     * Query string must not contain placeholders. If you need placeholder support see {@link PreparedQuery#parse(Class, String)}.
     * Resulting query doe
     *
     * @param type Type of the items which query should return
     * @param query Midpoint Query string without placeholders
     * @return new Typed Query which contains parsed filter and type information, no ordering and no paging information.
     * @param <T>
     * @throws SchemaException when supplied query string was syntactically or logically incorrect
     */
    public static <T> TypedQuery<T> parse(Class<T> type, String query) throws SchemaException {
        var filter = PrismContext.get().createQueryParser().parseFilter(type, query);
        return from(type, filter);
    }

    /**
     * Creates TypedFilter for supplied type and filter.
     *
     * @param type Type of the items which query should return
     * @param filter Existing filter to be used for typed query
     * @return Typed Query which contains  filter and type information, no ordering and no paging information.
     * @param <T> Type of the items which query should return
     */
    public static <T> TypedQuery<T> from(Class<T> type, ObjectFilter filter) {
        return new TypedQuery<>(type, filter);
    }

    /**
     * Creates new {@link ObjectQuery} representing this {@link TypedQuery}, created Object query uses same filter and
     * copy of current paging information.
     *
     * This allows to use one TypedQuery to be used to generate multiple page specific queries.
     *
     * @return new Object Query representing this Type Query
     */
    public ObjectQuery toObjectQuery() {
        var query = PrismContext.get().queryFactory().createQuery(filter);
        query.setPaging(buildObjectPaging());
        return query;

    }

    /**
     * Returns explicitly configured set of {@link GetOperationOptions}
     *
     * If set was not explicitly set using {@link #setOptions(Collection)}, it is constructed using {@link #operationOptionsBuilder()}.
     *
     * @return Set of {@link GetOperationOptions}
     */
    public Collection<SelectorOptions<GetOperationOptions>>  getOptions() {
        if (options == null) {
            options = operationOptionsBuilder().build();
        }
        return options;
    }

    public void setOptions(Collection<SelectorOptions<GetOperationOptions>> options) {
        this.options = options;
    }

    @Override
    protected TypedQuery<T> self() {
        return this;
    }
}
