/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.connector;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType.*;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;

/**
 * Mapping between {@link QConnector} and {@link ConnectorType}.
 */
public class QConnectorMapping
        extends QAssignmentHolderMapping<ConnectorType, QConnector, MConnector> {

    public static final String DEFAULT_ALIAS_NAME = "con";

    private static QConnectorMapping instance;

    // Explanation in class Javadoc for SqaleTableMapping
    public static QConnectorMapping init(@NotNull SqaleRepoContext repositoryContext) {
        instance = new QConnectorMapping(repositoryContext);
        return instance;
    }

    // Explanation in class Javadoc for SqaleTableMapping
    public static QConnectorMapping get() {
        return Objects.requireNonNull(instance);
    }

    private QConnectorMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QConnector.TABLE_NAME, DEFAULT_ALIAS_NAME,
                ConnectorType.class, QConnector.class, repositoryContext);

        addItemMapping(F_CONNECTOR_BUNDLE, stringMapper(q -> q.connectorBundle));
        addItemMapping(F_CONNECTOR_TYPE, stringMapper(q -> q.connectorType));
        addItemMapping(F_CONNECTOR_VERSION, stringMapper(q -> q.connectorVersion));
        addItemMapping(F_FRAMEWORK, uriMapper(q -> q.frameworkId));
        addRefMapping(F_CONNECTOR_HOST_REF,
                q -> q.connectorHostRefTargetOid,
                q -> q.connectorHostRefTargetType,
                q -> q.connectorHostRefRelationId,
                QConnectorHostMapping::get);

        addItemMapping(F_TARGET_SYSTEM_TYPE, multiUriMapper(q -> q.targetSystemTypes));
        addItemMapping(F_AVAILABLE, booleanMapper(q -> q.available));
    }

    @Override
    protected QConnector newAliasInstance(String alias) {
        return new QConnector(alias);
    }

    @Override
    public MConnector newRowObject() {
        return new MConnector();
    }

    @Override
    public @NotNull MConnector toRowObjectWithoutFullObject(
            ConnectorType schemaObject, JdbcSession jdbcSession) {
        MConnector row = super.toRowObjectWithoutFullObject(schemaObject, jdbcSession);

        row.connectorBundle = schemaObject.getConnectorBundle();
        row.connectorType = schemaObject.getConnectorType();
        row.connectorVersion = schemaObject.getConnectorVersion();
        row.frameworkId = processCacheableUri(schemaObject.getFramework());

        setReference(schemaObject.getConnectorHostRef(),
                o -> row.connectorHostRefTargetOid = o,
                t -> row.connectorHostRefTargetType = t,
                r -> row.connectorHostRefRelationId = r);

        row.targetSystemTypes = processCacheableUris(schemaObject.getTargetSystemType());
        row.available = schemaObject.isAvailable();

        return row;
    }
}
