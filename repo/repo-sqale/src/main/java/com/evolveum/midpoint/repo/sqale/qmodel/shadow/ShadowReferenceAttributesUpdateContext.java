/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.shadow;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.repo.sqale.mapping.SqaleNestedMapping;
import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Update context for nested containers stored in the same table used by the parent context.
 *
 * @param <S> schema type of the container mapped by the nested mapping
 * @param <Q> entity query type that holds the data for the mapped attributes
 * @param <R> row type related to the {@link Q}
 */
public class ShadowReferenceAttributesUpdateContext extends SqaleUpdateContext<ShadowType, QShadow, MShadow> {

    private final QueryModelMapping<ShadowType, QShadow, MShadow> mapping;

    public ShadowReferenceAttributesUpdateContext(
            SqaleUpdateContext<?, QShadow, MShadow> parentContext,
            QueryModelMapping<ShadowType, QShadow, MShadow> mapping) {
        super(parentContext, parentContext.row());

        this.mapping = mapping;
    }

    @Override
    public QShadow entityPath() {
        //noinspection unchecked
        return (QShadow) parentContext.entityPath();
    }

    @Override
    public QueryModelMapping<ShadowType, QShadow, MShadow> mapping() {
        return mapping;
    }

    @Override
    public <P extends Path<T>, T> void set(P path, T value) {
        throw new IllegalStateException();
    }

    @Override
    public <P extends Path<T>, T> void set(P path, Expression<T> value) {
        throw new IllegalStateException();
    }

    @Override
    public <P extends Path<T>, T> void setNull(P path) {
        throw new IllegalStateException();
    }

    @Override
    protected void finishExecutionOwn() {
        // Nothing to do, parent context has all the updates.

        // mapping.afterModify(); currently not needed for any nested container, but possible
        // If implemented, perhaps set "dirty" flag in "set" methods and only execute
        // if the nested container columns are really changed.
    }
}
