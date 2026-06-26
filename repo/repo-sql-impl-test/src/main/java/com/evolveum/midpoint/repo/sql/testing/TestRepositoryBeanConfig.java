/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.testing;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Repository test configuration selector.
 *
 * The imported configurations are mutually exclusive using their repository-type conditions.
 * Keeping the concrete configurations behind this Java import lets Spring evaluate those
 * conditions before registering repository-specific test beans.
 */
@Configuration
@Import({
        TestSqlRepositoryBeanConfig.class,
        TestSqaleRepositoryBeanConfig.class
})
public class TestRepositoryBeanConfig {
}
