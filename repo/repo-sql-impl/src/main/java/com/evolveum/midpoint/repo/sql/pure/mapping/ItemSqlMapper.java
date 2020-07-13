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

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.sql.pure.FilterProcessor;
import com.evolveum.midpoint.repo.sql.pure.SqlPathContext;

public class ItemSqlMapper {

    /**
     * Some mappers can map to multiple query attributes (e.g. poly-strings), but one is primary.
     * Primary mapping is used for order by clauses (if they are comparable).
     */
    @NotNull private final Function<EntityPath<?>, Path<?>> primaryItemMapping;

    @NotNull private final Function<SqlPathContext, FilterProcessor<?>> filterProcessorFactory;

    protected ItemSqlMapper(@NotNull Function<EntityPath<?>, Path<?>> primaryItemMapping,
            @NotNull Function<SqlPathContext, FilterProcessor<?>> filterProcessorFactory) {
        this.primaryItemMapping = Objects.requireNonNull(primaryItemMapping);
        this.filterProcessorFactory = Objects.requireNonNull(filterProcessorFactory);
    }

    public Path<?> itemPath(EntityPath<?> root) {
        return primaryItemMapping.apply(root);
    }

    public <T extends ObjectFilter> FilterProcessor<T> createFilterProcessor(
            SqlPathContext<?, ?> pathContext) {
        //noinspection unchecked
        return (FilterProcessor<T>) filterProcessorFactory.apply(pathContext);
    }
}
