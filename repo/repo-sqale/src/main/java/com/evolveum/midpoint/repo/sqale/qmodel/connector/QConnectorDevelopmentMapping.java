/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.connector;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType.*;

/**
 * Mapping between {@link QConnector} and {@link ConnectorType}.
 */
public class QConnectorDevelopmentMapping
        extends QObjectMapping<ConnectorDevelopmentType, QConnectorDevelopment, MConnectorDevelopment> {

    public static final String DEFAULT_ALIAS_NAME = "cdev";

    private static QConnectorDevelopmentMapping instance;

    // Explanation in class Javadoc for SqaleTableMapping
    public static QConnectorDevelopmentMapping init(@NotNull SqaleRepoContext repositoryContext) {
        instance = new QConnectorDevelopmentMapping(repositoryContext);
        return instance;
    }

    // Explanation in class Javadoc for SqaleTableMapping
    public static QConnectorDevelopmentMapping get() {
        return Objects.requireNonNull(instance);
    }

    private QConnectorDevelopmentMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QConnector.TABLE_NAME, DEFAULT_ALIAS_NAME,
                ConnectorDevelopmentType.class, QConnectorDevelopment.class, repositoryContext);

    }

    @Override
    protected QConnectorDevelopment newAliasInstance(String alias) {
        return new QConnectorDevelopment(alias);
    }

    @Override
    public MConnectorDevelopment newRowObject() {
        return new MConnectorDevelopment();
    }

    @Override
    public @NotNull MConnectorDevelopment toRowObjectWithoutFullObject(
            ConnectorDevelopmentType schemaObject, JdbcSession jdbcSession) {
        MConnectorDevelopment row = super.toRowObjectWithoutFullObject(schemaObject, jdbcSession);
        return row;
    }
}
