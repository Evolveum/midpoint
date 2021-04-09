/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.connector;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType.*;

import com.evolveum.midpoint.repo.sqale.qmodel.SqaleTableMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;

/**
 * Mapping between {@link QConnector} and {@link ConnectorType}.
 */
public class QConnectorMapping
        extends QObjectMapping<ConnectorType, QConnector, MConnector> {

    public static final String DEFAULT_ALIAS_NAME = "con";

    public static final QConnectorMapping INSTANCE = new QConnectorMapping();

    private QConnectorMapping() {
        super(QConnector.TABLE_NAME, DEFAULT_ALIAS_NAME, ConnectorType.class, QConnector.class);

        addItemMapping(F_CONNECTOR_BUNDLE, stringMapper(path(q -> q.connectorBundle)));
        addItemMapping(F_CONNECTOR_TYPE, stringMapper(path(q -> q.connectorType)));
        addItemMapping(F_CONNECTOR_VERSION, stringMapper(path(q -> q.connectorVersion)));
        addItemMapping(F_FRAMEWORK, uriMapper(path(q -> q.frameworkId)));
        addItemMapping(F_CONNECTOR_HOST_REF, SqaleTableMapping.refMapper(
                path(q -> q.connectorHostRefTargetOid),
                path(q -> q.connectorHostRefTargetType),
                path(q -> q.connectorHostRefRelationId)));

        // TODO mapping for List<String> F_TARGET_SYSTEM_TYPE
    }

    @Override
    protected QConnector newAliasInstance(String alias) {
        return new QConnector(alias);
    }

    @Override
    public ConnectorSqlTransformer createTransformer(SqlTransformerSupport transformerSupport) {
        return new ConnectorSqlTransformer(transformerSupport, this);
    }

    @Override
    public MConnector newRowObject() {
        return new MConnector();
    }
}
