/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmapping;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorHostType.F_HOSTNAME;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorHostType.F_PORT;

import com.evolveum.midpoint.repo.sqale.qbean.MConnectorHost;
import com.evolveum.midpoint.repo.sqale.qmodel.QConnectorHost;
import com.evolveum.midpoint.repo.sqale.qmodel.QObject;
import com.evolveum.midpoint.repo.sqlbase.SqlRepoContext;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.item.StringItemFilterProcessor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Mapping between {@link QObject} and {@link ObjectType}.
 */
public class QConnectorHostMapping
        extends QObjectMapping<ConnectorHostType, QConnectorHost, MConnectorHost> {

    public static final String DEFAULT_ALIAS_NAME = "conh";

    public static final QConnectorHostMapping INSTANCE = new QConnectorHostMapping();

    private QConnectorHostMapping() {
        super(QConnectorHost.TABLE_NAME, DEFAULT_ALIAS_NAME,
                ConnectorHostType.class, QConnectorHost.class);

        addItemMapping(F_HOSTNAME, StringItemFilterProcessor.mapper(path(q -> q.hostname)));
        addItemMapping(F_PORT, StringItemFilterProcessor.mapper(path(q -> q.port)));
    }

    @Override
    protected QConnectorHost newAliasInstance(String alias) {
        return new QConnectorHost(alias);
    }

    @Override
    public ObjectSqlTransformer<ConnectorHostType, QConnectorHost, MConnectorHost>
    createTransformer(SqlTransformerContext transformerContext, SqlRepoContext sqlRepoContext) {
        // TODO create specific transformer
        return new ObjectSqlTransformer<>(transformerContext, this);
    }

    @Override
    public MConnectorHost newRowObject() {
        return new MConnectorHost();
    }
}
