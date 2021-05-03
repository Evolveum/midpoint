/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.connector;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorHostType.F_HOSTNAME;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorHostType.F_PORT;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorHostType;

/**
 * Mapping between {@link QConnectorHost} and {@link ConnectorHostType}.
 */
public class QConnectorHostMapping
        extends QObjectMapping<ConnectorHostType, QConnectorHost, MConnectorHost> {

    public static final String DEFAULT_ALIAS_NAME = "conh";

    public static final QConnectorHostMapping INSTANCE = new QConnectorHostMapping();

    private QConnectorHostMapping() {
        super(QConnectorHost.TABLE_NAME, DEFAULT_ALIAS_NAME,
                ConnectorHostType.class, QConnectorHost.class);

        addItemMapping(F_HOSTNAME, stringMapper(q -> q.hostname));
        addItemMapping(F_PORT, stringMapper(q -> q.port));
    }

    @Override
    protected QConnectorHost newAliasInstance(String alias) {
        return new QConnectorHost(alias);
    }

    @Override
    public MConnectorHost newRowObject() {
        return new MConnectorHost();
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
