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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorHostType;

public class ConnectorHostSqlTransformer
        extends ObjectSqlTransformer<ConnectorHostType, QConnectorHost, MConnectorHost> {

    public ConnectorHostSqlTransformer(
            SqlTransformerSupport transformerSupport, QConnectorHostMapping mapping) {
        super(transformerSupport, mapping);
    }

    @Override
    public @NotNull MConnectorHost toRowObjectWithoutFullObject(
            ConnectorHostType schemaObject, JdbcSession jdbcSession) {
        MConnectorHost row = super.toRowObjectWithoutFullObject(schemaObject, jdbcSession);

        row.hostname = schemaObject.getHostname();
        row.port = schemaObject.getPort();

        return row;
    }
}
