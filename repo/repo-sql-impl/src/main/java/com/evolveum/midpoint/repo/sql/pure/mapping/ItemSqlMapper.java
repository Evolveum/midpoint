/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.pure.mapping;

import java.util.Objects;
import java.util.function.Function;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Path;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.sql.pure.FilterProcessor;
import com.evolveum.midpoint.repo.sql.pure.SqlPathContext;

public class ItemSqlMapper {

    /**
     * Primary mapping is used for order by clauses (if they are comparable).
     * Mappers can map to multiple query attributes (e.g. poly-string has orig and norm),
     * so normally the mapping(s) are encapsulated there, but for order we need one exposed.
     * Can be {@code null} which indicates that ordering is not possible.
     */
    @Nullable private final Function<EntityPath<?>, Path<?>> primaryItemMapping;

    @NotNull private final Function<SqlPathContext<?, ?, ?>, FilterProcessor<?>> filterProcessorFactory;

    public ItemSqlMapper(
            @NotNull Function<SqlPathContext<?, ?, ?>, FilterProcessor<?>> filterProcessorFactory,
            @Nullable Function<EntityPath<?>, Path<?>> primaryItemMapping) {
        this.filterProcessorFactory = Objects.requireNonNull(filterProcessorFactory);
        this.primaryItemMapping = primaryItemMapping;
    }

    public ItemSqlMapper(
            @NotNull Function<SqlPathContext<?, ?, ?>, FilterProcessor<?>> filterProcessorFactory) {
        this(filterProcessorFactory, null);
    }

    public @Nullable Path<?> itemPrimaryPath(EntityPath<?> root) {
        return primaryItemMapping != null ? primaryItemMapping.apply(root) : null;
    }

    public <T extends ObjectFilter> FilterProcessor<T> createFilterProcessor(
            SqlPathContext<?, ?, ?> pathContext) {
        //noinspection unchecked
        return (FilterProcessor<T>) filterProcessorFactory.apply(pathContext);
    }
}
