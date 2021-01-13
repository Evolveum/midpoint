/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase;

import com.evolveum.midpoint.repo.api.RepositoryServiceFactory;

/**
 * Version of RepositoryServiceFactory that exposes {@link JdbcRepositoryConfiguration}
 * common to various SQL-based repositories.
 */
public interface JdbcRepositoryServiceFactory extends RepositoryServiceFactory {

    JdbcRepositoryConfiguration jdbcRepositoryConfiguration();
}
