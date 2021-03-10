/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import javax.xml.namespace.QName;

import com.querydsl.sql.types.EnumAsObjectType;

import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainerType;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.MReferenceType;
import com.evolveum.midpoint.repo.sqlbase.JdbcRepositoryConfiguration;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlRepoContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMappingRegistry;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskWaitingReasonType;

/**
 * SQL repository context adding support for QName cache.
 */
public class SqaleRepoContext extends SqlRepoContext {

    private final UriCache uriCache;

    public SqaleRepoContext(
            JdbcRepositoryConfiguration jdbcRepositoryConfiguration,
            DataSource dataSource,
            QueryModelMappingRegistry mappingRegistry) {
        super(jdbcRepositoryConfiguration, dataSource, mappingRegistry);

        // each enum type must be registered if we want to map it as objects (to PG enum types)
        querydslConfig.register(new EnumAsObjectType<>(MContainerType.class));
        querydslConfig.register(new EnumAsObjectType<>(MReferenceType.class));
        querydslConfig.register(new EnumAsObjectType<>(OperationResultStatusType.class));
        querydslConfig.register(new EnumAsObjectType<>(TaskExecutionStateType.class));
        querydslConfig.register(new EnumAsObjectType<>(TaskWaitingReasonType.class));

        uriCache = new UriCache();
    }

    @PostConstruct
    public void init() {
        try (JdbcSession jdbcSession = newJdbcSession().startReadOnlyTransaction()) {
            uriCache.initialize(jdbcSession);
        }
    }

    /** @see UriCache#searchId(QName) */
    public Integer searchCachedUriId(QName qName) {
        return uriCache.searchId(qName);
    }

    /** @see UriCache#searchId(String) */
    public Integer searchCachedUriId(String uri) {
        return uriCache.searchId(uri);
    }

    /** @see UriCache#resolveToId(QName) */
    public Integer resolveToId(String uri) {
        return uriCache.resolveToId(uri);
    }

    /** Returns ID for URI creating new cache row in DB as needed. */
    public Integer processCachedUri(String uri, JdbcSession jdbcSession) {
        return uriCache.processCachedUri(uri, jdbcSession);
    }
}
