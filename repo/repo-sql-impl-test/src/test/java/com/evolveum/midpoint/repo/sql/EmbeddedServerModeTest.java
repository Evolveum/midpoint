/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql;

import java.sql.Connection;
import java.sql.DriverManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

@ContextConfiguration(locations = { "../../../../../ctx-test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class EmbeddedServerModeTest extends BaseSQLRepoTest {

    @Autowired
    private SqlRepositoryFactory sqlFactory;

    @Test
    public void testServerMode() throws Exception {
        logger.info("testServerMode");

        Connection connection = null;
        try {
            SqlRepositoryConfiguration config = sqlFactory.getConfiguration();

            Class.forName(config.getDriverClassName());
            connection = DriverManager.getConnection(config.getJdbcUrl(),
                    config.getJdbcUsername(), config.getJdbcPassword());
            AssertJUnit.assertNotNull(connection);
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }
}
