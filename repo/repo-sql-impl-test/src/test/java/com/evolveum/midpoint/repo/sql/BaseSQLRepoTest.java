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

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.data.common.RContainerType;
import com.evolveum.midpoint.repo.sql.testing.TestSqlRepositoryFactory;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import java.io.File;

/**
 * @author lazyman
 */
public class BaseSQLRepoTest extends AbstractTestNGSpringContextTests {

    private static final Trace LOGGER = TraceManager.getTrace(BaseSQLRepoTest.class);

    public static final File FOLDER_BASIC = new File("./src/test/resources/basic");

    @Autowired(required = true)
    protected RepositoryService repositoryService;
    @Autowired(required = true)
    protected PrismContext prismContext;
    @Autowired
    protected SessionFactory factory;

    @BeforeClass
    public void beforeClass() {
        System.out.print("\n\n\n>>>>>>>>>>>>>>>>>>>>>>>> START " + getClass().getName() + "<<<<<<<<<<<<<<<<<<<<<<<<");
        LOGGER.info("\n\n\n>>>>>>>>>>>>>>>>>>>>>>>> START {} <<<<<<<<<<<<<<<<<<<<<<<<", new Object[]{getClass().getName()});
    }

    @AfterClass
    public void afterClass() {
        System.out.print(">>>>>>>>>>>>>>>>>>>>>>>> FINISH " + getClass().getName() + "<<<<<<<<<<<<<<<<<<<<<<<<\n");
        LOGGER.info(">>>>>>>>>>>>>>>>>>>>>>>> FINISH {} <<<<<<<<<<<<<<<<<<<<<<<<\n", new Object[]{getClass().getName()});
    }

    @BeforeMethod
    public void beforeMethod() throws Exception {
        testDatabase(factory);
    }

    private void testDatabase(SessionFactory sessionFactory) throws Exception {
        Session session = sessionFactory.openSession();
        try {
            TestSqlRepositoryFactory sqlRepoFactory = applicationContext.getBean(TestSqlRepositoryFactory.class);
            AssertJUnit.assertNotNull(sqlRepoFactory);
            AssertJUnit.assertNotNull(sqlRepoFactory.getSqlConfiguration());

            LOGGER.info("jdbc url: {}", new Object[]{sqlRepoFactory.getSqlConfiguration().getJdbcUrl()});

            session.beginTransaction();
            for (RContainerType type : RContainerType.values()) {
                long count = (Long) session.createQuery("select count(*) from " + type.getClazz().getSimpleName()).uniqueResult();
                LOGGER.info(">>> {} {}", new Object[]{type.getClazz().getSimpleName(), count});
//                AssertJUnit.assertEquals(0L, count);
            }
            session.getTransaction().commit();
        } catch (Exception ex) {
            LOGGER.error("Couldn't test database.", ex);
            session.getTransaction().rollback();
            throw ex;
        } finally {
            session.close();
        }
    }

}
