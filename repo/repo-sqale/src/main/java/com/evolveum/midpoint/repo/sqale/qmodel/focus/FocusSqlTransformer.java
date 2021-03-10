/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.focus;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.qmodel.object.ObjectSqlTransformer;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

public class FocusSqlTransformer<S extends FocusType, Q extends QFocus<R>, R extends MFocus>
        extends ObjectSqlTransformer<S, Q, R> {

    public FocusSqlTransformer(
            SqlTransformerSupport transformerSupport, QFocusMapping<S, Q, R> mapping) {
        super(transformerSupport, mapping);
    }

    @Override
    public @NotNull R toRowObjectWithoutFullObject(S schemaObject, JdbcSession jdbcSession) {
        R r = super.toRowObjectWithoutFullObject(schemaObject, jdbcSession);

        r.costCenter = schemaObject.getCostCenter();
        // TODO finish focus fields

        return r;
    }
}
