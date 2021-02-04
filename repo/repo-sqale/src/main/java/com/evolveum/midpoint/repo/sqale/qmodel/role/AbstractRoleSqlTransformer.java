/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.role;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.qmodel.object.ObjectSqlTransformer;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;

public class AbstractRoleSqlTransformer<
        S extends AbstractRoleType, Q extends QAbstractRole<R>, R extends MAbstractRole>
        extends ObjectSqlTransformer<S, Q, R> {

    public AbstractRoleSqlTransformer(
            SqlTransformerContext transformerContext, QAbstractRoleMapping<S, Q, R> mapping) {
        super(transformerContext, mapping);
    }

    @Override
    public @NotNull R toRowObjectWithoutFullObject(S schemaObject, JdbcSession jdbcSession) {
        final R row = super.toRowObjectWithoutFullObject(schemaObject, jdbcSession);
        /*
        TODO:
        public String approvalProcess;
        public Boolean autoassignEnabled;
        public String displayNameNorm;
        public String displayNameOrig;
        public String identifier;
        public UUID ownerRefTargetOid;
        public Integer ownerRefTargetType;
        public Integer ownerRefRelationId;
        */
        row.requestable = schemaObject.isRequestable();
        row.riskLevel = schemaObject.getRiskLevel();
        return row;
    }
}
