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

    private final QNameCache qNameCache;

    public SqaleRepoContext(
            JdbcRepositoryConfiguration jdbcRepositoryConfiguration,
            DataSource dataSource,
            QueryModelMappingRegistry mappingRegistry) {
        super(jdbcRepositoryConfiguration, dataSource, mappingRegistry);

        qNameCache = new QNameCache();
    }

    @PostConstruct
    public void init() {
        try (JdbcSession jdbcSession = newJdbcSession().startReadOnlyTransaction()) {
            qNameCache.initialize(jdbcSession);
        }
    }

    public Integer getQNameId(QName relation) {
        return qNameCache.getIdMandatory(relation);
    }
}
