/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.connector;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.qmodel.object.ObjectSqlTransformer;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

public class ConnectorSqlTransformer
        extends ObjectSqlTransformer<ConnectorType, QConnector, MConnector> {

    public ConnectorSqlTransformer(
            SqlTransformerContext transformerContext, QConnectorMapping mapping) {
        super(transformerContext, mapping);
    }

    @Override
    public @NotNull MConnector toRowObjectWithoutFullObject(ConnectorType schemaObject, JdbcSession jdbcSession) {
        MConnector row = super.toRowObjectWithoutFullObject(schemaObject, jdbcSession);

        row.connectorBundle = schemaObject.getConnectorBundle();
        row.connectorType = schemaObject.getConnectorType();
        row.connectorVersion = schemaObject.getConnectorVersion();
        row.framework = schemaObject.getFramework();

        ObjectReferenceType ref = schemaObject.getConnectorHostRef();
        if (ref != null) {
            row.connectorHostRefTargetOid = oidToUUid(ref.getOid());
            row.connectorHostRefTargetType = schemaTypeToCode(ref.getType());
            row.connectorHostRefRelationId = processCachedUri(ref.getRelation(), jdbcSession);
        }

        return row;
    }
}
