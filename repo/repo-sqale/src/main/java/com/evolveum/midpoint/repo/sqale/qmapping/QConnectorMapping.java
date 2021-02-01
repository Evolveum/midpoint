/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmapping;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType.*;

import com.evolveum.midpoint.repo.sqale.qbean.MConnector;
import com.evolveum.midpoint.repo.sqale.qmodel.QConnector;
import com.evolveum.midpoint.repo.sqale.qmodel.QObject;
import com.evolveum.midpoint.repo.sqlbase.SqlRepoContext;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.item.StringItemFilterProcessor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Mapping between {@link QObject} and {@link ObjectType}.
 */
public class QConnectorMapping
        extends QObjectMapping<ConnectorType, QConnector, MConnector> {

    public static final String DEFAULT_ALIAS_NAME = "con";

    public static final QConnectorMapping INSTANCE = new QConnectorMapping();

    private QConnectorMapping() {
        super(QConnector.TABLE_NAME, DEFAULT_ALIAS_NAME, ConnectorType.class, QConnector.class);

        addItemMapping(F_CONNECTOR_BUNDLE,
                StringItemFilterProcessor.mapper(path(q -> q.connectorBundle)));
        addItemMapping(F_CONNECTOR_TYPE,
                StringItemFilterProcessor.mapper(path(q -> q.connectorType)));
        addItemMapping(F_CONNECTOR_VERSION,
                StringItemFilterProcessor.mapper(path(q -> q.connectorVersion)));
        addItemMapping(F_FRAMEWORK, StringItemFilterProcessor.mapper(path(q -> q.framework)));

        // TODO connector host ref mapping: connectorHostRefTargetOid, connectorHostRefTargetType, connectorHostRefRelationId
    }

    @Override
    protected QConnector newAliasInstance(String alias) {
        return new QConnector(alias);
    }

    @Override
    public ObjectSqlTransformer<ConnectorType, QConnector, MConnector>
    createTransformer(SqlTransformerContext transformerContext, SqlRepoContext sqlRepoContext) {
        // TODO create specific transformer
        return new ObjectSqlTransformer<>(transformerContext, this);
    }

    @Override
    public MConnector newRowObject() {
        return new MConnector();
    }
}
