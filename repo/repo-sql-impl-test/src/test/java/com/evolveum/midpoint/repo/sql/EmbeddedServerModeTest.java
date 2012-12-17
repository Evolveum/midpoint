/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
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
@ContextConfiguration(locations = {
        "../../../../../ctx-sql-server-mode-test.xml",
        "../../../../../ctx-repository.xml",
        "classpath:ctx-repo-cache.xml",
        "../../../../../ctx-configuration-sql-test.xml"})
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
