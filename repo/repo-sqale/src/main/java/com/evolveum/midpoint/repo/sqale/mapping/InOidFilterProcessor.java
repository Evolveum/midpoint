/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.mapping;

import java.util.Collection;
import java.util.UUID;

import com.querydsl.core.types.Predicate;

import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.FilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Filter processor that resolves {@link InOidFilter}.
 */
public class InOidFilterProcessor implements FilterProcessor<InOidFilter> {

    private final SqlQueryContext<?, ?, ?> context;

    public InOidFilterProcessor(SqlQueryContext<?, ?, ?> context) {
        this.context = context;
    }

    @Override
    public Predicate process(InOidFilter filter) throws QueryException {
        FlexibleRelationalPathBase<?> path = context.root();

        if (path instanceof QObject<?>) {
            return ((QObject<?>) path).oid.in(stringsToUuids(filter.getOids()));
        } else if (path instanceof QContainer<?>) {
            if (filter.isConsiderOwner()) {
                return ((QContainer<?>) path).ownerOid.in(stringsToUuids(filter.getOids()));
            } else {
                return ((QContainer<?>) path).cid.in(stringsToIntegers(filter.getOids()));
            }
        }

        throw new QueryException("InOidFilter cannot be applied to the entity: " + path);
    }

    private UUID[] stringsToUuids(Collection<String> oids) {
        return oids.stream().map(UUID::fromString).toArray(UUID[]::new);
    }

    private Integer[] stringsToIntegers(Collection<String> oids) {
        return oids.stream().map(Integer::valueOf).toArray(Integer[]::new);
    }
}
