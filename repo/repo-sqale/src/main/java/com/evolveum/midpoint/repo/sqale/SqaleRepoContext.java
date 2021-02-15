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

import com.evolveum.midpoint.repo.sqlbase.JdbcRepositoryConfiguration;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlRepoContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMappingRegistry;

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

    /** @see UriCache#resolveId(QName) */
    public Integer resolveId(String uri) {
        return uriCache.resolveId(uri);
    }

    /** Returns ID for URI creating new cache row in DB as needed. */
    public Integer processCachedUri(String uri, JdbcSession jdbcSession) {
        return uriCache.processCachedUri(uri, jdbcSession);
    }
}
