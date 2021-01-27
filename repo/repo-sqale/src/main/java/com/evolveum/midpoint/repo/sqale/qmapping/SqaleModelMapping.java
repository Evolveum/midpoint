/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmapping;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.qbean.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.QObject;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMapping;

/**
 * Mapping superclass with common functions for {@link QObject} and non-objects (e.g. containers).
 *
 * @see QueryModelMapping
 */
// TODO change the type of QObject to something more abstract later (ScaleObject?)
public abstract class SqaleModelMapping<S, Q extends QObject<R>, R extends MObject>
        extends QueryModelMapping<S, Q, R> {

    protected SqaleModelMapping(
            @NotNull String tableName,
            @NotNull String defaultAliasName,
            @NotNull Class<S> schemaType,
            @NotNull Class<Q> queryType) {
        super(tableName, defaultAliasName, schemaType, queryType);
    }
}
