/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.focus;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenericObjectType;

public class GenericObjectSqlTransformer
        extends FocusSqlTransformer<GenericObjectType, QGenericObject, MGenericObject> {

    public GenericObjectSqlTransformer(
            SqlTransformerSupport transformerSupport, QGenericObjectMapping mapping) {
        super(transformerSupport, mapping);
    }

    @Override
    public @NotNull MGenericObject toRowObjectWithoutFullObject(
            GenericObjectType genericObject, JdbcSession jdbcSession) {
        MGenericObject row = super.toRowObjectWithoutFullObject(genericObject, jdbcSession);

        row.genericObjectTypeId = processCacheableUri(genericObject.getObjectType(), jdbcSession);

        return row;
    }
}
