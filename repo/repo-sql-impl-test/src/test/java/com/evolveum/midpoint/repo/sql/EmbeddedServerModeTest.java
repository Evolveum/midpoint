/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class EmbeddedServerModeTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(EmbeddedServerModeTest.class);

    @Autowired
    SqlRepositoryFactory sqlFactory;

    @Test
    public void testServerMode() throws Exception {
        LOGGER.info("testServerMode");

        Connection connection = null;
        try {
            SqlRepositoryConfiguration config = sqlFactory.getSqlConfiguration();

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
