/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
    private SqlRepositoryConfiguration repositoryConfiguration;

    @Test
    public void testServerMode() throws Exception {
        logger.info("testServerMode");

        Connection connection = null;
        try {
            SqlRepositoryConfiguration config = repositoryConfiguration;

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
