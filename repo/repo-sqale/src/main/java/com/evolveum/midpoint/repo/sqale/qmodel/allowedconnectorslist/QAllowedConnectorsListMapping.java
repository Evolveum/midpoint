/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.allowedconnectorslist;

import java.util.Objects;

import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AllowedConnectorsListType;

/**
 * Mapping between {@link QAllowedConnectorsList} and {@link AllowedConnectorsListType}.
 */
public class QAllowedConnectorsListMapping
        extends QAssignmentHolderMapping<AllowedConnectorsListType, QAllowedConnectorsList, MObject> {

    public static final String DEFAULT_ALIAS_NAME = "acl";

    private static QAllowedConnectorsListMapping instance;

    // Explanation in class Javadoc for SqaleTableMapping
    public static QAllowedConnectorsListMapping init(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instance, repositoryContext)) {
            instance = new QAllowedConnectorsListMapping(repositoryContext);
        }
        return get();
    }

    // Explanation in class Javadoc for SqaleTableMapping
    public static QAllowedConnectorsListMapping get() {
        return Objects.requireNonNull(instance);
    }

    private QAllowedConnectorsListMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QAllowedConnectorsList.TABLE_NAME, DEFAULT_ALIAS_NAME,
                AllowedConnectorsListType.class, QAllowedConnectorsList.class, repositoryContext);
    }

    @Override
    protected QAllowedConnectorsList newAliasInstance(String alias) {
        return new QAllowedConnectorsList(alias);
    }
}
