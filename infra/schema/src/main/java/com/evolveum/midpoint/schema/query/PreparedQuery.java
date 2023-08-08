/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.query;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.PreparedPrismQuery;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Prepared Query represents query with filter with placeholders, which can be used for search.
 *
 * Prepared Query differs from {@link TypedQuery} that original query contained placeholders, which needs to be bound to actual values
 * before query is used.
 *
 * Values could be bound using {@link #bindValue(Object)} method for binding first unbound values or by using {@link #bind(Object...)}
 * method which binds multiple unbound values in order of their appeareance.
 *
 * Prepared Query can be converted to {@link TypedQuery} by binding values using {@link #bind(Object...)} method. Once prepared query
 * is successfully bound, filter is cached and new typed queries could be generated using {@link #toTypedQuery()}.
 *
 * @param <T> <T> Resulting item type
 */
public class PreparedQuery<T> extends AbstractTypedQuery<T, PreparedQuery<T>> {


    private PreparedPrismQuery delegate;

    private GetOperationOptionsBuilder options;
    /**
     * Final filter, available once query is successfully bound using {@link #bind(Object...)}.
     */
    private ObjectFilter finalFilter;

    PreparedQuery(Class<T> type, PreparedPrismQuery delegate) {
        super(type);
        this.delegate = delegate;
    }

    /**
     * Parses supplied query string for type with placeholder support.
     *
     * @param type Type of the items which query should return
     * @param query Midpoint Query string without placeholders
     * @return new Prepared Query which contains partially parsed filter, type information and allows for values to be bound to placeholders.
     * @param <T> Type of the items which query should return
     * @throws SchemaException when supplied query string was syntactically or logically incorrect
     */
    public static <T> PreparedQuery<T> parse(Class<T> type, String query) throws SchemaException {
        var prismQuery = PrismContext.get().createQueryParser().parse(type, query);
        return new PreparedQuery<>(type, prismQuery);
    }

    /**
     * Binds next unbound value in filter to provided value.
     *
     * IMPORTANT: Current behavior is strict in checking value types for bound values, user must supply
     * correct class for realValue (eg. {@link com.evolveum.midpoint.prism.polystring.PolyString},
     * {@link javax.xml.datatype.XMLGregorianCalendar}
     *
     * @param realValue Real Value to be bound
     * @throws SchemaException If provided value is invalid according to schema definition (type of value)
     * @throws IllegalStateException If there is no positional value to be bound
     */
    public PreparedQuery<T> bindValue(Object realValue) throws SchemaException {
        delegate.bindValue(realValue);
        return self();
    }

    /**
     * Binds multiple values and returns final Typed Query which will contain filter.
     *
     * @param args Real values to be bound to unbound placeholders in order of appearance. See {@link #bindValue(Object)} for more details.
     * @return Typed Query which contains final filter and configuration of paging and ordering.
     * @throws IllegalStateException If not all placeholders were bound to values
     * @throws SchemaException If provided values are invalid according to schema definition (type of value)
     *     resulting filter with bound values is invalid.
     */
    public TypedQuery<T> bind(Object... args) throws SchemaException {
        if (finalFilter == null) {
            finalFilter = delegate.bind(args);
        }
        return toTypedQuery();
    }

    /**
     * Creates new {@link TypedQuery} based on type, filter,  bound values and configuration of paging and ordering.
     *
     * @return New Typed Query based on bound values and configuration of paging and ordering.
     * @throws IllegalStateException If query was not successfully bound before using {@link #bind(Object...)} or {@link #build()}.
     */
    public TypedQuery<T> toTypedQuery() {
        if (finalFilter == null) {
            throw new IllegalStateException("Filter was not bound");
        }
        var typed = TypedQuery.from(type, finalFilter);
        typed.fillFrom(this);
        typed.setOptions(operationOptionsBuilder().build());
        return typed;
    }

    public ObjectQuery toObjectQuery() {
        return toTypedQuery().toObjectQuery();
    }

    /**
     *
     * @return
     * @throws SchemaException
     */
    public TypedQuery<T> build() throws SchemaException {
        return bind();
    }

    /**
     * @return True if all placeholders were bound to value.
     */
    public boolean allPlaceholdersBound() {
        return delegate.allPlaceholdersBound();
    }

    @Override
    protected PreparedQuery<T> self() {
        return this;
    }
}
