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
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;

public class ConnectorSqlTransformer
        extends ObjectSqlTransformer<ConnectorType, QConnector, MConnector> {

    public ConnectorSqlTransformer(
            SqlTransformerSupport transformerSupport, QConnectorMapping mapping) {
        super(transformerSupport, mapping);
    }

    @Override
    public @NotNull MConnector toRowObjectWithoutFullObject(
            ConnectorType schemaObject, JdbcSession jdbcSession) {
        MConnector row = super.toRowObjectWithoutFullObject(schemaObject, jdbcSession);

        row.connectorBundle = schemaObject.getConnectorBundle();
        row.connectorType = schemaObject.getConnectorType();
        row.connectorVersion = schemaObject.getConnectorVersion();
        row.frameworkId = processCacheableUri(schemaObject.getFramework(), jdbcSession);

        setReference(schemaObject.getConnectorHostRef(), jdbcSession,
                o -> row.connectorHostRefTargetOid = o,
                t -> row.connectorHostRefTargetType = t,
                r -> row.connectorHostRefRelationId = r);

        row.targetSystemTypes = arrayFor(schemaObject.getTargetSystemType());

        return row;
    }
}
